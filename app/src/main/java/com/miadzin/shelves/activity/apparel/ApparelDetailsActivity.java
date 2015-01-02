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

package com.miadzin.shelves.activity.apparel;

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
import com.miadzin.shelves.provider.apparel.ApparelManager;
import com.miadzin.shelves.provider.apparel.ApparelStore;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class ApparelDetailsActivity extends BaseDetailsActivity {
	private static final String EXTRA_APPAREL = "shelves.extra.apparel_id";

	private ApparelStore.Apparel mApparel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mApparel = getApparel();
		if (mApparel == null)
			finish();

		mContext = getString(R.string.apparel_label);
		mPrice = mApparel.getRetailPrice();
		mDetailsUrl = mApparel.getDetailsUrl();
		mID = mApparel.getInternalId();

		setupViews();
	}

	private ApparelStore.Apparel getApparel() {
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				return ApparelManager.findApparel(getContentResolver(),
						intent.getData(), SettingsActivity.getSortOrder(this));
			} else {
				final String apparelId = intent.getStringExtra(EXTRA_APPAREL);
				if (apparelId != null) {
					return ApparelManager.findApparel(getContentResolver(),
							apparelId, SettingsActivity.getSortOrder(this));
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
				mApparel.getInternalId(), defaultCover));

		setTextOrHide(R.id.label_title, mApparel.getTitle());

		if (!TextUtilities.isEmpty(mApparel.getLoanedTo())) {
			findViewById(R.id.label_loaned).setVisibility(View.VISIBLE);
			setTextOrHide((TextView) findViewById(R.id.loaned),
					mApparel.getLoanedTo());
		}

		setTextOrHide(R.id.label_author, mApparel.getAuthors());

		findViewById(R.id.label_pages).setVisibility(View.GONE);

		findViewById(R.id.label_date).setVisibility(View.GONE);

		setGestures();

		((TextView) findViewById(R.id.html_reviews)).setText(mApparel
				.getDescriptions().get(0).toString());

		List<String> tagList = mApparel.getTags();
		List<String> featureList = mApparel.getFeatures();
		String ean = mApparel.getEan();
		String upc = mApparel.getUpc();
		String fabric = mApparel.getFabric();
		String department = mApparel.getDepartment();

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

		if (!TextUtilities.isEmpty(department)) {
			if (setTextOrHide((TextView) findViewById(R.id.department),
					department))
				findViewById(R.id.label_department).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(fabric)) {
			if (setTextOrHide((TextView) findViewById(R.id.fabric), fabric))
				findViewById(R.id.label_fabric).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(ean)) {
			if (setTextOrHide((TextView) findViewById(R.id.ean), ean))
				findViewById(R.id.label_ean).setVisibility(View.VISIBLE);
		}

		if (!TextUtilities.isEmpty(upc)) {
			if (setTextOrHide((TextView) findViewById(R.id.upc), upc))
				findViewById(R.id.label_upc).setVisibility(View.VISIBLE);
		}

		((TextView) findViewById(R.id.notes)).setText(mApparel.getNotes());

		UIUtilities.showIdentificationDots(
				(TextView) findViewById(R.id.identification_dots),
				viewFlipper.getDisplayedChild());

		postSetupViews();
	}

	static void show(Context context, String apparelId) {
		final Intent intent = new Intent(context, ApparelDetailsActivity.class);
		intent.putExtra(EXTRA_APPAREL, apparelId);
		context.startActivity(intent);
	}

	public static void showFromOutside(Context context, String apparelId) {
		final Intent intent = new Intent(context, ApparelDetailsActivity.class);
		intent.putExtra(EXTRA_APPAREL, apparelId);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
