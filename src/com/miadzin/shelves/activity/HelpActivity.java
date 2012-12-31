/*
 * Copyright 2008 ZXing authors
 * Copyright 2010 Garen J. Torikian
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
 * 
 * GJT: The source below is heavily modified. I removed all instances to ZXing-specific
 * needs, and I changed the ids/strings of the help resources
 */

package com.miadzin.shelves.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.miadzin.shelves.R;
import com.miadzin.shelves.util.AnalyticsUtils;

/**
 * An HTML-based help screen with Back and Done buttons at the bottom.
 * 
 */
public class HelpActivity extends Activity {

	// Use this key and one of the values below when launching this activity via
	// intent. If not
	// present, the default page will be loaded.
	public static final String REQUESTED_PAGE_KEY = "requested_page_key";
	public static final String DEFAULT_PAGE = "index.html";
	public static final String WHATS_NEW_PAGE = "versions.html";
	private static final String BASE_URL = "file:///android_asset/html/";

	private WebView webView;
	private Button backButton;

	private final Button.OnClickListener backListener = new Button.OnClickListener() {
		public void onClick(View view) {
			webView.goBack();
		}
	};

	private final Button.OnClickListener doneListener = new Button.OnClickListener() {
		public void onClick(View view) {
			Intent resultIntent = new Intent();
			setResult(Activity.RESULT_OK, resultIntent);

			finish();
		}
	};

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		AnalyticsUtils.getInstance(this).trackPageView("/Help");
		setContentView(R.xml.help);

		webView = (WebView) findViewById(R.id.helpbrowser);
		webView.setWebViewClient(new HelpClient());

		Intent intent = getIntent();
		try {
			if (icicle != null) {
				webView.restoreState(icicle);
			} else if (intent != null) {
				String page = intent.getStringExtra(REQUESTED_PAGE_KEY);
				if (page != null && page.length() > 0) {
					webView.loadUrl(BASE_URL + page);
				} else {
					webView.loadUrl(BASE_URL + DEFAULT_PAGE);
				}
			} else {
				webView.loadUrl(BASE_URL + DEFAULT_PAGE);
			}
		} catch (NullPointerException npe) {
			Log.e("HelpActivity", npe.toString());
		}
		backButton = (Button) findViewById(R.id.helpBackButton);
		backButton.setOnClickListener(backListener);
		Button doneButton = (Button) findViewById(R.id.helpDoneButton);
		doneButton.setOnClickListener(doneListener);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		webView.saveState(state);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (webView.canGoBack()) {
				webView.goBack();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private final class HelpClient extends WebViewClient {
		@Override
		public void onPageFinished(WebView view, String url) {
			setTitle(view.getTitle());
			backButton.setEnabled(view.canGoBack());
		}
	}

	public static String getBaseUrl() {
		return BASE_URL;
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		try {
			super.onWindowFocusChanged(hasFocus);
		} catch (NullPointerException npe) {
			Log.e("HelpActivity", npe.toString());
		}
	}
}
