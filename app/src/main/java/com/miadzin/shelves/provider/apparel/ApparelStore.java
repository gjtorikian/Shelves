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

package com.miadzin.shelves.provider.apparel;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
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
 * Utility class to load apparel from a apparel store.
 */
public class ApparelStore extends ServerInfo {
	static final String LOG_TAG = "ApparelStore";

	public static class Apparel extends BaseItem implements Parcelable,
			BaseColumns {
		public static final Uri CONTENT_URI = Uri
				.parse("content://ApparelProvider/apparel");

		String mAuthors;
		List<Description> mDescriptions;
		String mFabric;
		private ServerImageLoader mLoader;

		public static HashMap<ImageSize, String> mImages;

		Apparel() {
			mStorePrefix = ServerInfo.NAME;
			mLoader = new ServerImageLoader();
			mImages = new HashMap<ImageSize, String>(6);
			mDescriptions = new ArrayList<Description>();
			mTags = new ArrayList<String>(1);
			mLanguage = new ArrayList<String>(1);
			mFeatures = new ArrayList<String>(1);
		}

		private Apparel(Parcel in) {
			mIsbn = in.readString();
			mEan = in.readString();
			mInternalId = in.readString();
			mTitle = in.readString();
			mTags = new ArrayList<String>(1);
		}

		public String getAuthors() {
			return mAuthors;
		}

		public List<Description> getDescriptions() {
			return mDescriptions;
		}

		public String getFabric() {
			return mFabric;
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

		@Override
		public String getImageUrl(ImageSize size) {
			String url = mImages.get(size);
			if (TextUtilities.isEmpty(url))
				return null;
			else
				return TextUtilities.unprotectString(url);
		}

		public ContentValues getContentValues() {
			final ContentValues values = new ContentValues();

			values.put(INTERNAL_ID, ServerInfo.NAME + mInternalId);
			values.put(EAN, mEan);
			values.put(ISBN, mIsbn);
			values.put(UPC, mUpc);
			values.put(TITLE, mTitle);
			values.put(AUTHORS, mAuthors);
			values.put(FABRIC, mFabric);
			values.put(DEPARTMENT, mDepartment);
			values.put(REVIEWS, TextUtilities.join(mDescriptions, "\n\n"));
			if (mLastModified != null) {
				values.put(LAST_MODIFIED, mLastModified.getTimeInMillis());
			}
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

		public static Apparel fromCursor(Cursor c) {
			final Apparel apparel = new Apparel();

			apparel.mInternalId = c.getString(
					c.getColumnIndexOrThrow(INTERNAL_ID)).substring(
					ServerInfo.NAME.length());
			apparel.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
			apparel.mIsbn = c.getString(c.getColumnIndexOrThrow(ISBN));
			apparel.mUpc = c.getString(c.getColumnIndexOrThrow(UPC));
			apparel.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));
			apparel.mAuthors = c.getString(c.getColumnIndexOrThrow(AUTHORS));
			apparel.mFabric = c.getString(c.getColumnIndexOrThrow(FABRIC));
			apparel.mDepartment = c.getString(c
					.getColumnIndexOrThrow(DEPARTMENT));
			apparel.mDescriptions.add(new Description("", c.getString(c
					.getColumnIndexOrThrow(REVIEWS))));

			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(c.getLong(c
					.getColumnIndexOrThrow(LAST_MODIFIED)));
			apparel.mLastModified = calendar;

			try {
				Collections.addAll(apparel.mTags,
						c.getString(c.getColumnIndexOrThrow(TAGS)).split(", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			apparel.mDetailsUrl = TextUtilities.unprotectString(c.getString(c
					.getColumnIndexOrThrow(DETAILS_URL)));

			String tiny_url = c.getString(c.getColumnIndexOrThrow(TINY_URL));

			if (tiny_url != null) {
				tiny_url = TextUtilities.unprotectString(c.getString(c
						.getColumnIndexOrThrow(TINY_URL)));

				final int density = Preferences.getDPI();

				switch (density) {
				case 320:
					Apparel.mImages.put(ImageSize.LARGE, tiny_url);
					Apparel.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 240:
					Apparel.mImages.put(ImageSize.MEDIUM, tiny_url);
					Apparel.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 120:
					Apparel.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					Apparel.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					break;
				case 160:
				default:
					Apparel.mImages.put(ImageSize.TINY, tiny_url);
					Apparel.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				}
			}

			// GJT: Added these for more details
			try {
				Collections.addAll(
						apparel.mFeatures,
						c.getString(c.getColumnIndexOrThrow(FEATURES)).split(
								", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			apparel.mRetailPrice = c.getString(c
					.getColumnIndexOrThrow(RETAIL_PRICE));

			apparel.mRating = c.getInt(c.getColumnIndexOrThrow(RATING));

			apparel.mLoanedTo = c.getString(c.getColumnIndex(LOANED_TO));

			String loanDate = c.getString(c.getColumnIndexOrThrow(LOAN_DATE));
			if (loanDate != null) {
				try {
					apparel.mLoanDate = Preferences.getDateFormat().parse(
							loanDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			String wishlistDate = c.getString(c
					.getColumnIndexOrThrow(WISHLIST_DATE));
			if (wishlistDate != null) {
				try {
					apparel.mWishlistDate = Preferences.getDateFormat().parse(
							wishlistDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			apparel.mEventId = c.getInt(c.getColumnIndexOrThrow(EVENT_ID));
			apparel.mNotes = c.getString(c.getColumnIndexOrThrow(NOTES));

			apparel.mQuantity = c.getString(c.getColumnIndex(QUANTITY));

			return apparel;
		}

		@Override
		public String toString() {
			return "Apparel[ISBN=" + mIsbn + ", EAN=" + mEan + ", UPC=" + mUpc
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

		public static final Creator<Apparel> CREATOR = new Creator<Apparel>() {
			public Apparel createFromParcel(Parcel in) {
				return new Apparel(in);
			}

			public Apparel[] newArray(int size) {
				return new Apparel[size];
			}
		};
	}

	/**
	 * Finds the apparel with the specified id.
	 * 
	 * @param id
	 *            The id of the apparel to find (ISBN-10, ISBN-13, etc.)
	 * @param mSavedImportType
	 * 
	 * @return A Apparel instance if the apparel was found or null otherwise.
	 */
	public Apparel findApparel(String id,
			final IOUtilities.inputTypes mSavedImportType, Context context) {
		Uri.Builder uri = assembleURI(id, mSavedImportType, context,
				VALUE_SEARCHINDEX_APPAREL, RESPONSE_TAG_EAN);

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
		Apparel apparel = createApparel();
		apparel = findApparelLookup(get, apparel, mSavedImportType, id);

		if (apparel != null) {
			return apparel;
		}

		apparel = createApparel();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_APPAREL, context,
				RESPONSE_TAG_ASIN);
		get = new HttpGet(uri.build().toString());
		apparel = findApparelLookup(get, apparel, mSavedImportType, id);

		if (apparel != null) {
			return apparel;
		}

		apparel = createApparel();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_APPAREL, context,
				RESPONSE_TAG_UPC);
		get = new HttpGet(uri.build().toString());
		apparel = findApparelLookup(get, apparel, mSavedImportType, id);

		if (apparel != null) {
			return apparel;
		} else {
			return null;
		}
	}

	private Apparel findApparelLookup(HttpGet get, final Apparel apparel,
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
									result[0] = parseApparel(parser, apparel);
								}
							}, mSavedImportType);
						}
					});
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not find " + mSavedImportType
					+ " item with ID " + id);
		}

		if (TextUtilities.isEmpty(apparel.mEan) && id.length() == 13) {
			apparel.mEan = id;
		} else if (TextUtilities.isEmpty(apparel.mIsbn) && id.length() == 10) {
			apparel.mIsbn = id;
		}

		return result[0] ? apparel : null;
	}

