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

package com.miadzin.shelves.provider.music;

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
 * Utility class to load music from a music store.
 */
public class MusicStore extends ServerInfo {
	static final String LOG_TAG = "MusicStore";

	public static class Music extends BaseItem implements Parcelable,
			BaseColumns {
		public static final Uri CONTENT_URI = Uri
				.parse("content://MusicProvider/music");

		List<String> mAuthors;
		List<Description> mDescriptions;
		private ServerImageLoader mLoader;
		List<String> mTracks;

		public static HashMap<ImageSize, String> mImages;

		@Override
		public String getImageUrl(ImageSize size) {
			String url = mImages.get(size);
			if (TextUtilities.isEmpty(url))
				return null;
			else
				return TextUtilities.unprotectString(url);
		}

		Music() {
			mStorePrefix = ServerInfo.NAME;
			mLoader = new ServerImageLoader();
			mImages = new HashMap<ImageSize, String>(6);
			mAuthors = new ArrayList<String>(1);
			mDescriptions = new ArrayList<Description>();
			mTags = new ArrayList<String>(1);
			mTracks = new ArrayList<String>(1);
		}

		private Music(Parcel in) {
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

		public List<String> getTracks() {
			return mTracks;
		}

		@Override
		public Bitmap loadCover(ImageSize thumbnail) {
			final String url = mImages.get(thumbnail);
			if (TextUtilities.isEmpty(url))
				return null;

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
			values.put(AUTHORS, TextUtilities.join(mAuthors, ", "));
			values.put(LABEL, mLabel);
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

			values.put(TRACKS, TextUtilities.join(mTracks, ", "));
			values.put(NOTES, mNotes);

			values.put(QUANTITY, mQuantity);

			return values;
		}

		public static Music fromCursor(Cursor c) {
			final Music music = new Music();

			music.mInternalId = c.getString(
					c.getColumnIndexOrThrow(INTERNAL_ID)).substring(
					ServerInfo.NAME.length());
			music.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
			music.mIsbn = c.getString(c.getColumnIndexOrThrow(ISBN));
			music.mUpc = c.getString(c.getColumnIndexOrThrow(UPC));
			music.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));
			Collections.addAll(music.mAuthors,
					c.getString(c.getColumnIndexOrThrow(AUTHORS)).split(", "));
			music.mLabel = c.getString(c.getColumnIndexOrThrow(LABEL));
			music.mDescriptions.add(new Description("", c.getString(c
					.getColumnIndexOrThrow(REVIEWS))));

			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(c.getLong(c
					.getColumnIndexOrThrow(LAST_MODIFIED)));
			music.mLastModified = calendar;
			try {
				Collections.addAll(music.mTags,
						c.getString(c.getColumnIndexOrThrow(TAGS)).split(", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
			try {
				music.mReleaseDate = format.parse(c.getString(c
						.getColumnIndexOrThrow(RELEASE_DATE)));
			} catch (ParseException e) {
				// Ignore
			}

			music.mDetailsUrl = TextUtilities.unprotectString(c.getString(c
					.getColumnIndexOrThrow(DETAILS_URL)));

			String tiny_url = c.getString(c.getColumnIndexOrThrow(TINY_URL));

			if (tiny_url != null) {
				tiny_url = TextUtilities.unprotectString(c.getString(c
						.getColumnIndexOrThrow(TINY_URL)));

				final int density = Preferences.getDPI();

				switch (density) {
				case 320:
					Music.mImages.put(ImageSize.LARGE, tiny_url);
					Music.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 240:
					Music.mImages.put(ImageSize.MEDIUM, tiny_url);
					Music.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 120:
					Music.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					Music.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					break;
				case 160:
				default:
					Music.mImages.put(ImageSize.TINY, tiny_url);
					Music.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				}
			}

			music.mFormat = c.getString(c.getColumnIndexOrThrow(FORMAT));
			music.mRetailPrice = c.getString(c
					.getColumnIndexOrThrow(RETAIL_PRICE));
			music.mRating = c.getInt(c.getColumnIndexOrThrow(RATING));

			music.mLoanedTo = c.getString(c.getColumnIndex(LOANED_TO));
			String loanDate = c.getString(c.getColumnIndexOrThrow(LOAN_DATE));
			if (loanDate != null) {
				try {
					music.mLoanDate = Preferences.getDateFormat().parse(
							loanDate);
				} catch (ParseException e) {
					// Ignore
				}
			}
			String wishlistDate = c.getString(c
					.getColumnIndexOrThrow(WISHLIST_DATE));
			if (wishlistDate != null) {
				try {
					music.mWishlistDate = Preferences.getDateFormat().parse(
							wishlistDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			music.mEventId = c.getInt(c.getColumnIndexOrThrow(EVENT_ID));

			try {
				Collections.addAll(music.mTracks,
						c.getString(c.getColumnIndexOrThrow(TRACKS))
								.split(", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}
			music.mNotes = c.getString(c.getColumnIndexOrThrow(NOTES));

			music.mQuantity = c.getString(c.getColumnIndex(QUANTITY));

			return music;
		}

		@Override
		public String toString() {
			return "Music[ISBN=" + mIsbn + ", EAN=" + mEan + ", UPC=" + mUpc
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

		public static final Creator<Music> CREATOR = new Creator<Music>() {
			public Music createFromParcel(Parcel in) {
				return new Music(in);
			}

			public Music[] newArray(int size) {
				return new Music[size];
			}
		};
	}

	/**
	 * Finds the music with the specified id.
	 * 
	 * @param id
	 *            The id of the music to find (ISBN-10, ISBN-13, etc.)
	 * @param mSavedImportType
	 * 
	 * @return A Music instance if the music was found or null otherwise.
	 */
	public Music findMusic(String id,
			final IOUtilities.inputTypes mSavedImportType, Context context) {
		switch (id.length()) {
		case 10:
			android.util.Log.i(LOG_TAG, "Looking up ISBN: " + id);
			break;
		case 13:
			android.util.Log.i(LOG_TAG, "Looking up EAN: " + id);
			break;
		case 11:
			id = "00" + id;
			android.util.Log
					.i(LOG_TAG, "Looking up (doubly padded) EAN: " + id);
			break;
		case 12:
			id = "0" + id;
			android.util.Log.i(LOG_TAG, "Looking up (padded) EAN: " + id);
			break;
		default:
			android.util.Log.i(LOG_TAG, "Looking up ID: " + id);
			break;
		}
		Uri.Builder uri = assembleURI(id, mSavedImportType, context,
				VALUE_SEARCHINDEX_MUSIC, RESPONSE_TAG_EAN);

		HttpGet get = new HttpGet(uri.build().toString());
		Music music = createMusic();
		music = findMusicLookup(get, music, mSavedImportType, id);

		if (music != null) {
			return music;
		}

		music = createMusic();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_MUSIC, context,
				RESPONSE_TAG_ASIN, VALUE_RESPONSEGROUP_LARGE);
		get = new HttpGet(uri.build().toString());
		music = findMusicLookup(get, music, mSavedImportType, id);

		if (music != null) {
			return music;
		}

		music = createMusic();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_MUSIC, context,
				RESPONSE_TAG_SKU, VALUE_RESPONSEGROUP_LARGE);
		get = new HttpGet(uri.build().toString());
		music = findMusicLookup(get, music, mSavedImportType, id);

		if (music != null) {
			return music;
		}

		music = createMusic();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_MUSIC, context,
				RESPONSE_TAG_UPC);
		get = new HttpGet(uri.build().toString());
		music = findMusicLookup(get, music, mSavedImportType, id);

		if (music != null) {
			return music;
		} else {
			return null;
		}
	}

	private Music findMusicLookup(HttpGet get, final Music music,
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
									result[0] = parseMusic(parser, music);
								}
							}, mSavedImportType);
						}
					});
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not find " + mSavedImportType
					+ " item with ID " + id);
		}

		if (TextUtilities.isEmpty(music.mEan) && id.length() == 13) {
			music.mEan = id;
		} else if (TextUtilities.isEmpty(music.mIsbn) && id.length() == 10) {
			music.mIsbn = id;
		}

		return result[0] ? music : null;
	}

	/**
	 * Searchs for music that match the provided query.
	 * 
	 * @param query
	 *            The free form query used to search for music.
	 * 
	 * @return A list of Music instances if query was successful or null
	 *         otherwise.
	 */
	public ArrayList<Music> searchMusic(String query, String page,
			final MusicSearchListener listener, Context context) {
		final Uri.Builder uri = buildSearchQuery(query,
				VALUE_SEARCHINDEX_MUSIC, page, context);
		final HttpGet get = new HttpGet(uri.build().toString());
		final ArrayList<Music> music = new ArrayList<Music>(10);

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									parseMusic(parser, music, listener);
								}
							}, null);
						}
					});

			return music;
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform search with query: "
					+ query, e);
		}

		return null;
	}

	/**
	 * Parses a music from the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the music.
	 * @param music
	 *            The music object to put the parsed data in.
	 * 
	 * @return True if the music could correctly be parsed, false otherwise.
	 */
	boolean parseMusic(XmlPullParser parser, Music music)
			throws XmlPullParserException, IOException {

		int type;
		String name;
		int number = 0;
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
					if (music.mInternalId == null)
						music.mInternalId = parser.getText();
				}
			} else if (RESPONSE_TAG_DETAILPAGEURL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					music.mDetailsUrl = parser.getText();
				}
			} else if (RESPONSE_TAG_IMAGESET.equals(name)) {
				if (RESPONSE_VALUE_CATEGORY_PRIMARY.equals(parser
						.getAttributeValue(null, RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, music);
				} else if (Music.mImages.get(ImageSize.THUMBNAIL) == null
						&& RESPONSE_VALUE_CATEGORY_VARIANT
								.equals(parser.getAttributeValue(null,
										RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, music);
				}
			} else if (RESPONSE_TAG_ITEMATTRIBUTES.equals(name)) {
				parseItemAttributes(parser, music);
			} else if (RESPONSE_TAG_LOWESTUSEDPRICE.equals(name)) {
				parsePrices(parser, music);
			} else if (RESPONSE_TAG_EDITORIALREVIEW.equals(name)) {
				music.mDescriptions.add(parseEditorialReview(parser));
			} else if (RESPONSE_TAG_DISC.equals(name)) {
				music.mTracks.add("Disc " + ++number + "|");
				parseTracks(parser, music);
			} else if (RESPONSE_TAG_ERRORS.equals(name)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates an instance of
	 * {@link com.miadzin.shelves.provider.music.MusicStore.Music} with this
	 * music store's name.
	 * 
	 * @return A new instance of Music.
	 */
	Music createMusic() {
		return new Music();
	}

	private void parseMusic(XmlPullParser parser, ArrayList<Music> musics,
			MusicSearchListener listener) throws IOException,
			XmlPullParserException {

		int type;
		while ((type = parser.next()) != XmlPullParser.END_TAG
				&& type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (findNextItem(parser)) {
				final Music music = createMusic();
				if (parseMusic(parser, music)) {
					musics.add(music);
					listener.onMusicFound(music, musics);
				}
			}
		}
	}

	private void parseTracks(XmlPullParser parser, Music music)
			throws XmlPullParserException, IOException {
		int number = 0;
		int type;
		String tag;
		final int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}
			tag = parser.getName();
			if (RESPONSE_TAG_TRACK.equals(tag)) {
				parser.next();
				music.mTracks.add("#" + ++number + ": " + parser.getText());
			}
		}
		music.mTracks.add("||");
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
	 * {@link com.miadzin.shelves.provider.music.MusicStore#searchMusic(String, com.miadzin.shelves.provider.music.MusicStore.MusicSearchListener)}
	 * .
	 */
	public interface MusicSearchListener {
		/**
		 * Invoked whenever a music was found by the search operation.
		 * 
		 * @param music
		 *            The music yield by the search query.
		 * @param music
		 *            The music found so far, including <code>music</code>.
		 */
		void onMusicFound(Music music, ArrayList<Music> musics);
	}

	private void parseItemAttributes(XmlPullParser parser, Music music)
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
			if (RESPONSE_TAG_ARTIST.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					music.mAuthors.add(parser.getText());
				}
			} else if (RESPONSE_TAG_EAN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					music.mEan = parser.getText();
				}
			} else if (RESPONSE_TAG_ISBN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					music.mIsbn = parser.getText();
				}
			} else if (RESPONSE_TAG_UPC.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					music.mUpc = parser.getText();
				}
			} else if (RESPONSE_TAG_BINDING.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					music.mFormat = parser.getText();
				}
			} else if (RESPONSE_TAG_ORIGINALRELEASEDATE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					final SimpleDateFormat format = new SimpleDateFormat(
							"yyyy-MM-dd");
					try {
						music.mReleaseDate = format.parse(parser.getText());
					} catch (ParseException e) {
						// Ignore
					}
				}
			} else if (RESPONSE_TAG_TITLE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					music.mTitle = parser.getText();
				}
			} else if (RESPONSE_TAG_LABEL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					music.mLabel = parser.getText();
				}
			}
		}
	}

	private void parsePrices(XmlPullParser parser, Music music)
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
						if (music.mRetailPrice == null
								|| (music.mRetailPrice != null && Double
										.valueOf(music.mRetailPrice
												.substring(1)) > Double
										.valueOf(price.substring(1)))) {
							music.mRetailPrice = price;
						}
					} catch (NumberFormatException n) {
						Log.e(LOG_TAG, n.toString());
						if (music.mRetailPrice == null)
							music.mRetailPrice = "";
					}
				}
			}
		}
	}

	private void parseImageSet(XmlPullParser parser, Music music)
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
				Music.mImages.put(ImageSize.TINY, parseImage(parser));
			} else if (RESPONSE_TAG_THUMBNAILIMAGE.equals(name)) {
				Music.mImages.put(ImageSize.THUMBNAIL, parseImage(parser));
			} else if (RESPONSE_TAG_MEDIUMIMAGE.equals(name)) {
				Music.mImages.put(ImageSize.MEDIUM, parseImage(parser));
			} else if (RESPONSE_TAG_LARGEIMAGE.equals(name)) {
				Music.mImages.put(ImageSize.LARGE, parseImage(parser));
			}
		}
	}

}
