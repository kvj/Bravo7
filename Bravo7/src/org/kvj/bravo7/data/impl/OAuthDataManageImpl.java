package org.kvj.bravo7.data.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.data.DataManageService;

import android.util.Log;

public class OAuthDataManageImpl implements DataManageService {

	class StreamEntity implements HttpEntity {

		private InputStream stream;
		private int size;
		private String contentType;

		public StreamEntity(InputStream stream, int size, String contentType) {
			this.stream = stream;
			this.size = size;
			this.contentType = contentType;
		}

		@Override
		public void consumeContent() throws IOException {
			Log.i(TAG, "consumeContent()");
		}

		@Override
		public InputStream getContent() throws IOException,
				IllegalStateException {
			Log.i(TAG, "getContent()");
			return stream;
		}

		@Override
		public Header getContentEncoding() {
			Log.i(TAG, "getContentEncoding()");
			return null;
		}

		@Override
		public long getContentLength() {
			Log.i(TAG, "getContentLength() = " + size);
			return size;
		}

		@Override
		public Header getContentType() {
			Log.i(TAG, "getContentType() = " + contentType);
			return new BasicHeader("Content-Type", contentType);
		}

		@Override
		public boolean isChunked() {
			Log.i(TAG, "isChunked()");
			return false;
		}

		@Override
		public boolean isRepeatable() {
			Log.i(TAG, "isRepeatable()");
			return false;
		}

		@Override
		public boolean isStreaming() {
			Log.i(TAG, "isStreaming()");
			return true;
		}

		@Override
		public void writeTo(OutputStream outstream) throws IOException {
			Log.i(TAG, "writeTo(): " + size);
			byte[] buffer = new byte[4096];
			int read = -1;
			int bytesTotal = 0;
			FileOutputStream fos = new FileOutputStream("/sdcard/tmp.image");
			while ((read = stream.read(buffer)) > 0) {
				outstream.write(buffer, 0, read);
				fos.write(buffer, 0, read);
				bytesTotal += read;
				outstream.flush();
				fos.flush();
			}
			Log.i(TAG, "Written bytes: " + bytesTotal);
			stream.close();
			fos.close();
		}

	}

	private enum RequestType {
		get, post, delete
	};

	private String rootURL = null;
	private String accessToken = null;

	private static String TAG = "OAuth2";

	public OAuthDataManageImpl(String rootURL, String accessToken) {
		this.rootURL = rootURL;
		this.accessToken = accessToken;
	}

	private JSONObject makeRequest(String path, RequestType type,
			List<NameValuePair> params) {
		return makeRequest(path, type, params, null);
	}

