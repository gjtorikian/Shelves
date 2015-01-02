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
 * 
 * GJT: Modifications in this file include all the stuff necessary for storing
 * the sort preferences in KEY_STORE
 */

package com.miadzin.shelves.activity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.miadzin.shelves.R;
import com.miadzin.shelves.activity.apparel.ApparelActivity;
import com.miadzin.shelves.activity.boardgames.BoardGamesActivity;
import com.miadzin.shelves.activity.books.BooksActivity;
import com.miadzin.shelves.activity.comics.ComicsActivity;
import com.miadzin.shelves.activity.gadgets.GadgetsActivity;
import com.miadzin.shelves.activity.movies.MoviesActivity;
import com.miadzin.shelves.activity.music.MusicActivity;
import com.miadzin.shelves.activity.software.SoftwareActivity;
import com.miadzin.shelves.activity.tools.ToolsActivity;
import com.miadzin.shelves.activity.toys.ToysActivity;
import com.miadzin.shelves.activity.videogames.VideoGamesActivity;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.provider.InternalAdapter;
import com.miadzin.shelves.provider.apparel.ApparelManager;
import com.miadzin.shelves.provider.apparel.ApparelStore;
import com.miadzin.shelves.provider.apparel.ApparelStore.Apparel;
import com.miadzin.shelves.provider.boardgames.BoardGamesManager;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore.BoardGame;
import com.miadzin.shelves.provider.books.BooksManager;
import com.miadzin.shelves.provider.books.BooksStore;
import com.miadzin.shelves.provider.books.BooksStore.Book;
import com.miadzin.shelves.provider.comics.ComicsManager;
import com.miadzin.shelves.provider.comics.ComicsStore;
import com.miadzin.shelves.provider.comics.ComicsStore.Comic;
import com.miadzin.shelves.provider.gadgets.GadgetsManager;
import com.miadzin.shelves.provider.gadgets.GadgetsStore;
import com.miadzin.shelves.provider.gadgets.GadgetsStore.Gadget;
import com.miadzin.shelves.provider.movies.MoviesManager;
import com.miadzin.shelves.provider.movies.MoviesStore;
import com.miadzin.shelves.provider.movies.MoviesStore.Movie;
import com.miadzin.shelves.provider.music.MusicManager;
import com.miadzin.shelves.provider.music.MusicStore;
import com.miadzin.shelves.provider.music.MusicStore.Music;
import com.miadzin.shelves.provider.software.SoftwareManager;
import com.miadzin.shelves.provider.software.SoftwareStore;
import com.miadzin.shelves.provider.software.SoftwareStore.Software;
import com.miadzin.shelves.provider.tools.ToolsManager;
import com.miadzin.shelves.provider.tools.ToolsStore;
import com.miadzin.shelves.provider.tools.ToolsStore.Tool;
import com.miadzin.shelves.provider.toys.ToysManager;
import com.miadzin.shelves.provider.toys.ToysStore;
import com.miadzin.shelves.provider.toys.ToysStore.Toy;
import com.miadzin.shelves.provider.videogames.VideoGamesManager;
import com.miadzin.shelves.provider.videogames.VideoGamesStore;
import com.miadzin.shelves.provider.videogames.VideoGamesStore.VideoGame;
import com.miadzin.shelves.server.ServerInfo;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.ExportUtilities;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.ImportUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;
import com.miadzin.shelves.util.auth.AccountChooser;
import com.miadzin.shelves.util.auth.AuthManager;
import com.miadzin.shelves.util.auth.AuthManager.AuthCallback;
import com.miadzin.shelves.util.auth.AuthManagerFactory;
import com.miadzin.shelves.util.auth.BringFromDocs;
import com.miadzin.shelves.util.auth.ModernAuthManager;
import com.miadzin.shelves.util.auth.SendToDocs;
import com.miadzin.shelves.util.loan.Calendars;

