package org.kvj.bravo7;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.impl.Controller.RefreshType;
import org.kvj.bravo7.impl.Controller.State;
import org.kvj.bravo7.impl.Controller.UpdateMonitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AllTimeService extends Service implements UpdateMonitor {

	SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");

	public class LocalBinder extends Binder {

		public Controller getController() {
			return controller;
		}
	}

	private static final String TAG = "Bravo7";
	private static final int STATE_NOTIFY_ID = 1;

	private ApplicationContext context = null;
	private final IBinder binder = new LocalBinder();
	private Controller controller = null;
	private NotificationManager notificationManager;
	private Notification stateNotification;
	private PendingIntent mainIntent;
	private Handler handler = null;
	private UpdateHandler currentUpdateHandler = null;

	private class UpdateHandler implements Runnable {

		private RefreshType refreshType = null;

		public UpdateHandler(RefreshType refreshType) {
			this.refreshType = refreshType;
		}

		@Override
		public void run() {
			context.log("Scheduled update: " + refreshType);
			controller.doReload(refreshType);
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
		context = ApplicationContext.getInstance(this);
		handler = new Handler();
		context.log("Service has been started");
		controller = new Controller(context);
		context.publishBean(controller);
		String ns = Context.NOTIFICATION_SERVICE;
		notificationManager = (NotificationManager) getSystemService(ns);
		stateNotification = new Notification(getIconByState(State.NotUpdated),
				"Starting...", System.currentTimeMillis());
		stateNotification.flags |= Notification.FLAG_NO_CLEAR
				| Notification.FLAG_ONGOING_EVENT;
		Intent mainScreen = new Intent(this, MainScreen.class);
		mainIntent = PendingIntent.getActivity(this, 0, mainScreen, 0);
		controller.addUpdateMonitor(this);
		Log.i(TAG, "Service created");
		notify("Service has been started");
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "Service bound");
		return binder;
	}

	private void notify(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Service destroyed");
		notify("Service has been stopped");
		notificationManager.cancel(STATE_NOTIFY_ID);
		if (currentUpdateHandler != null) {
			handler.removeCallbacks(currentUpdateHandler);
		}
		super.onDestroy();
	}

	@Override
	public void stateChanged(final State state, final String error) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				stateNotification.icon = getIconByState(state);
				stateNotification.when = System.currentTimeMillis();
				String title = "Bravo7";
				String text = error;
				Date dt = scheduleUpdate(state);
				if (text == null) {
					text = "Status OK";
				}
				if (state != State.Updating) {
					text += " [" + dateFormat.format(dt) + "]";
				}
				stateNotification.setLatestEventInfo(AllTimeService.this,
						title, text, mainIntent);
				notificationManager.notify(STATE_NOTIFY_ID, stateNotification);
			}
		});
	}

	private int getIconByState(State state) {
		boolean inLocation = controller.getLocationController().isBusy();
		if (inLocation) {
			return R.drawable.status_location;
		}
		switch (state) {
		case NotUpdated:
		case Startup:
			return R.drawable.status_need_refresh;
		case UpdatedError:
			return R.drawable.status_error;
		case Error:
			return R.drawable.status_error;
		case Updating:
			return R.drawable.status_refreshing;
		}
		return R.drawable.icon;
	}

	private Date scheduleUpdate(State state) {
		if (currentUpdateHandler != null) {
			handler.removeCallbacks(currentUpdateHandler);
		}
		int delay = context.getIntPreference("refresh_delay",
				R.string.refresh_delay);
		RefreshType refreshType = RefreshType.UpdateCheckins;
		switch (state) {
		case NotUpdated:
			refreshType = RefreshType.SendCheckins;
		case Startup:
			delay = context.getIntPreference("send_delay", R.string.send_delay);
			break;
		case UpdatedError:
		case Error:
			delay = context.getIntPreference("error_delay",
					R.string.error_delay);
			break;
		}
		Log.i(TAG, "Next update will be in " + delay + " mins");
		currentUpdateHandler = new UpdateHandler(refreshType);
		handler.postDelayed(currentUpdateHandler, delay * 60 * 1000);
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MINUTE, delay);
		return c.getTime();
	}

	@Override
	public void foregroundStateChanged(boolean foreground) {
		if (foreground) {
			startForeground(STATE_NOTIFY_ID, stateNotification);
		} else {
			stopForeground(false);
		}
	}

}
