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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.activity.HelpActivity;
import com.miadzin.shelves.activity.LoadImagesActivity;
import com.miadzin.shelves.activity.MainGridActivity;
import com.miadzin.shelves.activity.QuantityActivity;
import com.miadzin.shelves.activity.RateActivity;
import com.miadzin.shelves.activity.SettingsActivity;
import com.miadzin.shelves.activity.TagActivity;
import com.miadzin.shelves.drawable.CrossFadeDrawable;
import com.miadzin.shelves.drawable.FastBitmapDrawable;
import com.miadzin.shelves.provider.ItemImport;
import com.miadzin.shelves.provider.apparel.ApparelManager;
import com.miadzin.shelves.provider.boardgames.BoardGamesManager;
import com.miadzin.shelves.provider.books.BooksManager;
import com.miadzin.shelves.provider.comics.ComicsManager;
import com.miadzin.shelves.provider.gadgets.GadgetsManager;
import com.miadzin.shelves.provider.movies.MoviesManager;
import com.miadzin.shelves.provider.music.MusicManager;
import com.miadzin.shelves.provider.software.SoftwareManager;
import com.miadzin.shelves.provider.tools.ToolsManager;
import com.miadzin.shelves.provider.toys.ToysManager;
import com.miadzin.shelves.provider.videogames.VideoGamesManager;
import com.miadzin.shelves.scan.ScanIntent;
import com.miadzin.shelves.scan.ScanIntentIntegrator;
import com.miadzin.shelves.scan.ScanIntentResult;
import com.miadzin.shelves.util.ActivityHelper;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.ExportUtilities;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.IOUtilities.inputTypes;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.ImportUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;
import com.miadzin.shelves.view.ShelvesView;

public abstract class BaseItemActivity extends Activity {
	private static final String LOG_TAG = "BaseItemActivity";
	public static final String SOURCE = "BaseItemActivity";

	protected static final int REQUEST_SCAN_FOR_ADD = 1;
	protected static final int REQUEST_SCAN_FOR_CHECK = 2;
	protected static final int REQUEST_SCAN_FOR_CHECK_ONLINE = 3;
	protected static final int REQUEST_SCAN_FOR_ADD_BULK = 4;
	protected static final int REQUEST_ADDING_TAGS = 5;
	protected static final int REQUEST_LOAN = 6;
	protected static final int REQUEST_LOAN_CHANGE = 7;
	protected static final int REQUEST_RATE = 8;
	protected static final int REQUEST_COVER_CHANGE = 9;
	protected static final int REQUEST_LOAN_RESULT = 10;
	protected static final int REQUEST_ADDING_MULTI_TAGS = 11;
	protected static final int REQUEST_ADDING_MULTI_RATE = 12;
	protected static final int REQUEST_ADDING_MANUAL_ITEM = 13;
	protected static final int CHANGE_QUANTITY = 14;

	protected static final int COVER_TRANSITION_DURATION = 175;

	protected static final int WINDOW_DISMISS_DELAY = 600;
	protected static final int WINDOW_SHOW_DELAY = 600;

	protected static final String STATE_IMPORT_IN_PROGRESS = "shelves.import.inprogress";
	protected static final String STATE_IMPORT_INDEX = "shelves.import.index";
	protected static final String STATE_IMPORT_TYPE = "shelves.import.type";

	protected static final String STATE_EXPORT_IN_PROGRESS = "shelves.export.inprogress";
	protected static final String STATE_EXPORT_INDEX = "shelves.export.index";
	protected static final String STATE_EXPORT_TYPE = "shelves.export.type";

	protected ExportTask mExportTask;

	protected static final String STATE_ADD_IN_PROGRESS = "shelves.add.inprogress";

	protected static final int MENU_ITEM_SORT_RECENTLY_ADDED = 0;
	protected static final int MENU_ITEM_SORT_TITLE_ASC = 1;
	protected static final int MENU_ITEM_SORT_TITLE_DESC = 2;
	protected static final int MENU_ITEM_SORT_AUTHOR_ASC = 3;
	protected static final int MENU_ITEM_SORT_AUTHOR_DESC = 4;
	protected static final int MENU_ITEM_SORT_RATING_ASC = 5;
	protected static final int MENU_ITEM_SORT_RATING_DESC = 6;
	protected static final int MENU_ITEM_SORT_PRICE_ASC = 7;
	protected static final int MENU_ITEM_SORT_PRICE_DESC = 8;

	protected int itemIdString;

	public int prevOrientation;

	public static final String SHELF_VIEW = "shelf";
	public static final String LIST_VIEW = "list";
	public static final String LIST_VIEW_NO_COVER = "list_nocover";

	protected static final String EXPORT_EVERYTHING = "shelves.intent.action.ACTION_EXPORT_EVERYTHING";

	protected ActivityHelper mActivityHelper;
	protected SharedPreferences pref;

	protected boolean mPendingCoversUpdate;

	protected boolean mFingerUp = true;
	protected PopupWindow mPopup;

	protected FastBitmapDrawable mDefaultCover;

	protected View mGridPosition;
	protected TextView mPositionText;

	protected ProgressBar mImportProgress;
	protected View mImportPanel;
	protected View mExportPanel;
	protected View mAddPanel;

	protected ShelvesView mGrid;
	protected ListView mList;

	protected Bundle mSavedState;

	protected Boolean mPrefsActivated = false;
	protected Boolean mEverythingDeleted = false;

	public static final int FILTER_DIALOG_ID = 0;
	protected static final int MULTISELECT_DIALOG_ID = 2;
	protected static final int FINISH_MULTISELECT_DIALOG_ID = 4;
	public static final int ADD_ITEM_BY_TYPING_DIALOG_ID = 5;
	public static final int ADD_ITEM_BY_SCAN_DIALOG_ID = 6;
	public static final int PAID_FEATURE_DIALOG_ID = 7;
	public static final int UNSUPPORTED_FEATURE_DIALOG_ID = 8;
	public static final int SUPPORT_THE_DEV = 9;
	public static final int SORT_DIALOG_ID = 10;

	protected String activityToMatch;

	protected ViewFlipper categoryViewFlipper;

	protected enum VIEW_TYPE {
		shelfView, listView, listView_nocover
	}

    protected String viewType;

	List<String> mFilterTags;

	public static boolean mMultiSelect;
	public static String mMultiSelectId;
	public static String mMultiItemType;

	protected static ArrayList<String> multiSelectIds;
	protected ArrayList<Integer> multiSelectPos;

	public String mMultiSelectType;

	protected List<String> sortTypes;
	protected List<String> sortNames;
	protected StringBuilder sortPhrase;
	protected Dialog sortDialog;
	protected int sortTimes;
	protected static int sortCount = 0;

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

		pref = c.getSharedPreferences(Preferences.NAME, 0);

		setDisplayType();

		multiSelectIds = new ArrayList<String>();
		mMultiItemType = mMultiSelectId = "";
		mMultiSelect = false;

