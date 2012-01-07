package org.kvj.bravo7.ui;

import java.text.SimpleDateFormat;

import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.ApplicationContext.LogEntry;
import org.kvj.bravo7.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LogViewer extends SuperActivity {

	ListView list = null;
	SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy HH:mm");
	ApplicationContext context = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = ApplicationContext.getInstance();
		setContentView(R.layout.log_view);
		list = (ListView) findViewById(R.id.log_list);
		final LogEntry[] log = context.getLog().toArray(new LogEntry[] {});
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
					view = vi.inflate(R.layout.log_item, null);
				}
				TextView text = (TextView) view.findViewById(R.id.log_item_text);
				TextView date = (TextView) view.findViewById(R.id.log_item_date);
				LogEntry entry = getItem(position);
				text.setText(entry.getText());
				date.setText(dateFormat.format(entry.getDate()));
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
			public LogEntry getItem(int arg0) {
				return log[log.length - 1 - arg0];
			}

			@Override
			public int getCount() {
				return log.length;
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
}
