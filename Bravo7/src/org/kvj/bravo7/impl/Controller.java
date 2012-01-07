package org.kvj.bravo7.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.C2DMReceiver;
import org.kvj.bravo7.Constants;
import org.kvj.bravo7.R;
import org.kvj.bravo7.c2dm.C2DMessaging;
import org.kvj.bravo7.data.DataAccessService;
import org.kvj.bravo7.data.DataManageService;
import org.kvj.bravo7.data.impl.OAuthDataAccessImpl;
import org.kvj.bravo7.data.impl.OAuthDataManageImpl;
import org.kvj.bravo7.data.sql.CachingJSONStore;
import org.kvj.bravo7.data.sql.impl.CachingJSONStoreImpl;
import org.kvj.bravo7.event.Event;
import org.kvj.bravo7.event.EventGenerator;
import org.kvj.bravo7.event.EventGenerator.SendType;
import org.kvj.bravo7.event.EventListener;
import org.kvj.bravo7.event.impl.BatteryEventGenerator;
import org.kvj.bravo7.event.impl.CheckinLoaderEventGenerator;
import org.kvj.bravo7.event.impl.ReloadEventGenerator;
import org.kvj.bravo7.event.impl.SMSEventGenerator;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class Controller implements EventListener {

	public enum RefreshType {
		Full, SendCheckins, UpdateCheckins
	};

	class CronJob implements Runnable {

		String event;
		JSONObject params;
		String name;

		public CronJob(String event, String name, JSONObject params) {
			this.event = event;
			this.name = name;
			this.params = params;
		}

		@Override
		public void run() {
			context.log("Cron job by timer: " + name);
			synchronized (eventGenerators) {
				Event e = new Event(event, params);
				for (EventGenerator eg : eventGenerators) {
					if (eg.cron(e)) {
						break;
					}
				}
			}
			cron.post(new Runnable() {

				@Override
				public void run() {
					reloadCron();
				}
			});
		}

	}

	public static final int UPDATE_STEPS = 5;

	public enum State {
		NotUpdated, UpdatedOK, UpdatedError, Updating, Error, Startup
	};

	public interface NetworkResult {

		public void onResult(Object result);
	}

	public interface UpdateMonitor {

		public void stateChanged(State state, String error);

		public void foregroundStateChanged(boolean foreground);
	}

	protected static final String TAG = "Controller";
	private CachingJSONStore store = null;
	private static final String STORE_TEMPLATES = "tmpls";
	private static final String STORE_CHECKINS = "checkins";
	private static final String STORE_HOOKS = "hooks";
	// private Map<String, Bitmap> iconCache = new HashMap<String, Bitmap>();
	private List<UpdateMonitor> monitors = new ArrayList<Controller.UpdateMonitor>();
	private State state = State.NotUpdated;
	private String stateMessage = null;
	private List<EventGenerator> eventGenerators = new ArrayList<EventGenerator>();
	private Collection<JSONObject> hooks = null;
	private List<CronJob> cronJobs = new ArrayList<CronJob>();
	private Handler cron = new Handler();
	private LocationController locationController = null;

	private ApplicationContext context = null;

	public Controller(final ApplicationContext context) {
		this.context = context;
		store = new CachingJSONStoreImpl(context.getContext(), "data");
		stateChanged(State.Startup);
		locationController = new LocationController(context) {

			@Override
			public void locationStarted() {
				stateChanged(State.NotUpdated, "Waiting for location");
				synchronized (monitors) {
					for (UpdateMonitor monit : monitors) {
						monit.foregroundStateChanged(true);
					}
				}
			}

			@Override
			public void locationFinished(boolean ok) {
				synchronized (monitors) {
					for (UpdateMonitor monit : monitors) {
						monit.foregroundStateChanged(false);
					}
				}
				if (ok) {
					stateChanged(State.NotUpdated);
				}
			}
		};
		hooks = store.getStream(STORE_HOOKS, true).values();
		eventGenerators.add(new BatteryEventGenerator(context, this));
		eventGenerators.add(new SMSEventGenerator(context, this));
		eventGenerators.add(new ReloadEventGenerator(context, this));
		eventGenerators.add(new CheckinLoaderEventGenerator(context, this));
		new Thread() {

			public void run() {
				context.updateWidgets(-1);
				reloadCron();
				registerInC2DM();
			};
		}.start();
	}

	private DataManageService getManageService() {
		return new OAuthDataManageImpl(context.getStringPreference(
				Constants.URL_KEY, ""), context.getStringPreference(
				Constants.TOKEN_KEY, ""));
	}

	public synchronized String doReload(RefreshType refreshType) {
		if (locationController.isBusy()) {
			return stateChanged(State.UpdatedError, "Location service is active");
		}
		stateChanged(State.Updating, "Reloading...");
		DataManageService service = getManageService();
		if (service.ping() == null) {
			return stateChanged(State.UpdatedError, "Ping failed");
		}
		// publishProgress(0);
		Collection<JSONObject> newCheckins = store.getStream(STORE_CHECKINS,
				false).values();
		if (newCheckins == null) {
			return stateChanged(State.UpdatedError,
					"Error getting new checkins");
		}
		for (JSONObject ch : newCheckins) {
			String id = ch.optString("id", "");
			JSONObject location = locationController.getResult(id, JSONObject.class);
			if (location != null) {
				try {
					ch.put("location", location);
				} catch (JSONException e) {
				}
			}
			JSONArray path = locationController.getResult(id, JSONArray.class);
			if (path != null) {
				try {
					ch.put("path", path);
				} catch (JSONException e) {
				}
			}
			JSONObject checkin = service.newCheckin(ch);
			if (checkin != null) {
				Log.i(TAG, "Checkin uploaded: " + id);
				byte[] data = store.findBlobByObject(STORE_CHECKINS, ch);
				JSONObject dataInfo = ch.optJSONObject("__data");
				if (data != null && dataInfo != null) {
					Log.i(TAG, "Uploading: " + data.length);
					ByteArrayInputStream stream = new ByteArrayInputStream(data);
					boolean upload = service
							.uploadFile(id, dataInfo.optString("name", ""),
									dataInfo.optString("type", ""), stream,
									data.length);
					Log.i(TAG, "Upload result: " + upload);
					if (!upload) {
						service.removeCheckin(id);
						return stateChanged(State.UpdatedError,
								"Error uploading data");
					}
				} else {
					Log.i(TAG, "No data: " + data + ", " + dataInfo);
				}
			}
			if (!store.remove(STORE_CHECKINS, ch)) {
				return stateChanged(State.UpdatedError,
						"Error removing checkin");
			}
		}
		locationController.clear();
		if (refreshType == RefreshType.Full
				|| refreshType == RefreshType.UpdateCheckins) {
			Map<String, JSONObject> oldFavs = store.getStream(STORE_CHECKINS,
					true);
			List<String> ids = context
					.getStringArrayPreference(Constants.CHECKIN_FAVS);
			List<String> hashes = new ArrayList<String>();
			for (String id : ids) {
				JSONObject oldFav = oldFavs.get(id);
				if (null != oldFav) {
					hashes.add(oldFav.optString("hash", "-"));
				} else {
					hashes.add("-");
				}
			}
			List<JSONObject> checkins = service.getCheckins(ids, hashes);
			if (checkins == null) {
				return stateChanged(State.UpdatedError,
						"Error refreshing checkins");
			}
			if (!store.clear(STORE_CHECKINS)) {
				return stateChanged(State.UpdatedError,
						"Error removing checkins");
			}
			for (JSONObject ch : checkins) {
				if (ch.optBoolean("match", false)) {
					store.add(STORE_CHECKINS,
							oldFavs.get(ch.optString("id", "")), null, true);
				} else {
					store.add(STORE_CHECKINS, ch, null, true);
				}
			}
			context.updateWidgets(-1);
		}
		if (refreshType == RefreshType.Full) {
			if (!store.clear(STORE_TEMPLATES)) {
				return stateChanged(State.UpdatedError,
						"Error removing templates");
			}
			List<JSONObject> templates = service.getTemplates();
			if (templates == null) {
				return stateChanged(State.UpdatedError,
						"Error getting new templates");
			}
			for (JSONObject tmpl : templates) {
				store.add(STORE_TEMPLATES, tmpl, null, true);
			}
			if (!store.clear(STORE_HOOKS)) {
				return stateChanged(State.UpdatedError, "Error removing hooks");
			}
			hooks = service.getHooks(context.getIntPreference("cron_days",
					R.string.cron_days));
			if (hooks == null) {
				return stateChanged(State.UpdatedError,
						"Error getting new event hooks");
			}
			// Log.i(TAG, "Hooks arrived: "+hooks);
			for (JSONObject hook : hooks) {
				store.add(STORE_HOOKS, hook, null, true);
			}
			reloadCron();
			context.updateWidgets(-1);
		}
		return stateChanged(State.UpdatedOK);
	}

	private synchronized void reloadCron() {
		synchronized (cronJobs) {
			for (CronJob job : cronJobs) {
				cron.removeCallbacks(job);
			}
			cronJobs.clear();
		}
		Collection<JSONObject> hooks = store.getStream(STORE_HOOKS, true)
				.values();
		Date now = new Date();
		for (JSONObject hook : hooks) {
			JSONObject when = hook.optJSONObject("when");
			JSONObject params = hook.optJSONObject("params");
			if (null == when || null == params) {
				continue;
			}
			JSONArray dates = when.optJSONArray("lcron");
			String event = params.optString("event");
			if (dates == null || event == null) {
				continue;
			}
			synchronized (cronJobs) {
				for (int i = 0; i < dates.length(); i++) {
					Date dt = new Date(dates.optLong(i));
					if (dt.getTime() > now.getTime()) {
						CronJob job = new CronJob(event, event, params);
						int minutes = Math
								.round((dt.getTime() - now.getTime()) / 1000);
						Log.i(TAG,
								"Scheduling: " + event + ", "
										+ hook.optString("name", "???") + ", "
										+ minutes + ", " + dt);
						cron.postAtTime(job, dt.getTime());
						cronJobs.add(job);
						break;
					}
				}
			}
		}
	}

	public void reload(final RefreshType refreshType, final NetworkResult result) {
		AsyncTask<Void, Integer, String> task = new AsyncTask<Void, Integer, String>() {

			@Override
			protected String doInBackground(Void... params) {
				return doReload(refreshType);
			}

			@Override
			protected void onPostExecute(String res) {
				super.onPostExecute(res);
				result.onResult(res);
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
			}
		};
		task.execute(null);
	}

	public void search(final JSONObject query, final NetworkResult result) {
		AsyncTask<Void, Integer, Object> task = new AsyncTask<Void, Integer, Object>() {

			@Override
			protected Object doInBackground(Void... params) {
				DataManageService service = getManageService();
				List<JSONObject> result = service.getCheckins(query);
				if (result == null) {
					return "Search failed";
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object res) {
				super.onPostExecute(res);
				result.onResult(res);
			}
		};
		task.execute(null);
	}

	public void fetchToken(final String url, final String username,
			final String password, final NetworkResult result) {
		AsyncTask<Void, Integer, String> task = new AsyncTask<Void, Integer, String>() {

			@Override
			protected String doInBackground(Void... params) {
				Log.i(TAG, "fetchToken: " + url + ", " + username + ", "
						+ password);
				DataAccessService service = getAccessService(url);
				String token = service.getAccessToken(username, password);
				if (token != null) {
					context.setStringPreference(Constants.URL_KEY, url);
					context.setStringPreference(Constants.TOKEN_KEY, token);
					return null;
				}
				return "Invalid username/password";
			}

			@Override
			protected void onPostExecute(String res) {
				super.onPostExecute(res);
				result.onResult(res);
			}

		};
		task.execute(null);
	}

	private DataAccessService getAccessService(String url) {
		return new OAuthDataAccessImpl(url + "/login", url + "/token",
				Constants.CLIENT_ID, Constants.CLIENT_SECRET);
	}

	public Collection<JSONObject> getTemplates() {
		Collection<JSONObject> result = store.getStream(STORE_TEMPLATES, true)
				.values();
		if (result == null) {
			result = new ArrayList<JSONObject>();
		}
		return result;
	}

	public Bitmap getIcon(String name) {
		return getIcon(name, 0);
	}

	public Bitmap getIcon(String name, int size) {
		// Bitmap result = iconCache.get(name);
		// if (result != null && size == 0) {
		// return result;
		// }
		AssetManager am = context.getContext().getAssets();
		try {
			BufferedInputStream buf = new BufferedInputStream(am.open("icons/"
					+ name + ".png"));
			Options opts = new Options();
			if (size > 0) {
				opts.inSampleSize = size;
			}
			// opts.inScaled = true;
			// opts.inDensity = 160;
			Bitmap bitmap = BitmapFactory.decodeStream(buf, null, opts);
			buf.close();
			// if (size == 0) {
			// iconCache.put(name, bitmap);
			// }
			return bitmap;
		} catch (Exception e) {
			Log.w(TAG, "Icon not found: " + name);
		}
		return null;
	}

	private static String uuid(int size) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < size; i++) {
			builder.append((int) Math.floor(Math.random() * 10));
		}
		return builder.toString();
	}

	public String addCheckin(JSONObject checkin, byte[] data,
			boolean location, long pathSecs, boolean reload) {
		String id = uuid(12);
		try {
			checkin.put("id", id);
			Log.i(TAG, "New checkin ID: " + checkin.optString("id", "-"));
			checkin.put("force_create", true);
		} catch (JSONException e) {
		}
		if (!store.add(STORE_CHECKINS, checkin, data, false)) {
			return stateChanged(State.Error, "Error adding new checkin");
		}
		if (location) {
			Log.i(TAG, "Need location");
			locationController.addLocationRequest(id);
			return null;
		}
		if (pathSecs > 0) {
			Log.i(TAG, "Need path: " + pathSecs);
			locationController.addPathRequest(id, pathSecs);
			return null;
		}
		stateChanged(State.NotUpdated, "Checkin added");
		if (reload) {
			new Thread() {

				public void run() {
					doReload(RefreshType.SendCheckins);
				};
			}.start();
		}
		return null;
	}

	private String stateChanged(State state) {
		return stateChanged(state, null);
	}

	private String stateChanged(State state, String error) {
		this.state = state;
		this.stateMessage = error;
		synchronized (monitors) {
			for (UpdateMonitor monit : monitors) {
				monit.stateChanged(state, error);
			}
		}
		return error;
	}

	public void addUpdateMonitor(UpdateMonitor monitor) {
		synchronized (monitors) {
			if (!monitors.contains(monitor)) {
				monitors.add(monitor);
				monitor.stateChanged(state, stateMessage);
			}
		}
	}

	public void removeUpdateMonitor(UpdateMonitor monitor) {
		synchronized (monitors) {
			monitors.remove(monitor);
		}
	}

	public JSONObject getTemplate(String id) {
		return store.findByID(STORE_TEMPLATES, id);
	}

	public boolean isFavoriteCheckin(String id) {
		List<String> favs = context
				.getStringArrayPreference(Constants.CHECKIN_FAVS);
		return favs.contains(id);
	}

	public void favoriteCheckin(String id, boolean add) {
		context.setStringArrayPreference(Constants.CHECKIN_FAVS, id, add);
		stateChanged(State.NotUpdated);
	}

	public boolean isFavoriteTemplate(String id) {
		List<String> favs = context
				.getStringArrayPreference(Constants.TEMPLATE_FAVS);
		return favs.contains(id);
	}

	public void favoriteTemplate(String id, boolean add) {
		context.setStringArrayPreference(Constants.TEMPLATE_FAVS, id, add);
	}

	public Collection<JSONObject> getCheckins() {
		return store.getStream(STORE_CHECKINS, true).values();
	}

	public JSONObject getCheckin(String id) {
		return store.findByID(STORE_CHECKINS, id);
	}

	public Collection<JSONObject> getUnsentCheckins() {
		return store.getStream(STORE_CHECKINS, false).values();
	}

	public Collection<JSONObject> getChildren(JSONObject checkin) {
		String id = checkin.optString("id", "-");
		List<JSONObject> result = new ArrayList<JSONObject>();
		JSONArray refs = checkin.optJSONArray("refs");
		for (int i = 0; i < refs.length(); i++) {
			try {
				result.add(refs.getJSONObject(i));
			} catch (JSONException e) {
			}
		}
		Collection<JSONObject> unsent = getUnsentCheckins();
		for (JSONObject ch : unsent) {
			if (ch.optString("attach_to", "-").equals(id)) {
				result.add(ch);
			}
		}
		return result;
	}

	public void removeUnsentCheckin(JSONObject checkin) {
		store.remove(STORE_CHECKINS, checkin);
	}

	@Override
	public void eventReceived(EventGenerator generator, Event event) {
		if (hooks == null) {
			return;
		}
		for (JSONObject hook : hooks) {
			JSONObject when = hook.optJSONObject("when");
			if (when == null) {
				continue;
			}
			String hook_event = when.optString("event", "");
			if (hook_event.equals(event.getName())) {
				SendType st = generator.shouldCreateCheckin(
						hook.optString("id", ""), event,
						hook.optJSONObject("params"));
				if (st != SendType.NotSend) {
					generateCheckinFromEvent(hook.optJSONObject("params"),
							event.getData(), st);
				}
			}
		}
	}

	private void generateCheckinFromEvent(JSONObject params, JSONObject data,
			SendType sendType) {
		Log.i(TAG, "Generating event...");
		if (params == null || data == null) {
			return;
		}
		try {
			JSONObject checkin = new JSONObject();
			JSONObject template = getTemplate(params.optString("template", ""));
			if (template == null) {
				Log.w(TAG,
						"Template not found: "
								+ params.optString("template", ""));
				return;
			}
			checkin.put("created", new Date().getTime());
			checkin.put("template", params.optString("template"));
			JSONObject tmpl = template.optJSONObject("template");
			if (tmpl != null) {
				Iterator<String> keys = tmpl.keys();
				while (keys.hasNext()) {
					String key = (String) keys.next();
					String param = params.optString(key, "");
					if (!"".equals(param)) {
						String value = data.optString(param, "");
						if (!"".equals(value)) {
							checkin.put(key, value);
						}
					}
				}
			}
			Log.i(TAG,
					"About to create checkin from event: " + checkin.toString());
			addCheckin(checkin, null, false, 0, sendType == SendType.SendNow ? true : false);
		} catch (Exception e) {
		}
	}

	public void registerInC2DM() {
		Log.i(TAG, "Doing C2DM...");
		C2DMessaging.register(context.getContext(), C2DMReceiver.SERVICE);
	}

	public LocationController getLocationController() {
		return locationController;
	}
}
