/*
 * Copyright (C) 2010 Garen J. Torikian
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

package com.miadzin.shelves.base;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.miadzin.shelves.R;
import com.miadzin.shelves.util.ActivityHelper;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public abstract class AddBaseItemActivity extends Activity implements
		View.OnClickListener {
	private static final String LOG_TAG = "AddBaseItemActivity";

	protected static int COVER_WIDTH;
	protected static int COVER_HEIGHT;

	protected static final int DIALOG_ADD = 1;
	protected static final int UNSUPPORTED_FEATURE_DIALOG_ID = 2;

	protected static final String STATE_ADD_IN_PROGRESS = "shelves.add.inprogress";
	protected static final String STATE_SEARCH_IN_PROGRESS = "shelves.search.inprogress";

	protected View mSearchButton;
	protected EditText mSearchQuery;
	protected View mSearchPanel;
	protected View mAddPanel;

	protected int mPage = 1;

	private ActivityHelper mActivityHelper;
	protected String itemIdString;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Context c = getBaseContext();
		final String sActivity = this.toString();
		AnalyticsUtils.getInstance(c)
				.trackPageView(
						"/"
								+ sActivity.substring(0,
										sActivity.indexOf("Activity") + 8));

		setContentView(R.layout.screen_add_search);

		AdView mAdView = (AdView) findViewById(R.id.adview);
		if (!UIUtilities.isPaid(getContentResolver(), this)) {
			mAdView.setVisibility(View.VISIBLE);
			AdRequest adRequest = new AdRequest.Builder().build();
			mAdView.loadAd(adRequest);
		} else {
			mAdView.setVisibility(View.GONE);
		}

		final SharedPreferences pref = getBaseContext().getSharedPreferences(
				Preferences.NAME, 0);
		final int density = pref.getInt(Preferences.KEY_DPI, 160);

		switch (density) {
		case 320:
			COVER_WIDTH = COVER_HEIGHT = 158;
			break;
		case 240:
			COVER_WIDTH = COVER_HEIGHT = 105;
			break;
		case 120:
			COVER_WIDTH = COVER_HEIGHT = 52;
			break;
		case 160:
		default:
			COVER_WIDTH = COVER_HEIGHT = 70;
			break;
		}

		setupViews();
	}

	protected void setupViews() {
		mSearchQuery = (EditText) findViewById(R.id.input_search_query);
		mSearchQuery.addTextChangedListener(new SearchFieldWatcher());

		mSearchQuery.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_ENTER:
					mSearchButton.performClick();
					return true;
				default:
					return false;
				}
			}
		});

		mActivityHelper = ActivityHelper.createInstance(this);

		final String activityToMatch = this.toString();

		if (activityToMatch.contains("Apparel")) {
			itemIdString = getString(R.string.apparel_label_big);
		} else if (activityToMatch.contains("BoardGames")) {
			itemIdString = getString(R.string.boardgame_label_plural_big)
					+ "\nwww.boardgamegeek.com";
		} else if (activityToMatch.contains("Book")) {
			itemIdString = getString(R.string.book_label_plural_big);
		} else if (activityToMatch.contains("Comic")) {
			itemIdString = getString(R.string.comic_label_plural_big)
					+ "\nwww.comicvine.com";
		} else if (activityToMatch.contains("Gadget")) {
			itemIdString = getString(R.string.gadget_label_plural_big);
		} else if (activityToMatch.contains("Movie")) {
			itemIdString = getString(R.string.movie_label_plural_big);
		} else if (activityToMatch.contains("Music")) {
			itemIdString = getString(R.string.music_label_big);
		} else if (activityToMatch.contains("Software")) {
			itemIdString = getString(R.string.software_label_big);
		} else if (activityToMatch.contains("Tool")
				|| activityToMatch.contains("Tools")) {
			itemIdString = getString(R.string.tool_label_plural_big);
		} else if (activityToMatch.contains("Toy")) {
			itemIdString = getString(R.string.toy_label_plural_big);
		} else if (activityToMatch.contains("VideoGame")) {
			itemIdString = getString(R.string.videogame_label_plural_big);
		} else
			itemIdString = null;

		if (!UIUtilities.isHoneycomb()) {
			mActivityHelper.showActionBar(true);
			mActivityHelper.setupActionBar(itemIdString);

			View.OnClickListener paginationClickListener = new View.OnClickListener() {
				public void onClick(View view) {
					getNextResults();
				}
			};

			mActivityHelper.addActionButtonCompat(
					R.drawable.ic_action_pagination, null,
					paginationClickListener, true, true, true);

			mActivityHelper.setEnabled(R.drawable.ic_action_pagination, false);
		} else {
			mActivityHelper.setActionBarTitle(itemIdString);
		}
	}

	abstract protected void getNextResults();

	abstract protected void onSearch(boolean b);

	protected void showPanel(View panel, boolean slideUp) {
		panel.startAnimation(AnimationUtils.loadAnimation(this,
				slideUp ? R.anim.slide_in : R.anim.slide_out_top));
		panel.setVisibility(View.VISIBLE);
	}

	protected void hidePanel(View panel, boolean slideDown) {
		panel.startAnimation(AnimationUtils.loadAnimation(this,
				slideDown ? R.anim.slide_out : R.anim.slide_in_top));
		panel.setVisibility(View.GONE);
	}

	protected void disableSearchPanel() {
		mSearchButton.setEnabled(false);
		mSearchQuery.setEnabled(false);
		mActivityHelper.setEnabled(R.drawable.ic_action_pagination, false);
	}

	protected void enableSearchPanel() {
		mSearchButton.setEnabled(true);
		mSearchQuery.setEnabled(true);
		mActivityHelper.setEnabled(R.drawable.ic_action_pagination, true);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!TextUtilities.isEmpty(mSearchQuery.getText().toString())
				&& UIUtilities.isHoneycomb()) {
			getMenuInflater().inflate(R.menu.add_search, menu);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ic_action_pagination:
			if (!TextUtilities.isEmpty(mSearchQuery.getText().toString()))
				getNextResults();
			break;
		default:
			break;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_go:
			mPage = 1;
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mSearchQuery.getWindowToken(), 0);
			onSearch(true);
			break;
		}
	}

	private class SearchFieldWatcher implements TextWatcher {
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			mSearchButton.setEnabled(s.length() > 0);
		}

		public void afterTextChanged(Editable s) {
		}
	}
}