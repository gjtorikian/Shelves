/*
 * Copyright 2008 Google Inc.
 * Copyright 2010 Garen J. Torikian
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.miadzin.shelves.util.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.apps.mytracks.io.docs.DocsHelper;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper.AuthenticationException;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper.ConflictDetectedException;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper.QueryFunction;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.GDataServiceClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.docs.DocumentsClient;
import com.google.wireless.gdata.docs.SpreadsheetsClient;
import com.google.wireless.gdata.docs.XmlDocsGDataParserFactory;
import com.miadzin.shelves.R;
import com.miadzin.shelves.activity.SettingsActivity;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

/**
 * A helper class used to transmit tracks statistics from Google Docs/Trix.
 * 
 * @author Garen J. Torikian
 */

public class BringFromDocs {
	private static final String DOCS_DOWNLOAD_URL = "https://spreadsheets.google.com/feeds/download/spreadsheets/Export?key=%s&exportFormat=tsv&gid=0";
	private static final String DOCS_MY_SPREADSHEETS_FEED_URL = "https://docs.google.com/feeds/documents/private/full?"
			+ "category=mine,spreadsheet";

	private final Activity activity;
	private final AuthManager trixAuth;
	private final AuthManager docListAuth;
	private final HandlerThread handlerThread;
	private final Handler handler;

	private final String collectionToBring;
	private String workSheetId = null;

	private boolean success = true;
	private String statusMessage = "";
	private Runnable onCompletion = null;

	private final String LOG_TAG = "BringFromDocs";

	public BringFromDocs(Activity activity, AuthManager trixAuth,
			AuthManager docListAuth, String collectionToBring) {
		this.activity = activity;
		this.trixAuth = trixAuth;
		this.docListAuth = docListAuth;
		this.collectionToBring = collectionToBring;

		Log.d(LOG_TAG, "Bringing from Google Docs: " + collectionToBring);
		handlerThread = new HandlerThread("BringFromGoogleDocs");
		handlerThread.start();
		handler = new Handler(handlerThread.getLooper());
	}

	public void run() {
		handler.post(new Runnable() {

			public void run() {
				doDownload();
			}
		});
	}

	private void doDownload() {
		statusMessage = activity.getString(R.string.error_bringing_from_docs);
		success = false;

		try {

			// Transmit stats via GData feed:
			// -------------------------------

			Log.d(LOG_TAG, "Downloading spreadsheet");
			success = downloadFromDocs();
			if (success) {
				statusMessage = activity
						.getString(R.string.status_have_been_downloaded_to_docs);
			} else {
				statusMessage = activity
						.getString(R.string.error_bringing_from_docs);
			}
			Log.d(LOG_TAG, "Done.");
		} finally {
			if (onCompletion != null) {
				activity.runOnUiThread(onCompletion);
			}
		}
	}

