package org.kvj.bravo7.event.impl;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.event.Event;
import org.kvj.bravo7.event.EventGenerator;
import org.kvj.bravo7.event.EventGenerator.SendType;
import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.ui.widget.CheckinWidget;

import android.util.Log;

public class CheckinLoaderEventGenerator implements EventGenerator {

	private Controller controller = null;
	private ApplicationContext context = null;
	
	public CheckinLoaderEventGenerator(ApplicationContext context, Controller controller) {
		this.controller = controller;
		this.context = context;
	}
	
	@Override
	public SendType shouldCreateCheckin(String hook, Event event,
			JSONObject params) {
		return SendType.NotSend;
	}

	@Override
	public boolean cron(Event event) {
		if ("load_checkin".equals(event.getName())) {
			Log.i("CheckinLoaderEventGenerator", "Loading checkin "+
					event.getData().optString("checkin", "-")+" to "+
					event.getData().optString("name"));
			Map<Integer, JSONObject> configs = context.getWidgetConfigs(CheckinWidget.class.getName());
			for (Integer wID : configs.keySet()) {
				JSONObject conf = configs.get(wID);
				if (conf.optString("name", "").equals(event.getData().optString("name"))) {
					try {
						conf.remove("checkin_body");
						conf.remove("checkin");
						conf.put("checkin", event.getData().optString("checkin", "-"));
						context.setWidgetConfig(wID, conf);
						context.updateWidgets(wID);
						break;
					} catch (JSONException e) {
					}
				}
			}
			return true;
		}
		return false;
	}

}
