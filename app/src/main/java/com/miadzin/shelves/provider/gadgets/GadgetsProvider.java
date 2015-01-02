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

package com.miadzin.shelves.provider.gadgets;

import java.util.HashMap;
import java.util.regex.Pattern;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.LiveFolders;
import android.util.Log;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.activity.SettingsActivity;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.util.TextUtilities;

public class GadgetsProvider extends ContentProvider {
	private static final String LOG_TAG = "GadgetsProvider";

	public static final String DATABASE_NAME = "gadgets.db";
	// GJT: Version 2.2, changed to "2" to support item loans
	// GJT: Version 2.2.4, changed to "3" for UPC
	private static final int DATABASE_VERSION = 5;

	private static final int SEARCH = 1;
	private static final int GADGETS = 2;
	private static final int GADGET_ID = 3;
	private static final int LIVE_FOLDER_GADGETS = 4;

	private static final String AUTHORITY = "GadgetsProvider";

	private static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
				SEARCH);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY
				+ "/*", SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "gadgets", GADGETS);
		URI_MATCHER.addURI(AUTHORITY, "gadgets/#", GADGET_ID);
		URI_MATCHER.addURI(AUTHORITY, "live_folders/gadgets",
				LIVE_FOLDER_GADGETS);
	}

	private static final HashMap<String, String> SUGGESTION_PROJECTION_MAP;
	static {
		SUGGESTION_PROJECTION_MAP = new HashMap<String, String>();
		SUGGESTION_PROJECTION_MAP.put(SearchManager.SUGGEST_COLUMN_TEXT_1,
				BaseItem.TITLE + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
		SUGGESTION_PROJECTION_MAP
				.put(SearchManager.SUGGEST_COLUMN_TEXT_2, BaseItem.AUTHORS
						+ " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2);
		SUGGESTION_PROJECTION_MAP.put(
				SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, BaseItem._ID
						+ " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		SUGGESTION_PROJECTION_MAP.put(BaseItem._ID, BaseItem._ID);
	}

	private static final HashMap<String, String> LIVE_FOLDER_PROJECTION_MAP;
	static {
		LIVE_FOLDER_PROJECTION_MAP = new HashMap<String, String>();
		LIVE_FOLDER_PROJECTION_MAP.put(BaseColumns._ID, BaseItem._ID + " AS "
				+ BaseColumns._ID);
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders.NAME, BaseItem.TITLE
				+ " AS " + LiveFolders.NAME);
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders.DESCRIPTION,
				BaseItem.AUTHORS + " AS " + LiveFolders.DESCRIPTION);
	}

	private static SQLiteOpenHelper mOpenHelper;

	private Pattern[] mKeyPrefixes;
	private Pattern[] mKeySuffixes;

	private static Context dbContext;

	@Override
	public boolean onCreate() {
		dbContext = getContext();
		mOpenHelper = new DatabaseHelper(dbContext);

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtilities.isEmpty(sortOrder)) {
			orderBy = BaseItem.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		switch (URI_MATCHER.match(uri)) {
		case SEARCH:
			qb.setTables("gadgets");
			String query = uri.getLastPathSegment();
			if (!TextUtilities.isEmpty(query)) {
				qb.appendWhere(BaseItem.AUTHORS + " LIKE ");
				qb.appendWhereEscapeString('%' + query + '%');
				qb.appendWhere(" OR ");
				qb.appendWhere(BaseItem.TITLE + " LIKE ");
				qb.appendWhereEscapeString('%' + query + '%');
				qb.appendWhere(" OR ");
				// GJT: Used to enable tag results in suggestions bar

				// The following is to allow for searching tags in any order,
				// e.g.
				// danger, fight should return the same results as fight, danger
				if (query.contains(",")) {
					String[] tags = query.split(",");
					for (int i = 0; i < tags.length; i++) {
						String tag = tags[i].trim();

						qb.appendWhere(BaseItem.TAGS + " LIKE ");
						qb.appendWhereEscapeString('%' + tag + '%');
						if (i != tags.length - 1) {
							qb.appendWhere(" OR ");
						}
					}
				} else {
					qb.appendWhere(BaseItem.TAGS + " LIKE ");
					qb.appendWhereEscapeString('%' + query + '%');
				}
			}
			qb.setProjectionMap(SUGGESTION_PROJECTION_MAP);
			break;
		case GADGETS:
			qb.setTables("gadgets");
			break;
		case GADGET_ID:
			qb.setTables("gadgets");
			qb.appendWhere("_id=" + uri.getPathSegments().get(1));
			break;
		case LIVE_FOLDER_GADGETS: // GJT: Added this for live folder support
			qb.setTables("gadgets");
			qb.setProjectionMap(LIVE_FOLDER_PROJECTION_MAP);
			orderBy = SettingsActivity.getSortOrder();
			// GJT: Something I can't figure out...this query is being called
			// by ContentProvider$Transport.bulkQuery(), with "name ASC" as a
			// forced sort. I'll override it with the actual user pref
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case GADGETS:
			return "vnd.android.cursor.dir/vnd.com.miadzin.shelves.gadgets";
		case GADGET_ID:
			return "vnd.android.cursor.item/vnd.com.miadzin.shelves.gadgets";
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		ContentValues values;

		if (initialValues != null) {
			values = new ContentValues(initialValues);
			values.put(BaseItem.SORT_TITLE,
					keyFor(values.getAsString(BaseItem.TITLE)));
		} else {
			values = new ContentValues();
		}

		if (URI_MATCHER.match(uri) != GADGETS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final long rowId = db.insert("gadgets", BaseItem.TITLE, values);
		if (rowId > 0) {
			Uri insertUri = ContentUris.withAppendedId(
					GadgetsStore.Gadget.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(uri, null);
			ShelvesApplication.dataChanged();
			return insertUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private String keyFor(String name) {
		if (name == null)
			name = "";

		name = name.trim().toLowerCase();

		if (mKeyPrefixes == null) {
			final Resources resources = getContext().getResources();
			final String[] keyPrefixes = resources
					.getStringArray(R.array.prefixes);
			final int count = keyPrefixes.length;

			mKeyPrefixes = new Pattern[count];
			for (int i = 0; i < count; i++) {
				mKeyPrefixes[i] = Pattern
						.compile("^" + keyPrefixes[i] + "\\s+");
			}
		}

		if (mKeySuffixes == null) {
			final Resources resources = getContext().getResources();
			final String[] keySuffixes = resources
					.getStringArray(R.array.suffixes);
			final int count = keySuffixes.length;

			mKeySuffixes = new Pattern[count];
			for (int i = 0; i < count; i++) {
				mKeySuffixes[i] = Pattern
						.compile("\\s*" + keySuffixes[i] + "$");
			}
		}

		return TextUtilities.keyFor(mKeyPrefixes, mKeySuffixes, name);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		int count;
		switch (URI_MATCHER.match(uri)) {
		case GADGETS:
			count = db.delete("gadgets", selection, selectionArgs);
			break;
		case GADGET_ID:
			String segment = uri.getPathSegments().get(1);
			count = db.delete("gadgets", BaseItem._ID
					+ "="
					+ segment
					+ (!TextUtilities.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		ShelvesApplication.dataChanged();

		return count;
	}

	// GJT: Added this to allow for database deletion
	public static String getDatabaseName() {
		return DATABASE_NAME;
	}

	// GJT: This method was unimplemented; I filled it out
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		switch (URI_MATCHER.match(uri)) {
		case GADGETS:
			count = db.update("gadgets", values, selection, selectionArgs);
			break;
		case GADGET_ID:
			count = db.update("gadgets", values,
					BaseItem._ID
							+ " = "
							+ uri.getPathSegments().get(1)
							+ (!TextUtilities.isEmpty(selection) ? " AND ("
									+ selection + ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		ShelvesApplication.dataChanged();
		return count;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE gadgets (" + BaseItem._ID
					+ " INTEGER PRIMARY KEY, " + BaseItem.INTERNAL_ID
					+ " TEXT, " + BaseItem.EAN + " TEXT, " + BaseItem.ISBN
					+ " TEXT, " + BaseItem.TITLE + " TEXT, "
					+ BaseItem.SORT_TITLE + " TEXT, " + BaseItem.AUTHORS
					+ " TEXT, " + BaseItem.LABEL + " TEXT, " + BaseItem.REVIEWS
					+ " TEXT, " + BaseItem.LAST_MODIFIED + " INTEGER, "
					+ BaseItem.DETAILS_URL + " TEXT, " + BaseItem.TINY_URL
					+ " TEXT, " + BaseItem.TAGS + " TEXT, " + BaseItem.FEATURES
					+ " TEXT, "
					+ BaseItem.CONDITION
					+ " TEXT, "
					+ BaseItem.RETAIL_PRICE
					+ " TEXT, "
					+ BaseItem.RATING
					+ " INTEGER, "// GJT: below added in v2
					+ BaseItem.LOAN_DATE + " TEXT, " + BaseItem.LOANED_TO
					+ " TEXT, " + BaseItem.EVENT_ID + " INTEGER, "
					+ BaseItem.NOTES + " TEXT, " + BaseItem.UPC + " TEXT, "
					+ BaseItem.WISHLIST_DATE + " TEXT, " + BaseItem.QUANTITY
					+ " TEXT);");

			// GJT: Was this here for sorting?
			db.execSQL("CREATE INDEX gadgetIndexTitle ON gadgets("
					+ BaseItem.SORT_TITLE + ");");
			db.execSQL("CREATE INDEX gadgetIndexDirectors ON gadgets("
					+ BaseItem.AUTHORS + ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(LOG_TAG, "Upgrading database from version " + oldVersion
					+ " to " + newVersion);

			switch (oldVersion) {
			case 1:
				db.execSQL("ALTER TABLE gadgets ADD COLUMN "
						+ BaseItem.LOANED_TO + " TEXT");
				db.execSQL("ALTER TABLE gadgets ADD COLUMN "
						+ BaseItem.LOAN_DATE + " TEXT");
				db.execSQL("ALTER TABLE gadgets ADD COLUMN "
						+ BaseItem.EVENT_ID + " INTEGER");
				db.execSQL("ALTER TABLE gadgets ADD COLUMN " + BaseItem.NOTES
						+ " TEXT");
			case 2:
				db.execSQL("ALTER TABLE gadgets ADD COLUMN " + BaseItem.UPC
						+ " TEXT");
			case 3:
				db.execSQL("ALTER TABLE gadgets ADD COLUMN "
						+ BaseItem.WISHLIST_DATE + " TEXT");
			case 4:
				db.execSQL("ALTER TABLE gadgets ADD COLUMN "
						+ BaseItem.QUANTITY + " TEXT");
				break;
			default:
				break;
			}
		}
	}

	public static boolean deleteDatabase(Context context) {
		return context.deleteDatabase(DATABASE_NAME);
	}

	public static String[] getAllFromColumn(String column) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		Cursor cursor = db.query(
				DATABASE_NAME.substring(0, DATABASE_NAME.length() - 3),
				new String[] { column }, null, null, null, null, null);

		if (cursor.getCount() > 0) {
			String[] str = new String[cursor.getCount()];
			int i = 0;

			while (cursor.moveToNext()) {
				str[i] = cursor.getString(cursor.getColumnIndex(column));
				i++;
			}
			return str;
		} else {
			return new String[] {};
		}
	}
}
