package org.kvj.bravo7.event;

import org.json.JSONObject;

public class Event {
	private String name;
	private JSONObject data;
	
	public Event(String name, JSONObject data) {
		this.name = name;
		this.data = data;
	}
	
	public String getName() {
		return name;
	}
	
	public JSONObject getData() {
		return data;
	}
}
