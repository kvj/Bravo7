package org.kvj.bravo7.event;

import org.json.JSONObject;

public interface EventGenerator {
	public enum SendType {NotSend, Send, SendNow};
	public SendType shouldCreateCheckin(String hook, Event event, JSONObject params);
	public boolean cron(Event event);
}