	public boolean wasSuccess() {
		return success;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setOnCompletion(Runnable onCompletion) {
		this.onCompletion = onCompletion;
	}

	/**
	 * Downloads the collection from Google Docs using the docs GData feed.
	 * 
	 * @param c
	 *            the collection
	 */
	private boolean downloadFromDocs() {
		GDataWrapper<GDataServiceClient> docListWrapper = new GDataWrapper<GDataServiceClient>();
		docListWrapper.setAuthManager(docListAuth);
		docListWrapper.setRetryOnAuthFailure(true);

		GDataWrapper<GDataServiceClient> trixWrapper = new GDataWrapper<GDataServiceClient>();
		trixWrapper.setAuthManager(trixAuth);
		trixWrapper.setRetryOnAuthFailure(true);

		DocsHelper docsHelper = new DocsHelper();

		GDataClient androidClient = null;
		try {
			androidClient = GDataClientFactory.getGDataClient(activity);
			SpreadsheetsClient gdataClient = new SpreadsheetsClient(
					androidClient, new XmlDocsGDataParserFactory(
							new AndroidXmlParserFactory()));
			trixWrapper.setClient(gdataClient);
			Log.d(LOG_TAG, "GData connection prepared: " + this.docListAuth);

			DocumentsClient docsGdataClient = new DocumentsClient(
					androidClient, new XmlDocsGDataParserFactory(
							new AndroidXmlParserFactory()));
			docListWrapper.setClient(docsGdataClient);

			int progressLeft = 50 / 3;

			final String sheetTitle = collectionToBring + " (Shelves)";

			String spreadsheetId = null;
			// First try to find the spreadsheet:
			try {
				spreadsheetId = docsHelper.requestSpreadsheetId(docListWrapper,
						sheetTitle);
			} catch (IOException e) {
				Log.i(LOG_TAG, "Spreadsheet lookup failed.", e);
				return false;
			}

			if (spreadsheetId == null) {
				SettingsActivity.getInstance().getAndSetProgressValue(
						progressLeft);
				// Waiting a few seconds and trying again. Maybe the server
				// just had a hiccup
				// (unfortunately that happens quite a lot...).
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					Log.e(LOG_TAG, "Sleep interrupted", e);
				}
				try {
					spreadsheetId = docsHelper.requestSpreadsheetId(
							docListWrapper, sheetTitle);
				} catch (IOException e) {
					Log.i(LOG_TAG, "2nd spreadsheet lookup failed.", e);
					return false;
				}
			}

			if (spreadsheetId == null) {
				Log.e(LOG_TAG, "Finding spreadsheet really failed.");
				UIUtilities.showToast(activity,
						R.string.error_could_not_find_spreadsheet);
				return false;
			}

			SettingsActivity.getInstance().getAndSetProgressValue(progressLeft);
			if (!getShelvesData(trixWrapper, spreadsheetId, collectionToBring)) {
				Log.e(LOG_TAG, "Failed downloading spreadsheet");
			}
			Log.i(LOG_TAG, "Done downloading doc.");
		} catch (IOException e) {
			Log.e(LOG_TAG, "Unable to download docs.", e);
			return false;
		} catch (AuthenticationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (com.google.android.apps.mytracks.io.gdata.GDataWrapper.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConflictDetectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (com.google.android.apps.mytracks.io.gdata.GDataWrapper.HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (androidClient != null) {
				androidClient.close();
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private boolean getShelvesData(final GDataWrapper trixWrapper,
			String spreadsheetId, final String name)
			throws AuthenticationException,
			IOException,
			com.google.android.apps.mytracks.io.gdata.GDataWrapper.ParseException,
			ConflictDetectedException,
			com.google.android.apps.mytracks.io.gdata.GDataWrapper.HttpException {
		Log.i(LOG_TAG, "Downloading spreadsheet for " + name);

		final String url = String.format(DOCS_DOWNLOAD_URL, spreadsheetId);

		return trixWrapper.runQuery(new QueryFunction<GDataServiceClient>() {

			public void query(GDataServiceClient client)
					throws AuthenticationException,
					IOException,
					com.google.android.apps.mytracks.io.gdata.GDataWrapper.ParseException,
					ConflictDetectedException,
					com.google.android.apps.mytracks.io.gdata.GDataWrapper.HttpException {
				InputStream is;
				try {
					is = client.getMediaEntryAsStream(url, trixWrapper
							.getAuthManager().getAuthToken());

					final BufferedReader rd = new BufferedReader(
							new InputStreamReader(is));

					SettingsActivity.getInstance().getAndSetProgressValue(
							50 / 3);

					String line;

					StringBuilder sb = new StringBuilder();
					while ((line = rd.readLine()) != null) {
						sb.append(line).append("\n");
					}
					rd.close();

					final String result = sb.toString();

					TextUtilities.writeStringToFile(
							IOUtilities.getExternalFile("Shelves_to_Shelves_"
									+ name + ".txt"), result);

					if (TextUtilities.isEmpty(result))
						throw new IOException();
					Log.i(LOG_TAG, "GET finished.");
				} catch (HttpException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
	}
}
