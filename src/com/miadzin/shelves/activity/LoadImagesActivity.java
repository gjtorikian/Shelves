/*
 * Copyright (C) 2011 Garen J. Torikian
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

// http://stackoverflow.com/questions/2169649/open-an-image-in-androids-built-in-gallery-app-programmatically

package com.miadzin.shelves.activity;

import java.io.File;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;

import com.miadzin.shelves.R;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.ImportUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.UIUtilities;

public class LoadImagesActivity extends Activity {
	private String LOG_TAG = "LoadImagesActivity";

	private static final int SELECT_PICTURE = 1;

	private String selectedImagePath;
	private String filemanagerstring;
	private String mID = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsUtils.getInstance(this).trackPageView("/" + LOG_TAG);

		mID = this.getIntent().getExtras().getString("itemID");

		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"),
				SELECT_PICTURE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			if (requestCode == SELECT_PICTURE) {
				Uri selectedImageUri = data.getData();

				// OI FILE Manager
				filemanagerstring = selectedImageUri.getPath();

				// MEDIA GALLERY
				selectedImagePath = getPath(selectedImageUri);

				final String imagePath;
				if (selectedImagePath != null)
					imagePath = selectedImagePath;
				else if (filemanagerstring != null)
					imagePath = filemanagerstring;
				else
					imagePath = null;

				InputStream stream = null;
				Bitmap bitmap = null;

				try {
					File file = new File(imagePath);

					bitmap = ImageUtilities.decodeFile(file);

					if (bitmap != null) {
						if (mID != null) { // GJT: Was from context menu
							ImageUtilities.deleteCachedCover(mID);
							ImportUtilities.addCoverToCache(mID, ImageUtilities
									.createCover(bitmap,
											Preferences.getWidthForManager(),
											Preferences.getHeightForManager()));
							UIUtilities.showToast(getBaseContext(),
									R.string.success_refreshing_cover, false);
							setResult(RESULT_OK);
						} else { // GJT: Was from manual add
							Intent intent = new Intent();
							intent.putExtra("newCover", bitmap);
							setResult(RESULT_OK, intent);
						}
					}
				} catch (Exception e) {
				}

				finish();
			}
		}
	}

	// UPDATED!
	public String getPath(Uri uri) {
		String[] projection = { MediaColumns.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		if (cursor != null) {
			// HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
			// THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
			int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} else
			return null;
	}
}
