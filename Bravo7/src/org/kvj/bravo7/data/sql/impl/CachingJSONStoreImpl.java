package org.kvj.bravo7.data.sql.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.kvj.bravo7.data.sql.CachingJSONStore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class CachingJSONStoreImpl implements CachingJSONStore {

	private static final String DB_ID = "__DBID";
	private static final String TAG = "Store";
	DBHelper helper = null;

	public CachingJSONStoreImpl(Context context, String path) {
		helper = new DBHelper(context, path, 4) {

			@Override
			public void migrate(SQLiteDatabase db, int version) {
				switch (version) {
				case 1:
					db.execSQL("create table data (id integer primary key autoincrement, "
							+ "stream text, body text, existing integer default 1)");
					break;
				case 2:
					db.execSQL("alter table data add object_id text default \'\'");
					break;
				case 3:
					db.execSQL("alter table data add data blob");
					break;
				case 4:
					db.execSQL("alter table data add hash text");
					break;
				}
			}
		};
		if (!helper.open()) {
			helper = null;
		}
	}

	@Override
	public Map<String, JSONObject> getStream(String stream, boolean existing) {
		if (helper == null) {
			return null;
		}
		Cursor cursor = null;
		try {
			Map<String, JSONObject> result = new LinkedHashMap<String, JSONObject>();
			cursor = helper.getDatabase().query("data",
					new String[] { "id", "body", "object_id" },
					"stream=? and existing=?",
					new String[] { stream, existing ? "1" : "0" }, null, null,
					"id");
			while (cursor.moveToNext()) {
				JSONObject object = new JSONObject(cursor.getString(1));
				object.put(DB_ID, cursor.getInt(0));
				result.put(cursor.getString(2), object);
			}
			cursor.close();
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Error querying DB:", e);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return null;
	}

	@Override
	public boolean add(String stream, JSONObject object, byte[] data,
			boolean existing) {
		if (helper == null) {
			return false;
		}
		helper.getDatabase().beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("stream", stream);
			values.put("body", object.toString());
			values.put("existing", existing ? 1 : 0);
			values.put("object_id", object.optString("id"));
			values.put("hash", object.optString("hash"));
			if (data != null) {
				values.put("data", data);
			}
			helper.getDatabase().insert("data", null, values);
			helper.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error clearing DB:", e);
		} finally {
			helper.getDatabase().endTransaction();
		}
		return false;
	}

	@Override
	public boolean clear(String stream) {
		if (helper == null) {
			return false;
		}
		helper.getDatabase().beginTransaction();
		try {
			helper.getDatabase().delete("data", "stream=?",
					new String[] { stream });
			helper.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error clearing DB:", e);
		} finally {
			helper.getDatabase().endTransaction();
		}
		return false;
	}

	@Override
	public boolean remove(String stream, JSONObject object) {
		if (helper == null) {
			return false;
		}
		helper.getDatabase().beginTransaction();
		try {
			helper.getDatabase().delete("data", "stream=? and id=?",
					new String[] { stream, object.optString(DB_ID) });
			helper.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error clearing DB:", e);
		} finally {
			helper.getDatabase().endTransaction();
		}
		return false;
	}

	@Override
	public JSONObject findByID(String stream, String id) {
		if (helper == null) {
			return null;
		}
		Cursor cursor = null;
		try {
			JSONObject result = null;
			cursor = helper.getDatabase().query("data",
					new String[] { "id", "body" }, "stream=? and object_id=?",
					new String[] { stream, id }, null, null, "id");
			if (cursor.moveToNext()) {
				JSONObject object = new JSONObject(cursor.getString(1));
				object.put(DB_ID, cursor.getInt(0));
				result = object;
			}
			cursor.close();
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Error querying DB:", e);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return null;
	}

	@Override
	public byte[] findBlobByObject(String stream, JSONObject object) {
		if (helper == null) {
			return null;
		}
		Cursor cursor = null;
		try {
			byte[] result = null;
			cursor = helper.getDatabase().query("data",
					new String[] { "id", "data" }, "stream=? and id=?",
					new String[] { stream, object.optString(DB_ID, "") }, null,
					null, "id");
			if (cursor.moveToNext()) {
				byte[] data = cursor.getBlob(1);
				result = data;
			}
			cursor.close();
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Error querying DB:", e);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return null;
	}

	@Override
	public String getHash(String stream, String id, String def) {
		if (helper == null) {
			return def;
		}
		Cursor cursor = null;
		try {
			String result = def;
			cursor = helper.getDatabase().query("data",
					new String[] { "hash" }, "stream=? and object_id=?",
					new String[] { stream, id }, null, null, "id");
			if (cursor.moveToNext()) {
				result = cursor.getString(1);
				if (result == null || "".equals(result)) {
					result = def;
				}
			}
			cursor.close();
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Error querying DB:", e);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return def;
	}
}
