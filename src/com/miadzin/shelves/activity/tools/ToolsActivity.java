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

package com.miadzin.shelves.activity.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.ActivityNotFoundException;
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
import com.miadzin.shelves.provider.tools.ToolsManager;
import com.miadzin.shelves.provider.tools.ToolsStore;
import com.miadzin.shelves.provider.tools.ToolsStore.Tool;
import com.miadzin.shelves.provider.tools.ToolsUpdater;
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

public class ToolsActivity extends BaseItemActivity {
	private static final String LOG_TAG = "ToolsActivity";

	private static final int MESSAGE_UPDATE_TOOL_COVERS = 1;
	private static final int DELAY_SHOW_TOOL_COVERS = 550;

	private static final String ACTION_IMPORT_BULK_TOOLS = "shelves.intent.action.ACTION_IMPORT_BULK_TOOLS";
	private static final String ACTION_IMPORT_DL_TOOLS = "shelves.intent.action.ACTION_IMPORT_DL_TOOLS";
	private static final String ACTION_IMPORT_SHELVES_TOOLS = "shelves.intent.action.ACTION_IMPORT_SHELVES_TOOLS";
	public static final String ACTION_IMPORT_LIST_OF_TOOLS = "shelves.intent.action.ACTION_IMPORT_LIST_OF_TOOLS";

	private static final String ACTION_EXPORT_DL_TOOLS = "shelves.intent.action.ACTION_EXPORT_DL_TOOLS";
	public static final String ACTION_EXPORT_SHELVES_TOOLS = "shelves.intent.action.ACTION_EXPORT_SHELVES_TOOLS";

	private static final String STATE_IMPORT_TOOLS = "shelves.import.tools";
	private static final String STATE_EXPORT_TOOLS = "shelves.export.tools";
	private static final String STATE_ADD_TOOL = "shelves.add.tools";

	private ImportTask mImportTask;
	private ExportTask mExportTask;
	private IOUtilities.inputTypes mSavedImportType;
	private IOUtilities.outputTypes mSavedExportType;
	private AddTask mAddTask;

	private ToolsUpdater mToolsUpdater;

