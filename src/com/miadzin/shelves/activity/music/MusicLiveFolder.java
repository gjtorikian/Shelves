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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;

import com.miadzin.shelves.R;
import com.miadzin.shelves.provider.music.MusicStore;

public class MusicLiveFolder extends Activity {
	public static final Uri CONTENT_URI = Uri
			.parse("content://MusicProvider/live_folders/music");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String action = intent.getAction();

		if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) {
			setResult(
					RESULT_OK,
					createLiveFolder(this, CONTENT_URI,
							getString(R.string.live_folder_music),
							R.drawable.ic_livefolder_music_icon));
		} else {
			setResult(RESULT_CANCELED);
		}

		finish();
	}

	private static Intent createLiveFolder(Context context, Uri uri,
			String name, int icon) {

		final Intent intent = new Intent();

		intent.setData(uri);

		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, name);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON,
				Intent.ShortcutIconResource.fromContext(context, icon));
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
				LiveFolders.DISPLAY_MODE_LIST);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT, new Intent(
				Intent.ACTION_VIEW, MusicStore.Music.CONTENT_URI));

		return intent;
	}
}
