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

package com.miadzin.shelves.base;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.util.Log;

import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.activity.TagActivity;
import com.miadzin.shelves.server.ServerInfo;
import com.miadzin.shelves.util.IOUtilities;
import com.miadzin.shelves.util.TextUtilities;

public abstract class BaseItem {
	public final String LOG_TAG = "BaseItem";

	public enum ImageSize {
		// SWATCH,
		// SMALL,
		THUMBNAIL, TINY, MEDIUM, LARGE
	}

	// GJT: Added these, for sorting algorithms
	public static final String DEFAULT_SORT_ORDER = "sort_title ASC";

	public static final String TITLE_SORT_DESC = "sort_title DESC";
	public static final String AUTHOR_SORT_ASC = "authors ASC";
	public static final String AUTHOR_SORT_DESC = "authors DESC";
	public static final String RATING_SORT_ASC = "rating ASC";
	public static final String RATING_SORT_DESC = "rating DESC";

	public static final String _ID = "_id";
	public static final String INTERNAL_ID = "internal_id";
	public static final String EAN = "ean";
	public static final String ISBN = "isbn";
	public static final String UPC = "upc";
	public static final String TITLE = "title";
	public static final String SORT_TITLE = "sort_title";
	public static final String AUTHORS = "authors";
	public static final String PUBLISHER = "publisher";
	public static final String REVIEWS = "reviews";
	public static final String PAGES = "pages";
	public static final String LAST_MODIFIED = "last_modified";
	public static final String PUBLICATION = "publication";
	public static final String DETAILS_URL = "details_url";
	public static final String TINY_URL = "tiny_url";

	// GJT: Added these in v1.2, database 2
	public static final String TAGS = "tags";
	public static final String FORMAT = "format";
	public static final String CATEGORY = "category";
	public static final String EDITION = "edition";
	public static final String LANGUAGE = "language";
	public static final String DEWEY_NUMBER = "dewey_number";
	public static final String CONDITION = "condition";
	public static final String RETAIL_PRICE = "retail_price";
	public static final String RATING = "rating";

	// GJT: Added this in v2.2, database 4
	public static final String LOANED_TO = "loaned_to";
	public static final String LOAN_DATE = "loan_date";
	public static final String EVENT_ID = "event_id";
	public static final String NOTES = "notes";

	public static final String WISHLIST_DATE = "wishlist_date";

	public static final String QUANTITY = "quantity";

	public static final String DIRECTORS = "authors";
	public static final String ACTORS = "actors";
	public static final String LABEL = "label";
	public static final String RUNNING_TIME = "running_time";
	public static final String RELEASE_DATE = "release_date";
	public static final String THEATRICAL_DEBUT = "theatrical_debut";
	public static final String AUDIENCE = "audience";
	public static final String LANGUAGES = "languages";
	public static final String FEATURES = "features";
	public static final String GENRE = "genre";

	public static final String TRACKS = "tracks";

	public static final String FABRIC = "fabric";
	public static final String DEPARTMENT = "department";

	public static final String PLATFORM = "platform";

	public static final String ESRB = "esrb";

	public static final String OBJECTID = "objectid";
	public static final String MIN_PLAYERS = "min_players";
	public static final String MAX_PLAYERS = "max_players";
	public static final String PLAYING_TIME = "playing_time";
	public static final String AGE = "age";
	public static final String BGG_WISHLIST = "wishlist";

	public static final String ISSUE_NUMBER = "issue_number";
	public static final String CHARACTERS = "characters";
	public static final String ARTISTS = "artists";

	public String mIsbn;
	public String mEan;
	public String mUpc;
	public String mInternalId;
	public String mTitle;
	public String mDetailsUrl;
	public Calendar mLastModified;

	public List<String> mTags;
	public String mFormat;
	public String mAudience;
	public List<String> mLanguage;
	public List<String> mFeatures;
	public String mCondition;
	public String mRetailPrice;
	public int mRating;
	public String mLoanedTo;
	public Date mLoanDate;
	public int mEventId;
	public String mNotes;
	public String mLabel;
	public Date mReleaseDate;
	public String mGenre;
	public String mDepartment;

	public Date mWishlistDate;

	public String mQuantity;

	public String mStorePrefix;

	public final static long ONE_SECOND = 1000;
	public final static long ONE_MINUTE = ONE_SECOND * 60;
	public final static long ONE_DAY = ONE_MINUTE * 60 * 24;

	public String getIsbn() {
		return mIsbn;
	}

	public String getEan() {
		return mEan;
	}

	public String getUpc() {
		return mUpc;
	}

	public String getInternalId() {
		// GJT: Don't want to admit to using AWS--how about a different format?
		return ServerInfo.NAME + mInternalId;
	}

	public String getInternalIdNoPrefix() {
		return mInternalId;
	}

	public String getTitle() {
		return mTitle;
	}

	public String getDetailsUrl() {
		return TextUtilities.unprotectString(mDetailsUrl);
	}

	public String getSaneDetailsUrl() {
		return mDetailsUrl;
	}

