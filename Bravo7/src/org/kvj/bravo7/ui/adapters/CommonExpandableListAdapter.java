package org.kvj.bravo7.ui.adapters;

import org.json.JSONObject;

import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public abstract class CommonExpandableListAdapter implements
		ExpandableListAdapter {

	public boolean isContextMenuGroup(ContextMenuInfo menuInfo) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int groupPosition = ExpandableListView
				.getPackedPositionGroup(info.packedPosition);
		int childPosition = ExpandableListView
				.getPackedPositionChild(info.packedPosition);
		boolean group = false;
		if (ExpandableListView.getPackedPositionType(info.packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			group = true;
		}
		return group;
	}

	public Object getContextMenuObject(ContextMenuInfo menuInfo) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		JSONObject object = null;
		int groupPosition = ExpandableListView
				.getPackedPositionGroup(info.packedPosition);
		int childPosition = ExpandableListView
				.getPackedPositionChild(info.packedPosition);
		if (ExpandableListView.getPackedPositionType(info.packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			return getChild(groupPosition, childPosition);
		}
		if (ExpandableListView.getPackedPositionType(info.packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			return getGroup(groupPosition);
		}
		return null;
	}
}
