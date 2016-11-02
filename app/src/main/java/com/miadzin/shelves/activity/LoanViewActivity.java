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
import com.miadzin.shelves.util.loan.Calendars;

public class LoanViewActivity extends ListActivity {

	private final String LOG_TAG = "LoanViewActivity";

	private ArrayList<Loan> m_loans = null;
	private LoanAdapter m_adapter;
	private final int LOAN_VIEW_WAITING_DIALOG = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsUtils.getInstance(this).trackPageView("/" + LOG_TAG);
		setContentView(R.layout.loan_item_view);

		setupViews();
	}

	@Override
	protected void onResume() {
		super.onResume();

		setupViews();

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> parent,
					final View view, final int position, long time) {
				Loan l = m_adapter.items.get(position);
				viewLoanItem(l.loanItemType, l.loanItemId);

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
		new retrieveLoansTask().execute();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final ProgressDialog progressDialog = new ProgressDialog(
				LoanViewActivity.this);
		switch (id) {
		case LOAN_VIEW_WAITING_DIALOG:
			progressDialog.setTitle(getString(R.string.progress_dialog_wait));
			progressDialog.setIcon(android.R.drawable.ic_dialog_alert);
			progressDialog
					.setMessage(getString(R.string.loan_retrieving_dialog));
		}

		return progressDialog;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		getMenuInflater().inflate(R.menu.loan_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Loan l = m_adapter.items.get(info.position);

		switch (item.getItemId()) {
		case R.id.context_menu_item_loan_view:
			viewLoanItem(l.loanItemType, l.loanItemId);
			return true;
		case R.id.context_menu_item_loan_return:
			return removeLoan(l.loanItemType, l.loanItemId);

		}

		return super.onContextItemSelected(item);
	}

	private class retrieveLoansTask extends AsyncTask<Void, Void, Object> {

		private int loanCount;

		@Override
		protected void onPreExecute() {
			loanCount = 0;

			showDialog(LOAN_VIEW_WAITING_DIALOG);
		}

		@Override
		public Object doInBackground(Void... params) {
			ArrayList<Loan> loans = new ArrayList<Loan>();
			getLoans(loans);
			m_adapter = new LoanAdapter(getBaseContext(),
					R.layout.loan_item_rows, loans);

			return null;
		}

		private void getLoans(ArrayList<Loan> m_loans) {
			Cursor c = null;
			ContentResolver contentResolver = getContentResolver();

			HashSet<Uri> uris = new HashSet<Uri>(
					ShelvesApplication.TYPES_TO_URI.values());

			try {
				for (Uri uri : uris) {
					c = contentResolver
							.query(uri, new String[] { BaseItem.INTERNAL_ID,
									BaseItem.TITLE, BaseItem.LOANED_TO,
									BaseItem.LOAN_DATE }, BaseItem.LOANED_TO
									+ " NOT NULL AND " + BaseItem.LOANED_TO
									+ " != ''", null, null);

					if (c.moveToFirst()) {
						do {
							Loan l = new Loan();
							l.loanItemType = uri.toString();
							l.loanItemId = c.getString(0);
							l.loanName = c.getString(1);
							l.loanTo = c.getString(2);
							l.loanDate = c.getString(3);
							l.loanCover = ImageUtilities.getCachedCover(
									c.getString(0),
									new FastBitmapDrawable(BitmapFactory
											.decodeResource(getResources(),
													R.drawable.unknown_cover)));
							m_loans.add(l);
							loanCount++;
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
				dismissDialog(LOAN_VIEW_WAITING_DIALOG);
			} catch (IllegalArgumentException iae) {
				Log.e(LOG_TAG, "Dialog never shown: " + iae);
			}

			setListAdapter(m_adapter);
			registerForContextMenu(getListView());

			TabSelector.changeActionBarTitle(
					getString(R.string.application_name), loanCount);
		}
	}

	private class LoanAdapter extends ArrayAdapter<Loan> {
		private ArrayList<Loan> items;

		public LoanAdapter(Context context, int textViewResourceId,
				ArrayList<Loan> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater li = getLayoutInflater();
				convertView = li.inflate(R.layout.loan_item_rows, null);
			}
			Loan o = items.get(position);

			if (o != null) {
				TextView loan_title = (TextView) convertView
						.findViewById(R.id.loan_title);
				TextView loaned_to = (TextView) convertView
						.findViewById(R.id.loan_to);
				TextView loan_date = (TextView) convertView
						.findViewById(R.id.loan_date);
				ImageView loan_icon = (ImageView) convertView
						.findViewById(R.id.loan_icon);

				loan_title.setText(o.loanName);
				loaned_to.setText(o.loanTo);
				loan_date.setText(o.loanDate);
				loan_icon.setImageDrawable(o.loanCover);

			}

			return convertView;
		}

	}

	private void viewLoanItem(String mLoanItemType, String mLoanId) {
		if (mLoanItemType.contains("apparel"))
			ApparelDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
		else if (mLoanItemType.contains("boardgames"))
			BoardGameDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
		else if (mLoanItemType.contains("books"))
			BookDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
		else if (mLoanItemType.contains("gadgets"))
			GadgetDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
		else if (mLoanItemType.contains("movies"))
			MovieDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
		else if (mLoanItemType.contains("music"))
			MusicDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
		else if (mLoanItemType.contains("software"))
			SoftwareDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
		else if (mLoanItemType.contains("tools"))
			ToolDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
		else if (mLoanItemType.contains("toys"))
			ToyDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
		else if (mLoanItemType.contains("videogames"))
			VideoGameDetailsActivity.showFromOutside(getBaseContext(), mLoanId);
	}

	private boolean removeLoan(String mLoanItemType, String mLoanId) {
		ContentResolver cr = getContentResolver();
		ContentValues loanValues = new ContentValues();

		if (mLoanItemType.contains("apparel")) {
			Apparel apparel = ApparelManager.findApparel(getContentResolver(),
					mLoanId, null);

			if (apparel.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + apparel.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(ApparelStore.Apparel.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });
		} else if (mLoanItemType.contains("boardgames")) {
			BoardGame boardgame = BoardGamesManager.findBoardGame(
					getContentResolver(), mLoanId, null);

			if (boardgame.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + boardgame.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(BoardGamesStore.BoardGame.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });

		} else if (mLoanItemType.contains("books")) {
			Book book = BooksManager.findBook(getContentResolver(), mLoanId,
					null);

			if (book.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + book.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(BooksStore.Book.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });

		} else if (mLoanItemType.contains("gadgets")) {
			Gadget gadget = GadgetsManager.findGadget(getContentResolver(),
					mLoanId, null);

			if (gadget.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + gadget.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(GadgetsStore.Gadget.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });
		}

		else if (mLoanItemType.contains("movies")) {
			Movie movie = MoviesManager.findMovie(getContentResolver(),
					mLoanId, null);

			if (movie.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + movie.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(MoviesStore.Movie.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });
		} else if (mLoanItemType.contains("music")) {
			Music music = MusicManager.findMusic(getContentResolver(), mLoanId,
					null);

			if (music.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + music.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(MusicStore.Music.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });

		} else if (mLoanItemType.contains("software")) {
			Software software = SoftwareManager.findSoftware(
					getContentResolver(), mLoanId, null);

			if (software.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + software.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(SoftwareStore.Software.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });

		} else if (mLoanItemType.contains("tools")) {
			Tool tool = ToolsManager.findTool(getContentResolver(), mLoanId,
					null);

			if (tool.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + tool.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(ToolsStore.Tool.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });

		} else if (mLoanItemType.contains("toys")) {
			Toy toy = ToysManager.findToy(getContentResolver(), mLoanId, null);

			if (toy.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + toy.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(ToysStore.Toy.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });

		} else if (mLoanItemType.contains("videogames")) {
			VideoGame videogame = VideoGamesManager.findVideoGame(
					getContentResolver(), mLoanId, null);

			if (videogame.getEventId() > 0) {
				Calendars.setupCalendarUri();
				cr.delete(
						Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString()
								+ "/" + videogame.getEventId()), null, null);
			}
			loanValues.put(BaseItem.LOANED_TO, "");
			loanValues.put(BaseItem.LOAN_DATE, "");
			loanValues.put(BaseItem.EVENT_ID, -1);

			cr.update(VideoGamesStore.VideoGame.CONTENT_URI, loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mLoanId });
		}

		setupViews();
		UIUtilities.showToast(getBaseContext(), R.string.loan_returned);

		return true;
	}

	class Loan {
		String loanItemId;
		String loanName;
		String loanTo;
		String loanDate;
		String loanItemType;
		Drawable loanCover;
	}
}