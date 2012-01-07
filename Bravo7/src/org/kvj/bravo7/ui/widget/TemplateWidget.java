package org.kvj.bravo7.ui.widget;

import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.R;
import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.ui.CheckinForm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class TemplateWidget extends AppWidgetProvider {

	private static final String TAG = "TemplateWidget";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		ApplicationContext ctx = ApplicationContext.getInstance();
		if (ctx == null) {
			Log.w(TAG, "Context hasn't started. Skipping");
			return;
		}
		Controller controller = ctx.getBean(Controller.class);
		for (int i : appWidgetIds) {
			// Log.i(TAG, "About to update "+i);
			JSONObject config = ctx.getWidgetConfig(i);
			// Log.i(TAG, "Config "+config+", controller: "+controller);
			if (config != null && controller != null) {
				JSONObject template = controller.getTemplate(config.optString(
						"template", "-"));
				// Log.i(TAG, "Template: "+template);
				if (template != null) {
					Intent intent = new Intent(context, CheckinForm.class);
					intent.putExtra("object", template.toString());
					PendingIntent pendingIntent = PendingIntent.getActivity(
							context, i, intent,
							PendingIntent.FLAG_CANCEL_CURRENT);
					RemoteViews views = new RemoteViews(
							context.getPackageName(), R.layout.template_widget);
					Bitmap icon = controller.getIcon(template.optString("icon",
							"-"));
					if (icon != null) {
						views.setImageViewBitmap(R.id.template_widget_icon,
								icon);
					}
					views.setOnClickPendingIntent(R.id.template_widget_pane,
							pendingIntent);
					String caption = config.optString("text", "");
					if (!"".equals(caption)) {
						views.setViewVisibility(R.id.template_widget_text,
								View.VISIBLE);
						views.setTextViewText(R.id.template_widget_text,
								caption);
					} else {
						views.setViewVisibility(R.id.template_widget_text,
								View.GONE);
					}
					appWidgetManager.updateAppWidget(i, views);
				}
			}
		}
	}
}
