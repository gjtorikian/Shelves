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

package com.miadzin.shelves.provider.software;

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
 * Utility class to load software from a software store.
 */
public class SoftwareStore extends ServerInfo {
	static final String LOG_TAG = "SoftwareStore";

	public static class Software extends BaseItem implements Parcelable,
			BaseColumns {
		public static final Uri CONTENT_URI = Uri
				.parse("content://SoftwareProvider/software");

		String mAuthor;
		List<Description> mDescriptions;
		protected List<String> mPlatform;

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

		Software() {
			mStorePrefix = ServerInfo.NAME;
			mLoader = new ServerImageLoader();
			mImages = new HashMap<ImageSize, String>(6);
			mPlatform = new ArrayList<String>(1);
			mDescriptions = new ArrayList<Description>();
			mTags = new ArrayList<String>(1);
		}

		private Software(Parcel in) {
			mIsbn = in.readString();
			mEan = in.readString();
			mInternalId = in.readString();
			mTitle = in.readString();
			mPlatform = new ArrayList<String>(1);
			mTags = new ArrayList<String>(1);
		}

		public String getAuthors() {
			return mAuthor;
		}

		public List<Description> getDescriptions() {
			return mDescriptions;
		}

		public List<String> getPlatform() {
			return mPlatform;
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
			values.put(AUTHORS, mAuthor);
			values.put(LABEL, mLabel);
			values.put(PLATFORM, TextUtilities.join(mPlatform, ", "));
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
						.get(ImageSize.MEDIUM)));
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

