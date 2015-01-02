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

package com.miadzin.shelves.provider.videogames;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;

import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.provider.videogames.VideoGamesStore.VideoGame;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.ImportUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.loan.Calendars;

// GJT: Note that this item doesn't have an ISBN column
public class VideoGamesManager {
	static int VIDEOGAME_COVER_WIDTH;
	static int VIDEOGAME_COVER_HEIGHT;

	private static String sIdSelection;
	private static String sSelection;

	private static String[] sArguments1 = new String[1];
	private static String[] sArguments3 = new String[3];

	private static final String[] PROJECTION_ID_IID = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID };

	private static final String[] PROJECTION_ID = new String[] { BaseItem._ID };

	static {
		StringBuilder selection = new StringBuilder();
		selection.append(BaseItem.INTERNAL_ID);
		selection.append(" LIKE ?");
		sIdSelection = selection.toString();

		selection = new StringBuilder();
		selection.append(sIdSelection);
		selection.append(" OR ");
		selection.append(BaseItem.EAN);
		selection.append(" LIKE ? OR ");
		selection.append(BaseItem.UPC);
		selection.append(" LIKE ?");
		sSelection = selection.toString();
	}

	private VideoGamesManager() {
	}

	public static String findVideoGameId(ContentResolver contentResolver,
			String id, String sortOrder) {
		String internalId = null;
		Cursor c = null;

		try {
			final String[] arguments3 = sArguments3;
			arguments3[0] = arguments3[1] = arguments3[2] = id;
			c = contentResolver.query(VideoGamesStore.VideoGame.CONTENT_URI,
					PROJECTION_ID_IID, sSelection, arguments3, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					internalId = c.getString(c
							.getColumnIndexOrThrow(BaseItem.INTERNAL_ID));
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return internalId;
	}

	public static boolean videogameExists(ContentResolver contentResolver,
			String id, String sortOrder, IOUtilities.inputTypes type) {
		boolean exists;
		Cursor c = null;

		try {
			final String[] arguments3 = sArguments3;
			arguments3[0] = arguments3[1] = arguments3[2] = id.replaceFirst(
					"^0+(?!$)", "");

			c = contentResolver.query(VideoGamesStore.VideoGame.CONTENT_URI,
					PROJECTION_ID, sSelection, arguments3, sortOrder);
			exists = c.getCount() > 0;
		} finally {
			if (c != null)
				c.close();
		}

		return exists;
	}

	public static VideoGamesStore.VideoGame loadAndAddVideoGame(
			ContentResolver resolver, String id,
			VideoGamesStore videogamesStore,
			IOUtilities.inputTypes mSavedImportType, Context context) {

		final VideoGamesStore.VideoGame videogame = videogamesStore
				.findVideoGame(id, mSavedImportType, context);
		if (videogame != null) {
			Bitmap bitmap = null;

			bitmap = Preferences.getBitmapForManager(videogame);
			VIDEOGAME_COVER_WIDTH = Preferences.getWidthForManager();
			VIDEOGAME_COVER_HEIGHT = Preferences.getHeightForManager();

			if (bitmap != null) {
				bitmap = ImageUtilities.createCover(bitmap,
						VIDEOGAME_COVER_WIDTH, VIDEOGAME_COVER_HEIGHT);
				ImportUtilities.addCoverToCache(videogame.getInternalId(),
						bitmap);
				bitmap.recycle();
			}

			// Should kill duplicate item entry bug...
			Cursor c = null;
			sArguments1[0] = videogame.getInternalId();
			c = resolver.query(VideoGamesStore.VideoGame.CONTENT_URI, null,
					BaseItem.INTERNAL_ID + "='" + videogame.getInternalId()
							+ "'", null, null);
			if (c.moveToFirst()) {
				if (c.getCount() < 1) {
					final Uri uri = resolver.insert(
							VideoGamesStore.VideoGame.CONTENT_URI,
							videogame.getContentValues());
					if (uri != null) {
						if (c != null) {
							c.close();
						}
						return videogame;
					}
				}
			} else {
				if (c != null) {
					c.close();
				}
				final Uri uri = resolver.insert(
						VideoGamesStore.VideoGame.CONTENT_URI,
						videogame.getContentValues());
				return videogame;
			}
		}

		return null;
	}

	public static boolean deleteVideoGame(ContentResolver contentResolver,
			String videogameId) {
		VideoGame videogame = VideoGamesManager.findVideoGame(contentResolver,
				videogameId, null);
		int eventId = 0;

		if (videogame != null) {
			eventId = videogame.getEventId();
		}

		final String[] arguments1 = sArguments1;
		arguments1[0] = videogameId;
		int count = contentResolver
				.delete(VideoGamesStore.VideoGame.CONTENT_URI, sIdSelection,
						arguments1);
		ImageUtilities.deleteCachedCover(videogameId);

		if (eventId > 0) {
			Calendars.deleteCalendar(contentResolver, videogame);
		}

		return count > 0;
	}

	public static VideoGamesStore.VideoGame findVideoGame(
			ContentResolver contentResolver, String id, String sortOrder) {
		VideoGamesStore.VideoGame videogame = null;
		Cursor c = null;

		try {
			sArguments1[0] = id;
			c = contentResolver.query(VideoGamesStore.VideoGame.CONTENT_URI,
					null, sIdSelection, sArguments1, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					videogame = VideoGamesStore.VideoGame.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return videogame;
	}

	public static VideoGamesStore.VideoGame findVideoGame(
			ContentResolver contentResolver, Uri data, String sortOrder) {
		VideoGamesStore.VideoGame videogame = null;
		Cursor c = null;

		try {
			c = contentResolver.query(data, null, null, null, null);
			if (c != null && c.getCount() > 0) {
				if (c.moveToFirst()) {
					videogame = VideoGamesStore.VideoGame.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return videogame;
	}

	public static VideoGamesStore.VideoGame findVideoGameById(
			ContentResolver contentResolver, String id, String sortOrder) {
		VideoGamesStore.VideoGame videogame = null;
		Cursor c = null;

		try {
			final String[] arguments3 = sArguments3;
			arguments3[0] = arguments3[1] = arguments3[2] = id;

			c = contentResolver.query(VideoGamesStore.VideoGame.CONTENT_URI,
					null, sSelection, sArguments3, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					videogame = VideoGamesStore.VideoGame.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return videogame;
	}
}