	/**
	 * Searchs for apparel that match the provided query.
	 * 
	 * @param query
	 *            The free form query used to search for apparel.
	 * 
	 * @return A list of Apparel instances if query was successful or null
	 *         otherwise.
	 */
	public ArrayList<Apparel> searchApparel(String query, String page,
			final ApparelSearchListener listener, Context context) {
		final Uri.Builder uri = ServerInfo.buildSearchQuery(query,
				VALUE_SEARCHINDEX_APPAREL, page, context);
		final HttpGet get = new HttpGet(uri.build().toString());
		final ArrayList<Apparel> apparel = new ArrayList<Apparel>(10);

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									parseApparel(parser, apparel, listener);
								}
							}, null);
						}
					});

			return apparel;
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform search with query: "
					+ query, e);
		}

		return null;
	}

	/**
	 * Parses a apparel from the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the apparel.
	 * @param apparel
	 *            The apparel object to put the parsed data in.
	 * 
	 * @return True if the apparel could correctly be parsed, false otherwise.
	 */
	boolean parseApparel(XmlPullParser parser, Apparel apparel)
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
					if (apparel.mInternalId == null)
						apparel.mInternalId = parser.getText();
				}
			} else if (RESPONSE_TAG_DETAILPAGEURL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					apparel.mDetailsUrl = parser.getText();
				}
			} else if (RESPONSE_TAG_IMAGESET.equals(name)) {
				if (RESPONSE_VALUE_CATEGORY_PRIMARY.equals(parser
						.getAttributeValue(null, RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, apparel);
				} else if (Apparel.mImages.get(ImageSize.THUMBNAIL) == null
						&& RESPONSE_VALUE_CATEGORY_VARIANT
								.equals(parser.getAttributeValue(null,
										RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, apparel);
				}
			} else if (RESPONSE_TAG_ITEMATTRIBUTES.equals(name)) {
				parseItemAttributes(parser, apparel);
			} else if (RESPONSE_TAG_LOWESTUSEDPRICE.equals(name)) {
				parsePrices(parser, apparel);
			} else if (RESPONSE_TAG_EDITORIALREVIEW.equals(name)) {
				apparel.mDescriptions.add(parseEditorialReview(parser));
			} else if (RESPONSE_TAG_ERRORS.equals(name)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates an instance of
	 * {@link com.miadzin.shelves.provider.apparel.ApparelStore.Apparel} with
	 * this apparel store's name.
	 * 
	 * @return A new instance of Apparel.
	 */
	Apparel createApparel() {
		return new Apparel();
	}

	private void parseApparel(XmlPullParser parser,
			ArrayList<Apparel> apparels, ApparelSearchListener listener)
			throws IOException, XmlPullParserException {

		int type;
		while ((type = parser.next()) != XmlPullParser.END_TAG
				&& type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (findNextItem(parser)) {
				final Apparel apparel = createApparel();
				if (parseApparel(parser, apparel)) {
					apparels.add(apparel);
					listener.onApparelFound(apparel, apparels);
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
	 * {@link com.miadzin.shelves.provider.apparel.ApparelStore#searchApparel(String, com.miadzin.shelves.provider.apparel.ApparelStore.ApparelSearchListener)}
	 * .
	 */
	public interface ApparelSearchListener {
		/**
		 * Invoked whenever a apparel was found by the search operation.
		 * 
		 * @param apparel
		 *            The apparel yield by the search query.
		 * @param apparel
		 *            The apparel found so far, including <code>apparel</code>.
		 */
		void onApparelFound(Apparel apparel, ArrayList<Apparel> apparels);
	}

	private void parseItemAttributes(XmlPullParser parser, Apparel apparel)
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
					apparel.mAuthors = parser.getText();
				}
			} else if (RESPONSE_TAG_ISBN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					apparel.mIsbn = parser.getText();
				}
			} else if (RESPONSE_TAG_UPC.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					apparel.mUpc = parser.getText();
				}
			} else if (RESPONSE_TAG_FABRICTYPE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					apparel.mFabric = parser.getText();
				}
			} else if (RESPONSE_TAG_DEPARTMENT.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					apparel.mDepartment = parser.getText();
				}
			} else if (RESPONSE_TAG_BINDING.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					apparel.mFormat = parser.getText();
				}
			} else if (RESPONSE_TAG_FEATURE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					apparel.mFeatures.add(parser.getText());
				}
			} else if (RESPONSE_TAG_TITLE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					apparel.mTitle = parser.getText();
				}
			} else if (RESPONSE_TAG_LISTPRICE.equals(name)) {
				parsePrices(parser, apparel);
			} else if (RESPONSE_TAG_LABEL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					apparel.mLabel = parser.getText();
				}
			}
		}
	}

	private void parsePrices(XmlPullParser parser, Apparel apparel)
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
						if (apparel.mRetailPrice == null
								|| (apparel.mRetailPrice != null && Double
										.valueOf(apparel.mRetailPrice
												.substring(1)) > Double
										.valueOf(price.substring(1)))) {
							apparel.mRetailPrice = price;
						}
					} catch (NumberFormatException n) {
						Log.e(LOG_TAG, n.toString());
						if (apparel.mRetailPrice == null)
							apparel.mRetailPrice = "";
					}
				}
			}
		}
	}

	private void parseImageSet(XmlPullParser parser, Apparel apparel)
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
				Apparel.mImages.put(ImageSize.TINY, parseImage(parser));
			} else if (RESPONSE_TAG_THUMBNAILIMAGE.equals(name)) {
				Apparel.mImages.put(ImageSize.THUMBNAIL, parseImage(parser));
			} else if (RESPONSE_TAG_MEDIUMIMAGE.equals(name)) {
				Apparel.mImages.put(ImageSize.MEDIUM, parseImage(parser));
			} else if (RESPONSE_TAG_LARGEIMAGE.equals(name)) {
				Apparel.mImages.put(ImageSize.LARGE, parseImage(parser));
			}

		}
	}

}
