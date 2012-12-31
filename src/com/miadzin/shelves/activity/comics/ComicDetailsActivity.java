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

package com.miadzin.shelves.activity.comics;

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
import com.miadzin.shelves.provider.comics.ComicsManager;
import com.miadzin.shelves.provider.comics.ComicsStore;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class ComicDetailsActivity extends BaseDetailsActivity {
	private static final String EXTRA_COMIC = "shelves.extra.comic_id";

	private ComicsStore.Comic mComic;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mComic = getComic();
		if (mComic == null)
			finish();

		mContext = getString(R.string.comic_label_plural_small);
		mPrice = mComic.getRetailPrice();
		mDetailsUrl = mComic.getDetailsUrl();
		mID = mComic.getInternalId();

		setupViews();
	}

	private ComicsStore.Comic getComic() {
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				return ComicsManager.findComic(getContentResolver(),
						intent.getData(), SettingsActivity.getSortOrder(this));
			} else {
				final String comicId = intent.getStringExtra(EXTRA_COMIC);
				if (comicId != null) {
					return ComicsManager.findComic(getContentResolver(),
							comicId, SettingsActivity.getSortOrder(this));
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
				mComic.getInternalId(), defaultCover));

		setTextOrHide(R.id.label_title, mComic.getTitle());

		if (!TextUtilities.isEmpty(mComic.getLoanedTo())) {
			findViewById(R.id.label_loaned).setVisibility(View.VISIBLE);
			setTextOrHide((TextView) findViewById(R.id.loaned),
					mComic.getLoanedTo());
		}

		setTextOrHide(R.id.label_author, mComic.getAuthors());

		final Date publicationDate = mComic.getPublicationDate();
		if (publicationDate != null) {
			final String date = new SimpleDateFormat("MMMM yyyy")
					.format(publicationDate);
			((TextView) findViewById(R.id.label_date)).setText(date);
		} else {
			findViewById(R.id.label_date).setVisibility(View.GONE);
		}

		setGestures();

		((TextView) findViewById(R.id.html_reviews)).setText(mComic
				.getDescriptions());

		List<String> tagList = mComic.getTags();
		List<String> artistList = mComic.getArtists();
		List<String> characterList = mComic.getCharacters();
		String issueNumber = mComic.getIssueNumber();
		String ean = mComic.getEan();
		String upc = mComic.getUpc();

		if (!TextUtilities.isEmpty(tagList)) {
			String tags = TextUtilities.join(tagList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.tags), tags))
				findViewById(R.id.label_tags).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(artistList)) {
			String artists = TextUtilities.join(artistList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.artists), artists))
				findViewById(R.id.label_artists).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(characterList)) {
			String characters = TextUtilities.join(characterList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.characters),
					characters))
				findViewById(R.id.label_characters).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(issueNumber)) {
			if (setTextOrHide((TextView) findViewById(R.id.issue), issueNumber))
				findViewById(R.id.label_issue).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(ean)) {
			if (setTextOrHide((TextView) findViewById(R.id.ean), ean))
				findViewById(R.id.label_ean).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(upc)) {
			if (setTextOrHide((TextView) findViewById(R.id.upc), upc))
				findViewById(R.id.label_upc).setVisibility(View.VISIBLE);
		}

		((TextView) findViewById(R.id.notes)).setText(mComic.getNotes());

		UIUtilities.showIdentificationDots(
				(TextView) findViewById(R.id.identification_dots),
				viewFlipper.getDisplayedChild());

		postSetupViews();
	}

	static void show(Context context, String comicId) {
		final Intent intent = new Intent(context, ComicDetailsActivity.class);
		intent.putExtra(EXTRA_COMIC, comicId);
		context.startActivity(intent);
	}

	public static void showFromOutside(Context context, String comicId) {
		final Intent intent = new Intent(context, ComicDetailsActivity.class);
		intent.putExtra(EXTRA_COMIC, comicId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
