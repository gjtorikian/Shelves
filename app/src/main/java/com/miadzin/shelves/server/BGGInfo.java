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

package com.miadzin.shelves.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

import com.miadzin.shelves.provider.books.BooksStore;
import com.miadzin.shelves.util.CookieStore;
import com.miadzin.shelves.util.HttpManager;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.IOUtilities.inputTypes;
import com.miadzin.shelves.util.ImageUtilities.ExpiringBitmap;

public class BGGInfo {
	public static final String NAME = "5h3lv35";

	protected static final String DETAILS = "http://www.boardgamegeek.com/boardgame/";
	protected static final String REST_HOST = "boardgamegeek.com";
	protected static final String REST_SEARCH_URL = "/xmlapi/search";
	protected static final String REST_RETRIEVE_URL = "/xmlapi/boardgame";

	protected static final String RESPONSE_TAG_BOARDGAMES = "boardgames";
	protected static final String RESPONSE_TAG_BOARDGAME = "boardgame";
	protected static final String RESPONSE_TAG_URL = "URL";
	protected static final String RESPONSE_TAG_NAME = "name";

	protected static final String ID = "objectid";
	protected static final String PRIMARY_NAME = "primary";
	protected static final String RESPONSE_TAG_TYPE = "Type";

	protected static final String RESPONSE_TAG_YEARPUBLISHED = "yearpublished";
	protected static final String RESPONSE_TAG_MIN_PLAYERS = "minplayers";
	protected static final String RESPONSE_TAG_MAX_PLAYERS = "maxplayers";
	protected static final String RESPONSE_TAG_PLAYING_TIME = "playingtime";
	protected static final String RESPONSE_TAG_AGE = "age";
	protected static final String RESPONSE_TAG_DESCRIPTION = "description";
	protected static final String RESPONSE_TAG_IMAGESET = "image";
	protected static final String RESPONSE_TAG_THUMBNAIL = "thumbnail";
	protected static final String RESPONSE_TAG_BGPUBLISHER = "boardgamepublisher";

	private static final String LOG_TAG = "BGGInfo";

	// GJT: Fields common to all derivatives
	protected static String mStoreName;
	protected String mStoreLabel;
	protected String mHost;
	protected BGGImageLoader mLoader;

	protected BGGInfo() {
	}

	public static class BGGImageLoader {
		public ImageUtilities.ExpiringBitmap load(String url) {
			final String cookie = CookieStore.get().getCookie(url);
			return ImageUtilities.load(url, cookie);
		}
	}

	protected static String getName() {
		return mStoreName;
	}

	public String getLabel() {
		return mStoreLabel;
	}

	/**
	 * Executes an HTTP request on a REST web service. If the response is ok,
	 * the content is sent to the specified response handler.
	 * 
	 * @param host
	 * @param get
	 *            The GET request to executed.
	 * @param handler
	 *            The handler which will parse the response.
	 * 
	 * @throws java.io.IOException
	 */
	protected void executeRequest(HttpHost host, HttpGet get,
			ResponseHandler handler) throws IOException {

		HttpEntity entity = null;
		try {
			final HttpResponse response = HttpManager.execute(host, get);
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == HttpStatus.SC_MOVED_TEMPORARILY
					|| statusCode == HttpStatus.SC_SEE_OTHER) {
				Header header = response.getFirstHeader("Location");
				if (header != null) {
					String redirectURI = header.getValue();
					if ((redirectURI != null) && (!redirectURI.equals(""))) {
						android.util.Log.e(LOG_TAG, "Redirect target: "
								+ redirectURI);
						DefaultHttpClient httpclient = new DefaultHttpClient();
						get = new HttpGet(redirectURI);
						entity = httpclient.execute(get).getEntity();
						final InputStream in = entity.getContent();
						handler.handleResponse(in);
					}
				} else {
					android.util.Log.e(LOG_TAG, "Invalid redirect");
				}
			} else if (statusCode == HttpStatus.SC_OK) {
				entity = response.getEntity();
				final InputStream in = entity.getContent();
				handler.handleResponse(in);
			}
		} finally {
			if (entity != null) {
				entity.consumeContent();
			}
		}
	}

	/**
	 * Parses a valid XML response from the specified input stream. This method
	 * must invoke parse
	 * {@link ResponseParser#parseResponse(org.xmlpull.v1.XmlPullParser)} if the
	 * XML response is valid, or throw an exception if it is not.
	 * 
	 * @param in
	 *            The input stream containing the response sent by the web
	 *            service.
	 * @param responseParser
	 *            The parser to use when the response is valid.
	 * 
	 * @throws java.io.IOException
	 */
	public static void parseResponse(InputStream in,
			ResponseParser responseParser, IOUtilities.inputTypes inputType)
			throws IOException {
		final XmlPullParser parser = Xml.newPullParser();
		try {
			parser.setInput(new InputStreamReader(in));

			int type;
			while ((type = parser.next()) != XmlPullParser.START_TAG
					&& type != XmlPullParser.END_DOCUMENT) {
				// Empty
			}
			/*
			 * if (type != XmlPullParser.START_TAG) { throw new
			 * InflateException(parser.getPositionDescription() +
			 * ": No start tag found!"); }
			 * 
			 * String name; boolean valid = false; final int topDepth =
			 * parser.getDepth();
			 * 
			 * while (((type = parser.next()) != XmlPullParser.END_TAG || parser
			 * .getDepth() > topDepth) && type != XmlPullParser.END_DOCUMENT) {
			 * 
			 * if (type != XmlPullParser.START_TAG) { continue; }
			 * 
			 * name = parser.getName(); valid = true; if
			 * (RESPONSE_TAG_BOARDGAME.equals(name)) { valid = true; break; } }
			 * 
			 * if (valid)
			 */
			responseParser.parseResponse(parser);

		} catch (XmlPullParserException e) {
			final IOException ioe = new IOException(
					"Could not parse the response", e);
            throw ioe;
		}
	}

	/**
	 * Finds the next item entry in the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the item.
	 * 
	 * @return True if an item was found, false otherwise.
	 */
	protected boolean findNextItem(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		if (RESPONSE_TAG_BOARDGAMES.equals(parser.getName())) {
			return true;
		}
		if (RESPONSE_TAG_BOARDGAME.equals(parser.getName())) {
			return true;
		}
		int type;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (RESPONSE_TAG_BOARDGAME.equals(parser.getName())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Response handler used with
	 * {@link BooksStore#executeRequest(org.apache.http.HttpHost, org.apache.http.client.methods.HttpGet, BooksStore.ResponseHandler)}
	 * . The handler is invoked when a response is sent by the server. The
	 * response is made available as an input stream.
	 */
	public interface ResponseHandler {
		/**
		 * Processes the responses sent by the HTTP server following a GET
		 * request.
		 * 
		 * @param in
		 *            The stream containing the server's response.
		 * 
		 * @throws java.io.IOException
		 */
        void handleResponse(InputStream in) throws IOException;
	}

	/**
	 * Response parser. When the request returns a valid response, this parser
	 * is invoked to process the XML response.
	 */
	public interface ResponseParser {
		/**
		 * Processes the XML response sent by the web service after a successful
		 * request.
		 * 
		 * @param parser
		 *            The parser containing the XML responses.
		 * 
		 * @throws org.xmlpull.v1.XmlPullParserException
		 * @throws java.io.IOException
		 */
        void parseResponse(XmlPullParser parser)
				throws XmlPullParserException, IOException;
	}
}
