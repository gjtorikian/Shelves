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

package com.miadzin.shelves.util.backup;

import java.io.IOException;

import android.annotation.TargetApi;
import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.miadzin.shelves.base.BaseItemProvider;
import com.miadzin.shelves.provider.InternalAdapter;
import com.miadzin.shelves.provider.apparel.ApparelProvider;
import com.miadzin.shelves.provider.boardgames.BoardGamesProvider;
import com.miadzin.shelves.provider.books.BooksProvider;
import com.miadzin.shelves.provider.comics.ComicsProvider;
import com.miadzin.shelves.provider.gadgets.GadgetsProvider;
import com.miadzin.shelves.provider.movies.MoviesProvider;
import com.miadzin.shelves.provider.music.MusicProvider;
import com.miadzin.shelves.provider.software.SoftwareProvider;
import com.miadzin.shelves.provider.tools.ToolsProvider;
import com.miadzin.shelves.provider.toys.ToysProvider;
import com.miadzin.shelves.provider.videogames.VideoGamesProvider;
import com.miadzin.shelves.util.Preferences;

@TargetApi(8)
public class ShelvesBackupAgent extends BackupAgentHelper {

	public static final String LOG_TAG = "ShelvesBackupAgent";

	// A key to uniquely identify the set of backup data for the shared
	// preferences
	private static final String PREFS_BACKUP_KEY = "preference_backup_";
	// A key to uniquely identify the set of backup data for the internal
	// storage files
	private static final String INTERNAL_STORAGE_BACKUP_KEY = "database_backup_";

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOG_TAG, "onCreate() called");

		// GJT: get backup agent helper for shared preferences and add it to the
		// backup agent
		SharedPreferencesBackupHelper preferencesBackupHelper = new SharedPreferencesBackupHelper(
				this.getApplicationContext(), Preferences.NAME);
		addHelper(PREFS_BACKUP_KEY, preferencesBackupHelper);

		// GJT: get backup agent helper for all databases and add it to the
		// backup agent
		FileBackupHelper databaseBackupHelper = new FileBackupHelper(this,
				"../databases/" + InternalAdapter.DATABASE_NAME,
				"../databases/" + ApparelProvider.DATABASE_NAME,
				"../databases/" + BoardGamesProvider.DATABASE_NAME,
				"../databases/" + BooksProvider.DATABASE_NAME, "../databases/"
						+ ComicsProvider.DATABASE_NAME, "../databases/"
						+ GadgetsProvider.DATABASE_NAME, "../databases/"
						+ MoviesProvider.DATABASE_NAME, "../databases/"
						+ MusicProvider.DATABASE_NAME, "../databases/"
						+ SoftwareProvider.DATABASE_NAME, "../databases/"
						+ ToolsProvider.DATABASE_NAME, "../databases/"
						+ ToysProvider.DATABASE_NAME, "../databases/"
						+ VideoGamesProvider.DATABASE_NAME);
		addHelper(INTERNAL_STORAGE_BACKUP_KEY, databaseBackupHelper);

	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		Log.d(LOG_TAG, "onBackup() called");

		synchronized (BaseItemProvider.mDBLock) {
			Log.d(LOG_TAG, "onBackup() in lock");

			super.onBackup(oldState, data, newState);
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {
		Log.i(LOG_TAG, "onRestore() called");

		synchronized (BaseItemProvider.mDBLock) {
			Log.d(LOG_TAG, "onRestore() in lock");
			try {
				super.onRestore(data, appVersionCode, newState);
			} catch (Exception e) {
			}
		}
	}
}
