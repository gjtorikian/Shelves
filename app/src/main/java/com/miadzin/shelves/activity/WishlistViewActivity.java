package com.miadzin.shelves.activity;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.activity.apparel.ApparelDetailsActivity;
import com.miadzin.shelves.activity.boardgames.BoardGameDetailsActivity;
import com.miadzin.shelves.activity.books.BookDetailsActivity;
import com.miadzin.shelves.activity.gadgets.GadgetDetailsActivity;
import com.miadzin.shelves.activity.movies.MovieDetailsActivity;
import com.miadzin.shelves.activity.music.MusicDetailsActivity;
import com.miadzin.shelves.activity.software.SoftwareDetailsActivity;
import com.miadzin.shelves.activity.tools.ToolDetailsActivity;
import com.miadzin.shelves.activity.toys.ToyDetailsActivity;
import com.miadzin.shelves.activity.videogames.VideoGameDetailsActivity;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.drawable.FastBitmapDrawable;
import com.miadzin.shelves.provider.apparel.ApparelManager;
import com.miadzin.shelves.provider.apparel.ApparelStore;
import com.miadzin.shelves.provider.apparel.ApparelStore.Apparel;
import com.miadzin.shelves.provider.boardgames.BoardGamesManager;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore.BoardGame;
import com.miadzin.shelves.provider.books.BooksManager;
import com.miadzin.shelves.provider.books.BooksStore;
import com.miadzin.shelves.provider.books.BooksStore.Book;
import com.miadzin.shelves.provider.gadgets.GadgetsManager;
import com.miadzin.shelves.provider.gadgets.GadgetsStore;
import com.miadzin.shelves.provider.gadgets.GadgetsStore.Gadget;
import com.miadzin.shelves.provider.movies.MoviesManager;
import com.miadzin.shelves.provider.movies.MoviesStore;
import com.miadzin.shelves.provider.movies.MoviesStore.Movie;
import com.miadzin.shelves.provider.music.MusicManager;
import com.miadzin.shelves.provider.music.MusicStore;
import com.miadzin.shelves.provider.music.MusicStore.Music;
import com.miadzin.shelves.provider.software.SoftwareManager;
import com.miadzin.shelves.provider.software.SoftwareStore;
import com.miadzin.shelves.provider.software.SoftwareStore.Software;
import com.miadzin.shelves.provider.tools.ToolsManager;
import com.miadzin.shelves.provider.tools.ToolsStore;
import com.miadzin.shelves.provider.tools.ToolsStore.Tool;
import com.miadzin.shelves.provider.toys.ToysManager;
import com.miadzin.shelves.provider.toys.ToysStore;
import com.miadzin.shelves.provider.toys.ToysStore.Toy;
import com.miadzin.shelves.provider.videogames.VideoGamesManager;
import com.miadzin.shelves.provider.videogames.VideoGamesStore;
import com.miadzin.shelves.provider.videogames.VideoGamesStore.VideoGame;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class WishlistViewActivity extends ListActivity {

	private final String LOG_TAG = "WishlistViewActivity";

	private ArrayList<Wishlist> m_wishlist = null;
	private WishlistAdapter m_adapter;
	private final int WISHLIST_VIEW_WAITING_DIALOG = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsUtils.getInstance(this).trackPageView("/" + LOG_TAG);
		setContentView(R.layout.wishlist_item_view);
		setupViews();
	}

	@Override
	protected void onResume() {
		super.onResume();

		setupViews();

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> parent,
					final View view, final int position, long time) {
				Wishlist w = m_adapter.items.get(position);
				viewWishlistItem(w.wishlistItemType, w.wishlistItemId);

			}
		});
	}

	void setupViews() {
		AdView mAdView = (AdView) findViewById(R.id.adview);
		if (!UIUtilities.isPaid(getContentResolver(), this)) {
			mAdView.setVisibility(View.VISIBLE);
			AdRequest adRequest = new AdRequest.Builder().build();
			mAdView.loadAd(adRequest);
		} else {
			mAdView.setVisibility(View.GONE);
		}
		new retrieveWishlistsTask().execute();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final ProgressDialog progressDialog = new ProgressDialog(
				WishlistViewActivity.this);
		switch (id) {
		case WISHLIST_VIEW_WAITING_DIALOG:
			progressDialog.setTitle(getString(R.string.progress_dialog_wait));
			progressDialog.setIcon(android.R.drawable.ic_dialog_alert);
			progressDialog
					.setMessage(getString(R.string.wishlist_retrieving_dialog));
		}

		return progressDialog;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		getMenuInflater().inflate(R.menu.wishlist_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Wishlist w = m_adapter.items.get(info.position);

		switch (item.getItemId()) {
		case R.id.context_menu_item_wishlist_view:
			viewWishlistItem(w.wishlistItemType, w.wishlistItemId);
			return true;
		case R.id.context_menu_item_wishlist_return:
			return removeWishlistItem(w.wishlistItemType, w.wishlistItemId);

		}

		return super.onContextItemSelected(item);
	}

	private class retrieveWishlistsTask extends AsyncTask<Void, Void, Object> {

		private int wishlistCount;

		@Override
		protected void onPreExecute() {
			wishlistCount = 0;

			showDialog(WISHLIST_VIEW_WAITING_DIALOG);
		}

		@Override
		public Object doInBackground(Void... params) {
			ArrayList<Wishlist> wishlists = new ArrayList<Wishlist>();
			getWishlists(wishlists);
			m_adapter = new WishlistAdapter(getBaseContext(),
					R.layout.wishlist_item_rows, wishlists);

			return null;
		}

		private void getWishlists(ArrayList<Wishlist> m_wishlists) {
			Cursor c = null;
			ContentResolver contentResolver = getContentResolver();

			HashSet<Uri> uris = new HashSet<Uri>(
					ShelvesApplication.TYPES_TO_URI.values());

			try {
				for (Uri uri : uris) {
					c = contentResolver.query(uri, new String[] {
							BaseItem.INTERNAL_ID, BaseItem.TITLE,
							BaseItem.WISHLIST_DATE }, BaseItem.WISHLIST_DATE
							+ " NOT NULL AND " + BaseItem.WISHLIST_DATE
							+ " != ''", null, null);

					if (c.moveToFirst()) {
						do {
							Wishlist w = new Wishlist();
							w.wishlistItemType = uri.toString();
							w.wishlistItemId = c.getString(0);
							w.wishlistName = c.getString(1);
							w.wishlistDate = c.getString(2);
							w.wishlistCover = ImageUtilities.getCachedCover(
									c.getString(0),
									new FastBitmapDrawable(BitmapFactory
											.decodeResource(getResources(),
													R.drawable.unknown_cover)));
							m_wishlists.add(w);
							wishlistCount++;
						} while (c.moveToNext());
					}

					if (c != null) {
						c.close();
					}
				}

			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage());
			}
		}

		@Override
		protected void onPostExecute(Object result) {
			try {
				dismissDialog(WISHLIST_VIEW_WAITING_DIALOG);
			} catch (IllegalArgumentException iae) {
				Log.e(LOG_TAG, "Dialog never shown: " + iae);
			}

			setListAdapter(m_adapter);
			registerForContextMenu(getListView());

			TabSelector.changeActionBarTitle(
					getString(R.string.application_name), wishlistCount);
		}
	}

	private class WishlistAdapter extends ArrayAdapter<Wishlist> {
		private ArrayList<Wishlist> items;

		public WishlistAdapter(Context context, int textViewResourceId,
				ArrayList<Wishlist> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater li = getLayoutInflater();
				convertView = li.inflate(R.layout.wishlist_item_rows, null);
			}
			Wishlist o = items.get(position);
			if (o != null) {
				TextView wishlist_title = (TextView) convertView
						.findViewById(R.id.wishlist_title);
				TextView wishlist_date = (TextView) convertView
						.findViewById(R.id.wishlist_date);
				ImageView wishlist_icon = (ImageView) convertView
						.findViewById(R.id.wishlist_icon);

				wishlist_title.setText(o.wishlistName);
				wishlist_date.setText(o.wishlistDate);
				wishlist_icon.setImageDrawable(o.wishlistCover);

			}

			return convertView;
		}

	}

	private void viewWishlistItem(String mWishlistItemType, String mWishlistId) {
		if (mWishlistItemType.contains("apparel"))
			ApparelDetailsActivity.showFromOutside(getBaseContext(),
					mWishlistId);
		else if (mWishlistItemType.contains("boardgames"))
			BoardGameDetailsActivity.showFromOutside(getBaseContext(),
					mWishlistId);
		else if (mWishlistItemType.contains("books"))
			BookDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("gadgets"))
			GadgetDetailsActivity
					.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("movies"))
			MovieDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("music"))
			MusicDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("software"))
			SoftwareDetailsActivity.showFromOutside(getBaseContext(),
					mWishlistId);
		else if (mWishlistItemType.contains("tools"))
			ToolDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("toys"))
			ToyDetailsActivity.showFromOutside(getBaseContext(), mWishlistId);
		else if (mWishlistItemType.contains("videogames"))
			VideoGameDetailsActivity.showFromOutside(getBaseContext(),
					mWishlistId);
	}

	private boolean removeWishlistItem(String mWishlistItemType,
			String mWishlistId) {
		ContentResolver cr = getContentResolver();
		ContentValues wishlistValues = new ContentValues();

		if (mWishlistItemType.contains("apparel")) {
			Apparel apparel = ApparelManager.findApparel(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(ApparelStore.Apparel.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });
		} else if (mWishlistItemType.contains("boardgames")) {
			BoardGame boardgame = BoardGamesManager.findBoardGame(
					getContentResolver(), mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(BoardGamesStore.BoardGame.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("books")) {
			Book book = BooksManager.findBook(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(BooksStore.Book.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("gadgets")) {
			Gadget gadget = GadgetsManager.findGadget(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(GadgetsStore.Gadget.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });
		}

		else if (mWishlistItemType.contains("movies")) {
			Movie movie = MoviesManager.findMovie(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(MoviesStore.Movie.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });
		} else if (mWishlistItemType.contains("music")) {
			Music music = MusicManager.findMusic(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(MusicStore.Music.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("software")) {
			Software software = SoftwareManager.findSoftware(
					getContentResolver(), mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(SoftwareStore.Software.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("tools")) {
			Tool tool = ToolsManager.findTool(getContentResolver(),
					mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(ToolsStore.Tool.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("toys")) {
			Toy toy = ToysManager.findToy(getContentResolver(), mWishlistId,
					null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(ToysStore.Toy.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });

		} else if (mWishlistItemType.contains("videogames")) {
			VideoGame videogame = VideoGamesManager.findVideoGame(
					getContentResolver(), mWishlistId, null);

			wishlistValues.put(BaseItem.WISHLIST_DATE, "");

			cr.update(VideoGamesStore.VideoGame.CONTENT_URI, wishlistValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mWishlistId });
		}

		setupViews();
		UIUtilities.showToast(getBaseContext(), R.string.wishlist_removed_item);

		return true;
	}

	class Wishlist {
		String wishlistItemId;
		String wishlistName;
		String wishlistDate;
		String wishlistItemType;
		Drawable wishlistCover;
	}
}