	private final Handler mScrollHandler = new ScrollHandler();
	private int mScrollState = ShelvesScrollManager.SCROLL_STATE_IDLE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mToolsUpdater = new ToolsUpdater(this);

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
		} else if (ACTION_IMPORT_BULK_TOOLS.equals(action)) {
			onImport(IOUtilities.inputTypes.bulkScanTools);
		} else if (ACTION_IMPORT_DL_TOOLS.equals(action)) {
			onImport(IOUtilities.inputTypes.DLTools);
		} else if (ACTION_IMPORT_SHELVES_TOOLS.equals(action)) {
			onImport(IOUtilities.inputTypes.shelvesTools);
		} else if (ACTION_IMPORT_LIST_OF_TOOLS.equals(action)) {
			onImport(IOUtilities.inputTypes.listOfTools);
		} else if (ACTION_EXPORT_DL_TOOLS.equals(action)) {
			onExport(IOUtilities.outputTypes.DLTools, false);
		} else if (ACTION_EXPORT_SHELVES_TOOLS.equals(action)) {
			final boolean massExport;
			if (intent.getExtras() != null) {
				massExport = intent.getExtras().getBoolean(
						SettingsActivity.KEY_MASSEXPORT);
			} else {
				massExport = false;
			}
			onExport(IOUtilities.outputTypes.shelvesTools, massExport);
		}
	}

	@Override
	protected void setupViews() {
		final String sortOrder = SettingsActivity.getSortOrder(this);
		final BaseItemAdapter adapter = new BaseItemAdapter(this,
				this.managedQuery(ToolsStore.Tool.CONTENT_URI,
						BaseItemProvider.TOOLS_PROJECTION_IDS_AND_TITLE, null,
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
			mToolsUpdater.start();

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
		stopToolsUpdater();
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopToolsUpdater();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		dismissPopup();

		stopToolsUpdater();

		onCancelAdd();
		onCancelImport();
		onCancelExport();

		ImageUtilities.cleanupCache();
	}

	private void stopToolsUpdater() {
		final ToolsUpdater toolssUpdater = mToolsUpdater;
		toolssUpdater.clear();
		toolssUpdater.stop();
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
			final String toolsId = task.getToolId();
			task.cancel(true);

			if (toolsId != null) {
				outState.putBoolean(STATE_ADD_IN_PROGRESS, true);
				outState.putString(STATE_ADD_TOOL, toolsId);
			}

			mAddTask = null;
		}
	}

	private void restoreAddTask(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(STATE_ADD_IN_PROGRESS)) {
			final String id = savedInstanceState.getString(STATE_ADD_TOOL);
			if (!ToolsManager.toolExists(getContentResolver(), id,
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
			outState.putParcelableArrayList(STATE_IMPORT_TOOLS, task.mTools);
			outState.putInt(STATE_IMPORT_INDEX, task.mImportCount.get());

			mImportTask = null;
			mSavedImportType = task.getType();
		}
	}

	private void restoreImportTask(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(STATE_IMPORT_IN_PROGRESS)) {
			ArrayList<ItemImport> tools = savedInstanceState
					.getParcelableArrayList(STATE_IMPORT_TOOLS);
			int index = savedInstanceState.getInt(STATE_IMPORT_INDEX);
			String inputType = mSavedImportType.toString();

			if (tools != null) {
				if (index < tools.size()) {
					mImportTask = (ImportTask) new ImportTask(tools, index,
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
			outState.putStringArrayList(STATE_EXPORT_TOOLS,
					task.collectionItems);
			outState.putInt(STATE_EXPORT_INDEX, task.mExportCount.get());
			outState.putString(STATE_EXPORT_TYPE, task.mType.toString());

			mExportTask = null;
			mSavedExportType = task.getType();
		}
	}

	private void restoreExportTask(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(STATE_EXPORT_IN_PROGRESS)) {
			ArrayList<String> tools = savedInstanceState
					.getStringArrayList(STATE_EXPORT_TOOLS);
			int index = savedInstanceState.getInt(STATE_EXPORT_INDEX);
			String exportType = savedInstanceState.getString(STATE_IMPORT_TYPE);

			if (tools != null) {
				if (index < tools.size()) {
					mExportTask = (ExportTask) new ExportTask(tools, index,
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
			switch (item.getItemId()) {
			case R.id.menu_item_sort:
				showDialog(SORT_DIALOG_ID);
				return true;
			case R.id.menu_item_view_shelf:
				onChangeView("tools", VIEW_TYPE.shelfView);
				return true;
			case R.id.menu_item_view_list:
				onChangeView("tools", VIEW_TYPE.listView);
				return true;
			case R.id.menu_item_view_list_nocover:
				onChangeView("tools", VIEW_TYPE.listView_nocover);
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
				onBuy(ToolsManager.findTool(getContentResolver(), holder.id,
						SettingsActivity.getSortOrder(this)));
				return true;
			case R.id.context_menu_item_change_cover:
				onChangeCover(ToolsActivity.this, holder.id);
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
		onAddToolSearch();
	}

	@Override
	protected void callAddItemManually() {
		onAddManualItem(this, "tools");
	}

	@Override
	protected void callScanBulk() {
		prepareBulkFile(IOUtilities.FILE_BULK_SCAN_TOOLS);
		startScan(REQUEST_SCAN_FOR_ADD_BULK);
	}

	@Override
	protected void onView(String toolsId) {
		ToolDetailsActivity.show(this, toolsId);
	}

	@Override
	protected void onTag(String toolId) {
		Intent i = new Intent(ToolsActivity.this, TagActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", toolId);
		b.putString(
				"title",
				ToolsManager.findTool(getContentResolver(), toolId,
						SettingsActivity.getSortOrder(this)).getTitle());
		b.putString("type", getString(R.string.tool_label_plural_small));
		i.putExtras(b);
		startActivityForResult(i, REQUEST_ADDING_TAGS);
	}

	@Override
	protected void onRate(String toolId) {
		Intent i = new Intent(ToolsActivity.this, RateActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", toolId);
		b.putString(
				"title",
				ToolsManager.findTool(getContentResolver(), toolId,
						SettingsActivity.getSortOrder(this)).getTitle());
		b.putString("type", getString(R.string.tool_label_plural_small));
		i.putExtras(b);
		startActivityForResult(i, REQUEST_RATE);
	}

	@Override
	protected void onLoan(String toolId) {
		Intent i = new Intent(ToolsActivity.this, LoanActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", toolId);
		b.putString(
				"title",
				ToolsManager.findTool(getContentResolver(), toolId,
						SettingsActivity.getSortOrder(this)).getTitle());
		b.putString("type", getString(R.string.tool_label_plural_small));
		b.putString("action", "loan");
		i.putExtras(b);
		startActivityForResult(i, REQUEST_LOAN);
	}

	@Override
	protected void onLoanReturn(String toolId) {
		ContentResolver cr = getContentResolver();
		ContentValues loanValues = new ContentValues();

		Tool tool = ToolsManager.findTool(getContentResolver(), toolId, null);

		if (tool.getEventId() > 0) {
			Calendars.setupCalendarUri();
			cr.delete(
					Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString() + "/"
							+ tool.getEventId()), null, null);
		}
		loanValues.put(BaseItem.LOANED_TO, "");
		loanValues.put(BaseItem.LOAN_DATE, "");
		loanValues.put(BaseItem.EVENT_ID, -1);

		cr.update(ToolsStore.Tool.CONTENT_URI, loanValues, BaseItem.INTERNAL_ID
				+ "=?", new String[] { toolId });

		mActivityHelper
				.setActionBarTitle((getString(itemIdString) + setupActionBarTitle(
						getContentResolver(), this.toString())));
		UIUtilities.showToast(getBaseContext(), R.string.loan_returned);
	}

	@Override
	protected void onLoanChange(String toolId) {
		Intent i = new Intent(ToolsActivity.this, LoanActivity.class);
		Bundle b = new Bundle();
		b.putString("itemID", toolId);
		b.putString(
				"title",
				ToolsManager.findTool(getContentResolver(), toolId,
						SettingsActivity.getSortOrder(this)).getTitle());
		b.putString("type", getString(R.string.tool_label_plural_small));
		b.putString("action", "change");
		i.putExtras(b);
		startActivityForResult(i, REQUEST_LOAN_CHANGE);
	}

	private void onBuy(ToolsStore.Tool tools) {
		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tools
				.getDetailsUrl()));
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e("BrowserNotFound", e.toString());
		}
	}

	@Override
	protected void onDelete(String toolId) {
		if (ToolsManager.deleteTool(getContentResolver(), toolId)) {
			mActivityHelper
					.setActionBarTitle((getString(itemIdString) + setupActionBarTitle(
							getContentResolver(), this.toString())));
			UIUtilities.showToast(
					this,
					getString(R.string.success_item_deleted,
							getString(R.string.tool_label_single_big)));
		}
	}

	private void onAddToolSearch() {
		AddToolActivity.show(this);
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
					onScanAddBulk(scanResult, IOUtilities.FILE_BULK_SCAN_TOOLS);
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
				onImport(IOUtilities.inputTypes.bulkScanTools);
				break;
			}
		default:
			break;
		}
	}

	private void onScanAdd(ScanIntentResult scanResult) {
		if (ScanIntent.isValidFormat(scanResult.getFormatName())) {
			final String id = scanResult.getContents();
			if (!ToolsManager.toolExists(getContentResolver(), id,
					SettingsActivity.getSortOrder(this), null)) {
				mAddTask = (AddTask) new AddTask().execute(id);
			} else {
				UIUtilities.showToast(
						this,
						getString(R.string.error_item_exists,
								getString(R.string.tool_label)));
			}
		}
	}

	private void onScanCheck(ScanIntentResult scanResult) {
		if (ScanIntent.isValidFormat(scanResult.getFormatName())) {
			final String id = scanResult.getContents();
			final String toolsId = ToolsManager.findToolId(
					getContentResolver(), id,
					SettingsActivity.getSortOrder(this));

			// GJT: You need this second check, because sometimes the barcode of
			// an item is not what the internet returns
			// Thus, duplicates can slip through
			final ToolsStore toolsStore = new ToolsStore();
			final ToolsStore.Tool tool = toolsStore.findTool(id,
					IOUtilities.inputTypes.shelvesTools, getBaseContext());

			if (tool == null)
				return;// GJT: Do nothing; not sure how you can get to this
						// state, but someone did

			String ean = toolsId;
			if (tool.getEan() != null)
				ean = tool.getEan();

			if (toolsId == null
					|| (ean != null && !ToolsManager.toolExists(
							getContentResolver(), ean, null,
							IOUtilities.inputTypes.shelvesTools))) {
				UIUtilities.showImageToast(
						this,
						getString(R.string.success_item_not_found,
								getString(R.string.tool_label)), getResources()
								.getDrawable(R.drawable.unknown_cover_x));
			} else {
				ToolDetailsActivity.show(this, tool.getInternalId());
			}
		}
	}

	@Override
	protected void postUpdateCovers() {
		Handler handler = mScrollHandler;
		Message message = handler.obtainMessage(MESSAGE_UPDATE_TOOL_COVERS,
				ToolsActivity.this);
		handler.removeMessages(MESSAGE_UPDATE_TOOL_COVERS);
		mPendingCoversUpdate = true;
		handler.sendMessage(message);
	}

	private class AddTask extends AsyncTask<String, Void, ToolsStore.Tool> {
		private final Object mLock = new Object();
		private String mToolId;

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

		String getToolId() {
			synchronized (mLock) {
				return mToolId;
			}
		}

		@Override
		public ToolsStore.Tool doInBackground(String... params) {
			synchronized (mLock) {
				mToolId = params[0];
			}
			return ToolsManager.loadAndAddTool(getContentResolver(), mToolId,
					new ToolsStore(), mSavedImportType, ToolsActivity.this);
		}

		@Override
		public void onCancelled() {
			hidePanel(mAddPanel, false);
		}

		@Override
		public void onPostExecute(ToolsStore.Tool tools) {
			if (tools == null) {
				UIUtilities.showToast(
						ToolsActivity.this,
						getString(R.string.error_adding_item,
								getString(R.string.tool_label)));
			} else {
				UIUtilities.showFormattedImageToast(ToolsActivity.this,
						R.string.success_added, ImageUtilities.getCachedCover(
								tools.getInternalId(), mDefaultCover), tools
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
		ArrayList<ItemImport> mTools;

		ImportTask(IOUtilities.inputTypes type) {
			mType = type;
		}

		public IOUtilities.inputTypes getType() {
			return mType;
		}

		ImportTask(ArrayList<ItemImport> tools, int index, String inputType) {
			mTools = tools;
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
				if (mTools == null)
					mTools = ImportUtilities.loadItems(mType);

				// GJT: Solves an odd race condition from above
				if (mTools == null) {
					onCancelImport();
				}

				final List<ItemImport> list = mTools;
				final ToolsStore toolsStore = new ToolsStore();
				final int count = list.size();
				final ContentResolver resolver = mResolver;
				final AtomicInteger importCount = mImportCount;

				for (int i = importCount.get(); i < count; i++) {
					publishProgress(i, count);
					if (isCancelled())
						return null;

					final ItemImport currItem = list.get(i);

					final String id;
					if (mType == inputTypes.shelvesTools) {
						id = TextUtilities.unprotectString(currItem.internalID);
					} else {
						id = currItem.id_one;
					}

					if (!ToolsManager.toolExists(mResolver, id, null, mType)) {
						if (isCancelled())
							return null;

						ToolsStore.Tool tool = null;
						final boolean isManual = TextUtilities
								.isManualItem(currItem.internalID);
						if (!isManual) {
							tool = ToolsManager.loadAndAddTool(resolver, id,
									toolsStore, mType, ToolsActivity.this);
						}

						if (tool != null) {
							addOrUpdate(
									tool,
									mType,
									getBaseContext().getString(
											R.string.tool_label_plural_small),
									currItem);

							android.util.Log.d(LOG_TAG, tool.toString());
							imported++;
						} else if (isManual) {
							addManualItemsFromImport(
									ToolsStore.Tool.CONTENT_URI, manualItemNum);

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
									ToolsManager.findToolById(mResolver, id,
											null),
									mType,
									getBaseContext().getString(
											R.string.tool_label_plural_small),
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
				UIUtilities.showToast(ToolsActivity.this,
						R.string.error_import_asynch);
			} else if (countImport == null
					|| (countImport == 0 && missingImport == 0 && existsImport == 0)) {
				UIUtilities.showToast(ToolsActivity.this,
						R.string.error_missing_import_file);
			} else {
				Intent i = new Intent(ToolsActivity.this, ImportResults.class);
				Bundle b = new Bundle();
				b.putInt("countImport", countImport);
				b.putInt("missingImport", missingImport);
				b.putString("missingItems", missingItems.toString());
				b.putInt("existsImport", existsImport);
				b.putString("existingItems", existingItems.toString());
				b.putInt("singularID", R.string.tool_label);
				b.putInt("pluralID", R.string.tool_label_plural_small);
				b.putString("resultsFile", IOUtilities.FILE_SCAN_TOOLS_RESULTS);
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
						MESSAGE_UPDATE_TOOL_COVERS, ToolsActivity.this);
				handler.removeMessages(MESSAGE_UPDATE_TOOL_COVERS);
				handler.sendMessageDelayed(message, mFingerUp ? 0
						: DELAY_SHOW_TOOL_COVERS);
				mPendingCoversUpdate = true;
			} else if (scrollState == SCROLL_STATE_FLING) {
				mPendingCoversUpdate = false;
				mScrollHandler.removeMessages(MESSAGE_UPDATE_TOOL_COVERS);
			}

			if (scrollState == SCROLL_STATE_IDLE) {
				mScrollHandler.removeCallbacks(mShowPopup);

				final ToolsUpdater toolsUpdater = mToolsUpdater;
				final int count = view.getChildCount();
				for (int i = 0; i < count; i++) {
					toolsUpdater.offer(((BaseItemViewHolder) view.getChildAt(i)
							.getTag()).id);
				}
			} else {
				mToolsUpdater.clear();
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
			}

			if (TextUtilities.isEmpty(title)) {
				title = " ";
			}

			if (sortByTitle || sortByAuthor) {
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
				}

				if (TextUtilities.isEmpty(title)) {
					title = " ";
				}

				if (sortByTitle || sortByAuthor) {
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
			case MESSAGE_UPDATE_TOOL_COVERS:
				((ToolsActivity) msg.obj).updateCovers();
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
