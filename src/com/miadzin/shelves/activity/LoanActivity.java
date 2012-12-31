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

import java.util.Date;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.provider.apparel.ApparelManager;
import com.miadzin.shelves.provider.apparel.ApparelStore;
import com.miadzin.shelves.provider.boardgames.BoardGamesManager;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore;
import com.miadzin.shelves.provider.books.BooksManager;
import com.miadzin.shelves.provider.books.BooksStore;
import com.miadzin.shelves.provider.comics.ComicsManager;
import com.miadzin.shelves.provider.comics.ComicsStore;
import com.miadzin.shelves.provider.gadgets.GadgetsManager;
import com.miadzin.shelves.provider.gadgets.GadgetsStore;
import com.miadzin.shelves.provider.movies.MoviesManager;
import com.miadzin.shelves.provider.movies.MoviesStore;
import com.miadzin.shelves.provider.music.MusicManager;
import com.miadzin.shelves.provider.music.MusicStore;
import com.miadzin.shelves.provider.software.SoftwareManager;
import com.miadzin.shelves.provider.software.SoftwareStore;
import com.miadzin.shelves.provider.tools.ToolsManager;
import com.miadzin.shelves.provider.tools.ToolsStore;
import com.miadzin.shelves.provider.toys.ToysManager;
import com.miadzin.shelves.provider.toys.ToysStore;
import com.miadzin.shelves.provider.videogames.VideoGamesManager;
import com.miadzin.shelves.provider.videogames.VideoGamesStore;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.UIUtilities;
import com.miadzin.shelves.util.loan.Calendars;
import com.miadzin.shelves.util.loan.ContactAccessor;
import com.miadzin.shelves.util.loan.ContactInfo;
import com.miadzin.shelves.util.loan.DateControlSet;

public class LoanActivity extends Activity {

	private ApparelStore.Apparel mApparel;
	private BooksStore.Book mBook;
	private BoardGamesStore.BoardGame mBoardGame;
	private ComicsStore.Comic mComic;
	private GadgetsStore.Gadget mGadget;
	private MoviesStore.Movie mMovie;
	private MusicStore.Music mMusic;
	private SoftwareStore.Software mSoftware;
	private ToolsStore.Tool mTool;
	private ToysStore.Toy mToy;
	private VideoGamesStore.VideoGame mVideoGame;

	private String mID = null;
	private String mType = null;
	private String mAction = null;

	private Button contactSelectButton;
	private DateControlSet loanDateButtons;
	private CheckBox addToCalendar;
	private Button loanButton;
	private Button cancelButton;

	private String loanedToName;
	private String loanDate;

	// Request code for the contact picker activity
	private static final int PICK_CONTACT_REQUEST = 1;

	private static final String LOG_TAG = "LoanActivity";
	/**
	 * An SDK-specific instance of {@link ContactAccessor}. The activity does
	 * not need to know what SDK it is running in: all idiosyncrasies of
	 * different SDKs are encapsulated in the implementations of the
	 * ContactAccessor class.
	 */
	private final ContactAccessor mContactAccessor = new ContactAccessor();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsUtils.getInstance(this).trackPageView("/" + LOG_TAG);

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		mID = this.getIntent().getExtras().getString("itemID");
		mType = this.getIntent().getExtras().getString("type");
		mAction = this.getIntent().getExtras().getString("action");

