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

package com.miadzin.shelves.provider.movies;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;

import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.provider.movies.MoviesStore.Movie;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.ImportUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.loan.Calendars;

public class MoviesManager {
	static int MOVIE_COVER_WIDTH;
	static int MOVIE_COVER_HEIGHT;

	private static String sIdSelection;
	private static String sSelection;

	private static String[] sArguments1 = new String[1];
	private static String[] sArguments4 = new String[4];

	private static final String[] PROJECTION_ID_IID = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID };

	protected static final String[] PROJECTION_ID = new String[] { BaseItem._ID };

	private static final String LOG_TAG = "MoviesManager";
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

	private MoviesManager() {
	}

	public static String findMovieId(ContentResolver contentResolver,
			String id, String sortOrder) {
		String internalId = null;
		Cursor c = null;

		try {
			final String[] arguments4 = sArguments4;
			arguments4[0] = arguments4[1] = arguments4[2] = arguments4[3] = id;
			c = contentResolver.query(MoviesStore.Movie.CONTENT_URI,
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

	public static boolean movieExists(ContentResolver contentResolver,
			String id, String sortOrder, IOUtilities.inputTypes type) {
		boolean exists;
		Cursor c = null;

		try {
			final String[] arguments4 = sArguments4;
			arguments4[0] = arguments4[1] = arguments4[2] = arguments4[3] = id
					.replaceFirst("^0+(?!$)", "");

			c = contentResolver.query(MoviesStore.Movie.CONTENT_URI,
					PROJECTION_ID, sSelection, arguments4, sortOrder);
			exists = c.getCount() > 0;
		} finally {
			if (c != null)
				c.close();
		}

		return exists;
	}

	public static MoviesStore.Movie loadAndAddMovie(ContentResolver resolver,
			String id, MoviesStore moviesStore,
			IOUtilities.inputTypes mSavedImportType, Context context) {

		final MoviesStore.Movie movie = moviesStore.findMovie(id,
				mSavedImportType, context);
		if (movie != null) {
			Bitmap bitmap = null;

			bitmap = Preferences.getBitmapForManager(movie);
			MOVIE_COVER_WIDTH = Preferences.getWidthForManager();
			MOVIE_COVER_HEIGHT = Preferences.getHeightForManager();

			if (bitmap != null) {
				bitmap = ImageUtilities.createCover(bitmap, MOVIE_COVER_WIDTH,
						MOVIE_COVER_HEIGHT);
				ImportUtilities.addCoverToCache(movie.getInternalId(), bitmap);
				bitmap.recycle();
			}

			// Should kill duplicate item entry bug...
			Cursor c = null;
			sArguments1[0] = movie.getInternalId();
			c = resolver.query(MoviesStore.Movie.CONTENT_URI, null,
					BaseItem.INTERNAL_ID + "='" + movie.getInternalId() + "'",
					null, null);
			if (c.moveToFirst()) {
				if (c.getCount() < 1) {
					final Uri uri = resolver.insert(
							MoviesStore.Movie.CONTENT_URI,
							movie.getContentValues());
					if (uri != null) {
						if (c != null) {
							c.close();
						}
						return movie;
					}
				}
			} else {
				if (c != null) {
					c.close();
				}
				final Uri uri = resolver.insert(MoviesStore.Movie.CONTENT_URI,
						movie.getContentValues());
				return movie;
			}
		}

		return null;
	}

	public static boolean deleteMovie(ContentResolver contentResolver,
			String movieId) {
		Movie movie = MoviesManager.findMovie(contentResolver, movieId, null);
		int eventId = 0;

		if (movie != null) {
			eventId = movie.getEventId();
		}

		final String[] arguments1 = sArguments1;
		arguments1[0] = movieId;
		int count = contentResolver.delete(MoviesStore.Movie.CONTENT_URI,
				sIdSelection, arguments1);
		ImageUtilities.deleteCachedCover(movieId);

		if (eventId > 0) {
			Calendars.deleteCalendar(contentResolver, movie);
		}

		return count > 0;
	}

	public static MoviesStore.Movie findMovie(ContentResolver contentResolver,
			String id, String sortOrder) {
		MoviesStore.Movie movie = null;
		Cursor c = null;

		try {
			sArguments1[0] = id;
			c = contentResolver.query(MoviesStore.Movie.CONTENT_URI, null,
					sIdSelection, sArguments1, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					movie = MoviesStore.Movie.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return movie;
	}

	public static MoviesStore.Movie findMovie(ContentResolver contentResolver,
			Uri data, String sortOrder) {
		MoviesStore.Movie movie = null;
		Cursor c = null;

		try {
			c = contentResolver.query(data, null, null, null, null);
			if (c != null && c.getCount() > 0) {
				if (c.moveToFirst()) {
					movie = MoviesStore.Movie.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return movie;
	}

	public static MoviesStore.Movie findMovieById(
			ContentResolver contentResolver, String id, String sortOrder) {
		MoviesStore.Movie movie = null;
		Cursor c = null;

		try {
			final String[] arguments4 = sArguments4;
			arguments4[0] = arguments4[1] = arguments4[2] = arguments4[3] = id;

			c = contentResolver.query(MoviesStore.Movie.CONTENT_URI, null,
					sSelection, sArguments4, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					movie = MoviesStore.Movie.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}

		return movie;
	}
}
