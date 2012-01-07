package org.kvj.bravo7.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.data.DataAccessService;
import org.kvj.bravo7.data.DataManageService;
import org.kvj.bravo7.data.impl.OAuthDataAccessImpl;
import org.kvj.bravo7.data.impl.OAuthDataManageImpl;
import org.kvj.bravo7.data.sql.CachingJSONStore;
import org.kvj.bravo7.data.sql.impl.CachingJSONStoreImpl;

import android.test.AndroidTestCase;

public class OAuthTest extends AndroidTestCase {

	private static final String HOST = "http://172.16.10.23:3000";
	private static final String TOKEN = "8043528979";
//	private static final String HOST = "http://133.139.237.95:3000";
//	private static final String TOKEN = "6096670752";
	private static String TAG = "TEST";
	
	
	
//	public void testLogin() {
//		DataAccessService accessService = 
//			new OAuthDataAccessImpl(HOST+"/login", HOST+"/token", 
//					"fb23dcdaff40f2bb", "9ea2f5f5bda2f8a6");
//		String token = accessService.getAccessToken("test", "testtest");
//		assertNotNull(token);
//		System.err.println("Token = "+token);
//	}
//	
	public void testOAuthLib() {
		try {
//			assertEquals("http://172.16.10.23:3000/login?type=user_agent&client_id=client", 
//					accessService.getLoginURL());
//			assertEquals("1304581612271", accessService.getAccessToken("783681"));
			DataManageService manageService = 
				new OAuthDataManageImpl(HOST, TOKEN);
			assertEquals("test", manageService.ping());
			List<JSONObject> templates = manageService.getTemplates();
			assertNotNull(templates);
			assertTrue(templates.size()>0);
			JSONObject template = templates.get(0);
//			System.err.println("Template template: "+template.get("template"));
			JSONObject checkin = new JSONObject();
			checkin.put("template", template.optString("id"));
			checkin.put("created", new Date().getTime());
			checkin.put("text", "Hi korea!");
			checkin = manageService.newCheckin(checkin);
			assertNotNull(checkin);
			assertNotNull(checkin.optString("id"));
			assertTrue(checkin.optString("id").length()>0);
			JSONObject checkin2 = new JSONObject();
			checkin2.put("template", template.optString("id"));
			checkin2.put("created", new Date().getTime());
			checkin2.put("text", "Hi china!");
			JSONArray refs = new JSONArray();
			refs.put(checkin.getString("id"));
			checkin2.put("refs", refs);
			checkin2 = manageService.newCheckin(checkin2);
			List<String> ids = new ArrayList<String>();
			ids.add(checkin.optString("id"));
			ids.add(checkin2.optString("id"));
			List<JSONObject> checkins = manageService.getCheckins(ids);
			assertNotNull(checkins);
			assertEquals(2, checkins.size());
			assertEquals("Hi korea!", checkins.get(0).optString("text"));
			assertEquals("Hi china!", checkins.get(1).optString("text"));
			checkin2 = checkins.get(1);
			JSONArray arr = checkin2.optJSONArray("refs");
			assertNotNull(arr);
			assertEquals(1, arr.length());
			assertEquals(checkin.optString("id"), arr.getJSONObject(0).optString("id"));
			JSONObject query = new JSONObject();
			query.put("field", "text");
			query.put("search", "hi");
//			assertEquals(2, manageService.getCheckins(query).size());
//			query.put("search", "korea");
//			assertEquals(1, manageService.getCheckins(query).size());
//			query.put("search", "china");
//			assertEquals(1, manageService.getCheckins(query).size());
//			query.put("search", "russia");
//			assertEquals(0, manageService.getCheckins(query).size());
			for (int i = 0; i < 1; i++) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				InputStream is = getClass().getResourceAsStream("test.file");
				int bytes = 0;
				int b = -1;
				while((b = is.read()) != -1) {
					outputStream.write(b);
					bytes++;
					if (bytes<10) {
						System.err.print(""+b+" ");
					}
				}
				System.err.println("-in");
				
				assertTrue(manageService.uploadFile(checkin.optString("id"), 
						"image", "image/jpeg", 
						new ByteArrayInputStream(outputStream.toByteArray()), 
						outputStream.size()));
				is = manageService.downloadFile(checkin.optString("id"), "image");
				assertNotNull(is);
				bytes = 0;
				while((b = is.read()) != -1) {
					outputStream.write(b);
					bytes++;
					if (bytes<10) {
						System.err.print(""+b+" ");
					}
				}
				System.err.println("-out");
				assertEquals(13663, bytes);
				is.close();
			}
			assertTrue(manageService.removeCheckin(checkin.optString("id")));
			assertTrue(manageService.removeCheckin(checkin2.optString("id")));
		} catch (Exception e) {
			e.printStackTrace();
			fail("No exceptions expected");
		}
	}

	public void testJSONStore() {
		String stream = "stream0";
		CachingJSONStore store = new CachingJSONStoreImpl(getContext(), "test5");
		assertTrue(store.clear(stream));
		assertEquals(0, store.getStream(stream, true).size());
		assertTrue(store.add(stream, new JSONObject(), null, true));
		assertTrue(store.add(stream, new JSONObject(), null, false));
		assertEquals(1, store.getStream(stream, true).size());
		assertEquals(1, store.getStream(stream, false).size());
		assertTrue(store.remove(stream, store.getStream(stream, true).get(0)));
		assertEquals(0, store.getStream(stream, true).size());
		assertTrue(store.clear(stream));
		assertEquals(0, store.getStream(stream, false).size());
	}
	
}
