package org.kvj.bravo7.ui;

import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.Constants;
import org.kvj.bravo7.R;
import org.kvj.bravo7.bg.ControllerConnector;
import org.kvj.bravo7.bg.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.impl.Controller.NetworkResult;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginDialog extends SuperActivity {

	private Button loginButton;
	private EditText url, login, password;
	ApplicationContext context;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = ApplicationContext.getInstance(this);
		setContentView(R.layout.login);
		loginButton = (Button) findViewById(R.id.login_button);
		url = (EditText) findViewById(R.id.login_url);
		login = (EditText) findViewById(R.id.login_login);
		password = (EditText) findViewById(R.id.login_password);
		url.setText(context.getStringPreference(Constants.URL_KEY, "http://kvj.me/delta3"));
		loginButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (controller != null) {
					controller.fetchToken(url.getText().toString(), 
							login.getText().toString(), 
							password.getText().toString(), 
							new NetworkResult() {
								
								@Override
								public void onResult(Object result) {
									if (result != null) {
										LoginDialog.this.notify("Token result: "+result);
									} else {
										finish();
									}
								}
							});
				} else {
					LoginDialog.this.notify("No connection");
				}
			}
		});
	}

}
