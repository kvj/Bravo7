package org.kvj.bravo7.ui;

import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PrefActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(ApplicationContext.PREF_NAME);
		addPreferencesFromResource(R.xml.preferences);
	}
}
