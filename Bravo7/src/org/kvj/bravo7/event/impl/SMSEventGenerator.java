package org.kvj.bravo7.event.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.event.Event;
import org.kvj.bravo7.event.EventGenerator;
import org.kvj.bravo7.event.EventListener;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSEventGenerator extends BroadcastReceiver implements
		EventGenerator {

	private static final String TAG = "SMSEventGenerator";
	private EventListener listener = null;
	private ContentResolver resolver = null;

	public SMSEventGenerator(ApplicationContext context, EventListener listener) {
		this.listener = listener;
		IntentFilter filter = new IntentFilter(
				"android.provider.Telephony.SMS_RECEIVED");
		context.getContext().registerReceiver(this, filter);
		resolver = context.getContext().getContentResolver();
	}

	@Override
	public SendType shouldCreateCheckin(String hook, Event event,
			JSONObject params) {
		// Log.i(TAG, "SMS2: from: [" + event.getData().optString("message") +
		// "]");
		if (params.optBoolean("immediate", false)) {
			return SendType.SendNow;
		}
		return SendType.Send;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "SMS came...");
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			Object[] pdus = (Object[]) bundle.get("pdus");
			Map<String, JSONObject> smses = new LinkedHashMap<String, JSONObject>();
			for (int i = 0; i < pdus.length; i++) {
				SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdus[i]);
				String displayName = sms.getOriginatingAddress();
				try {
					Uri uri = Uri.withAppendedPath(
							PhoneLookup.CONTENT_FILTER_URI,
							Uri.encode(displayName));
					Cursor c = resolver.query(uri,
							new String[] { PhoneLookup.DISPLAY_NAME }, null,
							null, null);
					try {
						if (c.moveToFirst()) {
							displayName = c.getString(0);
						}
					} catch (Exception e) {
					} finally {
						c.close();
					}
					// Log.i(TAG,
					// "SMS: from[" + sms.getIndexOnIcc() + "]["
					// + sms.getIndexOnSim() + "]: ["
					// + sms.getOriginatingAddress() + "]["
					// + displayName + "]["
					// + sms.getPseudoSubject() + "]: ["
					// + sms.getMessageBody() + "]");

					JSONObject object = smses.get(sms.getOriginatingAddress());
					if (null == object) {
						object = new JSONObject();
						object.put("from", displayName);
						object.put("phone", sms.getOriginatingAddress());
						object.put("message", sms.getMessageBody());
						smses.put(sms.getOriginatingAddress(), object);
					} else {
						object.put("message", object.optString("message", "")
								+ sms.getMessageBody());
					}
				} catch (Exception e) {
				}
				for (JSONObject object : smses.values()) {
					listener.eventReceived(this, new Event("incoming_sms",
							object));
				}
			}
		}
	}

	@Override
	public boolean cron(Event event) {
		return false;
	}

}
