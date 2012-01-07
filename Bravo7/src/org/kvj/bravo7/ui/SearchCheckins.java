package org.kvj.bravo7.ui;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.MainScreen;
import org.kvj.bravo7.R;
import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.impl.Controller.NetworkResult;
import org.kvj.bravo7.ui.SearchCheckins.CheckinProvider.BindType;
import org.kvj.bravo7.ui.adapters.CheckinListAdapter;
import org.kvj.bravo7.ui.widget.CheckinWidget;
import org.kvj.bravo7.ui.widget.WidgetSupport;
import org.kvj.bravo7.ui.widget.WidgetSupport.AskResult;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;

public class SearchCheckins extends SuperActivity {

	public static abstract class CheckinProvider implements Serializable {

		public enum BindType {
			Local, Remote
		};

		abstract public boolean showSearch();

		abstract public boolean showFavorite();

		abstract public boolean showRemove();

		abstract public String getTitle();

		abstract public void reload(SuperActivity ctx, Controller controller,
				CheckinListAdapter adapter, boolean modified);

		public boolean longClick(JSONObject object, boolean group) {
			return false;
		}

		public BindType showBind(JSONObject object) {
			return BindType.Remote;
		}

		public boolean showCreateWith() {
			return true;
		}

		public boolean showCreateChild() {
			return true;
		}

	}

	private static final String TAG = "SearchCheckins";
	ExpandableListView list = null;
	CheckinListAdapter adapter = null;
	Button searchButton = null;
	EditText editText = null;
	CheckinProvider provider = null;
	WidgetSupport widget = null;
	String fromWidgetID = null;
	private Date currentDate = new Date();
	SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy");
	ApplicationContext context = null;

