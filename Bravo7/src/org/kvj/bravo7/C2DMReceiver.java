package org.kvj.bravo7;

import java.io.IOException;

import org.kvj.bravo7.c2dm.C2DMBaseReceiver;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class C2DMReceiver extends C2DMBaseReceiver {

	private static final String TAG = "C2DMReceiver";
	public static final String SERVICE = "bravo7api@gmail.com";

	public C2DMReceiver() {
		super(SERVICE);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.i(TAG, "Message arrived");
	}

	@Override
	public void onError(Context context, String errorId) {
		Log.wtf(TAG, "onError: "+errorId);
	}
	
	@Override
	public void onRegistered(Context context, String registrationId)
			throws IOException {
		super.onRegistered(context, registrationId);
		Log.i(TAG, "Registered: "+registrationId);
	}

}
