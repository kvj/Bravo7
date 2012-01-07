package org.kvj.bravo7.data;

import java.io.InputStream;
import java.util.List;

import org.json.JSONObject;

public interface DataManageService {

	public String ping();

	public List<JSONObject> getTemplates();

	public List<JSONObject> getCheckins(List<String> ids, List<String> hashes);

	public List<JSONObject> getCheckins(JSONObject query);

	public JSONObject newCheckin(JSONObject data);

	public boolean removeCheckin(String id);

	public List<JSONObject> getHooks(int cronDays);

	public boolean uploadFile(String id, String field, String ctype,
			InputStream stream, int size);

	public InputStream downloadFile(String id, String field);
}
