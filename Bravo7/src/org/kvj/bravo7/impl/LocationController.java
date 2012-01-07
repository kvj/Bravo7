package org.kvj.bravo7.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.R;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

abstract public class LocationController implements LocationListener {

	public enum LocationType {
		LocationSpot, LocationPath
	};

	abstract public class LocationRequest {

		private String id = null;
		private boolean wantLocation = false;
		private boolean done = false;

		public LocationRequest(String id) {
			this.id = id;
		}

		abstract LocationType getType();

		abstract boolean consumeLocation(JSONObject location);

		public abstract Object getResult();

		public String getId() {
			return id;
		}

		public boolean isWantLocation() {
			return wantLocation;
		}

		public void setWantLocation(boolean wantLocation) {
			this.wantLocation = wantLocation;
		}

		public boolean isDone() {
			return done;
		}

		public void setDone(boolean done) {
			this.done = done;
		}
	}

	public class LocationSpotRequest extends LocationRequest {

		private JSONObject location = null;

		public LocationSpotRequest(String id) {
			super(id);
		}

		@Override
		LocationType getType() {
			return LocationType.LocationSpot;
		}

		@Override
		boolean consumeLocation(JSONObject location) {
			this.location = location;
			return true;
		}

		@Override
		public Object getResult() {
			return location;
		}
	}

	public class LocationPathRequest extends LocationRequest implements Runnable {

		JSONArray path = new JSONArray();
		long timeout = 0;
		boolean needFinish = false;

		public LocationPathRequest(String id, long timeout) {
			super(id);
			this.timeout = timeout;
		}

		@Override
		public LocationType getType() {
			return LocationType.LocationPath;
		}

		@Override
		boolean consumeLocation(JSONObject location) {
			handler.removeCallbacks(this);
			if (location != null) {
				path.put(location);
			}
			if (needFinish) {
				return true;
			}
			handler.postDelayed(this, 1000 * timeout);
			return false;
		}

		@Override
		public void run() {
			startRequest(this);
		}

		@Override
		public JSONArray getResult() {
			return path;
		}

		public long getTimeout() {
			return timeout;
		}

		public void finish(Date date, boolean lastRun) {
			handler.removeCallbacks(this);
			if (lastRun) {
				needFinish = true;
				startRequest(this);
				return;
			}
			try {
				JSONArray newPath = new JSONArray();
				for (int i = 0; i < path.length(); i++) {
					JSONObject location = path.getJSONObject(i);
					long dt = location.getLong("at");
					if (dt <= date.getTime()) {
						newPath.put(location);
					}
				}
				path = newPath;
			} catch (Exception e) {
				e.printStackTrace();
			}
			synchronized (requests) {
				setDone(true);
			}
			checkFinish();
		}

	}

	private static final String TAG = "LocationController";
	ApplicationContext context = null;
	LocationManager manager = null;
	private String[] locationProviders = { LocationManager.GPS_PROVIDER,
			LocationManager.NETWORK_PROVIDER };
	private LocationTimeout timeout = new LocationTimeout();
	private Map<String, LocationRequest> requests = new HashMap<String, LocationController.LocationRequest>();
	private Handler handler = new Handler();
	private boolean enabled = false;

	class LocationTimeout implements Runnable {

		@Override
		public void run() {
			disableLocation(null);
		}

	}

	public LocationController(ApplicationContext context) {
		this.context = context;
		manager = (LocationManager) context.getContext().getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public void onLocationChanged(Location location) {
		int accuracy = context.getIntPreference("location_accuracy",
				R.string.location_accuracy);
		Log.i(TAG, "Location[" + location.getProvider() + "]: "
					+ location.getAccuracy() + ", " + location.getSpeed());
		if (location.getAccuracy() < accuracy) {
			// Found
			JSONObject loc = new JSONObject();
			try {
				loc.put("lon", location.getLongitude());
				loc.put("lat", location.getLatitude());
				loc.put("speed", location.getSpeed());
				loc.put("alt", location.getAltitude());
				loc.put("acc", location.getAccuracy());
				loc.put("at", new Date().getTime());
				Log.i(TAG, "Got location: " + loc);
				disableLocation(loc);
			} catch (JSONException e) {
			}
		} else {

		}
	}

	private void disableLocation(JSONObject location) {
		if (enabled) {
			Log.i(TAG, "Disabling location");
			manager.removeUpdates(this);
			enabled = false;
			synchronized (requests) {
				for (LocationRequest request : requests.values()) {
					if (request.isWantLocation()) {
						request.setWantLocation(false);
						if (request.consumeLocation(location)) {
							request.setDone(true);
						}
					}
				}
			}
		}
		checkFinish();
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.i(TAG, "Provider " + provider + " is disabled");
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.i(TAG, "Provider " + provider + " is enabled");
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i(TAG, "Provider " + provider + " status changed: " + status);
	}

	synchronized private void enableLocation() {
		if (!enabled) {
			handler.removeCallbacks(timeout);
			int locationMaxMins = context.getIntPreference("location_timeout",
					R.string.location_timeout);
			if (locationMaxMins > 0) {
				handler.postDelayed(timeout, locationMaxMins * 60 * 1000);
			}
			for (String prov : locationProviders) {
				manager.requestLocationUpdates(prov, 0, 0, this);
			}
			enabled = true;
			Log.i(TAG, "All location providers started");
			locationStarted();
		}
	}

	public boolean isBusy() {
		synchronized (requests) {
			for (LocationRequest request : requests.values()) {
				if (!request.isDone()) {
					return true;
				}
			}
		}
		return false;
	}

	public void addLocationRequest(String id) {
		LocationRequest request = new LocationSpotRequest(id);
		synchronized (requests) {
			requests.put(id, request);
		}
		startRequest(request);
	}

	public void addPathRequest(String id, long intervalSec) {
		LocationRequest request = new LocationPathRequest(id, intervalSec);
		synchronized (requests) {
			requests.put(id, request);
		}
		startRequest(request);
	}

	public <T> T getResult(String id, Class<T> cl) {
		LocationRequest request = requests.get(id);
		if (request == null) {
			return null;
		}
		Object object = request.getResult();
		if (null == object) {
			return null;
		}
		if (!cl.isAssignableFrom(object.getClass())) {
			return null;
		}
		return (T) request.getResult();
	}

	public void clear() {
		requests.clear();
	}

	private void startRequest(LocationRequest request) {
		request.setWantLocation(true);
		enableLocation();
	}

	private void checkFinish() {
		boolean finished = !isBusy();
		if (finished) {
			Log.i(TAG, "All locations done");
			locationFinished(true);
		}
	}

	public LocationPathRequest[] getActivePathRequests() {
		List<LocationPathRequest> result = new ArrayList<LocationController.LocationPathRequest>();
		synchronized (requests) {
			for (LocationRequest request : requests.values()) {
				if (!request.isDone() && request.getType() == LocationType.LocationPath) {
					result.add((LocationPathRequest) request);
				}
			}
		}
		return result.toArray(new LocationPathRequest[0]);
	}

	abstract public void locationStarted();

	abstract public void locationFinished(boolean ok);
}
