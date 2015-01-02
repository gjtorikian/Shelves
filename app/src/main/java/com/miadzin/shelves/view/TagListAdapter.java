/***************************************************************************
 *   Copyright 2005-2009 Last.fm Ltd.                                      *
 *   Portions contributed by Casey Link, Lukasz Wisniewski,                *
 *   Mike Jennings, and Michael Novak Jr.                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.         *
 ***************************************************************************/
package com.miadzin.shelves.view;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.miadzin.shelves.R;

/**
 * Adapter class for tags used within TagActivity, implements specific behavior
 * like fading out clicked tags, smaller font etc.
 * 
 * @author Lukasz Wisniewski
 */
public class TagListAdapter extends BaseAdapter {
	protected transient Activity mContext;

	/**
	 * Internal class representing single row entry
	 * 
	 * @author Lukasz Wisniewski
	 */
	private class Entry {
		/**
		 * Text to be displayed
		 */
		String text;
		/**
		 * Indicates whether the entry has already been added meaning whether it
		 * should be grayed out
		 */
		boolean added;

		public Entry(String text, boolean added) {
			this.text = text;
			this.added = added;
		}
	}

	private ArrayList<Entry> mList;

	public TagListAdapter(Activity context) {
		mContext = context;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;

		ViewHolder holder;

		if (row == null) {
			LayoutInflater inflater = mContext.getLayoutInflater();
			row = inflater.inflate(R.layout.tag_row, null);

			holder = new ViewHolder();
			holder.label = (TextView) row.findViewById(R.id.row_label);

			row.setTag(holder);
		} else {
			holder = (ViewHolder) row.getTag();
		}

		// TODO remove hardcoded colors
		if (mList.get(position).added) {
			holder.label.setTextColor(0x337a7a7a);
		} else {
			holder.label.setTextColor(mContext.getResources()
					.getColorStateList(R.drawable.list_entry_color));
		}

		holder.label.setText(mList.get(position).text);

		return row;
	}

	public int getCount() {
		if (mList != null)
			return mList.size();
		else
			return 0;
	}

	public Object getItem(int position) {
		return mList.get(position).text;
	}

	public long getItemId(int position) {
		return position;
	}

	@Override
	public boolean isEnabled(int position) {
		return !mList.get(position).added;
	}

	/**
	 * Holder pattern implementation, performance boost
	 * 
	 * @author Lukasz Wisniewski
	 */
	static class ViewHolder {
		TextView label;
	}

	/**
	 * Sets data source (ArrayList of tags) for this adapter
	 * 
	 * @param tags
	 *            ArrayList of tags
	 */
	public void setSource(ArrayList<String> tags) {
		setSource(tags, null);
	}

	/**
	 * Sets data source (ArrayList of tags) for this adapter, additionally
	 * allows to disable so called present tags which could have been previously
	 * added by the user and we want them to be grayed out
	 * 
	 * @param tags
	 *            ArrayList of tags
	 * @param presentTags
	 *            ArrayList of already added tags or null
	 */
	public void setSource(ArrayList<String> tags, ArrayList<String> presentTags) {
		mList = new ArrayList<Entry>();
		for (int i = 0; i < tags.size(); i++) {
			Entry entry = new Entry(tags.get(i), false);
			if (presentTags != null) {
				entry.added = presentTags.contains(tags.get(i));
			}
			mList.add(entry);
		}
		notifyDataSetChanged();
	}

	/**
	 * Notify adapter that a tag at given position has been added
	 * 
	 * @param position
	 */
	public void tagAdded(int position) {
		mList.get(position).added = true;
		notifyDataSetChanged();
	}

	/**
	 * Notify adapter that a given tag was unadded
	 * 
	 * @param tag
	 */
	public void tagUnadded(String tag) {
		for (int i = 0; i < mList.size(); i++) {
			if (mList.get(i).text.equals(tag)) {
				mList.get(i).added = false;
			}
		}
		notifyDataSetChanged();
	}

}
