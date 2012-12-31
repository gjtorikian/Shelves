/*
 * Copyright (C) 2010 Garen J Torikian
 * Taken liberally from Last.fm Android client
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.util.ActivityHelper;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;
import com.miadzin.shelves.view.TagLayout;
import com.miadzin.shelves.view.TagLayoutListener;
import com.miadzin.shelves.view.TagListAdapter;

public class TagActivity extends Activity {
	private ActivityHelper mActivityHelper;

	private EditText mTagEditText;
	private ImageButton mTagButton;
	private TagLayout mTagLayout;
	private Button mSaveButton;
	private Button mCancelButton;
	private ListView mTagList;
	private TagListAdapter mUserTagListAdapter;
	private List<String> myTags = null;

	private String mID = null;
	private String mType = null;

	private Animation mFadeOutAnimation;
	boolean animate = false;

	private ArrayList<String> mUserTags;
	private ArrayList<String> mAddedTags;

	public static final int MAX_TAGS = 8;
	public static final int MAX_TAG_LENGTH = 20;

	private static final String LOG_TAG = "TagActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsUtils.getInstance(this).trackPageView("/" + LOG_TAG);

		mID = this.getIntent().getExtras().getString("itemID");
		mType = this.getIntent().getExtras().getString("type");

		this.setContentView(R.layout.tag_dialog);

		mTagEditText = (EditText) findViewById(R.id.tag_text_edit);
		mTagButton = (ImageButton) findViewById(R.id.tag_add_button);
		mTagLayout = (TagLayout) findViewById(R.id.TagLayout);
		mSaveButton = (Button) findViewById(R.id.tag_save_button);
		mCancelButton = (Button) findViewById(R.id.tag_cancel_button);
		mTagList = (ListView) findViewById(R.id.TagList);

		mFadeOutAnimation = AnimationUtils.loadAnimation(this,
				R.anim.tag_row_fadeout);
		mTagLayout.setAnimationsEnabled(true);

		setupViews();

		// add callback listeners
		mTagEditText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_ENTER:
					mTagButton.performClick();
					mTagEditText.setText("");
					return true;
				default:
					return false;
				}
			}
		});

		mTagButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				addTag(mTagEditText.getText().toString());
				mTagEditText.setText("");
			}
		});

		mTagLayout.setTagLayoutListener(new TagLayoutListener() {
			public void tagRemoved(String tag) {
				mUserTagListAdapter.tagUnadded(tag);
				mAddedTags.remove(tag);
			}
		});

		mTagLayout.setAreaHint(R.string.tagarea_hint);

		mTagList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> parent,
					final View view, final int position, long time) {
				if (!animate) {
					String tag = (String) parent.getItemAtPosition(position);
					if (addTag(tag)) {
						mFadeOutAnimation
								.setAnimationListener(new AnimationListener() {

									public void onAnimationEnd(
											Animation animation) {
										((TagListAdapter) parent.getAdapter())
												.tagAdded(position);
										animate = false;
									}

									public void onAnimationRepeat(
											Animation animation) {
									}

									public void onAnimationStart(
											Animation animation) {
										animate = true;
									}

								});
						view.findViewById(R.id.row_label).startAnimation(
								mFadeOutAnimation);
					}
				}

			}

		});

		mSaveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ContentValues tagValues = new ContentValues();
				tagValues.put(BaseItem.TAGS, mTagLayout.getTagsAsString());

				getContentResolver().update(
						ShelvesApplication.TYPES_TO_URI.get(mType), tagValues,
						BaseItem.INTERNAL_ID + "=?", new String[] { mID });

				UIUtilities.showToast(getBaseContext(), R.string.tags_set);

				Intent resultIntent = new Intent();
				resultIntent.putExtra("addedTags", mAddedTags.toString());
				setResult(Activity.RESULT_OK, resultIntent);

				finish();
			}
		});

		mCancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});

	}

	private void setupViews() {
		mActivityHelper = ActivityHelper.createInstance(this);

		mUserTagListAdapter = new TagListAdapter(this);
		mAddedTags = new ArrayList<String>();

		if (getLastNonConfigurationInstance() != null) {
			Object savedState[] = (Object[]) getLastNonConfigurationInstance();
			if (!TextUtilities.isEmpty((String) savedState[0])) {
				for (String tag : TextUtilities.breakString(
						(String) savedState[0], ",")) {
					mTagLayout.addTag(tag);
				}
			}
			mUserTags = (ArrayList<String>) savedState[1];

			try {
				fillData();
				return;
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
		new LoadTagTask().execute((Object) null);

		if (UIUtilities.isHoneycomb()) {
			mActivityHelper
					.setActionBarTitle(getString(R.string.context_menu_item_tag_label)
							+ "\n"
							+ this.getIntent().getExtras().getString("title"));
		}
	}

	/**
	 * Fills mUserTagListListAdapter with all tags across the item
	 */
	private void fillData() {
		if (!TextUtilities.isEmpty(myTags)) {
			for (int i = 0; i < myTags.size(); i++) {
				mTagLayout.addTag(myTags.get(i));
			}
		}
		mUserTagListAdapter.setSource(mUserTags,
				mTagLayout.getTagsAsArrayList());
		mTagList.setAdapter(mUserTagListAdapter);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Object savedState[] = new Object[2];
		savedState[0] = mTagLayout.getTagsAsString();
		savedState[1] = mUserTags;

		return savedState;
	}

	private boolean addTag(String tag) {
		if (TextUtilities.isEmpty(tag)
				|| mTagLayout.getTagsAsString().contains(tag))
			return false;

		if (mTagLayout.getTagsAsSet().size() >= MAX_TAGS) {
			UIUtilities.showToast(this, R.string.tag_add_error);
			return false;
		}

		final String scrubbedTag = tag.replace(",", "");
		mTagLayout.addTag(scrubbedTag);
		mAddedTags.add(scrubbedTag);
		return true;
	}

	private class LoadTagTask extends AsyncTask<Object, Integer, Object> {
		ProgressDialog mLoadDialog;

		@Override
		public void onPreExecute() {
			if (mLoadDialog == null) {
				mLoadDialog = ProgressDialog.show(TagActivity.this, "",
						getString(R.string.tags_fetching), true, false);
				mLoadDialog.setCancelable(true);
			}
		}

		@Override
		public Object doInBackground(Object... params) {
			Cursor c = null;
			ContentResolver contentResolver = getContentResolver();
			mUserTags = new ArrayList<String>();

			c = contentResolver.query(
					ShelvesApplication.TYPES_TO_URI.get(mType), new String[] {
							BaseItem.INTERNAL_ID, BaseItem.TAGS },
					BaseItem.TAGS + " NOT NULL", null, null);
			if (c.moveToFirst()) {
				do {
					if (mID.equals(c.getString(0))) {
						myTags = TextUtilities.breakString(c.getString(1), ",");
					}
					for (String tag : c.getString(1).split(",")) {
						if (!TextUtilities.isEmpty(tag)
								&& !mUserTags.contains(tag)) {
							mUserTags.add(tag);
						}
					}
				} while (c.moveToNext());
			}

			if (c != null)
				c.close();
			Collections.sort(mUserTags);
			return null;
		}

		@Override
		public void onPostExecute(Object result) {
			fillData();
			try {
				if (mLoadDialog != null) {
					mLoadDialog.dismiss();
					mLoadDialog = null;
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	}
}
