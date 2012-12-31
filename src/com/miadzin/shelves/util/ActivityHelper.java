/*
 * Copyright 2011 Garen J. Torikian
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miadzin.shelves.util;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.miadzin.shelves.R;

/**
 * A class that handles some common activity-related functionality in the app,
 * such as setting up the action bar. This class provides functionality useful
 * for both phones and tablets, and does not require any Android 3.0-specific
 * features.
 */
public class ActivityHelper {
	protected Activity mActivity;

	/**
	 * Factory method for creating {@link ActivityHelper} objects for a given
	 * activity. Depending on which device the app is running, either a basic
	 * helper or Honeycomb-specific helper will be returned.
	 */
	public static ActivityHelper createInstance(Activity activity) {
		return UIUtilities.isHoneycomb() ? new ActivityHelperHoneycomb(activity)
				: new ActivityHelper(activity);
	}

	protected ActivityHelper(Activity activity) {
		mActivity = activity;
	}

	public Activity getActivity() {
		return mActivity;
	}

	/**
	 * Invoke "search" action, triggering a default search.
	 */
	public void goSearch() {
		mActivity.onSearchRequested();
	}

	/**
	 * Sets up the action bar with the given title and accent color. If title is
	 * null, then the app logo will be shown instead of a title. Otherwise, a
	 * home button and title are visible. If color is null, then the default
	 * colorstrip is visible.
	 */
	public void setupActionBar(CharSequence title) {
		final ViewGroup actionBarCompat = getActionBarCompat();
		if (actionBarCompat == null) {
			return;
		}

		actionBarCompat.removeAllViews();

		LinearLayout.LayoutParams springLayoutParams = new LinearLayout.LayoutParams(
				0, ViewGroup.LayoutParams.FILL_PARENT);
		springLayoutParams.weight = 1;

		if (title == null)
			title = " ";
		if (title != null) {
			TextView titleText = new TextView(mActivity, null,
					R.style.ActionBarCompatText);
			titleText.setId(R.id.actionbar_compat_text);
			titleText.setLayoutParams(springLayoutParams);
			titleText.setText(title);
			titleText.setPadding(8, 0, 0, 0);
			titleText.setTextColor(Color.BLACK);
			titleText.setGravity(Gravity.CENTER_VERTICAL);

			actionBarCompat.addView(titleText);
		}
	}

	/**
	 * Sets the action bar title to the given string.
	 */
	public void setActionBarTitle(CharSequence title) {
		ViewGroup actionBar = getActionBarCompat();
		if (actionBar == null) {
			return;
		}

		TextView titleText = (TextView) actionBar
				.findViewById(R.id.actionbar_compat_text);
		if (titleText != null) {
			titleText.setText(title);
		}
	}

	/**
	 * Returns the {@link ViewGroup} for the action bar on phones (compatibility
	 * action bar). Can return null, and will return null on Honeycomb.
	 */
	public ViewGroup getActionBarCompat() {
		return (ViewGroup) mActivity.findViewById(R.id.actionbar_compat);
	}

	public void showActionBar(boolean show) {
		if (show)
			getActionBarCompat().setVisibility(View.VISIBLE);
		else
			getActionBarCompat().setVisibility(View.GONE);
	}

	/**
	 * Adds an action bar button to the compatibility action bar (on phones).
	 */
	public void addActionButtonCompat(int itemId, String text,
			View.OnClickListener clickListener, boolean isImageButton,
			boolean addSeperator, boolean separatorAfter) {
		final ViewGroup actionBar = getActionBarCompat();
		if (actionBar == null) {
			return;
		}

		// Create the button
		if (isImageButton) {
			ImageButton actionButton = new ImageButton(mActivity, null,
					R.style.ActionBarCompatButton);
			actionButton.setLayoutParams(new ViewGroup.LayoutParams(
					(int) mActivity.getResources().getDimension(
							R.dimen.actionbar_compat_height),
					ViewGroup.LayoutParams.FILL_PARENT));
			actionButton.setId(itemId); // GJT: View id is the same as the
										// graphic's id
			actionButton.setImageResource(itemId);
			actionButton.setScaleType(ImageView.ScaleType.CENTER);
			if (!TextUtilities.isEmpty(text))
				actionButton.setContentDescription(text);
			actionButton.setOnClickListener(clickListener);

			setSeperator(actionBar, actionButton, addSeperator, separatorAfter);
		} else {
			LinearLayout.LayoutParams springLayoutParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.FILL_PARENT);

			TextView textButton = new TextView(mActivity, null,
					R.style.ActionBarCompatText);
			textButton.setLayoutParams(springLayoutParams);
			textButton.setId(itemId); // GJT: View id is the same as the
										// graphic's id
			textButton.setText(text);
			textButton.setOnClickListener(clickListener);
			textButton.setTextColor(Color.BLACK);
			textButton.setGravity(Gravity.CENTER_VERTICAL);

			setSeperator(actionBar, textButton, addSeperator, separatorAfter);
		}
	}

	public void showOrHideActionButtonCompat(int viewId, boolean show) {
		final ViewGroup actionBar = getActionBarCompat();
		if (actionBar == null) {
			return;
		}

		View hiddenView = actionBar.findViewById(viewId);

		if (!show)
			hiddenView.setVisibility(View.GONE);
		else
			hiddenView.setVisibility(View.VISIBLE);
	}

	// Add separator and button to the action bar in the desired order
	public void setSeperator(ViewGroup actionBar, View actionItem,
			boolean addSeperator, boolean separatorAfter) {
		if (addSeperator) {
			// Create the separator
			ImageView separator = new ImageView(mActivity, null,
					R.style.ActionBarCompatSeparator);
			separator.setLayoutParams(new ViewGroup.LayoutParams(2,
					ViewGroup.LayoutParams.FILL_PARENT));

			if (!separatorAfter) {
				actionBar.addView(separator);
			}

			actionBar.addView(actionItem);

			if (separatorAfter) {
				actionBar.addView(separator);
			}
		} else {
			actionBar.addView(actionItem);
		}
	}

	public void setEnabled(int viewId, boolean enable) {
		final ViewGroup actionBar = getActionBarCompat();
		if (actionBar == null) {
			return;
		}

		View enabledView = actionBar.findViewById(viewId);

		if (enable)
			enabledView.setVisibility(View.VISIBLE);
		else
			enabledView.setVisibility(View.GONE);
	}
}