		setContentView(R.layout.loan_item_dialog);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.dialog_title);

		final TextView loanTitle = (TextView) findViewById(R.id.dialogTitle);
		if (loanTitle != null) {
			loanTitle.setText(getString(R.string.loan_item_button) + ": "
					+ this.getIntent().getExtras().getString("title"));
		}

		Calendars.setupCalendarUri();

		contactSelectButton = (Button) findViewById(R.id.loan_item_to_button);
		contactSelectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pickContact();
			}
		});

		loanDateButtons = new DateControlSet(this,
				(Button) findViewById(R.id.loan_date),
				(Button) findViewById(R.id.loan_time));

		addToCalendar = (CheckBox) findViewById(R.id.loan_item_add_to_cal);

		if (!Calendars.isCalendarPresent(getBaseContext(),
				getString(R.string.preferences_calendar_default_id))) {
			addToCalendar.setEnabled(false);
		}

		loanButton = (Button) findViewById(R.id.loan_item_button);
		loanButton.setOnClickListener(new LoanItemListener(this, mID, mType));

		cancelButton = (Button) this.findViewById(R.id.loan_item_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});

		if (mAction.equals("change")) {
			if (mType.equals("apparel")) {
				mApparel = ApparelManager.findApparel(getContentResolver(),
						mID, SettingsActivity.getSortOrder(this));
				if (mApparel == null)
					finish();
				contactSelectButton.setText(mApparel.getLoanedTo());
				loanedToName = mApparel.getLoanedTo();
				loanDateButtons.setDate(mApparel.getLoanDate());
			} else if (mType.equals("boardgames")) {
				mBoardGame = BoardGamesManager.findBoardGame(
						getContentResolver(), mID,
						SettingsActivity.getSortOrder(this));
				if (mBoardGame == null)
					finish();
				contactSelectButton.setText(mBoardGame.getLoanedTo());
				loanedToName = mBoardGame.getLoanedTo();
				loanDateButtons.setDate(mBoardGame.getLoanDate());
			} else if (mType.equals("books")) {
				mBook = BooksManager.findBook(getContentResolver(), mID,
						SettingsActivity.getSortOrder(this));
				if (mBook == null)
					finish();
				contactSelectButton.setText(mBook.getLoanedTo());
				loanedToName = mBook.getLoanedTo();
				loanDateButtons.setDate(mBook.getLoanDate());
			} else if (mType.equals("comics")) {
				mComic = ComicsManager.findComic(getContentResolver(), mID,
						SettingsActivity.getSortOrder(this));
				if (mComic == null)
					finish();
				contactSelectButton.setText(mComic.getLoanedTo());
				loanedToName = mComic.getLoanedTo();
				loanDateButtons.setDate(mComic.getLoanDate());
			} else if (mType.equals("gadgets")) {
				mGadget = GadgetsManager.findGadget(getContentResolver(), mID,
						SettingsActivity.getSortOrder(this));
				if (mGadget == null)
					finish();
				contactSelectButton.setText(mGadget.getLoanedTo());
				loanedToName = mGadget.getLoanedTo();
				loanDateButtons.setDate(mGadget.getLoanDate());
			} else if (mType.equals("movies")) {
				mMovie = MoviesManager.findMovie(getContentResolver(), mID,
						SettingsActivity.getSortOrder(this));
				if (mMovie == null)
					finish();
				contactSelectButton.setText(mMovie.getLoanedTo());
				loanedToName = mMovie.getLoanedTo();
				loanDateButtons.setDate(mMovie.getLoanDate());
			} else if (mType.equals("music")) {
				mMusic = MusicManager.findMusic(getContentResolver(), mID,
						SettingsActivity.getSortOrder(this));
				if (mMusic == null)
					finish();
				contactSelectButton.setText(mMusic.getLoanedTo());
				loanedToName = mMusic.getLoanedTo();
				loanDateButtons.setDate(mMusic.getLoanDate());
			} else if (mType.equals("software")) {
				mSoftware = SoftwareManager.findSoftware(getContentResolver(),
						mID, SettingsActivity.getSortOrder(this));
				if (mSoftware == null)
					finish();
				contactSelectButton.setText(mSoftware.getLoanedTo());
				loanedToName = mSoftware.getLoanedTo();
				loanDateButtons.setDate(mSoftware.getLoanDate());
			} else if (mType.equals("tools")) {
				mTool = ToolsManager.findTool(getContentResolver(), mID,
						SettingsActivity.getSortOrder(this));
				if (mTool == null)
					finish();
				contactSelectButton.setText(mTool.getLoanedTo());
				loanedToName = mTool.getLoanedTo();
				loanDateButtons.setDate(mTool.getLoanDate());
			} else if (mType.equals("toys")) {
				mToy = ToysManager.findToy(getContentResolver(), mID,
						SettingsActivity.getSortOrder(this));
				if (mToy == null)
					finish();
				contactSelectButton.setText(mToy.getLoanedTo());
				loanedToName = mToy.getLoanedTo();
				loanDateButtons.setDate(mToy.getLoanDate());
			} else if (mType.equals("videogames")) {
				mVideoGame = VideoGamesManager.findVideoGame(
						getContentResolver(), mID,
						SettingsActivity.getSortOrder(this));
				if (mVideoGame == null)
					finish();
				contactSelectButton.setText(mVideoGame.getLoanedTo());
				loanedToName = mVideoGame.getLoanedTo();
				loanDateButtons.setDate(mVideoGame.getLoanDate());
			}
		}
	}

	/**
	 * Click handler for the Pick Contact button. Invokes a contact picker
	 * activity. The specific intent used to bring up that activity differs
	 * between versions of the SDK, which is why we delegate the creation of the
	 * intent to ContactAccessor.
	 */
	protected void pickContact() {
		startActivityForResult(mContactAccessor.getPickContactIntent(),
				PICK_CONTACT_REQUEST);
	}

	/**
	 * Invoked when the contact picker activity is finished. The
	 * {@code contactUri} parameter will contain a reference to the contact
	 * selected by the user. We will treat it as an opaque URI and allow the
	 * SDK-specific ContactAccessor to handle the URI accordingly.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
			loadContactInfo(data.getData());
		}
	}

	/**
	 * Load contact information on a background thread.
	 */
	private void loadContactInfo(Uri contactUri) {
		AsyncTask<Uri, Void, ContactInfo> task = new AsyncTask<Uri, Void, ContactInfo>() {

			@Override
			protected ContactInfo doInBackground(Uri... uris) {
				return mContactAccessor.loadContact(getContentResolver(),
						uris[0]);
			}

			@Override
			protected void onPostExecute(ContactInfo result) {
				loanedToName = result.getDisplayName();
				contactSelectButton.setText(loanedToName);
			}
		};

		task.execute(contactUri);
	}

	public class LoanItemListener implements OnClickListener {
		private Activity activity;
		private String mID;
		private String mType;

		public LoanItemListener(Activity activity, String id, String type) {
			this.activity = activity;
			this.mID = id;
			this.mType = type;
		}

		public void onClick(View v) {
			String loanTitle = null;
			String eventId = null;
			String title = null;
			String description = null;
			ContentResolver cr = getContentResolver();
			ContentValues loanValues = new ContentValues();
			Context context = activity.getApplicationContext();

			loanDate = Preferences.getDateFormat().format(
					loanDateButtons.getDate());

			if (loanedToName == null) {
				UIUtilities.showToast(getBaseContext(),
						R.string.loan_error_need_contact);
				return;
			}

			if (mApparel != null && mApparel.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mApparel.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mBoardGame != null && mBoardGame.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mBoardGame.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mBook != null && mBook.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mBook.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mComic != null && mComic.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mComic.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mGadget != null && mGadget.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mGadget.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mMovie != null && mMovie.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mMovie.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mMusic != null && mMusic.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mMusic.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mSoftware != null && mSoftware.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mSoftware.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mTool != null && mTool.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mTool.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mToy != null && mToy.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mToy.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			} else if (mVideoGame != null && mVideoGame.getEventId() > 0) {
				Uri eventURI = Uri.parse(Calendars.CALENDAR_EVENTS_URI
						.toString() + "/" + mVideoGame.getEventId());
				Cursor c = cr.query(eventURI, null, null, null, null);
				if (c.moveToFirst()) {
					do {
						title = c.getString(c.getColumnIndexOrThrow("title"));
						description = c.getString(c
								.getColumnIndexOrThrow("description"));
					} while (c.moveToNext());
					if (c != null) {
						c.close();
					}
				}

				cr.delete(eventURI, null, null);
			}

			if (addToCalendar.isChecked()) {
				if (mType.equals("apparel")) {
					loanTitle = ApparelManager.findApparel(
							getContentResolver(), mID, null).getTitle();
				} else if (mType.equals("boardgames")) {
					loanTitle = BoardGamesManager.findBoardGame(
							getContentResolver(), mID, null).getTitle();
				} else if (mType.equals("books")) {
					loanTitle = BooksManager.findBook(getContentResolver(),
							mID, null).getTitle();
				} else if (mType.equals("gadgets")) {
					loanTitle = GadgetsManager.findGadget(getContentResolver(),
							mID, null).getTitle();
				} else if (mType.equals("movies")) {
					loanTitle = MoviesManager.findMovie(getContentResolver(),
							mID, null).getTitle();
				} else if (mType.equals("music")) {
					loanTitle = MusicManager.findMusic(getContentResolver(),
							mID, null).getTitle();
				} else if (mType.equals("software")) {
					loanTitle = SoftwareManager.findSoftware(
							getContentResolver(), mID, null).getTitle();
				} else if (mType.equals("tools")) {
					loanTitle = ToolsManager.findTool(getContentResolver(),
							mID, null).getTitle();
				} else if (mType.equals("toys")) {
					loanTitle = ToysManager.findToy(getContentResolver(), mID,
							null).getTitle();
				} else if (mType.equals("videogames")) {
					loanTitle = VideoGamesManager.findVideoGame(
							getContentResolver(), mID, null).getTitle();
				}

				if (ShelvesApplication.mCalendarAPIAvailable) {
					Intent calIntent = new Intent(Intent.ACTION_INSERT);
					calIntent.setType("vnd.android.cursor.item/event");
					calIntent.putExtra(
							Events.TITLE,
							getString(R.string.loan_calendar_title, loanTitle,
									loanedToName));
					if (description != null) {
						calIntent.putExtra(Events.DESCRIPTION, description);
					} else {
						calIntent.putExtra(Events.DESCRIPTION,
								getString(R.string.loan_calendar_description));
					}

					Long dueDate = createCalendarTimes(loanDateButtons
							.getDate());

					calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
							dueDate);
					calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
							dueDate);

					startActivity(calIntent);
				} else {
					ContentValues calendarVals = new ContentValues();

					if (title != null) {
						calendarVals.put("title", title);
					} else {
						calendarVals.put(
								"title",
								getString(R.string.loan_calendar_title,
										loanTitle, loanedToName));
					}
					calendarVals.put("calendar_id",
							SettingsActivity.getCalendar(getBaseContext()));
					if (description != null) {
						calendarVals.put("description", description);
					} else {
						calendarVals.put("description",
								getString(R.string.loan_calendar_description));
					}
					calendarVals.put("hasAlarm", 0);
					calendarVals.put("transparency", 0);
					calendarVals.put("visibility", 0);

					createCalendarStartEndTimes(loanDateButtons.getDate(),
							calendarVals);

					try {
						Uri result = cr.insert(Calendars.CALENDAR_EVENTS_URI,
								calendarVals);
						eventId = result.getPathSegments().get(
								result.getPathSegments().size() - 1);

					} catch (IllegalArgumentException e) {
						Log.e("LoanActivity", "Error creating calendar event!",
								e);
						UIUtilities.showToast(context,
								R.string.loan_unsuccessful);
					}
				}
			}

			loanValues.put(BaseItem.LOANED_TO, loanedToName);
			loanValues.put(BaseItem.LOAN_DATE, loanDate);

			if (eventId != null) {
				loanValues.put(BaseItem.EVENT_ID, eventId);
			}

			getContentResolver().update(
					ShelvesApplication.TYPES_TO_URI.get(mType), loanValues,
					BaseItem.INTERNAL_ID + "=?", new String[] { mID });

			UIUtilities.showToast(context, R.string.loan_successful);
			setResult(RESULT_OK);
			finish();
		}
	}

	/**
	 * Take the values from the model and set the calendar start and end times
	 * based on these. Sets keys 'dtstart' and 'dtend'.
	 * 
	 * @param definite
	 *            definite due date or null
	 * @param values
	 */
	public static void createCalendarStartEndTimes(Date definite,
			ContentValues values) {
		Long deadlineDate = createCalendarTimes(definite);

		values.put("dtstart", deadlineDate);
		values.put("dtend", deadlineDate);
	}

	public static Long createCalendarTimes(Date definite) {
		if (definite != null)
			return definite.getTime();
		else
			return System.currentTimeMillis() + 24 * 3600 * 1000L;
	}
}
