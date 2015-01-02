/*
 * Copyright (C) 2008 Romain Guy
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

package com.miadzin.shelves.provider.boardgames;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Process;

import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.util.HttpManager;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.ImportUtilities;
import com.miadzin.shelves.util.Preferences;

public class BoardGamesUpdater implements Runnable {
	private static final String LOG_TAG = "BoardGamesUpdater";

	private static final long ONE_DAY = 24 * 60 * 60 * 1000;

	private static final HashMap<String, Long> sLastChecks = new HashMap<String, Long>();

	private final BlockingQueue<String> mQueue = new ArrayBlockingQueue<String>(
			12);
	private final ContentResolver mResolver;
	private final SimpleDateFormat mLastModifiedFormat;
	private final String mSelection;
	private final String[] mArguments = new String[1];
	private final ContentValues mValues = new ContentValues();

	private Thread mThread;
	private volatile boolean mStopped;

	public BoardGamesUpdater(Context context) {
		mResolver = context.getContentResolver();
		mLastModifiedFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z");
		mSelection = BaseItem._ID + "=?";
	}

	public void start() {
		if (mThread == null) {
			mStopped = false;
			mThread = new Thread(this, "BoardGamesUpdater");
			mThread.start();
		}
	}

	public void stop() {
		if (mThread != null) {
			mStopped = true;
			mThread.interrupt();
			mThread = null;
		}
	}

	public void offer(String... boardgames) {
		for (String boardgameId : boardgames) {
			if (boardgameId != null)
				mQueue.offer(boardgameId);
		}
	}

	public void clear() {
		mQueue.clear();
	}

	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		final ImageUtilities.ExpiringBitmap expiring = new ImageUtilities.ExpiringBitmap();

		while (!mStopped) {
			try {
				final String boardgameId = mQueue.take();

				final Long lastCheck = sLastChecks.get(boardgameId);
				if (lastCheck != null
						&& (lastCheck + ONE_DAY) >= System.currentTimeMillis()) {
					continue;
				}
				sLastChecks.put(boardgameId, System.currentTimeMillis());

				final BoardGamesStore.BoardGame boardgame = BoardGamesManager
						.findBoardGame(mResolver, boardgameId, null);

				if (boardgame == null)
					continue;

				final String imgURL = Preferences
						.getImageURLForUpdater(boardgame);

				if (boardgame.getLastModified() == null || imgURL == null) {
					continue;
				}

				if (boardgameCoverUpdated(boardgame, expiring)
						&& expiring.lastModified != null) {
					ImageUtilities.deleteCachedCover(boardgameId);

					final Bitmap bitmap = Preferences
							.getBitmapForManager(boardgame);

					ImportUtilities.addCoverToCache(boardgame.getInternalId(),
							bitmap);

					if (bitmap != null)
						bitmap.recycle();

					mValues.put(BaseItem.LAST_MODIFIED,
							expiring.lastModified.getTimeInMillis());
					mArguments[0] = boardgameId;
					mResolver.update(BoardGamesStore.BoardGame.CONTENT_URI,
							mValues, mSelection, mArguments);
				}

				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
	}

	private boolean boardgameCoverUpdated(BoardGamesStore.BoardGame boardgame,
			ImageUtilities.ExpiringBitmap expiring) {
		expiring.lastModified = null;

		final String tinyThumbnail = Preferences
				.getImageURLForUpdater(boardgame);

		if (tinyThumbnail != null && !tinyThumbnail.equals("")) {
			HttpGet get = null;
			try {
				get = new HttpGet(Preferences.getImageURLForUpdater(boardgame));

			} catch (NullPointerException npe) {
				android.util.Log.e(LOG_TAG,
						"Could not check modification image for " + boardgame,
						npe);
			}

			HttpEntity entity = null;
			try {
				final HttpResponse response = HttpManager.execute(get);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					entity = response.getEntity();

					final Header header = response
							.getFirstHeader("Last-Modified");
					if (header != null) {
						final Calendar calendar = Calendar.getInstance();
						try {
							calendar.setTime(mLastModifiedFormat.parse(header
									.getValue()));
							expiring.lastModified = calendar;
							return calendar.after(boardgame.getLastModified());
						} catch (ParseException e) {
							return false;
						}
					}
				}
			} catch (IOException e) {
				android.util.Log
						.e(LOG_TAG, "Could not check modification date for "
								+ boardgame, e);
			} catch (IllegalArgumentException iae) {
				android.util.Log.e(LOG_TAG, "Null get value for " + boardgame,
						iae);
			} finally {
				if (entity != null) {
					try {
						entity.consumeContent();
					} catch (IOException e) {
						android.util.Log.e(LOG_TAG,
								"Could not check modification date for "
										+ boardgame, e);
					}
				}
			}
		}
		return false;
	}
}
