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

package com.miadzin.shelves.provider.videogames;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
import android.util.Log;

import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.base.BaseItem.ImageSize;
import com.miadzin.shelves.server.ServerInfo;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.IOUtilities.inputTypes;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;

/**
 * Utility class to load videogames from a videogames store.
 */
public class VideoGamesStore extends ServerInfo {
	static final String LOG_TAG = "VideoGamesStore";

	public static class VideoGame extends BaseItem implements Parcelable,
			BaseColumns {
		public static final Uri CONTENT_URI = Uri
				.parse("content://VideoGamesProvider/videogames");

		String mAuthor;
		String mPlatform;
		List<Description> mDescriptions;
		String mEsrb;

		private ServerImageLoader mLoader;

		public static HashMap<ImageSize, String> mImages;

		@Override
		public String getImageUrl(ImageSize size) {
			String url = mImages.get(size);
			if (TextUtilities.isEmpty(url))
				return null;
			else
				return TextUtilities.unprotectString(url);
		}

		VideoGame() {
			mStorePrefix = ServerInfo.NAME;
			mLoader = new ServerImageLoader();
			mImages = new HashMap<ImageSize, String>(6);
			mDescriptions = new ArrayList<Description>();
			mTags = new ArrayList<String>(1);
			mFeatures = new ArrayList<String>(1);
		}

		private VideoGame(Parcel in) {
			mEsrb = in.readString();
			mEan = in.readString();
			mInternalId = in.readString();
			mTitle = in.readString();
			mTags = new ArrayList<String>(1);
		}

		public String getAuthors() {
			return mAuthor;
		}

		public List<Description> getDescriptions() {
			return mDescriptions;
		}

		public String getPlatform() {
			return mPlatform;
		}

		public String getEsrb() {
			return mEsrb;
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

			values.put(INTERNAL_ID, ServerInfo.NAME + mInternalId);
			values.put(EAN, mEan);
			values.put(UPC, mUpc);
			values.put(ESRB, mEsrb);
			values.put(TITLE, mTitle);
			values.put(AUTHORS, mAuthor);
			values.put(REVIEWS, TextUtilities.join(mDescriptions, "\n\n"));
			if (mLastModified != null) {
				values.put(LAST_MODIFIED, mLastModified.getTimeInMillis());
			}
			values.put(RELEASE_DATE,
					mReleaseDate != null ? format.format(mReleaseDate) : "");
			values.put(DETAILS_URL, TextUtilities.protectString(mDetailsUrl));
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

			values.put(TAGS, TextUtilities.join(mTags, ", "));
			values.put(FORMAT, mFormat);
			values.put(GENRE, mGenre);
			values.put(FEATURES, TextUtilities.join(mFeatures, ", "));
			values.put(RETAIL_PRICE, mRetailPrice);
			values.put(PLATFORM, mPlatform);
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

		public static VideoGame fromCursor(Cursor c) {
			final VideoGame videogame = new VideoGame();

			videogame.mInternalId = c.getString(
					c.getColumnIndexOrThrow(INTERNAL_ID)).substring(
					ServerInfo.NAME.length());
			videogame.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
			videogame.mUpc = c.getString(c.getColumnIndexOrThrow(UPC));
			videogame.mEsrb = c.getString(c.getColumnIndexOrThrow(ESRB));
			videogame.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));
			videogame.mAuthor = c.getString(c.getColumnIndexOrThrow(AUTHORS));
			videogame.mDescriptions.add(new Description("", c.getString(c
					.getColumnIndexOrThrow(REVIEWS))));
			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(c.getLong(c
					.getColumnIndexOrThrow(LAST_MODIFIED)));
			videogame.mLastModified = calendar;
			try {
				Collections.addAll(videogame.mTags,
						c.getString(c.getColumnIndexOrThrow(TAGS)).split(", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
			try {
				videogame.mReleaseDate = format.parse(c.getString(c
						.getColumnIndexOrThrow(RELEASE_DATE)));
			} catch (ParseException e) {
				// Ignore
			}

			videogame.mDetailsUrl = TextUtilities.unprotectString(c.getString(c
					.getColumnIndexOrThrow(DETAILS_URL)));

			String tiny_url = c.getString(c.getColumnIndexOrThrow(TINY_URL));

			if (tiny_url != null) {
				tiny_url = TextUtilities.unprotectString(c.getString(c
						.getColumnIndexOrThrow(TINY_URL)));

				final int density = Preferences.getDPI();

				switch (density) {
				case 320:
					VideoGame.mImages.put(ImageSize.LARGE, tiny_url);
					VideoGame.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 240:
					VideoGame.mImages.put(ImageSize.MEDIUM, tiny_url);
					VideoGame.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 120:
					VideoGame.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					VideoGame.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					break;
				case 160:
				default:
					VideoGame.mImages.put(ImageSize.TINY, tiny_url);
					VideoGame.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				}
			}

			// GJT: Added these for more details
			videogame.mFormat = c.getString(c.getColumnIndexOrThrow(FORMAT));
			videogame.mGenre = c.getString(c.getColumnIndexOrThrow(GENRE));

			try {
				Collections.addAll(
						videogame.mFeatures,
						c.getString(c.getColumnIndexOrThrow(FEATURES)).split(
								", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			videogame.mRetailPrice = c.getString(c
					.getColumnIndexOrThrow(RETAIL_PRICE));
			videogame.mPlatform = c
					.getString(c.getColumnIndexOrThrow(PLATFORM));

			videogame.mRating = c.getInt(c.getColumnIndexOrThrow(RATING));

			videogame.mLoanedTo = c.getString(c.getColumnIndex(LOANED_TO));
			String loanDate = c.getString(c.getColumnIndexOrThrow(LOAN_DATE));
			if (loanDate != null) {
				try {
					videogame.mLoanDate = Preferences.getDateFormat().parse(
							loanDate);
				} catch (ParseException e) {
					// Ignore
				}
			}
			String wishlistDate = c.getString(c
					.getColumnIndexOrThrow(WISHLIST_DATE));
			if (wishlistDate != null) {
				try {
					videogame.mWishlistDate = Preferences.getDateFormat()
							.parse(wishlistDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			videogame.mEventId = c.getInt(c.getColumnIndexOrThrow(EVENT_ID));
			videogame.mNotes = c.getString(c.getColumnIndexOrThrow(NOTES));

			videogame.mQuantity = c.getString(c.getColumnIndex(QUANTITY));

			return videogame;
		}

		@Override
		public String toString() {
			return "VideoGame[EAN=" + mEan + ", UPC=" + mUpc + ", IID="
					+ mInternalId + "]";
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(mEsrb);
			dest.writeString(mEan);
			dest.writeString(mInternalId);
			dest.writeString(mTitle);

			dest.writeStringList(mTags);
		}

		public static final Creator<VideoGame> CREATOR = new Creator<VideoGame>() {
			public VideoGame createFromParcel(Parcel in) {
				return new VideoGame(in);
			}

			public VideoGame[] newArray(int size) {
				return new VideoGame[size];
			}
		};
	}

	/**
	 * Finds the videogame with the specified id.
	 * 
	 * @param id
	 *            The id of the videogame to find (ISBN-10, ISBN-13, etc.)
	 * @param mSavedImportType
	 * 
	 * @return A VideoGame instance if the videogame was found or null
	 *         otherwise.
	 */
	public VideoGame findVideoGame(String id,
			final IOUtilities.inputTypes mSavedImportType, Context context) {
		switch (id.length()) {
		case 10:
			android.util.Log.i(LOG_TAG, "Looking up ID: " + id);
			break;
		case 13:
			android.util.Log.i(LOG_TAG, "Looking up EAN: " + id);
			break;
		case 11:
			id = "00" + id;
			android.util.Log.i(LOG_TAG, "Looking up ID x2: " + id);
			break;
		default:
			android.util.Log.i(LOG_TAG, "Looking up ID: " + id);
			break;
		}

		Uri.Builder uri = assembleURI(id, mSavedImportType, context,
				VALUE_SEARCHINDEX_VIDEOGAMES, RESPONSE_TAG_EAN);

		HttpGet get = new HttpGet(uri.build().toString());
		VideoGame videogames = createVideoGame();
		videogames = findVideoGameLookup(get, videogames, mSavedImportType, id);

		if (videogames != null) {
			return videogames;
		}

		videogames = createVideoGame();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_VIDEOGAMES, context,
				RESPONSE_TAG_ASIN);
		get = new HttpGet(uri.build().toString());
		videogames = findVideoGameLookup(get, videogames, mSavedImportType, id);

		if (videogames != null) {
			return videogames;
		}

		videogames = createVideoGame();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_VIDEOGAMES, context,
				RESPONSE_TAG_UPC);
		get = new HttpGet(uri.build().toString());
		videogames = findVideoGameLookup(get, videogames, mSavedImportType, id);

		if (videogames != null) {
			return videogames;
		} else {
			return null;
		}
	}

	private VideoGame findVideoGameLookup(HttpGet get,
			final VideoGame videogame, final inputTypes mSavedImportType,
			String id) {
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
									result[0] = parseVideoGame(parser,
											videogame);
								}
							}, mSavedImportType);
						}
					});
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not find " + mSavedImportType
					+ " item with ID " + id);
		}

		if (TextUtilities.isEmpty(videogame.mEan) && id.length() == 13) {
			videogame.mEan = id;
		} else if (TextUtilities.isEmpty(videogame.mIsbn) && id.length() == 10) {
			videogame.mIsbn = id;
		}

		return result[0] ? videogame : null;
	}

	/**
	 * Searchs for videogames that match the provided query.
	 * 
	 * @param query
	 *            The free form query used to search for videogames.
	 * 
	 * @return A list of VideoGame instances if query was successful or null
	 *         otherwise.
	 */
	public ArrayList<VideoGame> searchVideoGames(String query, String page,
			final VideoGameSearchListener listener, Context context) {
		final Uri.Builder uri = buildSearchQuery(query,
				VALUE_SEARCHINDEX_VIDEOGAMES, page, context);
		final HttpGet get = new HttpGet(uri.build().toString());
		final ArrayList<VideoGame> videogames = new ArrayList<VideoGame>(10);

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									parseVideoGames(parser, videogames,
											listener);
								}
							}, null);
						}
					});

			return videogames;
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform search with query: "
					+ query, e);
		}

		return null;
	}

	/**
	 * Parses a videogame from the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the videogame.
	 * @param videogame
	 *            The videogame object to put the parsed data in.
	 * 
	 * @return True if the videogame could correctly be parsed, false otherwise.
	 */
	boolean parseVideoGame(XmlPullParser parser, VideoGame videogame)
			throws XmlPullParserException, IOException {

		int type;
		String name;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			name = parser.getName();

			if (RESPONSE_TAG_ASIN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					// GJT: Fixes issues with
					// <SimilarProduct><ASIN/><SimilarProduct>--gets an
					// additional, incorrect ASIN !
					if (videogame.mInternalId == null)
						videogame.mInternalId = parser.getText();
				}
			} else if (RESPONSE_TAG_DETAILPAGEURL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mDetailsUrl = parser.getText();
				}
			} else if (RESPONSE_TAG_IMAGESET.equals(name)) {
				if (RESPONSE_VALUE_CATEGORY_PRIMARY.equals(parser
						.getAttributeValue(null, RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, videogame);
				} else if (VideoGame.mImages.get(ImageSize.THUMBNAIL) == null
						&& RESPONSE_VALUE_CATEGORY_VARIANT
								.equals(parser.getAttributeValue(null,
										RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, videogame);
				}
			} else if (RESPONSE_TAG_ITEMATTRIBUTES.equals(name)) {
				parseItemAttributes(parser, videogame);
			} else if (RESPONSE_TAG_LOWESTUSEDPRICE.equals(name)) {
				parsePrices(parser, videogame);
			} else if (RESPONSE_TAG_EDITORIALREVIEW.equals(name)) {
				videogame.mDescriptions.add(parseEditorialReview(parser));
			} else if (RESPONSE_TAG_ERRORS.equals(name)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates an instance of {@link VideoGamesStore.VideoGame} with this
	 * videogame store's name.
	 * 
	 * @return A new instance of VideoGame.
	 */
	VideoGame createVideoGame() {
		return new VideoGame();
	}

	private void parseVideoGames(XmlPullParser parser,
			ArrayList<VideoGame> videogames, VideoGameSearchListener listener)
			throws IOException, XmlPullParserException {

		int type;
		while ((type = parser.next()) != XmlPullParser.END_TAG
				&& type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (findNextItem(parser)) {
				final VideoGame videogame = createVideoGame();
				if (parseVideoGame(parser, videogame)) {
					videogames.add(videogame);
					listener.onVideoGameFound(videogame, videogames);
				}
			}
		}
	}

	/**
	 * Interface used to load images with an expiring date. The expiring date is
	 * handled by the image cache to check for updated images from time to time.
	 */
    interface ImageLoader {
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
	 * {@link VideoGamesStore#searchVideoGames(String, VideoGamesStore.VideoGameSearchListener)}
	 * .
	 */
	public interface VideoGameSearchListener {
		/**
		 * Invoked whenever a videogame was found by the search operation.
		 * 
		 * @param videogame
		 *            The videogame yield by the search query.
		 * @param videogames
		 *            The videogames found so far, including
		 *            <code>videogame</code>.
		 */
		void onVideoGameFound(VideoGame videogame,
				ArrayList<VideoGame> videogames);
	}

	private void parseItemAttributes(XmlPullParser parser, VideoGame videogame)
			throws IOException, XmlPullParserException {

		int type;
		String name;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			name = parser.getName();

			if (RESPONSE_TAG_EAN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mEan = parser.getText();
				}
			} else if (RESPONSE_TAG_UPC.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mUpc = parser.getText();
				}
			} else if (RESPONSE_TAG_ESRBAGERATING.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mEsrb = parser.getText();
				}
			} else if (RESPONSE_TAG_BINDING.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mFormat = parser.getText();
				}
			} else if (RESPONSE_TAG_FEATURES.equals(name)
					|| RESPONSE_TAG_SPECIALFEATURES.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mFeatures.add(parser.getText());
				}
			} else if (RESPONSE_TAG_RELEASEDATE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					final SimpleDateFormat format = new SimpleDateFormat(
							"yyyy-MM-dd");
					try {
						videogame.mReleaseDate = format.parse(parser.getText());
					} catch (ParseException e) {
						// Ignore
					}
				}
			} else if (RESPONSE_TAG_HARDWAREPLATFORM.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mPlatform = parser.getText();
				}
			} else if (RESPONSE_TAG_TITLE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mTitle = parser.getText();
				}
			} else if (RESPONSE_TAG_BRAND.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mAuthor = parser.getText();
				}
			} else if (RESPONSE_TAG_GENRE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					videogame.mGenre = parser.getText();
				}
			}
		}
	}

	private void parsePrices(XmlPullParser parser, VideoGame videogame)
			throws IOException, XmlPullParserException {

		int type;
		String name;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			name = parser.getName();
			if (RESPONSE_TAG_FORMATTEDPRICE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					String price = parser.getText();

					try {
						if (videogame.mRetailPrice == null
								|| (videogame.mRetailPrice != null && Double
										.valueOf(videogame.mRetailPrice
												.substring(1)) > Double
										.valueOf(price.substring(1)))) {
							videogame.mRetailPrice = price;
						}
					} catch (NumberFormatException n) {
						Log.e(LOG_TAG, n.toString());
						if (videogame.mRetailPrice == null)
							videogame.mRetailPrice = "";
					}
				}
			}
		}
	}

	private void parseImageSet(XmlPullParser parser, VideoGame videogame)
			throws IOException, XmlPullParserException {

		int type;
		String name;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			name = parser.getName();
			if (RESPONSE_TAG_TINYIMAGE.equals(name)) {
				VideoGame.mImages.put(ImageSize.TINY, parseImage(parser));
			} else if (RESPONSE_TAG_THUMBNAILIMAGE.equals(name)) {
				VideoGame.mImages.put(ImageSize.THUMBNAIL, parseImage(parser));
			} else if (RESPONSE_TAG_MEDIUMIMAGE.equals(name)) {
				VideoGame.mImages.put(ImageSize.MEDIUM, parseImage(parser));
			} else if (RESPONSE_TAG_LARGEIMAGE.equals(name)) {
				VideoGame.mImages.put(ImageSize.LARGE, parseImage(parser));
			}
		}
	}
}
