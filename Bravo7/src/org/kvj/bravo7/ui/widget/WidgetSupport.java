package org.kvj.bravo7.ui.widget;

import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.ui.SuperActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.EditText;

public class WidgetSupport {

	public interface AskResult {
		public String result(String value);
	}

	private static final String TAG = "WidgetSuport";

	private boolean widget = false;
	private int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
	private SuperActivity activity = null;

	public WidgetSupport(SuperActivity activity) {
		this.activity = activity;
		if (activity.getIntent() != null
				&& activity.getIntent().getExtras() != null) {
			widgetID = activity
					.getIntent()
					.getExtras()
					.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
							AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		if (widgetID != AppWidgetManager.INVALID_APPWIDGET_ID) {
			widget = true;
		}
	}

	public boolean isWidget() {
		return widget;
	}

	public void ask(String message, String value, final AskResult askResult) {
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);

		alert.setTitle(activity.getTitle());
		alert.setMessage(message);

		// Set an EditText view to get user input
		final EditText input = new EditText(activity);
		input.setSingleLine(true);
		input.setText(value);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String result = input.getText().toString().trim();
				askResult.result(result);
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});

		alert.show();
	}

	public void finish(JSONObject config) {
		try {
			ApplicationContext context = ApplicationContext
					.getInstance(activity);
			context.setWidgetConfig(widgetID, config);
			context.updateWidgets(widgetID);
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
			activity.setResult(Activity.RESULT_OK, resultValue);
			activity.finish();
		} catch (Exception e) {
			Log.e(TAG, "Error:", e);
			activity.notify("Error configuring widget");
		}
	}
}
