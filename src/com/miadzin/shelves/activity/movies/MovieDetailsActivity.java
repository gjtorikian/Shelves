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

package com.miadzin.shelves.activity.movies;

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
import com.miadzin.shelves.provider.movies.MoviesManager;
import com.miadzin.shelves.provider.movies.MoviesStore;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class MovieDetailsActivity extends BaseDetailsActivity {
	private static final String EXTRA_MOVIE = "shelves.extra.movie_id";

	private MoviesStore.Movie mMovie;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMovie = getMovie();
		if (mMovie == null)
			finish();

		mContext = getString(R.string.movie_label_plural_small);
		mPrice = mMovie.getRetailPrice();
		mDetailsUrl = mMovie.getDetailsUrl();
		mID = mMovie.getInternalId();

		setupViews();
	}

	private MoviesStore.Movie getMovie() {
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				return MoviesManager.findMovie(getContentResolver(),
						intent.getData(), SettingsActivity.getSortOrder(this));
			} else {
				final String movieId = intent.getStringExtra(EXTRA_MOVIE);
				if (movieId != null) {
					return MoviesManager.findMovie(getContentResolver(),
							movieId, SettingsActivity.getSortOrder(this));
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
				mMovie.getInternalId(), defaultCover));

		setTextOrHide(R.id.label_title, mMovie.getTitle());

		if (!TextUtilities.isEmpty(mMovie.getLoanedTo())) {
			findViewById(R.id.label_loaned).setVisibility(View.VISIBLE);
			setTextOrHide((TextView) findViewById(R.id.loaned),
					mMovie.getLoanedTo());
		}

		setTextOrHide(R.id.label_author,
				TextUtilities.joinAuthors(mMovie.getDirectors(), ", "));

		final String runningTime = mMovie.getRunningTime();
		if (runningTime != null && !runningTime.equals("")) {
			((TextView) findViewById(R.id.label_pages)).setText(getString(
					R.string.label_minutes, runningTime));
		} else {
			findViewById(R.id.label_pages).setVisibility(View.GONE);
		}

		final Date releaseDate = mMovie.getReleaseDate();
		if (releaseDate != null) {
			final String date = new SimpleDateFormat("MMMM yyyy")
					.format(releaseDate);
			((TextView) findViewById(R.id.label_date)).setText(date);
		} else {
			findViewById(R.id.label_date).setVisibility(View.GONE);
		}

		setTextOrHide(R.id.label_publisher, mMovie.getLabel());

		setGestures();

		((TextView) findViewById(R.id.html_reviews)).setText(mMovie
				.getDescriptions().get(0).toString());

		List<String> tagList = mMovie.getTags();
		String format = mMovie.getFormat();
		List<String> actorList = mMovie.getActors();
		String audience = mMovie.getAudience();
		List<String> featureList = mMovie.getFeatures();
		List<String> languageList = mMovie.getLanguages();
		String ean = mMovie.getEan();
		String isbn = mMovie.getIsbn();
		String upc = mMovie.getUpc();
		Date theatricalDebut = mMovie.getTheatricalDebut();

		if (!TextUtilities.isEmpty(tagList)) {
			String tags = TextUtilities.join(tagList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.tags), tags))
				findViewById(R.id.label_tags).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(format)) {
			if (setTextOrHide((TextView) findViewById(R.id.format), format))
				findViewById(R.id.label_format).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(actorList)) {
			String actors = TextUtilities.join(actorList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.actors), actors))
				findViewById(R.id.label_actors).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(audience)) {
			if (setTextOrHide((TextView) findViewById(R.id.audience), audience))
				findViewById(R.id.label_audience).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(featureList)) {
			String features = TextUtilities.join(featureList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.features), features))
				findViewById(R.id.label_features).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(languageList)) {
			String langs = TextUtilities.join(languageList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.languages), langs))
				findViewById(R.id.label_languages).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(audience)) {
			if (setTextOrHide((TextView) findViewById(R.id.audience), audience))
				findViewById(R.id.label_audience).setVisibility(View.VISIBLE);
		}

		if (theatricalDebut != null) {
			final String date = new SimpleDateFormat("MMMM yyyy")
					.format(theatricalDebut);
			if (setTextOrHide((TextView) findViewById(R.id.theatrical_debut),
					date))
				findViewById(R.id.label_theatrical_debut).setVisibility(
						View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(ean)) {
			if (setTextOrHide((TextView) findViewById(R.id.ean), ean))
				findViewById(R.id.label_ean).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(isbn)) {
			if (setTextOrHide((TextView) findViewById(R.id.isbn), isbn))
				findViewById(R.id.label_isbn).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(upc)) {
			if (setTextOrHide((TextView) findViewById(R.id.upc), upc))
				findViewById(R.id.label_upc).setVisibility(View.VISIBLE);
		}

		((TextView) findViewById(R.id.notes)).setText(mMovie.getNotes());

		UIUtilities.showIdentificationDots(
				(TextView) findViewById(R.id.identification_dots),
				viewFlipper.getDisplayedChild());

		postSetupViews();
	}

	static void show(Context context, String movieId) {
		final Intent intent = new Intent(context, MovieDetailsActivity.class);
		intent.putExtra(EXTRA_MOVIE, movieId);
		context.startActivity(intent);
	}

	public static void showFromOutside(Context context, String movieId) {
		final Intent intent = new Intent(context, MovieDetailsActivity.class);
		intent.putExtra(EXTRA_MOVIE, movieId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
