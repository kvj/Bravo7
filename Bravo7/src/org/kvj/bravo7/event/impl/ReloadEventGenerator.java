package org.kvj.bravo7.event.impl;

import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.event.Event;
import org.kvj.bravo7.event.EventGenerator;
import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.impl.Controller.NetworkResult;
import org.kvj.bravo7.impl.Controller.RefreshType;

import android.util.Log;

public class ReloadEventGenerator implements EventGenerator {

	private Controller controller = null;
	
	public ReloadEventGenerator(ApplicationContext context, Controller controller) {
		this.controller = controller;
	}
	
	@Override
	public SendType shouldCreateCheckin(String hook, Event event,
			JSONObject params) {
		return SendType.NotSend;
	}

	@Override
	public boolean cron(Event event) {
		if ("reload".equals(event.getName())) {
			Log.i("ReloadEventGenerator", "Reloading everything by cron...");
			controller.doReload(RefreshType.Full);
			return true;
		}
		return false;
	}

}
