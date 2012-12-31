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

package com.miadzin.shelves.activity.movies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miadzin.shelves.R;
import com.miadzin.shelves.activity.LoanActivity;
import com.miadzin.shelves.activity.RateActivity;
import com.miadzin.shelves.activity.SettingsActivity;
import com.miadzin.shelves.activity.TagActivity;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.base.BaseItemActivity;
import com.miadzin.shelves.base.BaseItemAdapter;
import com.miadzin.shelves.base.BaseItemProvider;
import com.miadzin.shelves.base.BaseItemViewHolder;
import com.miadzin.shelves.provider.ItemImport;
import com.miadzin.shelves.provider.movies.MoviesManager;
import com.miadzin.shelves.provider.movies.MoviesStore;
import com.miadzin.shelves.provider.movies.MoviesStore.Movie;
import com.miadzin.shelves.provider.movies.MoviesUpdater;
import com.miadzin.shelves.scan.ScanIntent;
import com.miadzin.shelves.scan.ScanIntentIntegrator;
import com.miadzin.shelves.scan.ScanIntentResult;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.IOUtilities.inputTypes;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.ImportResults;
import com.miadzin.shelves.util.ImportUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;
import com.miadzin.shelves.util.loan.Calendars;
import com.miadzin.shelves.view.ShelvesView;

public class MoviesActivity extends BaseItemActivity {
	private static final String LOG_TAG = "MoviesActivity";

	private static final int MESSAGE_UPDATE_MOVIE_COVERS = 1;
	private static final int DELAY_SHOW_MOVIE_COVERS = 550;

	private static final String ACTION_IMPORT_BULK_MOVIES = "shelves.intent.action.ACTION_IMPORT_BULK_MOVIES";
	private static final String ACTION_IMPORT_DL_MOVIES = "shelves.intent.action.ACTION_IMPORT_DL_MOVIES";
	private static final String ACTION_IMPORT_MEDIAMAN_MOVIES = "shelves.intent.action.ACTION_IMPORT_MEDIAMAN_MOVIES";
	private static final String ACTION_IMPORT_SHELVES_MOVIES = "shelves.intent.action.ACTION_IMPORT_SHELVES_MOVIES";
	public static final String ACTION_IMPORT_LIST_OF_MOVIES = "shelves.intent.action.ACTION_IMPORT_LIST_OF_MOVIES";

	private static final String ACTION_EXPORT_DL_MOVIES = "shelves.intent.action.ACTION_EXPORT_DL_MOVIES";
	private static final String ACTION_EXPORT_MEDIAMAN_MOVIES = "shelves.intent.action.ACTION_EXPORT_MEDIAMAN_MOVIES";
	public static final String ACTION_EXPORT_SHELVES_MOVIES = "shelves.intent.action.ACTION_EXPORT_SHELVES_MOVIES";

	private static final String STATE_IMPORT_MOVIES = "shelves.import.movies";
	private static final String STATE_EXPORT_MOVIES = "shelves.export.movies";
	private static final String STATE_ADD_MOVIE = "shelves.add.movies";

	private static final int MENU_ITEM_SORT_LABEL_ASC = 8;
	private static final int MENU_ITEM_SORT_LABEL_DESC = 9;
	private static final int MENU_ITEM_SORT_FORMAT_ASC = 10;
	private static final int MENU_ITEM_SORT_FORMAT_DESC = 11;
	private static final int MENU_ITEM_SORT_AUDIENCE_ASC = 12;
	private static final int MENU_ITEM_SORT_AUDIENCE_DESC = 13;

	private ImportTask mImportTask;
	private ExportTask mExportTask;
	private IOUtilities.inputTypes mSavedImportType;
	private IOUtilities.outputTypes mSavedExportType;
	private AddTask mAddTask;

	private MoviesUpdater mMoviesUpdater;

	private final Handler mScrollHandler = new ScrollHandler();
	private int mScrollState = ShelvesScrollManager.SCROLL_STATE_IDLE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMoviesUpdater = new MoviesUpdater(this);

		setupViews();
		handleSearchQuery(getIntent());

