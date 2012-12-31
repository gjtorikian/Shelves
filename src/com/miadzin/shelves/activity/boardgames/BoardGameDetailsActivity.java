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

package com.miadzin.shelves.activity.boardgames;

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
import com.miadzin.shelves.provider.boardgames.BoardGamesManager;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class BoardGameDetailsActivity extends BaseDetailsActivity {
	private static final String EXTRA_BOARDGAME = "shelves.extra.boardgame_id";

	private BoardGamesStore.BoardGame mBoardGame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBoardGame = getBoardGame();
		if (mBoardGame == null)
			finish();

		mContext = getString(R.string.boardgame_label_plural_small);
		mDetailsUrl = mBoardGame.getSaneDetailsUrl();
		mID = mBoardGame.getInternalId();

		setupViews();
	}

	private BoardGamesStore.BoardGame getBoardGame() {
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				return BoardGamesManager.findBoardGame(getContentResolver(),
						intent.getData(), SettingsActivity.getSortOrder(this));
			} else {
				final String boardgameId = intent
						.getStringExtra(EXTRA_BOARDGAME);
				if (boardgameId != null) {
					return BoardGamesManager.findBoardGame(
							getContentResolver(), boardgameId,
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
				mBoardGame.getInternalId(), defaultCover));

		setTextOrHide(R.id.label_title, mBoardGame.getTitle());

		if (!TextUtilities.isEmpty(mBoardGame.getLoanedTo())) {
			findViewById(R.id.label_loaned).setVisibility(View.VISIBLE);
			setTextOrHide((TextView) findViewById(R.id.loaned),
					mBoardGame.getLoanedTo());
		}

		setTextOrHide(R.id.label_author, mBoardGame.getAuthor());

		setTextOrHide(R.id.label_date, mBoardGame.getPublicationDate());

		setGestures();

		((TextView) findViewById(R.id.html_reviews)).setText(mBoardGame
				.getDescriptions());
		/*
		 * ((TextView) findViewById(R.id.html_reviews))
		 * .setOnTouchListener(gestureListener); ((TextView)
		 * findViewById(R.id.notes)) .setOnTouchListener(gestureListener);
		 * 
		 * ((FrameLayout) findViewById(R.id.detail_background))
		 * .setOnTouchListener(gestureListener);
		 */

		List<String> tagList = mBoardGame.getTags();
		String minPlayers = mBoardGame.getMinPlayers();
		String maxPlayers = mBoardGame.getMaxPlayers();
		String age = mBoardGame.getAge();
		String playingTime = mBoardGame.getPlayingTime();

		String ean = mBoardGame.getEan();

		if (!TextUtilities.isEmpty(tagList)) {
			String tags = TextUtilities.join(tagList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.tags), tags))
				findViewById(R.id.label_tags).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(minPlayers)) {
			if (setTextOrHide((TextView) findViewById(R.id.minPlayers),
					minPlayers))
				findViewById(R.id.label_minPlayers).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(maxPlayers)) {
			if (setTextOrHide((TextView) findViewById(R.id.maxPlayers),
					maxPlayers))
				findViewById(R.id.label_maxPlayers).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(age)) {
			if (setTextOrHide((TextView) findViewById(R.id.age), age))
				findViewById(R.id.label_age).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(playingTime)) {
			if (setTextOrHide((TextView) findViewById(R.id.playingTime),
					playingTime))
				findViewById(R.id.label_playingTime)
						.setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(ean)) {
			if (setTextOrHide((TextView) findViewById(R.id.ean), ean))
				findViewById(R.id.label_ean).setVisibility(View.VISIBLE);
		}

		((TextView) findViewById(R.id.notes)).setText(mBoardGame.getNotes());

		UIUtilities.showIdentificationDots(
				(TextView) findViewById(R.id.identification_dots),
				viewFlipper.getDisplayedChild());

		postSetupViews();
	}

	static void show(Context context, String boardgameId) {
		final Intent intent = new Intent(context,
				BoardGameDetailsActivity.class);
		intent.putExtra(EXTRA_BOARDGAME, boardgameId);
		context.startActivity(intent);
	}

	public static void showFromOutside(Context context, String boardgameId) {
		final Intent intent = new Intent(context,
				BoardGameDetailsActivity.class);
		intent.putExtra(EXTRA_BOARDGAME, boardgameId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
