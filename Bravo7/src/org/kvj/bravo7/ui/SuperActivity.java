package org.kvj.bravo7.ui;

import org.kvj.bravo7.bg.ControllerConnector;
import org.kvj.bravo7.bg.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.impl.Controller;

import android.app.Activity;
import android.widget.Toast;

public class SuperActivity extends Activity implements ControllerReceiver {

	static final String TAG = "SuperActivity";
	protected Controller controller = null;
	ControllerConnector connector = new ControllerConnector(this, this);

	@Override
	public void onController(Controller controller) {
		// Log.i(TAG, "Activity["+getClass().getSimpleName()+"]: "+controller);
		this.controller = controller;
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Log.i(TAG, "Activity["+getClass().getSimpleName()+"]: starting");
		connector.connectController();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Log.i(TAG, "Activity["+getClass().getSimpleName()+"]: stopping");
		connector.disconnectController();
	}

	public void notify(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

}
