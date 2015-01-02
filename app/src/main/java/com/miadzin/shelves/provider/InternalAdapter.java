/*
 * Copyright (C) 2010 Garen J Torikian
 * Taken liberally from Last.fm Android client
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

package com.miadzin.shelves.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.miadzin.shelves.base.BaseItem;

public class InternalAdapter {
	public static final String KEY_TIMES = "times";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_LICENSE = "license_code";
	public static final String KEY_DROPBOX_PUB = "dropbox_pub";
	public static final String KEY_DROPBOX_SEC = "dropbox_sec";

	private static final String TAG = "InternalDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table expire (_id integer primary key autoincrement, "
			+ "times text, license_code text);";

	public static final String DATABASE_NAME = "internal.db";
	private static final String DATABASE_TABLE_EXPIRE = "expire";
	private static final String DATABASE_TABLE_DROPBOX = "dropbox";
	private static final int DATABASE_VERSION = 4;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
			db.execSQL("CREATE TABLE " + DATABASE_TABLE_DROPBOX + " ("
					+ KEY_ROWID + " INTEGER PRIMARY KEY, "
					+ BaseItem.INTERNAL_ID + " TEXT, " + KEY_DROPBOX_PUB
					+ " TEXT, " + KEY_DROPBOX_SEC + " TEXT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion);
			switch (oldVersion) {
			case 3:
				db.execSQL("CREATE TABLE " + DATABASE_TABLE_DROPBOX + " ("
						+ KEY_ROWID + " INTEGER PRIMARY KEY, "
						+ BaseItem.INTERNAL_ID + " TEXT, " + KEY_DROPBOX_PUB
						+ " TEXT, " + KEY_DROPBOX_SEC + " TEXT);");
				break;
			default:
				db.execSQL("DROP TABLE IF EXISTS notes");
				db.execSQL("DROP TABLE IF EXISTS expire");
				onCreate(db);
				break;
			}

		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public InternalAdapter(Context ctx) {
		this.mCtx = ctx;

		DatabaseHelper helper = new DatabaseHelper(ctx);
		this.mDb = helper.getWritableDatabase();
		mDb.close();
	}

	/**
	 * Open the database. If it cannot be opened, try to create a new instance
	 * of the database. If it cannot be created, throw an exception to signal
	 * the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public InternalAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public long insertKeys(String key, String secret) {
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_DROPBOX_PUB, key);
		updateValues.put(KEY_DROPBOX_SEC, secret);

		return mDb.insert(DATABASE_TABLE_DROPBOX, null, updateValues);
	}

	public long insertLicense(String license) {
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_LICENSE, license);

		return mDb.insert(DATABASE_TABLE_EXPIRE, null, updateValues);
	}

	public long updateLicense(String license) {
		ContentValues updateValues = new ContentValues();
		updateValues.put(KEY_LICENSE, license);

		return mDb.update(DATABASE_TABLE_EXPIRE, updateValues, null, null);
	}

	/**
	 * Return a Cursor over the list of all licenses in the database
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchInfo() {
		return mDb.query(DATABASE_TABLE_EXPIRE, new String[] { KEY_ROWID,
				KEY_TIMES, KEY_LICENSE }, null, null, null, null, null);
	}

	public Cursor fetchDropboxInfo() {
		return mDb.query(DATABASE_TABLE_DROPBOX, new String[] { KEY_ROWID,
				KEY_DROPBOX_PUB, KEY_DROPBOX_SEC }, null, null, null, null,
				null);
	}
}
