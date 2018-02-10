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

package com.miadzin.shelves.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.base.BaseItem;

import java.text.SimpleDateFormat;
import java.util.Locale;

public final class Preferences {
	public static final String NAME = "Shelves";

	public static final String KEY_BOOKSTORE = "shelves.bookstore";
	public static final String KEY_DATABASE = "shelves.database"; // GJT: Added
	// this, for
	// database
	// region
	public static final String KEY_SORT = "shelves.sort"; // GJT: Added this,
	// for sorting
	public static final String KEY_SORT_NUM = "shelves.sort_num"; // GJT: Added
																	// this,
	// for sorting
	public static final String KEY_CALENDAR = "shelves.calendar"; // GJT: Added
	// this, for
	// loan
	// notifications

	public static final String KEY_CHANGED_DATE_RANGE = "shelves.recentDate";

	public static final String KEY_IMPORT = "shelves.import";
	public static final String KEY_EXPORT = "shelves.export"; // GJT: Added
																// this, for
																// exporting

	public static final String KEY_IMPORT_EXPORT = "shelves.importExport";
	public static final String SEND_TO_GOOGLE_DOCS = "shelves.sendToGoogleDocs";
	public static final String BRING_FROM_GOOGLE_DOCS = "shelves.bringFromGoogleDocs";

	public static final String AUTHENTICATE_DROPBOX = "shelves.authenticateDropbox";
	public static final String SEND_TO_DROPBOX = "shelves.sendToDropbox";
	public static final String BRING_FROM_DROPBOX = "shelves.bringFromDropbox";

	public static final String DOWNLOAD_COVER_APPAREL = "shelves.downloadCoverApparel";
	public static final String DOWNLOAD_COVER_BOARDGAMES = "shelves.downloadCoverBoardGames";
	public static final String DOWNLOAD_COVER_BOOKS = "shelves.downloadCoverBooks";
	public static final String DOWNLOAD_COVER_COMICS = "shelves.downloadCoverComics";
	public static final String DOWNLOAD_COVER_GADGETS = "shelves.downloadCoverGadgets";
	public static final String DOWNLOAD_COVER_MOVIES = "shelves.downloadCoverMovies";
	public static final String DOWNLOAD_COVER_MUSIC = "shelves.downloadCoverMusic";
	public static final String DOWNLOAD_COVER_SOFTWARE = "shelves.downloadCoverSoftware";
	public static final String DOWNLOAD_COVER_TOOLS = "shelves.downloadCoverTools";
	public static final String DOWNLOAD_COVER_TOYS = "shelves.downloadCoverToys";
	public static final String DOWNLOAD_COVER_VIDEOGAMES = "shelves.downloadCoverVideoGames";

	// GJT: Added the following, for deleting all data
	public static final String DELETE_ALL_APPAREL = "shelves.deleteApparel";
	public static final String DELETE_ALL_BOARDGAMES = "shelves.deleteBoardGames";
	public static final String DELETE_ALL_BOOKS = "shelves.deleteBooks";
	public static final String DELETE_ALL_COMICS = "shelves.deleteComics";
	public static final String DELETE_ALL_GADGETS = "shelves.deleteGadgets";
	public static final String DELETE_ALL_MOVIES = "shelves.deleteMovies";
	public static final String DELETE_ALL_MUSIC = "shelves.deleteMusic";
	public static final String DELETE_ALL_SOFTWARE = "shelves.deleteSoftware";
	public static final String DELETE_ALL_TOOLS = "shelves.deleteTools";
	public static final String DELETE_ALL_TOYS = "shelves.deleteToys";
	public static final String DELETE_ALL_VIDEOGAMES = "shelves.deleteVideoGames";

	public static final String KEY_ITEMS = "shelves.items"; // GJT: Added this,
															// for displaying
															// icons at starting
															// grid

	public static final String SHELVES_INI = "shelves.ini";
	public static final String KEY_DPI = "DPI=";

	public static final String KEY_APPAREL_SORT = "Apparel_sortType";
	public static final String KEY_BOARDGAME_SORT = "BoardGame_sortType";
	public static final String KEY_BOOK_SORT = "Book_sortType";
	public static final String KEY_COMIC_SORT = "Comic_sortType";
	public static final String KEY_GADGET_SORT = "Gadget_sortType";
	public static final String KEY_MOVIE_SORT = "Movie_sortType";
	public static final String KEY_MUSIC_SORT = "Music_sortType";
	public static final String KEY_SOFTWARE_SORT = "Software_sortType";
	public static final String KEY_TOOL_SORT = "Tool_sortType";
	public static final String KEY_TOY_SORT = "Toy_sortType";
	public static final String KEY_VIDEOGAME_SORT = "VideoGame_sortType";

