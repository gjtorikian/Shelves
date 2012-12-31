/*
 * Copyright (C) 2011 Garen J. Torikian
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

package com.miadzin.shelves.base;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.util.TextUtilities;

public abstract class BaseItemProvider {
	public final String LOG_TAG = "BaseItemProvider";

	public static final String[] APPAREL_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.LOANED_TO, BaseItem.RATING, BaseItem.RETAIL_PRICE,
			BaseItem.DEPARTMENT, BaseItem.FABRIC, BaseItem.FEATURES,
			BaseItem.CONDITION, BaseItem.WISHLIST_DATE, BaseItem.QUANTITY };

	public static final String[] BOARDGAMES_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.LOANED_TO, BaseItem.RATING, BaseItem.AGE,
			BaseItem.PLAYING_TIME, BaseItem.MIN_PLAYERS, BaseItem.MAX_PLAYERS,
			BaseItem.WISHLIST_DATE, BaseItem.QUANTITY };

	public static final String[] BOOKS_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.LOANED_TO, BaseItem.RATING, BaseItem.PUBLISHER,
			BaseItem.PAGES, BaseItem.FORMAT, BaseItem.RETAIL_PRICE,
			BaseItem.DEWEY_NUMBER, BaseItem.CONDITION, BaseItem.WISHLIST_DATE,
			BaseItem.QUANTITY };

	public static final String[] COMICS_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.LOANED_TO, BaseItem.RATING, BaseItem.PUBLISHER,
			BaseItem.ARTISTS, BaseItem.CHARACTERS, BaseItem.ISSUE_NUMBER,
			BaseItem.WISHLIST_DATE, BaseItem.QUANTITY };

	public static final String[] GADGETS_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.FEATURES, BaseItem.CONDITION, BaseItem.LOANED_TO,
			BaseItem.RATING, BaseItem.RETAIL_PRICE, BaseItem.WISHLIST_DATE,
			BaseItem.QUANTITY };

	public static final String[] MOVIES_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.ACTORS, BaseItem.DIRECTORS,
			BaseItem.TAGS, BaseItem.LOANED_TO, BaseItem.RATING,
			BaseItem.RETAIL_PRICE, BaseItem.FORMAT, BaseItem.LABEL,
			BaseItem.AUDIENCE, BaseItem.FEATURES, BaseItem.LANGUAGES,
			BaseItem.WISHLIST_DATE, BaseItem.QUANTITY };

	public static final String[] MUSIC_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.LOANED_TO, BaseItem.RATING, BaseItem.RETAIL_PRICE,
			BaseItem.FORMAT, BaseItem.LABEL, BaseItem.TRACKS,
			BaseItem.WISHLIST_DATE, BaseItem.QUANTITY };

	public static final String[] SOFTWARE_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.LOANED_TO, BaseItem.RATING, BaseItem.RETAIL_PRICE,
			BaseItem.FORMAT, BaseItem.PLATFORM, BaseItem.LABEL,
			BaseItem.WISHLIST_DATE, BaseItem.QUANTITY };

	public static final String[] TOOLS_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.LOANED_TO, BaseItem.RATING, BaseItem.RETAIL_PRICE,
			BaseItem.CONDITION, BaseItem.FEATURES, BaseItem.WISHLIST_DATE,
			BaseItem.QUANTITY };

	public static final String[] TOYS_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.LOANED_TO, BaseItem.RATING, BaseItem.RETAIL_PRICE,
			BaseItem.CONDITION, BaseItem.FEATURES, BaseItem.WISHLIST_DATE,
			BaseItem.QUANTITY };

	public static final String[] VIDEOGAMES_PROJECTION_IDS_AND_TITLE = new String[] {
			BaseItem._ID, BaseItem.INTERNAL_ID, BaseItem.TITLE,
			BaseItem.SORT_TITLE, BaseItem.AUTHORS, BaseItem.TAGS,
			BaseItem.LOANED_TO, BaseItem.RATING, BaseItem.RETAIL_PRICE,
			BaseItem.PLATFORM, BaseItem.ESRB, BaseItem.FORMAT, BaseItem.GENRE,
			BaseItem.CONDITION, BaseItem.FEATURES, BaseItem.WISHLIST_DATE,
			BaseItem.QUANTITY };

	// Object intrinsic BackupAgent lock
	public static final Object[] mDBLock = new Object[0];

	public static Cursor runQuery(Activity mActivity, String mSortOrder,
			CharSequence constraint) {
		final StringBuilder selection = new StringBuilder();
		String[] projectionArray = null;
		final String activityToMatch = mActivity.toString();
		final Uri uri = getActivityUri(activityToMatch);

		if (activityToMatch.contains("Apparel")) {
			projectionArray = APPAREL_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.DEPARTMENT);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.FABRIC);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.FEATURES);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.CONDITION);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("BoardGames")) {
			projectionArray = BOARDGAMES_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.AGE);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.MIN_PLAYERS);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.MAX_PLAYERS);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.PLAYING_TIME);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("Books")) {
			projectionArray = BOOKS_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.PUBLISHER);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.FORMAT);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.DEWEY_NUMBER);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.CONDITION);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("Comics")) {
			projectionArray = COMICS_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.ARTISTS);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.CHARACTERS);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.ISSUE_NUMBER);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("Gadgets")) {
			projectionArray = GADGETS_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.FEATURES);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.CONDITION);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("Movies")) {
			projectionArray = MOVIES_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.ACTORS);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.LABEL);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.FORMAT);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.AUDIENCE);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.FEATURES);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.LANGUAGES);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("Music")) {
			projectionArray = MUSIC_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.LABEL);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.FORMAT);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.TRACKS);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("Software")) {
			projectionArray = SOFTWARE_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.LABEL);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.PLATFORM);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.FORMAT);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("Tools")) {
			projectionArray = TOOLS_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.FEATURES);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.CONDITION);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("Toys")) {
			projectionArray = TOYS_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.FEATURES);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.CONDITION);
			selection.append(" LIKE ? OR ");
		} else if (activityToMatch.contains("VideoGames")) {
			projectionArray = VIDEOGAMES_PROJECTION_IDS_AND_TITLE;

			selection.append(BaseItem.PLATFORM);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.ESRB);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.FORMAT);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.GENRE);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.FEATURES);
			selection.append(" LIKE ? OR ");
			selection.append(BaseItem.CONDITION);
			selection.append(" LIKE ? OR ");
		}

		selection.append(BaseItem.TITLE);
		selection.append(" LIKE ? OR ");
		selection.append(BaseItem.AUTHORS);
		selection.append(" LIKE ? OR ");
		selection.append(BaseItem.LOANED_TO);
		selection.append(" LIKE ? OR ");
		selection.append(BaseItem.TAGS);
		selection.append(" LIKE ?");

		final String mSelection = selection.toString();

		if (constraint == null || constraint.length() == 0) {
			return mActivity.managedQuery(uri, projectionArray, null, null,
					mSortOrder);
		}

		final StringBuilder buffer = new StringBuilder();
		String stringConstraint = constraint.toString();

		stringConstraint = TextUtilities.itrim(TextUtilities
				.rtrim(stringConstraint));

		if (stringConstraint.contains(",")) {
			String[] tags = stringConstraint.split(", ");
			for (int i = 0; i < tags.length; i++) {
				CharSequence tag = tags[i].trim();

				buffer.append('%').append(tag).append('%');
			}
		}

		else {
			buffer.append('%').append(stringConstraint).append('%');
		}

		final int arrSize = TextUtilities.countMatches(mSelection, "LIKE");
		final String[] arguments = new String[arrSize];
		final String strBuffer = buffer.toString();

		for (int i = 0; i < arrSize; i++) {
			arguments[i] = strBuffer;
		}

		return mActivity.managedQuery(uri, projectionArray, mSelection,
				arguments, mSortOrder);
	}

	static public Uri getActivityUri(String activityToMatch) {
		Uri uri = null;

		if (activityToMatch.contains("Apparel"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(R.string.apparel_label));
		else if (activityToMatch.contains("BoardGames"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(
							R.string.boardgame_label_plural_small));
		else if (activityToMatch.contains("Books"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(R.string.book_label_plural_small));
		else if (activityToMatch.contains("Comics"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(R.string.comic_label_plural_small));
		else if (activityToMatch.contains("Gadgets"))
			uri = ShelvesApplication.TYPES_TO_URI
					.get(ShelvesApplication.getContext().getString(
							R.string.gadget_label_plural_small));
		else if (activityToMatch.contains("Movies"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(R.string.movie_label_plural_small));
		else if (activityToMatch.contains("Music"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(R.string.music_label_plural_small));
		else if (activityToMatch.contains("Software"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(R.string.software_label));
		else if (activityToMatch.contains("Tools"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(R.string.tool_label_plural_small));
		else if (activityToMatch.contains("Toys"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(R.string.toy_label_plural_small));
		else if (activityToMatch.contains("VideoGames"))
			uri = ShelvesApplication.TYPES_TO_URI.get(ShelvesApplication
					.getContext().getString(
							R.string.videogame_label_plural_small));

		return uri;
	}
}