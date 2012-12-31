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

package com.miadzin.shelves.activity.videogames;

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
import com.miadzin.shelves.provider.videogames.VideoGamesManager;
import com.miadzin.shelves.provider.videogames.VideoGamesStore;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class VideoGameDetailsActivity extends BaseDetailsActivity {
	private static final String EXTRA_VIDEOGAME = "shelves.extra.videogame_id";

	private VideoGamesStore.VideoGame mVideoGame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mVideoGame = getVideoGame();
		if (mVideoGame == null)
			finish();

		mContext = getString(R.string.videogame_label_plural_small);
		mPrice = mVideoGame.getRetailPrice();
		mDetailsUrl = mVideoGame.getDetailsUrl();
		mID = mVideoGame.getInternalId();

		setupViews();
	}

	private VideoGamesStore.VideoGame getVideoGame() {
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				return VideoGamesManager.findVideoGame(getContentResolver(),
						intent.getData(), SettingsActivity.getSortOrder(this));
			} else {
				final String videogameId = intent
						.getStringExtra(EXTRA_VIDEOGAME);
				if (videogameId != null) {
					return VideoGamesManager.findVideoGame(
							getContentResolver(), videogameId,
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
				mVideoGame.getInternalId(), defaultCover));

		setTextOrHide(R.id.label_title, mVideoGame.getTitle());

		if (!TextUtilities.isEmpty(mVideoGame.getLoanedTo())) {
			findViewById(R.id.label_loaned).setVisibility(View.VISIBLE);
			setTextOrHide((TextView) findViewById(R.id.loaned),
					mVideoGame.getLoanedTo());
		}

		setTextOrHide(R.id.label_author, mVideoGame.getAuthors());

		findViewById(R.id.label_pages).setVisibility(View.GONE);

		final Date releaseDate = mVideoGame.getReleaseDate();
		if (releaseDate != null) {
			final String date = new SimpleDateFormat("MMMM yyyy")
					.format(releaseDate);
			((TextView) findViewById(R.id.label_date)).setText(date);
		} else {
			findViewById(R.id.label_date).setVisibility(View.GONE);
		}

		setGestures();

		((TextView) findViewById(R.id.html_reviews)).setText(mVideoGame
				.getDescriptions().get(0).toString());

		List<String> tagList = mVideoGame.getTags();
		String esrb = mVideoGame.getEsrb();
		String genre = mVideoGame.getGenre();
		String platform = mVideoGame.getPlatform();
		List<String> featureList = mVideoGame.getFeatures();
		String ean = mVideoGame.getEan();
		String upc = mVideoGame.getUpc();

		if (!TextUtilities.isEmpty(tagList)) {
			String tags = TextUtilities.join(tagList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.tags), tags))
				findViewById(R.id.label_tags).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(platform)) {
			if (setTextOrHide((TextView) findViewById(R.id.platform), platform))
				findViewById(R.id.label_platform).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(esrb)) {
			if (setTextOrHide((TextView) findViewById(R.id.audience), esrb))
				findViewById(R.id.label_audience).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(genre)) {
			if (setTextOrHide((TextView) findViewById(R.id.genre), genre))
				findViewById(R.id.label_genre).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(featureList)) {
			String features = TextUtilities.join(featureList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.features), features))
				findViewById(R.id.label_features).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(ean)) {
			if (setTextOrHide((TextView) findViewById(R.id.ean), ean))
				findViewById(R.id.label_ean).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(upc)) {
			if (setTextOrHide((TextView) findViewById(R.id.upc), upc))
				findViewById(R.id.label_upc).setVisibility(View.VISIBLE);
		}

		((TextView) findViewById(R.id.notes)).setText(mVideoGame.getNotes());

		UIUtilities.showIdentificationDots(
				(TextView) findViewById(R.id.identification_dots),
				viewFlipper.getDisplayedChild());

		postSetupViews();
	}

	static void show(Context context, String videogameId) {
		final Intent intent = new Intent(context,
				VideoGameDetailsActivity.class);
		intent.putExtra(EXTRA_VIDEOGAME, videogameId);
		context.startActivity(intent);
	}

	public static void showFromOutside(Context context, String videogameId) {
		final Intent intent = new Intent(context,
				VideoGameDetailsActivity.class);
		intent.putExtra(EXTRA_VIDEOGAME, videogameId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
