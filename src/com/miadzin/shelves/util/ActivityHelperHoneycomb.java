/*
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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.view.Menu;

/**
 * An extension of {@link ActivityHelper} that provides Android 3.0-specific
 * functionality for Honeycomb tablets. It thus requires API level 11.
 */
@TargetApi(11)
public class ActivityHelperHoneycomb extends ActivityHelper {
	private Menu mOptionsMenu;

	protected ActivityHelperHoneycomb(Activity activity) {
		super(activity);
	}

	@Override
	public void setActionBarTitle(CharSequence title) {
		ActionBar actionBar = mActivity.getActionBar();

		if (actionBar != null) {
			actionBar.setDisplayShowHomeEnabled(false);

			if (!TextUtilities.isEmpty(title)) {
				actionBar.setDisplayShowTitleEnabled(true);
				String[] titles = title.toString().split("\n");
				actionBar.setTitle(titles[0]);

				if (titles.length > 1)
					actionBar.setSubtitle(titles[1]);
			}
		}
	}

	@Override
	public void setEnabled(int viewId, boolean enable) {
		mActivity.invalidateOptionsMenu();
	}
}