	private JSONObject makeRequest(String path, RequestType type,
			List<NameValuePair> params, StreamEntity entity) {
		StringBuffer sb = new StringBuffer();
		sb.append(rootURL);
		sb.append(path);
		if (rootURL.indexOf("?") != -1 || path.indexOf("?") != -1) {
			sb.append("&");
		} else {
			sb.append("?");
		}
		sb.append("access_token=");
		sb.append(accessToken);
		Log.i(TAG, "Accessing " + sb);
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			client.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
				@Override
				public boolean retryRequest(IOException exception,
						int executionCount, HttpContext context) {
					Log.i(TAG, "Retry? " + executionCount);
					return false;
				}
			});
			HttpRequestBase request = null;
			switch (type) {
			case post:
				HttpPost httpPost = new HttpPost(sb.toString());
				if (entity != null) {
					httpPost.setEntity(entity);
				}
				if (params != null && params.size() > 0) {
					httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
				}
				request = httpPost;
				break;
			case delete:
				if (params != null && params.size() > 0) {
					sb.append("&");
					// Log.i(TAG, "URLEncoder: "+URLEncodedUtils.format(params,
					// "utf-8"));
					sb.append(URLEncodedUtils.format(params, "utf-8"));
				}
				request = new HttpDelete(sb.toString());
				break;
			default:
				request = new HttpGet(sb.toString());
			}
			HttpResponse response = client.execute(request);
			// Log.i(TAG,
			// "Response: "+response.getStatusLine().getStatusCode());
			HttpEntity ent = response.getEntity();
			JSONObject obj = new JSONObject(EntityUtils.toString(ent, "utf-8"));
			if (obj.optString("error").length() > 0) {
				// Have error
				Log.e(TAG, "Error from server: " + obj.optString("error"));
				return null;
			}
			return obj;
		} catch (Exception e) {
			Log.e(TAG, "HTTP error:", e);
		}
		return null;
	}

	@Override
	public String ping() {
		JSONObject object = makeRequest("/ping", RequestType.get, null);
		if (object != null) {
			return object.optString("login");
		}
		return null;
	}

	@Override
	public List<JSONObject> getTemplates() {
		JSONObject object = makeRequest("/templates", RequestType.post, null);
		if (object != null) {
			JSONArray array = object.optJSONArray("array");
			List<JSONObject> result = new ArrayList<JSONObject>();
			try {
				for (int i = 0; i < array.length(); i++) {
					result.add(array.getJSONObject(i));
				}
				return result;
			} catch (Exception e) {
			}
		}
		return null;
	}

	private List<JSONObject> processCheckins(JSONArray array, boolean alsoRefs) {
		List<JSONObject> result = new ArrayList<JSONObject>();
		try {
			for (int i = 0; i < array.length(); i++) {
				JSONObject ch = array.getJSONObject(i);
				JSONArray arr = ch.optJSONArray("refs");
				// Log.i(TAG,
				// "processCheckins: "+ch.optString("text")+", "+arr+", "+alsoRefs);
				if (!alsoRefs) {
					ch.put("refs", new JSONArray());
					result.add(array.getJSONObject(i));
					continue;
				}
				if (arr != null && arr.length() > 0) {
					List<String> refIDs = new ArrayList<String>();
					for (int j = 0; j < arr.length(); j++) {
						refIDs.add(arr.optString(j));
					}
					List<JSONObject> refs = getCheckins(refIDs,
							new ArrayList(), ch.optString("sort", null), false);
					if (refs == null) {
						return null;
					}
					ch.put("refs", new JSONArray(refs));
				}
				result.add(array.getJSONObject(i));
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<JSONObject> getCheckins(List<String> ids, List<String> hashes) {
		return getCheckins(ids, hashes, null, true);
	}

	public List<JSONObject> getCheckins(List<String> ids, List<String> hashes,
			String sort, boolean alsoRefs) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (String id : ids) {
			pairs.add(new BasicNameValuePair("ids", id));
		}
		for (String id : hashes) {
			pairs.add(new BasicNameValuePair("hashes", id));
		}
		if (null != sort) {
			pairs.add(new BasicNameValuePair("sort", sort));
		}
		JSONObject object = makeRequest("/checkins", RequestType.post, pairs);
		if (object != null) {
			JSONArray array = object.optJSONArray("array");
			return processCheckins(array, alsoRefs);
		}
		return null;
	}

	@Override
	public JSONObject newCheckin(JSONObject data) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		Iterator<String> keys = data.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			if ("refs".equals(key)) {
				JSONArray arr = data.optJSONArray("refs");
				if (arr == null) {
					arr = new JSONArray();
					arr.put(data.optString("refs"));
				}
				for (int i = 0; i < arr.length(); i++) {
					pairs.add(new BasicNameValuePair(key, arr.optString(i)));
				}
				continue;
			}
			pairs.add(new BasicNameValuePair(key, data.optString(key)));
		}
		JSONObject result = makeRequest("/checkin", RequestType.post, pairs);
		return result;
	}

	@Override
	public boolean removeCheckin(String id) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("id", id));
		JSONObject result = makeRequest("/checkin", RequestType.delete, pairs);
		System.err.println("Remove: " + result);
		return result != null;
	}

	@Override
	public List<JSONObject> getCheckins(JSONObject query) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		if (query.has("from") && query.has("to")) {
			pairs.add(new BasicNameValuePair("from", query
					.optString("from", "")));
			pairs.add(new BasicNameValuePair("to", query.optString("to", "")));
		}
		pairs.add(new BasicNameValuePair("query", query.toString()));
		if (query.has("sort")) {
			pairs.add(new BasicNameValuePair("sort", query
					.optString("sort", "")));
		}
		JSONObject object = makeRequest("/checkins", RequestType.post, pairs);
		if (object != null) {
			JSONArray array = object.optJSONArray("array");
			return processCheckins(array, true);
		}
		return null;
	}

	@Override
	public List<JSONObject> getHooks(int cronDays) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("type", "cell"));
		pairs.add(new BasicNameValuePair("cronDays", Integer.toString(cronDays)));
		JSONObject object = makeRequest("/hooks", RequestType.post, pairs);
		if (object != null) {
			JSONArray array = object.optJSONArray("array");
			List<JSONObject> result = new ArrayList<JSONObject>();
			try {
				for (int i = 0; i < array.length(); i++) {
					result.add(array.getJSONObject(i));
				}
				return result;
			} catch (Exception e) {
			}
		}
		return null;
	}

	@Override
	public boolean uploadFile(String id, String field, String ctype,
			InputStream stream, int size) {
		Log.i(TAG, "Uploading: " + field + ", " + ctype + ", " + size);
		StreamEntity entity = new StreamEntity(stream, size, ctype);
		JSONObject object = makeRequest("/upload/" + id + "/" + field + "/"
				+ size, RequestType.post, null, entity);
		if (object != null) {
			String error = object.optString("error", "");
			return "".equals(error);
		}
		return false;
	}

	@Override
	public InputStream downloadFile(String id, String field) {
		StringBuffer sb = new StringBuffer();
		sb.append(rootURL);
		sb.append("/download/" + id + "/" + field);
		sb.append("?");
		sb.append("access_token=");
		sb.append(accessToken);
		Log.i(TAG, "Downloading " + sb);
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			client.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
				@Override
				public boolean retryRequest(IOException exception,
						int executionCount, HttpContext context) {
					Log.i(TAG, "Retry? " + executionCount);
					return false;
				}
			});
			HttpRequestBase request = null;
			request = new HttpGet(sb.toString());
			HttpResponse response = client.execute(request);
			HttpEntity ent = response.getEntity();
			return ent.getContent();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
