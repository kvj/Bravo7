package org.kvj.bravo7.event.impl;

import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.event.Event;
import org.kvj.bravo7.event.EventGenerator;
import org.kvj.bravo7.event.EventListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryEventGenerator extends BroadcastReceiver implements
		EventGenerator {

	private static final String TAG = "BatteryEventGenerator";
	private EventListener listener = null;
	private int lastAnnounceLevel = -1;
	private int lastAnnounceState = -1;

	public BatteryEventGenerator(ApplicationContext context,
			EventListener listener) {
		this.listener = listener;
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		context.getContext().registerReceiver(this, filter);
	}

	@Override
	public SendType shouldCreateCheckin(String hook, Event event,
			JSONObject params) {
		int level = event.getData().optInt("level", lastAnnounceLevel);
		int state = event.getData().optInt("state", lastAnnounceState);
		int delta = Math.abs(lastAnnounceLevel - level);
		int deltaParam = params.optInt("delta", 200);
		int limitParam = params.optInt("limit", -1);
		if ((delta >= deltaParam)
				|| (state != lastAnnounceState)
				|| (lastAnnounceLevel < limitParam && level >= limitParam && state == 1)
				|| (lastAnnounceLevel > limitParam && level <= limitParam && state == 0)) {
			lastAnnounceLevel = level;
			lastAnnounceState = state;
			return SendType.Send;
		}
		return SendType.NotSend;
	}

	@Override
	public void onReceive(Context arg0, Intent intent) {
		int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		// int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
		int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
		int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		int state = 0;
		if (status == BatteryManager.BATTERY_STATUS_CHARGING
				|| status == BatteryManager.BATTERY_STATUS_FULL) {
			state = 1;
		}
		// Log.i(TAG, "Battery: "+level+", "+temp+", "+state);
		if (lastAnnounceLevel == -1) {
			lastAnnounceLevel = level;
		}
		if (lastAnnounceState == -1) {
			lastAnnounceState = state;
		}
		try {
			JSONObject object = new JSONObject();
			object.put("level", level);
			object.put("temp", temp);
			object.put("voltage", voltage);
			object.put("state", state);
			listener.eventReceived(this, new Event(
					state == 0 ? "battery_dcharge" : "battery_charge", object));
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	@Override
	public boolean cron(Event event) {
		return false;
	}

}