	public static final String KEY_APPAREL_VIEW = "Apparel_viewType";
	public static final String KEY_BOARDGAME_VIEW = "BoardGame_viewType";
	public static final String KEY_BOOK_VIEW = "Book_viewType";
	public static final String KEY_COMIC_VIEW = "Comic_viewType";
	public static final String KEY_GADGET_VIEW = "Gadget_viewType";
	public static final String KEY_MOVIE_VIEW = "Movie_viewType";
	public static final String KEY_MUSIC_VIEW = "Music_viewType";
	public static final String KEY_SOFTWARE_VIEW = "Software_viewType";
	public static final String KEY_TOOL_VIEW = "Tool_viewType";
	public static final String KEY_TOY_VIEW = "Toy_viewType";
	public static final String KEY_VIDEOGAME_VIEW = "VideoGame_viewType";

	public static final String KEY_OVERRIDE_IMPORT = "overrideImport";

	public static final String KEY_AD = "ad";

	private static final String LOG_TAG = "Preferences";

	Preferences() {
	}

	// GJT: All the methods below taken from Astrid and modified for use
	// in the Shelves loan Add to Calendar mechanism

	public static boolean is24HourFormat(Context context) {
		String value = android.provider.Settings.System.getString(
				context.getContentResolver(),
				android.provider.Settings.System.TIME_12_24);
		boolean b24 = !(value == null || value.equals("12"));
		return b24;
	}

	public static SimpleDateFormat getTimeFormat(Context context) {
		String value;
		if (is24HourFormat(context)) {
			value = "H:mm";
		} else {
			value = "h:mm a";
		}

		return new SimpleDateFormat(value);
	}

	public static SimpleDateFormat getDateFormat() {
		// united states, you are backwards
		if (Locale.US.equals(Locale.getDefault())
				|| Locale.CANADA.equals(Locale.getDefault()))
			return new SimpleDateFormat("EEE, MMM d yyyy");
		else
			return new SimpleDateFormat("EEE, d MMM yyyy");
	}

	public static SimpleDateFormat getYearMonthFormat() {
		return new SimpleDateFormat("MMM yyyy");
	}

	/** Get default calendar id. */
	public static String getDefaultCalendarID(Context context) {
		Resources r = context.getResources();
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(r.getString(R.string.preferences_calendar_default),
						r.getString(R.string.preferences_calendar_default_id));
	}

	public static int getDPI() {
		switch (ShelvesApplication.getContext().getResources()
				.getDisplayMetrics().densityDpi) {
		case DisplayMetrics.DENSITY_LOW:
			return 120;
		case DisplayMetrics.DENSITY_MEDIUM:
			return 160;
		case DisplayMetrics.DENSITY_HIGH:
			return 240;
		default: // XHIGH, XXHIGH, XXXHIGH...
			return 320;
		}

	}

	public static Bitmap getBitmapForManager(BaseItem item) {
		final int density = Preferences.getDPI();
		switch (density) {
		case 320:
			return item.loadCover(BaseItem.ImageSize.LARGE);
		case 240:
			return item.loadCover(BaseItem.ImageSize.MEDIUM);
		case 120:
			return item.loadCover(BaseItem.ImageSize.THUMBNAIL);
		case 160:
		default:
			return item.loadCover(BaseItem.ImageSize.TINY);
		}
	}

	public static int getWidthForManager() {
		final int density = Preferences.getDPI();
		switch (density) {
		case 320:
			return 200;
		case 240:
			return 150;
		case 120:
			return 75;
		case 160:
		default:
			return 100;
		}
	}

	public static int getHeightForManager() {
		final int density = Preferences.getDPI();

		switch (density) {
		case 320:
			return 240;
		case 240:
			return 180;
		case 120:
			return 90;
		case 160:
		default:
			return 120;
		}
	}

	public static String getImageURLForUpdater(BaseItem item) {
		final int density = Preferences.getDPI();
		switch (density) {
		case 320:
			return item.getImageUrl(BaseItem.ImageSize.LARGE);
		case 240:
			return item.getImageUrl(BaseItem.ImageSize.MEDIUM);
		case 120:
			return item.getImageUrl(BaseItem.ImageSize.THUMBNAIL);
		case 160:
		default:
			return item.getImageUrl(BaseItem.ImageSize.TINY);
		}
	}
}
