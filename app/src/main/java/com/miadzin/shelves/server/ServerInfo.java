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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.net.Uri;
import android.util.Xml;
import android.view.InflateException;

import com.miadzin.shelves.BuildConfig;
import com.miadzin.shelves.activity.SettingsActivity;
import com.miadzin.shelves.provider.books.BooksStore;
import com.miadzin.shelves.util.CookieStore;
import com.miadzin.shelves.util.Entities;
import com.miadzin.shelves.util.HttpManager;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.ImageUtilities;

public class ServerInfo {
	public static final String NAME = "5h3lv35";

	protected static final String DETAIL_PHRASE = "detail";
	public static final String IMAGE_PHRASE = "image";
	protected static final String IMAGE_PATH = "/images/";

	protected static final String VALUE_SEARCHINDEX_APPAREL = "Apparel";
	protected static final String VALUE_RESPONSEGROUP_LARGE = "Large";
	protected static final String VALUE_RESPONSEGROUP_MEDIUM = "Medium";

	protected static final String RESPONSE_TAG_REQUEST = "Request";
	protected static final String RESPONSE_TAG_ISVALID = "IsValid";
	protected static final String RESPONSE_TAG_ERRORS = "Errors";
	protected static final String RESPONSE_TAG_ASIN = "ASIN";
	protected static final String RESPONSE_TAG_DETAILPAGEURL = "DetailPageURL";
	protected static final String RESPONSE_TAG_ITEMATTRIBUTES = "ItemAttributes";
	protected static final String RESPONSE_TAG_MANUFACTURER = "Manufacturer";
	protected static final String RESPONSE_TAG_FABRICTYPE = "FabricType";
	protected static final String RESPONSE_TAG_BRAND = "Brand";
	protected static final String RESPONSE_TAG_LABEL = "Label";
	protected static final String RESPONSE_TAG_ISBN = "ISBN";
	protected static final String RESPONSE_TAG_EAN = "EAN";
	protected static final String RESPONSE_TAG_UPC = "UPC";
	protected static final String RESPONSE_TAG_SKU = "SKU";
	protected static final String RESPONSE_TAG_FEATURE = "Feature";
	protected static final String RESPONSE_TAG_FORMAT = "Format";
	protected static final String RESPONSE_TAG_RUNNING_TIME = "RunningTime";
	protected static final String RESPONSE_TAG_RELEASEDATE = "ReleaseDate";
	protected static final String RESPONSE_TAG_THEATRICALDEBUT = "TheatricalReleaseDate";
	protected static final String RESPONSE_TAG_TITLE = "Title";
	protected static final String RESPONSE_TAG_OFFERSUMMARY = "OfferSummary";
	protected static final String RESPONSE_TAG_EDITORIALREVIEW = "EditorialReview";
	protected static final String RESPONSE_TAG_SOURCE = "Source";
	protected static final String RESPONSE_TAG_CONTENT = "Content";
	protected static final String RESPONSE_TAG_IMAGESET = "ImageSet";
	// protected static final String RESPONSE_TAG_SWATCHIMAGE = "SwatchImage";
	// protected static final String RESPONSE_TAG_SMALLIMAGE = "SmallImage";
	protected static final String RESPONSE_TAG_THUMBNAILIMAGE = "ThumbnailImage";
	protected static final String RESPONSE_TAG_TINYIMAGE = "TinyImage";
	protected static final String RESPONSE_TAG_MEDIUMIMAGE = "MediumImage";
	protected static final String RESPONSE_TAG_LARGEIMAGE = "LargeImage";
	protected static final String RESPONSE_TAG_URL = "URL";
	protected static final String RESPONSE_TAG_ITEM = "Item";

	protected static final String RESPONSE_ATTR_CATEGORY = "Category";
	protected static final String RESPONSE_VALUE_CATEGORY_PRIMARY = "primary";
	protected static final String RESPONSE_VALUE_CATEGORY_VARIANT = "variant";

