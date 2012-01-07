package org.kvj.bravo7.ui.adapters;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.R;
import org.kvj.bravo7.impl.Controller;
import org.kvj.bravo7.impl.TemplateEngine;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class CheckinListAdapter extends CommonExpandableListAdapter {

	SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy h:mm a");
	List<JSONObject> data = new ArrayList<JSONObject>();
	Collection<JSONObject> unsent = new ArrayList<JSONObject>();
	DataSetObserver observer = null;
	Controller controller = null;

	public CheckinListAdapter(Controller controller) {
		this.controller = controller;
	}

	private List<JSONObject> getUnsetChildren(String id) {
		List<JSONObject> result = new ArrayList<JSONObject>();
		for (JSONObject ch : unsent) {
			if (ch.optString("attach_to", "-").equals(id)) {
				result.add(ch);
			}
		}
		return result;
	}

	public void setData(Collection<JSONObject> data) {
		this.data.clear();
		this.data.addAll(data);
		unsent = controller.getUnsentCheckins();
		if (null != observer) {
			observer.onChanged();
		}
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public JSONObject getChild(int groupPosition, int childPosition) {
		JSONObject checkin = data.get(groupPosition);
		JSONArray refs = checkin.optJSONArray("refs");
		if (childPosition < refs.length()) {
			return refs.optJSONObject(childPosition);
		}
		List<JSONObject> unsentChildren = getUnsetChildren(checkin
				.optString("id"));
		return unsentChildren.get(childPosition - refs.length());
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	private View objectToView(JSONObject object, View view, Context context,
			boolean group) {
		if (view == null) {
			LayoutInflater vi = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = vi.inflate(R.layout.checkin_item, null);
		}
		view.setPadding(group ? 35 : 20, 0, 0, 0);
		ImageView icon = (ImageView) view.findViewById(R.id.checkin_icon);
		TextView text = (TextView) view.findViewById(R.id.checkin_text);
		TextView date = (TextView) view.findViewById(R.id.checkin_date);
		date.setText(dateFormat.format(new Date(object.optLong("created",
				System.currentTimeMillis()))));
		JSONObject template = controller.getTemplate(object.optString(
				"template", "-"));
		if (template != null) {
			Bitmap bmap = controller.getIcon(template.optString("icon", ""));
			if (bmap != null) {
				icon.setImageBitmap(bmap);
			}
		}
		text.setText(display(object, template));
		return view;
	}

	private String display(JSONObject object, JSONObject template) {
		String display = template.optString("display", "");
		if ("".equals(display)) {
			display = "{text}";
		}
		return TemplateEngine.apply(display, object, "<No data>");
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		return objectToView(getChild(groupPosition, childPosition),
				convertView, parent.getContext(), false);
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		JSONObject checkin = data.get(groupPosition);
		JSONArray refs = checkin.optJSONArray("refs");
		Collection<JSONObject> unsentChildren = getUnsetChildren(checkin
				.optString("id"));
		return refs.length() + unsentChildren.size();
	}

	@Override
	public long getCombinedChildId(long groupId, long childId) {
		return childId;
	}

	@Override
	public long getCombinedGroupId(long groupId) {
		return groupId;
	}

	@Override
	public JSONObject getGroup(int groupPosition) {
		return data.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return data.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		return objectToView(getGroup(groupPosition), convertView,
				parent.getContext(), true);
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void onGroupCollapsed(int groupPosition) {
	}

	@Override
	public void onGroupExpanded(int groupPosition) {
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		this.observer = observer;
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		this.observer = null;
	}

}
