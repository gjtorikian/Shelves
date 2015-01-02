package com.miadzin.shelves.activity;

import android.annotation.TargetApi;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.miadzin.shelves.BuildConfig;
import com.miadzin.shelves.R;
import com.miadzin.shelves.provider.InternalAdapter;
import com.miadzin.shelves.util.ActivityHelper;
import com.miadzin.shelves.util.UIUtilities;
import com.miadzin.shelves.licensing.LicenseCheck;

public class TabSelector extends TabActivity {
	static ActivityHelper mActivityHelper;

	private final String LOG_TAG = "TabSelector";
	private InternalAdapter mDbHelper;

	static TabHost tabHost;
	static MainGridActivity mainGridActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_tab_screen);

		mDbHelper = new InternalAdapter(this);
        setupView(LicenseCheck.check(getBaseContext()));
	}

	private void setupView(boolean purchased) {
		Log.d(LOG_TAG, "Ads: " + purchased);
		setContentView(R.layout.main_tab_screen);

		mActivityHelper = ActivityHelper.createInstance(this);

		if (!UIUtilities.isHoneycomb()) {
			mActivityHelper.showActionBar(true);
			mActivityHelper
					.setupActionBar(getString(R.string.application_name));

			createAddRemoveCollectionButton();

			View.OnClickListener addHelpClickListener = new View.OnClickListener() {
				public void onClick(View view) {
					launchHelp();
				}
			};

			mActivityHelper.addActionButtonCompat(R.drawable.ic_action_help,
					null, addHelpClickListener, true, true, true);
		}

		Resources res = getResources();
		tabHost = getTabHost();

		TabHost.TabSpec tabSpec;
		Intent intent;
		intent = new Intent().setClass(this, MainGridActivity.class);
		intent.putExtra("bought", purchased);
		tabSpec = tabHost
				.newTabSpec("grid")
				.setIndicator(getString(R.string.tab_item_selection),
						res.getDrawable(R.drawable.tab_grid))
				.setContent(intent);
		tabHost.addTab(tabSpec);

		intent = new Intent().setClass(this, LoanViewActivity.class);
		intent.putExtra("bought", purchased);
		tabSpec = tabHost
				.newTabSpec("loan")
				.setIndicator(getString(R.string.tab_loaned_items),
						res.getDrawable(R.drawable.tab_loan))
				.setContent(intent);
		tabHost.addTab(tabSpec);

		intent = new Intent().setClass(this, WishlistViewActivity.class);
		intent.putExtra("bought", purchased);
		tabSpec = tabHost
				.newTabSpec("wish")
				.setIndicator(getString(R.string.tab_wishlist),
						res.getDrawable(R.drawable.tab_wish))
				.setContent(intent);
		tabHost.addTab(tabSpec);

		tabHost.setOnTabChangedListener(new OnTabChangeListener() {

			@TargetApi(11)
			public void onTabChanged(String tabId) {
				if (!UIUtilities.isHoneycomb()) {
					if (tabHost.getCurrentTab() == 0) {
						mActivityHelper.showOrHideActionButtonCompat(
								R.drawable.ic_menu_selectall_holo_light, true);
					}

					else {
						mActivityHelper.showOrHideActionButtonCompat(
								R.drawable.ic_menu_selectall_holo_light, false);
					}
				} else {
					try {
						invalidateOptionsMenu();
					} catch (NoSuchMethodError e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		});
	}

	public static void changeActionBarTitle(String appName, int count) {
		if (mActivityHelper != null)
			mActivityHelper.setActionBarTitle(appName + " | " + count);
	}

	private void createAddRemoveCollectionButton() {
		View.OnClickListener addRemoveCollectionClickListener = new View.OnClickListener() {
			public void onClick(View view) {
				launchAddRemoveCollection();
			}
		};

		mActivityHelper.addActionButtonCompat(
				R.drawable.ic_menu_selectall_holo_light, null,
				addRemoveCollectionClickListener, true, true, true);
	}

	private void launchAddRemoveCollection() {
		if (mainGridActivity != null && mainGridActivity.hasWindowFocus())
			try {
				mainGridActivity
						.showDialog(MainGridActivity.ADD_REMOVE_ITEM_DIALOG);
			} catch (Exception e) {
			}
	}

	private void launchHelp() {
		Context c = getBaseContext();
		final Intent intent = new Intent(c, HelpActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		c.startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (UIUtilities.isHoneycomb()) {
			getMenuInflater().inflate(R.menu.main_grid, menu);

			return true;
		}
		return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem collection_visibility = menu.findItem(R.id.menu_item_add);

		if (tabHost != null) {
			if (tabHost.getCurrentTab() == 0)
				collection_visibility.setVisible(true);
			else
				collection_visibility.setVisible(false);
		} else
			collection_visibility.setVisible(false);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_add:
			launchAddRemoveCollection();
			return true;
		case R.id.menu_item_help:
			launchHelp();
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setupView(LicenseCheck.check(getBaseContext()));
	}
}