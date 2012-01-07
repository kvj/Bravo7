package org.kvj.bravo7.bg;

import org.kvj.bravo7.AllTimeService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(context, AllTimeService.class);
		context.startService(i);
	}

}