		public static Software fromCursor(Cursor c) {
			final Software software = new Software();

			software.mInternalId = c.getString(
					c.getColumnIndexOrThrow(INTERNAL_ID)).substring(
					ServerInfo.NAME.length());
			software.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
			software.mIsbn = c.getString(c.getColumnIndexOrThrow(ISBN));
			software.mUpc = c.getString(c.getColumnIndexOrThrow(UPC));
			software.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));
			software.mAuthor = c.getString(c.getColumnIndexOrThrow(AUTHORS));
			Collections.addAll(software.mPlatform,
					c.getString(c.getColumnIndexOrThrow(PLATFORM)).split(", "));
			software.mLabel = c.getString(c.getColumnIndexOrThrow(LABEL));
			software.mDescriptions.add(new Description("", c.getString(c
					.getColumnIndexOrThrow(REVIEWS))));

			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(c.getLong(c
					.getColumnIndexOrThrow(LAST_MODIFIED)));
			software.mLastModified = calendar;
			try {
				Collections.addAll(software.mTags,
						c.getString(c.getColumnIndexOrThrow(TAGS)).split(", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
			try {
				software.mReleaseDate = format.parse(c.getString(c
						.getColumnIndexOrThrow(RELEASE_DATE)));
			} catch (ParseException e) {
				// Ignore
			}

			software.mDetailsUrl = TextUtilities.unprotectString(c.getString(c
					.getColumnIndexOrThrow(DETAILS_URL)));

			String tiny_url = c.getString(c.getColumnIndexOrThrow(TINY_URL));

			if (tiny_url != null) {
				tiny_url = TextUtilities.unprotectString(c.getString(c
						.getColumnIndexOrThrow(TINY_URL)));

				final int density = Preferences.getDPI();

				switch (density) {
				case 430:
					Software.mImages.put(ImageSize.LARGE, tiny_url);
					Software.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 240:
					Software.mImages.put(ImageSize.MEDIUM, tiny_url);
					Software.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 120:
					Software.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					Software.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					break;
				case 160:
				default:
					Software.mImages.put(ImageSize.TINY, tiny_url);
					Software.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				}
			}

			software.mFormat = c.getString(c.getColumnIndexOrThrow(FORMAT));
			software.mRetailPrice = c.getString(c
					.getColumnIndexOrThrow(RETAIL_PRICE));
			software.mRating = c.getInt(c.getColumnIndexOrThrow(RATING));

			software.mLoanedTo = c.getString(c.getColumnIndex(LOANED_TO));
			String loanDate = c.getString(c.getColumnIndexOrThrow(LOAN_DATE));
			if (loanDate != null) {
				try {
					software.mLoanDate = Preferences.getDateFormat().parse(
							loanDate);
				} catch (ParseException e) {
					// Ignore
				}
			}
			String wishlistDate = c.getString(c
					.getColumnIndexOrThrow(WISHLIST_DATE));
			if (wishlistDate != null) {
				try {
					software.mWishlistDate = Preferences.getDateFormat().parse(
							wishlistDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			software.mEventId = c.getInt(c.getColumnIndexOrThrow(EVENT_ID));
			software.mNotes = c.getString(c.getColumnIndexOrThrow(NOTES));

			software.mQuantity = c.getString(c.getColumnIndex(QUANTITY));

			return software;
		}

		@Override
		public String toString() {
			return "Software[ISBN=" + mIsbn + ", EAN=" + mEan + ", UPC=" + mUpc
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
			dest.writeStringList(mPlatform);
			dest.writeStringList(mTags);
		}

		public static final Creator<Software> CREATOR = new Creator<Software>() {
			public Software createFromParcel(Parcel in) {
				return new Software(in);
			}

			public Software[] newArray(int size) {
				return new Software[size];
			}
		};
	}

	/**
	 * Finds the software with the specified id.
	 * 
	 * @param id
	 *            The id of the software to find (ISBN-10, ISBN-13, etc.)
	 * @param mSavedImportType
	 * 
	 * @return A Software instance if the software was found or null otherwise.
	 */
	public Software findSoftware(String id,
			final IOUtilities.inputTypes mSavedImportType, Context context) {
		Uri.Builder uri = assembleURI(id, mSavedImportType, context,
				VALUE_SEARCHINDEX_SOFTWARE, RESPONSE_TAG_EAN);

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
		Software software = createSoftware();
		software = findSoftwareLookup(get, software, mSavedImportType, id);

		if (software != null) {
			return software;
		}

		software = createSoftware();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_SOFTWARE, context,
				RESPONSE_TAG_ASIN);
		get = new HttpGet(uri.build().toString());
		software = findSoftwareLookup(get, software, mSavedImportType, id);

		if (software != null) {
			return software;
		}

		software = createSoftware();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_SOFTWARE, context,
				RESPONSE_TAG_UPC);
		get = new HttpGet(uri.build().toString());
		software = findSoftwareLookup(get, software, mSavedImportType, id);

		if (software != null) {
			return software;
		} else {
			return null;
		}
	}

	private Software findSoftwareLookup(HttpGet get, final Software software,
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
									result[0] = parseSoftware(parser, software);
								}
							}, mSavedImportType);
						}
					});
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not find " + mSavedImportType
					+ " item with ID " + id);
		}

		if (TextUtilities.isEmpty(software.mEan) && id.length() == 13) {
			software.mEan = id;
		} else if (TextUtilities.isEmpty(software.mIsbn) && id.length() == 10) {
			software.mIsbn = id;
		}

		return result[0] ? software : null;
	}

	/**
	 * Searchs for software that match the provided query.
	 * 
	 * @param query
	 *            The free form query used to search for software.
	 * 
	 * @return A list of Software instances if query was successful or null
	 *         otherwise.
	 */
	public ArrayList<Software> searchSoftware(String query, String page,
			final SoftwareSearchListener listener, Context context) {
		final Uri.Builder uri = buildSearchQuery(query,
				VALUE_SEARCHINDEX_SOFTWARE, page, context);
		final HttpGet get = new HttpGet(uri.build().toString());
		final ArrayList<Software> software = new ArrayList<Software>(10);

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									parseSoftware(parser, software, listener);
								}
							}, null);
						}
					});

			return software;
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform search with query: "
					+ query, e);
		}

		return null;
	}

	/**
	 * Parses a software from the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the software.
	 * @param software
	 *            The software object to put the parsed data in.
	 * 
	 * @return True if the software could correctly be parsed, false otherwise.
	 */
	boolean parseSoftware(XmlPullParser parser, Software software)
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
					if (software.mInternalId == null)
						software.mInternalId = parser.getText();
				}
			} else if (RESPONSE_TAG_DETAILPAGEURL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					software.mDetailsUrl = parser.getText();
				}
			} else if (RESPONSE_TAG_IMAGESET.equals(name)) {
				if (RESPONSE_VALUE_CATEGORY_PRIMARY.equals(parser
						.getAttributeValue(null, RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, software);
				} else if (Software.mImages.get(ImageSize.THUMBNAIL) == null
						&& RESPONSE_VALUE_CATEGORY_VARIANT
								.equals(parser.getAttributeValue(null,
										RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, software);
				}
			} else if (RESPONSE_TAG_ITEMATTRIBUTES.equals(name)) {
				parseItemAttributes(parser, software);
			} else if (RESPONSE_TAG_LOWESTUSEDPRICE.equals(name)) {
				parsePrices(parser, software);
			} else if (RESPONSE_TAG_EDITORIALREVIEW.equals(name)) {
				software.mDescriptions.add(parseEditorialReview(parser));
			} else if (RESPONSE_TAG_ERRORS.equals(name)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates an instance of
	 * {@link com.miadzin.shelves.provider.software.SoftwareStore.Software} with
	 * this software store's name.
	 * 
	 * @return A new instance of Software.
	 */
	Software createSoftware() {
		return new Software();
	}

	private void parseSoftware(XmlPullParser parser,
			ArrayList<Software> softwares, SoftwareSearchListener listener)
			throws IOException, XmlPullParserException {

		int type;
		while ((type = parser.next()) != XmlPullParser.END_TAG
				&& type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (findNextItem(parser)) {
				final Software software = createSoftware();
				if (parseSoftware(parser, software)) {
					softwares.add(software);
					listener.onSoftwareFound(software, softwares);
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
	 * {@link com.miadzin.shelves.provider.software.SoftwareStore#searchSoftware(String, com.miadzin.shelves.provider.software.SoftwareStore.SoftwareSearchListener)}
	 * .
	 */
	public interface SoftwareSearchListener {
		/**
		 * Invoked whenever a software was found by the search operation.
		 * 
		 * @param software
		 *            The software yield by the search query.
		 * @param software
		 *            The software found so far, including <code>software</code>
		 *            .
		 */
		void onSoftwareFound(Software software, ArrayList<Software> softwares);
	}

	private void parseItemAttributes(XmlPullParser parser, Software software)
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
					software.mAuthor = parser.getText();
				}
			} else if (RESPONSE_TAG_PLATFORM.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					software.mPlatform.add(parser.getText());
				}
			} else if (RESPONSE_TAG_EAN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					software.mEan = parser.getText();
				}
			} else if (RESPONSE_TAG_ISBN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					software.mIsbn = parser.getText();
				}
			} else if (RESPONSE_TAG_UPC.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					software.mUpc = parser.getText();
				}
			} else if (RESPONSE_TAG_BINDING.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					software.mFormat = parser.getText();
				}
			} else if (RESPONSE_TAG_RELEASEDATE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					final SimpleDateFormat format = new SimpleDateFormat(
							"yyyy-MM-dd");
					try {
						software.mReleaseDate = format.parse(parser.getText());
					} catch (ParseException e) {
						// Ignore
					}
				}
			} else if (RESPONSE_TAG_TITLE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					software.mTitle = parser.getText();
				}
			} else if (RESPONSE_TAG_LABEL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					software.mLabel = parser.getText();
				}
			}
		}
	}

	private void parsePrices(XmlPullParser parser, Software software)
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
						if (software.mRetailPrice == null
								|| (software.mRetailPrice != null && Double
										.valueOf(software.mRetailPrice
												.substring(1)) > Double
										.valueOf(price.substring(1)))) {
							software.mRetailPrice = price;
						}
					} catch (NumberFormatException n) {
						Log.e(LOG_TAG, n.toString());
						if (software.mRetailPrice == null)
							software.mRetailPrice = "";
					}
				}
			}
		}
	}

	private void parseImageSet(XmlPullParser parser, Software software)
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
				Software.mImages.put(ImageSize.TINY, parseImage(parser));
			} else if (RESPONSE_TAG_THUMBNAILIMAGE.equals(name)) {
				Software.mImages.put(ImageSize.THUMBNAIL, parseImage(parser));
			} else if (RESPONSE_TAG_MEDIUMIMAGE.equals(name)) {
				Software.mImages.put(ImageSize.MEDIUM, parseImage(parser));
			} else if (RESPONSE_TAG_LARGEIMAGE.equals(name)) {
				Software.mImages.put(ImageSize.LARGE, parseImage(parser));
			}
		}
	}
}
