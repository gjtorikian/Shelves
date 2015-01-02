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

package com.miadzin.shelves.provider.music;

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

public class MusicUpdater implements Runnable {
	private static final String LOG_TAG = "MusicUpdater";

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

	public MusicUpdater(Context context) {
		mResolver = context.getContentResolver();
		mLastModifiedFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z");
		mSelection = BaseItem._ID + "=?";
	}

	public void start() {
		if (mThread == null) {
			mStopped = false;
			mThread = new Thread(this, "MusicUpdater");
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

	public void offer(String... music) {
		for (String musicId : music) {
			if (musicId != null)
				mQueue.offer(musicId);
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
				final String musicId = mQueue.take();

				final Long lastCheck = sLastChecks.get(musicId);
				if (lastCheck != null
						&& (lastCheck + ONE_DAY) >= System.currentTimeMillis()) {
					continue;
				}
				sLastChecks.put(musicId, System.currentTimeMillis());

				final MusicStore.Music music = MusicManager.findMusic(
						mResolver, musicId, null);

				if (music == null)
					continue;

				final String imgURL = Preferences.getImageURLForUpdater(music);

				if (music.getLastModified() == null || imgURL == null) {
					continue;
				}

				if (musicCoverUpdated(music, expiring)
						&& expiring.lastModified != null) {
					ImageUtilities.deleteCachedCover(musicId);

					final Bitmap bitmap = Preferences
							.getBitmapForManager(music);

					ImportUtilities.addCoverToCache(music.getInternalId(),
							bitmap);

					if (bitmap != null)
						bitmap.recycle();

					mValues.put(BaseItem.LAST_MODIFIED,
							expiring.lastModified.getTimeInMillis());
					mArguments[0] = musicId;
					mResolver.update(MusicStore.Music.CONTENT_URI, mValues,
							mSelection, mArguments);
				}

				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// Ignore
			}
		}
	}

	private boolean musicCoverUpdated(MusicStore.Music music,
			ImageUtilities.ExpiringBitmap expiring) {
		expiring.lastModified = null;

		final String tinyThumbnail = Preferences.getImageURLForUpdater(music);

		if (tinyThumbnail != null && !tinyThumbnail.equals("")) {
			HttpGet get = null;
			try {
				get = new HttpGet(Preferences.getImageURLForUpdater(music));

			} catch (NullPointerException npe) {
				android.util.Log.e(LOG_TAG,
						"Could not check modification image for " + music, npe);
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
							return calendar.after(music.getLastModified());
						} catch (ParseException e) {
							return false;
						}
					}
				}
			} catch (IOException e) {
				android.util.Log.e(LOG_TAG,
						"Could not check modification date for " + music, e);
			} catch (IllegalArgumentException iae) {
				android.util.Log.e(LOG_TAG, "Null get value for " + music, iae);
			} finally {
				if (entity != null) {
					try {
						entity.consumeContent();
					} catch (IOException e) {
						android.util.Log.e(LOG_TAG,
								"Could not check modification date for "
										+ music, e);
					}
				}
			}
		}
		return false;
	}
}
