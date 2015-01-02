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

package com.miadzin.shelves.activity.gadgets;

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
import com.miadzin.shelves.provider.gadgets.GadgetsManager;
import com.miadzin.shelves.provider.gadgets.GadgetsStore;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class GadgetDetailsActivity extends BaseDetailsActivity {
	private static final String EXTRA_GADGET = "shelves.extra.gadget_id";

	private GadgetsStore.Gadget mGadget;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGadget = getGadget();
		if (mGadget == null)
			finish();

		mContext = getString(R.string.gadget_label_plural_small);
		mPrice = mGadget.getRetailPrice();
		mDetailsUrl = mGadget.getDetailsUrl();
		mID = mGadget.getInternalId();

		setupViews();
	}

	private GadgetsStore.Gadget getGadget() {
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				return GadgetsManager.findGadget(getContentResolver(),
						intent.getData(), SettingsActivity.getSortOrder(this));
			} else {
				final String gadgetId = intent.getStringExtra(EXTRA_GADGET);
				if (gadgetId != null) {
					return GadgetsManager.findGadget(getContentResolver(),
							gadgetId, SettingsActivity.getSortOrder(this));
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
				mGadget.getInternalId(), defaultCover));

		setTextOrHide(R.id.label_title, mGadget.getTitle());

		if (!TextUtilities.isEmpty(mGadget.getLoanedTo())) {
			findViewById(R.id.label_loaned).setVisibility(View.VISIBLE);
			setTextOrHide((TextView) findViewById(R.id.loaned),
					mGadget.getLoanedTo());
		}

		setTextOrHide(R.id.label_author, mGadget.getAuthors());

		findViewById(R.id.label_pages).setVisibility(View.GONE);

		final Date releaseDate = mGadget.getReleaseDate();
		if (releaseDate != null) {
			final String date = new SimpleDateFormat("MMMM yyyy")
					.format(releaseDate);
			((TextView) findViewById(R.id.label_date)).setText(date);
		} else {
			findViewById(R.id.label_date).setVisibility(View.GONE);
		}

		setGestures();

		((TextView) findViewById(R.id.html_reviews)).setText(mGadget
				.getDescriptions().get(0).toString());

		List<String> tagList = mGadget.getTags();
		List<String> featureList = mGadget.getFeatures();
		String ean = mGadget.getEan();
		String isbn = mGadget.getIsbn();
		String upc = mGadget.getUpc();

		if (!TextUtilities.isEmpty(tagList)) {
			String tags = TextUtilities.join(tagList, ", ");
			if (setTextOrHide((TextView) findViewById(R.id.tags), tags))
				findViewById(R.id.label_tags).setVisibility(View.VISIBLE);
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

		if (!TextUtilities.isEmpty(isbn)) {
			if (setTextOrHide((TextView) findViewById(R.id.isbn), isbn))
				findViewById(R.id.label_isbn).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(upc)) {
			if (setTextOrHide((TextView) findViewById(R.id.upc), upc))
				findViewById(R.id.label_upc).setVisibility(View.VISIBLE);
		}

		((TextView) findViewById(R.id.notes)).setText(mGadget.getNotes());

		UIUtilities.showIdentificationDots(
				(TextView) findViewById(R.id.identification_dots),
				viewFlipper.getDisplayedChild());

		postSetupViews();
	}

	static void show(Context context, String gadgetId) {
		final Intent intent = new Intent(context, GadgetDetailsActivity.class);
		intent.putExtra(EXTRA_GADGET, gadgetId);
		context.startActivity(intent);
	}

	public static void showFromOutside(Context context, String gadgetId) {
		final Intent intent = new Intent(context, GadgetDetailsActivity.class);
		intent.putExtra(EXTRA_GADGET, gadgetId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
