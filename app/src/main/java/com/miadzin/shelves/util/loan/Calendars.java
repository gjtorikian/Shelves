package com.miadzin.shelves.util.loan;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.ListPreference;
import android.provider.CalendarContract;
import android.util.Log;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.UIUtilities;

public class Calendars {

	public static Uri CALENDAR_CONTENT_URI;
	public static Uri CALENDAR_EVENTS_URI;

	private static final boolean USE_ICS_NAMES = UIUtilities.isICS();

	private static final String ID_COLUMN_NAME = "_id";
	private static final String ICS_CALENDAR_PREFIX = "calendar_";
	private static final String DISPLAY_COLUMN_NAME = (USE_ICS_NAMES ? ICS_CALENDAR_PREFIX
			: "")
			+ "displayName";
	private static final String ACCESS_LEVEL_COLUMN_NAME = (USE_ICS_NAMES ? ICS_CALENDAR_PREFIX
			: "")
			+ "access_level";

	private static final String[] CALENDARS_PROJECTION = new String[] {
			ID_COLUMN_NAME, // Calendars._ID,
			DISPLAY_COLUMN_NAME // Calendars.DISPLAY_NAME
	};

	// Only show calendars that the user can modify. Access level 500
	// corresponds to Calendars.CONTRIBUTOR_ACCESS
	private static final String CALENDARS_WHERE = ACCESS_LEVEL_COLUMN_NAME
			+ ">= 500";

	private static final String CALENDARS_WHERE_ID = ACCESS_LEVEL_COLUMN_NAME
			+ " >= 500 AND " + ID_COLUMN_NAME + "=?";

	private static final String CALENDARS_SORT = DISPLAY_COLUMN_NAME + " ASC";

