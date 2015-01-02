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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.mytracks.io.docs.DocsHelper;
import com.google.android.apps.mytracks.io.docs.DocsTagBuilder;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.GDataWrapper;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.GDataServiceClient;
import com.google.wireless.gdata.docs.DocumentsClient;
import com.google.wireless.gdata.docs.SpreadsheetsClient;
import com.google.wireless.gdata.docs.XmlDocsGDataParserFactory;
import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.activity.SettingsActivity;
import com.miadzin.shelves.util.ExportUtilities;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.TextUtilities;

/**
 * A helper class used to transmit tracks statistics to Google Docs/Trix.
 * 
 * @author Sandor Dornbush
 * @author Garen J. Torikian
 */

public class SendToDocs {
	/** The GData service name for Google Spreadsheets (aka Trix) */
	public static final String GDATA_SERVICE_NAME_TRIX = "wise";

	/** The GData service name for the Google Docs Document List */
	public static final String GDATA_SERVICE_NAME_DOCLIST = "writely";

	private final Activity activity;
	private final AuthManager trixAuth;
	private final AuthManager docListAuth;

	private boolean createdNewSpreadSheet = false;

	private boolean success = true;
	private String statusMessage = "";
	private Runnable onCompletion = null;

	private final ContentResolver cr;
	private final String[] collectionsToSend;

	// the timeout until a connection is established
	private static final int CONNECTION_TIMEOUT = 1500; /* 5 seconds */

	// the timeout for waiting for data
	private static final int SOCKET_TIMEOUT = 1500; /* 5 seconds */

	// the timeout until a ManagedClientConnection is got
	// from ClientConnectionRequest
	private static final long MCC_TIMEOUT = 1500; /* 5 seconds */

	private final String LOG_TAG = "SendToDocs";

	public SendToDocs(Activity activity, AuthManager trixAuth,
			AuthManager docListAuth, String[] collectionsToSend) {
		this.activity = activity;
		this.trixAuth = trixAuth;
		this.docListAuth = docListAuth;
		this.collectionsToSend = collectionsToSend;
		this.cr = activity.getContentResolver();
	}

	public void sendToDocs() {
		StringBuilder collection = new StringBuilder();
		for (String c : collectionsToSend)
			collection.append(c).append(", ");
		Log.d(LOG_TAG, "Sending to Google Docs: " + collection.toString());
		new Thread("SendToGoogleDocs") {
			@Override
			public void run() {
				doUpload();
			}
		}.start();
	}

