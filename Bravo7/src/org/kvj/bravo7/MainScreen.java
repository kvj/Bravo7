package org.kvj.bravo7;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.impl.Controller.NetworkResult;
import org.kvj.bravo7.impl.Controller.RefreshType;
import org.kvj.bravo7.ui.CheckinForm;
import org.kvj.bravo7.ui.LocationQueue;
import org.kvj.bravo7.ui.LogViewer;
import org.kvj.bravo7.ui.LoginDialog;
import org.kvj.bravo7.ui.PrefActivity;
import org.kvj.bravo7.ui.SearchCheckins;
import org.kvj.bravo7.ui.SuperActivity;
import org.kvj.bravo7.ui.adapters.CheckinListAdapter;
import org.kvj.bravo7.ui.adapters.TemplateListAdapter;
import org.kvj.bravo7.ui.widget.WidgetSupport;
import org.kvj.bravo7.ui.widget.WidgetSupport.AskResult;

import android.app.ProgressDialog;
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
import android.view.View.OnCreateContextMenuListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

public class MainScreen extends SuperActivity implements Serializable {

	private static final long serialVersionUID = -7168953319288072977L;

	public interface Reloaded {

		public void reload(boolean ok);
	}

	private static final String TAG = "Main";
	private ApplicationContext context = null;
	private ExpandableListView list = null;
	private TemplateListAdapter listAdapter = null;
	WidgetSupport widget = null;
	JSONObject selectedTemplate = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = ApplicationContext.getInstance();
		setContentView(R.layout.main);
		widget = new WidgetSupport(this);
		list = (ExpandableListView) findViewById(R.id.templates_list);
		list.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				boolean group = listAdapter.isContextMenuGroup(menuInfo);
				if (group) {
					return;
				}
				final JSONObject object = (JSONObject) listAdapter
						.getContextMenuObject(menuInfo);
				if (object == null) {
					return;
				}
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.template_menu, menu);
				final String id = object.optString("id", "-");
				final boolean isFav = controller.isFavoriteTemplate(id);
				menu.getItem(0).setTitle(
						isFav ? "Unmark favorite" : "Mark favorite");
				menu.getItem(0).setOnMenuItemClickListener(
						new OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item) {
								controller.favoriteTemplate(id, !isFav);
								return true;
							}
						});
			}
		});
		Intent i = new Intent(this, AllTimeService.class);
		startService(i);
		list.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				final JSONObject object = listAdapter.getChild(groupPosition,
						childPosition);
				if (widget.isWidget()) {
					widget.ask("Template name?", object.optString("name", ""),
							new AskResult() {

								@Override
								public String result(String value) {
									JSONObject config = new JSONObject();
									try {
										config.putOpt("text", value);
										config.putOpt("template",
												object.optString("id", "-"));
										widget.finish(config);
									} catch (Exception e) {
										Log.e(TAG, "Error:", e);
										MainScreen.this
												.notify("Error configuring widget");
									}
									return null;
								}
							});
					return true;
				}
				Intent callIntent = getIntent();
				Intent intent = new Intent(MainScreen.this, CheckinForm.class);
				intent.putExtra("object", object.toString());
				intent.putExtra("ref", callIntent.getStringExtra("ref"));
				intent.putExtra("attach_to",
						callIntent.getStringExtra("attach_to"));
				startActivity(intent);
				return true;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.templates, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.templates_login:
			intent = new Intent(this, LoginDialog.class);
			startActivity(intent);
			break;
		case R.id.templates_search:
			intent = new Intent(this, SearchCheckins.class);
			context.publishBean("checkinProvider",
					new SearchCheckins.CheckinProvider() {

						private static final long serialVersionUID = 7888220694234751755L;

						@Override
						public boolean showSearch() {
							return true;
						}

						@Override
						public boolean showFavorite() {
							return true;
						}

						@Override
						public boolean showRemove() {
							return false;
						}

						@Override
						public void reload(SuperActivity ctx,
								Controller controller,
								CheckinListAdapter adapter, boolean modified) {
							if (modified) {
								MainScreen.reload(controller, ctx,
										RefreshType.UpdateCheckins,
										new Reloaded() {

											@Override
											public void reload(boolean ok) {
											}

										});
							}
						}

						@Override
						public String getTitle() {
							return "Search";
						}

					});
			startActivity(intent);
			break;
		case R.id.templates_fav:
			intent = new Intent(this, SearchCheckins.class);
			// Log.i(TAG, "Context: " + context);
			context.publishBean("checkinProvider", createFavProvider(null));
			startActivity(intent);
			break;
		case R.id.templates_unsent:
			intent = new Intent(this, SearchCheckins.class);
			context.publishBean("checkinProvider",
					new SearchCheckins.CheckinProvider() {

						private static final long serialVersionUID = -2285796196630385160L;

						@Override
						public boolean showSearch() {
							return false;
						}

						@Override
						public boolean showFavorite() {
							return false;
						}

						@Override
						public boolean showRemove() {
							return true;
						}

						@Override
						public void reload(SuperActivity ctx,
								final Controller controller,
								final CheckinListAdapter adapter,
								boolean modified) {
							adapter.setData(controller.getUnsentCheckins());
						}

						@Override
						public String getTitle() {
							return "Unsent checkins";
						}

					});
			startActivity(intent);
			break;
		case R.id.templates_preferences:
			intent = new Intent(this, PrefActivity.class);
			startActivity(intent);
			break;
		case R.id.log_view_menu:
			intent = new Intent(this, LogViewer.class);
			startActivity(intent);
			break;
		case R.id.templates_location:
			intent = new Intent(this, LocationQueue.class);
			startActivity(intent);
			break;
		case R.id.templates_reload:
			reload(controller, this, RefreshType.Full, new Reloaded() {

				@Override
				public void reload(boolean ok) {
					if (ok) {
						listAdapter.setData(controller.getTemplates());
					}
				}
			});
			break;
		}
		return true;
	}

	public static void reload(Controller controller,
			final SuperActivity context, RefreshType refreshType,
			final Reloaded reloaded) {
		if (controller == null) {
			context.notify("No connection");
			return;
		}
		final ProgressDialog dialog = new ProgressDialog(context);
		dialog.setTitle("Reloading");
		dialog.setMessage("Reloading. Please wait");
		dialog.show();
		controller.reload(refreshType, new NetworkResult() {

			@Override
			public void onResult(Object result) {
				dialog.dismiss();
				if (result != null) {
					context.notify((String) result);
					reloaded.reload(false);
				} else {
					context.notify("Updated");
					reloaded.reload(true);
				}
			}
		});
	}

	@Override
	public void onController(Controller controller) {
		super.onController(controller);
		context = ApplicationContext.getInstance();
		if (controller != null && listAdapter == null) {
			listAdapter = new TemplateListAdapter(controller);
			list.setAdapter(listAdapter);
			listAdapter.setData(controller.getTemplates());
		}
	}

	public static SearchCheckins.CheckinProvider createFavProvider(
			final String onlyID) {
		return new SearchCheckins.CheckinProvider() {

			@Override
			public boolean showSearch() {
				return false;
			}

			@Override
			public boolean showFavorite() {
				return true;
			}

			@Override
			public boolean showRemove() {
				return false;
			}

			@Override
			public boolean longClick(JSONObject object, boolean group) {
				try {
					object.put("group", group);
				} catch (JSONException e) {
				}
				return super.longClick(object, group);
			}

			@Override
			public BindType showBind(JSONObject object) {
				if (object.optBoolean("group")) {
					return BindType.Local;
				}
				return BindType.Remote;
			}

			@Override
			public void reload(SuperActivity ctx, final Controller controller,
					final CheckinListAdapter adapter, boolean modified) {
				if (modified) {
					MainScreen.reload(controller, ctx,
							RefreshType.UpdateCheckins, new Reloaded() {

								@Override
								public void reload(boolean ok) {
									if (ok) {
										adapter.setData(controller
												.getCheckins());
									}
								}

							});
				} else {
					if (onlyID != null) {
						JSONObject object = controller.getCheckin(onlyID);
						if (object != null) {
							List<JSONObject> data = new ArrayList<JSONObject>();
							data.add(object);
							adapter.setData(data);
							return;
						}
					}
					adapter.setData(controller.getCheckins());
				}
			}

			@Override
			public String getTitle() {
				return "Favorites";
			}

		};
	}
}