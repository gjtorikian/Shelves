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

package com.miadzin.shelves.activity.apparel;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miadzin.shelves.R;
import com.miadzin.shelves.activity.SettingsActivity;
import com.miadzin.shelves.base.AddBaseItemActivity;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.drawable.FastBitmapDrawable;
import com.miadzin.shelves.provider.apparel.ApparelManager;
import com.miadzin.shelves.provider.apparel.ApparelStore;
import com.miadzin.shelves.provider.apparel.ApparelStore.Apparel;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class AddApparelActivity extends AddBaseItemActivity implements
		AdapterView.OnItemClickListener {

	private static final String STATE_ADD_APPAREL = "shelves.add.apparel";
	private static final String STATE_SEARCH_QUERY = "shelves.search.apparel";
	private static final String STATE_APPAREL_TO_ADD = "shelves.add.apparelToAdd";

	private SearchTask mSearchTask;
	private AddTask mAddTask;

	private SearchResultsAdapter mApparelAdapter;
	private ApparelStore.Apparel mApparelToAdd;

	static void show(Context context) {
		final Intent intent = new Intent(context, AddApparelActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void setupViews() {
		super.setupViews();

		mSearchQuery.setHint(R.string.search_add_hint_apparel);

		mSearchButton = findViewById(R.id.button_go);
		mSearchButton.setOnClickListener(this);
		mSearchButton.setEnabled(false);

		final FastBitmapDrawable cover = new FastBitmapDrawable(
				ImageUtilities.createShadow(BitmapFactory.decodeResource(
						getResources(), R.drawable.unknown_cover_no_shadow),
						COVER_WIDTH, COVER_HEIGHT));

		mApparelAdapter = new SearchResultsAdapter(this, cover);

		final SearchResultsAdapter resultsAdapter = mApparelAdapter;
		final SearchResultsAdapter oldAdapter = (SearchResultsAdapter) getLastNonConfigurationInstance();

		if (oldAdapter != null) {
			final int count = oldAdapter.getCount();
			for (int i = 0; i < count; i++) {
				resultsAdapter.add(oldAdapter.getItem(i));
			}
		}

		final ListView searchResults = (ListView) findViewById(R.id.list_search_results);
		searchResults.setAdapter(resultsAdapter);
		searchResults.setOnItemClickListener(this);
		registerForContextMenu(searchResults);
	}

	@Override
	protected void getNextResults() {
		mPage++;
		mSearchTask = (SearchTask) new SearchTask(false).execute(mSearchQuery
				.getText().toString());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		onCancelAdd();
		onCancelSearch();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		restoreApparelToAdd(savedInstanceState);
		restoreAddTask(savedInstanceState);
		restoreSearchTask(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (isFinishing()) {
			saveApparelToAdd(outState);
			saveAddTask(outState);
			saveSearchTask(outState);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mApparelAdapter;
	}

	private void saveApparelToAdd(Bundle outState) {
		if (mApparelToAdd != null) {
			outState.putParcelable(STATE_APPAREL_TO_ADD, mApparelToAdd);
		}
	}

	private void restoreApparelToAdd(Bundle savedInstanceState) {
		final Object data = savedInstanceState.get(STATE_APPAREL_TO_ADD);
		if (data != null) {
			mApparelToAdd = (ApparelStore.Apparel) data;
		}
	}

	private void saveAddTask(Bundle outState) {
		final AddTask task = mAddTask;
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			final String apparelId = task.getApparelId();
			task.cancel(true);

			if (apparelId != null) {
				outState.putBoolean(STATE_ADD_IN_PROGRESS, true);
				outState.putString(STATE_ADD_APPAREL, apparelId);
			}

			mAddTask = null;
		}
	}

	private void restoreAddTask(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(STATE_ADD_IN_PROGRESS)) {
			final String id = savedInstanceState.getString(STATE_ADD_APPAREL);
			if (!ApparelManager.apparelExists(getContentResolver(), id,
					SettingsActivity.getSortOrder(this), null)) {
				mAddTask = (AddTask) new AddTask().execute(id);
			}
		}
	}

	private void saveSearchTask(Bundle outState) {
		final SearchTask task = mSearchTask;
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			final String apparelId = task.getQuery();
			task.cancel(true);

			if (apparelId != null) {
				outState.putBoolean(STATE_SEARCH_IN_PROGRESS, true);
				outState.putString(STATE_SEARCH_QUERY, apparelId);
			}

			mSearchTask = null;
		}
	}

	private void restoreSearchTask(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(STATE_SEARCH_IN_PROGRESS)) {
			final String query = savedInstanceState
					.getString(STATE_SEARCH_QUERY);
			if (!TextUtilities.isEmpty(query)) {
				mSearchTask = (SearchTask) new SearchTask(false).execute(query);
			}
		}
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		mApparelToAdd = mApparelAdapter.getItem(position).apparel;
		showDialog(DIALOG_ADD);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ADD:
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(mApparelToAdd != null ? mApparelToAdd.getTitle()
					: " ");

			builder.setMessage(R.string.dialog_add_message);
			builder.setPositiveButton(R.string.add_label,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (mApparelToAdd != null) {
								onAdd(mApparelToAdd.getInternalIdNoPrefix());
								mApparelToAdd = null;
							}
						}
					});
			builder.setNegativeButton(R.string.dialog_add_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							mApparelToAdd = null;
							dismissDialog(DIALOG_ADD);
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mApparelToAdd = null;
					dismissDialog(DIALOG_ADD);
				}
			});
			builder.setCancelable(true);

			return builder.create();
		}

		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		switch (id) {
		case DIALOG_ADD:
			if (mApparelToAdd != null && dialog != null) {
				dialog.setTitle(mApparelToAdd.getTitle());
			}
			break;
		}
	}

	@Override
	protected void onSearch(boolean b) {
		if (mSearchTask == null
				|| mSearchTask.getStatus() == SearchTask.Status.FINISHED) {
			mSearchTask = (SearchTask) new SearchTask(b).execute(mSearchQuery
					.getText().toString());
		} else {
			UIUtilities.showToast(this, R.string.error_search_in_progress);
		}
	}

	private void onCancelSearch() {
		if (mSearchTask != null
				&& mSearchTask.getStatus() == AsyncTask.Status.RUNNING) {
			mSearchTask.cancel(true);
			mSearchTask = null;
		}
	}

	private void onAdd(String id) {
		if (!ApparelManager.apparelExists(getApplicationContext()
				.getContentResolver(), id, SettingsActivity.getSortOrder(this),
				null)) {
			mAddTask = (AddTask) new AddTask().execute(id);
		} else {
			UIUtilities.showToast(
					this,
					getString(R.string.error_item_exists,
							getString(R.string.apparel_label)));
		}
	}

	private void onCancelAdd() {
		if (mAddTask != null
				&& mAddTask.getStatus() == AsyncTask.Status.RUNNING) {
			mAddTask.cancel(true);
			mAddTask = null;
		}
	}

	private class AddTask extends AsyncTask<String, Void, ApparelStore.Apparel> {
		private final Object mLock = new Object();
		private String mApparelId;
		private FastBitmapDrawable mDefaultCover;

		@Override
		public void onPreExecute() {
			final Bitmap defaultCoverBitmap = BitmapFactory.decodeResource(
					getResources(), R.drawable.unknown_cover);
			mDefaultCover = new FastBitmapDrawable(defaultCoverBitmap);

			if (mAddPanel == null) {
				mAddPanel = ((ViewStub) findViewById(R.id.stub_add)).inflate();
				((ProgressBar) mAddPanel.findViewById(R.id.progress))
						.setIndeterminate(true);

				((TextView) findViewById(R.id.label_import))
						.setText(R.string.adding_label);

				final View cancelButton = mAddPanel
						.findViewById(R.id.button_cancel);
				cancelButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						onCancelAdd();
					}
				});
			}

			disableSearchPanel();
			showPanel(mAddPanel, false);
		}

		String getApparelId() {
			synchronized (mLock) {
				return mApparelId;
			}
		}

		@Override
		public ApparelStore.Apparel doInBackground(String... params) {
			synchronized (mLock) {
				mApparelId = params[0];
			}
			return ApparelManager.loadAndAddApparel(getContentResolver(),
					mApparelId, new ApparelStore(), null,
					AddApparelActivity.this);
		}

		@Override
		public void onCancelled() {
			enableSearchPanel();
			hidePanel(mAddPanel, false);
		}

		@Override
		public void onPostExecute(ApparelStore.Apparel apparel) {
			enableSearchPanel();
			if (apparel == null) {
				UIUtilities.showToast(
						AddApparelActivity.this,
						getString(R.string.error_adding_item,
								getString(R.string.apparel_label)));
			} else {
				UIUtilities.showFormattedImageToast(AddApparelActivity.this,
						R.string.success_added, ImageUtilities.getCachedCover(
								apparel.getInternalId(), mDefaultCover),
						apparel.getTitle());
			}
			hidePanel(mAddPanel, false);
		}
	}

	private class SearchTask extends AsyncTask<String, ResultApparel, Void>
			implements ApparelStore.ApparelSearchListener {

		private final Object mLock = new Object();
		private String mQuery;
		private boolean clearResults;

		public SearchTask(boolean clear) {
			clearResults = clear;
		}

		@Override
		public void onPreExecute() {
			disableSearchPanel();

			if (mSearchPanel == null) {
				mSearchPanel = ((ViewStub) findViewById(R.id.stub_search))
						.inflate();

				ProgressBar progress = (ProgressBar) mSearchPanel
						.findViewById(R.id.progress);
				progress.setIndeterminate(true);

				((TextView) findViewById(R.id.label_import))
						.setText(R.string.search_progress);

				final View cancelButton = mSearchPanel
						.findViewById(R.id.button_cancel);
				cancelButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						onCancelSearch();
					}
				});
			}

			if (clearResults)
				mApparelAdapter.clear();
			showPanel(mSearchPanel, true);
		}

		String getQuery() {
			synchronized (mLock) {
				return mQuery;
			}
		}

		@Override
		public Void doInBackground(String... params) {
			synchronized (mLock) {
				mQuery = params[0];
			}
			new ApparelStore().searchApparel(mQuery, String.valueOf(mPage),
					this, AddApparelActivity.this);

			return null;
		}

		@Override
		public void onProgressUpdate(ResultApparel... values) {
			for (ResultApparel apparel : values) {
				mApparelAdapter.add(apparel);
			}
		}

		@Override
		public void onPostExecute(Void ignore) {
			enableSearchPanel();

			UIUtilities.showToast(
					AddApparelActivity.this,
					getString(R.string.success_item_found,
							mApparelAdapter.getCount(),
							getString(R.string.apparel_label)));
			hidePanel(mSearchPanel, true);
		}

		@Override
		public void onCancelled() {
			enableSearchPanel();

			hidePanel(mSearchPanel, true);
		}

		public void onApparelFound(ApparelStore.Apparel apparel,
				ArrayList<ApparelStore.Apparel> apparels) {
			if (apparel != null && !isCancelled()) {
				publishProgress(new ResultApparel(apparel));
			}
		}
	}

	private static class SearchResultsAdapter extends
			ArrayAdapter<ResultApparel> {
		private final LayoutInflater mLayoutInflater;
		private final FastBitmapDrawable mDefaultCover;

		SearchResultsAdapter(AddApparelActivity addApparelActivity,
				FastBitmapDrawable cover) {
			super(addApparelActivity, 0);
			mDefaultCover = cover;
			mLayoutInflater = LayoutInflater.from(addApparelActivity);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mLayoutInflater.inflate(R.layout.search_results,
						parent, false);

				holder = new ViewHolder();
				holder.cover = (ImageView) convertView
						.findViewById(R.id.image_cover);
				holder.title = (TextView) convertView
						.findViewById(R.id.label_title);
				holder.author = (TextView) convertView
						.findViewById(R.id.label_author);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final ResultApparel apparel = getItem(position);
			holder.apparel = apparel.apparel;
			holder.title.setText(apparel.title);
			holder.author.setText(apparel.authors);

			final boolean hasCover = apparel.cover != null;
			holder.cover.setImageDrawable(hasCover ? apparel.cover
					: mDefaultCover);

			return convertView;
		}
	}

	private static class ViewHolder {
		ImageView cover;
		TextView title;
		TextView author;
		ApparelStore.Apparel apparel;
	}

	private static class ResultApparel {
		final ApparelStore.Apparel apparel;
		final String text;
		final String title;
		final String authors;

		final FastBitmapDrawable cover;

		ResultApparel(ApparelStore.Apparel apparel) {
			this.apparel = apparel;
			Bitmap bitmap = ImageUtilities.createShadow(
					apparel.loadCover(BaseItem.ImageSize.THUMBNAIL),
					COVER_WIDTH, COVER_HEIGHT);
			if (bitmap != null) {
				cover = new FastBitmapDrawable(bitmap);
			} else {
				cover = null;
			}

			title = apparel.getTitle();
			authors = apparel.getAuthors();
			text = title + ' ' + authors;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	// GJT: Added these, for showing apparel descriptions before add
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle(mApparelAdapter.getItem((int) info.id).apparel
				.getTitle());

		getMenuInflater().inflate(R.menu.add_item, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.context_menu_add_details:
			onShowDetails(mApparelAdapter.getItem((int) info.id).apparel);
			return true;
		case R.id.context_menu_item_buy:
			onBuy(mApparelAdapter.getItem((int) info.id).apparel);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public void onShowDetails(ApparelStore.Apparel apparel) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				TextUtilities.join(apparel.getDescriptions(), "\n\n"))
				.setCancelable(true).setTitle(apparel.getTitle());
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void onBuy(Apparel apparel) {
		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apparel
				.getDetailsUrl()));
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e("BrowserNotFound", e.toString());
		}
	}
}
