package org.kvj.bravo7.data.sql;

import java.util.Map;

import org.json.JSONObject;

public interface CachingJSONStore {
	public Map<String, JSONObject> getStream(String stream, boolean existing);

	public boolean add(String stream, JSONObject object, byte[] blob,
			boolean existing);

	public JSONObject findByID(String stream, String id);

	public byte[] findBlobByObject(String stream, JSONObject object);

	public boolean clear(String stream);

	public boolean remove(String stream, JSONObject object);

	public String getHash(String stream, String id, String def);
}
