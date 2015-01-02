/*
 * Copyright (C) 2010 Garen J Torikian
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

package com.miadzin.shelves.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.UIUtilities;

public class RateActivity extends Activity {
	private final String LOG_TAG = "RateActivity";

	private String mID = null;
	private String mType = null;

	private int mRating;

	private Button rateButton;
	private Button cancelButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsUtils.getInstance(this).trackPageView("/" + LOG_TAG);
		mID = this.getIntent().getExtras().getString("itemID");
		mType = this.getIntent().getExtras().getString("type");

		setContentView(R.layout.rating_dialog);

		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.string.context_menu_item_rate_label);

		final RatingBar ratingBar = (RatingBar) findViewById(R.id.ratingBar);
		final TextView ratingText = (TextView) findViewById(R.id.ratingText);

		Cursor c = getContentResolver().query(
				ShelvesApplication.TYPES_TO_URI.get(mType),
				new String[] { BaseItem.INTERNAL_ID, BaseItem.RATING },
				BaseItem.INTERNAL_ID + " = '" + mID + "'", null, null);
		if (c.moveToFirst()) {
			ratingBar.setRating(c.getInt(1));
		}
		if (c != null)
			c.close();

		ratingBar
				.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
					public void onRatingChanged(RatingBar ratingBar,
							float rating, boolean fromUser) {
						mRating = (int) FloatMath.ceil(rating);

						String message = "";

						switch (mRating) {
						case 1:
							message = getString(R.string.rate_one);
							break;
						case 2:
							message = getString(R.string.rate_two);
							break;
						case 3:
							message = getString(R.string.rate_three);
							break;
						case 4:
							message = getString(R.string.rate_four);
							break;
						case 5:
							message = getString(R.string.rate_five);
							break;
						default:
							break;
						}
						ratingText.setText(String.valueOf(mRating) + ": "
								+ message);
					}
				});

		rateButton = (Button) this.findViewById(R.id.rate_item_button);
		rateButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				final ContentValues rateValue = new ContentValues();
				rateValue.put(BaseItem.RATING, mRating);

				getContentResolver().update(
						ShelvesApplication.TYPES_TO_URI.get(mType), rateValue,
						BaseItem.INTERNAL_ID + "=?", new String[] { mID });

				UIUtilities.showToast(getBaseContext(), R.string.rate_set);

				Intent resultIntent = new Intent();
				resultIntent.putExtra("addedRate", mRating);
				setResult(Activity.RESULT_OK, resultIntent);

				finish();
			}
		});

		cancelButton = (Button) this.findViewById(R.id.rate_item_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});
	}
}