		AdView mAdView = (AdView) findViewById(R.id.adview);
		if (!UIUtilities.isPaid(getContentResolver(), this)) {
			mAdView.setVisibility(View.VISIBLE);
			AdRequest adRequest = new AdRequest.Builder().build();
			mAdView.loadAd(adRequest);
			final int rand = 1 + (int) (Math.random() * ((3000 - 1) + 1));
			if (rand == 5 && !ShelvesApplication.mFirstRun)
				showDialog(SUPPORT_THE_DEV);
		} else {
			mAdView.setVisibility(View.GONE);
		}

		Object savedState[] = (Object[]) getLastNonConfigurationInstance();

		if (savedState != null && (Boolean) savedState[0]) {
			mMultiSelect = true;
			multiSelectIds = (ArrayList<String>) savedState[1];
			mMultiSelectType = (String) savedState[2];
		}

		sortTypes = new ArrayList(9);
		sortNames = new ArrayList(9);
		sortPhrase = new StringBuilder();

		sortTypes
				.add(getString(R.string.preferences_sort_recently_added_desc_label));
		sortTypes.add(getString(R.string.preferences_sort_title_asc_label));
		sortTypes.add(getString(R.string.preferences_sort_title_desc_label));
		sortTypes.add(getString(R.string.preferences_sort_author_asc_label));
		sortTypes.add(getString(R.string.preferences_sort_author_desc_label));
		sortTypes.add(getString(R.string.preferences_sort_rating_asc_label));
		sortTypes.add(getString(R.string.preferences_sort_rating_desc_label));
		sortTypes.add(getString(R.string.preferences_sort_price_asc_label));
		sortTypes.add(getString(R.string.preferences_sort_price_desc_label));

