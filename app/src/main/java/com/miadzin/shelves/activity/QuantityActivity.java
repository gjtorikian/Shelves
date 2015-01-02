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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.miadzin.shelves.R;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.base.BaseItemActivity;
import com.miadzin.shelves.util.AnalyticsUtils;

@TargetApi(11)
public class QuantityActivity extends Activity {

	private String mID = null;
	private String mType = null;

	private Button okButton;
	private Button cancelButton;

	private static final String LOG_TAG = "QuantityActivity";

	@Override
	@TargetApi(11)
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsUtils.getInstance(this).trackPageView("/" + LOG_TAG);

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		mID = this.getIntent().getExtras().getString("itemID");
		mType = this.getIntent().getExtras().getString("type");

		Log.i(LOG_TAG, mType);

		setContentView(R.layout.quantity_dialog);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.dialog_title);

		final TextView quantityTitle = (TextView) findViewById(R.id.dialogTitle);
		quantityTitle.setText(getString(R.string.quantity_label));

		final NumberPicker quantitySelector = (NumberPicker) findViewById(R.id.quantity_spinner);

		String[] nums = new String[99];
		for (int i = 0; i < nums.length; i++)
			nums[i] = Integer.toString(i + 1);

		quantitySelector.setMinValue(1);
		quantitySelector.setMaxValue(100);
		quantitySelector.setWrapSelectorWheel(false);
		quantitySelector.setDisplayedValues(nums);
		quantitySelector.setValue(2);

		cancelButton = (Button) this.findViewById(R.id.quantity_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		okButton = (Button) this.findViewById(R.id.quantity_ok);
		okButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ContentValues quantity = new ContentValues();
				quantity.put(BaseItem.QUANTITY,
						String.valueOf(quantitySelector.getValue()));
				final Uri uri = BaseItemActivity.findItemUri(mType);

				getContentResolver().update(uri, quantity,
						BaseItem.INTERNAL_ID + "=?", new String[] { mID });

				finish();
			}
		});
	}
}
