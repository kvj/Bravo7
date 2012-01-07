package org.kvj.bravo7.ui.widget;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.Constants;
import org.kvj.bravo7.MainScreen;
import org.kvj.bravo7.R;
import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.impl.TemplateEngine;
import org.kvj.bravo7.ui.CheckinForm;
import org.kvj.bravo7.ui.SearchCheckins;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;

public class CheckinWidget extends AppWidgetProvider {

	@Override
	public void onReceive(Context context, Intent intent) {
		int widget = intent.getIntExtra("widget", -1);
		if (widget != -1) {
			String type = intent.getStringExtra("type");
			// Log.i(TAG, "Got intent: " + widget + ", " + type);
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.checkin_widget);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);
			ApplicationContext ctx = ApplicationContext.getInstance();
			Controller controller = ctx.getBean(Controller.class);
			if ("toggle".equals(type)) {
				boolean visible = intent.getBooleanExtra("visible", false);
				Log.i(TAG, "Got intent: " + widget + ", " + type + ", "
						+ visible);
				Intent intent2 = new Intent(intent);
				intent2.putExtra("visible", !visible);
				views.setOnClickPendingIntent(R.id.checkin_w_menu_toggle,
						PendingIntent.getBroadcast(context, widget, intent2,
								PendingIntent.FLAG_CANCEL_CURRENT));
				views.setViewVisibility(R.id.checkin_w_menu,
						visible ? View.GONE : View.VISIBLE);
				if (!visible) {
					views.removeAllViews(R.id.checkin_w_menu);
					List<String> favs = ctx
							.getStringArrayPreference(Constants.TEMPLATE_FAVS);
					int index = 0;
					for (String favID : favs) {
						index++;
						JSONObject tmpl = controller.getTemplate(favID);
						if (null == tmpl) {
							continue;
						}
						RemoteViews item = new RemoteViews(
								context.getPackageName(),
								R.layout.checkin_widget_menu_item);
						Bitmap icon = controller.getIcon(
								tmpl.optString("icon", "-"), 2);
						if (icon != null) {
							item.setImageViewBitmap(R.id.checkin_w_menu_item, icon);
						}
						Intent startAdd = new Intent(context, CheckinForm.class);
						startAdd.putExtra("update_widget", widget);
						startAdd.putExtra("object", tmpl.toString());
						startAdd.putExtra("attach_to",
								intent.getStringExtra("id"));
						item.setOnClickPendingIntent(R.id.checkin_w_menu_item,
								PendingIntent.getActivity(context, widget
										+ index, startAdd,
										PendingIntent.FLAG_CANCEL_CURRENT));
						views.addView(R.id.checkin_w_menu, item);
					}
				}
			}
			manager.updateAppWidget(widget, views);
		}
		super.onReceive(context, intent);
	}

	class CheckinLine {
		String display;
		JSONObject data;
		String icon;
		int padding = 0;
	}

	private static final String TAG = "CheckinWidget";

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
		for (int wID : appWidgetIds) {
			JSONObject config = ctx.getWidgetConfig(wID);
			List<CheckinLine> lines = new ArrayList<CheckinWidget.CheckinLine>();
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.checkin_widget);
			views.removeAllViews(R.id.checkin_w_content);
			views.setViewVisibility(R.id.checkin_w_menu, View.GONE);
			if (config != null && controller != null) {
				List<JSONObject> checkins = new ArrayList<JSONObject>();
				// Log.i(TAG, "Reload template: "+config);
				JSONObject checkin = null;
				PendingIntent pendingIntent = null;
				if (config.has("checkin")) {
					checkin = controller.getCheckin(config.optString("checkin",
							""));
					if (null != checkin) {
						Intent intent = new Intent(context,
								SearchCheckins.class);
						intent.putExtra("show_local",
								checkin.optString("id", ""));
						pendingIntent = PendingIntent.getActivity(context, wID,
								intent, PendingIntent.FLAG_CANCEL_CURRENT);
					}
				}
				if (config.has("checkin_body")) {
					checkin = config.optJSONObject("checkin_body");
					if (null != checkin) {
						Intent intent = new Intent(context, MainScreen.class);
						pendingIntent = PendingIntent.getActivity(context, wID,
								intent, PendingIntent.FLAG_CANCEL_CURRENT);
					}
				}
				if (checkin != null) {
					checkins.add(checkin);
					checkins.addAll(controller.getChildren(checkin));
					if (pendingIntent != null) {
						views.setOnClickPendingIntent(R.id.checkin_w_root,
								pendingIntent);
					}
					Intent toggleMenu = new Intent(context, CheckinWidget.class);
					toggleMenu.putExtra("widget", wID);
					toggleMenu.putExtra("id", checkin.optString("id", ""));
					toggleMenu.putExtra("type", "toggle");
					views.setOnClickPendingIntent(R.id.checkin_w_menu_toggle,
							PendingIntent.getBroadcast(context, wID,
									toggleMenu,
									PendingIntent.FLAG_CANCEL_CURRENT));
				}
				for (int i = 0; i < checkins.size(); i++) {
					checkin = checkins.get(i);
					JSONObject template = controller.getTemplate(checkin
							.optString("template", "-"));
					if (template == null) {
						Log.w(TAG,
								"Reload template no template: "
										+ checkin.optString("template", "-"));
						continue;
					}
					CheckinLine line = new CheckinLine();
					line.data = checkin;
					line.icon = template.optString("icon", "-");
					line.display = template.optString("display", "");
					if ("".equals(line.display)) {
						line.display = "{text}";
					}
					line.padding = i == 0 ? 0 : 1;
					lines.add(line);
				}
			}
			// Log.i(TAG, "Found lines: "+lines.size());
			for (int i = 0; i < lines.size(); i++) {
				CheckinLine line = lines.get(i);
				RemoteViews item = new RemoteViews(context.getPackageName(),
						R.layout.checkin_widget_item);
				StringBuilder padding = new StringBuilder();
				for (int j = 0; j < line.padding; j++) {
					padding.append("  ");
				}
				item.setTextViewText(R.id.checkin_w_item_padding,
						padding.toString());
				Bitmap icon = controller.getIcon(line.icon, 2);
				if (icon != null) {
					item.setImageViewBitmap(R.id.checkin_w_item_icon, icon);
				}
				item.setTextViewText(R.id.checkin_w_item_text,
						TemplateEngine.apply(line.display, line.data));
				views.addView(R.id.checkin_w_content, item);
				// Log.i(TAG, "Added line: "+line.data);
			}
			appWidgetManager.updateAppWidget(wID, views);
		}
	}
}