	protected static final String RESPONSE_TAG_DEPARTMENT = "Department";
	protected static final String RESPONSE_TAG_AUDIENCE = "AudienceRating";
	protected static final String RESPONSE_TAG_BINDING = "Binding";
	protected static final String RESPONSE_TAG_DEWEY = "DeweyDecimalNumber";
	protected static final String RESPONSE_TAG_EDITION = "Edition";
	protected static final String RESPONSE_TAG_LANGUAGE = "Language";
	protected static final String RESPONSE_TAG_NAME = "Name";
	protected static final String RESPONSE_TAG_TYPE = "Type";
	protected static final String RESPONSE_TAG_LISTPRICE = "ListPrice";
	protected static final String RESPONSE_TAG_FORMATTEDPRICE = "FormattedPrice";
	protected static final String RESPONSE_TAG_LOWESTUSEDPRICE = "LowestUsedPrice";

	protected static final String VALUE_SEARCHINDEX_BOOKS = "Books";
	protected static final String RESPONSE_TAG_AUTHOR = "Author";
	protected static final String RESPONSE_TAG_CREATOR = "Creator";

	protected static final String RESPONSE_TAG_NUMBEROFPAGES = "NumberOfPages";
	protected static final String RESPONSE_TAG_PUBLICATIONDATE = "PublicationDate";

	protected static final String VALUE_SEARCHINDEX_GADGETS = "Electronics";

	protected static final String VALUE_SEARCHINDEX_MOVIES = "DVD";
	protected static final String RESPONSE_TAG_DIRECTOR = "Director";
	protected static final String RESPONSE_TAG_ACTOR = "Actor";
	protected static final String RESPONSE_TAG_RATIO = "AspectRatio";

	protected static final String VALUE_SEARCHINDEX_MUSIC = "Music";
	protected static final String RESPONSE_TAG_DISC = "Disc";
	protected static final String RESPONSE_TAG_TRACK = "Track";

	protected static final String RESPONSE_TAG_ARTIST = "Artist";
	protected static final String RESPONSE_TAG_ORIGINALRELEASEDATE = "OriginalReleaseDate";

	protected static final String VALUE_SEARCHINDEX_SOFTWARE = "Software";
	protected static final String RESPONSE_TAG_PLATFORM = "Platform";

	protected static final String VALUE_SEARCHINDEX_TOOLS = "Tools";

	protected static final String VALUE_SEARCHINDEX_TOYS = "Toys";

	protected static final String VALUE_SEARCHINDEX_VIDEOGAMES = "VideoGames";

	protected static final String RESPONSE_TAG_ESRBAGERATING = "ESRBAgeRating";
	protected static final String RESPONSE_TAG_FEATURES = "Features";
	protected static final String RESPONSE_TAG_SPECIALFEATURES = "SpecialFeatures";
	protected static final String RESPONSE_TAG_GENRE = "Genre";
	protected static final String RESPONSE_TAG_HARDWAREPLATFORM = "HardwarePlatform";

	/*
	 * FILL THIS SET OUT WITH YOUR OWN SERVER CONTENT
	 */
    private static final String SERVER_SCRIPT = BuildConfig.SERVER_SCRIPT; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String DETAIL_START = BuildConfig.DETAIL_START; // YOU ARE SUPPOSED TO FILL THIS OUT!
    public static final String IMAGE_START = BuildConfig.IMAGE_START; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String API_REST_HOST = BuildConfig.API_REST_HOST; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String API_REST_URL = BuildConfig.API_REST_URL; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String API_ITEM_LOOKUP = BuildConfig.API_ITEM_LOOKUP; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String API_ITEM_SEARCH = BuildConfig.API_ITEM_SEARCH; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_API_KEY = BuildConfig.PARAM_API_KEY; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_API_VERSION = BuildConfig.PARAM_API_VERSION; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_API_SERVICE = BuildConfig.PARAM_API_SERVICE; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_OPERATION = BuildConfig.PARAM_OPERATION; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_IDTYPE = BuildConfig.PARAM_IDTYPE; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_SEARCHINDEX = BuildConfig.PARAM_SEARCHINDEX; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_ITEMID = BuildConfig.PARAM_ITEMID; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_RESPONSEGROUP = BuildConfig.PARAM_RESPONSEGROUP; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_KEYWORDS = BuildConfig.PARAM_KEYWORDS; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_ASSOCIATETAG = BuildConfig.PARAM_ASSOCIATETAG; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String PARAM_ITEMPAGE = BuildConfig.PARAM_ITEMPAGE; // YOU ARE SUPPOSED TO FILL THIS OUT!

