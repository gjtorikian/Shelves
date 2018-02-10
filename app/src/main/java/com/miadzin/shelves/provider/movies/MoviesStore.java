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

package com.miadzin.shelves.provider.movies;

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
 * Utility class to load movies from a movies store.
 */
public class MoviesStore extends ServerInfo {
	static final String LOG_TAG = "MoviesStore";

	public static class Movie extends BaseItem implements Parcelable,
			BaseColumns {
		public static final Uri CONTENT_URI = Uri
				.parse("content://MoviesProvider/movies");

		List<String> mAuthors;
		List<Description> mDescriptions;
		List<String> mDirectors;
		List<String> mActors;
		String mRunningTime;
		Date mTheatricalDebut;

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

		Movie() {
			mStorePrefix = ServerInfo.NAME;
			mLoader = new ServerImageLoader();
			mImages = new HashMap<ImageSize, String>(6);
			mDirectors = new ArrayList<String>(1);
			mDescriptions = new ArrayList<Description>();
			mTags = new ArrayList<String>(1);
			mActors = new ArrayList<String>(1);
			mLanguage = new ArrayList<String>(1);
			mFeatures = new ArrayList<String>(1);
		}

		private Movie(Parcel in) {
			mIsbn = in.readString();
			mEan = in.readString();
			mInternalId = in.readString();
			mTitle = in.readString();
			mDirectors = new ArrayList<String>(1);
			mTags = new ArrayList<String>(1);
			in.readStringList(mDirectors);
		}

		public List<String> getAuthors() {
			return mAuthors;
		}

		public List<Description> getDescriptions() {
			return mDescriptions;
		}

		public List<String> getDirectors() {
			return mDirectors;
		}

		public String getRunningTime() {
			return mRunningTime;
		}

		public List<String> getActors() {
			return mActors;
		}

		public Date getTheatricalDebut() {
			return mTheatricalDebut;
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
			values.put(DIRECTORS, TextUtilities.joinAuthors(mDirectors, ", "));
			values.put(ACTORS, TextUtilities.join(mActors, ", "));
			values.put(LABEL, mLabel);
			values.put(REVIEWS, TextUtilities.join(mDescriptions, "\n\n"));
			values.put(RUNNING_TIME, mRunningTime);
			if (mLastModified != null) {
				values.put(LAST_MODIFIED, mLastModified.getTimeInMillis());
			}
			values.put(RELEASE_DATE,
					mReleaseDate != null ? format.format(mReleaseDate) : "");
			values.put(THEATRICAL_DEBUT,
					mTheatricalDebut != null ? format.format(mTheatricalDebut)
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

			values.put(TAGS, TextUtilities.join(mTags, ", "));
			values.put(FORMAT, mFormat);
			values.put(GENRE, mGenre);
			values.put(AUDIENCE, mAudience);
			values.put(LANGUAGES, TextUtilities.join(mLanguage, ", "));
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

		public static Movie fromCursor(Cursor c) {
			final Movie movie = new Movie();

			movie.mInternalId = c.getString(
					c.getColumnIndexOrThrow(INTERNAL_ID)).substring(
					ServerInfo.NAME.length());
			movie.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
			movie.mIsbn = c.getString(c.getColumnIndexOrThrow(ISBN));
			movie.mUpc = c.getString(c.getColumnIndexOrThrow(UPC));
			movie.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));
			Collections
					.addAll(movie.mDirectors,
							c.getString(c.getColumnIndexOrThrow(DIRECTORS))
									.split(", "));
			Collections.addAll(movie.mActors,
					c.getString(c.getColumnIndexOrThrow(ACTORS)).split(", "));
			movie.mLabel = c.getString(c.getColumnIndexOrThrow(LABEL));
			movie.mDescriptions.add(new Description("", c.getString(c
					.getColumnIndexOrThrow(REVIEWS))));
			movie.mRunningTime = c.getString(c
					.getColumnIndexOrThrow(RUNNING_TIME));

			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(c.getLong(c
					.getColumnIndexOrThrow(LAST_MODIFIED)));
			movie.mLastModified = calendar;
			try {
				Collections.addAll(movie.mTags,
						c.getString(c.getColumnIndexOrThrow(TAGS)).split(", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
			try {
				movie.mReleaseDate = format.parse(c.getString(c
						.getColumnIndexOrThrow(RELEASE_DATE)));
			} catch (ParseException e) {
				// Ignore
			}

			try {
				movie.mTheatricalDebut = format.parse(c.getString(c
						.getColumnIndexOrThrow(THEATRICAL_DEBUT)));
			} catch (ParseException e) {
				// Ignore
			}

			movie.mDetailsUrl = TextUtilities.unprotectString(c.getString(c
					.getColumnIndexOrThrow(DETAILS_URL)));

			String tiny_url = c.getString(c.getColumnIndexOrThrow(TINY_URL));

			if (tiny_url != null) {
				tiny_url = TextUtilities.unprotectString(c.getString(c
						.getColumnIndexOrThrow(TINY_URL)));

				final int density = Preferences.getDPI();

				switch (density) {
				case 320:
					Movie.mImages.put(ImageSize.LARGE, tiny_url);
					Movie.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 240:
					Movie.mImages.put(ImageSize.MEDIUM, tiny_url);
					Movie.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				case 120:
					Movie.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					Movie.mImages.put(ImageSize.THUMBNAIL, tiny_url);
					break;
				case 160:
				default:
					Movie.mImages.put(ImageSize.TINY, tiny_url);
					Movie.mImages.put(ImageSize.THUMBNAIL,
							tiny_url.replace("zoom=1", "zoom=5"));
					break;
				}
			}

			// GJT: Added these for more details
			movie.mFormat = c.getString(c.getColumnIndexOrThrow(FORMAT));
			movie.mGenre = c.getString(c.getColumnIndexOrThrow(GENRE));
			movie.mAudience = c.getString(c.getColumnIndexOrThrow(AUDIENCE));
			try {
				Collections.addAll(
						movie.mLanguage,
						c.getString(c.getColumnIndexOrThrow(LANGUAGES)).split(
								", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}
			try {
				Collections.addAll(
						movie.mFeatures,
						c.getString(c.getColumnIndexOrThrow(FEATURES)).split(
								", "));
			} catch (Exception e) {
				// GJT: Ignore, probably null
			}

			movie.mRetailPrice = c.getString(c
					.getColumnIndexOrThrow(RETAIL_PRICE));
			movie.mRating = c.getInt(c.getColumnIndexOrThrow(RATING));

			movie.mLoanedTo = c.getString(c.getColumnIndex(LOANED_TO));
			String loanDate = c.getString(c.getColumnIndexOrThrow(LOAN_DATE));
			if (loanDate != null) {
				try {
					movie.mLoanDate = Preferences.getDateFormat().parse(
							loanDate);
				} catch (ParseException e) {
					// Ignore
				}
			}
			String wishlistDate = c.getString(c
					.getColumnIndexOrThrow(WISHLIST_DATE));
			if (wishlistDate != null) {
				try {
					movie.mWishlistDate = Preferences.getDateFormat().parse(
							wishlistDate);
				} catch (ParseException e) {
					// Ignore
				}
			}
			movie.mEventId = c.getInt(c.getColumnIndexOrThrow(EVENT_ID));
			movie.mNotes = c.getString(c.getColumnIndexOrThrow(NOTES));

			movie.mQuantity = c.getString(c.getColumnIndex(QUANTITY));

			return movie;
		}

		@Override
		public String toString() {
			return "Movie[ISBN=" + mIsbn + ", EAN=" + mEan + ", UPC=" + mUpc
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
			dest.writeStringList(mDirectors);
			dest.writeStringList(mTags);
		}

		public static final Creator<Movie> CREATOR = new Creator<Movie>() {
			public Movie createFromParcel(Parcel in) {
				return new Movie(in);
			}

			public Movie[] newArray(int size) {
				return new Movie[size];
			}
		};
	}

	/**
	 * Finds the movie with the specified id.
	 * 
	 * @param id
	 *            The id of the movie to find (ISBN-10, ISBN-13, etc.)
	 * @param mSavedImportType
	 * 
	 * @return A Movie instance if the movie was found or null otherwise.
	 */
	public Movie findMovie(String id,
			final IOUtilities.inputTypes mSavedImportType, Context context) {
		Uri.Builder uri = assembleURI(id, mSavedImportType, context,
				VALUE_SEARCHINDEX_MOVIES, RESPONSE_TAG_EAN);

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
		Movie movie = createMovie();
		movie = findMovieLookup(get, movie, mSavedImportType, id);

		if (movie != null) {
			return movie;
		}

		movie = createMovie();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_MOVIES, context,
				RESPONSE_TAG_ASIN);
		get = new HttpGet(uri.build().toString());
		movie = findMovieLookup(get, movie, mSavedImportType, id);

		if (movie != null) {
			return movie;
		}

		movie = createMovie();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_MOVIES, context,
				RESPONSE_TAG_SKU);
		get = new HttpGet(uri.build().toString());
		movie = findMovieLookup(get, movie, mSavedImportType, id);

		if (movie != null) {
			return movie;
		}

		movie = createMovie();
		uri = buildFindQuery(id, VALUE_SEARCHINDEX_MOVIES, context,
				RESPONSE_TAG_UPC);
		get = new HttpGet(uri.build().toString());
		movie = findMovieLookup(get, movie, mSavedImportType, id);

		if (movie != null) {
			return movie;
		} else {
			return null;
		}
	}

	private Movie findMovieLookup(HttpGet get, final Movie movie,
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
									result[0] = parseMovie(parser, movie);
								}
							}, mSavedImportType);
						}
					});
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not find " + mSavedImportType
					+ " item with ID " + id);
		}

		if (TextUtilities.isEmpty(movie.mEan) && id.length() == 13) {
			movie.mEan = id;
		} else if (TextUtilities.isEmpty(movie.mIsbn) && id.length() == 10) {
			movie.mIsbn = id;
		}

		return result[0] ? movie : null;
	}

	/**
	 * Searchs for movies that match the provided query.
	 * 
	 * @param query
	 *            The free form query used to search for movies.
	 * 
	 * @return A list of Movie instances if query was successful or null
	 *         otherwise.
	 */
	public ArrayList<Movie> searchMovies(String query, String page,
			final MovieSearchListener listener, Context context) {
		final Uri.Builder uri = buildSearchQuery(query,
				VALUE_SEARCHINDEX_MOVIES, page, context);
		final HttpGet get = new HttpGet(uri.build().toString());
		final ArrayList<Movie> movies = new ArrayList<Movie>(10);

		try {
			executeRequest(new HttpHost(mHost, 80, "http"), get,
					new ResponseHandler() {
						public void handleResponse(InputStream in)
								throws IOException {
							parseResponse(in, new ResponseParser() {
								public void parseResponse(XmlPullParser parser)
										throws XmlPullParserException,
										IOException {
									parseMovies(parser, movies, listener);
								}
							}, null);
						}
					});

			return movies;
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform search with query: "
					+ query, e);
		}

		return null;
	}

	/**
	 * Parses a movie from the XML input stream.
	 * 
	 * @param parser
	 *            The XML parser to use to parse the movie.
	 * @param movie
	 *            The movie object to put the parsed data in.
	 * 
	 * @return True if the movie could correctly be parsed, false otherwise.
	 */
	boolean parseMovie(XmlPullParser parser, Movie movie)
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
					if (movie.mInternalId == null)
						movie.mInternalId = parser.getText();
				}
			} else if (RESPONSE_TAG_DETAILPAGEURL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mDetailsUrl = parser.getText();
				}
			} else if (RESPONSE_TAG_IMAGESET.equals(name)) {
				if (RESPONSE_VALUE_CATEGORY_PRIMARY.equals(parser
						.getAttributeValue(null, RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, movie);
				} else if (Movie.mImages.get(ImageSize.THUMBNAIL) == null
						&& RESPONSE_VALUE_CATEGORY_VARIANT
								.equals(parser.getAttributeValue(null,
										RESPONSE_ATTR_CATEGORY))) {
					parseImageSet(parser, movie);
				}
			} else if (RESPONSE_TAG_ITEMATTRIBUTES.equals(name)) {
				parseItemAttributes(parser, movie);
			} else if (RESPONSE_TAG_LOWESTUSEDPRICE.equals(name)) {
				parsePrices(parser, movie);
			} else if (RESPONSE_TAG_EDITORIALREVIEW.equals(name)) {
				movie.mDescriptions.add(parseEditorialReview(parser));
			} else if (RESPONSE_TAG_ERRORS.equals(name)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Creates an instance of
	 * {@link com.miadzin.shelves.provider.movies.MoviesStore.Movie} with this
	 * movie store's name.
	 * 
	 * @return A new instance of Movie.
	 */
	Movie createMovie() {
		return new Movie();
	}

	private void parseMovies(XmlPullParser parser, ArrayList<Movie> movies,
			MovieSearchListener listener) throws IOException,
			XmlPullParserException {

		int type;
		while ((type = parser.next()) != XmlPullParser.END_TAG
				&& type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			if (findNextItem(parser)) {
				final Movie movie = createMovie();
				if (parseMovie(parser, movie)) {
					movies.add(movie);
					listener.onMovieFound(movie, movies);
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
	 * {@link com.miadzin.shelves.provider.movies.MoviesStore#searchMovies(String, com.miadzin.shelves.provider.movies.MoviesStore.MovieSearchListener)}
	 * .
	 */
	public interface MovieSearchListener {
		/**
		 * Invoked whenever a movie was found by the search operation.
		 * 
		 * @param movie
		 *            The movie yield by the search query.
		 * @param movies
		 *            The movies found so far, including <code>movie</code>.
		 */
		void onMovieFound(Movie movie, ArrayList<Movie> movies);
	}

	private void parseItemAttributes(XmlPullParser parser, Movie movie)
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
			if (RESPONSE_TAG_DIRECTOR.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mDirectors.add(parser.getText());
				}
			}
			if (RESPONSE_TAG_ACTOR.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mActors.add(parser.getText());
				}
			}
			if (RESPONSE_TAG_AUDIENCE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mAudience = parser.getText();
				}
			} else if (RESPONSE_TAG_EAN.equals(name)
					|| RESPONSE_TAG_SKU.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mEan = parser.getText();
				}
			} else if (RESPONSE_TAG_ISBN.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mIsbn = parser.getText();
				}
			} else if (RESPONSE_TAG_UPC.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mUpc = parser.getText();
				}
			} else if (RESPONSE_TAG_BINDING.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mFormat = parser.getText();
				}
			} else if (RESPONSE_TAG_FORMAT.equals(name)
					|| RESPONSE_TAG_RATIO.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mFeatures.add(parser.getText());
				}
			} else if (RESPONSE_TAG_RUNNING_TIME.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mRunningTime = parser.getText();
				}
			} else if (RESPONSE_TAG_RELEASEDATE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					final SimpleDateFormat format = new SimpleDateFormat(
							"yyyy-MM-dd");
					try {
						movie.mReleaseDate = format.parse(parser.getText());
					} catch (ParseException e) {
						// Ignore
					}
				}
			} else if (RESPONSE_TAG_THEATRICALDEBUT.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					final SimpleDateFormat format = new SimpleDateFormat(
							"yyyy-MM-dd");
					try {
						movie.mTheatricalDebut = format.parse(parser.getText());
					} catch (ParseException e) {
						// Ignore
					}
				}
			} else if (RESPONSE_TAG_TITLE.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mTitle = parser.getText();
				}
			} else if (RESPONSE_TAG_LABEL.equals(name)) {
				if (parser.next() == XmlPullParser.TEXT) {
					movie.mLabel = parser.getText();
				}
			} else if (RESPONSE_TAG_LANGUAGE.equals(name)) {
				movie.mLanguage.add(parseLanguage(parser));
			}
		}
	}

	private void parsePrices(XmlPullParser parser, Movie movie)
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
						if (movie.mRetailPrice == null
								|| (movie.mRetailPrice != null && Double
										.valueOf(movie.mRetailPrice
												.substring(1)) > Double
										.valueOf(price.substring(1)))) {
							movie.mRetailPrice = price;
						}
					} catch (NumberFormatException n) {
						Log.e(LOG_TAG, n.toString());
						if (movie.mRetailPrice == null)
							movie.mRetailPrice = "";
					}
				}
			}
		}
	}

	private void parseImageSet(XmlPullParser parser, Movie movie)
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
				Movie.mImages.put(ImageSize.TINY, parseImage(parser));
			} else if (RESPONSE_TAG_THUMBNAILIMAGE.equals(name)) {
				Movie.mImages.put(ImageSize.THUMBNAIL, parseImage(parser));
			} else if (RESPONSE_TAG_MEDIUMIMAGE.equals(name)) {
				Movie.mImages.put(ImageSize.MEDIUM, parseImage(parser));
			} else if (RESPONSE_TAG_LARGEIMAGE.equals(name)) {
				Movie.mImages.put(ImageSize.LARGE, parseImage(parser));
			}
		}
	}

}
