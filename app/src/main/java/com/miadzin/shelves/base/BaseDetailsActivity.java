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

package com.miadzin.shelves.base;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.miadzin.shelves.R;
import com.miadzin.shelves.util.ActivityHelper;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public abstract class BaseDetailsActivity extends Activity {
	private ActivityHelper mActivityHelper;

	protected String mContext;
	protected String mPrice;
	protected String mDetailsUrl;
	protected String mID;
	protected ContentValues textValues;

	// GJT: For flinging between item description & details
	protected static final int SWIPE_MIN_DISTANCE = 120;
	protected static final int SWIPE_MAX_OFF_PATH = 250;
	protected static final int SWIPE_THRESHOLD_VELOCITY = 200;
	protected GestureDetector gestureDetector;
	protected View.OnTouchListener gestureListener;
	protected Animation slideLeftIn;
	protected Animation slideLeftOut;
	protected Animation slideRightIn;
	protected Animation slideRightOut;
	protected ViewFlipper viewFlipper;

	protected static final int EDIT_ACTIVITY_REQUEST_CODE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Context c = getBaseContext();
		final String sActivity = this.toString();
		AnalyticsUtils.getInstance(c)
				.trackPageView(
						"/"
								+ sActivity.substring(0,
										sActivity.indexOf("Activity") + 8));

		setContentView(R.layout.screen_details);

		AdView mAdView = (AdView) findViewById(R.id.adview);
		if (!UIUtilities.isPaid(getContentResolver(), this)) {
			mAdView.setVisibility(View.VISIBLE);
			AdRequest adRequest = new AdRequest.Builder().build();
			mAdView.loadAd(adRequest);
		} else {
			mAdView.setVisibility(View.GONE);
		}
	}

	protected abstract void setupViews();

	protected void postSetupViews() {
		mActivityHelper = ActivityHelper.createInstance(this);

		if (!UIUtilities.isHoneycomb()) {
			mActivityHelper.showActionBar(true);
			mActivityHelper.setupActionBar(" ");

			View.OnClickListener editClickListener = new View.OnClickListener() {
				public void onClick(View view) {
					onEdit();
				}
			};

			View.OnClickListener viewClickListener = new View.OnClickListener() {
				public void onClick(View view) {
					onBuy();
				}
			};

			mActivityHelper.addActionButtonCompat(R.drawable.ic_action_edit,
					null, editClickListener, true, true, true);

			mActivityHelper.addActionButtonCompat(
					R.drawable.ic_action_view_online, null, viewClickListener,
					true, true, true);

			if (!TextUtilities.isEmpty(mPrice)) {
				mActivityHelper.addActionButtonCompat(
						R.string.menu_item_view_short, "@" + mPrice,
						viewClickListener, false, true, true);
			}

			else {
				mActivityHelper.addActionButtonCompat(
						R.string.menu_item_view_short, "", viewClickListener,
						false, true, true);
			}

		} else {
			mActivityHelper.setActionBarTitle(" ");
		}
	}

	protected void setTextOrHide(int id, String text) {
		if (!TextUtilities.isEmpty(text)) {
			((TextView) findViewById(id)).setText(text);
		} else {
			findViewById(id).setVisibility(View.GONE);
		}
	}

	// GJT: Added this, function to control details dialog fields
	protected boolean setTextOrHide(TextView view, String text) {
		if (!TextUtilities.isEmpty(text)) {
			view.setText(text);
			view.setVisibility(View.VISIBLE);
			return true;
		} else {
			view.setVisibility(View.GONE);
			return false;
		}
	}

	// GJT: Added this, for "more information" menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (UIUtilities.isHoneycomb()) {
			getMenuInflater().inflate(R.menu.item_details, menu);

			if (!TextUtilities.isEmpty(mPrice)) {
				menu.findItem(R.id.menu_item_view).setTitle(
						getString(R.string.menu_item_view_short, "@" + mPrice));
			}

			else {
				menu.findItem(R.id.menu_item_view).setTitle(
						getString(R.string.menu_item_view_short, ""));
			}

			return true;
		}
		return false;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_edit:
			onEdit();
			break;
		case R.id.menu_item_view:
			onBuy();
			break;
		}

		return true;
	}

	protected void onEdit() {
		Intent i = new Intent(BaseDetailsActivity.this,
				BaseManualAddActivity.class);
		Bundle b = new Bundle();

		b.putString("type", mActivityHelper.getActivity().toString()
				.toLowerCase());
		b.putString("mID", mID);

		i.putExtras(b);
		startActivityForResult(i, EDIT_ACTIVITY_REQUEST_CODE);
	}

	protected void onBuy() {
		if (!TextUtilities.isEmpty(mDetailsUrl)) {
			final Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(mDetailsUrl));

			startActivity(intent);
		} else {
			UIUtilities.showToast(this, R.string.cannot_view_manual_item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case EDIT_ACTIVITY_REQUEST_CODE:
			switch (resultCode) {
			case RESULT_OK:
				UIUtilities.showToast(getBaseContext(), R.string.edit_set);
				finish();
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}
	}

	class MyGestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					viewFlipper.setInAnimation(slideLeftIn);
					viewFlipper.setOutAnimation(slideLeftOut);
					viewFlipper.showNext();
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					viewFlipper.setInAnimation(slideRightIn);
					viewFlipper.setOutAnimation(slideRightOut);
					viewFlipper.showPrevious();
				}

				UIUtilities.showIdentificationDots(
						(TextView) findViewById(R.id.identification_dots),
						viewFlipper.getDisplayedChild());
			} catch (Exception e) {
				// nothing
			}

			return false;
		}
	}

	protected void setGestures() {
		viewFlipper = (ViewFlipper) findViewById(R.id.flipper);
		viewFlipper.setMeasureAllChildren(false);

		slideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
		slideLeftOut = AnimationUtils
				.loadAnimation(this, R.anim.slide_left_out);
		slideRightIn = AnimationUtils
				.loadAnimation(this, R.anim.slide_right_in);
		slideRightOut = AnimationUtils.loadAnimation(this,
				R.anim.slide_right_out);

		gestureDetector = new GestureDetector(new MyGestureDetector());
		gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
		};
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (gestureDetector != null) {
			gestureDetector.onTouchEvent(ev);
		}
		return super.dispatchTouchEvent(ev);
	}
}