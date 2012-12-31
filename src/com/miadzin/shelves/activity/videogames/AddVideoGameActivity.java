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

package com.miadzin.shelves.activity.videogames;

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
import com.miadzin.shelves.provider.videogames.VideoGamesManager;
import com.miadzin.shelves.provider.videogames.VideoGamesStore;
import com.miadzin.shelves.provider.videogames.VideoGamesStore.VideoGame;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class AddVideoGameActivity extends AddBaseItemActivity implements
		AdapterView.OnItemClickListener {

	private static final String STATE_ADD_VIDEOGAME = "shelves.add.videogame";
	private static final String STATE_SEARCH_QUERY = "shelves.search.videogame";
	private static final String STATE_VIDEOGAME_TO_ADD = "shelves.add.videogameToAdd";

	private SearchTask mSearchTask;
	private AddTask mAddTask;

	private SearchResultsAdapter mVideoGamesAdapter;
	private VideoGamesStore.VideoGame mVideoGameToAdd;

	static void show(Context context) {
		final Intent intent = new Intent(context, AddVideoGameActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void setupViews() {
		super.setupViews();

		mSearchQuery.setHint(R.string.search_add_hint_videogame);

		mSearchButton = findViewById(R.id.button_go);
		mSearchButton.setOnClickListener(this);
		mSearchButton.setEnabled(false);

		final FastBitmapDrawable cover = new FastBitmapDrawable(
				ImageUtilities.createShadow(BitmapFactory.decodeResource(
						getResources(), R.drawable.unknown_cover_no_shadow),
						COVER_WIDTH, COVER_HEIGHT));

		mVideoGamesAdapter = new SearchResultsAdapter(this, cover);

		final SearchResultsAdapter resultsAdapter = mVideoGamesAdapter;
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
		restoreVideoGameToAdd(savedInstanceState);
		restoreAddTask(savedInstanceState);
		restoreSearchTask(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (isFinishing()) {
			saveVideoGameToAdd(outState);
			saveAddTask(outState);
			saveSearchTask(outState);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mVideoGamesAdapter;
	}

	private void saveVideoGameToAdd(Bundle outState) {
		if (mVideoGameToAdd != null) {
			outState.putParcelable(STATE_VIDEOGAME_TO_ADD, mVideoGameToAdd);
		}
	}

	private void restoreVideoGameToAdd(Bundle savedInstanceState) {
		final Object data = savedInstanceState.get(STATE_VIDEOGAME_TO_ADD);
		if (data != null) {
			mVideoGameToAdd = (VideoGamesStore.VideoGame) data;
		}
	}

	private void saveAddTask(Bundle outState) {
		final AddTask task = mAddTask;
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			final String videogameId = task.getVideoGameId();
			task.cancel(true);

			if (videogameId != null) {
				outState.putBoolean(STATE_ADD_IN_PROGRESS, true);
				outState.putString(STATE_ADD_VIDEOGAME, videogameId);
			}

			mAddTask = null;
		}
	}

	private void restoreAddTask(Bundle savedInstanceState) {
		if (savedInstanceState.getBoolean(STATE_ADD_IN_PROGRESS)) {
			final String id = savedInstanceState.getString(STATE_ADD_VIDEOGAME);
			if (!VideoGamesManager.videogameExists(getContentResolver(), id,
					SettingsActivity.getSortOrder(this), null)) {
				mAddTask = (AddTask) new AddTask().execute(id);
			}
		}
	}

	private void saveSearchTask(Bundle outState) {
		final SearchTask task = mSearchTask;
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			final String videogameId = task.getQuery();
			task.cancel(true);

			if (videogameId != null) {
				outState.putBoolean(STATE_SEARCH_IN_PROGRESS, true);
				outState.putString(STATE_SEARCH_QUERY, videogameId);
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
		mVideoGameToAdd = mVideoGamesAdapter.getItem(position).videogame;
		showDialog(DIALOG_ADD);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ADD:
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(mVideoGameToAdd != null ? mVideoGameToAdd
					.getTitle() : " ");

			builder.setMessage(R.string.dialog_add_message);
			builder.setPositiveButton(R.string.add_label,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (mVideoGameToAdd != null) {
								onAdd(mVideoGameToAdd.getInternalIdNoPrefix());
								mVideoGameToAdd = null;
							}
						}
					});
			builder.setNegativeButton(R.string.dialog_add_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							mVideoGameToAdd = null;
							dismissDialog(DIALOG_ADD);
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mVideoGameToAdd = null;
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
			if (mVideoGameToAdd != null && dialog != null) {
				dialog.setTitle(mVideoGameToAdd.getTitle());
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
		if (!VideoGamesManager.videogameExists(getApplicationContext()
				.getContentResolver(), id, SettingsActivity.getSortOrder(this),
				null)) {
			mAddTask = (AddTask) new AddTask().execute(id);
		} else {
			UIUtilities.showToast(
					this,
					getString(R.string.error_item_exists,
							getString(R.string.videogame_label)));
		}
	}

	private void onCancelAdd() {
		if (mAddTask != null
				&& mAddTask.getStatus() == AsyncTask.Status.RUNNING) {
			mAddTask.cancel(true);
			mAddTask = null;
		}
	}

	private class AddTask extends
			AsyncTask<String, Void, VideoGamesStore.VideoGame> {
		private final Object mLock = new Object();
		private String mVideoGameId;
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

		String getVideoGameId() {
			synchronized (mLock) {
				return mVideoGameId;
			}
		}

		@Override
		public VideoGamesStore.VideoGame doInBackground(String... params) {
			synchronized (mLock) {
				mVideoGameId = params[0];
			}
			return VideoGamesManager.loadAndAddVideoGame(getContentResolver(),
					mVideoGameId, new VideoGamesStore(), null,
					AddVideoGameActivity.this);
		}

		@Override
		public void onCancelled() {
			enableSearchPanel();
			hidePanel(mAddPanel, false);
		}

		@Override
		public void onPostExecute(VideoGamesStore.VideoGame videogame) {
			enableSearchPanel();
			if (videogame == null) {
				UIUtilities.showToast(
						AddVideoGameActivity.this,
						getString(R.string.error_adding_item,
								getString(R.string.videogame_label)));
			} else {
				UIUtilities.showFormattedImageToast(AddVideoGameActivity.this,
						R.string.success_added, ImageUtilities.getCachedCover(
								videogame.getInternalId(), mDefaultCover),
						videogame.getTitle());
			}
			hidePanel(mAddPanel, false);
		}
	}

	private class SearchTask extends AsyncTask<String, ResultVideoGame, Void>
			implements VideoGamesStore.VideoGameSearchListener {

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
				mVideoGamesAdapter.clear();
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
			new VideoGamesStore().searchVideoGames(mQuery,
					String.valueOf(mPage), this, AddVideoGameActivity.this);

			return null;
		}

		@Override
		public void onProgressUpdate(ResultVideoGame... values) {
			for (ResultVideoGame videogame : values) {
				mVideoGamesAdapter.add(videogame);
			}
		}

		@Override
		public void onPostExecute(Void ignore) {
			enableSearchPanel();

			UIUtilities.showToast(
					AddVideoGameActivity.this,
					getString(R.string.success_item_found,
							mVideoGamesAdapter.getCount(),
							getString(R.string.videogame_label_plural_small)));
			hidePanel(mSearchPanel, true);
		}

		@Override
		public void onCancelled() {
			enableSearchPanel();

			hidePanel(mSearchPanel, true);
		}

		public void onVideoGameFound(VideoGamesStore.VideoGame videogame,
				ArrayList<VideoGamesStore.VideoGame> videogames) {
			if (videogame != null && !isCancelled()) {
				publishProgress(new ResultVideoGame(videogame));
			}
		}
	}

	private static class SearchResultsAdapter extends
			ArrayAdapter<ResultVideoGame> {
		private final LayoutInflater mLayoutInflater;
		private final FastBitmapDrawable mDefaultCover;

		SearchResultsAdapter(AddVideoGameActivity addVideoGameActivity,
				FastBitmapDrawable cover) {
			super(addVideoGameActivity, 0);
			mDefaultCover = cover;
			mLayoutInflater = LayoutInflater.from(addVideoGameActivity);
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

			final ResultVideoGame videogame = getItem(position);
			holder.videogame = videogame.videogame;
			holder.title.setText(videogame.title);
			holder.author.setText(videogame.author);

			final boolean hasCover = videogame.cover != null;
			holder.cover.setImageDrawable(hasCover ? videogame.cover
					: mDefaultCover);

			return convertView;
		}
	}

	private static class ViewHolder {
		ImageView cover;
		TextView title;
		TextView author;
		VideoGamesStore.VideoGame videogame;
	}

	private static class ResultVideoGame {
		final VideoGamesStore.VideoGame videogame;
		final String text;
		final String title;
		final String author;

		final FastBitmapDrawable cover;

		ResultVideoGame(VideoGamesStore.VideoGame videogame) {
			this.videogame = videogame;
			Bitmap bitmap = ImageUtilities.createShadow(
					videogame.loadCover(BaseItem.ImageSize.THUMBNAIL),
					COVER_WIDTH, COVER_HEIGHT);
			if (bitmap != null) {
				cover = new FastBitmapDrawable(bitmap);
			} else {
				cover = null;
			}

			title = videogame.getTitle();
			author = videogame.getAuthors();
			text = title + ' ' + author;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	// GJT: Added these, for showing videogame descriptions before add
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle(mVideoGamesAdapter.getItem((int) info.id).videogame
				.getTitle());

		getMenuInflater().inflate(R.menu.add_item, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.context_menu_add_details:
			onShowDetails(mVideoGamesAdapter.getItem((int) info.id).videogame);
			return true;
		case R.id.context_menu_item_buy:
			onBuy(mVideoGamesAdapter.getItem((int) info.id).videogame);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public void onShowDetails(VideoGame videogame) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String platform = videogame.getPlatform();
		String message = TextUtilities
				.join(videogame.getDescriptions(), "\n\n");

		if (platform != null && !platform.equals("")) {
			message = platform + ": " + message;
		}

		builder.setMessage(message).setCancelable(true)
				.setTitle(videogame.getTitle());
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void onBuy(VideoGame videogame) {
		final Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse(videogame.getDetailsUrl()));
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e("BrowserNotFound", e.toString());
		}
	}
}
