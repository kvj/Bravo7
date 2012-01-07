package org.kvj.bravo7.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.R;
import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.impl.LocationController.LocationPathRequest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

public class LocationQueue extends SuperActivity {

	ListView list = null;
	SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy HH:mm");
	ApplicationContext context = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = ApplicationContext.getInstance();
		setContentView(R.layout.location_queue);
		list = (ListView) findViewById(R.id.location_queue_list);
		registerForContextMenu(list);
	}

	private void loadData() {
		final LocationPathRequest[] data =
				controller.getLocationController().getActivePathRequests();
		list.setAdapter(new ListAdapter() {

			@Override
			public void unregisterDataSetObserver(DataSetObserver observer) {
			}

			@Override
			public void registerDataSetObserver(DataSetObserver observer) {
			}

			@Override
			public boolean isEmpty() {
				return false;
			}

			@Override
			public boolean hasStableIds() {
				return false;
			}

			@Override
			public int getViewTypeCount() {
				return 1;
			}

			@Override
			public View getView(int position, View view, ViewGroup parent) {
				if (view == null) {
					LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(
							Context.LAYOUT_INFLATER_SERVICE);
					view = vi.inflate(R.layout.location_queue_item, null);
				}
				TextView text1 = (TextView) view.findViewById(R.id.location_queue_item_text1);
				TextView text2 = (TextView) view.findViewById(R.id.location_queue_item_text2);
				LocationPathRequest entry = getItem(position);
				JSONArray path = entry.getResult();
				text1.setText("Points so far: " + path.length() + " / every "
						+ (entry.getTimeout() / 60) + " min");
				text2.setText("");
				if (path.length() > 0) {
					try {
						JSONObject location = path.getJSONObject(path.length() - 1);
						text2.setText("Last point: "
								+ dateFormat.format(new Date(location.getLong("at"))));
					} catch (Exception e) {
					}
				}
				// text.setText(entry.getText());
				// date.setText(dateFormat.format(entry.getDate()));
				return view;
			}

			@Override
			public int getItemViewType(int position) {
				return 0;
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public LocationPathRequest getItem(int arg0) {
				return data[arg0];
			}

			@Override
			public int getCount() {
				return data.length;
			}

			@Override
			public boolean isEnabled(int position) {
				return true;
			}

			@Override
			public boolean areAllItemsEnabled() {
				return true;
			}
		});
	}

	@Override
	public void onController(Controller controller) {
		super.onController(controller);
		loadData();
	}

	private void showFinishDialog(final LocationPathRequest request) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Finish path:");
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		final CheckBox lastPoint = new CheckBox(this);
		lastPoint.setText("Take last point and stop");
		lastPoint.setChecked(true);
		final DatePicker datePicker = new DatePicker(this);
		datePicker.setEnabled(false);
		final TimePicker timePicker = new TimePicker(this);
		timePicker.setEnabled(false);

		lastPoint.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				datePicker.setEnabled(!isChecked);
				timePicker.setEnabled(!isChecked);
			}
		});

		layout.addView(lastPoint);
		layout.addView(datePicker);
		layout.addView(timePicker);

		alert.setView(layout);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int whichButton) {
				Calendar c = Calendar.getInstance();
				c.set(Calendar.DAY_OF_MONTH, 1);
				c.set(Calendar.YEAR, datePicker.getYear());
				c.set(Calendar.MONTH, datePicker.getMonth());
				c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
				c.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
				c.set(Calendar.MINUTE, timePicker.getCurrentMinute());
				request.finish(c.getTime(), lastPoint.isChecked());
				loadData();
			}
		});

		// alert.setNegativeButton("Cancel",
		// new DialogInterface.OnClickListener() {
		//
		// public void onClick(DialogInterface dialog, int whichButton) {
		// }
		// });

		alert.show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		inflater.inflate(R.menu.location_queue_menu, menu);
		menu.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				LocationPathRequest request = (LocationPathRequest) list.
						getItemAtPosition(info.position);
				showFinishDialog(request);
				return true;
			}
		});
	}

}