		sortNames.add("_id COLLATE NOCASE desc");
		sortNames.add("sort_title COLLATE NOCASE asc");
		sortNames.add("sort_title COLLATE NOCASE desc");
		sortNames.add("authors COLLATE NOCASE asc");
		sortNames.add("authors COLLATE NOCASE desc");
		sortNames.add("rating COLLATE NOCASE asc");
		sortNames.add("rating COLLATE NOCASE desc");
		sortNames.add("retail_price COLLATE NOCASE asc");
		sortNames.add("retail_price COLLATE NOCASE desc");
	}

	protected abstract void setupViews();

	protected abstract void onView(String stringId);

	protected abstract void onDelete(String id);

	protected abstract void onTag(String viewId);

	protected abstract void onRate(String viewId);

	protected abstract void onLoan(String viewId);

	protected abstract void onLoanChange(String viewId);

	protected abstract void onLoanReturn(String viewId);

	protected abstract void postUpdateCovers();

	protected void setDisplayType() {
		viewType = pref.getString(findItemViewType(this.toString()),
				BaseItemActivity.SHELF_VIEW);

		if (viewType.equals(BaseItemActivity.SHELF_VIEW)) {
			setContentView(R.layout.screen_shelves);
			getWindow().setBackgroundDrawable(null);
		} else if (viewType.equals(BaseItemActivity.LIST_VIEW)
				|| viewType.equals(BaseItemActivity.LIST_VIEW_NO_COVER)) {
			setContentView(R.layout.screen_shelves_list);
			TextView emptyView = new TextView(getBaseContext());
			emptyView.setLayoutParams(new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			emptyView.setVisibility(View.GONE);
			ListView currList = (ListView) findViewById(android.R.id.list);
			((ViewGroup) currList.getParent()).addView(emptyView);
			currList.setEmptyView(emptyView);
		}
	}

	protected void postSetupViews() {
		sortTimes = Integer.valueOf(pref.getString(Preferences.KEY_SORT_NUM,
				"1"));

		mGridPosition = getLayoutInflater().inflate(R.layout.grid_position,
				null);
		mPositionText = (TextView) mGridPosition.findViewById(R.id.text);

		mPopup = null;

		mActivityHelper = ActivityHelper.createInstance(this);

		activityToMatch = this.toString();

		if (activityToMatch.contains("Apparel")) {
			itemIdString = R.string.apparel_label_big;
			mMultiItemType = getString(R.string.apparel_label);
		} else if (activityToMatch.contains("BoardGames")) {
			itemIdString = R.string.boardgame_label_plural_big;
			mMultiItemType = getString(R.string.boardgame_label_plural_small);
		} else if (activityToMatch.contains("Books")) {
			itemIdString = R.string.book_label_plural_big;
			mMultiItemType = getString(R.string.book_label_plural_small);
		} else if (activityToMatch.contains("Comics")) {
			itemIdString = R.string.comic_label_plural_big;
			mMultiItemType = getString(R.string.comic_label_plural_small);
		} else if (activityToMatch.contains("Gadgets")) {
			itemIdString = R.string.gadget_label_plural_big;
			mMultiItemType = getString(R.string.gadget_label_plural_small);
		} else if (activityToMatch.contains("Movies")) {
			itemIdString = R.string.movie_label_plural_big;
			mMultiItemType = getString(R.string.movie_label_plural_small);
		} else if (activityToMatch.contains("Music")) {
			itemIdString = R.string.music_label_big;
			mMultiItemType = getString(R.string.music_label);
		} else if (activityToMatch.contains("Software")) {
			itemIdString = R.string.software_label_big;
			mMultiItemType = getString(R.string.software_label);
		} else if (activityToMatch.contains("Tools")) {
			itemIdString = R.string.tool_label_plural_big;
			mMultiItemType = getString(R.string.tool_label_plural_small);
		} else if (activityToMatch.contains("Toys")) {
			itemIdString = R.string.toy_label_plural_big;
			mMultiItemType = getString(R.string.toy_label_plural_small);
		} else if (activityToMatch.contains("VideoGames")) {
			itemIdString = R.string.videogame_label_plural_big;
			mMultiItemType = getString(R.string.videogame_label_plural_small);
		} else
			itemIdString = -1;

		if (!UIUtilities.isHoneycomb()) {
			mActivityHelper.showActionBar(true);

			mActivityHelper
					.setupActionBar(getString(itemIdString)
							+ setupActionBarTitle(getContentResolver(),
									activityToMatch));

			View.OnClickListener addClickListener = new View.OnClickListener() {
				public void onClick(View view) {
					if (mMultiSelect)
						showDialog(FINISH_MULTISELECT_DIALOG_ID);
					else
						mActivityHelper.getActivity().showDialog(
								BaseItemActivity.ADD_ITEM_BY_TYPING_DIALOG_ID);
				}
			};

			View.OnClickListener scanClickListener = new View.OnClickListener() {
				public void onClick(View view) {
					if (mMultiSelect) {
						showDialog(FINISH_MULTISELECT_DIALOG_ID);
					} else {
						mActivityHelper.getActivity().showDialog(
								BaseItemActivity.ADD_ITEM_BY_SCAN_DIALOG_ID);
					}
				}
			};

			View.OnClickListener filterClickListener = new View.OnClickListener() {
				public void onClick(View view) {
					if (mMultiSelect) {
						showDialog(FINISH_MULTISELECT_DIALOG_ID);
					} else {
						removeDialogSafely(FILTER_DIALOG_ID);
						mActivityHelper.getActivity().showDialog(
								BaseItemActivity.FILTER_DIALOG_ID);
					}
				}
			};

			View.OnClickListener searchClickListener = new View.OnClickListener() {
				public void onClick(View view) {
					if (mMultiSelect) {
						showDialog(FINISH_MULTISELECT_DIALOG_ID);
					} else {
						mActivityHelper.goSearch();
					}
				}
			};

			mActivityHelper.addActionButtonCompat(R.drawable.ic_action_add,
					null, addClickListener, true, true, true);

			mActivityHelper.addActionButtonCompat(R.drawable.ic_action_scan,
					null, scanClickListener, true, true, true);

			mActivityHelper.addActionButtonCompat(R.drawable.ic_action_filter,
					null, filterClickListener, true, true, true);

			mActivityHelper.addActionButtonCompat(R.drawable.ic_action_search,
					null, searchClickListener, true, true, true);
		} else {
			mActivityHelper.setActionBarTitle(getString(itemIdString));
		}
	}

	protected static String setupActionBarTitle(ContentResolver cr,
			String activityToMatch) {
		Cursor c = null;
		Cursor l = null;
		Cursor w = null;

		final Uri uri = findItemUri(activityToMatch);

		c = cr.query(uri, new String[] { BaseItem._ID }, null, null, null);
		l = cr.query(uri, new String[] { BaseItem._ID, BaseItem.LOANED_TO },
				BaseItem.LOANED_TO + " NOT NULL AND " + BaseItem.LOANED_TO
						+ " != ''", null, null);
		w = cr.query(uri,
				new String[] { BaseItem._ID, BaseItem.WISHLIST_DATE },
				BaseItem.WISHLIST_DATE + " NOT NULL AND "
						+ BaseItem.WISHLIST_DATE + " != ''", null, null);

		final String title = "\n" + c.getCount() + " | " + l.getCount() + " / "
				+ w.getCount();

		if (c != null)
			c.close();
		if (l != null)
			l.close();
		if (w != null)
			w.close();

		return title;
	}

	protected FastBitmapDrawable getDefaultCover() {
		return mDefaultCover;
	}

	protected void handleSearchQuery(Intent queryIntent) {
		final String queryAction = queryIntent.getAction();
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			onSearch(queryIntent);
		} else if (Intent.ACTION_VIEW.equals(queryAction)) {
			final Intent viewIntent = new Intent(Intent.ACTION_VIEW,
					queryIntent.getData());
			startActivity(viewIntent);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		mActivityHelper
				.setActionBarTitle((getString(itemIdString) + setupActionBarTitle(
						getContentResolver(), this.toString())));
	}

	protected void onBuy(String detailsUrl) {
		if (TextUtilities.isEmpty(detailsUrl))
			UIUtilities.showToast(this, R.string.cannot_view_manual_item);
		else {
			final Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(detailsUrl));
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Log.e("BrowserNotFound", e.toString());
			}
		}
	}

	protected void onAddManualItem(Activity activity, String type) {
		if (!UIUtilities.isPaid(getContentResolver(), this)) {
			showDialog(PAID_FEATURE_DIALOG_ID);
		} else {
			Intent i = new Intent(this, BaseManualAddActivity.class);
			Bundle b = new Bundle();
			b.putString("type", mActivityHelper.getActivity().toString()
					.toLowerCase());
			i.putExtras(b);
			startActivityForResult(i, REQUEST_ADDING_MANUAL_ITEM);
		}
	}

	protected void onSearch(Intent intent) {
		final String queryString = intent.getStringExtra(SearchManager.QUERY);
		if (mGrid == null)
			mList.setFilterText(queryString);
		else
			mGrid.setFilterText(queryString);
	}

	public boolean isPendingCoversUpdate() {
		return mPendingCoversUpdate;
	}

	// GJT: Mark the save state as dirty to force a refresh
	public void setFromPrefs() {
		mPrefsActivated = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.shelves, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mMultiSelect) {
			showDialog(FINISH_MULTISELECT_DIALOG_ID);
			return false;
		}
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (UIUtilities.isHoneycomb()) {
			if (mMultiSelect) {
				showDialog(FINISH_MULTISELECT_DIALOG_ID);
				return false;
			} else {
				switch (item.getItemId()) {

				case R.id.menu_item_add_from_internet:
					callAddItemFromInternet();
					return true;
				case R.id.menu_item_add_manual:
					callAddItemManually();
					return true;
				case R.id.menu_item_add:
					startScan(REQUEST_SCAN_FOR_ADD);
					return true;
				case R.id.menu_item_add_bulk:
					callScanBulk();
					return true;
				case R.id.menu_item_check:
					startScan(REQUEST_SCAN_FOR_CHECK);
					return true;
				case R.id.menu_item_check_online:
					startScan(REQUEST_SCAN_FOR_CHECK_ONLINE);
					return true;
				case R.id.menu_item_filter:
					removeDialogSafely(FILTER_DIALOG_ID);
					mActivityHelper.getActivity().showDialog(
							BaseItemActivity.FILTER_DIALOG_ID);
					return true;
				case R.id.menu_item_search:
					mActivityHelper.goSearch();
					return true;
				}
			}
		}

		switch (item.getItemId()) {
		case R.id.menu_item_settings:
			onSettings();
			return true;
		case R.id.menu_item_multiselect:
			if (!UIUtilities.isPaid(getContentResolver(), this)) {
				showDialog(PAID_FEATURE_DIALOG_ID);
			} else {
				showDialog(MULTISELECT_DIALOG_ID);
			}

			return true;
		case R.id.menu_item_help:
			onHelp();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	// GJT: Displays help doc
	protected void onHelp() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.setClassName(this, HelpActivity.class.getName());
		startActivity(intent);
	}

	private void onSettings() {
		SettingsActivity.show(this);
	}

	protected void onWishlist(String id) {
		String dateStr = TextUtilities.getCurrentDate();

		ContentValues wishlistValues = new ContentValues();
		wishlistValues.put(BaseItem.WISHLIST_DATE, dateStr);

		getContentResolver().update(findItemUri(this.toString()),
				wishlistValues, BaseItem.INTERNAL_ID + "=?",
				new String[] { id });

		mActivityHelper
				.setActionBarTitle((getString(itemIdString) + setupActionBarTitle(
						getContentResolver(), this.toString())));
		UIUtilities.showToast(getBaseContext(), R.string.wishlist_added_item);
	}

	protected void onWishlistRemove(String id) {
		ContentValues wishlistValues = new ContentValues();
		wishlistValues.put(BaseItem.WISHLIST_DATE, "");

		getContentResolver().update(findItemUri(this.toString()),
				wishlistValues, BaseItem.INTERNAL_ID + "=?",
				new String[] { id });

		mActivityHelper
				.setActionBarTitle((getString(itemIdString) + setupActionBarTitle(
						getContentResolver(), this.toString())));
		UIUtilities.showToast(getBaseContext(), R.string.wishlist_removed_item);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			return onSearchRequested();
		}

		return super.onKeyUp(keyCode, event);
	}

	protected void startScan(int code) {
		if (activityToMatch.contains("BoardGames")
				|| activityToMatch.contains("Comic")) {
			showDialog(UNSUPPORTED_FEATURE_DIALOG_ID);
		} else {
			ScanIntentIntegrator integrator = new ScanIntentIntegrator(this);
			integrator.initiateScan(code);
		}
	}

	protected void prepareBulkFile(String file) {
		this.deleteFile(IOUtilities.getFileName(file));
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(IOUtilities.getExternalFile(file));

			fos.write(BaseItem.EAN.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			IOUtilities.ensureParentCache();
		} catch (IOException e) {
			return;
		}
	}

	protected void onScanAddBulk(ScanIntentResult data, String fileBulkScan) {
		if (ScanIntent.isValidFormat(data.getFormatName())) {
			try {
				FileOutputStream fos = new FileOutputStream(
						IOUtilities.getExternalFile(fileBulkScan), true);

				fos.write(new String("\n" + data.getContents()).getBytes());

				fos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		startScan(REQUEST_SCAN_FOR_ADD_BULK);
	}

	protected void onScanCheckOnline(ScanIntentResult data) {
		if (ScanIntent.isValidFormat(data.getFormatName())) {
			final String id = data.getContents();

			// just find item from amazon
			final String urlString = "http://www.amazon.com/gp/aw/s/ref=is_box_/192-9752328-3344260?k="
					+ id + "&x=16&y=9";
			final Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(urlString));
			startActivity(intent);
		}
	}

	protected void updateCovers() {
		if (viewType.equals(BaseItemActivity.LIST_VIEW_NO_COVER))
			return;

		mPendingCoversUpdate = false;

		final ShelvesView grid = mGrid;
		final ListView list = mList;
		final FastBitmapDrawable cover = mDefaultCover;
		final int count = grid == null ? mList.getChildCount() : mGrid
				.getChildCount();

		for (int i = 0; i < count; i++) {
			final View view = grid == null ? list.getChildAt(i) : grid
					.getChildAt(i);
			final BaseItemViewHolder holder = (BaseItemViewHolder) view
					.getTag();
			if (holder.queryCover) {
				final String id = holder.id;

				FastBitmapDrawable cached = ImageUtilities.getCachedCover(id,
						cover);

				if (grid == null) {
					holder.cover.setImageDrawable(ImageUtilities
							.getCachedCover(id, cached));
				} else {
					CrossFadeDrawable d = holder.transition;
					d.setEnd(cached.getBitmap());
					holder.title.setCompoundDrawablesWithIntrinsicBounds(null,
							null, null, d);
					d.startTransition(COVER_TRANSITION_DURATION);
				}
				holder.queryCover = false;
			}
		}

		if (grid == null)
			mList.invalidate();
		else
			mGrid.invalidate();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		final BaseItemViewHolder holder = (BaseItemViewHolder) info.targetView
				.getTag();

		if (mMultiSelect) {
			UIUtilities
					.showToast(getBaseContext(), R.string.multiselect_verify);
		} else {
			super.onCreateContextMenu(menu, v, menuInfo);

			menu.setHeaderTitle(((TextView) info.targetView
					.findViewById(R.id.title)).getText());

			getMenuInflater().inflate(R.menu.item_context, menu);

			BaseItem i;

			if (activityToMatch.contains("Apparel"))
				i = ApparelManager.findApparel(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this));
			if (activityToMatch.contains("BoardGames"))
				i = BoardGamesManager.findBoardGame(getContentResolver(),
						holder.id, SettingsActivity.getSortOrder(this));
			else if (activityToMatch.contains("Books"))
				i = BooksManager.findBook(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this));
			else if (activityToMatch.contains("Comics"))
				i = ComicsManager.findComic(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this));
			else if (activityToMatch.contains("Gadgets"))
				i = GadgetsManager.findGadget(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this));
			else if (activityToMatch.contains("Movies"))
				i = MoviesManager.findMovie(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this));
			else if (activityToMatch.contains("Music"))
				i = MusicManager.findMusic(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this));
			else if (activityToMatch.contains("Software"))
				i = SoftwareManager.findSoftware(getContentResolver(),
						holder.id, SettingsActivity.getSortOrder(this));
			else if (activityToMatch.contains("Tool"))
				i = ToolsManager.findTool(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this));
			else if (activityToMatch.contains("Toy"))
				i = ToysManager.findToy(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this));
			else
				i = VideoGamesManager.findVideoGame(getContentResolver(),
						holder.id, SettingsActivity.getSortOrder(this));

			Date d = i.getWishlistDate();

			final boolean canLoan = !TextUtilities.isEmpty(i.getLoanedTo());
			final boolean canWishlist = (d != null && !TextUtilities.isEmpty(d
					.toString()));

			if (canLoan) {
				menu.removeItem(R.id.context_menu_item_loan);
				menu.removeItem(R.id.context_menu_item_wishlist);
				menu.removeItem(R.id.context_menu_item_wishlist_remove);
			} else if (canWishlist) {
				menu.removeItem(R.id.context_menu_item_wishlist);
				menu.removeItem(R.id.context_menu_item_loan);
				menu.removeItem(R.id.context_menu_item_loan_return);
				menu.removeItem(R.id.context_menu_item_loan_change);
			} else if (!canLoan && !canWishlist) {
				menu.removeItem(R.id.context_menu_item_wishlist_remove);
				menu.removeItem(R.id.context_menu_item_loan_return);
				menu.removeItem(R.id.context_menu_item_loan_change);
			}

			if (Build.VERSION.SDK_INT < 11) {
				menu.removeItem(R.id.context_menu_item_quantity);
			}
		}
	}

	protected boolean checkContextAction(int actionId, String itemId) {
		switch (actionId) {
		case R.id.context_menu_item_quantity:
			if (!UIUtilities.isPaid(getContentResolver(), this)) {
				showDialog(PAID_FEATURE_DIALOG_ID);
			} else {
				onQuantity(itemId);
			}
			return true;
		case R.id.context_menu_item_tag:
			onTag(itemId);
			return true;
		case R.id.context_menu_item_rate:
			onRate(itemId);
			return true;
		case R.id.context_menu_item_wishlist:
			if (!UIUtilities.isPaid(getContentResolver(), this)) {
				showDialog(PAID_FEATURE_DIALOG_ID);
			} else {
				onWishlist(itemId);
			}
			return true;
		case R.id.context_menu_item_wishlist_remove:
			onWishlistRemove(itemId);
			return true;
		case R.id.context_menu_item_loan:
			onLoan(itemId);
			return true;
		case R.id.context_menu_item_loan_change:
			onLoanChange(itemId);
			return true;
		case R.id.context_menu_item_loan_return:
			onLoanReturn(itemId);
			return true;
		case R.id.context_menu_item_delete:
			onDelete(itemId);
			return true;
		default:
			return false;
		}
	}

	protected void onChangeCover(Activity activity, String id) {
		Intent i = new Intent(activity, LoadImagesActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", id);
		i.putExtras(b);
		startActivityForResult(i, REQUEST_COVER_CHANGE);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Object savedState[] = new Object[3];
		savedState[0] = mMultiSelect;
		savedState[1] = multiSelectIds;
		savedState[2] = mMultiSelectType;

		return savedState;
	}

	protected void onQuantity(String itemId) {
		Intent i = new Intent(this, QuantityActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", itemId);
		b.putString("type", this.toString());
		i.putExtras(b);
		startActivityForResult(i, CHANGE_QUANTITY);
	}

	public static ArrayList<String> getMultiSelectIds() {
		return multiSelectIds;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU && mImportPanel != null
				&& mImportPanel.getVisibility() == View.VISIBLE)
			return true; // GJT: Block menu key on import
		else
			return super.onKeyDown(keyCode, event);
	}

	protected void showPanel(View panel, boolean slideUp) {
		panel.startAnimation(AnimationUtils.loadAnimation(this,
				slideUp ? R.anim.slide_in : R.anim.slide_out_top));
		panel.setVisibility(View.VISIBLE);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		prevOrientation = getRequestedOrientation();

		switch (getResources().getConfiguration().orientation) {
		case Configuration.ORIENTATION_LANDSCAPE:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		case Configuration.ORIENTATION_PORTRAIT:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		default:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			break;
		}
	}

	protected void hidePanel(View panel, boolean slideDown) {
		panel.startAnimation(AnimationUtils.loadAnimation(this,
				slideDown ? R.anim.slide_out : R.anim.slide_in_top));
		panel.setVisibility(View.GONE);

		mActivityHelper
				.setActionBarTitle((getString(itemIdString) + setupActionBarTitle(
						getContentResolver(), this.toString())));
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setRequestedOrientation(prevOrientation);
	}

	protected void dismissPopup() {
		if (mPopup != null) {
			mPopup.dismiss();
		}
	}

	protected void showPopup() {
		if (mPopup == null) {
			PopupWindow p = new PopupWindow(this);
			p.setFocusable(false);
			p.setContentView(mGridPosition);
			p.setWidth(ViewGroup.LayoutParams.FILL_PARENT);
			p.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			p.setBackgroundDrawable(null);

			p.setAnimationStyle(R.style.PopupAnimation);

			mPopup = p;
		}

		if (mGrid != null && mGrid.getWindowVisibility() == View.VISIBLE) {
			mPopup.showAtLocation(mGrid, Gravity.CENTER, 0, 0);
		} else if (mList != null && mList.getWindowVisibility() == View.VISIBLE) {
			mPopup.showAtLocation(mList, Gravity.CENTER, 0, 0);
		}
	}

	protected void onChangeSort(String type, String sortType) {
		SharedPreferences.Editor editor = pref.edit();

		if (type.equals("apparel")) {
			editor.putString(Preferences.KEY_APPAREL_SORT, sortType);
		} else if (type.equals("boardgames")) {
			editor.putString(Preferences.KEY_BOARDGAME_SORT, sortType);
		} else if (type.equals("books")) {
			editor.putString(Preferences.KEY_BOOK_SORT, sortType);
		} else if (type.contains("comics")) {
			editor.putString(Preferences.KEY_COMIC_SORT, sortType);
		} else if (type.contains("gadgets")) {
			editor.putString(Preferences.KEY_GADGET_SORT, sortType);
		} else if (type.contains("movies")) {
			editor.putString(Preferences.KEY_MOVIE_SORT, sortType);
		} else if (type.contains("music")) {
			editor.putString(Preferences.KEY_MUSIC_SORT, sortType);
		} else if (type.contains("software")) {
			editor.putString(Preferences.KEY_SOFTWARE_SORT, sortType);
		} else if (type.contains("tools")) {
			editor.putString(Preferences.KEY_TOOL_SORT, sortType);
		} else if (type.contains("toys")) {
			editor.putString(Preferences.KEY_TOY_SORT, sortType);
		} else if (type.contains("videogames")) {
			editor.putString(Preferences.KEY_VIDEOGAME_SORT, sortType);
		}

		editor.commit();
		setupViews();
	}

	protected void onChangeView(String type, VIEW_TYPE viewType) {
		SharedPreferences.Editor editor = pref.edit();

		switch (viewType) {
		case shelfView:
			if (type.equals("apparel"))
				editor.putString(Preferences.KEY_APPAREL_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("boardgames"))
				editor.putString(Preferences.KEY_BOARDGAME_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("books"))
				editor.putString(Preferences.KEY_BOOK_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("comics"))
				editor.putString(Preferences.KEY_COMIC_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("gadgets"))
				editor.putString(Preferences.KEY_GADGET_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("movies"))
				editor.putString(Preferences.KEY_MOVIE_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("music"))
				editor.putString(Preferences.KEY_MUSIC_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("software"))
				editor.putString(Preferences.KEY_SOFTWARE_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("tools"))
				editor.putString(Preferences.KEY_TOOL_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("toys"))
				editor.putString(Preferences.KEY_TOY_VIEW,
						BaseItemActivity.SHELF_VIEW);
			else if (type.equals("videogames"))
				editor.putString(Preferences.KEY_VIDEOGAME_VIEW,
						BaseItemActivity.SHELF_VIEW);
			break;
		case listView:
			if (type.equals("apparel"))
				editor.putString(Preferences.KEY_APPAREL_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("boardgames"))
				editor.putString(Preferences.KEY_BOARDGAME_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("books"))
				editor.putString(Preferences.KEY_BOOK_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("comics"))
				editor.putString(Preferences.KEY_COMIC_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("gadgets"))
				editor.putString(Preferences.KEY_GADGET_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("movies"))
				editor.putString(Preferences.KEY_MOVIE_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("music"))
				editor.putString(Preferences.KEY_MUSIC_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("software"))
				editor.putString(Preferences.KEY_SOFTWARE_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("tools"))
				editor.putString(Preferences.KEY_TOOL_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("toys"))
				editor.putString(Preferences.KEY_TOY_VIEW,
						BaseItemActivity.LIST_VIEW);
			else if (type.equals("videogames"))
				editor.putString(Preferences.KEY_VIDEOGAME_VIEW,
						BaseItemActivity.LIST_VIEW);
			break;
		case listView_nocover:
			if (type.equals("apparel"))
				editor.putString(Preferences.KEY_APPAREL_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("boardgames"))
				editor.putString(Preferences.KEY_BOARDGAME_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("books"))
				editor.putString(Preferences.KEY_BOOK_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("comics"))
				editor.putString(Preferences.KEY_COMIC_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("gadgets"))
				editor.putString(Preferences.KEY_GADGET_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("movies"))
				editor.putString(Preferences.KEY_MOVIE_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("music"))
				editor.putString(Preferences.KEY_MUSIC_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("software"))
				editor.putString(Preferences.KEY_SOFTWARE_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("tools"))
				editor.putString(Preferences.KEY_TOOL_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("toys"))
				editor.putString(Preferences.KEY_TOY_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			else if (type.equals("videogames"))
				editor.putString(Preferences.KEY_VIDEOGAME_VIEW,
						BaseItemActivity.LIST_VIEW_NO_COVER);
			break;
		}

		editor.commit();
		final Context context = getBaseContext();

		final Intent intent = new Intent(context, MainGridActivity.class);
		intent.putExtra(SOURCE, type);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		finish();
		context.startActivity(intent);
	}

	// Used in all Import Tasks
	protected void addOrUpdate(BaseItem bi, inputTypes mType, String itemName,
			ItemImport currItem) {
		if (bi != null) {
			switch (mType) {
			case boardGameGeekBoardGames:
				bi.addInfo(getContentResolver(), itemName, currItem.title,
						IOUtilities.NO_OP, IOUtilities.NO_OP,
						IOUtilities.NO_OP, currItem.rating, IOUtilities.NO_OP,
						IOUtilities.NO_OP, IOUtilities.NO_OP,
						IOUtilities.NO_OP, currItem.wishlist);
				break;
			case DLApparel:
			case DLBooks:
			case DLGadgets:
			case DLMovies:
			case DLMusic:
			case DLSoftware:
			case DLTools:
			case DLToys:
			case DLVideoGames:
				bi.addInfo(getContentResolver(), itemName, IOUtilities.NO_OP,
						IOUtilities.NO_OP, IOUtilities.NO_OP,
						IOUtilities.NO_OP, currItem.rating, currItem.notes, "",
						"", "", "");
				break;
			case libraryThingBooks:
				bi.addInfo(getContentResolver(), itemName, IOUtilities.NO_OP,
						IOUtilities.NO_OP, IOUtilities.NO_OP, currItem.tags,
						currItem.rating, IOUtilities.NO_OP, "", "", "", "");
				break;
			case mediaManBooks:
			case mediaManMovies:
			case mediaManMusic:
			case mediaManVideoGames:
				final String rating = String.valueOf((Integer
						.parseInt(currItem.rating) / 10));
				bi.addInfo(getContentResolver(), itemName, "", "",
						IOUtilities.NO_OP, currItem.tags, rating, "", "", "",
						"", "");
				break;
			case shelvesApparel:
			case shelvesBoardGames:
			case shelvesBooks:
			case shelvesComics:
			case shelvesGadgets:
			case shelvesMovies:
			case shelvesMusic:
			case shelvesSoftware:
			case shelvesTools:
			case shelvesToys:
			case shelvesVideoGames:
				bi.addInfo(getContentResolver(), itemName, currItem.title,
						currItem.sort_title, currItem.desc, currItem.tags,
						currItem.rating, currItem.notes, currItem.loan_date,
						currItem.loan_to, currItem.event_id, currItem.wishlist);
				break;
			}
		}
	}

	protected void addManualItemsFromImport(Uri uri, int itemNum) {
		ContentValues textValues = new ContentValues();

		String lineObj = ImportUtilities.manualItems.get(itemNum);
		String[] line = lineObj.split("\t");
		final int lineLength = line.length;

		for (int j = 1; j < ImportUtilities.header.length; j++) {
			if (j < lineLength) {
				if (TextUtilities.isEmpty(line[j]))
					textValues.put(ImportUtilities.header[j], "");
				else
					textValues.put(ImportUtilities.header[j], line[j]);
			}
		}

		try {
			getContentResolver().insert(uri, textValues);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}
	}

	protected void applyTagsToMultiSelect(String addedTags) {
		addedTags = TextUtilities.removeBrackets(addedTags);

		ContentResolver cr = getContentResolver();

		Iterator itr = multiSelectIds.iterator();

		if (itr.hasNext()) {

			final Uri uri = ShelvesApplication.TYPES_TO_URI.get(mMultiItemType);

			do {
				Set<String> uniqueTags = new HashSet<String>();

				Cursor c = null;
				String mID = (String) itr.next();

				try {
					c = cr.query(uri, new String[] { BaseItem._ID,
							BaseItem.TAGS }, BaseItem.INTERNAL_ID + "=?",
							new String[] { mID }, null);
					if (c.getCount() > 0) {
						if (c.moveToFirst()) {
							List<String> existingTags = TextUtilities
									.breakString(c.getString(1), ",");
							if (!TextUtilities.isEmpty(existingTags)) {
								for (String existingTag : existingTags) {
									uniqueTags.add(existingTag);
								}
							}

							StringTokenizer tokens = new StringTokenizer(
									addedTags, ", ");

							while (tokens.hasMoreTokens()) {
								uniqueTags.add(tokens.nextToken());
							}
						}
					}
				} finally {
					if (c != null)
						c.close();
				}

				List<String> uniqueTagsList = asSortedList(uniqueTags);
				ContentValues tagValues = new ContentValues();
				tagValues
						.put(BaseItem.TAGS, TextUtilities
								.removeBrackets(uniqueTagsList.toString()));

				cr.update(uri, tagValues, BaseItem.INTERNAL_ID + "=?",
						new String[] { mID });
			} while (itr.hasNext());

			setupViews();
			postApplyMulti();
		}
	}

	protected void applyRateToMultiSelect(int rating) {
		ContentResolver cr = getContentResolver();

		Iterator itr = multiSelectIds.iterator();

		if (itr.hasNext()) {

			final Uri uri = ShelvesApplication.TYPES_TO_URI.get(mMultiItemType);

			do {
				String mID = (String) itr.next();

				ContentValues rateValue = new ContentValues();
				rateValue.put(BaseItem.RATING, rating);

				cr.update(uri, rateValue, BaseItem.INTERNAL_ID + "=?",
						new String[] { mID });
			} while (itr.hasNext());

			postApplyMulti();
		}
	}

	protected void applyDeleteToMultiSelect() {
		Iterator itr = multiSelectIds.iterator();

		if (itr.hasNext()) {
			do {
				String mID = (String) itr.next();
				onDelete(mID);
			} while (itr.hasNext());

			postApplyMulti();
		}
	}

	private void postApplyMulti() {
		multiSelectIds = new ArrayList<String>();
		mMultiSelect = false;
	}

	public static <T extends Comparable<? super T>> List<T> asSortedList(
			Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PAID_FEATURE_DIALOG_ID:
			return new AlertDialog.Builder(this)
					.setMessage(R.string.support_the_dev)
					.setPositiveButton(R.string.okay_label,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									if (!TextUtilities
											.isEmpty(mMultiSelectType)) {
										mMultiSelect = true;
										dialog.dismiss();
										setupViews();
									}
								}
							}).create();
		case MULTISELECT_DIALOG_ID:
			final String[] items = {
					getString(R.string.context_menu_item_tag_label),
					getString(R.string.context_menu_item_rate_label),
					getString(R.string.context_menu_item_delete_label) };

			return new AlertDialog.Builder(this)
					.setTitle(R.string.multiselect_type_title)
					.setSingleChoiceItems(items, -1,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									mMultiSelectType = items[item];
								}
							})
					.setPositiveButton(R.string.okay_label,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									if (!TextUtilities
											.isEmpty(mMultiSelectType)) {
										mMultiSelect = true;
										setupViews();
										dialog.dismiss();
									}
								}
							})
					.setNegativeButton(R.string.cancel_label,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mMultiSelect = false;
									dialog.dismiss();
								}
							}).create();
		case FINISH_MULTISELECT_DIALOG_ID:
			return new AlertDialog.Builder(this)
					.setTitle(R.string.multiselect_finish)
					.setPositiveButton(R.string.okay_label,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									if (multiSelectIds == null
											|| multiSelectIds.isEmpty()
											|| multiSelectIds.size() == 0) {
										mMultiSelect = false;
										dialog.dismiss();
									} else {
										String firstId = multiSelectIds.get(0);

										if (mMultiSelectType
												.equals(getString(R.string.context_menu_item_tag_label))
												&& firstId != null) {
											Intent i = new Intent(
													BaseItemActivity.this,
													TagActivity.class);
											Bundle b = new Bundle();
											b.putString("itemID", firstId);
											b.putString(
													"title",
													getString(R.string.menu_item_multiselect_label));
											b.putString("type", mMultiItemType);
											i.putExtras(b);
											startActivityForResult(i,
													REQUEST_ADDING_MULTI_TAGS);
										} else if (mMultiSelectType
												.equals(getString(R.string.context_menu_item_rate_label))
												&& firstId != null) {
											Intent i = new Intent(
													BaseItemActivity.this,
													RateActivity.class);
											Bundle b = new Bundle();
											b.putString("itemID", firstId);
											b.putString(
													"title",
													getString(R.string.menu_item_multiselect_label));
											b.putString("type", mMultiItemType);
											i.putExtras(b);
											startActivityForResult(i,
													REQUEST_ADDING_MULTI_RATE);
										} else if (mMultiSelectType
												.equals(getString(R.string.context_menu_item_delete_label))
												&& firstId != null) {
											applyDeleteToMultiSelect();
										}

										mMultiSelect = false;
										dialog.dismiss();
									}
								}
							})
					.setNegativeButton(R.string.cancel_label,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).create();
		case FILTER_DIALOG_ID:
			constructTagList();
			return createFilterDialog(id);
		case ADD_ITEM_BY_TYPING_DIALOG_ID:
			final String[] add_choices = {
					getString(R.string.menu_item_add_from_internet_label),
					getString(R.string.menu_item_add_manually_label) };

			return new AlertDialog.Builder(this)
					.setTitle(R.string.add_label)
					.setItems(add_choices,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									switch (item) {
									case 0:
										callAddItemFromInternet();
										dismissDialog(ADD_ITEM_BY_TYPING_DIALOG_ID);
										break;
									case 1:
										callAddItemManually();
										dismissDialog(ADD_ITEM_BY_TYPING_DIALOG_ID);
										break;
									}
								}
							}).create();
		case ADD_ITEM_BY_SCAN_DIALOG_ID:
			final String[] scan_choices = {
					getString(R.string.menu_item_add_label),
					getString(R.string.menu_item_add_bulk_label),
					getString(R.string.menu_item_check_label),
					getString(R.string.menu_item_check_online_label) };

			return new AlertDialog.Builder(this)
					.setTitle(R.string.menu_item_scan_label)
					.setItems(scan_choices,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									switch (item) {
									case 0:
										startScan(REQUEST_SCAN_FOR_ADD);
										dismissDialog(ADD_ITEM_BY_SCAN_DIALOG_ID);
										break;
									case 1:
										callScanBulk();
										dismissDialog(ADD_ITEM_BY_SCAN_DIALOG_ID);
										break;
									case 2:
										startScan(REQUEST_SCAN_FOR_CHECK);
										dismissDialog(ADD_ITEM_BY_SCAN_DIALOG_ID);
										break;
									case 3:
										startScan(REQUEST_SCAN_FOR_CHECK_ONLINE);
										dismissDialog(ADD_ITEM_BY_SCAN_DIALOG_ID);
										break;
									}
								}
							}).create();
		case SORT_DIALOG_ID:
			sortDialog = new AlertDialog.Builder(this)
					.setTitle(R.string.menu_item_sort_label)
					.setSingleChoiceItems(
							sortTypes
									.toArray(new CharSequence[sortTypes.size()]),
							-1, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									sortCount++;
									sortPhrase.append(sortNames.get(which));
									if (sortCount == sortTimes) {
										if (activityToMatch.contains("Apparel")) {
											onChangeSort("apparel",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("BoardGames")) {
											onChangeSort("boardgames",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("Books")) {
											onChangeSort("books",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("Comics")) {
											onChangeSort("comics",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("Gadgets")) {
											onChangeSort("gadgets",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("Movies")) {
											onChangeSort("movies",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("Music")) {
											onChangeSort("music",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("Software")) {
											onChangeSort("software",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("Tools")) {
											onChangeSort("tools",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("Toys")) {
											onChangeSort("toys",
													sortPhrase.toString());
										} else if (activityToMatch
												.contains("VideoGames")) {
											onChangeSort("videogames",
													sortPhrase.toString());
										}

										BaseItemActivity.sortCount = 0;
										dialog.dismiss();

										sortCount = 0;
										sortPhrase = new StringBuilder();
									} else {
										sortPhrase.append(", ");
										dialog.dismiss();
										new Handler().postDelayed(
												new Runnable() {
													public void run() {
														showDialog(SORT_DIALOG_ID);
													}
												}, 800);
									}
								}
							}).create();
			return sortDialog;
		case UNSUPPORTED_FEATURE_DIALOG_ID:
			return UIUtilities.createUnsupportedDialog(this);
		case SUPPORT_THE_DEV:
			return new AlertDialog.Builder(this)
					.setMessage(R.string.a_plea)
					.setPositiveButton(R.string.zxing_go_get,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									try {
										startActivity(new Intent(
												Intent.ACTION_VIEW,
												Uri.parse("market://search?q=pname:com.miadzin.shelves.unlocker")));
										finish();
									} catch (ActivityNotFoundException e) {
										Log.e("MarketNotFound", e.toString());
										UIUtilities.showToast(getBaseContext(),
												R.string.error_no_market);
									}
									dialog.dismiss();
								}
							})
					.setNegativeButton(R.string.main_grid_ok_button,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).create();
		}

		return super.onCreateDialog(id);
	}

	// GJT: Using these as overrides in Action Bar
	abstract protected void callAddItemFromInternet();

	abstract protected void callAddItemManually();

	abstract protected void callScanBulk();

	protected void constructTagList() {
		mFilterTags = new ArrayList<String>();

		Cursor c = null;

		final Uri uri = findItemUri(this.toString());
		c = getContentResolver().query(uri,
				new String[] { BaseItem.INTERNAL_ID, BaseItem.TAGS },
				BaseItem.TAGS + " NOT NULL AND " + BaseItem.TAGS + " != ''",
				null, null);

		if (c.moveToFirst()) {
			do {
				for (String tag : c.getString(1).split(",")) {
					tag = tag.trim();
					if (!TextUtilities.isEmpty(tag)
							&& !mFilterTags.contains(tag))
						mFilterTags.add(tag);
				}
			} while (c.moveToNext());
		}

		if (c != null)
			c.close();

		Collections.sort(mFilterTags);
	}

	protected Dialog createFilterDialog(int id) {
		if (TextUtilities.isEmpty(mFilterTags)) {
			return new AlertDialog.Builder(this)
					.setTitle(R.string.tag_filter_title)
					.setMessage(R.string.tags_none)
					.setNeutralButton(R.string.okay_label,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).create();
		}

		final String[] items = mFilterTags.toArray(new String[mFilterTags
				.size()]);
		final List<String> selected = new ArrayList<String>();

		return new AlertDialog.Builder(this)
				.setTitle(R.string.tag_filter_title)
				.setMultiChoiceItems(items, null,
						new DialogInterface.OnMultiChoiceClickListener() {
							public void onClick(
									DialogInterface dialogInterface, int pos,
									boolean state) {
								if (state)
									selected.add(items[pos]);
								else
									selected.remove(items[pos]);
							}
						})
				.setPositiveButton(getString(R.string.menu_item_filter_label),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								final String selectedString = selected
										.toString();
								if (mGrid != null)
									mGrid.setFilterText(selectedString
											.substring(1,
													selectedString.length() - 1));
								else
									mList.setFilterText(selectedString
											.substring(1,
													selectedString.length() - 1));
								dialog.dismiss();
							}
						})
				.setNegativeButton(getString(R.string.cancel_label),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						}).create();
	}

	private String findItemViewType(final String activityToMatch) {
		if (activityToMatch.contains("Apparel"))
			return Preferences.KEY_APPAREL_VIEW;
		if (activityToMatch.contains("BoardGames"))
			return Preferences.KEY_BOARDGAME_VIEW;
		else if (activityToMatch.contains("Books"))
			return Preferences.KEY_BOOK_VIEW;
		else if (activityToMatch.contains("Comics"))
			return Preferences.KEY_COMIC_VIEW;
		else if (activityToMatch.contains("Gadgets"))
			return Preferences.KEY_GADGET_VIEW;
		else if (activityToMatch.contains("Movies"))
			return Preferences.KEY_MOVIE_VIEW;
		else if (activityToMatch.contains("Music"))
			return Preferences.KEY_MUSIC_VIEW;
		else if (activityToMatch.contains("Software"))
			return Preferences.KEY_SOFTWARE_VIEW;
		else if (activityToMatch.contains("Tools"))
			return Preferences.KEY_TOOL_VIEW;
		else if (activityToMatch.contains("Toys"))
			return Preferences.KEY_TOY_VIEW;
		else if (activityToMatch.contains("VideoGames"))
			return Preferences.KEY_VIDEOGAME_VIEW;
		else
			return null;
	}

	protected void onExport(IOUtilities.outputTypes type, boolean massExport) {
		if (mExportTask == null
				|| mExportTask.getStatus() == ExportTask.Status.FINISHED) {
			mExportTask = (ExportTask) new ExportTask(type, massExport)
					.execute();
		} else {
			UIUtilities.showToast(this, R.string.error_export_in_progress);
		}
	}

	public class ExportTask extends AsyncTask<Void, Integer, Integer> {
		private ContentResolver mResolver;
		public IOUtilities.outputTypes mType;
		private Boolean isMassExport = false;

		public final AtomicInteger mExportCount = new AtomicInteger();
		public ArrayList<String> collectionItems;

		public ExportTask(IOUtilities.outputTypes type, boolean massExport) {
			mType = type;

			if (massExport)
				isMassExport = true;
		}

		public IOUtilities.outputTypes getType() {
			return mType;
		}

		public Boolean getIsMassExport() {
			return isMassExport;
		}

		public ExportTask(ArrayList<String> items, int index, String exportType) {
			collectionItems = items;
			mExportCount.set(index);
			mType = IOUtilities.outputTypes.valueOf(exportType);
		}

		@Override
		public void onPreExecute() {
			if (mExportPanel == null) {
				mExportPanel = ((ViewStub) findViewById(R.id.stub_export))
						.inflate();
				((ProgressBar) mExportPanel.findViewById(R.id.progress))
						.setIndeterminate(true);

				((TextView) mExportPanel.findViewById(R.id.label_import))
						.setText(getText(R.string.export_label));

				final View cancelButton = mExportPanel
						.findViewById(R.id.button_cancel);
				cancelButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						onCancelExport();
					}
				});
			}

			mResolver = getContentResolver();

			showPanel(mExportPanel, true);
		}

		@Override
		public Integer doInBackground(Void... params) {
			int status = 0;

			try {

				status = ExportUtilities.exportItems(mType, mResolver) ? 1 : 0;

			} catch (IOException e) {
				return null;
			}

			return status;
		}

		@Override
		public void onCancelled() {
			hidePanel(mExportPanel, true);
		}

		@Override
		public void onPostExecute(Integer status) {
			if (status == 0 && !isMassExport) {
				UIUtilities.showToast(getBaseContext(),
						R.string.error_missing_export_file);
			} else {
				UIUtilities.showFormattedToast(getBaseContext(),
						R.string.success_exported, status);
			}
			hidePanel(mExportPanel, true);

			if (isMassExport) {
				finish();
			}
		}
	}

	protected void onCancelExport() {
		if (mExportTask != null
				&& mExportTask.getStatus() == AsyncTask.Status.RUNNING) {
			mExportTask.cancel(true);
			mExportTask = null;
		}
	}

	static public Uri findItemUri(final String activityToMatch) {
		return BaseItemProvider.getActivityUri(activityToMatch);
	}

	public class FingerTracker implements View.OnTouchListener {
		public boolean onTouch(View view, MotionEvent event) {
			final int action = event.getAction();
			mFingerUp = action == MotionEvent.ACTION_UP
					|| action == MotionEvent.ACTION_CANCEL;
			if (mFingerUp) { // GJT: Removed some code, due to a bug:
								// http://code.google.com/p/android/issues/detail?id=4260
								// && mScrollState !=
								// ShelvesScrollManager.SCROLL_STATE_FLING
				postUpdateCovers();
			}
			return false;
		}
	}

	public class ItemViewer implements AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			final String stringId = ((BaseItemViewHolder) view.getTag()).id;

			if (!mMultiSelect)
				onView(stringId);
			else {
				mMultiSelectId = stringId;

				CheckBox multiSelect = ((BaseItemViewHolder) view.getTag()).mMultiselectCheckbox;

				multiSelect.setVisibility(View.VISIBLE);
				multiSelect.setEnabled(true);
				multiSelect.setChecked(true);
				multiSelect.bringToFront();
			}
		}
	}

	public void removeDialogSafely(final int id) {

		runOnUiThread(new Runnable() {
			public void run() {
				try {
					removeDialog(id);
				} catch (IllegalArgumentException e) {
					// This will be thrown if this dialog was not shown before.
				}
			}
		});
	}
}