    protected static final String VALUE_SERVICE = BuildConfig.VALUE_SERVICE; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String VALUE_VERSION = BuildConfig.VALUE_VERSION; // YOU ARE SUPPOSED TO FILL THIS OUT!
    protected static final String VALUE_ASSOCIATEID = BuildConfig.VALUE_ASSOCIATEID; // YOU ARE SUPPOSED TO FILL THIS OUT!
    public static final String API_REST_HOST_US = BuildConfig.API_REST_HOST_US; // YOU ARE SUPPOSED TO FILL THIS OUT!
    public static final String API_REST_HOST_CA = BuildConfig.API_REST_HOST_CA; // YOU ARE SUPPOSED TO FILL THIS OUT!
    public static final String API_REST_HOST_UK = BuildConfig.API_REST_HOST_UK; // YOU ARE SUPPOSED TO FILL THIS OUT!
    public static final String API_REST_HOST_FR = BuildConfig.API_REST_HOST_FR; // YOU ARE SUPPOSED TO FILL THIS OUT!
    public static final String API_REST_HOST_DE = BuildConfig.API_REST_HOST_DE; // YOU ARE SUPPOSED TO FILL THIS OUT!
    public static final String API_REST_HOST_JP = BuildConfig.API_REST_HOST_JP; // YOU ARE SUPPOSED TO FILL THIS OUT!
    public static final String API_REST_HOST_IT = BuildConfig.API_REST_HOST_IT; // YOU ARE SUPPOSED TO FILL THIS OUT!
    public static final String API_REST_HOST_CN = BuildConfig.API_REST_HOST_CN; // YOU ARE SUPPOSED TO FILL THIS OUT!

    // YOU ARE SUPPOSED TO FILL THESE OUT!
	private static final String[] API_KEYS = { BuildConfig.API_KEYS_ONE, BuildConfig.API_KEYS_TWO };

	// Dropbox stuff
	public final static String DROPBOX_APP_KEY = BuildConfig.DROPBOX_APP_KEY; // YOU ARE SUPPOSED TO FILL THIS OUT!
	public final static String DROPBOX_APP_SECRET = BuildConfig.DROPBOX_APP_SECRET; // YOU ARE SUPPOSED TO FILL THIS OUT!
	/*
	 * END FILL; HAVE A NICE DAY!
	 */

	private static final String LOG_TAG = "PublicServerInfo";

	// GJT: Fields common to all derivatives
	final protected String mHost = API_REST_HOST;
	protected ServerImageLoader mLoader = new ServerImageLoader();

	protected ServerInfo() {
	}

	public static class ServerImageLoader {
		public ImageUtilities.ExpiringBitmap load(String url) {
			final String cookie = CookieStore.get().getCookie(url);
			return ImageUtilities.load(url, cookie);
		}
	}

	protected static Uri.Builder assembleURI(String id,
			IOUtilities.inputTypes type, Context context, String searchIndex,
			String altTag) {

		if (type == null) {
			if (searchIndex.equals(VALUE_SEARCHINDEX_MUSIC))
				return buildFindQuery(id, searchIndex, context, altTag,
						VALUE_RESPONSEGROUP_LARGE);
			else
				return buildFindQuery(id, searchIndex, context, altTag);
		}

		switch (type) {
		case DLApparel:
		case DLBooks:
		case DLGadgets:
		case DLMovies:
		case DLMusic:
		case DLSoftware:
		case DLTools:
		case DLToys:
		case DLVideoGames:
			if (searchIndex.equals(VALUE_SEARCHINDEX_MUSIC))
				return buildFindQuery(id, searchIndex, context,
						RESPONSE_TAG_ASIN, VALUE_RESPONSEGROUP_LARGE);
			else
				return buildFindQuery(id, searchIndex, context,
						RESPONSE_TAG_ASIN);
		default:
			if (searchIndex.equals(VALUE_SEARCHINDEX_MUSIC))
				return buildFindQuery(id, searchIndex, context, altTag,
						VALUE_RESPONSEGROUP_LARGE);
			else
				return buildFindQuery(id, searchIndex, context, altTag);
		}

	}