	public List<String> getTags() {
		return mTags;
	}

	public String getFormat() {
		return mFormat;
	}

	public List<String> getLanguages() {
		return mLanguage;
	}

	public String getRetailPrice() {
		if (!TextUtilities.isEmpty(mRetailPrice))
			return mRetailPrice;
		else
			return " ";
	}

	public int getRating() {
		return mRating;
	}

	public Calendar getLastModified() {
		return mLastModified;
	}

	public String getLoanedTo() {
		return mLoanedTo;
	}

	public Date getLoanDate() {
		return mLoanDate;
	}

	public int getEventId() {
		return mEventId;
	}

	public Date getWishlistDate() {
		return mWishlistDate;
	}

	public String getNotes() {
		return mNotes;
	}

	public Date getReleaseDate() {
		return mReleaseDate;
	}

	public String getLabel() {
		return mLabel;
	}

	public String getGenre() {
		return mGenre;
	}

	public String getAudience() {
		return mAudience;
	}

	public List<String> getFeatures() {
		return mFeatures;
	}

	public String getDepartment() {
		return mDepartment;
	}

	public String getQuantity() {
		return mQuantity;
	}

	public abstract Bitmap loadCover(ImageSize size);

	public abstract String getImageUrl(ImageSize size);

	public void addInfo(ContentResolver contentResolver, String type,
			String title, String sort_title, String description, String tags,
			String rating, String notes, String loan_date, String loan_to,
			String event_id, String wishlist) {
		ContentValues contentValues = new ContentValues();
		StringBuilder tagSB = new StringBuilder();

		if (!TextUtilities.isEmpty(tags) && !tags.equals(IOUtilities.NO_OP)) {
			String[] tagArray = tags.split(",");
			if (tagArray.length == 1) {
				contentValues.put(BaseItem.TAGS, tagArray[0]);
			} else {
				for (int i = 0; i < tagArray.length && i < TagActivity.MAX_TAGS; i++) {
					if (!TextUtilities.isEmpty(tagArray[i])) {
						if (tagArray[i].length() > TagActivity.MAX_TAG_LENGTH) {
							tagArray[i] = tagArray[i].substring(0,
									TagActivity.MAX_TAG_LENGTH);
						}
						tagSB.append(tagArray[i].trim()).append(",");
					}
				}

				if (tagSB.length() > 0) {
					contentValues.put(BaseItem.TAGS,
							tagSB.deleteCharAt(tagSB.length() - 1).toString());
				} else {
					contentValues.put(BaseItem.TAGS,
							tagSB.deleteCharAt(tagSB.length()).toString());
				}
			}
		} else
			contentValues.put(BaseItem.TAGS, "");

		try {
			if (!TextUtilities.isEmpty(rating)) {
				int rate = Integer.valueOf(rating);

				if (0 <= rate && rate <= 5) {
					contentValues.put(BaseItem.RATING, Integer.valueOf(rating));
				} else {
					contentValues.put(BaseItem.RATING, 0);
				}
			} else {
				contentValues.put(BaseItem.RATING, 0);
			}
		} catch (NumberFormatException nfe) {
			Log.e(LOG_TAG, rating + ": " + nfe.toString());
		}
		if (!TextUtilities.isEmpty(title) && !title.equals(IOUtilities.NO_OP)) {
			contentValues.put(BaseItem.TITLE, title);
		}

		if (!TextUtilities.isEmpty(sort_title)
				&& !sort_title.equals(IOUtilities.NO_OP)) {
			contentValues.put(BaseItem.SORT_TITLE, sort_title);
		}

		if (!TextUtilities.isEmpty(description)
				&& !description.equals(IOUtilities.NO_OP)) {
			contentValues.put(BaseItem.REVIEWS, description);
		}

		if (!TextUtilities.isEmpty(notes) && !notes.equals(IOUtilities.NO_OP)) {
			contentValues.put(BaseItem.NOTES, notes);
		} else
			contentValues.put(BaseItem.NOTES, "");

		if (!TextUtilities.isEmpty(loan_date)
				&& !loan_date.equals(IOUtilities.NO_OP)) {
			contentValues.put(BaseItem.LOAN_DATE, loan_date);
		}

		if (!TextUtilities.isEmpty(loan_to)
				&& !loan_to.equals(IOUtilities.NO_OP)) {
			contentValues.put(BaseItem.LOANED_TO, loan_to);
		}

		if (!TextUtilities.isEmpty(event_id)
				&& !event_id.equals(IOUtilities.NO_OP)) {
			contentValues.put(BaseItem.EVENT_ID, event_id);
		}

		if (!TextUtilities.isEmpty(wishlist)
				&& !wishlist.equals(IOUtilities.NO_OP)) {
			contentValues.put(BaseItem.WISHLIST_DATE, wishlist);
		}

		try {
			contentResolver.update(ShelvesApplication.TYPES_TO_URI.get(type),
					contentValues, BaseItem.INTERNAL_ID + "=?",
					new String[] { ServerInfo.NAME + mInternalId });
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}
	}
}