		onNewIntent(getIntent());
	}

	int getScrollState() {
		return mScrollState;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		final String action = intent.getAction();
		if (Intent.ACTION_SEARCH.equals(action)) {
			onSearch(intent);
		} else if (Intent.ACTION_VIEW.equals(action)) {
			final Intent viewIntent = new Intent(Intent.ACTION_VIEW,
					intent.getData());
			startActivity(viewIntent);
		} else if (ACTION_IMPORT_BULK_MOVIES.equals(action)) {
			onImport(IOUtilities.inputTypes.bulkScanMovies);
		} else if (ACTION_IMPORT_DL_MOVIES.equals(action)) {
			onImport(IOUtilities.inputTypes.DLMovies);
		} else if (ACTION_IMPORT_MEDIAMAN_MOVIES.equals(action)) {
			onImport(IOUtilities.inputTypes.mediaManMovies);
		} else if (ACTION_IMPORT_SHELVES_MOVIES.equals(action)) {
			onImport(IOUtilities.inputTypes.shelvesMovies);
		} else if (ACTION_IMPORT_LIST_OF_MOVIES.equals(action)) {
			onImport(IOUtilities.inputTypes.listOfMovies);
		} else if (ACTION_EXPORT_DL_MOVIES.equals(action)) {
			onExport(IOUtilities.outputTypes.DLMovies, false);
		} else if (ACTION_EXPORT_MEDIAMAN_MOVIES.equals(action)) {
			onExport(IOUtilities.outputTypes.mediaManMovies, false);
		} else if (ACTION_EXPORT_SHELVES_MOVIES.equals(action)) {
			final boolean massExport;
			if (intent.getExtras() != null) {
				massExport = intent.getExtras().getBoolean(
						SettingsActivity.KEY_MASSEXPORT);
			} else {
				massExport = false;
			}
			onExport(IOUtilities.outputTypes.shelvesMovies, massExport);
		}
	}

	@Override
	protected void setupViews() {
		final String sortOrder = SettingsActivity.getSortOrder(this);
		final BaseItemAdapter adapter = new BaseItemAdapter(this,
				this.managedQuery(MoviesStore.Movie.CONTENT_URI,
						BaseItemProvider.MOVIES_PROJECTION_IDS_AND_TITLE, null,
						null, sortOrder), true, sortOrder, viewType);

		if (viewType.equals(BaseItemActivity.SHELF_VIEW)) {
			mDefaultCover = adapter.getDefaultCover();

			mGrid = (ShelvesView) findViewById(R.id.grid_shelves);

			final ShelvesView grid = mGrid;

			grid.setTextFilterEnabled(true);
			grid.setAdapter(adapter);
			grid.setOnScrollListener(new ShelvesScrollManager());
			grid.setOnTouchListener(new FingerTracker());
			grid.setOnItemSelectedListener(new SelectionTracker());
			grid.setOnItemClickListener(new ItemViewer());

			registerForContextMenu(grid);
		} else if (viewType.equals(BaseItemActivity.LIST_VIEW)) {
			mDefaultCover = adapter.getDefaultCover();

			mList = (ListView) findViewById(android.R.id.list);

			final ListView list = mList;

			list.setTextFilterEnabled(true);
			list.setAdapter(adapter);
			list.setOnScrollListener(new ShelvesScrollManager());
			list.setOnTouchListener(new FingerTracker());
			list.setOnItemSelectedListener(new SelectionTracker());
			list.setOnItemClickListener(new ItemViewer());
			list.setFocusableInTouchMode(true);
			list.setFocusable(true);

			registerForContextMenu(list);
		} else if (viewType.equals(BaseItemActivity.LIST_VIEW_NO_COVER)) {
			mList = (ListView) findViewById(android.R.id.list);

			final ListView list = mList;

			list.setTextFilterEnabled(true);
			list.setAdapter(adapter);
			list.setOnScrollListener(new ShelvesScrollManager());
			list.setOnTouchListener(new FingerTracker());
			list.setOnItemSelectedListener(new SelectionTracker());
			list.setOnItemClickListener(new ItemViewer());
			list.setFocusableInTouchMode(true);
			list.setFocusable(true);

			registerForContextMenu(list);
		}
		postSetupViews();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!viewType.equals(BaseItemActivity.LIST_VIEW_NO_COVER))
			mMoviesUpdater.start();

		if (mSavedState != null && !mPrefsActivated)
			restoreLocalState(mSavedState);
		// GJT: Force a refresh when coming back from the settings menu;
		// this is needed for sorting (though might not be optimal)
		else if (mPrefsActivated || mEverythingDeleted) {
			mPrefsActivated = false;
			mEverythingDeleted = false;
			mPopup = null; // GJT: Popup cleared to enable scrolling dialog
			setupViews();
		}

		postUpdateCovers();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopMoviesUpdater();
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopMoviesUpdater();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		dismissPopup();

		stopMoviesUpdater();

		onCancelAdd();
		onCancelImport();
		onCancelExport();

		ImageUtilities.cleanupCache();
	}

	private void stopMoviesUpdater() {
		final MoviesUpdater moviessUpdater = mMoviesUpdater;
		moviessUpdater.clear();
		moviessUpdater.stop();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		restoreLocalState(savedInstanceState);
		mSavedState = null;
	}

	private void restoreLocalState(Bundle savedInstanceState) {
		try {
			restoreAddTask(savedInstanceState);
			restoreImportTask(savedInstanceState);
			restoreExportTask(savedInstanceState);
		} catch (NullPointerException npe) {
			Log.w(LOG_TAG, npe.toString());
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveAddTask(outState);
		saveImportTask(outState);
		saveExportTask(outState);
		mSavedState = outState;
	}

	private void saveAddTask(Bundle outState) {
		final AddTask task = mAddTask;
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			final String moviesId = task.getMovieId();
			task.cancel(true);

			if (moviesId != null) {
				outState.putBoolean(STATE_ADD_IN_PROGRESS, true);
				outState.putString(STATE_ADD_MOVIE, moviesId);
			}

			mAddTask = null;
		}
	}

	private void restoreAddTask(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(STATE_ADD_IN_PROGRESS)) {
			final String id = savedInstanceState.getString(STATE_ADD_MOVIE);
			if (!MoviesManager.movieExists(getContentResolver(), id,
					SettingsActivity.getSortOrder(this), null)) {
				mAddTask = (AddTask) new AddTask().execute(id);
			}
		}
	}

	private void saveImportTask(Bundle outState) {
		final ImportTask task = mImportTask;
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			task.cancel(true);

			outState.putBoolean(STATE_IMPORT_IN_PROGRESS, true);
			outState.putParcelableArrayList(STATE_IMPORT_MOVIES, task.mMovies);
			outState.putInt(STATE_IMPORT_INDEX, task.mImportCount.get());

			mImportTask = null;
			mSavedImportType = task.getType();
		}
	}

	private void restoreImportTask(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(STATE_IMPORT_IN_PROGRESS)) {
			ArrayList<ItemImport> movies = savedInstanceState
					.getParcelableArrayList(STATE_IMPORT_MOVIES);
			int index = savedInstanceState.getInt(STATE_IMPORT_INDEX);
			String inputType = mSavedImportType.toString();

			if (movies != null) {
				if (index < movies.size()) {
					mImportTask = (ImportTask) new ImportTask(movies, index,
							inputType).execute();
				}
			} else {
				mImportTask = (ImportTask) new ImportTask(mSavedImportType)
						.execute();
			}
		}
	}

	private void saveExportTask(Bundle outState) {
		final ExportTask task = mExportTask;
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			task.cancel(true);

			outState.putBoolean(STATE_EXPORT_IN_PROGRESS, true);
			outState.putStringArrayList(STATE_EXPORT_MOVIES,
					task.collectionItems);
			outState.putInt(STATE_EXPORT_INDEX, task.mExportCount.get());
			outState.putString(STATE_EXPORT_TYPE, task.mType.toString());

			mExportTask = null;
			mSavedExportType = task.getType();
		}
	}

	private void restoreExportTask(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(STATE_EXPORT_IN_PROGRESS)) {
			ArrayList<String> movies = savedInstanceState
					.getStringArrayList(STATE_EXPORT_MOVIES);
			int index = savedInstanceState.getInt(STATE_EXPORT_INDEX);
			String exportType = savedInstanceState.getString(STATE_IMPORT_TYPE);

			if (movies != null) {
				if (index < movies.size()) {
					mExportTask = (ExportTask) new ExportTask(movies, index,
							exportType).execute();
				}
			} else {
				mExportTask = (ExportTask) new ExportTask(mSavedExportType,
						savedInstanceState
								.getBoolean(SettingsActivity.KEY_MASSEXPORT))
						.execute();
			}
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		final boolean baseMenuSelection = super.onMenuItemSelected(featureId,
				item);
		if (!baseMenuSelection) {
			final int itemId = item.getItemId();
			switch (itemId) {
			case R.id.menu_item_sort:
				sortTypes
						.add(getString(R.string.preferences_sort_audience_asc_label));
				sortTypes
						.add(getString(R.string.preferences_sort_audience_desc_label));
				sortTypes
						.add(getString(R.string.preferences_sort_format_asc_label));
				sortTypes
						.add(getString(R.string.preferences_sort_format_desc_label));
				sortTypes
						.add(getString(R.string.preferences_sort_label_asc_label));
				sortTypes
						.add(getString(R.string.preferences_sort_label_desc_label));

				sortNames.add("audience COLLATE NOCASE asc");
				sortNames.add("audience COLLATE NOCASE desc");
				sortNames.add("format COLLATE NOCASE asc");
				sortNames.add("format COLLATE NOCASE desc");
				sortNames.add("label COLLATE NOCASE asc");
				sortNames.add("label COLLATE NOCASE desc");

				showDialog(SORT_DIALOG_ID);

				return true;
			case R.id.menu_item_view_shelf:
				onChangeView("movies", VIEW_TYPE.shelfView);
				return true;
			case R.id.menu_item_view_list:
				onChangeView("movies", VIEW_TYPE.listView);
				return true;
			case R.id.menu_item_view_list_nocover:
				onChangeView("movies", VIEW_TYPE.listView_nocover);
				return true;
			}
			return super.onMenuItemSelected(featureId, item);
		}
		return false; // GJT: Should never happen
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		final BaseItemViewHolder holder = (BaseItemViewHolder) info.targetView
				.getTag();

		boolean contextParent = checkContextAction(item.getItemId(), holder.id);

		if (!contextParent) {
			switch (item.getItemId()) {
			case R.id.context_menu_item_buy:
				onBuy(MoviesManager.findMovie(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this)).getDetailsUrl());
				return true;
			case R.id.context_menu_item_change_cover:
				onChangeCover(MoviesActivity.this, holder.id);
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		} else {
			return true;
		}
	}

	@Override
	protected void callAddItemFromInternet() {
		onAddMovieSearch();
	}

	@Override
	protected void callAddItemManually() {
		onAddManualItem(this, "movies");
	}

	@Override
	protected void callScanBulk() {
		prepareBulkFile(IOUtilities.FILE_BULK_SCAN_MOVIES);
		startScan(REQUEST_SCAN_FOR_ADD_BULK);
	}

	@Override
	protected void onView(String moviesId) {
		MovieDetailsActivity.show(this, moviesId);
	}

	@Override
	protected void onTag(String movieId) {
		Intent i = new Intent(MoviesActivity.this, TagActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", movieId);
		b.putString(
				"title",
				MoviesManager.findMovie(getContentResolver(), movieId,
						SettingsActivity.getSortOrder(this)).getTitle());
		b.putString("type", getString(R.string.movie_label_plural_small));
		i.putExtras(b);
		startActivityForResult(i, REQUEST_ADDING_TAGS);
	}

	@Override
	protected void onRate(String movieId) {
		Intent i = new Intent(MoviesActivity.this, RateActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", movieId);
		b.putString(
				"title",
				MoviesManager.findMovie(getContentResolver(), movieId,
						SettingsActivity.getSortOrder(this)).getTitle());
		b.putString("type", getString(R.string.movie_label_plural_small));
		i.putExtras(b);
		startActivityForResult(i, REQUEST_RATE);
	}

	@Override
	protected void onLoan(String movieId) {
		Intent i = new Intent(MoviesActivity.this, LoanActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", movieId);
		b.putString(
				"title",
				MoviesManager.findMovie(getContentResolver(), movieId,
						SettingsActivity.getSortOrder(this)).getTitle());
		b.putString("type", getString(R.string.movie_label_plural_small));
		b.putString("action", "loan");
		i.putExtras(b);
		startActivityForResult(i, REQUEST_LOAN);
	}

	@Override
	protected void onLoanReturn(String movieId) {
		ContentResolver cr = getContentResolver();
		ContentValues loanValues = new ContentValues();

		Movie movie = MoviesManager.findMovie(getContentResolver(), movieId,
				null);

		if (movie.getEventId() > 0) {
			Calendars.setupCalendarUri();
			cr.delete(
					Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString() + "/"
							+ movie.getEventId()), null, null);
		}
		loanValues.put(BaseItem.LOANED_TO, "");
		loanValues.put(BaseItem.LOAN_DATE, "");
		loanValues.put(BaseItem.EVENT_ID, -1);

		cr.update(MoviesStore.Movie.CONTENT_URI, loanValues,
				BaseItem.INTERNAL_ID + "=?", new String[] { movieId });

		mActivityHelper
				.setActionBarTitle((getString(itemIdString) + setupActionBarTitle(
						getContentResolver(), this.toString())));
		UIUtilities.showToast(getBaseContext(), R.string.loan_returned);
	}

	@Override
	protected void onLoanChange(String movieId) {
		Intent i = new Intent(MoviesActivity.this, LoanActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", movieId);
		b.putString(
				"title",
				MoviesManager.findMovie(getContentResolver(), movieId,
						SettingsActivity.getSortOrder(this)).getTitle());
		b.putString("type", getString(R.string.movie_label_plural_small));
		b.putString("action", "change");
		i.putExtras(b);
		startActivityForResult(i, REQUEST_LOAN_CHANGE);
	}

	@Override
	protected void onDelete(String movieId) {
		if (MoviesManager.deleteMovie(getContentResolver(), movieId)) {
			mActivityHelper
					.setActionBarTitle((getString(itemIdString) + setupActionBarTitle(
							getContentResolver(), this.toString())));
			UIUtilities.showToast(
					this,
					getString(R.string.success_item_deleted,
							getString(R.string.movie_label_single_big)));
		}
	}

	private void onAddMovieSearch() {
		AddMovieActivity.show(this);
	}

	private void onImport(IOUtilities.inputTypes type) {
		if (mImportTask == null
				|| mImportTask.getStatus() == ImportTask.Status.FINISHED) {
			mImportTask = (ImportTask) new ImportTask(type).execute();
		} else {
			UIUtilities.showToast(this, R.string.error_import_in_progress);
		}
	}

	private void onCancelAdd() {
		if (mAddTask != null
				&& mAddTask.getStatus() == AsyncTask.Status.RUNNING) {
			mAddTask.cancel(true);
			mAddTask = null;
		}
	}

	private void onCancelImport() {
		if (mImportTask != null
				&& mImportTask.getStatus() == AsyncTask.Status.RUNNING) {
			mImportTask.cancel(true);
			mImportTask = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		switch (resultCode) {
		case RESULT_OK:
			ScanIntentResult scanResult;
			switch (requestCode) {
			case REQUEST_SCAN_FOR_ADD:
				scanResult = ScanIntentIntegrator.parseActivityResult(
						requestCode, resultCode, intent);
				if (scanResult != null) {
					onScanAdd(scanResult);
				}
				break;
			case REQUEST_SCAN_FOR_ADD_BULK:
				scanResult = ScanIntentIntegrator.parseActivityResult(
						requestCode, resultCode, intent);
				if (scanResult != null) {
					onScanAddBulk(scanResult, IOUtilities.FILE_BULK_SCAN_MOVIES);
				}
				break;
			case REQUEST_SCAN_FOR_CHECK:
				scanResult = ScanIntentIntegrator.parseActivityResult(
						requestCode, resultCode, intent);
				if (scanResult != null) {
					onScanCheck(scanResult);
				}
				break;
			case REQUEST_SCAN_FOR_CHECK_ONLINE:
				scanResult = ScanIntentIntegrator.parseActivityResult(
						requestCode, resultCode, intent);
				if (scanResult != null) {
					onScanCheckOnline(scanResult);
				}
				break;
			case REQUEST_ADDING_TAGS:
				break;
			case REQUEST_LOAN:
				break;
			case REQUEST_COVER_CHANGE:
				setupViews();
				break;
			case REQUEST_LOAN_RESULT:
				break;
			case REQUEST_ADDING_MULTI_TAGS:
				applyTagsToMultiSelect(intent.getStringExtra("addedTags"));
				break;
			case REQUEST_ADDING_MULTI_RATE:
				applyRateToMultiSelect(intent.getIntExtra("addedRate", 0));
				break;
			}
			break;
		case RESULT_CANCELED:
			switch (requestCode) {
			case REQUEST_SCAN_FOR_ADD_BULK:
				onImport(IOUtilities.inputTypes.bulkScanMovies);
				break;
			}
		}
	}

	private void onScanAdd(ScanIntentResult scanResult) {
		if (ScanIntent.isValidFormat(scanResult.getFormatName())) {
			final String id = scanResult.getContents();
			if (!MoviesManager.movieExists(getContentResolver(), id,
					SettingsActivity.getSortOrder(this), null)) {
				mAddTask = (AddTask) new AddTask().execute(id);
			} else {
				UIUtilities.showToast(
						this,
						getString(R.string.error_item_exists,
								getString(R.string.movie_label)));
			}
		}
	}

	private void onScanCheck(ScanIntentResult scanResult) {
		if (ScanIntent.isValidFormat(scanResult.getFormatName())) {
			final String id = scanResult.getContents();
			final String moviesId = MoviesManager.findMovieId(
					getContentResolver(), id,
					SettingsActivity.getSortOrder(this));

			// GJT: You need this second check, because sometimes the barcode of
			// an item is not what the internet returns
			// Thus, duplicates can slip through
			final MoviesStore moviesStore = new MoviesStore();
			final MoviesStore.Movie movie = moviesStore.findMovie(id,
					IOUtilities.inputTypes.shelvesMovies, getBaseContext());

			if (movie == null)
				return;// GJT: Do nothing; not sure how you can get to this
						// state, but someone did

			String ean = moviesId;
			if (movie.getEan() != null)
				ean = movie.getEan();

			if (moviesId == null
					|| (ean != null && !MoviesManager.movieExists(
							getContentResolver(), ean, null,
							IOUtilities.inputTypes.shelvesMovies))) {
				UIUtilities.showImageToast(
						this,
						getString(R.string.success_item_not_found,
								getString(R.string.movie_label)),
						getResources().getDrawable(R.drawable.unknown_cover_x));
			} else {
				MovieDetailsActivity.show(this, movie.getInternalId());
			}
		}
	}

	@Override
	protected void postUpdateCovers() {
		Handler handler = mScrollHandler;
		Message message = handler.obtainMessage(MESSAGE_UPDATE_MOVIE_COVERS,
				MoviesActivity.this);
		handler.removeMessages(MESSAGE_UPDATE_MOVIE_COVERS);
		mPendingCoversUpdate = true;
		handler.sendMessage(message);
	}

	private class AddTask extends AsyncTask<String, Void, MoviesStore.Movie> {
		private final Object mLock = new Object();
		private String mMovieId;

		@Override
		public void onPreExecute() {
			if (mAddPanel == null) {
				mAddPanel = ((ViewStub) findViewById(R.id.stub_add)).inflate();
				((ProgressBar) mAddPanel.findViewById(R.id.progress))
						.setIndeterminate(true);
				((TextView) mAddPanel.findViewById(R.id.label_import))
						.setText(getText(R.string.adding_label));

				final View cancelButton = mAddPanel
						.findViewById(R.id.button_cancel);
				cancelButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						onCancelAdd();
					}
				});
			}

			showPanel(mAddPanel, false);
		}

		String getMovieId() {
			synchronized (mLock) {
				return mMovieId;
			}
		}

		@Override
		public MoviesStore.Movie doInBackground(String... params) {
			synchronized (mLock) {
				mMovieId = params[0];
			}
			return MoviesManager.loadAndAddMovie(getContentResolver(),
					mMovieId, new MoviesStore(), mSavedImportType,
					MoviesActivity.this);
		}

		@Override
		public void onCancelled() {
			hidePanel(mAddPanel, false);
		}

		@Override
		public void onPostExecute(MoviesStore.Movie movies) {
			if (movies == null) {
				UIUtilities.showToast(
						MoviesActivity.this,
						getString(R.string.error_adding_item,
								getString(R.string.movie_label)));
			} else {
				UIUtilities.showFormattedImageToast(MoviesActivity.this,
						R.string.success_added, ImageUtilities.getCachedCover(
								movies.getInternalId(), mDefaultCover), movies
								.getTitle());
			}
			hidePanel(mAddPanel, false);
		}
	}

	private class ImportTask extends AsyncTask<Void, Integer, Integer> {
		private ContentResolver mResolver;
		private IOUtilities.inputTypes mType;

		private int existsImport = 0;
		private int missingImport = 0;
		private int manualItemNum = 0;

		StringBuilder existingItems = new StringBuilder();
		StringBuilder missingItems = new StringBuilder();

		final AtomicInteger mImportCount = new AtomicInteger();
		ArrayList<ItemImport> mMovies;

		ImportTask(IOUtilities.inputTypes type) {
			mType = type;
		}

		public IOUtilities.inputTypes getType() {
			return mType;
		}

		ImportTask(ArrayList<ItemImport> movies, int index, String inputType) {
			mMovies = movies;
			mImportCount.set(index);
			mType = IOUtilities.inputTypes.valueOf(inputType);
		}

		@Override
		public void onPreExecute() {
			if (mImportPanel == null) {
				mImportPanel = ((ViewStub) findViewById(R.id.stub_import))
						.inflate();
				mImportProgress = (ProgressBar) mImportPanel
						.findViewById(R.id.progress);

				final View cancelButton = mImportPanel
						.findViewById(R.id.button_cancel);
				cancelButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						onCancelImport();
					}
				});
			}

			mResolver = getContentResolver();
			mImportProgress.setProgress(0);

			showPanel(mImportPanel, true);
		}

		@Override
		public Integer doInBackground(Void... params) {
			int imported = 0;

			try {
				if (mMovies == null)
					mMovies = ImportUtilities.loadItems(mType);

				// GJT: Solves an odd race condition from above
				if (mMovies == null) {
					onCancelImport();
				}

				final List<ItemImport> list = mMovies;
				final MoviesStore moviesStore = new MoviesStore();
				final int count = list.size();
				final ContentResolver resolver = mResolver;
				final AtomicInteger importCount = mImportCount;

				for (int i = importCount.get(); i < count; i++) {
					publishProgress(i, count);
					if (isCancelled())
						return null;

					final ItemImport currItem = list.get(i);

					final String id;
					if (mType == inputTypes.shelvesMovies) {
						id = TextUtilities.unprotectString(currItem.internalID);
					} else {
						id = currItem.id_one;
					}

					if (!MoviesManager.movieExists(mResolver, id, null, mType)) {
						if (isCancelled())
							return null;

						MoviesStore.Movie movie = null;
						final boolean isManual = TextUtilities
								.isManualItem(currItem.internalID);
						if (!isManual) {
							movie = MoviesManager.loadAndAddMovie(resolver, id,
									moviesStore, mType, MoviesActivity.this);
						}

						if (movie != null) {
							addOrUpdate(
									movie,
									mType,
									getBaseContext().getString(
											R.string.movie_label_plural_small),
									currItem);

							android.util.Log.d(LOG_TAG, movie.toString());
							imported++;
						} else if (isManual) {
							addManualItemsFromImport(
									MoviesStore.Movie.CONTENT_URI,
									manualItemNum);

							android.util.Log.d(LOG_TAG, "Manual add for" + id);
							imported++;
							manualItemNum++;
						} else {
							missingImport++;

							if (TextUtilities.isEmpty(id))
								missingItems.append(R.string.error_no_barcode);
							else
								missingItems.append(id).append("\n");

						}
					} else {
						existsImport++;
						existingItems.append(id).append("\n");

						if (pref.getBoolean(Preferences.KEY_OVERRIDE_IMPORT,
								true))
							addOrUpdate(
									MoviesManager.findMovieById(mResolver, id,
											null),
									mType,
									getBaseContext().getString(
											R.string.movie_label_plural_small),
									currItem);
					}
					importCount.incrementAndGet();
					try {
						Thread.sleep(1000); // GJT: Let AWS rest
					} catch (InterruptedException e) {
						// Auto-generated catch block
						e.printStackTrace();
					}
				}

			} catch (IOException e) {
				return null;
			}

			return imported;
		}

		@Override
		public void onProgressUpdate(Integer... values) {
			final ProgressBar progress = mImportProgress;
			progress.setMax(values[1]);
			progress.setProgress(values[0]);
		}

		@Override
		public void onCancelled() {
			hidePanel(mImportPanel, true);
		}

		@Override
		public void onPostExecute(Integer countImport) {
			if (isCancelled()) {
				UIUtilities.showToast(MoviesActivity.this,
						R.string.error_import_asynch);
			} else if (countImport == null
					|| (countImport == 0 && missingImport == 0 && existsImport == 0)) {
				UIUtilities.showToast(MoviesActivity.this,
						R.string.error_missing_import_file);
			} else {
				Intent i = new Intent(MoviesActivity.this, ImportResults.class);
				Bundle b = new Bundle();
				b.putInt("countImport", countImport);
				b.putInt("missingImport", missingImport);
				b.putString("missingItems", missingItems.toString());
				b.putInt("existsImport", existsImport);
				b.putString("existingItems", existingItems.toString());
				b.putInt("singularID", R.string.movie_label);
				b.putInt("pluralID", R.string.movie_label_plural_small);
				b.putString("resultsFile", IOUtilities.FILE_SCAN_MOVIES_RESULTS);
				i.putExtras(b);

				startActivityForResult(i, REQUEST_LOAN_RESULT);
			}
			hidePanel(mImportPanel, true);
		}
	}

	private class ShelvesScrollManager implements AbsListView.OnScrollListener {
		private String mPreviousPrefix;
		private boolean mPopupWillShow;
		private boolean sortByTitle = SettingsActivity.getSortOrder().contains(
				"title");
		private boolean sortByAuthor = SettingsActivity.getSortOrder()
				.contains("author");
		private boolean sortByRating = SettingsActivity.getSortOrder()
				.contains("rating");
		private boolean sortByPrice = SettingsActivity.getSortOrder().contains(
				"price");
		private boolean sortByAudience = SettingsActivity.getSortOrder()
				.contains("audience");
		private boolean sortByLabel = SettingsActivity.getSortOrder().contains(
				"label");
		private boolean sortByFormat = SettingsActivity.getSortOrder()
				.contains("format");

		private final Runnable mShowPopup = new Runnable() {
			public void run() {
				showPopup();
			}
		};

		private final Runnable mDismissPopup = new Runnable() {
			public void run() {
				mScrollHandler.removeCallbacks(mShowPopup);
				mPopupWillShow = false;
				dismissPopup();
			}
		};

		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (mScrollState == SCROLL_STATE_FLING
					&& scrollState != SCROLL_STATE_FLING) {
				final Handler handler = mScrollHandler;
				final Message message = handler.obtainMessage(
						MESSAGE_UPDATE_MOVIE_COVERS, MoviesActivity.this);
				handler.removeMessages(MESSAGE_UPDATE_MOVIE_COVERS);
				handler.sendMessageDelayed(message, mFingerUp ? 0
						: DELAY_SHOW_MOVIE_COVERS);
				mPendingCoversUpdate = true;
			} else if (scrollState == SCROLL_STATE_FLING) {
				mPendingCoversUpdate = false;
				mScrollHandler.removeMessages(MESSAGE_UPDATE_MOVIE_COVERS);
			}

			if (scrollState == SCROLL_STATE_IDLE) {
				mScrollHandler.removeCallbacks(mShowPopup);

				final MoviesUpdater moviesUpdater = mMoviesUpdater;
				final int count = view.getChildCount();
				for (int i = 0; i < count; i++) {
					moviesUpdater.offer(((BaseItemViewHolder) view
							.getChildAt(i).getTag()).id);
				}
			} else {
				mMoviesUpdater.clear();
			}

			mScrollState = scrollState;
		}

		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {

			if (mScrollState != SCROLL_STATE_FLING)
				return;

			final int count = view.getChildCount();
			if (count == 0)
				return;

			final StringBuilder buffer = new StringBuilder(7);

			String title = null;
			BaseItemViewHolder viewData = ((BaseItemViewHolder) view
					.getChildAt(0).getTag());

			// GJT: Added scroll check, for sort
			if (sortByTitle) {
				title = viewData.sortTitle;
			} else if (sortByAuthor) {
				title = viewData.sortAuthors;
			} else if (sortByRating) {
				title = String.valueOf(viewData.rateNum.getRating()).substring(
						0, 1);
			} else if (sortByPrice) {
				title = viewData.sortPrice;
			} else if (sortByAudience) {
				title = viewData.sortAudience;
			} else if (sortByFormat) {
				title = viewData.sortFormat;
			} else if (sortByLabel) {
				title = viewData.sortLabel;
			}

			if (TextUtilities.isEmpty(title)) {
				title = " ";
			}

			if (sortByTitle || sortByAuthor || sortByLabel) {
				title = title.substring(0, Math.min(title.length(), 2));
			}

			if (title.length() == 2) {
				buffer.append(Character.toUpperCase(title.charAt(0)));
				buffer.append(title.charAt(1));
			} else {
				buffer.append(title.toUpperCase());
			}

			if (count > 1) {
				buffer.append(" - ");

				final int lastChild = count - 1;
				viewData = ((BaseItemViewHolder) view.getChildAt(lastChild)
						.getTag());

				if (sortByTitle) {
					title = viewData.sortTitle;
				} else if (sortByAuthor) {
					title = viewData.sortAuthors;
				} else if (sortByRating) {
					title = String.valueOf(viewData.rateNum.getRating())
							.substring(0, 1);
				} else if (sortByPrice) {
					title = viewData.sortPrice;
				} else if (sortByAudience) {
					title = viewData.sortAudience;
				} else if (sortByFormat) {
					title = viewData.sortFormat;
				} else if (sortByLabel) {
					title = viewData.sortLabel;
				}

				if (TextUtilities.isEmpty(title)) {
					title = " ";
				}

				if (sortByTitle || sortByAuthor || sortByLabel) {
					title = title.substring(0, Math.min(title.length(), 2));
				}

				if (title.length() == 2) {
					buffer.append(Character.toUpperCase(title.charAt(0)));
					buffer.append(title.charAt(1));
				} else {
					buffer.append(title.toUpperCase());
				}
			}

			final String prefix = buffer.toString();
			final Handler scrollHandler = mScrollHandler;

			if (!mPopupWillShow && (mPopup == null || !mPopup.isShowing())
					&& !prefix.equals(mPreviousPrefix)) {

				mPopupWillShow = true;
				final Runnable showPopup = mShowPopup;
				scrollHandler.removeCallbacks(showPopup);
				scrollHandler.postDelayed(showPopup, WINDOW_SHOW_DELAY);
			}

			mPositionText.setText(prefix);
			mPreviousPrefix = prefix;

			final Runnable dismissPopup = mDismissPopup;
			scrollHandler.removeCallbacks(dismissPopup);
			scrollHandler.postDelayed(dismissPopup, WINDOW_DISMISS_DELAY);
		}
	}

	private static class ScrollHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_UPDATE_MOVIE_COVERS:
				((MoviesActivity) msg.obj).updateCovers();
				break;
			}
		}
	}

	private class FingerTracker implements View.OnTouchListener {
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

	private class SelectionTracker implements
			AdapterView.OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> adapterView, View view,
				int position, long id) {
			if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
				postUpdateCovers();
			}
		}

		public void onNothingSelected(AdapterView<?> adapterView) {
		}
	}
}
