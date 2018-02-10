/*
 * Copyright (C) 2008 Google Inc.
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

package com.miadzin.shelves.provider.comics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.base.BaseItem.ImageSize;
import com.miadzin.shelves.server.CVInfo;
import com.miadzin.shelves.util.Entities;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.IOUtilities.inputTypes;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;

/**
 * Utility class to load comics from a comics store.
 */
public class ComicsStore extends CVInfo {
	static final String LOG_TAG = "ComicsStore";

	public static class Comic extends BaseItem implements Parcelable,
			BaseColumns {
		public static final Uri CONTENT_URI = Uri
				.parse("content://ComicsProvider/comics");

		String mAuthors = null;
		String mDescriptions;
		Date mPublicationDate;
		List<String> mArtists;
		List<String> mCharacters;
		String mIssueNumber;

		private CVImageLoader mLoader;

		Map<ImageSize, String> mImages;

		@Override
		public String getImageUrl(ImageSize size) {
			String url = mImages.get(size);
			if (TextUtilities.isEmpty(url))
				return null;
			else
				return TextUtilities.unprotectString(url);
		}

		Comic() {
			this("", null);
		}

		Comic(String storePrefix, CVImageLoader loader) {
			mStorePrefix = CVInfo.NAME;
			mLoader = loader;
			mImages = new HashMap<ImageSize, String>(6);
			mDescriptions = new String();
			mTags = new ArrayList<String>(1);
			mArtists = new ArrayList<String>(1);
			mCharacters = new ArrayList<String>(1);
		}

		private Comic(Parcel in) {
			mIsbn = in.readString();
			mEan = in.readString();
			mInternalId = in.readString();
			mTitle = in.readString();
			mTags = new ArrayList<String>(1);
		}

		public String getDescriptions() {
			return mDescriptions;
		}

		public String getAuthors() {
			if (!TextUtilities.isEmpty(mAuthors))
				return mAuthors;
			else
				return "";
		}

		public Date getPublicationDate() {
			return mPublicationDate;
		}

		public List<String> getCharacters() {
			return mCharacters;
		}

		public List<String> getArtists() {
			return mArtists;
		}

		public String getIssueNumber() {
			return mIssueNumber;
		}

		@Override
		public Bitmap loadCover(ImageSize size) {
			String url = mImages.get(size);

			if (TextUtilities.isEmpty(url)) {
				url = mImages.get(ImageSize.MEDIUM);
				if (TextUtilities.isEmpty(url))
					return null;
			}

			final ImageUtilities.ExpiringBitmap expiring;
			if (mLoader == null) {
				expiring = ImageUtilities.load(url);
			} else {
				expiring = mLoader.load(url);
			}
			mLastModified = expiring.lastModified;

			return expiring.bitmap;
		}

		public ContentValues getContentValues() {
			final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
			final ContentValues values = new ContentValues();

			values.put(INTERNAL_ID, CVInfo.NAME + mInternalId);
			values.put(EAN, mEan);
			values.put(TITLE, mTitle);
			values.put(AUTHORS, mAuthors);
			values.put(REVIEWS, mDescriptions);

			if (mLastModified != null) {
				values.put(LAST_MODIFIED, mLastModified.getTimeInMillis());
			}

			values.put(PUBLICATION,
					mPublicationDate != null ? format.format(mPublicationDate)
							: "");

			values.put(DETAILS_URL, mDetailsUrl);

			final int density = Preferences.getDPI();

			switch (density) {
			case 320:
				values.put(TINY_URL, TextUtilities.protectString(mImages
						.get(ImageSize.LARGE)));
				break;
			case 240:
				values.put(TINY_URL, TextUtilities.protectString(mImages
						.get(ImageSize.MEDIUM)));
				break;
			case 120:
				values.put(TINY_URL, TextUtilities.protectString(mImages
						.get(ImageSize.THUMBNAIL)));
				break;
			case 160:
			default:
				values.put(TINY_URL, TextUtilities.protectString(mImages
						.get(ImageSize.TINY)));
				break;
			}

			values.put(CHARACTERS, TextUtilities.join(mCharacters, ", "));
			values.put(ARTISTS, TextUtilities.join(mArtists, ", "));

			values.put(ISSUE_NUMBER, mIssueNumber);

			values.put(RETAIL_PRICE, mRetailPrice);
			// GJT: Added these, for more data
			values.put(RATING, mRating);
			values.put(LOANED_TO, mLoanedTo);
			if (mLoanDate != null) {
				values.put(LOAN_DATE,
						Preferences.getDateFormat().format(mLoanDate));
			}
			if (mWishlistDate != null) {
				values.put(WISHLIST_DATE,
						Preferences.getDateFormat().format(mWishlistDate));
			}
			values.put(EVENT_ID, mEventId);
			values.put(NOTES, mNotes);

			values.put(QUANTITY, mQuantity);

			return values;
		}

		public static Comic fromCursor(Cursor c) {
			final Comic comic = new Comic();

			comic.mInternalId = c.getString(
					c.getColumnIndexOrThrow(INTERNAL_ID)).substring(
					CVInfo.NAME.length());
			comic.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
			comic.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));

