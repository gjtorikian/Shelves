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

package com.miadzin.shelves.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.activity.apparel.ApparelActivity;
import com.miadzin.shelves.activity.boardgames.BoardGamesActivity;
import com.miadzin.shelves.activity.books.BooksActivity;
import com.miadzin.shelves.activity.comics.ComicsActivity;
import com.miadzin.shelves.activity.gadgets.GadgetsActivity;
import com.miadzin.shelves.activity.movies.MoviesActivity;
import com.miadzin.shelves.activity.music.MusicActivity;
import com.miadzin.shelves.activity.software.SoftwareActivity;
import com.miadzin.shelves.activity.tools.ToolsActivity;
import com.miadzin.shelves.activity.toys.ToysActivity;
import com.miadzin.shelves.activity.videogames.VideoGamesActivity;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.base.BaseItemActivity;
import com.miadzin.shelves.provider.apparel.ApparelStore;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore;
import com.miadzin.shelves.provider.books.BooksStore;
import com.miadzin.shelves.provider.comics.ComicsStore;
import com.miadzin.shelves.provider.gadgets.GadgetsStore;
import com.miadzin.shelves.provider.movies.MoviesStore;
import com.miadzin.shelves.provider.music.MusicStore;
import com.miadzin.shelves.provider.software.SoftwareStore;
import com.miadzin.shelves.provider.tools.ToolsStore;
import com.miadzin.shelves.provider.toys.ToysStore;
import com.miadzin.shelves.provider.videogames.VideoGamesStore;
import com.miadzin.shelves.server.ServerInfo;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class MainGridActivity extends Activity {
	// GJT: Added this for What's New launch
	private static final String PACKAGE_NAME = "com.miadzin.shelves";

	public static final int ADD_REMOVE_ITEM_DIALOG = 1;
	private static final int NO_ITEM_DIALOG = 2;
	private static final int CHANGE_LOCALE_EXPLANATION_DIALOG = 3;
	private static final int CHANGE_LOCALE_DIALOG = 4;

	private ImageAdapter ia;
	private GridView mGridView;

	private int lastVersion = -1;
	private int selectedLocale;

	// Used for populating the grid
	private ArrayList<String> itemCounts;
	private ArrayList<String> loanCounts;
	private ArrayList<String> wishlistCounts;
	private ArrayList<Class<?>> itemCollections;

	// references to the images and titles
	private ArrayList<Integer> mThumbIds;
	private ArrayList<String> mTitleIds;
	private String[] titleArray;

	private final String LOG_TAG = "MainGridActivity";
	private boolean bought;

	private String mSource;

	public int itemCounter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(LOG_TAG, "Using " + Build.VERSION.SDK_INT);

		TabSelector.mainGridActivity = this;

		bought = this.getIntent().getExtras().getBoolean("bought");

		// GJT: Used when the view has changed from grid to list and vice versa
		mSource = this.getIntent().getExtras()
				.getString(BaseItemActivity.SOURCE);
		Context context = getBaseContext();
		if (!TextUtilities.isEmpty(mSource)) {
			finish();
			if (mSource.equals("apparel"))
				startActivity(new Intent(context, ApparelActivity.class)
						.putExtra("bought", bought));
			else if (mSource.equals("boardgames"))
				startActivity(new Intent(context, BoardGamesActivity.class)
						.putExtra("bought", bought));
			else if (mSource.equals("books"))
				startActivity(new Intent(context, BooksActivity.class)
						.putExtra("bought", bought));
			else if (mSource.equals("comics"))
				startActivity(new Intent(context, ComicsActivity.class)
						.putExtra("bought", bought));
			else if (mSource.equals("gadgets"))
				startActivity(new Intent(context, GadgetsActivity.class)
						.putExtra("bought", bought));
			else if (mSource.equals("movies"))
				startActivity(new Intent(context, MoviesActivity.class)
						.putExtra("bought", bought));
			else if (mSource.equals("music"))
				startActivity(new Intent(context, MusicActivity.class)
						.putExtra("bought", bought));
			else if (mSource.equals("software"))
				startActivity(new Intent(context, SoftwareActivity.class)
						.putExtra("bought", bought));
			else if (mSource.equals("tools"))
				startActivity(new Intent(context, ToolsActivity.class)
						.putExtra("bought", bought));
			else if (mSource.equals("toys"))
				startActivity(new Intent(context, ToysActivity.class).putExtra(
						"bought", bought));
			else if (mSource.equals("videogames"))
				startActivity(new Intent(context, VideoGamesActivity.class)
						.putExtra("bought", bought));
		}

		launchWhatsNewMessage();
		setupViews();
	}

	@Override
	protected void onResume() {
		super.onResume();

		setupViews();
	}

	private void setupViews() {
		setContentView(R.layout.main_grid);

		ContentResolver cr = getContentResolver();
		Cursor c = null;
		Cursor l = null;
		Cursor w = null;

		itemCounter = 0;
		itemCounts = new ArrayList<String>(0);
		loanCounts = new ArrayList<String>(0);
		wishlistCounts = new ArrayList<String>(0);

		for (int i = 0; i < 11; i++) {
			switch (i) {
			case 0:
				c = cr.query(ApparelStore.Apparel.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(ApparelStore.Apparel.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.LOANED_TO }, BaseItem.LOANED_TO
						+ " NOT NULL AND " + BaseItem.LOANED_TO + " != ''",
						null, null);
				w = cr.query(ApparelStore.Apparel.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;

			case 1:
				c = cr.query(BoardGamesStore.BoardGame.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(BoardGamesStore.BoardGame.CONTENT_URI,
						new String[] { BaseItem._ID, BaseItem.LOANED_TO },
						BaseItem.LOANED_TO + " NOT NULL AND "
								+ BaseItem.LOANED_TO + " != ''", null, null);
				w = cr.query(BoardGamesStore.BoardGame.CONTENT_URI,
						new String[] { BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			case 2:
				c = cr.query(BooksStore.Book.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(BooksStore.Book.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.LOANED_TO }, BaseItem.LOANED_TO
						+ " NOT NULL AND " + BaseItem.LOANED_TO + " != ''",
						null, null);
				w = cr.query(BooksStore.Book.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			case 3:
				c = cr.query(ComicsStore.Comic.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(ComicsStore.Comic.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.LOANED_TO }, BaseItem.LOANED_TO
						+ " NOT NULL AND " + BaseItem.LOANED_TO + " != ''",
						null, null);
				w = cr.query(ComicsStore.Comic.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			case 4:
				c = cr.query(GadgetsStore.Gadget.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(GadgetsStore.Gadget.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.LOANED_TO }, BaseItem.LOANED_TO
						+ " NOT NULL AND " + BaseItem.LOANED_TO + " != ''",
						null, null);
				w = cr.query(GadgetsStore.Gadget.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			case 5:
				c = cr.query(MoviesStore.Movie.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(MoviesStore.Movie.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.LOANED_TO }, BaseItem.LOANED_TO
						+ " NOT NULL AND " + BaseItem.LOANED_TO + " != ''",
						null, null);
				w = cr.query(MoviesStore.Movie.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			case 6:
				c = cr.query(MusicStore.Music.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(MusicStore.Music.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.LOANED_TO }, BaseItem.LOANED_TO
						+ " NOT NULL AND " + BaseItem.LOANED_TO + " != ''",
						null, null);
				w = cr.query(MusicStore.Music.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			case 7:
				c = cr.query(SoftwareStore.Software.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(SoftwareStore.Software.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.LOANED_TO }, BaseItem.LOANED_TO
						+ " NOT NULL AND " + BaseItem.LOANED_TO + " != ''",
						null, null);
				w = cr.query(SoftwareStore.Software.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			case 8:
				c = cr.query(ToolsStore.Tool.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(ToolsStore.Tool.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.LOANED_TO }, BaseItem.LOANED_TO
						+ " NOT NULL AND " + BaseItem.LOANED_TO + " != ''",
						null, null);
				w = cr.query(ToolsStore.Tool.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			case 9:
				c = cr.query(ToysStore.Toy.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(ToysStore.Toy.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.LOANED_TO }, BaseItem.LOANED_TO
						+ " NOT NULL AND " + BaseItem.LOANED_TO + " != ''",
						null, null);
				w = cr.query(ToysStore.Toy.CONTENT_URI, new String[] {
						BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			case 10:
				c = cr.query(VideoGamesStore.VideoGame.CONTENT_URI,
						new String[] { BaseItem._ID }, null, null, null);
				l = cr.query(VideoGamesStore.VideoGame.CONTENT_URI,
						new String[] { BaseItem._ID, BaseItem.LOANED_TO },
						BaseItem.LOANED_TO + " NOT NULL AND "
								+ BaseItem.LOANED_TO + " != ''", null, null);
				w = cr.query(VideoGamesStore.VideoGame.CONTENT_URI,
						new String[] { BaseItem._ID, BaseItem.WISHLIST_DATE },
						BaseItem.WISHLIST_DATE + " NOT NULL AND "
								+ BaseItem.WISHLIST_DATE + " != ''", null, null);
				break;
			}

			itemCounts.add(Integer.toString(c.getCount()));
			loanCounts.add(Integer.toString(l.getCount()));
			wishlistCounts.add(Integer.toString(w.getCount()));

			itemCounter += c.getCount();

			if (c != null)
				c.close();
			if (l != null)
				l.close();
			if (w != null)
				w.close();
		}

		TabSelector.changeActionBarTitle(getString(R.string.application_name),
				itemCounter);

		// GJT: Must do these here (not outside scope), otherwise NPE
		final Integer[] thumbArray = new Integer[] {
				R.drawable.ic_livefolder_apparel_icon,
				R.drawable.ic_livefolder_boardgame_icon,
				R.drawable.ic_livefolder_book_icon,
				R.drawable.ic_livefolder_comic_icon,
				R.drawable.ic_livefolder_gadget_icon,
				R.drawable.ic_livefolder_movie_icon,
				R.drawable.ic_livefolder_music_icon,
				R.drawable.ic_livefolder_software_icon,
				R.drawable.ic_livefolder_tool_icon,
				R.drawable.ic_livefolder_toy_icon,
				R.drawable.ic_livefolder_videogame_icon };
		mThumbIds = new ArrayList<Integer>(Arrays.asList(thumbArray));

		final Resources res = getBaseContext().getResources();

		titleArray = new String[] { res.getString(R.string.apparel_label_big),
				res.getString(R.string.boardgame_label_plural_big),
				res.getString(R.string.book_label_plural_big),
				res.getString(R.string.comic_label_plural_big),
				res.getString(R.string.gadget_label_plural_big),
				res.getString(R.string.movie_label_plural_big),
				res.getString(R.string.music_label_big),
				res.getString(R.string.software_label_big),
				res.getString(R.string.tool_label_plural_big),
				res.getString(R.string.toy_label_plural_big),
				res.getString(R.string.videogame_label_plural_big) };
		mTitleIds = new ArrayList<String>(Arrays.asList(titleArray));

		final Class<?>[] itemCollectionArray = new Class<?>[] {
				ApparelActivity.class, BoardGamesActivity.class,
				BooksActivity.class, ComicsActivity.class,
				GadgetsActivity.class, MoviesActivity.class,
				MusicActivity.class, SoftwareActivity.class,
				ToolsActivity.class, ToysActivity.class,
				VideoGamesActivity.class };
		itemCollections = new ArrayList<Class<?>>(
				Arrays.asList(itemCollectionArray));

		final SharedPreferences pref = getBaseContext().getSharedPreferences(
				Preferences.NAME, 0);
		final String[] itemsToNotShow = pref.getString(Preferences.KEY_ITEMS,
				"").split(", ");

		Arrays.sort(itemsToNotShow, new TextUtilities.RevStrComp());

		for (String item : itemsToNotShow) {
			if (!TextUtilities.isEmpty(item)) {
				for (int i = 0; i < mTitleIds.size(); i++) {
					if (item.equals(mTitleIds.get(i))) {
						mThumbIds.remove(i);
						mTitleIds.remove(i);
						itemCollections.remove(i);
						itemCounts.remove(i);
						loanCounts.remove(i);
						wishlistCounts.remove(i);
					}
				}
			}
		}

		mGridView = (GridView) findViewById(R.id.main_grid_screen);
		ia = new ImageAdapter(this);
		mGridView.setAdapter(ia);
		mGridView.setOnItemClickListener(new CollectionSelector());

		AdView mAdView = (AdView) findViewById(R.id.adview);

		if (!bought) {
			mAdView.setVisibility(View.VISIBLE);
			AdRequest adRequest = new AdRequest.Builder().build();
			mAdView.loadAd(adRequest);

			Button buyFromMarket = (Button) findViewById(R.id.launch_market);

			buyFromMarket.setVisibility(View.VISIBLE);
			buyFromMarket.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					try {
						startActivity(new Intent(
								Intent.ACTION_VIEW,
								Uri.parse("market://search?q=pname:com.miadzin.shelves.unlocker")));
						finish();
					} catch (ActivityNotFoundException e) {
						Log.e("MarketNotFound", e.toString());
						UIUtilities.showToast(getBaseContext(),
								R.string.error_no_market);
					}
				}
			});
		} else {
			mAdView.setVisibility(View.GONE);
		}

		if (itemCounts.size() == 0) {
			showDialog(NO_ITEM_DIALOG);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case ADD_REMOVE_ITEM_DIALOG:
			final List<String> selected = new ArrayList<String>();
			final List<String> unselected = new ArrayList<String>();

			return new AlertDialog.Builder(this)
					.setTitle(R.string.main_grid_item_dialog_title)
					.setMultiChoiceItems(titleArray, null,
							new DialogInterface.OnMultiChoiceClickListener() {
								public void onClick(
										DialogInterface dialogInterface,
										int pos, boolean state) {
									if (state)
										selected.add(titleArray[pos]);
									else
										unselected.add(titleArray[pos]);
								}
							})
					.setPositiveButton(
							getString(R.string.main_grid_item_dialog_change),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									final SharedPreferences pref = getBaseContext()
											.getSharedPreferences(
													Preferences.NAME, 0);
									for (Object i : unselected.toArray()) {
										if (selected.contains(i))
											selected.remove(i);
									}

									final String selectedString = selected
											.toString();

									SharedPreferences.Editor editor = pref
											.edit();
									editor.putString(
											Preferences.KEY_ITEMS,
											selectedString
													.substring(
															1,
															selectedString
																	.length() - 1));

									editor.commit();
									setupViews();
									dialog.dismiss();
								}
							})
					.setNegativeButton(getString(R.string.cancel_label),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).create();
		case NO_ITEM_DIALOG:
			return new AlertDialog.Builder(this)
					.setMessage(R.string.main_grid_insult)
					.setPositiveButton(getString(R.string.main_grid_ok_button),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							}).create();
		case CHANGE_LOCALE_EXPLANATION_DIALOG:
			return new AlertDialog.Builder(this)
					.setTitle(R.string.preferences_database_dialog_title)
					.setMessage(R.string.preferences_database_explanation)
					.setNeutralButton(getString(R.string.okay_label),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									showDialog(CHANGE_LOCALE_DIALOG);

									dialog.dismiss();
								}
							}).create();
		case CHANGE_LOCALE_DIALOG:
			final String[] regionArray = {
					getString(R.string.preferences_database_us),
					getString(R.string.preferences_database_ca),
					getString(R.string.preferences_database_uk),
					getString(R.string.preferences_database_fr),
					getString(R.string.preferences_database_de),
					getString(R.string.preferences_database_jp),
					getString(R.string.preferences_database_it),
					getString(R.string.preferences_database_cn) };

			return new AlertDialog.Builder(this)
					.setTitle(R.string.preferences_database_dialog_title)
					.setSingleChoiceItems(regionArray, 0,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									selectedLocale = which;
								}
							})
					.setPositiveButton(getString(R.string.okay_label),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									final SharedPreferences pref = getBaseContext()
											.getSharedPreferences(
													Preferences.NAME, 0);

									final String[] dbs = {
											ServerInfo.API_REST_HOST_US,
											ServerInfo.API_REST_HOST_CA,
											ServerInfo.API_REST_HOST_UK,
											ServerInfo.API_REST_HOST_FR,
											ServerInfo.API_REST_HOST_DE,
											ServerInfo.API_REST_HOST_JP,
											ServerInfo.API_REST_HOST_IT,
											ServerInfo.API_REST_HOST_CN };

									SharedPreferences.Editor editor = pref
											.edit();
									editor.putString(Preferences.KEY_DATABASE,
											dbs[selectedLocale]);

									editor.commit();

									dialog.dismiss();
								}
							}).create();
		}

		return super.onCreateDialog(id);
	}

	// Displays the What's New dialog if it's the first time the user starts a
	// new version.
	private int launchWhatsNewMessage() {
		lastVersion = -1;
		try {
			PackageInfo info = getPackageManager().getPackageInfo(PACKAGE_NAME,
					0);
			int currentVersion = info.versionCode;
			SharedPreferences prefs = this.getSharedPreferences(
					Preferences.NAME, 0);
			lastVersion = prefs.getInt(SettingsActivity.KEY_HELP_VERSION_SHOWN,
					0);
			if (currentVersion > lastVersion) {
				prefs.edit()
						.putInt(SettingsActivity.KEY_HELP_VERSION_SHOWN,
								currentVersion).commit();
				Intent intent = new Intent(this, HelpActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				// Show the default page on a clean install, and the what's new
				// page on an upgrade.
				String page = (lastVersion == 0) ? HelpActivity.DEFAULT_PAGE
						: HelpActivity.WHATS_NEW_PAGE;
				if (lastVersion == 0)
					ShelvesApplication.mFirstRun = true;
				intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, page);
				startActivityForResult(intent, CHANGE_LOCALE_EXPLANATION_DIALOG);
			}
		} catch (PackageManager.NameNotFoundException e) {
			Log.w("MainGridActivity: ", e);
		}

		return lastVersion;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			switch (requestCode) {
			case CHANGE_LOCALE_EXPLANATION_DIALOG:
				if (lastVersion <= 0)
					showDialog(CHANGE_LOCALE_EXPLANATION_DIALOG);
				break;
			}
		}
	}

	public class ImageAdapter extends BaseAdapter {
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			return mThumbIds.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			View cell = convertView;
			if (cell == null) { // if it's not recycled, initialize some
				// attributes
				LayoutInflater li = getLayoutInflater();
				cell = li.inflate(R.layout.main_list_selection, null);
			}

			TextView tv = (TextView) cell.findViewById(R.id.menu_text);
			tv.setText(mTitleIds.get(position));

			TextView textCount = (TextView) cell.findViewById(R.id.menu_count);
			textCount.setText(itemCounts.get(position) + " | "
					+ loanCounts.get(position) + " / "
					+ wishlistCounts.get(position));

			final ImageView iv = (ImageView) cell.findViewById(R.id.menu_image);
			iv.setImageResource(mThumbIds.get(position));

			return cell;
		}

		public ArrayList<String> getText() {
			return mTitleIds;
		}
	}

	private class CollectionSelector implements AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			startActivity(new Intent(view.getContext(),
					itemCollections.get(position)));
		}
	}
}