	/**
	 * Builds an HTTP GET request for the specified API method. The returned
	 * request contains the web service path, the query parameter for the API
	 * KEY and the query parameter for the specified method.
	 * 
	 * @param method
	 *            The API method to invoke.
	 * 
	 * @return A Uri.Builder containing the GET path, the API key and the method
	 *         already encoded.
	 */
	protected static Uri.Builder buildGetMethod(String method, Context context) {
		final Uri.Builder builder = new Uri.Builder();

		builder.path(SERVER_SCRIPT);
		builder.appendQueryParameter("EndpointUri",
				SettingsActivity.getDatabaseRegion(context) + API_REST_URL);
		builder.appendQueryParameter(PARAM_API_KEY, randomAPIKey());
		builder.appendQueryParameter(PARAM_API_VERSION, VALUE_VERSION);
		builder.appendQueryParameter(PARAM_API_SERVICE, VALUE_SERVICE);
		builder.appendQueryParameter(PARAM_OPERATION, method);
		builder.appendQueryParameter(PARAM_ASSOCIATETAG, VALUE_ASSOCIATEID);

		return builder;
	}

	private static String randomAPIKey() {
		int randomPos = 0 + (int) (Math.random() * ((API_KEYS.length - 1 - 0) + 1));

		return API_KEYS[randomPos];
	}

	public static class Description {
		private String mSource;
		private String mContent;

		public Description(String source, String content) {
			Entities e = new Entities();
			mSource = source;

			// GJT: All this crap just because Google Books has some weird ASCII
			// response
			mContent = e.unescape(content.replaceAll("\\<.*?>", ""));
		}

		String getSource() {
			return mSource;
		}

		String getContent() {
			return mContent;
		}

		@Override
		public String toString() {
			// GJT: Changed this
			// TODO: We should be storing reviews in a separate table
			return mContent;
			/*
			 * return "<p class=\".source\">" + mSource +
			 * "</p>\n<p class=\".content\">" + mContent + "</p>";
			 */
		}
	}

	/**
	 * Constructs the query used to search for an item. The query can be any
	 * combination of keywords. The store is free to interpret the keywords in
	 * any way.
	 * 
	 * @param query
	 *            A free form text query to search for item.
	 * 
	 * @return The Uri to the list of item matching the query.
	 */

	public static Uri.Builder buildSearchQuery(String query,
			String searchIndex, String page, Context context) {
		final Uri.Builder uri = buildGetMethod(API_ITEM_SEARCH, context);
		uri.appendQueryParameter(PARAM_KEYWORDS, query);
		uri.appendQueryParameter(PARAM_SEARCHINDEX, searchIndex);
		uri.appendQueryParameter(PARAM_RESPONSEGROUP,
				VALUE_RESPONSEGROUP_MEDIUM);
		uri.appendQueryParameter(PARAM_ITEMPAGE, page);
		// android.util.Log.d(LOG_TAG, uri.toString());
		return uri;
	}

	/**
	 * Constructs the query used to find an item identified by its id. The
	 * unique identifier should be either the EAN (ISBN-13) or ISBN (ISBN-10) of
	 * the item to find.
	 * 
	 * @param id
	 *            The EAN or ISBN of the item to find.
	 * 
	 * @return The Uri to the item details for this store.
	 */

	public static Uri.Builder buildFindQuery(String id, String searchIndex,
			Context context, String responseType) {
		return buildFindQuery(id, searchIndex, context, responseType,
				VALUE_RESPONSEGROUP_MEDIUM);
	}

