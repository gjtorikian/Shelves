/*
 * Copyright (C) 2008 Romain Guy
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

package com.miadzin.shelves.activity.books;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.miadzin.shelves.R;
import com.miadzin.shelves.activity.SettingsActivity;
import com.miadzin.shelves.base.BaseDetailsActivity;
import com.miadzin.shelves.drawable.FastBitmapDrawable;
import com.miadzin.shelves.provider.books.BooksManager;
import com.miadzin.shelves.provider.books.BooksStore;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class BookDetailsActivity extends BaseDetailsActivity {
	private static final String EXTRA_BOOK = "shelves.extra.book_id";

	private BooksStore.Book mBook;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBook = getBook();
		if (mBook == null)
			finish();

		mContext = getString(R.string.book_label_plural_small);
		mPrice = mBook.getRetailPrice();
		mDetailsUrl = mBook.getDetailsUrl();
		mID = mBook.getInternalId();

		setupViews();
	}

	private BooksStore.Book getBook() {
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				return BooksManager.findBook(getContentResolver(),
						intent.getData(), SettingsActivity.getSortOrder(this));
			} else {
				final String bookId = intent.getStringExtra(EXTRA_BOOK);
				if (bookId != null) {
					return BooksManager.findBook(getContentResolver(), bookId,
							SettingsActivity.getSortOrder(this));
				}
			}
		}
		return null;
	}

	@Override
	protected void setupViews() {
		final FastBitmapDrawable defaultCover = new FastBitmapDrawable(
				BitmapFactory.decodeResource(getResources(),
						R.drawable.unknown_cover));

		final ImageView cover = (ImageView) findViewById(R.id.image_cover);
		cover.setImageDrawable(ImageUtilities.getCachedCover(
				mBook.getInternalId(), defaultCover));

		setTextOrHide(R.id.label_title, mBook.getTitle());

		if (!TextUtilities.isEmpty(mBook.getLoanedTo())) {
			findViewById(R.id.label_loaned).setVisibility(View.VISIBLE);
			setTextOrHide((TextView) findViewById(R.id.loaned),
					mBook.getLoanedTo());
		}

		setTextOrHide(R.id.label_author,
				TextUtilities.joinAuthors(mBook.getAuthors(), ", "));

		final int pages = mBook.getPagesCount();
		if (pages > 0) {
			((TextView) findViewById(R.id.label_pages)).setText(getString(
					R.string.label_pages, pages));
		} else {
			findViewById(R.id.label_pages).setVisibility(View.GONE);
		}

		final Date publicationDate = mBook.getPublicationDate();
		if (publicationDate != null) {
			final String date = new SimpleDateFormat("MMMM yyyy")
					.format(publicationDate);
			((TextView) findViewById(R.id.label_date)).setText(date);
		} else {
			findViewById(R.id.label_date).setVisibility(View.GONE);
		}

		setTextOrHide(R.id.label_publisher, mBook.getPublisher());

		setGestures();

		((TextView) findViewById(R.id.html_reviews)).setText(mBook
				.getDescriptions().get(0).toString());
		/*
		 * ((TextView) findViewById(R.id.html_reviews))
		 * .setOnTouchListener(gestureListener); ((TextView)
		 * findViewById(R.id.notes)) .setOnTouchListener(gestureListener);
		 * 
		 * ((FrameLayout) findViewById(R.id.detail_background))
		 * .setOnTouchListener(gestureListener);
		 */

		List<String> tagList = mBook.getTags();
		String format = mBook.getFormat();
		String category = mBook.getCategory();
		String edition = mBook.getEdition();
		List<String> languageList = mBook.getLanguages();
		String ean = mBook.getEan();
		String isbn = mBook.getIsbn();
		String dewey = mBook.getDeweyNumber();
		String upc = mBook.getUpc();

		if (!TextUtilities.isEmpty(tagList)) {
			String tags = TextUtilities.join(tagList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.tags), tags))
				findViewById(R.id.label_tags).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(format)) {
			if (setTextOrHide((TextView) findViewById(R.id.format), format))
				findViewById(R.id.label_format).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(category)) {
			if (setTextOrHide((TextView) findViewById(R.id.category), category))
				findViewById(R.id.label_category).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(edition)) {
			if (setTextOrHide((TextView) findViewById(R.id.edition), edition))
				findViewById(R.id.label_edition).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(languageList)) {
			String langs = TextUtilities.join(languageList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.languages), langs))
				findViewById(R.id.label_languages).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(isbn)) {
			if (setTextOrHide((TextView) findViewById(R.id.isbn), isbn))
				findViewById(R.id.label_isbn).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(dewey)) {
			if (setTextOrHide((TextView) findViewById(R.id.dewey), dewey))
				findViewById(R.id.label_dewey).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(ean)) {
			if (setTextOrHide((TextView) findViewById(R.id.ean), ean))
				findViewById(R.id.label_ean).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(upc)) {
			if (setTextOrHide((TextView) findViewById(R.id.upc), upc))
				findViewById(R.id.label_upc).setVisibility(View.VISIBLE);
		}

		((TextView) findViewById(R.id.notes)).setText(mBook.getNotes());

		UIUtilities.showIdentificationDots(
				(TextView) findViewById(R.id.identification_dots),
				viewFlipper.getDisplayedChild());

		postSetupViews();
	}

	static void show(Context context, String bookId) {
		final Intent intent = new Intent(context, BookDetailsActivity.class);
		intent.putExtra(EXTRA_BOOK, bookId);
		context.startActivity(intent);
	}

	public static void showFromOutside(Context context, String bookId) {
		final Intent intent = new Intent(context, BookDetailsActivity.class);
		intent.putExtra(EXTRA_BOOK, bookId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
