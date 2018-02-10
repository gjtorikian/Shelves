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

package com.miadzin.shelves.provider.books;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.DisplayMetrics;
import android.util.Log;

import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.base.BaseItem.ImageSize;
import com.miadzin.shelves.server.ServerInfo;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.IOUtilities.inputTypes;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to load books from a books store.
 */
public class BooksStore extends ServerInfo {
	static final String LOG_TAG = "BooksStore";

	public static class Book extends BaseItem implements Parcelable,
			BaseColumns {
		public static final Uri CONTENT_URI = Uri
				.parse("content://shelves/books");

		List<String> mAuthors;
		List<Description> mDescriptions;
		int mPages;
		String mDeweyNumber;
		String mEdition;
		String mCategory;
		String mPublisher;
		Date mPublicationDate;

		private ServerImageLoader mLoader;

		Map<ImageSize, String> mImages;

		@Override
		public String getImageUrl(ImageSize size) {
			String url = mImages.get(size);
			if (TextUtilities.isEmpty(url))
				return null;
			else
				return TextUtilities.unprotectString(url);
		}

		Book() {
			mStorePrefix = ServerInfo.NAME;
			mLoader = new ServerImageLoader();
			mImages = new HashMap<ImageSize, String>(6);
			mAuthors = new ArrayList<String>(1);
			mDescriptions = new ArrayList<Description>();
			mTags = new ArrayList<String>(1);
			mLanguage = new ArrayList<String>(1);
		}

		private Book(Parcel in) {
			mIsbn = in.readString();
			mEan = in.readString();
			mInternalId = in.readString();
			mTitle = in.readString();
			mAuthors = new ArrayList<String>(1);
			mTags = new ArrayList<String>(1);
			in.readStringList(mAuthors);
		}

		public List<String> getAuthors() {
			return mAuthors;
		}

		public List<Description> getDescriptions() {
			return mDescriptions;
		}

		public int getPagesCount() {
			return mPages;
		}

		public String getDeweyNumber() {
			return mDeweyNumber;
		}

		public String getEdition() {
			return mEdition;
		}

		public String getCategory() {
			return mCategory;
		}

		public String getPublisher() {
			return mPublisher;
		}

		public Date getPublicationDate() {
			return mPublicationDate;
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
			values.put(AUTHORS, TextUtilities.joinAuthors(mAuthors, ", "));
			values.put(PUBLISHER, mPublisher);
			values.put(REVIEWS, TextUtilities.join(mDescriptions, "\n\n"));
			values.put(PAGES, mPages);
			if (mLastModified != null) {
				values.put(LAST_MODIFIED, mLastModified.getTimeInMillis());
			}
			values.put(PUBLICATION,
					mPublicationDate != null ? format.format(mPublicationDate)
							: "");
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

			// GJT: Added these, for more data
			values.put(TAGS, TextUtilities.join(mTags, ", "));
			values.put(FORMAT, mFormat);
			values.put(CATEGORY, mCategory);
			values.put(DEWEY_NUMBER, mDeweyNumber);
			values.put(EDITION, mEdition);
			values.put(LANGUAGE, TextUtilities.join(mLanguage, ", "));
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

		public static Book fromCursor(Cursor c) {
			final Book book = new Book();

			book.mInternalId = c
					.getString(c.getColumnIndexOrThrow(INTERNAL_ID)).substring(
							ServerInfo.NAME.length());
			book.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
			book.mIsbn = c.getString(c.getColumnIndexOrThrow(ISBN));
			book.mUpc = c.getString(c.getColumnIndexOrThrow(UPC));
			book.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));
			Collections.addAll(book.mAuthors,
					c.getString(c.getColumnIndexOrThrow(AUTHORS)).split(", "));
			book.mPublisher = c.getString(c.getColumnIndexOrThrow(PUBLISHER));
			book.mDescriptions.add(new Description("", c.getString(c
					.getColumnIndexOrThrow(REVIEWS))));
			book.mPages = c.getInt(c.getColumnIndexOrThrow(PAGES));

			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(c.getLong(c
					.getColumnIndexOrThrow(LAST_MODIFIED)));
			book.mLastModified = calendar;

			try {
				Collections.addAll(book.mTags,
						c.getString(c.getColumnIndexOrThrow(TAGS)).split(", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
			try {
				book.mPublicationDate = format.parse(c.getString(c
						.getColumnIndexOrThrow(PUBLICATION)));
			} catch (ParseException e) {
				// Ignore
			}

			book.mDetailsUrl = TextUtilities.unprotectString(c.getString(c
					.getColumnIndexOrThrow(DETAILS_URL)));

			String tiny_url = c.getString(c.getColumnIndexOrThrow(TINY_URL));

			if (tiny_url != null) {
				tiny_url = TextUtilities.unprotectString(c.getString(c
						.getColumnIndexOrThrow(TINY_URL)));

				final int density = Preferences.getDPI();

				switch (density) {
				case 320:
					book.mImages.put(ImageSize.LARGE, tiny_url);
					book.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 240:
					book.mImages.put(ImageSize.MEDIUM, tiny_url);
					book.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 120:
					book.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					book.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					break;
				case 160:
				default:
					book.mImages.put(ImageSize.TINY, tiny_url);
					book.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				}
			}

			// GJT: Added these for more details
			book.mFormat = c.getString(c.getColumnIndexOrThrow(FORMAT));
			book.mCategory = c.getString(c.getColumnIndexOrThrow(CATEGORY));
			book.mEdition = c.getString(c.getColumnIndexOrThrow(EDITION));
			try {
				Collections.addAll(
						book.mLanguage,
						c.getString(c.getColumnIndexOrThrow(LANGUAGE)).split(
								", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}
			book.mDeweyNumber = c.getString(c
					.getColumnIndexOrThrow(DEWEY_NUMBER));
			book.mRetailPrice = c.getString(c
					.getColumnIndexOrThrow(RETAIL_PRICE));

			book.mRating = c.getInt(c.getColumnIndexOrThrow(RATING));

			book.mLoanedTo = c.getString(c.getColumnIndex(LOANED_TO));

			String loanDate = c.getString(c.getColumnIndexOrThrow(LOAN_DATE));
			if (loanDate != null) {
				try {
					book.mLoanDate = Preferences.getDateFormat()
							.parse(loanDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			String wishlistDate = c.getString(c
					.getColumnIndexOrThrow(WISHLIST_DATE));
			if (wishlistDate != null) {
				try {
					book.mWishlistDate = Preferences.getDateFormat().parse(
							wishlistDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			book.mEventId = c.getInt(c.getColumnIndexOrThrow(EVENT_ID));
			book.mNotes = c.getString(c.getColumnIndexOrThrow(NOTES));

			book.mQuantity = c.getString(c.getColumnIndex(QUANTITY));

			return book;
		}

		@Override
		public String toString() {
			return "Book[ISBN=" + mIsbn + ", EAN=" + mEan + ", UPC=" + mUpc
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
			dest.writeStringList(mAuthors);
			dest.writeStringList(mTags);
		}

		public static final Creator<Book> CREATOR = new Creator<Book>() {
			public Book createFromParcel(Parcel in) {
				return new Book(in);
			}

			public Book[] newArray(int size) {
				return new Book[size];
			}
		};
	}

	/**
	 * Finds the book with the specified id.
	 * 
	 * @param id
	 *            The id of the book to find (ISBN-10, ISBN-13, etc.)
	 * @param type
	 * 
	 * @return A Book instance if the book was found or null otherwise.
	 */
	public Book findBook(String id,
			final IOUtilities.inputTypes mSavedImportType, Context context) {
		Uri.Builder uri = assembleURI(id, mSavedImportType, context,
				VALUE_SEARCHINDEX_BOOKS, RESPONSE_TAG_ISBN);

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
		Book book = createBook();
		book = findBookLookup(get, book, mSavedImportType, id);

		if (book != null) {
			return book;
		}

		book = createBook();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_BOOKS, context,
				RESPONSE_TAG_ASIN);
		get = new HttpGet(uri.build().toString());
		book = findBookLookup(get, book, mSavedImportType, id);

		if (book != null) {
			return book;
		}

		book = createBook();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_BOOKS, context,
				RESPONSE_TAG_EAN);
		get = new HttpGet(uri.build().toString());
		book = findBookLookup(get, book, mSavedImportType, id);

		if (book != null) {
			return book;
		} else {
			return null;
		}
	}

	private Book findBookLookup(HttpGet get, final Book book,
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
									result[0] = parseBook(parser, book);
								}
							}, mSavedImportType);
						}
					});
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not find " + mSavedImportType
					+ " item with ID " + id);
		}

		if (TextUtilities.isEmpty(book.mEan) && id.length() == 13) {
			book.mEan = id;
		} else if (TextUtilities.isEmpty(book.mIsbn) && id.length() == 10) {
			book.mIsbn = id;
		}

		return result[0] ? book : null;
	}

	/**
	 * Searches for books that match the provided query.
	 * 
	 * @param query
	 *            The free form query used to search for books.
	 * 
	 * @return A list of Book instances if query was successful or null
	 *         otherwise.
	 */
	public ArrayList<Book> searchBooks(String query, String page,
			final BookSearchListener listener, Context context) {
		final Uri.Builder uri = ServerInfo.buildSearchQuery(query,
				VALUE_SEARCHINDEX_BOOKS, page, context);
		final HttpGet get = new HttpGet(uri.build().toString());
		final ArrayList<Book> books = new ArrayList<Book>(10);

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									parseBooks(parser, books, listener);
								}
							}, null);
						}
					});

			return books;
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform search with query: "
					+ query, e);
		}

		return null;
	}

	/**
	 * Parses a book from the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the book.
	 * @param book
	 *            The book object to put the parsed data in.
	 * 
	 * @return True if the book could correctly be parsed, false otherwise.
	 */
	boolean parseBook(XmlPullParser parser, Book book)
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
					if (book.mInternalId == null)
						book.mInternalId = parser.getText();
				}
			} else if (RESPONSE_TAG_DETAILPAGEURL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mDetailsUrl = parser.getText();
				}
			} else if (RESPONSE_TAG_IMAGESET.equals(name)) {
				if (RESPONSE_VALUE_CATEGORY_PRIMARY.equals(parser
						.getAttributeValue(null, RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, book);
				} else if (book.mImages.get(ImageSize.THUMBNAIL) == null
						&& RESPONSE_VALUE_CATEGORY_VARIANT
								.equals(parser.getAttributeValue(null,
										RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, book);
				}
			} else if (RESPONSE_TAG_ITEMATTRIBUTES.equals(name)) {
				parseItemAttributes(parser, book);
			} else if (RESPONSE_TAG_LOWESTUSEDPRICE.equals(name)) {
				parsePrices(parser, book);
			} else if (RESPONSE_TAG_EDITORIALREVIEW.equals(name)) {
				book.mDescriptions.add(parseEditorialReview(parser));
			} else if (RESPONSE_TAG_ERRORS.equals(name)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates an instance of
	 * {@link com.miadzin.shelves.provider.books.BooksStore.Book} with this book
	 * store's name.
	 * 
	 * @return A new instance of Book.
	 */
	Book createBook() {
		return new Book();
	}

	private void parseBooks(XmlPullParser parser, ArrayList<Book> books,
			BookSearchListener listener) throws IOException,
			XmlPullParserException {

		int type;
		while ((type = parser.next()) != XmlPullParser.END_TAG
				&& type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (findNextItem(parser)) {
				final Book book = createBook();
				if (parseBook(parser, book)) {
					books.add(book);
					listener.onBookFound(book, books);
				}
			}
		}
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
	 * {@link com.miadzin.shelves.provider.books.BooksStore#searchBooks(String, com.miadzin.shelves.provider.books.BooksStore.BookSearchListener)}
	 * .
	 */
	public interface BookSearchListener {
		/**
		 * Invoked whenever a book was found by the search operation.
		 * 
		 * @param book
		 *            The book yield by the search query.
		 * @param books
		 *            The books found so far, including <code>book</code>.
		 */
		void onBookFound(Book book, ArrayList<Book> books);
	}

	private void parseItemAttributes(XmlPullParser parser, Book book)
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
			if (RESPONSE_TAG_AUTHOR.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mAuthors.add(parser.getText());
				}
			}
			if (RESPONSE_TAG_CREATOR.equals(name)) {
				if (parser.getAttributeValue(0).equals("Translator")) {
					if (parser.next() == XmlPullParser.TEXT) {
						book.mAuthors.add(parser.getText());
					}
				}
			} else if (RESPONSE_TAG_EAN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mEan = parser.getText();
				}
			} else if (RESPONSE_TAG_ISBN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mIsbn = parser.getText();
				}
			} else if (RESPONSE_TAG_UPC.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mUpc = parser.getText();
				}
			} else if (RESPONSE_TAG_NUMBEROFPAGES.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mPages = Integer.parseInt(parser.getText());
				}
			} else if (RESPONSE_TAG_PUBLICATIONDATE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					final SimpleDateFormat format = new SimpleDateFormat(
							"yyyy-MM-dd");
					try {
						book.mPublicationDate = format.parse(parser.getText());
					} catch (ParseException e) {
						// Ignore
					}
				}
			} else if (RESPONSE_TAG_TITLE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mTitle = parser.getText();
				}
			} else if (RESPONSE_TAG_LABEL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mPublisher = parser.getText();
				}
			} else if (RESPONSE_TAG_BINDING.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mFormat = parser.getText();
				}
			} else if (RESPONSE_TAG_DEWEY.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mDeweyNumber = parser.getText();
				}
			} else if (RESPONSE_TAG_EDITION.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					book.mEdition = parser.getText();
				}
			} else if (RESPONSE_TAG_LANGUAGE.equals(name)) {
				book.mLanguage.add(parseLanguage(parser));
			}
		}
	}

	private void parsePrices(XmlPullParser parser, Book book)
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
						if (book.mRetailPrice == null
								|| (book.mRetailPrice != null && Double
										.valueOf(book.mRetailPrice.substring(1)) > Double
										.valueOf(price.substring(1)))) {
							book.mRetailPrice = price;
						}
					} catch (NumberFormatException n) {
						Log.e(LOG_TAG, n.toString());
						if (book.mRetailPrice == null)
							book.mRetailPrice = "";
					}
				}
			}
		}
	}

	private void parseImageSet(XmlPullParser parser, Book book)
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
				book.mImages.put(ImageSize.TINY, parseImage(parser));
			} else if (RESPONSE_TAG_THUMBNAILIMAGE.equals(name)) {
				book.mImages.put(ImageSize.THUMBNAIL, parseImage(parser));
			} else if (RESPONSE_TAG_MEDIUMIMAGE.equals(name)) {
				book.mImages.put(ImageSize.MEDIUM, parseImage(parser));
			} else if (RESPONSE_TAG_LARGEIMAGE.equals(name)) {
				book.mImages.put(ImageSize.LARGE, parseImage(parser));
			}
		}
	}
}