	public static Uri.Builder buildFindQuery(String id, String searchIndex,
			Context context, String responseType, String responseGroup) {

		/*
		 * GJT: Clicking on an item in the Add*Activity list view sometimes
		 * returns multiple results, like with EANs 025195018524 & 0786936786866
		 * for DVDs. So if we're coming from an Add screen, override whatever
		 * load[Item] says and force-use a search on the ASIN Otherwise, resume
		 * as planned, like on imports or barcode lookups, which use
		 * EAN/ISBN/UPC
		 */
		Pattern pattern = Pattern.compile("Add[A-Za-z]+Activity");
		Matcher matcher = pattern.matcher(context.toString());

		final Uri.Builder uri = buildGetMethod(API_ITEM_LOOKUP, context);
		if (matcher.find() || responseType.equals(RESPONSE_TAG_ASIN)) {
			uri.appendQueryParameter(PARAM_IDTYPE, RESPONSE_TAG_ASIN);
			// GJT: Can't have a SearchIndex AND use ASIN
		} else {
			uri.appendQueryParameter(PARAM_SEARCHINDEX, searchIndex);
			uri.appendQueryParameter(PARAM_IDTYPE, responseType);
		}
		uri.appendQueryParameter(PARAM_RESPONSEGROUP, responseGroup);
		uri.appendQueryParameter(PARAM_ITEMID, id);
		return uri;
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

			if (type != XmlPullParser.START_TAG) {
				throw new InflateException(parser.getPositionDescription()
						+ ": No start tag found!");
			}

			String name;
			boolean valid = false;
			final int topDepth = parser.getDepth();

			while (((type = parser.next()) != XmlPullParser.END_TAG || parser
					.getDepth() > topDepth)
					&& type != XmlPullParser.END_DOCUMENT) {

				if (type != XmlPullParser.START_TAG) {
					continue;
				}

				name = parser.getName();

				if (RESPONSE_TAG_REQUEST.equals(name)) {
					valid = isRequestValid(parser);
					break;
				}
			}

			if (valid)
				responseParser.parseResponse(parser);

		} catch (XmlPullParserException e) {
			final IOException ioe = new IOException(
					"Could not parse the response", e);
            throw ioe;
		}
	}

	private static boolean isRequestValid(XmlPullParser parser)
			throws XmlPullParserException, IOException {

		int type;
		String name;
		boolean valid = false;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			name = parser.getName();

			if (RESPONSE_TAG_ISVALID.equals(name)) {
				if (parser.next() != XmlPullParser.TEXT
						|| !"True".equals(parser.getText())) {
					throw new IOException("Invalid request");
				} else {
					valid = true;
				}
			} else if (RESPONSE_TAG_ERRORS.equals(name)) {
				valid = false;
			}
		}

		return valid;
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
		if (RESPONSE_TAG_ITEM.equals(parser.getName())) {
			return true;
		}

		int type;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (RESPONSE_TAG_ITEM.equals(parser.getName())) {
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

	protected Description parseEditorialReview(XmlPullParser parser)
			throws IOException, XmlPullParserException {

		int type;
		String name;
		String source = null;
		String content = null;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			name = parser.getName();
			if (RESPONSE_TAG_SOURCE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					source = parser.getText();
				}
			} else if (RESPONSE_TAG_CONTENT.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					content = parser.getText();
				}
			}
		}

		if (content != null) {
			return new Description(source, content);
		}
		return new Description("", "");
	}

	protected String parseImage(XmlPullParser parser) throws IOException,
			XmlPullParserException {

		int type;
		String name;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			name = parser.getName();
			if (RESPONSE_TAG_URL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					return parser.getText();
				}
			}
		}
		return null;
	}

	protected String parseLanguage(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		String langName = null;
		String langType = null;

		while (true) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				String tag = parser.getName();
				if (RESPONSE_TAG_NAME.equals(tag)) {
					parser.next();
					langName = parser.getText();
				} else if (RESPONSE_TAG_TYPE.equals(tag)) {
					parser.next();
					langType = parser.getText();
				}
			} else if (langType != null && eventType == XmlPullParser.END_TAG) {
				break;
			}
		}

		return langName + " (" + langType + ")";
	}
}