			comic.mAuthors = c.getString(c.getColumnIndexOrThrow(AUTHORS));

			comic.mDescriptions = c.getString(c.getColumnIndexOrThrow(REVIEWS));

			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(c.getLong(c
					.getColumnIndexOrThrow(LAST_MODIFIED)));
			comic.mLastModified = calendar;

			try {
				Collections.addAll(comic.mTags,
						c.getString(c.getColumnIndexOrThrow(TAGS)).split(", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
			try {
				comic.mPublicationDate = format.parse(c.getString(c
						.getColumnIndexOrThrow(PUBLICATION)));
			} catch (ParseException e) {
				// Ignore
			}

			comic.mDetailsUrl = c.getString(c
					.getColumnIndexOrThrow(DETAILS_URL));

			String tiny_url = c.getString(c.getColumnIndexOrThrow(TINY_URL));

			if (tiny_url != null) {
				tiny_url = TextUtilities.unprotectString(c.getString(c
						.getColumnIndexOrThrow(TINY_URL)));

				final int density = Preferences.getDPI();

				switch (density) {
				case 320:
					comic.mImages.put(ImageSize.LARGE, tiny_url);
					break;
				case 240:
					comic.mImages.put(ImageSize.MEDIUM, tiny_url);
					break;
				case 120:
					comic.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					break;
				case 160:
				default:
					comic.mImages.put(ImageSize.TINY, tiny_url);
					break;
				}
			}

			Collections.addAll(comic.mCharacters,
					c.getString(c.getColumnIndexOrThrow(CHARACTERS))
							.split(", "));
			Collections.addAll(comic.mArtists,
					c.getString(c.getColumnIndexOrThrow(ARTISTS)).split(", "));

			comic.mIssueNumber = c.getString(c.getColumnIndex(ISSUE_NUMBER));

			comic.mRetailPrice = c.getString(c
					.getColumnIndexOrThrow(RETAIL_PRICE));
			comic.mRating = c.getInt(c.getColumnIndexOrThrow(RATING));

			comic.mLoanedTo = c.getString(c.getColumnIndex(LOANED_TO));

			String loanDate = c.getString(c.getColumnIndexOrThrow(LOAN_DATE));
			if (loanDate != null) {
				try {
					comic.mLoanDate = Preferences.getDateFormat().parse(
							loanDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			String wishlistDate = c.getString(c
					.getColumnIndexOrThrow(WISHLIST_DATE));
			if (wishlistDate != null) {
				try {
					comic.mWishlistDate = Preferences.getDateFormat().parse(
							wishlistDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			comic.mEventId = c.getInt(c.getColumnIndexOrThrow(EVENT_ID));
			comic.mNotes = c.getString(c.getColumnIndexOrThrow(NOTES));

			comic.mQuantity = c.getString(c.getColumnIndex(QUANTITY));

			return comic;
		}

		@Override
		public String toString() {
			return "Comic[EAN=" + mEan + "]";
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(mEan);
			dest.writeString(mInternalId);
			dest.writeString(mTitle);
			dest.writeStringList(mTags);
		}

		public static final Creator<Comic> CREATOR = new Creator<Comic>() {
			public Comic createFromParcel(Parcel in) {
				return new Comic(in);
			}

			public Comic[] newArray(int size) {
				return new Comic[size];
			}
		};
	}

	public ComicsStore() {
		mStoreName = "CVComics";
		mStoreLabel = "CV";
		mHost = CVInfo.REST_HOST;
		mLoader = new CVImageLoader();
	}

	/**
	 * Finds the comic with the specified id.
	 * 
	 * @param id
	 *            The id of the comic to find (ISBN-10, ISBN-13, etc.)
	 * @param type
	 * 
	 * @return A Comic instance if the comic was found or null otherwise.
	 */
	public Comic findComic(String id,
			final IOUtilities.inputTypes mSavedImportType, Context context) {
		final Uri.Builder uri = CVInfo.buildFindQuery(id);
		final String uriBuild = uri.build().toString();
		android.util.Log.i(LOG_TAG, "Looking up comic #" + id);

		HttpGet get = new HttpGet(uriBuild);
		Comic comic = createComic();
		comic = findComicLookup(get, comic, mSavedImportType, id);

		if (comic != null) {
			return comic;
		} else
			return null;
	}

	private Comic findComicLookup(HttpGet get, final Comic comic,
			final inputTypes mSavedImportType, String id) {
		final boolean[] result = new boolean[1];

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									result[0] = parseComic(parser, comic);
								}
							}, mSavedImportType);
						}
					});
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not find " + mSavedImportType
					+ " item with ID " + id);
		}

