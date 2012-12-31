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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;

import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore.BoardGame;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.ImportUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.loan.Calendars;

public class BoardGamesManager {
	static int BOARDGAME_COVER_WIDTH;
	static int BOARDGAME_COVER_HEIGHT;

	private static String sIdSelection;
	private static String sSelection;

	private static String[] sArguments1 = new String[1];
	private static String[] sArguments4 = new String[4];

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
		selection.append(" LIKE ? OR ");
		selection.append(BaseItem.ISBN);
		selection.append(" LIKE ?");
		sSelection = selection.toString();
	}

	private BoardGamesManager() {
	}

	public static String findBoardGameId(ContentResolver contentResolver,
			String id, String sortOrder) {
		String internalId = null;
		Cursor c = null;

		try {
			final String[] arguments4 = sArguments4;
			arguments4[0] = arguments4[1] = arguments4[2] = arguments4[3] = id;
			c = contentResolver.query(BoardGamesStore.BoardGame.CONTENT_URI,
					PROJECTION_ID_IID, sSelection, arguments4, sortOrder);
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

	public static boolean boardgameExists(ContentResolver contentResolver,
			String id, String sortOrder, IOUtilities.inputTypes type) {
		boolean exists;
		Cursor c = null;

		try {
			final String[] arguments4 = sArguments4;
			arguments4[0] = arguments4[1] = arguments4[2] = arguments4[3] = id
					.replaceFirst("^0+(?!$)", "");

			c = contentResolver.query(BoardGamesStore.BoardGame.CONTENT_URI,
					PROJECTION_ID, sSelection, arguments4, sortOrder);
			exists = c.getCount() > 0;
		} finally {
			if (c != null)
				c.close();
		}

		return exists;
	}

	public static BoardGamesStore.BoardGame loadAndAddBoardGame(
			ContentResolver resolver, String id,
			BoardGamesStore boardgamesStore, IOUtilities.inputTypes type,
			Context context) {

		final BoardGamesStore.BoardGame boardgame = boardgamesStore
				.findBoardGame(id, type, context);
		if (boardgame != null) {
			Bitmap bitmap = null;

			bitmap = Preferences.getBitmapForManager(boardgame);
			BOARDGAME_COVER_WIDTH = Preferences.getWidthForManager();
			BOARDGAME_COVER_HEIGHT = Preferences.getHeightForManager();

			if (bitmap != null) {
				bitmap = ImageUtilities.createCover(bitmap,
						BOARDGAME_COVER_WIDTH, BOARDGAME_COVER_HEIGHT);
				ImportUtilities.addCoverToCache(boardgame.getInternalId(),
						bitmap);
				bitmap.recycle();
			}

			// Should kill duplicate item entry bug...
			Cursor c = null;
			sArguments1[0] = boardgame.getInternalId();
			c = resolver.query(BoardGamesStore.BoardGame.CONTENT_URI, null,
					BaseItem.INTERNAL_ID + "='" + boardgame.getInternalId()
							+ "'", null, null);
			if (c.moveToFirst()) {
				if (c.getCount() < 1) {
					final Uri uri = resolver.insert(
							BoardGamesStore.BoardGame.CONTENT_URI,
							boardgame.getContentValues());
					if (uri != null) {
						if (c != null) {
							c.close();
						}
						return boardgame;
					}
				}
			} else {
				if (c != null) {
					c.close();
				}
				final Uri uri = resolver.insert(
						BoardGamesStore.BoardGame.CONTENT_URI,
						boardgame.getContentValues());
				return boardgame;
			}
		}

		return null;
	}

	public static boolean deleteBoardGame(ContentResolver contentResolver,
			String boardgameId) {
		BoardGame boardgame = BoardGamesManager.findBoardGame(contentResolver,
				boardgameId, null);
		int eventId = 0;

		if (boardgame != null) {
			eventId = boardgame.getEventId();
		}

		final String[] arguments1 = sArguments1;
		arguments1[0] = boardgameId;
		int count = contentResolver
				.delete(BoardGamesStore.BoardGame.CONTENT_URI, sIdSelection,
						arguments1);
		ImageUtilities.deleteCachedCover(boardgameId);

		if (eventId > 0) {
			Calendars.deleteCalendar(contentResolver, boardgame);
		}
		return count > 0;
	}

	public static BoardGamesStore.BoardGame findBoardGame(
			ContentResolver contentResolver, String id, String sortOrder) {
		BoardGamesStore.BoardGame boardgame = null;
		Cursor c = null;

		try {
			sArguments1[0] = id;
			c = contentResolver.query(BoardGamesStore.BoardGame.CONTENT_URI,
					null, sIdSelection, sArguments1, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					boardgame = BoardGamesStore.BoardGame.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return boardgame;
	}

	public static BoardGamesStore.BoardGame findBoardGame(
			ContentResolver contentResolver, Uri data, String sortOrder) {
		BoardGamesStore.BoardGame boardgame = null;
		Cursor c = null;

		try {
			c = contentResolver.query(data, null, null, null, null);
			if (c != null && c.getCount() > 0) {
				if (c.moveToFirst()) {
					boardgame = BoardGamesStore.BoardGame.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return boardgame;
	}

	public static BoardGamesStore.BoardGame findBoardGameById(
			ContentResolver contentResolver, String id, String sortOrder) {
		BoardGamesStore.BoardGame boardgame = null;
		Cursor c = null;

		try {
			final String[] arguments4 = sArguments4;
			arguments4[0] = arguments4[1] = arguments4[2] = arguments4[3] = id;

			c = contentResolver.query(BoardGamesStore.BoardGame.CONTENT_URI,
					null, sSelection, sArguments4, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					boardgame = BoardGamesStore.BoardGame.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return boardgame;
	}
}
