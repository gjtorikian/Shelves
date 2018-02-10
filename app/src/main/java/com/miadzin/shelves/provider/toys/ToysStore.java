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

package com.miadzin.shelves.provider.toys;

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
 * Utility class to load toys from a toys store.
 */
public class ToysStore extends ServerInfo {
	static final String LOG_TAG = "ToysStore";

	public static class Toy extends BaseItem implements Parcelable, BaseColumns {
		public static final Uri CONTENT_URI = Uri
				.parse("content://ToysProvider/toys");

		String mAuthors;
		List<Description> mDescriptions;
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

		Toy() {
			mStorePrefix = ServerInfo.NAME;
			mLoader = new ServerImageLoader();
			mImages = new HashMap<ImageSize, String>(6);
			mDescriptions = new ArrayList<Description>();
			mTags = new ArrayList<String>(1);
			mLanguage = new ArrayList<String>(1);
			mFeatures = new ArrayList<String>(1);
		}

		private Toy(Parcel in) {
			mIsbn = in.readString();
			mEan = in.readString();
			mInternalId = in.readString();
			mTitle = in.readString();
			mAuthors = in.readString();
			mTags = new ArrayList<String>(1);
		}

		public String getAuthors() {
			return mAuthors;
		}

		public List<Description> getDescriptions() {
			return mDescriptions;
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
			values.put(ISBN, mIsbn);
			values.put(UPC, mUpc);
			values.put(TITLE, mTitle);
			values.put(AUTHORS, mAuthors);
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
			values.put(FEATURES, TextUtilities.join(mFeatures, ", "));
			values.put(RETAIL_PRICE, mRetailPrice);
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

		public static Toy fromCursor(Cursor c) {
			final Toy toy = new Toy();

			toy.mInternalId = c.getString(c.getColumnIndexOrThrow(INTERNAL_ID))
					.substring(ServerInfo.NAME.length());
			toy.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
			toy.mIsbn = c.getString(c.getColumnIndexOrThrow(ISBN));
			toy.mUpc = c.getString(c.getColumnIndexOrThrow(UPC));
			toy.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));
			toy.mAuthors = c.getString(c.getColumnIndexOrThrow(AUTHORS));
			toy.mDescriptions.add(new Description("", c.getString(c
					.getColumnIndexOrThrow(REVIEWS))));

			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(c.getLong(c
					.getColumnIndexOrThrow(LAST_MODIFIED)));
			toy.mLastModified = calendar;
			try {
				Collections.addAll(toy.mTags,
						c.getString(c.getColumnIndexOrThrow(TAGS)).split(", "));
				Collections.addAll(
						toy.mFeatures,
						c.getString(c.getColumnIndexOrThrow(FEATURES)).split(
								", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
			try {
				toy.mReleaseDate = format.parse(c.getString(c
						.getColumnIndexOrThrow(RELEASE_DATE)));
			} catch (ParseException e) {
				// Ignore
			}

			toy.mDetailsUrl = TextUtilities.unprotectString(c.getString(c
					.getColumnIndexOrThrow(DETAILS_URL)));

			String tiny_url = c.getString(c.getColumnIndexOrThrow(TINY_URL));

			if (tiny_url != null) {
				tiny_url = TextUtilities.unprotectString(c.getString(c
						.getColumnIndexOrThrow(TINY_URL)));

				final int density = Preferences.getDPI();

				switch (density) {
				case 320:
					Toy.mImages.put(ImageSize.LARGE, tiny_url);
					Toy.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 240:
					Toy.mImages.put(ImageSize.MEDIUM, tiny_url);
					Toy.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 120:
					Toy.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					Toy.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					break;
				case 160:
				default:
					Toy.mImages.put(ImageSize.TINY, tiny_url);
					Toy.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				}
			}

			toy.mRetailPrice = c.getString(c
					.getColumnIndexOrThrow(RETAIL_PRICE));
			toy.mRating = c.getInt(c.getColumnIndexOrThrow(RATING));

			toy.mLoanedTo = c.getString(c.getColumnIndex(LOANED_TO));
			String loanDate = c.getString(c.getColumnIndexOrThrow(LOAN_DATE));
			if (loanDate != null) {
				try {
					toy.mLoanDate = Preferences.getDateFormat().parse(loanDate);
				} catch (ParseException e) {
					// Ignore
				}
			}
			String wishlistDate = c.getString(c
					.getColumnIndexOrThrow(WISHLIST_DATE));
			if (wishlistDate != null) {
				try {
					toy.mWishlistDate = Preferences.getDateFormat().parse(
							wishlistDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			toy.mEventId = c.getInt(c.getColumnIndexOrThrow(EVENT_ID));
			toy.mNotes = c.getString(c.getColumnIndexOrThrow(NOTES));

			toy.mQuantity = c.getString(c.getColumnIndex(QUANTITY));

			return toy;
		}

		@Override
		public String toString() {
			return "Toy[ISBN=" + mIsbn + ", EAN=" + mEan + ", UPC=" + mUpc
					+ ", IID=" + mInternalId + "]";
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(mIsbn);
			dest.writeString(mEan);
			dest.writeString(mInternalId);
			dest.writeString(mTitle);
			dest.writeString(mAuthors);
			dest.writeStringList(mTags);
		}

		public static final Creator<Toy> CREATOR = new Creator<Toy>() {
			public Toy createFromParcel(Parcel in) {
				return new Toy(in);
			}

			public Toy[] newArray(int size) {
				return new Toy[size];
			}
		};
	}

	/**
	 * Finds the toy with the specified id.
	 * 
	 * @param id
	 *            The id of the toy to find (ISBN-10, ISBN-13, etc.)
	 * @param mSavedImportType
	 * 
	 * @return A Toy instance if the toy was found or null otherwise.
	 */
	public Toy findToy(String id,
			final IOUtilities.inputTypes mSavedImportType, Context context) {
		Uri.Builder uri = assembleURI(id, mSavedImportType, context,
				VALUE_SEARCHINDEX_TOYS, RESPONSE_TAG_EAN);

		switch (id.length()) {
		case 10:
			android.util.Log.i(LOG_TAG, "Looking up ISBN: " + id);
			break;
		case 13:
			android.util.Log.i(LOG_TAG, "Looking up EAN: " + id);
			break;
		default:
			android.util.Log.i(LOG_TAG, "Looking up ID: " + id);
			break;
		}

		HttpGet get = new HttpGet(uri.build().toString());
		Toy toy = createToy();
		toy = findToyLookup(get, toy, mSavedImportType, id);

		if (toy != null) {
			return toy;
		}

		toy = createToy();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_TOYS, context,
				RESPONSE_TAG_ASIN);
		get = new HttpGet(uri.build().toString());
		toy = findToyLookup(get, toy, mSavedImportType, id);

		if (toy != null) {
			return toy;
		}

		toy = createToy();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_TOYS, context,
				RESPONSE_TAG_UPC);
		get = new HttpGet(uri.build().toString());
		toy = findToyLookup(get, toy, mSavedImportType, id);

		if (toy != null) {
			return toy;
		} else {
			return null;
		}
	}

	private Toy findToyLookup(HttpGet get, final Toy toy,
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
									result[0] = parseToy(parser, toy);
								}
							}, mSavedImportType);
						}
					});
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not find " + mSavedImportType
					+ " item with ID " + id);
		}

		if (TextUtilities.isEmpty(toy.mEan) && id.length() == 13) {
			toy.mEan = id;
		} else if (TextUtilities.isEmpty(toy.mIsbn) && id.length() == 10) {
			toy.mIsbn = id;
		}

		return result[0] ? toy : null;
	}

	/**
	 * Searchs for toys that match the provided query.
	 * 
	 * @param query
	 *            The free form query used to search for toys.
	 * 
	 * @return A list of Toy instances if query was successful or null
	 *         otherwise.
	 */
	public ArrayList<Toy> searchToys(String query, String page,
			final ToySearchListener listener, Context context) {
		final Uri.Builder uri = buildSearchQuery(query, VALUE_SEARCHINDEX_TOYS,
				page, context);
		final HttpGet get = new HttpGet(uri.build().toString());
		final ArrayList<Toy> toys = new ArrayList<Toy>(10);

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									parseToys(parser, toys, listener);
								}
							}, null);
						}
					});

			return toys;
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform search with query: "
					+ query, e);
		}

		return null;
	}

	/**
	 * Parses a toy from the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the toy.
	 * @param toy
	 *            The toy object to put the parsed data in.
	 * 
	 * @return True if the toy could correctly be parsed, false otherwise.
	 */
	boolean parseToy(XmlPullParser parser, Toy toy)
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
					if (toy.mInternalId == null)
						toy.mInternalId = parser.getText();
				}
			} else if (RESPONSE_TAG_DETAILPAGEURL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mDetailsUrl = parser.getText();
				}
			} else if (RESPONSE_TAG_IMAGESET.equals(name)) {
				if (RESPONSE_VALUE_CATEGORY_PRIMARY.equals(parser
						.getAttributeValue(null, RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, toy);
				} else if (Toy.mImages.get(ImageSize.THUMBNAIL) == null
						&& RESPONSE_VALUE_CATEGORY_VARIANT
								.equals(parser.getAttributeValue(null,
										RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, toy);
				}
			} else if (RESPONSE_TAG_ITEMATTRIBUTES.equals(name)) {
				parseItemAttributes(parser, toy);
			} else if (RESPONSE_TAG_LOWESTUSEDPRICE.equals(name)) {
				parsePrices(parser, toy);
			} else if (RESPONSE_TAG_EDITORIALREVIEW.equals(name)) {
				toy.mDescriptions.add(parseEditorialReview(parser));
			} else if (RESPONSE_TAG_ERRORS.equals(name)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates an instance of
	 * {@link com.miadzin.shelves.provider.toys.ToysStore.Toy} with this toy
	 * store's name.
	 * 
	 * @return A new instance of Toy.
	 */
	Toy createToy() {
		return new Toy();
	}

	private void parseToys(XmlPullParser parser, ArrayList<Toy> toys,
			ToySearchListener listener) throws IOException,
			XmlPullParserException {

		int type;
		while ((type = parser.next()) != XmlPullParser.END_TAG
				&& type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (findNextItem(parser)) {
				final Toy toy = createToy();
				if (parseToy(parser, toy)) {
					toys.add(toy);
					listener.onToyFound(toy, toys);
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
	 * {@link com.miadzin.shelves.provider.toys.ToysStore#searchToys(String, com.miadzin.shelves.provider.toys.ToysStore.ToySearchListener)}
	 * .
	 */
	public interface ToySearchListener {
		/**
		 * Invoked whenever a toy was found by the search operation.
		 * 
		 * @param toy
		 *            The toy yield by the search query.
		 * @param toys
		 *            The toys found so far, including <code>toy</code>.
		 */
		void onToyFound(Toy toy, ArrayList<Toy> toys);
	}

	private void parseItemAttributes(XmlPullParser parser, Toy toy)
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
			if (RESPONSE_TAG_MANUFACTURER.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mAuthors = parser.getText();
				}
			} else if (RESPONSE_TAG_AUDIENCE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mAudience = parser.getText();
				}
			} else if (RESPONSE_TAG_EAN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mEan = parser.getText();
				}
			} else if (RESPONSE_TAG_ISBN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mIsbn = parser.getText();
				}
			} else if (RESPONSE_TAG_UPC.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mUpc = parser.getText();
				}
			} else if (RESPONSE_TAG_BINDING.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mFormat = parser.getText();
				}
			} else if (RESPONSE_TAG_FEATURE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mFeatures.add(parser.getText());
				}
			} else if (RESPONSE_TAG_LISTPRICE.equals(name)) {
				parsePrices(parser, toy);
			} else if (RESPONSE_TAG_RELEASEDATE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					final SimpleDateFormat format = new SimpleDateFormat(
							"yyyy-MM-dd");
					try {
						toy.mReleaseDate = format.parse(parser.getText());
					} catch (ParseException e) {
						// Ignore
					}
				}
			} else if (RESPONSE_TAG_TITLE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mTitle = parser.getText();
				}
			} else if (RESPONSE_TAG_LABEL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					toy.mLabel = parser.getText();
				}
			} else if (RESPONSE_TAG_LANGUAGE.equals(name)) {
				toy.mLanguage.add(parseLanguage(parser));
			}
		}
	}

	private void parsePrices(XmlPullParser parser, Toy toy) throws IOException,
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
			if (RESPONSE_TAG_FORMATTEDPRICE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					String price = parser.getText();

					try {
						if (toy.mRetailPrice == null
								|| (toy.mRetailPrice != null && Double
										.valueOf(toy.mRetailPrice.substring(1)) > Double
										.valueOf(price.substring(1)))) {
							toy.mRetailPrice = price;
						}
					} catch (NumberFormatException n) {
						Log.e(LOG_TAG, n.toString());
						if (toy.mRetailPrice == null)
							toy.mRetailPrice = "";
					}
				}
			}
		}
	}

	private void parseImageSet(XmlPullParser parser, Toy toy)
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
				Toy.mImages.put(ImageSize.TINY, parseImage(parser));
			} else if (RESPONSE_TAG_THUMBNAILIMAGE.equals(name)) {
				Toy.mImages.put(ImageSize.THUMBNAIL, parseImage(parser));
			} else if (RESPONSE_TAG_MEDIUMIMAGE.equals(name)) {
				Toy.mImages.put(ImageSize.MEDIUM, parseImage(parser));
			} else if (RESPONSE_TAG_LARGEIMAGE.equals(name)) {
				Toy.mImages.put(ImageSize.LARGE, parseImage(parser));
			}
		}
	}

}