	private void doUpload() {
		statusMessage = activity.getString(R.string.error_sending_to_docs);
		success = false;

		try {

			// Transmit info via GData feed:
			// -------------------------------

			Log.d(LOG_TAG, "Uploading to spreadsheet");
			success = uploadToDocs();
			if (success) {
				statusMessage = activity
						.getString(R.string.status_have_been_uploaded_to_docs);
			} else {
				statusMessage = activity
						.getString(R.string.error_sending_to_docs);
			}
			SettingsActivity.getInstance().getAndSetProgressValue(
					100 - SettingsActivity.getInstance().getProgressValue());
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
	 * Uploads the collection to Google Docs using the docs GData feed.
	 * 
	 * @param c
	 *            the collection
	 */
	private boolean uploadToDocs() {
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

			final int progressLeft = (50 / collectionsToSend.length) / 3;

			for (String name : collectionsToSend) {
				Log.d(LOG_TAG, "Sending to Google Docs: " + name);

				DocumentsClient docsGdataClient = new DocumentsClient(
						androidClient, new XmlDocsGDataParserFactory(
								new AndroidXmlParserFactory()));
				docListWrapper.setClient(docsGdataClient);

				final String sheetTitle = name + " (Shelves) ";

				String spreadsheetId = null;
				// First try to find the spreadsheet:
				/*
				 * try { spreadsheetId = docsHelper.requestSpreadsheetId(
				 * docListWrapper, sheetTitle); } catch (IOException e) {
				 * Log.i(LOG_TAG, "Spreadsheet lookup failed.", e); return
				 * false; }
				 * 
				 * if (spreadsheetId == null) {
				 * SettingsActivity.getInstance().getAndSetProgressValue(
				 * progressLeft); // Waiting a few seconds and trying again.
				 * Maybe the server // just // had a // hiccup (unfortunately
				 * that happens quite a lot...). try { Thread.sleep(5000); }
				 * catch (InterruptedException e) { Log.e(LOG_TAG,
				 * "Sleep interrupted", e); }
				 * 
				 * try { spreadsheetId = docsHelper.requestSpreadsheetId(
				 * docListWrapper, sheetTitle); } catch (IOException e) {
				 * Log.i(LOG_TAG, "2nd spreadsheet lookup failed.", e); return
				 * false; } }
				 */

				// We were unable to find an existing spreadsheet, so create a
				// new one.
				if (spreadsheetId == null) {
					Log.i(LOG_TAG, "Creating new spreadsheet: " + sheetTitle);

					try {
						spreadsheetId = docsHelper.createSpreadsheet(activity,
								docListWrapper, name.toLowerCase(), sheetTitle);
					} catch (IOException e) {
						Log.i(LOG_TAG, "Failed to create new spreadsheet "
								+ sheetTitle, e);
						return false;
					}

					createdNewSpreadSheet = true;

					if (spreadsheetId == null) {
						SettingsActivity.getInstance().getAndSetProgressValue(
								progressLeft);
						// The previous creation might have succeeded even
						// though GData reported an error. Seems to be a know
						// bug,
						// see
						// http://code.google.com/p/gdata-issues/issues/detail?id=929
						Log.w(LOG_TAG,
								"Create might have failed. Trying to find created document.");
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							Log.e(LOG_TAG, "Sleep interrupted", e);
						}

						try {
							spreadsheetId = docsHelper.requestSpreadsheetId(
									docListWrapper, sheetTitle);
						} catch (IOException e) {
							Log.i(LOG_TAG, "Failed create-failed lookup", e);
							return false;
						}

						if (spreadsheetId == null) {
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								Log.e(LOG_TAG, "Sleep interrupted", e);
							}

							try {
								Log.w(LOG_TAG, "Checking id one more time");
								spreadsheetId = docsHelper
										.requestSpreadsheetId(docListWrapper,
												sheetTitle);
							} catch (IOException e) {
								Log.i(LOG_TAG, "Failed create-failed relookup",
										e);
								return false;
							}
						}
						if (spreadsheetId == null) {
							Log.w(LOG_TAG,
									"Creating new spreadsheet really failed.");
							return false;
						}
					}
				}

				String worksheetId = null;
				try {
					worksheetId = docsHelper.getWorksheetId(trixWrapper,
							spreadsheetId);
					if (worksheetId == null) {
						throw new IOException(
								"Worksheet ID lookup returned empty");
					}
				} catch (IOException e) {
					Log.i(LOG_TAG, "Looking up worksheet id failed.", e);
					return false;
				}
				SettingsActivity.getInstance().getAndSetProgressValue(
						progressLeft);

				final String fileName = ExportUtilities
						.determineShelvesFileName(name);

				Uri uri = ShelvesApplication.TYPES_TO_URI.get(name
						.toLowerCase());

				if (uri == null)
					uri = ShelvesApplication.TYPES_TO_URI.get(name);

				ExportUtilities.exportingToShelves(
						IOUtilities.getExternalFile(fileName), cr, uri);

				final String[] data = TextUtilities
						.readFileAsString(
								IOUtilities.getExternalFile(fileName)
										.toString()).replaceAll("\"", "")
						.split("\n");

				Log.i(LOG_TAG, "Updating spreadsheet for " + name);

				final int dataLength = data.length;
				int updatePercentage = dataLength / progressLeft;

				if (data != null) {
					final String[] headers = data[0].split("\\t+");

					for (int i = 1; i < dataLength; i++) {
						final String[] row = data[i].split("\\t+");
						DocsTagBuilder dtb = new DocsTagBuilder();

						/*
						 * if (!createdNewSpreadSheet) { if
						 * (querySpreadsheet(spreadsheetId, worksheetId,
						 * trixAuth, row[0])) continue; }
						 */

						for (int j = 0; j < row.length; j++) {
							dtb.append(headers[j], row[j]);
						}

						putShelvesData(activity, trixAuth, spreadsheetId,
								worksheetId, dtb);

						if (updatePercentage <= 0)
							updatePercentage = 5;
						if (i % updatePercentage == 0)
							SettingsActivity.getInstance()
									.getAndSetProgressValue(1);

						// GJT: Can't send more than 20 inserts/second...
						// just chill out no matter what
						if (dataLength % 20 == 0) {
							try {
								Thread.sleep(1500);
							} catch (InterruptedException e) {
								Log.e(LOG_TAG, "Sleep interrupted", e);
							}
						}
					}
				}

				Log.i(LOG_TAG, "Done uploading to docs.");
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "Unable to upload docs.", e);
			return false;
		} finally {
			if (androidClient != null) {
				androidClient.close();
			}
		}
		return true;
	}

	private boolean querySpreadsheet(String spreadsheetId, String worksheetId,
			AuthManager trixAuth, String iid) throws IOException {

		final String worksheetUri = String.format(
				DocsHelper.DOCS_SPREADSHEET_URL_FORMAT, spreadsheetId,
				worksheetId);

		URL url = new URL(String.format(worksheetUri + "?sq=%s",
				URLEncoder.encode("id=" + iid, "UTF8")));
		URLConnection conn = url.openConnection();
		conn.addRequestProperty(DocsHelper.CONTENT_TYPE_PARAM,
				DocsHelper.ATOM_FEED_MIME_TYPE);
		conn.addRequestProperty("Authorization",
				"GoogleLogin auth=" + trixAuth.getAuthToken());

		BufferedReader rd = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));

		String line;
		while ((line = rd.readLine()) != null) {
			if (line.contains("internalid:")) {
				Log.i(LOG_TAG, "Skipping _id " + String.valueOf(iid)
						+ " since it already exists");
				rd.close();
				return true;
			}

		}
		rd.close();

		return false;
	}

	private void putShelvesData(Context context, AuthManager trixAuth,
			String spreadsheetId, String worksheetId, DocsTagBuilder tagBuilder)
			throws IOException {

		String worksheetUri = String.format(
				DocsHelper.DOCS_SPREADSHEET_URL_FORMAT, spreadsheetId,
				worksheetId);

		String postText = new StringBuilder()
				.append("<entry xmlns='http://www.w3.org/2005/Atom' "
						+ "xmlns:gsx='http://schemas.google.com/spreadsheets/"
						+ "2006/extended'>").append(tagBuilder.build())
				.append("</entry>").toString();

		// Log.i(LOG_TAG, "Inserting at: " + spreadsheetId + " => " +
		// worksheetUri);

		// Log.i(LOG_TAG, postText);

		writeRowData(trixAuth, worksheetUri, postText);

		// Log.i(LOG_TAG, "Post finished.");
	}

	/**
	 * Writes spreadsheet row data to the indicated worksheet.
	 * 
	 * @param trixAuth
	 *            The GData authorization for the spreadsheet service.
	 * @param worksheetUri
	 *            The URI of the worksheet to be altered.
	 * @param postText
	 *            The XML tags describing the change to be made.
	 * @throws IOException
	 *             Thrown if an error occurs during the write.
	 */
	protected void writeRowData(AuthManager trixAuth, String worksheetUri,
			String postText) throws IOException {
		// No need for a wrapper because we know that the authorization was good
		// enough to get this far.
		URL url = new URL(worksheetUri);
		URLConnection conn = url.openConnection();
		conn.addRequestProperty(DocsHelper.CONTENT_TYPE_PARAM,
				DocsHelper.ATOM_FEED_MIME_TYPE);
		conn.addRequestProperty("Authorization",
				"GoogleLogin auth=" + trixAuth.getAuthToken());
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		wr.write(postText);
		wr.flush();

		// Get the response.
		// TODO: Should we parse the response, rather than simply throwing it
		// away?
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
			// Process line.
			Log.i(LOG_TAG, "r: " + line);
		}
		wr.close();
		rd.close();
	}
}
