package org.kvj.bravo7.bg;

import org.kvj.bravo7.AllTimeService;
import org.kvj.bravo7.AllTimeService.LocalBinder;
import org.kvj.bravo7.impl.Controller;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class ControllerConnector implements ServiceConnection {
	
	private static final String TAG = "ControllerConnector";
	private Activity activity = null;
	private ControllerReceiver receiver = null;
	private LocalBinder localBinder = null;
	
	public ControllerConnector(Activity activity, ControllerReceiver receiver) {
		this.activity = activity;
		this.receiver = receiver;
	}
	
	public interface ControllerReceiver {
		public void onController(Controller controller);
	}
	
    @Override
    public void onServiceDisconnected(ComponentName arg0) {
	    Log.i(TAG, "["+activity.getClass().getSimpleName()+"]service disconnected");
    	localBinder = null;
    	receiver.onController(null);
    }

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	    Log.i(TAG, "["+activity.getClass().getSimpleName()+"]service connected: "+service);
        localBinder = (LocalBinder) service;
        receiver.onController(getController());
	}
	
	public void connectController() {
	    Intent intent = new Intent(activity, AllTimeService.class);
//	    Log.i(TAG, "["+activity.getClass().getSimpleName()+"]Binding to service...");
        activity.bindService(intent, this, Context.BIND_AUTO_CREATE);
//	    Log.i(TAG, "["+activity.getClass().getSimpleName()+"]Binding to service done");
	}
	
	public void disconnectController() {
		if (localBinder != null) {
			activity.unbindService(this);
		}
	}
	
	public Controller getController() {
		if (localBinder == null) {
			return null;
		}
		return localBinder.getController();
	}
}
