package org.kvj.bravo7.ui.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.R;
import org.kvj.bravo7.impl.Controller;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TemplateListAdapter extends CommonExpandableListAdapter {

	private List<JSONObject> data = new ArrayList<JSONObject>();
	private List<String> tags = new ArrayList<String>();
	private DataSetObserver observer = null;
	private Controller controller = null;

	public TemplateListAdapter(Controller controller) {
		this.controller = controller;
	}

	public void setData(Collection<JSONObject> data) {
		this.data.clear();
		this.data.addAll(data);
		if (observer != null) {
			observer.onChanged();
		}
		tags.clear();
		for (JSONObject obj : data) {
			JSONArray arr = obj.optJSONArray("tags");
			if (arr != null) {
				for (int i = 0; i < arr.length(); i++) {
					String tag = arr.optString(i).toLowerCase();
					if (tags.indexOf(tag) == -1) {
						tags.add(tag);
					}
				}
			}
		}
		Collections.sort(tags);
		tags.add(0, "No tags");
	}

	private List<JSONObject> getTemplateByTag(int index) {
		List<JSONObject> result = new ArrayList<JSONObject>();
		String _tag = tags.get(index);
		for (JSONObject tmpl : data) {
			JSONArray arr = tmpl.optJSONArray("tags");
			if (arr != null) {
				if (index == 0) {
					if (arr.length() == 0) {
						result.add(tmpl);
					}
					continue;
				}
				for (int i = 0; i < arr.length(); i++) {
					String tag = arr.optString(i);
					if (_tag.equalsIgnoreCase(tag)) {
						result.add(tmpl);
						break;
					}
				}
			}
		}
		Collections.sort(result, new Comparator<JSONObject>() {

			@Override
			public int compare(JSONObject object1, JSONObject object2) {
				String name1 = object1.optString("name", "");
				String name2 = object2.optString("name", "");
				return name1.compareToIgnoreCase(name2);
			}
		});
		return result;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public JSONObject getChild(int groupPosition, int childPosition) {
		List<JSONObject> list = getTemplateByTag(groupPosition);
		return list.get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		JSONObject obj = getChild(groupPosition, childPosition);
		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.template_item, null);
		}
		TextView textView = (TextView) convertView
				.findViewById(R.id.template_item_text);
		textView.setText(obj.optString("name"));
		ImageView imageView = (ImageView) convertView
				.findViewById(R.id.template_item_icon);
		imageView.setImageBitmap(controller.getIcon(obj.optString("icon")));
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return getTemplateByTag(groupPosition).size();
	}

	@Override
	public long getCombinedChildId(long groupId, long childId) {
		return groupId * 1000 + childId + 1;
	}

	@Override
	public long getCombinedGroupId(long groupId) {
		return groupId * 1000;
	}

	@Override
	public Object getGroup(int groupPosition) {
		return tags.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return tags.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.template_tag, null);
		}
		TextView textView = (TextView) convertView
				.findViewById(R.id.template_item_text);
		textView.setText(tags.get(groupPosition));
		return convertView;
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
		// TODO Auto-generated method stub

	}

	@Override
	public void onGroupExpanded(int groupPosition) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		this.observer = observer;
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (this.observer == observer) {
			this.observer = null;
		}
	}
}
