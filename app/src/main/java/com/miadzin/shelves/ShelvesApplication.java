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

package com.miadzin.shelves;

import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.ads.MobileAds;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.miadzin.shelves.provider.apparel.ApparelStore;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore;
import com.miadzin.shelves.provider.books.BooksStore;
import com.miadzin.shelves.provider.comics.ComicsStore;
import com.miadzin.shelves.provider.gadgets.GadgetsStore;
import com.miadzin.shelves.provider.movies.MoviesStore;
import com.miadzin.shelves.provider.music.MusicStore;
import com.miadzin.shelves.provider.software.SoftwareStore;
import com.miadzin.shelves.provider.tools.ToolsStore;
import com.miadzin.shelves.provider.toys.ToysStore;
import com.miadzin.shelves.provider.videogames.VideoGamesStore;
import com.miadzin.shelves.util.CookieStore;
import com.miadzin.shelves.util.auth.AccountChooser;
import com.miadzin.shelves.util.auth.AuthManager;
import com.miadzin.shelves.util.backup.BackupManagerWrapper;

public class ShelvesApplication extends Application {
	private final String LOG_TAG = "ShelvesApplication";

	private static ShelvesApplication instance;

	public static final Map<String, Uri> TYPES_TO_URI = new HashMap<String, Uri>();

	private AuthManager auth;
	private final HashMap<String, AuthManager> authMap = new HashMap<String, AuthManager>();
	private final AccountChooser accountChooser = new AccountChooser();

	private static boolean mBackupManagerAvailable;
	public static boolean mCalendarAPIAvailable;
	public static boolean mFirstRun = false;

	private static BackupManagerWrapper mBackupManagerWrapper = null;

	public ShelvesApplication() {
		instance = this;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		CookieStore.initialize(this);

		MobileAds.initialize(getApplicationContext(), "ca-app-pub-6838254586967039~4163145735");

		TYPES_TO_URI.put(getContext().getString(R.string.apparel_label),
				ApparelStore.Apparel.CONTENT_URI);
		TYPES_TO_URI.put(getContext().getString(R.string.apparel_label_big),
				ApparelStore.Apparel.CONTENT_URI);

		TYPES_TO_URI.put(
				getContext().getString(R.string.boardgame_label_plural_small),
				BoardGamesStore.BoardGame.CONTENT_URI);
		TYPES_TO_URI.put(
				getContext().getString(R.string.boardgame_label_plural_big),
				BoardGamesStore.BoardGame.CONTENT_URI);

		TYPES_TO_URI.put(
				getContext().getString(R.string.book_label_plural_small),
				BooksStore.Book.CONTENT_URI);
		TYPES_TO_URI.put(
				getContext().getString(R.string.book_label_plural_big),
				BooksStore.Book.CONTENT_URI);

		TYPES_TO_URI.put(
				getContext().getString(R.string.comic_label_plural_small),
				ComicsStore.Comic.CONTENT_URI);
		TYPES_TO_URI.put(getContext()
				.getString(R.string.comic_label_plural_big),
				ComicsStore.Comic.CONTENT_URI);

		TYPES_TO_URI.put(
				getContext().getString(R.string.gadget_label_plural_small),
				GadgetsStore.Gadget.CONTENT_URI);
		TYPES_TO_URI.put(
				getContext().getString(R.string.gadget_label_plural_big),
				GadgetsStore.Gadget.CONTENT_URI);

		TYPES_TO_URI.put(
				getContext().getString(R.string.movie_label_plural_small),
				MoviesStore.Movie.CONTENT_URI);
		TYPES_TO_URI.put(getContext()
				.getString(R.string.movie_label_plural_big),
				MoviesStore.Movie.CONTENT_URI);

		TYPES_TO_URI.put(
				getContext().getString(R.string.music_label_plural_small),
				MusicStore.Music.CONTENT_URI);
		TYPES_TO_URI.put(getContext().getString(R.string.music_label_big),
				MusicStore.Music.CONTENT_URI);

		TYPES_TO_URI.put(getContext().getString(R.string.software_label),
				SoftwareStore.Software.CONTENT_URI);
		TYPES_TO_URI.put(getContext().getString(R.string.software_label_big),
				SoftwareStore.Software.CONTENT_URI);

		TYPES_TO_URI.put(
				getContext().getString(R.string.tool_label_plural_small),
				ToolsStore.Tool.CONTENT_URI);
		TYPES_TO_URI.put(
				getContext().getString(R.string.tool_label_plural_big),
				ToolsStore.Tool.CONTENT_URI);

		TYPES_TO_URI.put(getContext()
				.getString(R.string.toy_label_plural_small),
				ToysStore.Toy.CONTENT_URI);
		TYPES_TO_URI.put(getContext().getString(R.string.toy_label_plural_big),
				ToysStore.Toy.CONTENT_URI);

		TYPES_TO_URI.put(
				getContext().getString(R.string.videogame_label_plural_small),
				VideoGamesStore.VideoGame.CONTENT_URI);
		TYPES_TO_URI.put(
				getContext().getString(R.string.videogame_label_plural_big),
				VideoGamesStore.VideoGame.CONTENT_URI);

		mBackupManagerAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
		mCalendarAPIAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

		if (mBackupManagerAvailable) {
			mBackupManagerWrapper = new BackupManagerWrapper(this);// TODO:
																	// should
																	// this be
																	// app
																	// context?
			Log.i(LOG_TAG, "BackupManager available.");
		} else {
			Log.i(LOG_TAG, "BackupManager not available.");
		}
	}

	public static Context getContext() {
		return instance;
	}

	public static ShelvesApplication getInstance() {
		return instance;
	}

	public AccountChooser getAccountChooser() {
		return accountChooser;
	}

	public static void dataChanged() {
		if (mBackupManagerWrapper != null) {
			mBackupManagerWrapper.dataChanged();
		}
	}
}