		return result[0] ? comic : null;
	}

	/**
	 * Searches for comics that match the provided query.
	 * 
	 * @param query
	 *            The free form query used to search for comics.
	 * 
	 * @return A list of Comic instances if query was successful or null
	 *         otherwise.
	 */
	public ArrayList<Comic> searchComics(String query, String page,
			final ComicSearchListener listener, Context context) {
		final Uri.Builder uri = CVInfo.buildSearchQuery(query, page);

		android.util.Log.i(LOG_TAG, "Looking up comics: " + query
				+ " @offset: " + page);
		final HttpGet get = new HttpGet(uri.build().toString());
		final ArrayList<Comic> comics = new ArrayList<Comic>(10);

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									parseComics(parser, comics, listener);
								}
							}, null);
						}
					});

			return comics;
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform search with query: "
					+ query, e);
		}

		return null;
	}

	/**
	 * Parses a comic from the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the comic.
	 * @param comic
	 *            The comic object to put the parsed data in.
	 * 
	 * @return True if the comic could correctly be parsed, false otherwise.
	 */
	boolean parseComic(XmlPullParser parser, Comic comic)
			throws XmlPullParserException, IOException {

		int type;
		String name;
		final int depth = parser.getDepth();

		String publishMonth = "";
		String publishYear = "";

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			name = parser.getName();

			// getDepth() is to remove bug when trying to add comic
			if (RESPONSE_TAG_ID.equals(name) && parser.getDepth() <= 4) {
				if (parser.next() == XmlPullParser.TEXT) {
					if (comic.mInternalId == null) {
						comic.mInternalId = parser.getText();
						comic.mEan = comic.mInternalId;
					}
				}
			} else if (RESPONSE_TAG_PERSON.equals(name)) {
				comic.mArtists.add(parseArtists(parser));
			} else if (RESPONSE_TAG_IMAGESET.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					final String imgURL = parser.getText();
					comic.mImages.put(ImageSize.THUMBNAIL, imgURL);
					comic.mImages.put(ImageSize.TINY, imgURL);
					comic.mImages.put(ImageSize.MEDIUM, imgURL);
					comic.mImages.put(ImageSize.LARGE, imgURL);
				}
			} else if (RESPONSE_TAG_DESCRIPTION.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					Entities e = new Entities();
					String content = parser.getText();
					comic.mDescriptions = e.unescape(content.replaceAll(
							"\\<.*?>", ""));
				}
			} else if (RESPONSE_TAG_DETAILPAGEURL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					if (TextUtilities.isEmpty(comic.mDetailsUrl)
							|| parser.getDepth() == 3)
						comic.mDetailsUrl = parser.getText();
				}
			} else if (RESPONSE_TAG_MONTHPUBLISHED.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					publishMonth = parser.getText();
				}
			} else if (RESPONSE_TAG_YEARPUBLISHED.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					publishYear = parser.getText();

					final SimpleDateFormat monthAndYearFormat = new SimpleDateFormat(
							"yyyy-MM");
					final SimpleDateFormat yearFormat = new SimpleDateFormat(
							"yyyy");

					try {
						if (!TextUtilities.isEmpty(publishMonth)
								&& !TextUtilities.isEmpty(publishYear))
							comic.mPublicationDate = monthAndYearFormat
									.parse(publishYear + "-" + publishMonth);
						else
							comic.mPublicationDate = yearFormat
									.parse(publishYear);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			} else if (RESPONSE_TAG_CHARACTER.equals(name)) {
				if (TextUtilities.isEmpty(comic.mAuthors)) {
					String[] charactersResult = parseCharacters(parser, true)
							.split("~");
					comic.mCharacters.add(charactersResult[0]);
					comic.mAuthors = charactersResult[1];
				} else {
					comic.mCharacters.add(parseCharacters(parser, false));
				}
			} else if (RESPONSE_TAG_ISSUENUMBER.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					comic.mIssueNumber = parser.getText();
				}
			} else if (RESPONSE_TAG_NAME.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					if (TextUtilities.isEmpty(comic.mTitle)
							|| parser.getDepth() == 3)
						comic.mTitle = parser.getText();
				}
			}
		}

		return true;
	}

	/**
	 * Creates an instance of
	 * {@link com.miadzin.shelves.provider.comics.ComicsStore.Comic} with this
	 * comic store's name.
	 * 
	 * @return A new instance of Comic.
	 */
	Comic createComic() {
		return new Comic(getName(), mLoader);
	}

	private void parseComics(XmlPullParser parser, ArrayList<Comic> comics,
			ComicSearchListener listener) throws IOException,
			XmlPullParserException {

		int type;
		while ((type = parser.next()) != XmlPullParser.END_TAG
				&& type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (findNextItem(parser)) {
				final Comic comic = createComic();
				if (parseComic(parser, comic)) {
					comics.add(comic);
					listener.onComicFound(comic, comics);
				}
			}
		}
	}

	protected String parseArtists(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		String authorName = null;
		List<String> roles = new ArrayList<String>();

		while (true) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				String tag = parser.getName();
				if (RESPONSE_TAG_NAME.equals(tag)) {
					parser.next();
					authorName = parser.getText();

				} else if (RESPONSE_TAG_ROLE.equals(tag)) {
					parser.next();
					roles.add(TextUtilities.capitalizeString(parser.getText()));

				}
			} else if (roles.size() > 0 && eventType == XmlPullParser.END_TAG) {
				break;
			}
		}

		StringBuilder authorWithRoles = new StringBuilder();
		authorWithRoles.append(authorName);
		authorWithRoles.append(" (");
		authorWithRoles.append(TextUtilities.join(roles, ", "));
		authorWithRoles.append(")");

		return authorWithRoles.toString();
	}

	protected String parseCharacters(XmlPullParser parser, boolean getPublisher)
			throws XmlPullParserException, IOException {
		String characterName = null;
		String ID = null;

		while (true) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				String tag = parser.getName();
				if (RESPONSE_TAG_NAME.equals(tag)) {
					parser.next();
					characterName = parser.getText();
				}
				if (RESPONSE_TAG_ID.equals(tag)) {
					parser.next();
					ID = parser.getText();
				}
			} else if (characterName != null
					&& eventType == XmlPullParser.END_TAG) {
				break;
			}
		}

		if (getPublisher)
			return characterName + "~" + sendGetRequest(ID);
		else
			return characterName;
	}

	public static String sendGetRequest(String id) {
		String result = null;

		// Send a GET request to the servlet
		try {
			// Send data
			String urlStr = "http://" + mHost
					+ CVInfo.buildGetPublisherQuery(id).build().toString();

			URL url = new URL(urlStr);
			URLConnection conn = url.openConnection();
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			rd.close();
			result = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] restOfResult = result.split("name\": ");
		String resultName = restOfResult[1].substring(0,
				restOfResult[1].indexOf("}"));
		return resultName.replaceAll("\"", "");
	}

	/**
	 * Interface used to load images with an expiring date. The expiring date is
	 * handled by the image cache to check for updated images from time to time.
	 */
	public interface ImageLoader {
		/**
		 * Load the specified as a Bitmap and associates an expiring date to it.
		 * 
		 * @param url
		 *            The URL of the image to load.
		 * 
		 * @return The Bitmap decoded from the URL and an expiration date.
		 */
        ImageUtilities.ExpiringBitmap load(String url);
	}

	/**
	 * Listener invoked by
	 * {@link com.miadzin.shelves.provider.comics.ComicsStore#searchComics(String, com.miadzin.shelves.provider.comics.ComicsStore.ComicSearchListener)}
	 * .
	 */
	public interface ComicSearchListener {
		/**
		 * Invoked whenever a comic was found by the search operation.
		 * 
		 * @param comic
		 *            The comic yield by the search query.
		 * @param comics
		 *            The comics found so far, including <code>comic</code>.
		 */
		void onComicFound(Comic comic, ArrayList<Comic> comics);
	}
}
