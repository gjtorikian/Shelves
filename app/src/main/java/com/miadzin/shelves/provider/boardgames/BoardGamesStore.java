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

package com.miadzin.shelves.provider.boardgames;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
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
import com.miadzin.shelves.server.BGGInfo;
import com.miadzin.shelves.util.Entities;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.IOUtilities.inputTypes;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;

/**
 * Utility class to load boardgames from a boardgames store.
 */
public class BoardGamesStore extends BGGInfo {
	static final String LOG_TAG = "BoardGamesStore";

	public static class BoardGame extends BaseItem implements Parcelable,
			BaseColumns {
		public static final Uri CONTENT_URI = Uri
				.parse("content://BoardGamesProvider/boardgames");

		String mAuthor;
		String mDescriptions;
		String mPublicationDate;
		String mMinPlayers;
		String mMaxPlayers;
		String mAge;
		String mPlayingTime;

		private BGGImageLoader mLoader;

		Map<ImageSize, String> mImages;

		@Override
		public String getImageUrl(ImageSize size) {
			String url = mImages.get(size);
			if (TextUtilities.isEmpty(url))
				return null;
			else
				return TextUtilities.unprotectString(url);
		}

		BoardGame() {
			this("", null);
		}

		BoardGame(String storePrefix, BGGImageLoader loader) {
			mStorePrefix = BGGInfo.NAME;
			mLoader = loader;
			mImages = new HashMap<ImageSize, String>(6);
			mDescriptions = new String();
			mTags = new ArrayList<String>(1);
		}

		private BoardGame(Parcel in) {
			mIsbn = in.readString();
			mEan = in.readString();
			mInternalId = in.readString();
			mTitle = in.readString();
			mTags = new ArrayList<String>(1);
		}

		public String getDescriptions() {
			return mDescriptions;
		}

		public String getAuthor() {
			return mAuthor;
		}

		public String getPublicationDate() {
			return mPublicationDate;
		}

		public String getMinPlayers() {
			return mMinPlayers;
		}

		public String getMaxPlayers() {
			return mMaxPlayers;
		}

		public String getAge() {
			return mAge;
		}

		public String getPlayingTime() {
			return mPlayingTime;
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

			values.put(INTERNAL_ID, BGGInfo.NAME + mInternalId);
			values.put(EAN, mEan);
			values.put(TITLE, mTitle);
			values.put(AUTHORS, mAuthor);
			values.put(REVIEWS, mDescriptions);

			if (mLastModified != null) {
				values.put(LAST_MODIFIED, mLastModified.getTimeInMillis());
			}

			values.put(PUBLICATION, mPublicationDate);

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

			values.put(MIN_PLAYERS, mMinPlayers);
			values.put(MAX_PLAYERS, mMaxPlayers);
			values.put(AGE, mAge);
			values.put(PLAYING_TIME, mPlayingTime);

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

		public static BoardGame fromCursor(Cursor c) {
			final BoardGame boardgame = new BoardGame();

			boardgame.mInternalId = c.getString(
					c.getColumnIndexOrThrow(INTERNAL_ID)).substring(
					BGGInfo.NAME.length());
			boardgame.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
			boardgame.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));

			boardgame.mAuthor = c.getString(c.getColumnIndexOrThrow(AUTHORS));
			boardgame.mDescriptions = c.getString(c
					.getColumnIndexOrThrow(REVIEWS));

			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(c.getLong(c
					.getColumnIndexOrThrow(LAST_MODIFIED)));
			boardgame.mLastModified = calendar;

			try {
				Collections.addAll(boardgame.mTags,
						c.getString(c.getColumnIndexOrThrow(TAGS)).split(", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			boardgame.mPublicationDate = c.getString(c
					.getColumnIndexOrThrow(PUBLICATION));

			boardgame.mDetailsUrl = c.getString(c
					.getColumnIndexOrThrow(DETAILS_URL));

			String tiny_url = c.getString(c.getColumnIndexOrThrow(TINY_URL));

			if (tiny_url != null) {
				tiny_url = TextUtilities.unprotectString(c.getString(c
						.getColumnIndexOrThrow(TINY_URL)));

				final int density = Preferences.getDPI();

				switch (density) {
				case 320:
					boardgame.mImages.put(ImageSize.LARGE, tiny_url);
					break;
				case 240:
					boardgame.mImages.put(ImageSize.MEDIUM, tiny_url);
					break;
				case 120:
					boardgame.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					break;
				case 160:
				default:
					boardgame.mImages.put(ImageSize.TINY, tiny_url);
					break;
				}
			}

			boardgame.mMinPlayers = c.getString(c.getColumnIndex(MIN_PLAYERS));
			boardgame.mMaxPlayers = c.getString(c.getColumnIndex(MAX_PLAYERS));
			boardgame.mAge = c.getString(c.getColumnIndex(AGE));
			boardgame.mPlayingTime = c
					.getString(c.getColumnIndex(PLAYING_TIME));

			boardgame.mRating = c.getInt(c.getColumnIndexOrThrow(RATING));

			boardgame.mLoanedTo = c.getString(c.getColumnIndex(LOANED_TO));

			String loanDate = c.getString(c.getColumnIndexOrThrow(LOAN_DATE));
			if (loanDate != null) {
				try {
					boardgame.mLoanDate = Preferences.getDateFormat().parse(
							loanDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			String wishlistDate = c.getString(c
					.getColumnIndexOrThrow(WISHLIST_DATE));
			if (wishlistDate != null) {
				try {
					boardgame.mWishlistDate = Preferences.getDateFormat()
							.parse(wishlistDate);
				} catch (ParseException e) {
					// Ignore
				}
			}

			boardgame.mEventId = c.getInt(c.getColumnIndexOrThrow(EVENT_ID));
			boardgame.mNotes = c.getString(c.getColumnIndexOrThrow(NOTES));

			boardgame.mQuantity = c.getString(c.getColumnIndex(QUANTITY));

			return boardgame;
		}

		@Override
		public String toString() {
			return "BoardGame[EAN=" + mEan + "]";
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

		public static final Creator<BoardGame> CREATOR = new Creator<BoardGame>() {
			public BoardGame createFromParcel(Parcel in) {
				return new BoardGame(in);
			}

			public BoardGame[] newArray(int size) {
				return new BoardGame[size];
			}
		};
	}

	public BoardGamesStore() {
		mStoreName = "BGGBoardGames";
		mStoreLabel = "BGG";
		mHost = BGGInfo.REST_HOST;
		mLoader = new BGGImageLoader();
	}

	/**
	 * Finds the boardgame with the specified id.
	 * 
	 * @param id
	 *            The id of the boardgame to find (ISBN-10, ISBN-13, etc.)
	 * @param type
	 * 
	 * @return A BoardGame instance if the boardgame was found or null
	 *         otherwise.
	 */
	public BoardGame findBoardGame(String id,
			final IOUtilities.inputTypes mSavedImportType, Context context) {
		Uri.Builder uri = new Uri.Builder();
		uri.path(REST_RETRIEVE_URL);
		uri.appendPath(id);

		android.util.Log.i(LOG_TAG, "Looking up boardgame #" + id);

		HttpGet get = new HttpGet(uri.build().toString());
		BoardGame boardgame = createBoardGame();
		boardgame = findBoardGameLookup(get, boardgame, mSavedImportType, id);

		if (boardgame != null) {
			return boardgame;
		} else
			return null;
	}

	private BoardGame findBoardGameLookup(HttpGet get,
			final BoardGame boardgame, final inputTypes mSavedImportType,
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
									result[0] = parseBoardGame(parser,
											boardgame);
								}
							}, mSavedImportType);
						}
					});
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not find " + mSavedImportType
					+ " item with ID " + id);
		}

		if (!TextUtilities.isEmpty(boardgame.mEan)) {
			boardgame.mInternalId = boardgame.mEan;
			boardgame.mDetailsUrl = DETAILS + boardgame.mEan;
		}

		return result[0] ? boardgame : null;
	}

	/**
	 * Searches for boardgames that match the provided query.
	 * 
	 * @param query
	 *            The free form query used to search for boardgames.
	 * 
	 * @return A list of BoardGame instances if query was successful or null
	 *         otherwise.
	 */
	public ArrayList<BoardGame> searchBoardGames(String query, String page,
			final BoardGameSearchListener listener, Context context) {
		Uri.Builder uri = new Uri.Builder();
		uri.path(REST_SEARCH_URL);
		uri.appendQueryParameter("search", query);

		android.util.Log.i(LOG_TAG, "Looking up boardgames: " + query);
		final HttpGet get = new HttpGet(uri.build().toString());
		final ArrayList<BoardGame> boardgames = new ArrayList<BoardGame>(10);

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									parseBoardGames(parser, boardgames,
											listener);
								}
							}, null);
						}
					});

			return boardgames;
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform search with query: "
					+ query, e);
		}

		return null;
	}

	/**
	 * Parses a boardgame from the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the boardgame.
	 * @param boardgame
	 *            The boardgame object to put the parsed data in.
	 * 
	 * @return True if the boardgame could correctly be parsed, false otherwise.
	 */
	boolean parseBoardGame(XmlPullParser parser, BoardGame boardgame)
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

			if (RESPONSE_TAG_BOARDGAME.equals(name)) {
				final int attrCount = parser.getAttributeCount();
				for (int i = 0; i < attrCount; i++) {
					if (parser.getAttributeName(i).equals(ID)) {
						boardgame.mEan = parser.getAttributeValue(i);
						break;
					}
				}
			} else if (RESPONSE_TAG_IMAGESET.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					String imgURL = parser.getText();

					// If the image url does not include the protocol then we need to add it.
					if (imgURL.matches("^//.*$")) {
						imgURL = "http:" + imgURL;
					}

					boardgame.mImages.put(ImageSize.TINY, imgURL);
					boardgame.mImages.put(ImageSize.MEDIUM, imgURL);
					boardgame.mImages.put(ImageSize.LARGE, imgURL);
				}
			} else if (RESPONSE_TAG_DESCRIPTION.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					Entities e = new Entities();
					String content = parser.getText();
					boardgame.mDescriptions = e.unescape(content.replaceAll(
							"\\<.*?>", ""));
				}
			} else if (RESPONSE_TAG_YEARPUBLISHED.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					boardgame.mPublicationDate = parser.getText();
				}
			} else if (RESPONSE_TAG_MIN_PLAYERS.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					boardgame.mMinPlayers = parser.getText();
				}
			} else if (RESPONSE_TAG_MAX_PLAYERS.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					boardgame.mMaxPlayers = parser.getText();
				}
			} else if (RESPONSE_TAG_PLAYING_TIME.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					boardgame.mPlayingTime = parser.getText();
				}
			} else if (RESPONSE_TAG_AGE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					boardgame.mAge = parser.getText();
				}
			} else if (TextUtilities.isEmpty(boardgame.mTitle)
					&& RESPONSE_TAG_NAME.equals(name)) {
				final int attrCount = parser.getAttributeCount();
				if (attrCount < 1) {
					if (parser.next() == XmlPullParser.TEXT) {
						boardgame.mTitle = parser.getText(); // GJT: To handle
																// searches
																// where the
																// name isn't
																// primary
					}
				}
				for (int i = 0; i < attrCount; i++) {
					if (parser.getAttributeName(i).equals(PRIMARY_NAME)) {
						if (parser.next() == XmlPullParser.TEXT) {
							boardgame.mTitle = parser.getText();
						}
						break;
					}
				}

			} else if (RESPONSE_TAG_BGPUBLISHER.toLowerCase().equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					boardgame.mAuthor = parser.getText();
				}
			}
		}

		return true;
	}

	/**
	 * Creates an instance of
	 * {@link com.miadzin.shelves.provider.boardgames.BoardGamesStore.BoardGame}
	 * with this boardgame store's name.
	 * 
	 * @return A new instance of BoardGame.
	 */
	BoardGame createBoardGame() {
		return new BoardGame(getName(), mLoader);
	}

	private void parseBoardGames(XmlPullParser parser,
			ArrayList<BoardGame> boardgames, BoardGameSearchListener listener)
			throws IOException, XmlPullParserException {

		int type;
		while ((type = parser.next()) != XmlPullParser.END_TAG
				&& type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			String objectid = "";
			final int attrCount = parser.getAttributeCount();
			for (int i = 0; i < attrCount; i++) {
				if (parser.getAttributeName(i).equals(ID)) {
					objectid = parser.getAttributeValue(i);
					break;
				}
			}

			if (findNextItem(parser)) {
				final BoardGame boardgame = createBoardGame();
				if (parseBoardGame(parser, boardgame)) {
					boardgame.mEan = boardgame.mInternalId = objectid;
					boardgame.mDetailsUrl = DETAILS + boardgame.mEan;
					boardgames.add(boardgame);
					listener.onBoardGameFound(boardgame, boardgames);
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
	 * {@link com.miadzin.shelves.provider.boardgames.BoardGamesStore#searchBoardGames(String, com.miadzin.shelves.provider.boardgames.BoardGamesStore.BoardGameSearchListener)}
	 * .
	 */
	public interface BoardGameSearchListener {
		/**
		 * Invoked whenever a boardgame was found by the search operation.
		 * 
		 * @param boardgame
		 *            The boardgame yield by the search query.
		 * @param boardgames
		 *            The boardgames found so far, including
		 *            <code>boardgame</code>.
		 */
		void onBoardGameFound(BoardGame boardgame,
				ArrayList<BoardGame> boardgames);
	}
}
