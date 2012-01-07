package org.kvj.bravo7.data.impl;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.kvj.bravo7.data.DataAccessService;

import android.util.Log;

public class OAuthDataAccessImpl implements DataAccessService {

	private static String TAG = "OAuth2";
	
	private String loginURL = null;
	private String tokenURL = null;
	private String clientID = null;
	private String clientSecret = null;
	
	public OAuthDataAccessImpl(String loginURL, String tokenURL, 
			String clientID, String clientSecret) {
		this.loginURL = loginURL;
		this.tokenURL = tokenURL;
		this.clientID = clientID;
		this.clientSecret = clientSecret;
	}
	
	@Override
	public String getLoginURL() {
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(loginURL);
			if (loginURL.indexOf("?") != -1) {
				sb.append("&");
			} else {
				sb.append("?");
			}
			sb.append("type=user_agent&client_id=");
			sb.append(clientID);
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getAccessToken(String code, String username, String password) {
		try {
			boolean isUsernamePassword = (username != null && password != null);
			StringBuffer sb = new StringBuffer();
			if (isUsernamePassword) {
				sb.append(loginURL);
			} else {
				sb.append(tokenURL);
			}
			if (sb.indexOf("?") != -1) {
				sb.append("&");
			} else {
				sb.append("?");
			}
			sb.append("client_id=");
			sb.append(clientID);
			sb.append("&client_secret=");
			sb.append(clientSecret);
			if (isUsernamePassword) {
				sb.append("&type=username");
				sb.append("&username=");
				sb.append(username);
				sb.append("&password=");
				sb.append(password);
			} else {
				sb.append("&code=");
				sb.append(code);
			}
			Log.i(TAG, "Accessing "+sb);
			DefaultHttpClient client = new DefaultHttpClient();
			client.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
				@Override
				public boolean retryRequest(IOException exception, int executionCount,
						HttpContext context) {
					Log.i(TAG, "Retry? "+executionCount);
					return false;
				}
			});
			HttpGet request = new HttpGet(sb.toString());
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				List<NameValuePair> pairs = URLEncodedUtils.parse(entity);
				String accessToken = null;
				String error = null;
				for (NameValuePair p : pairs) {
					if ("error".equals(p.getName())) {
						error = p.getValue();
						break;
					}
					if ("access_token".equals(p.getName())) {
						accessToken = p.getValue();
						break;
					}
				}
				if (accessToken == null) {
					Log.e(TAG, "No access token in response: "+error);
					return null;
				}
				return accessToken;
			} else {
				Log.e(TAG, "No response HttpEntity");
			}
		} catch (Exception e) {
			Log.e(TAG, "Error accessing token", e);
		}
		return null;
	}

	@Override
	public String getAccessToken(String username, String password) {
		return getAccessToken(null, username, password);
	}

	@Override
	public String getAccessToken(String code) {
		return getAccessToken(code, null, null);
	}

}
