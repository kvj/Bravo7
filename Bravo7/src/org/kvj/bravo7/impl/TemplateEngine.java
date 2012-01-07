package org.kvj.bravo7.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class TemplateEngine {

	public static String apply(String display, JSONObject data) {
		return apply(display, data, "");
	}

	public static String apply(String display, JSONObject data, String def) {
		Pattern pattern = Pattern.compile("\\{[a-z_]+\\}");
		Matcher m = pattern.matcher(display);
		StringBuffer result = new StringBuffer();
		while (m.find()) {
			String found = m.group();
			m.appendReplacement(result, data.optString(
					m.group().substring(1, found.length() - 1), ""));
		}
		m.appendTail(result);
		return result.length() > 0 ? result.toString() : def;
	}
}