	private void setupFavHandler(MenuItem item, final JSONObject object) {
		final String id = object.optString("id");
		final boolean nowFavorite = controller.isFavoriteCheckin(id);
		if (nowFavorite) {
			item.setTitle("Unmark as favorite");
		} else {
			item.setTitle("Mark as favorite");
		}
		item.setVisible(provider.showFavorite());
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				controller.favoriteCheckin(id, !nowFavorite);
				provider.reload(SearchCheckins.this, controller, adapter, true);
				return true;
			}
		});
	}

	private void setupRemoveHandler(MenuItem item, final JSONObject object) {
		item.setVisible(provider.showRemove());
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				controller.removeUnsentCheckin(object);
				provider.reload(SearchCheckins.this, controller, adapter, true);
				return true;
			}
		});
	}

	private void setupCreateWith(MenuItem item, final JSONObject object) {
		item.setVisible(provider.showCreateWith());
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(SearchCheckins.this,
						MainScreen.class);
				intent.putExtra("ref", object.optString("id", ""));
				startActivity(intent);
				return true;
			}
		});
	}

	private void setupCreateChild(MenuItem item, final JSONObject object) {
		item.setVisible(provider.showCreateChild());
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(SearchCheckins.this,
						MainScreen.class);
				intent.putExtra("attach_to", object.optString("id", ""));
				startActivity(intent);
				return true;
			}
		});
	}

	private void setupBindHandler(MenuItem item, final JSONObject object) {
		// item.setVisible(provider.showBind(object));
		final Map<Integer, JSONObject> configs = context
				.getWidgetConfigs(CheckinWidget.class.getName());
		final List<Integer> widgetIDs = new ArrayList<Integer>();
		for (Integer widgetID : configs.keySet()) {
			JSONObject conf = configs.get(widgetID);
			// Log.i(TAG, "Config: "+conf);
			if (!"".equals(conf.optString("name", ""))) {
				widgetIDs.add(widgetID);
			}
		}
		Log.i(TAG, "Bind: " + configs.size() + ", " + widgetIDs.size() + ", "
				+ provider.showBind(object));
		if (widgetIDs.size() == 0) {
			item.setVisible(false);
			return;
		}
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder alert = new AlertDialog.Builder(
						SearchCheckins.this);

				alert.setTitle("Select widget:");
				CharSequence[] items = new CharSequence[widgetIDs.size()];
				for (int i = 0; i < widgetIDs.size(); i++) {
					JSONObject config = configs.get(widgetIDs.get(i));
					items[i] = config.optString("name", "");
				}
				alert.setItems(items, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						JSONObject config = configs.get(widgetIDs.get(which));
						if (config != null) {
							config.remove("checkin_body");
							config.remove("checkin");
							try {
								if (provider.showBind(object) == BindType.Local) {
									config.put("checkin",
											object.optString("id", "-"));
								} else {
									config.put("checkin_body", object);
								}
								context.setWidgetConfig(widgetIDs.get(which),
										config);
								context.updateWidgets(widgetIDs.get(which));
							} catch (JSONException e) {
							}
						}
					}
				});
				alert.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						});

				alert.show();
				return true;
			}
		});
	}

	private void setupWidget() {
		widget = new WidgetSupport(this);
		if (widget.isWidget()) {
			ApplicationContext.getInstance(this).publishBean("checkinProvider",
					new CheckinProvider() {

						@Override
						public boolean showSearch() {
							return false;
						}

						@Override
						public boolean showRemove() {
							return false;
						}

						@Override
						public boolean showFavorite() {
							return false;
						}

						@Override
						public void reload(SuperActivity ctx,
								Controller controller,
								CheckinListAdapter adapter, boolean modified) {
							adapter.setData(controller.getCheckins());
						}

						@Override
						public String getTitle() {
							return "Select checkin to display";
						}

						@Override
						public boolean longClick(final JSONObject object,
								final boolean group) {
							widget.ask("Widget name?", "", new AskResult() {
								@Override
								public String result(String value) {
									JSONObject config = new JSONObject();
									try {
										if (group) {
											config.putOpt("checkin",
													object.optString("id", ""));
										} else {
											config.putOpt("checkin_body",
													object);
										}
										config.putOpt("name", value);
										widget.finish(config);
									} catch (JSONException e) {
									}
									return null;
								}
							});
							return true;
						}
					});
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = ApplicationContext.getInstance();
		setContentView(R.layout.search);
		setupWidget();
		list = (ExpandableListView) findViewById(R.id.search_list);
		list.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				boolean group = adapter.isContextMenuGroup(menuInfo);
				JSONObject object = (JSONObject) adapter
						.getContextMenuObject(menuInfo);
				if (object == null) {
					return;
				}
				if (provider.longClick(object, group)) {
					return;
				}
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.checkin_menu, menu);
				setupFavHandler(menu.getItem(0), object);
				setupRemoveHandler(menu.getItem(1), object);
				setupBindHandler(menu.getItem(2), object);
				setupCreateChild(menu.getItem(3), object);
				setupCreateWith(menu.getItem(4), object);
			}
		});
		editText = (EditText) findViewById(R.id.search_text);
		searchButton = (Button) findViewById(R.id.search_button);
		fromWidgetID = getIntent().getStringExtra("show_local");
		if (null != fromWidgetID) {
			provider = MainScreen.createFavProvider(fromWidgetID);
		} else {
			provider = ApplicationContext.getInstance(this).getBean(
					"checkinProvider", CheckinProvider.class);
		}
		setTitle(provider.getTitle());
		if (!provider.showSearch()) {
			editText.setVisibility(View.GONE);
			searchButton.setVisibility(View.GONE);
		} else {
			searchButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					String query = editText.getText().toString().trim()
							.toLowerCase();
					doSearch(query, null, null);
				}
			});
		}
	}

	private void doSearch(String query, Date from, Date to) {
		JSONObject q = new JSONObject();
		try {
			if (query != null) {
				if ("".equals(query)) {
					notify("No query provided");
					return;
				}
				q.putOpt("field", "text");
				q.putOpt("search", query);
				q.putOpt("sort", "!created");
				setTitle(provider.getTitle());
			} else if (from != null && to != null) {
				q.putOpt("from", from.getTime());
				q.putOpt("to", to.getTime());
				q.putOpt("sort", "created");
			} else {
				notify("Invalid query");
				return;
			}
		} catch (JSONException e) {
		}
		// Log.i(TAG, "Searching: "+q);
		final ProgressDialog dialog = ProgressDialog.show(this, "Search",
				"Searching...");
		controller.search(q, new NetworkResult() {

			@Override
			public void onResult(Object result) {
				dialog.dismiss();
				if (result instanceof String) {
					SearchCheckins.this.notify((String) result);
				} else {
					adapter.setData((List<JSONObject>) result);
				}
			}

		});
	}

	@Override
	public void onController(Controller controller) {
		super.onController(controller);
		if (controller != null && adapter == null) {
			adapter = new CheckinListAdapter(controller);
			list.setAdapter(adapter);
			provider.reload(this, controller, adapter, false);
			if (adapter.getGroupCount() > 0 && null != fromWidgetID) {
				list.expandGroup(0);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!provider.showSearch()) {
			return false;
		}
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Calendar c = Calendar.getInstance();
		c.setTime(currentDate);
		switch (item.getItemId()) {
		case R.id.search_day_left:
			c.add(Calendar.DAY_OF_YEAR, -1);
			break;
		case R.id.search_day_right:
			c.add(Calendar.DAY_OF_YEAR, 1);
			break;
		case R.id.search_today:
			c.setTime(new Date());
			break;
		case R.id.search_last_hour:
			currentDate = new Date();
			c.setTime(currentDate);
			c.add(Calendar.HOUR_OF_DAY, -1);
			setTitle("Last hour:");
			doSearch(null, c.getTime(), currentDate);
			return true;
		default:
			return true;
		}
		currentDate = c.getTime();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		Date from = c.getTime();
		c.set(Calendar.HOUR_OF_DAY, 23);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		setTitle(dateFormat.format(currentDate));
		doSearch(null, from, c.getTime());
		return true;
	}
}