public class SettingsActivity extends PreferenceActivity implements
		Preference.OnPreferenceChangeListener {

	static final String KEY_HELP_VERSION_SHOWN = "preferences_help_version_shown";

	private static String mSortOrder = BaseItem.DEFAULT_SORT_ORDER;
	private static String mDatabase = ServerInfo.API_REST_HOST_US;
	private static String mSortNum = "1";
	private static String mCalendar = "1";

	private final int DELETE_ALL_DIALOG = 1;

	private final int SEND_TO_GOOGLE_DIALOG = 2;
	private final int BRING_FROM_GOOGLE_DIALOG = 3;
	private final int SEND_COLLECTIONS_GOOGLE_DIALOG = 4;
	private final int BRING_COLLECTIONS_GOOGLE_DIALOG = 5;
	private final int SEND_TO_GOOGLE_RESULT_DIALOG = 6;
	private final int BRING_FROM_GOOGLE_RESULT_DIALOG = 7;
	private final int AUTHENTICATE_TO_DOCS = 8;
	private final int AUTHENTICATE_FROM_DOCS = 9;
	private final int AUTHENTICATE_TO_TRIX = 10;
	private final int AUTHENTICATE_FROM_TRIX = 11;
	private final int SEND_TO_DOCS = 12;
	private final int BRING_FROM_DOCS = 13;
	private final int EXPORT_EVERYTHING_DIALOG = 14;
	private final int PAID_FEATURE_DIALOG_ID = 15;

	private final int DOWNLOAD_COVER_DIALOG = 16;
	private final int BRING_FROM_DROPBOX = 17;

	private static final String ACTION_EXPORT_EVERYTHING = "shelves.intent.action.ACTION_EXPORT_EVERYTHING";

	public int prevOrientation;

	// Authentication
	private AuthManager lastAuth;
	private final HashMap<String, AuthManager> authMap = new HashMap<String, AuthManager>();
	private AccountChooser accountChooser;
	private String lastAccountName;
	private String lastAccountType;

	PreferenceScreen authenticateDropbox;
	PreferenceScreen sendToDropbox;
	PreferenceScreen bringFromDropbox;
	PreferenceCategory importExport;

	private static SettingsActivity instance = null;

	public boolean sendToDocsSuccess = false;
	public String sendToDocsMessage = "";
	public boolean bringFromDocsSuccess = false;
	public String bringFromDocsMessage = "";

	private final String LOG_TAG = "SettingsActivity";

	private String[] titleArray;
	private String[] importFileArray;

	private ProgressDialog sendToGoogleDialog;
	private AlertDialog sendToGoogleResultDialog;
	private String[] collectionsToSend;

	private ProgressDialog bringFromGoogleDialog;
	private AlertDialog bringFromGoogleResultDialog;
	private int selectedCollectionIndex = -1;

	public static String KEY_MASSEXPORT = "massExport";

	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;

	private DropboxAPI<AndroidAuthSession> mDBApi;

	Intent[] intentArray = new Intent[11];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		AnalyticsUtils.getInstance(this).trackPageView("/" + LOG_TAG);

		getPreferenceManager().setSharedPreferencesName(Preferences.NAME);
		addPreferencesFromResource(R.xml.preferences);

		AppKeyPair appKeys = new AppKeyPair(ServerInfo.DROPBOX_APP_KEY,
				ServerInfo.DROPBOX_APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys,
				ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);

		ListPreference databasePreference = (ListPreference) findPreference(Preferences.KEY_DATABASE);
		databasePreference.setOnPreferenceChangeListener(this);
		databasePreference.setValue(getDatabaseRegion(this));

		setDatabasePreferenceEntries(databasePreference);
		setDatabasePreferenceSummary(databasePreference,
				getDatabaseRegion(this));

		ListPreference sortNumPreference = (ListPreference) findPreference(Preferences.KEY_SORT_NUM);
		sortNumPreference.setOnPreferenceChangeListener(this);
		sortNumPreference.setValue(getSortNum(this));

		setSortNumEntries(sortNumPreference);
		setSortNumSummary(sortNumPreference, getSortNum(this));

		ListPreference defaultCalendarPreference = (ListPreference) findPreference(Preferences.KEY_CALENDAR);
		if (!Calendars.isCalendarPresent(getBaseContext(),
				getString(R.string.preferences_calendar_default_id))) {
			defaultCalendarPreference.setEnabled(false);
		} else {
			defaultCalendarPreference.setOnPreferenceChangeListener(this);

			if (savedInstanceState != null
					&& savedInstanceState.getBoolean("noCalendarStart") == true) {
				Calendars.initCalendarsPreference(this,
						defaultCalendarPreference);

				ensureValidDefaultCalendarPreference(defaultCalendarPreference,
						getBaseContext());
			}
		}
		String currCal = getCalendar(this);
		defaultCalendarPreference.setSummary(getString(
				R.string.preferences_calendar_summary,
				Calendars.getCalendarString(this, currCal)));

		titleArray = new String[] { getString(R.string.apparel_label_big),
				getString(R.string.boardgame_label_plural_big),
				getString(R.string.book_label_plural_big),
				getString(R.string.comic_label_plural_big),
				getString(R.string.gadget_label_plural_big),
				getString(R.string.movie_label_plural_big),
				getString(R.string.music_label_big),
				getString(R.string.software_label_big),
				getString(R.string.tool_label_plural_big),
				getString(R.string.toy_label_plural_big),
				getString(R.string.videogame_label_plural_big) };

		importFileArray = new String[] { getString(R.string.IMPORT_FILE_SHELVES_APPAREL),
				getString(R.string.IMPORT_FILE_SHELVES_BOARDGAMES),
				getString(R.string.IMPORT_FILE_SHELVES_BOOKS),
				getString(R.string.IMPORT_FILE_SHELVES_COMICS),
				getString(R.string.IMPORT_FILE_SHELVES_GADGETS),
				getString(R.string.IMPORT_FILE_SHELVES_MOVIES),
				getString(R.string.IMPORT_FILE_SHELVES_MUSIC),
				getString(R.string.IMPORT_FILE_SHELVES_SOFTWARE),
				getString(R.string.IMPORT_FILE_SHELVES_TOOLS),
				getString(R.string.IMPORT_FILE_SHELVES_TOYS),
				getString(R.string.IMPORT_FILE_SHELVES_VIDEOGAMES) };

		intentArray[0] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_APPAREL");
		intentArray[1] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_BOARDGAMES");
		intentArray[2] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_BOOKS");
		intentArray[3] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_COMICS");
		intentArray[4] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_GADGETS");
		intentArray[5] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_MOVIES");
		intentArray[6] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_MUSIC");
		intentArray[7] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_SOFTWARE");
		intentArray[8] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_TOOLS");
		intentArray[9] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_TOYS");
		intentArray[10] = new Intent(
				"shelves.intent.action.ACTION_IMPORT_SHELVES_VIDEOGAMES");

		importExport = (PreferenceCategory) findPreference(Preferences.KEY_IMPORT_EXPORT);

		PreferenceScreen sendToGoogle = (PreferenceScreen) findPreference(Preferences.SEND_TO_GOOGLE_DOCS);
		sendToGoogle
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {
						showDialog(SEND_COLLECTIONS_GOOGLE_DIALOG);
						return true;
					}
				});

		PreferenceScreen bringFromGoogle = (PreferenceScreen) findPreference(Preferences.BRING_FROM_GOOGLE_DOCS);
		bringFromGoogle
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {
						showDialog(BRING_COLLECTIONS_GOOGLE_DIALOG);
						return true;
					}
				});

		authenticateDropbox = (PreferenceScreen) findPreference(Preferences.AUTHENTICATE_DROPBOX);
		authenticateDropbox
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {
						if (!UIUtilities.isPaid(getContentResolver(),
								getBaseContext())) {
							showDialog(PAID_FEATURE_DIALOG_ID);
							return true;
						}

						mDBApi.getSession().startAuthentication(
								SettingsActivity.this);
						return true;
					}
				});

		sendToDropbox = (PreferenceScreen) findPreference(Preferences.SEND_TO_DROPBOX);
		sendToDropbox
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {
						if (!UIUtilities.isPaid(getContentResolver(),
								getBaseContext())) {
							showDialog(PAID_FEATURE_DIALOG_ID);
							return true;
						}

						new ExportEverythingTask(true).execute();

						return true;
					}
				});

		bringFromDropbox = (PreferenceScreen) findPreference(Preferences.BRING_FROM_DROPBOX);
		bringFromDropbox
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					public boolean onPreferenceClick(Preference preference) {
						if (!UIUtilities.isPaid(getContentResolver(),
								getBaseContext())) {
							showDialog(PAID_FEATURE_DIALOG_ID);
							return true;
						}

						showDialog(BRING_FROM_DROPBOX);

						return true;
					}
				});

		establishDropboxUI();

		/*
		 * PreferenceScreen downloadCoverApparelPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_APPAREL);
		 * downloadCoverApparelPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverBoardGamesPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_BOARDGAMES);
		 * downloadCoverBoardGamesPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverBooksPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_BOOKS);
		 * downloadCoverBooksPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverComicsPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_COMICS);
		 * downloadCoverComicsPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverGadgetsPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_GADGETS);
		 * downloadCoverGadgetsPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverMoviesPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_MOVIES);
		 * downloadCoverMoviesPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverMusicPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_MUSIC);
		 * downloadCoverMusicPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverSoftwarePref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_SOFTWARE);
		 * downloadCoverSoftwarePref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverToolsPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_TOOLS);
		 * downloadCoverToolsPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverToysPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_TOYS);
		 * downloadCoverToysPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 * 
		 * PreferenceScreen downloadCoverVideoGamesPref = (PreferenceScreen)
		 * findPreference(Preferences.DOWNLOAD_COVER_VIDEOGAMES);
		 * downloadCoverVideoGamesPref .setOnPreferenceClickListener(new
		 * DownloadCoverClickListener());
		 */

		// GJT: Needed for "Delete Everything" pref
		PreferenceScreen deleteApparelPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_APPAREL);
		deleteApparelPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteBoardGamesPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_BOARDGAMES);
		deleteBoardGamesPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteBooksPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_BOOKS);
		deleteBooksPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteComicsPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_COMICS);
		deleteComicsPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteGadgetsPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_GADGETS);
		deleteGadgetsPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteMoviesPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_MOVIES);
		deleteMoviesPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteMusicPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_MUSIC);
		deleteMusicPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteSoftwarePref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_SOFTWARE);
		deleteSoftwarePref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteToolsPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_TOOLS);
		deleteToolsPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteToysPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_TOYS);
		deleteToysPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		PreferenceScreen deleteVideoGamesPref = (PreferenceScreen) findPreference(Preferences.DELETE_ALL_VIDEOGAMES);
		deleteVideoGamesPref
				.setOnPreferenceClickListener(new DeletePreferenceClickListener());

		final Intent intent = findPreference(Preferences.KEY_IMPORT)
				.getIntent();
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		// GJT: Sets the version number in the Settings screen
		try {
			findPreference("version")
					.setSummary(
							getPackageManager().getPackageInfo(
									"com.miadzin.shelves", 0).versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void establishDropboxUI() {
		InternalAdapter mDbHelper = new InternalAdapter(getBaseContext());
		mDbHelper.open();
		Cursor cInternal = mDbHelper.fetchDropboxInfo();

		try {
			if (cInternal.moveToFirst()) {
				if (TextUtilities.isEmpty(cInternal.getString(2))) {
					if (cInternal != null)
						cInternal.close();
					mDbHelper.close();

					importExport.removePreference(sendToDropbox);
					importExport.removePreference(bringFromDropbox);
				} else {
					AccessTokenPair access = new AccessTokenPair(
							cInternal.getString(1), cInternal.getString(2));
					mDBApi.getSession().setAccessTokenPair(access);

					mDbHelper.close();

					importExport.removePreference(authenticateDropbox);
				}
			} else {
				if (cInternal != null)
					cInternal.close();
				mDbHelper.close();

				importExport.removePreference(sendToDropbox);
				importExport.removePreference(bringFromDropbox);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (cInternal != null)
				cInternal.close();
		}
	}

	private void setDatabasePreferenceEntries(ListPreference preference) {
		final CharSequence[] values = new CharSequence[8];
		final CharSequence[] labels = new CharSequence[8];

		values[0] = ServerInfo.API_REST_HOST_US;
		labels[0] = getString(R.string.preferences_database_us);
		values[1] = ServerInfo.API_REST_HOST_CA;
		labels[1] = getString(R.string.preferences_database_ca);
		values[2] = ServerInfo.API_REST_HOST_UK;
		labels[2] = getString(R.string.preferences_database_uk);
		values[3] = ServerInfo.API_REST_HOST_FR;
		labels[3] = getString(R.string.preferences_database_fr);
		values[4] = ServerInfo.API_REST_HOST_DE;
		labels[4] = getString(R.string.preferences_database_de);
		values[5] = ServerInfo.API_REST_HOST_JP;
		labels[5] = getString(R.string.preferences_database_jp);
		values[6] = ServerInfo.API_REST_HOST_IT;
		labels[6] = getString(R.string.preferences_database_it);
		values[7] = ServerInfo.API_REST_HOST_CN;
		labels[7] = getString(R.string.preferences_database_cn);

		preference.setEntries(labels);
		preference.setEntryValues(values);
	}

	private void setSortNumEntries(ListPreference preference) {
		final CharSequence[] values = new CharSequence[3];
		final CharSequence[] labels = new CharSequence[3];

		values[0] = labels[0] = "1";
		values[1] = labels[1] = "2";
		values[2] = labels[2] = "3";

		preference.setEntries(labels);
		preference.setEntryValues(values);
	}

	private void setDatabasePreferenceSummary(Preference preference,
			String database) {
		String orderFromXML = ServerInfo.API_REST_HOST_US;

		if (database.equals(ServerInfo.API_REST_HOST_US)) {
			orderFromXML = getString(R.string.preferences_database_us_value);
		} else if (database.equals(ServerInfo.API_REST_HOST_CA)) {
			orderFromXML = getString(R.string.preferences_database_ca_value);
		} else if (database.equals(ServerInfo.API_REST_HOST_UK)) {
			orderFromXML = getString(R.string.preferences_database_uk_value);
		} else if (database.equals(ServerInfo.API_REST_HOST_FR)) {
			orderFromXML = getString(R.string.preferences_database_fr_value);
		} else if (database.equals(ServerInfo.API_REST_HOST_DE)) {
			orderFromXML = getString(R.string.preferences_database_de_value);
		} else if (database.equals(ServerInfo.API_REST_HOST_JP)) {
			orderFromXML = getString(R.string.preferences_database_jp_value);
		} else if (database.equals(ServerInfo.API_REST_HOST_IT)) {
			orderFromXML = getString(R.string.preferences_database_it_value);
		} else if (database.equals(ServerInfo.API_REST_HOST_CN)) {
			orderFromXML = getString(R.string.preferences_database_cn_value);
		}

		preference.setSummary(getString(R.string.preferences_database_summary,
				orderFromXML));
	}

	private void setSortNumSummary(Preference preference, String sortNum) {
		preference.setSummary(getString(R.string.sort_num_summary, sortNum));
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		final String key = preference.getKey();

		if (Preferences.KEY_DATABASE.equals(key)) {
			final String databaseName = newValue.toString();
			setDatabasePreferenceSummary(preference, databaseName);
			((ListPreference) preference).setValue(databaseName);
		} else if (Preferences.KEY_SORT_NUM.equals(key)) {
			final String sortNum = newValue.toString();
			setSortNumSummary(preference, sortNum);
			((ListPreference) preference).setValue(sortNum);
		} else if (Preferences.KEY_CALENDAR.equals(key)) {
			Context context = getBaseContext();
			final String calendarName = newValue.toString();
			preference.setSummary(getString(
					R.string.preferences_calendar_summary,
					Calendars.getCalendarString(context, calendarName)));
			((ListPreference) preference).setValue(calendarName);
		}

		return false;
	}

	public static String getSortOrder(Context context) {
		// double check show() up above...
		String type = context.toString().toLowerCase();
		final SharedPreferences pref = context.getSharedPreferences(
				Preferences.NAME, 0);
		String sortOrder = null;

		if (type.contains("apparel")) {
			sortOrder = pref.getString(Preferences.KEY_APPAREL_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("boardgames")) {
			sortOrder = pref.getString(Preferences.KEY_BOARDGAME_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("books")) {
			sortOrder = pref.getString(Preferences.KEY_BOOK_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("comics")) {
			sortOrder = pref.getString(Preferences.KEY_COMIC_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("gadgets")) {
			sortOrder = pref.getString(Preferences.KEY_GADGET_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("movies")) {
			sortOrder = pref.getString(Preferences.KEY_MOVIE_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("music")) {
			sortOrder = pref.getString(Preferences.KEY_MUSIC_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("software")) {
			sortOrder = pref.getString(Preferences.KEY_SOFTWARE_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("tools")) {
			sortOrder = pref.getString(Preferences.KEY_TOOL_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("toys")) {
			sortOrder = pref.getString(Preferences.KEY_TOY_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		} else if (type.contains("videogames")) {
			sortOrder = pref.getString(Preferences.KEY_VIDEOGAME_SORT,
					BaseItem.DEFAULT_SORT_ORDER);
		}

		SharedPreferences.Editor editor = pref.edit();
		editor.putString(Preferences.KEY_SORT, sortOrder);

		editor.commit();

		mSortOrder = sortOrder;

		return sortOrder;
	}

	public static String getSortOrder() {
		return mSortOrder;
	}

	public static String getDatabaseRegion(Context context) {
		final SharedPreferences pref = context.getSharedPreferences(
				Preferences.NAME, 0);
		final String databaseRegion = pref.getString(Preferences.KEY_DATABASE,
				ServerInfo.API_REST_HOST_US);

		SharedPreferences.Editor editor = pref.edit();
		editor.putString(Preferences.KEY_DATABASE, databaseRegion);

		editor.commit();

		mDatabase = databaseRegion;

		return databaseRegion;
	}

	public static String getSortNum(Context context) {
		final SharedPreferences pref = context.getSharedPreferences(
				Preferences.NAME, 0);
		final String sortNum = pref.getString(Preferences.KEY_SORT_NUM, "1");

		SharedPreferences.Editor editor = pref.edit();
		editor.putString(Preferences.KEY_SORT_NUM, sortNum);

		editor.commit();

		mSortNum = sortNum;

		return sortNum;
	}

	public static String getSortNum() {
		return mSortNum;
	}

	public static String getDatabaseRegion() {
		return mDatabase;
	}

	public static String getCalendar(Context context) {
		final SharedPreferences pref = context.getSharedPreferences(
				Preferences.NAME, 0);
		final String calendar = pref.getString(Preferences.KEY_CALENDAR, "1");

		SharedPreferences.Editor editor = pref.edit();
		editor.putString(Preferences.KEY_CALENDAR, calendar);

		editor.commit();

		mCalendar = calendar;

		return calendar;
	}

	public static String getCalendar() {
		return mCalendar;
	}

	/**
	 * Ensures that the default calendar preference is pointing to
	 * user-modifiable calendar that exists. If the calendar does not exist
	 * anymore, the preference is reset to default value.
	 * 
	 * @param context
	 *            Context
	 */
	public void ensureValidDefaultCalendarPreference(Preference pref,
			Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Resources r = context.getResources();
		Editor editor = prefs.edit();
		// Don't use a default calendar if one doesn't exist
		if (!prefs.contains(r.getString(R.string.preferences_calendar_default))
				|| !Calendars.isCalendarPresent(context, prefs.getString(
						r.getString(R.string.preferences_calendar_default),
						null))) {
			editor.putString(
					r.getString(R.string.preferences_calendar_default),
					r.getString(R.string.preferences_calendar_default_id));
			editor.commit();

			pref.setSummary(getString(R.string.preferences_calendar_summary,
					r.getString(R.string.preferences_calendar_default_shelves)));
		}
	}

	public static void show(Context context) {
		final String contextString = context.toString().toLowerCase();

		if (contextString.contains("apparel")) {
			((ApparelActivity) context).setFromPrefs();
		} else if (contextString.contains("boardgames")) {
			((BoardGamesActivity) context).setFromPrefs();
		} else if (contextString.contains("books")) {
			((BooksActivity) context).setFromPrefs();
		} else if (contextString.contains("comics")) {
			((ComicsActivity) context).setFromPrefs();
		} else if (contextString.contains("gadgets")) {
			((GadgetsActivity) context).setFromPrefs();
		} else if (contextString.contains("movies")) {
			((MoviesActivity) context).setFromPrefs();
		} else if (contextString.contains("music")) {
			((MusicActivity) context).setFromPrefs();
		} else if (contextString.contains("software")) {
			((SoftwareActivity) context).setFromPrefs();
		} else if (contextString.contains("tools")) {
			((ToolsActivity) context).setFromPrefs();
		} else if (contextString.contains("toys")) {
			((ToysActivity) context).setFromPrefs();
		} else if (contextString.contains("videogames")) {
			((VideoGamesActivity) context).setFromPrefs();
		}

		final Intent intent = new Intent(context, SettingsActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mDBApi.getSession().authenticationSuccessful()) {
			try {
				// MANDATORY call to complete auth.
				// Sets the access token on the session
				mDBApi.getSession().finishAuthentication();

				AccessTokenPair tokens = mDBApi.getSession()
						.getAccessTokenPair();

				// Store keys to persist the access token pair

				InternalAdapter mDbHelper = new InternalAdapter(
						getBaseContext());
				mDbHelper.open();

				mDbHelper.insertKeys(tokens.key, tokens.secret);

				mDbHelper.close();

				establishDropboxUI();

				importExport.addPreference(sendToDropbox);
				importExport.addPreference(bringFromDropbox);

			} catch (IllegalStateException e) {
				Log.w(LOG_TAG, "Error authenticating to Dropbox: ", e);
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		final String action = intent.getAction();
		if (ACTION_EXPORT_EVERYTHING.equals(action)) {
			new ExportEverythingTask(false).execute();
		}
	}

	private void sendToDropbox(String filename) {
		String outportFile = IOUtilities.getExternalFile(filename).toString();

		try {
			// Uploading content.
			String fileContents = TextUtilities.readFileAsString(outportFile);
			ByteArrayInputStream inputStream = new ByteArrayInputStream(
					fileContents.getBytes());

			Entry newEntry = mDBApi.putFileOverwrite("/" + filename,
					inputStream, fileContents.length(), null);
			Log.i("DbExampleLog", filename + " rev is: " + newEntry.rev);
		} catch (DropboxUnlinkedException e) {
			// User has unlinked, ask them to link again here.
			Log.e("DbExampleLog", "User has unlinked.");
		} catch (DropboxException e) {
			Log.e("DbExampleLog", "Something went wrong while uploading "
					+ filename);
		} catch (IOException e) {
			Log.e("LOG_TAG", "Couldn't read file: ");
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,
			final Intent results) {
		switch (requestCode) {
		case ModernAuthManager.GET_LOGIN: {
			if (resultCode == RESULT_CANCELED || lastAuth == null) {
				break;
			}

			// This will invoke onAuthResult appropriately.
			lastAuth.authResult(resultCode, results);
			break;
		}
		case AUTHENTICATE_TO_DOCS: {
			if (resultCode == RESULT_OK) {
				setProgressValue(0);
				setProgressMessage(getString(R.string.progress_message_authenticating_docs));
				authenticate(results, AUTHENTICATE_TO_TRIX, "writely", "send");
			} else {
				dismissDialogSafely(SEND_TO_GOOGLE_DIALOG);
			}
			break;
		}
		case AUTHENTICATE_TO_TRIX: {
			if (resultCode == RESULT_OK) {
				setProgressValue(30);
				setProgressMessage(getString(R.string.progress_message_authenticating_docs));
				authenticate(results, SEND_TO_DOCS, "wise", "send");
			} else {
				dismissDialogSafely(SEND_TO_GOOGLE_DIALOG);
			}
			break;
		}
		case AUTHENTICATE_FROM_DOCS: {
			if (resultCode == RESULT_OK) {
				setProgressValue(0);
				setProgressMessage(getString(R.string.progress_message_authenticating_docs));
				authenticate(results, AUTHENTICATE_FROM_TRIX, "writely", "from");
			} else {
				dismissDialogSafely(BRING_FROM_GOOGLE_DIALOG);
			}
			break;
		}
		case AUTHENTICATE_FROM_TRIX: {
			if (resultCode == RESULT_OK) {
				setProgressValue(30);
				setProgressMessage(getString(R.string.progress_message_authenticating_docs));
				authenticate(results, BRING_FROM_DOCS, "wise", "from");
			} else {
				dismissDialogSafely(BRING_FROM_GOOGLE_DIALOG);
			}
			break;
		}
		case SEND_TO_DOCS: {
			if (results != null && resultCode == RESULT_OK) {
				Log.d(LOG_TAG, "Sending to Docs....");
				setProgressValue(50);
				setProgressMessage(getString(R.string.progress_message_sending_docs));
				final SendToDocs sender = new SendToDocs(this,
						authMap.get(SendToDocs.GDATA_SERVICE_NAME_TRIX),
						authMap.get(SendToDocs.GDATA_SERVICE_NAME_DOCLIST),
						collectionsToSend);
				Runnable onCompletion = new Runnable() {
					public void run() {
						setProgressValue(100);

						dismissDialogSafely(SEND_TO_GOOGLE_DIALOG);

						// TODO: Use this message
						sendToDocsMessage = sender.getStatusMessage();
						sendToDocsSuccess = sender.wasSuccess();

						showDialogSafely(SEND_TO_GOOGLE_RESULT_DIALOG);
					}
				};
				sender.setOnCompletion(onCompletion);
				sender.sendToDocs();
			} else {
				dismissDialogSafely(SEND_TO_GOOGLE_DIALOG);
			}
			break;
		}
		case BRING_FROM_DOCS: {
			if (results != null
					&& resultCode == RESULT_OK
					&& (0 <= selectedCollectionIndex && selectedCollectionIndex <= 11)) {
				Log.d(LOG_TAG, "Bringing from Docs....");
				setProgressValue(50);
				setProgressMessage(getString(R.string.progress_message_bringing_docs));
				final BringFromDocs sender = new BringFromDocs(this,
						authMap.get(SendToDocs.GDATA_SERVICE_NAME_TRIX),
						authMap.get(SendToDocs.GDATA_SERVICE_NAME_DOCLIST),
						selectedCollectionIndex);
				Runnable onCompletion = new Runnable() {
					public void run() {
						setProgressValue(100);
						dismissDialogSafely(BRING_FROM_GOOGLE_DIALOG);
						bringFromDocsMessage = sender.getStatusMessage();
						bringFromDocsSuccess = sender.wasSuccess();
						showDialog(BRING_FROM_GOOGLE_RESULT_DIALOG);
					}
				};
				sender.setOnCompletion(onCompletion);
				sender.run();
			} else {
				dismissDialogSafely(BRING_FROM_GOOGLE_DIALOG);
			}
			break;
		}
		}
	}

	public void setProgressMessage(final String text) {
		runOnUiThread(new Runnable() {
			public void run() {
				synchronized (this) {
					if (sendToGoogleDialog != null) {
						sendToGoogleDialog.setMessage(text);
					}
					if (bringFromGoogleDialog != null) {
						bringFromGoogleDialog.setMessage(text);
					}
				}
			}
		});
	}

	public void getAndSetProgressValue(final int percent) {
		runOnUiThread(new Runnable() {
			public void run() {
				synchronized (this) {
					if (sendToGoogleDialog != null) {
						sendToGoogleDialog.setProgress(sendToGoogleDialog
								.getProgress() + percent);
					}
					if (bringFromGoogleDialog != null) {
						bringFromGoogleDialog.setProgress(bringFromGoogleDialog
								.getProgress() + percent);
					}
				}
			}
		});
	}

	public void setProgressValue(final int percent) {
		runOnUiThread(new Runnable() {
			public void run() {
				synchronized (this) {
					if (sendToGoogleDialog != null
							&& sendToGoogleDialog.getProgress() <= 100) {
						sendToGoogleDialog.setProgress(percent);
					}
					if (bringFromGoogleDialog != null
							&& bringFromGoogleDialog.getProgress() <= 100) {
						bringFromGoogleDialog.setProgress(percent);
					}
				}
			}
		});
	}

	public int getProgressValue() {
		if (sendToGoogleDialog != null) {
			return sendToGoogleDialog.getProgress();
		} else if (bringFromGoogleDialog != null) {
			return bringFromGoogleDialog.getProgress();
		} else
			return 0;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final ProgressDialog progressDialog = new ProgressDialog(
				SettingsActivity.this);
		switch (id) {
		case PAID_FEATURE_DIALOG_ID:
			return new AlertDialog.Builder(this)
					.setMessage(R.string.support_the_dev)
					.setPositiveButton(R.string.okay_label,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).create();
		case DELETE_ALL_DIALOG:
			progressDialog.setTitle(getString(R.string.progress_dialog_wait));
			progressDialog.setIcon(android.R.drawable.ic_dialog_alert);
			progressDialog
					.setMessage(getString(R.string.preferences_delete_everything_progress_text));
			break;
		case EXPORT_EVERYTHING_DIALOG:
			progressDialog.setTitle(getString(R.string.progress_dialog_wait));
			progressDialog.setIcon(android.R.drawable.ic_dialog_alert);
			progressDialog.setMessage(getString(R.string.export_label));
			break;
		case SEND_COLLECTIONS_GOOGLE_DIALOG:
			final List<String> selectedList = new ArrayList<String>();
			return new AlertDialog.Builder(this)
					.setTitle(R.string.preference_send_to_google)
					.setMultiChoiceItems(titleArray, null,
							new DialogInterface.OnMultiChoiceClickListener() {
								public void onClick(
										DialogInterface dialogInterface,
										int pos, boolean state) {
									if (state)
										selectedList.add(titleArray[pos]);
									else
										selectedList.remove(titleArray[pos]);
								}
							})
					.setPositiveButton(getString(R.string.okay_label),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									collectionsToSend = null;

									final String selectedString = selectedList
											.toString();
									final String selectedItems = selectedString
											.substring(1,
													selectedString.length() - 1);
									if (!TextUtilities.isEmpty(selectedItems)) {
										collectionsToSend = selectedItems
												.replaceAll(", ", ",").split(
														",");

										setProgressValue(0);
										setProgressMessage("");
										showDialog(SEND_TO_GOOGLE_DIALOG);
										onActivityResult(AUTHENTICATE_TO_DOCS,
												RESULT_OK, new Intent());
									}
									dialog.dismiss();
								}
							})
					.setNegativeButton(getString(R.string.cancel_label),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).create();
		case BRING_COLLECTIONS_GOOGLE_DIALOG:
			return new AlertDialog.Builder(this)
					.setTitle(R.string.preference_bring_from_google)
					.setSingleChoiceItems(titleArray, selectedCollectionIndex,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									selectedCollectionIndex = which;
								}
							})
					.setPositiveButton(getString(R.string.okay_label),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									setProgressValue(0);
									setProgressMessage("");
									showDialog(BRING_FROM_GOOGLE_DIALOG);
									onActivityResult(AUTHENTICATE_FROM_DOCS,
											RESULT_OK, new Intent());
									dialog.dismiss();
								}
							})
					.setNegativeButton(getString(R.string.cancel_label),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).create();
		case SEND_TO_GOOGLE_DIALOG:
			sendToGoogleDialog = new ProgressDialog(this);
			sendToGoogleDialog.setIcon(android.R.drawable.ic_dialog_info);
			sendToGoogleDialog.setTitle(R.string.progress_dialog_wait);
			sendToGoogleDialog
					.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			sendToGoogleDialog.setMessage("");
			sendToGoogleDialog.setMax(100);
			sendToGoogleDialog.setProgress(10);
			return sendToGoogleDialog;
		case BRING_FROM_GOOGLE_DIALOG:
			bringFromGoogleDialog = new ProgressDialog(this);
			bringFromGoogleDialog.setIcon(android.R.drawable.ic_dialog_info);
			bringFromGoogleDialog.setTitle(R.string.progress_dialog_wait);
			bringFromGoogleDialog
					.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			bringFromGoogleDialog.setMessage("");
			bringFromGoogleDialog.setMax(100);
			bringFromGoogleDialog.setProgress(10);
			return bringFromGoogleDialog;
		case SEND_TO_GOOGLE_RESULT_DIALOG:
			AlertDialog.Builder sendBuilder = new AlertDialog.Builder(this);
			sendBuilder.setIcon(android.R.drawable.ic_dialog_info);
			sendBuilder.setMessage(R.string.error_sending_to_docs);
			sendBuilder.setNeutralButton(getString(R.string.okay_label),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			sendToGoogleResultDialog = sendBuilder.create();
			return sendToGoogleResultDialog;
		case BRING_FROM_GOOGLE_RESULT_DIALOG:
			AlertDialog.Builder fromBuilder = new AlertDialog.Builder(this);
			fromBuilder.setIcon(android.R.drawable.ic_dialog_info);
			if (!bringFromDocsSuccess)
				fromBuilder.setMessage(R.string.error_bringing_from_docs);
			else
				fromBuilder
						.setMessage(R.string.status_have_been_downloaded_to_docs);
			fromBuilder.setNeutralButton(getString(R.string.okay_label),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (bringFromDocsSuccess) {
								startActivity(intentArray[selectedCollectionIndex]);
							}
							dialog.dismiss();
						}
					});
			bringFromGoogleResultDialog = fromBuilder.create();
			return bringFromGoogleResultDialog;
		case BRING_FROM_DROPBOX:
			return new AlertDialog.Builder(this)
					.setTitle(R.string.preference_bring_from_google)
					.setSingleChoiceItems(titleArray, selectedCollectionIndex,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									selectedCollectionIndex = which;
								}
							})
					.setPositiveButton(getString(R.string.okay_label),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// Get file.
									ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
									try {
										DropboxFileInfo info = mDBApi.getFile(
												"/testing.txt", null,
												outputStream, null);
										TextUtilities.writeStringToFile(
												IOUtilities
														.getExternalFile(importFileArray[selectedCollectionIndex]),
												new String(outputStream
														.toByteArray()));
									} catch (DropboxException e) {
										Log.e("DbExampleLog",
												"Something went wrong while downloading.");
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									startActivity(intentArray[selectedCollectionIndex]);
									dialog.dismiss();
								}
							})
					.setNegativeButton(getString(R.string.cancel_label),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).create();
		}

		return progressDialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		switch (id) {
		case SEND_TO_GOOGLE_DIALOG:
		case BRING_FROM_GOOGLE_DIALOG:
			resetGoogleStatus();

			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

			prevOrientation = getRequestedOrientation();

			switch (getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_LANDSCAPE:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
			default:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
				break;
			}

			break;
		case SEND_TO_GOOGLE_RESULT_DIALOG:
			sendToGoogleResultDialog
					.setIcon(sendToDocsSuccess ? android.R.drawable.ic_dialog_info
							: android.R.drawable.ic_dialog_alert);
			sendToGoogleResultDialog
					.setMessage(sendToDocsSuccess ? getString(R.string.status_have_been_uploaded_to_docs)
							: getString(R.string.error_sending_to_docs));
			break;
		case BRING_FROM_GOOGLE_RESULT_DIALOG:
			bringFromGoogleResultDialog
					.setIcon(bringFromDocsSuccess ? android.R.drawable.ic_dialog_info
							: android.R.drawable.ic_dialog_alert);
			bringFromGoogleResultDialog
					.setMessage(bringFromDocsSuccess ? getString(R.string.status_have_been_downloaded_to_docs)
							: getString(R.string.error_bringing_from_docs));
			break;
		}
	}

	/**
	 * Dismisses the progress dialog if it is showing. Executed on the UI
	 * thread.
	 */
	public void dismissDialogSafely(final int id) {

		runOnUiThread(new Runnable() {
			public void run() {
				try {
					dismissDialog(id);
					if (id == SEND_TO_GOOGLE_DIALOG
							|| id == BRING_FROM_GOOGLE_DIALOG) {
						getWindow().clearFlags(
								WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

						setRequestedOrientation(prevOrientation);
					}
				} catch (IllegalArgumentException e) {
					// This will be thrown if this dialog was not shown before.
				}
			}
		});
	}

	/**
	 * Just like showDialog, but will catch a BadTokenException that sometimes
	 * (very rarely) gets thrown. This might happen if the user hits the "back"
	 * button immediately after sending tracks to google.
	 * 
	 * @param id
	 *            the dialog id
	 */
	public void showDialogSafely(final int id) {
		runOnUiThread(new Runnable() {
			public void run() {
				try {
					showDialog(id);
				} catch (BadTokenException e) {
					Log.w(LOG_TAG, "Could not display dialog with id " + id, e);
				} catch (IllegalStateException e) {
					Log.w(LOG_TAG, "Could not display dialog with id " + id, e);
				}
			}
		});
	}

	/**
	 * Initializes the authentication manager which obtains an authentication
	 * token, prompting the user for a login and password if needed.
	 */
	private void authenticate(final Intent results, final int requestCode,
			final String service, final String type) {
		lastAuth = authMap.get(service);
		if (lastAuth == null) {
			Log.i(LOG_TAG, "Creating a new authentication for service: "
					+ service);
			lastAuth = AuthManagerFactory.getAuthManager(this,
					ModernAuthManager.GET_LOGIN, null, true, service);
			authMap.put(service, lastAuth);
		}

		Log.d(LOG_TAG, "Logging in to " + service + "...");
		if (AuthManagerFactory.useModernAuthManager()) {
			runOnUiThread(new Runnable() {
				// @Override
				public void run() {
					chooseAccount(results, requestCode, service);
				}
			});
		} else {
			doLogin(results, requestCode, service, null);
		}
	}

	private void chooseAccount(final Intent results, final int requestCode,
			final String service) {
		if (accountChooser == null) {
			accountChooser = new AccountChooser();

			// Restore state if necessary.
			if (lastAccountName != null && lastAccountType != null) {
				accountChooser.setChosenAccount(lastAccountName,
						lastAccountType);
			}
		}

		accountChooser.chooseAccount(SettingsActivity.this,
				new AccountChooser.AccountHandler() {
					// @Override
					public void onAccountSelected(Account account) {
						if (account == null) {
							dismissDialog(SEND_TO_GOOGLE_DIALOG);
							dismissDialog(BRING_FROM_GOOGLE_DIALOG);
							finish();
							return;
						}

						lastAccountName = account.name;
						lastAccountType = account.type;
						doLogin(results, requestCode, service, account);
					}
				});
	}

	private void doLogin(final Intent results, final int requestCode,
			final String service, final Object account) {
		lastAuth.doLogin(new AuthCallback() {
			// @Override
			public void onAuthResult(boolean success) {
				Log.i(LOG_TAG, "Login success for " + service + ": " + success);
				onActivityResult(requestCode, RESULT_OK, results);
				if (!success) {
					onActivityResult(requestCode, RESULT_CANCELED, results);
					return;
				}
			}
		}, account);
	}

	/**
	 * Resets status information for sending to Docs.
	 */
	private void resetGoogleStatus() {
		sendToDocsMessage = "";
		sendToDocsSuccess = false;

		bringFromDocsMessage = "";
		bringFromDocsSuccess = false;
	}

	class DownloadCoverClickListener implements OnPreferenceClickListener {

		public boolean onPreferenceClick(final Preference pref) {
			new DownloadCoverCollectionTask(pref.getKey()).execute();
			return true;
		}
	}

	class DownloadCoverCollectionTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog downloadingDialog = new ProgressDialog(
				SettingsActivity.this);
		private String mType = null;

		DownloadCoverCollectionTask(String type) {
			mType = type;
		}

		@Override
		protected void onPreExecute() {
			showDialog(DOWNLOAD_COVER_DIALOG);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			Cursor c = null;
			ContentResolver contentResolver = getContentResolver();
			try {
				if (mType.equals("shelves.downloadCoverApparel")) {
					c = contentResolver.query(ApparelStore.Apparel.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Apparel apparel = ApparelStore.Apparel
									.fromCursor(c);
							final String mID = apparel.getInternalId();
							final Bitmap bitmap = Preferences
									.getBitmapForManager(apparel);

							ImageUtilities.deleteCachedCover(mID);
							ImportUtilities.addCoverToCache(mID, ImageUtilities
									.createCover(bitmap,
											Preferences.getWidthForManager(),
											Preferences.getHeightForManager()));
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverBoardGames")) {
					c = contentResolver.query(
							BoardGamesStore.BoardGame.CONTENT_URI, null, null,
							null, null);
					if (c.moveToFirst()) {
						do {
							BoardGame boardgame = BoardGamesStore.BoardGame
									.fromCursor(c);
							BoardGamesManager.deleteBoardGame(
									getContentResolver(),
									boardgame.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverBooks")) {
					c = contentResolver.query(BooksStore.Book.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Book book = BooksStore.Book.fromCursor(c);
							final String mID = book.getInternalId();
							final Bitmap bitmap = Preferences
									.getBitmapForManager(book);

							ImageUtilities.deleteCachedCover(mID);
							ImportUtilities.addCoverToCache(mID, ImageUtilities
									.createCover(bitmap,
											Preferences.getWidthForManager(),
											Preferences.getHeightForManager()));
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverComics")) {
					c = contentResolver.query(ComicsStore.Comic.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Comic comic = ComicsStore.Comic.fromCursor(c);
							ComicsManager.deleteComic(getContentResolver(),
									comic.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverGadgets")) {
					c = contentResolver.query(GadgetsStore.Gadget.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Gadget gadget = GadgetsStore.Gadget.fromCursor(c);
							GadgetsManager.deleteGadget(getContentResolver(),
									gadget.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverMovies")) {
					c = contentResolver.query(MoviesStore.Movie.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Movie movie = MoviesStore.Movie.fromCursor(c);
							MoviesManager.deleteMovie(getContentResolver(),
									movie.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverMusic")) {
					c = contentResolver.query(MusicStore.Music.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Music music = MusicStore.Music.fromCursor(c);
							MusicManager.deleteMusic(getContentResolver(),
									music.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverSoftware")) {
					c = contentResolver.query(
							SoftwareStore.Software.CONTENT_URI, null, null,
							null, null);
					if (c.moveToFirst()) {
						do {
							Software software = SoftwareStore.Software
									.fromCursor(c);
							SoftwareManager.deleteSoftware(
									getContentResolver(),
									software.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverTools")) {
					c = contentResolver.query(ToolsStore.Tool.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Tool tool = ToolsStore.Tool.fromCursor(c);
							ToolsManager.deleteTool(getContentResolver(),
									tool.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverToys")) {
					c = contentResolver.query(ToysStore.Toy.CONTENT_URI, null,
							null, null, null);
					if (c.moveToFirst()) {
						do {
							Toy toy = ToysStore.Toy.fromCursor(c);
							ToysManager.deleteToy(getContentResolver(),
									toy.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.downloadCoverVideoGames")) {
					c = contentResolver.query(
							VideoGamesStore.VideoGame.CONTENT_URI, null, null,
							null, null);
					if (c.moveToFirst()) {
						do {
							VideoGame videogame = VideoGamesStore.VideoGame
									.fromCursor(c);
							VideoGamesManager.deleteVideoGame(
									getContentResolver(),
									videogame.getInternalId());
						} while (c.moveToNext());
					}
				}
			} finally {
				if (c != null)
					c.close();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (downloadingDialog != null)
				dismissDialogSafely(DOWNLOAD_COVER_DIALOG);

			/*
			 * UIUtilities.showToast(getApplicationContext(),
			 * R.string.preferences_download_cover_everything_success);
			 */
		}

	}

	public class ExportEverythingTask extends AsyncTask<Void, Void, Void> {
		private Boolean mToDropbox = false;
		private ContentResolver mResolver = getContentResolver();
		private ProgressDialog exportingDialog = new ProgressDialog(
				SettingsActivity.this);

		public ExportEverythingTask(boolean dropbox) {
			mToDropbox = dropbox;
		}

		@Override
		public void onPreExecute() {
			showDialog(EXPORT_EVERYTHING_DIALOG);
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesApparel, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesBoardGames, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesBooks, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesComics, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesGadgets, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesMovies, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesMusic, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesSoftware, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesTools, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesToys, mResolver);
				ExportUtilities.exportItems(
						IOUtilities.outputTypes.shelvesVideoGames, mResolver);

				if (mToDropbox) {
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_APPAREL);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_BOARDGAMES);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_BOOKS);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_COMICS);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_GADGETS);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_MOVIES);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_MUSIC);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_SOFTWARE);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_TOOLS);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_TOYS);
					sendToDropbox(ExportUtilities.EXPORT_FILE_SHELVES_VIDEOGAMES);
				}
			} catch (Exception e) {
				Log.e(LOG_TAG, "Export error, toDropbox is : " + mToDropbox);
				e.printStackTrace();
			}

			return null;
		}

		@Override
		public void onPostExecute(Void notused) {
			if (exportingDialog != null)
				dismissDialogSafely(EXPORT_EVERYTHING_DIALOG);

			UIUtilities.showToast(getApplicationContext(),
					R.string.success_exported);
		}
	}

	class DeletePreferenceClickListener implements OnPreferenceClickListener {

		public boolean onPreferenceClick(final Preference pref) {
			new AlertDialog.Builder(SettingsActivity.this)
					.setMessage(
							getString(R.string.preferences_delete_everything_dialog))
					.setPositiveButton(
							getString(R.string.preferences_delete_everything_ok_button),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									new DeleteCollectionTask(pref.getKey())
											.execute();
								}
							})
					.setNegativeButton(
							getString(R.string.preferences_delete_everything_cancel_button),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).show();
			return true;
		}
	}

	class DeleteCollectionTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog deletingDialog = new ProgressDialog(
				SettingsActivity.this);
		private String mType = null;

		DeleteCollectionTask(String type) {
			mType = type;
		}

		@Override
		protected void onPreExecute() {
			showDialog(DELETE_ALL_DIALOG);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			Cursor c = null;
			ContentResolver contentResolver = getContentResolver();
			try {
				if (mType.equals("shelves.deleteApparel")) {
					c = contentResolver.query(ApparelStore.Apparel.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Apparel apparel = ApparelStore.Apparel
									.fromCursor(c);
							ApparelManager.deleteApparel(getContentResolver(),
									apparel.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteBoardGames")) {
					c = contentResolver.query(
							BoardGamesStore.BoardGame.CONTENT_URI, null, null,
							null, null);
					if (c.moveToFirst()) {
						do {
							BoardGame boardgame = BoardGamesStore.BoardGame
									.fromCursor(c);
							BoardGamesManager.deleteBoardGame(
									getContentResolver(),
									boardgame.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteBooks")) {
					c = contentResolver.query(BooksStore.Book.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Book book = BooksStore.Book.fromCursor(c);
							BooksManager.deleteBook(getContentResolver(),
									book.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteComics")) {
					c = contentResolver.query(ComicsStore.Comic.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Comic comic = ComicsStore.Comic.fromCursor(c);
							ComicsManager.deleteComic(getContentResolver(),
									comic.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteGadgets")) {
					c = contentResolver.query(GadgetsStore.Gadget.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Gadget gadget = GadgetsStore.Gadget.fromCursor(c);
							GadgetsManager.deleteGadget(getContentResolver(),
									gadget.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteMovies")) {
					c = contentResolver.query(MoviesStore.Movie.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Movie movie = MoviesStore.Movie.fromCursor(c);
							MoviesManager.deleteMovie(getContentResolver(),
									movie.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteMusic")) {
					c = contentResolver.query(MusicStore.Music.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Music music = MusicStore.Music.fromCursor(c);
							MusicManager.deleteMusic(getContentResolver(),
									music.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteSoftware")) {
					c = contentResolver.query(
							SoftwareStore.Software.CONTENT_URI, null, null,
							null, null);
					if (c.moveToFirst()) {
						do {
							Software software = SoftwareStore.Software
									.fromCursor(c);
							SoftwareManager.deleteSoftware(
									getContentResolver(),
									software.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteTools")) {
					c = contentResolver.query(ToolsStore.Tool.CONTENT_URI,
							null, null, null, null);
					if (c.moveToFirst()) {
						do {
							Tool tool = ToolsStore.Tool.fromCursor(c);
							ToolsManager.deleteTool(getContentResolver(),
									tool.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteToys")) {
					c = contentResolver.query(ToysStore.Toy.CONTENT_URI, null,
							null, null, null);
					if (c.moveToFirst()) {
						do {
							Toy toy = ToysStore.Toy.fromCursor(c);
							ToysManager.deleteToy(getContentResolver(),
									toy.getInternalId());
						} while (c.moveToNext());
					}
				} else if (mType.equals("shelves.deleteVideoGames")) {
					c = contentResolver.query(
							VideoGamesStore.VideoGame.CONTENT_URI, null, null,
							null, null);
					if (c.moveToFirst()) {
						do {
							VideoGame videogame = VideoGamesStore.VideoGame
									.fromCursor(c);
							VideoGamesManager.deleteVideoGame(
									getContentResolver(),
									videogame.getInternalId());
						} while (c.moveToNext());
					}
				}
			} finally {
				if (c != null)
					c.close();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (deletingDialog != null)
				dismissDialogSafely(DELETE_ALL_DIALOG);

			UIUtilities.showToast(getApplicationContext(),
					R.string.preferences_delete_everything_success);
		}

	}

	public static SettingsActivity getInstance() {
		return instance;
	}

	public AccountChooser getAccountChooser() {
		return accountChooser;
	}
}
