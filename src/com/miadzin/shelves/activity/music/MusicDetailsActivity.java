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

package com.miadzin.shelves.activity.music;

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
import com.miadzin.shelves.provider.music.MusicManager;
import com.miadzin.shelves.provider.music.MusicStore;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class MusicDetailsActivity extends BaseDetailsActivity {
	private static final String EXTRA_MUSIC = "shelves.extra.music_id";

	private MusicStore.Music mMusic;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMusic = getMusic();
		if (mMusic == null)
			finish();

		mContext = getString(R.string.music_label);
		mPrice = mMusic.getRetailPrice();
		mDetailsUrl = mMusic.getDetailsUrl();
		mID = mMusic.getInternalId();

		setupViews();
	}

	private MusicStore.Music getMusic() {
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				return MusicManager.findMusic(getContentResolver(),
						intent.getData(), SettingsActivity.getSortOrder(this));
			} else {
				final String musicId = intent.getStringExtra(EXTRA_MUSIC);
				if (musicId != null) {
					return MusicManager.findMusic(getContentResolver(),
							musicId, SettingsActivity.getSortOrder(this));
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
				mMusic.getInternalId(), defaultCover));

		setTextOrHide(R.id.label_title, mMusic.getTitle());

		if (!TextUtilities.isEmpty(mMusic.getLoanedTo())) {
			findViewById(R.id.label_loaned).setVisibility(View.VISIBLE);
			setTextOrHide((TextView) findViewById(R.id.loaned),
					mMusic.getLoanedTo());
		}

		setTextOrHide(R.id.label_author,
				TextUtilities.join(mMusic.getAuthors(), ", "));

		findViewById(R.id.label_pages).setVisibility(View.GONE);

		final Date releaseDate = mMusic.getReleaseDate();
		if (releaseDate != null) {
			final String date = new SimpleDateFormat("MMMM yyyy")
					.format(releaseDate);
			((TextView) findViewById(R.id.label_date)).setText(date);
		} else {
			findViewById(R.id.label_date).setVisibility(View.GONE);
		}

		setTextOrHide(R.id.label_publisher, mMusic.getLabel());

		setGestures();

		((TextView) findViewById(R.id.html_reviews)).setText(mMusic
				.getDescriptions().get(0).toString());

		List<String> tagList = mMusic.getTags();
		String format = mMusic.getFormat();
		String ean = mMusic.getEan();
		String isbn = mMusic.getIsbn();
		String upc = mMusic.getUpc();
		List<String> trackList = mMusic.getTracks();

		if (!TextUtilities.isEmpty(tagList)) {
			String tags = TextUtilities.join(tagList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.tags), tags))
				findViewById(R.id.label_tags).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(format)) {
			if (setTextOrHide((TextView) findViewById(R.id.format), format))
				findViewById(R.id.label_format).setVisibility(View.VISIBLE);
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

		if (!TextUtilities.isEmpty(trackList)) {
			String tracks = trackList.toString();
			tracks = tracks.replace("|", "\n").replace(", #", "\n    #")
					.replace("[Disc", "Disc").replace("\n]", "")
					.replace(", Disc", "Disc").replace(", ,", "")
					.replace(", \n", "").replace("Disc", "\nDisc");
			if (setTextOrHide((TextView) findViewById(R.id.tracks), tracks))
				findViewById(R.id.label_tracks).setVisibility(View.VISIBLE);
		}

		((TextView) findViewById(R.id.notes)).setText(mMusic.getNotes());

		UIUtilities.showIdentificationDots(
				(TextView) findViewById(R.id.identification_dots),
				viewFlipper.getDisplayedChild());

		postSetupViews();
	}

	static void show(Context context, String musicId) {
		final Intent intent = new Intent(context, MusicDetailsActivity.class);
		intent.putExtra(EXTRA_MUSIC, musicId);
		context.startActivity(intent);
	}

	public static void showFromOutside(Context context, String musicId) {
		final Intent intent = new Intent(context, MusicDetailsActivity.class);
		intent.putExtra(EXTRA_MUSIC, musicId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