	@TargetApi(14)
	public static void setupCalendarUri() {
		if (Build.VERSION.SDK_INT >= 14) {
			CALENDAR_CONTENT_URI = CalendarContract.Calendars.CONTENT_URI;
			CALENDAR_EVENTS_URI = CalendarContract.Events.CONTENT_URI;
		} else if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
			CALENDAR_CONTENT_URI = Uri
					.parse("content://com.android.calendar/calendars");
			CALENDAR_EVENTS_URI = Uri
					.parse("content://com.android.calendar/events");
		} else {
			CALENDAR_CONTENT_URI = Uri.parse("content://calendar/calendars");
			CALENDAR_EVENTS_URI = Uri.parse("content://calendar/events");
		}
	}

	private static String LOG_TAG = "Calendars";

	/**
	 * Appends all user-modifiable calendars to listPreference.
	 * 
	 * @param context
	 *            context
	 * @param listPreference
	 *            preference to init
	 */
	public static void initCalendarsPreference(Context context,
			ListPreference listPreference) {

		setupCalendarUri();

		ContentResolver cr = context.getContentResolver();
		Resources r = context.getResources();
		Cursor c = cr.query(CALENDAR_CONTENT_URI, CALENDARS_PROJECTION,
				CALENDARS_WHERE, null, CALENDARS_SORT);

		// Fetch the current setting. Invalid calendar id will
		// be changed to default value.
		final SharedPreferences pref = context.getSharedPreferences(
				Preferences.NAME, 0);
		String currentSetting = pref.getString(Preferences.KEY_CALENDAR, "1");

		int currentSettingIndex = -1;

		if (c == null || c.getCount() == 0) {
			// Something went wrong when querying calendars
			// Keep it to none
			listPreference
					.setEntries(new String[] { r
							.getString(R.string.preferences_calendar_default_shelves) });
			listPreference.setEntryValues(new String[] { r
					.getString(R.string.preferences_calendar_default_id) });
			listPreference.setValueIndex(0);
			listPreference
					.setSummary(r.getString(
							R.string.preferences_database_summary,
							r.getString(R.string.preferences_calendar_default_shelves)));
			listPreference.setEnabled(true);
			c.close();
			return;
		}

		int calendarCount = c.getCount();

		String[] entries = new String[calendarCount];
		String[] entryValues = new String[calendarCount];

		// Iterate calendars one by one, and fill up the list preference
		try {
			int row = 0;
			int idColumn = c.getColumnIndex(ID_COLUMN_NAME);
			int nameColumn = c.getColumnIndex(DISPLAY_COLUMN_NAME);
			while (c.moveToNext()) {
				String id = c.getString(idColumn);
				String name = c.getString(nameColumn);
				entries[row] = name;
				entryValues[row] = id;

				// Found currently selected calendar
				if (currentSetting.equals(id)) {
					currentSettingIndex = row;
				}

				row++;
			}

			if (currentSettingIndex <= 0) {
				// Should not happen!
				// Keep the default of none
				Log.d("Calendars", "initCalendarsPreference: Unknown calendar.");
				listPreference
						.setEntries(new String[] { r
								.getString(R.string.preferences_calendar_default_shelves) });
				listPreference.setEntryValues(new String[] { r
						.getString(R.string.preferences_calendar_default_id) });
				listPreference.setValueIndex(0);
				listPreference
						.setSummary(r.getString(
								R.string.preferences_calendar_summary,
								r.getString(R.string.preferences_calendar_default_shelves)));
				listPreference.setEnabled(true);
			} else if (currentSettingIndex >= entryValues.length) {
				currentSettingIndex = 0;
			}

			listPreference.setEntries(entries);
			listPreference.setEntryValues(entryValues);

			listPreference.setValueIndex(currentSettingIndex);
			listPreference.setEnabled(true);
			listPreference.setSummary(r.getString(
					R.string.preferences_calendar_summary,
					listPreference.getEntries()[currentSettingIndex]));

		} finally {
			c.close();
		}
	}

	public static String getCalendarString(Context context, String pos) {
		setupCalendarUri();

		ContentResolver cr = context.getContentResolver();
		Cursor c = cr.query(CALENDAR_CONTENT_URI, CALENDARS_PROJECTION,
				CALENDARS_WHERE, null, CALENDARS_SORT);

		String calendarName = "<empty>";

		if (c != null) {
			try {
				int row = 0;
				int idColumn = c.getColumnIndex(ID_COLUMN_NAME);
				int nameColumn = c.getColumnIndex(DISPLAY_COLUMN_NAME);
				while (c.moveToNext()) {
					String id = c.getString(idColumn);
					String name = c.getString(nameColumn);

					if (pos.equals(id)) {
						calendarName = name;
						break;
					}
				}
			} finally {
				c.close();
			}
		}

		return calendarName;
	}

	/**
	 * Checks whether user-modifiable calendar is present with a given id.
	 * 
	 * @param context
	 *            Context
	 * @param id
	 *            Calendar ID to search for
	 * @return true, if user-modifiable calendar with the given id exists; false
	 *         otherwise.
	 */
	public static boolean isCalendarPresent(Context context, String id) {
		if (id == null)
			return false;

		if (ShelvesApplication.mCalendarAPIAvailable)
			return true;

		ContentResolver cr = context.getContentResolver();
		Cursor c = null;
		setupCalendarUri();
		try {
			c = cr.query(CALENDAR_CONTENT_URI, CALENDARS_PROJECTION,
					CALENDARS_WHERE_ID, new String[] { id }, CALENDARS_SORT);
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return (c != null) && (c.getCount() != 0);
	}

	public static void deleteCalendar(ContentResolver contentResolver,
			BaseItem bi) {
		try {
			Calendars.setupCalendarUri();
			contentResolver.delete(
					Uri.parse(Calendars.CALENDAR_EVENTS_URI.toString() + "/"
							+ bi.getEventId()), null, null);
		} catch (IllegalArgumentException iae) {
			Log.e(LOG_TAG,
					"Unknown URI: " + Calendars.CALENDAR_EVENTS_URI.toString()
							+ "/" + bi.getEventId() + iae.toString());
		}
	}
}