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

package com.miadzin.shelves.base;

import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.activity.LoadImagesActivity;
import com.miadzin.shelves.drawable.FastBitmapDrawable;
import com.miadzin.shelves.provider.apparel.ApparelManager;
import com.miadzin.shelves.provider.apparel.ApparelStore.Apparel;
import com.miadzin.shelves.provider.boardgames.BoardGamesManager;
import com.miadzin.shelves.provider.boardgames.BoardGamesStore.BoardGame;
import com.miadzin.shelves.provider.books.BooksManager;
import com.miadzin.shelves.provider.books.BooksStore.Book;
import com.miadzin.shelves.provider.comics.ComicsManager;
import com.miadzin.shelves.provider.comics.ComicsStore.Comic;
import com.miadzin.shelves.provider.gadgets.GadgetsManager;
import com.miadzin.shelves.provider.gadgets.GadgetsProvider;
import com.miadzin.shelves.provider.gadgets.GadgetsStore.Gadget;
import com.miadzin.shelves.provider.movies.MoviesManager;
import com.miadzin.shelves.provider.movies.MoviesProvider;
import com.miadzin.shelves.provider.movies.MoviesStore.Movie;
import com.miadzin.shelves.provider.music.MusicManager;
import com.miadzin.shelves.provider.music.MusicProvider;
import com.miadzin.shelves.provider.music.MusicStore.Music;
import com.miadzin.shelves.provider.software.SoftwareManager;
import com.miadzin.shelves.provider.software.SoftwareProvider;
import com.miadzin.shelves.provider.software.SoftwareStore.Software;
import com.miadzin.shelves.provider.tools.ToolsManager;
import com.miadzin.shelves.provider.tools.ToolsProvider;
import com.miadzin.shelves.provider.tools.ToolsStore.Tool;
import com.miadzin.shelves.provider.toys.ToysManager;
import com.miadzin.shelves.provider.toys.ToysProvider;
import com.miadzin.shelves.provider.toys.ToysStore.Toy;
import com.miadzin.shelves.provider.videogames.VideoGamesManager;
import com.miadzin.shelves.provider.videogames.VideoGamesProvider;
import com.miadzin.shelves.provider.videogames.VideoGamesStore.VideoGame;
import com.miadzin.shelves.util.AnalyticsUtils;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.ImportUtilities;
import com.miadzin.shelves.util.Preferences;
import com.miadzin.shelves.util.TextUtilities;
import com.miadzin.shelves.util.UIUtilities;

public class BaseManualAddActivity extends Activity {
	private final String LOG_TAG = "BaseManualAddActivity";
	public static final String manualSuffix = "_manual";

	private Pattern[] mKeyPrefixes;
	private Pattern[] mKeySuffixes;

	private int mRating;

	private final int ADD_NEW_COVER = 0;

	private Bitmap bitmap;
	private Bitmap startingCover;
	private ImageView editCover;
	private Button removeCoverButton;
	private boolean coverChanged = false;

	private Spinner monthSpinner;
	protected String month;
	private Spinner month_theatrical_Spinner;
	protected String month_theatrical;

	EditText edit_ean;
	EditText edit_department;
	EditText edit_fabric;
	EditText edit_features;
	EditText edit_isbn;
	EditText edit_publishers;
	EditText edit_pages;
	EditText edit_year;
	EditText edit_artists;
	EditText edit_characters;
	EditText edit_format;
	EditText edit_edition;
	EditText edit_languages;
	EditText edit_dewey;
	EditText edit_actors;
	EditText edit_studio;
	EditText edit_running_time;
	EditText edit_year_theatrical;
	EditText edit_audience;
	EditText edit_language;
	EditText edit_label;
	EditText edit_platform;
	EditText edit_genre;
	EditText edit_minPlayers;
	EditText edit_maxPlayers;
	EditText edit_age;
	EditText edit_playingTime;
	EditText edit_quantity;

	Apparel apparel;
	BoardGame boardgame;
	Book book;
	Comic comic;
	Gadget gadget;
	Movie movie;
	Music music;
	Software software;
	Tool tool;
	Toy toy;
	VideoGame videogame;

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);

		final Context c = getBaseContext();
		final String sActivity = this.toString();
		AnalyticsUtils.getInstance(c)
				.trackPageView(
						"/"
								+ sActivity.substring(0,
										sActivity.indexOf("Activity") + 8));

		final String type = this.getIntent().getExtras().getString("type");
		final String mID = this.getIntent().getExtras().getString("mID");
		final Uri uri;

		if (type.contains("apparel")) {
			setContentView(R.layout.new_blank_apparel);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.apparel_label));

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_department = (EditText) findViewById(R.id.edit_department);
			edit_fabric = (EditText) findViewById(R.id.edit_fabric);
			edit_features = (EditText) findViewById(R.id.edit_features);
			edit_quantity = (EditText) findViewById(R.id.edit_quantity);

			if (mID != null) {
				apparel = ApparelManager.findApparel(getContentResolver(), mID,
						null);

				edit_ean.setText(apparel.getEan());
				edit_department.setText(apparel.getDepartment());
				edit_fabric.setText(apparel.getFabric());
				edit_features.setText(TextUtilities.removeBrackets(apparel
						.getFeatures().toString()));
			}
		} else if (type.contains("boardgames")) {
			setContentView(R.layout.new_blank_boardgame);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.boardgame_label_plural_small));

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_year = (EditText) findViewById(R.id.edit_year);
			edit_minPlayers = (EditText) findViewById(R.id.edit_min_players);
			edit_maxPlayers = (EditText) findViewById(R.id.edit_max_players);
			edit_age = (EditText) findViewById(R.id.edit_age);
			edit_playingTime = (EditText) findViewById(R.id.edit_playing_time);

			if (mID != null) {
				boardgame = BoardGamesManager.findBoardGame(
						getContentResolver(), mID, null);

				edit_ean.setText(boardgame.getEan());
				edit_year.setText(boardgame.getPublicationDate());
				edit_minPlayers.setText(boardgame.getMinPlayers());
				edit_maxPlayers.setText(boardgame.getMaxPlayers());
				edit_age.setText(boardgame.getAge());
				edit_playingTime.setText(boardgame.getPlayingTime());
			}
		} else if (type.contains("books")) {
			setContentView(R.layout.new_blank_book);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.book_label_plural_small));

			monthSpinner = (Spinner) findViewById(R.id.month_prompt);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.month_array,
							android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			monthSpinner.setAdapter(adapter);

			monthSpinner
					.setOnItemSelectedListener(new MonthYearSelectedListener());

			edit_isbn = (EditText) findViewById(R.id.edit_isbn);
			edit_publishers = (EditText) findViewById(R.id.edit_publishers);
			edit_pages = (EditText) findViewById(R.id.edit_pages);
			edit_year = (EditText) findViewById(R.id.edit_year);
			edit_format = (EditText) findViewById(R.id.edit_format);
			edit_edition = (EditText) findViewById(R.id.edit_edition);
			edit_languages = (EditText) findViewById(R.id.edit_language);
			edit_dewey = (EditText) findViewById(R.id.edit_dewey);

			if (mID != null) {
				book = BooksManager.findBook(getContentResolver(), mID, null);

				edit_isbn.setText(book.getIsbn());
				edit_publishers.setText(book.getPublisher());

				final int pages = book.getPagesCount();
				if (pages < 1)
					edit_pages.setText(" ");
				else
					edit_pages.setText(String.valueOf(pages));

				monthSpinner.setSelection(adapter.getPosition(TextUtilities
						.getPubMonth(book.getPublicationDate())));
				edit_year.setText(TextUtilities.getPubYear(book
						.getPublicationDate()));
				edit_format.setText(book.getFormat());
				edit_edition.setText(book.getEdition());
				edit_languages.setText(TextUtilities.removeBrackets(book
						.getLanguages().toString()));
				edit_dewey.setText(book.getDeweyNumber());
			}
		} else if (type.contains("comics")) {
			setContentView(R.layout.new_blank_comic);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.comic_label_plural_small));

			monthSpinner = (Spinner) findViewById(R.id.month_prompt);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.month_array,
							android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			monthSpinner.setAdapter(adapter);

			monthSpinner
					.setOnItemSelectedListener(new MonthYearSelectedListener());

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_year = (EditText) findViewById(R.id.edit_year);
			edit_artists = (EditText) findViewById(R.id.edit_artists);
			edit_characters = (EditText) findViewById(R.id.edit_characters);

			if (mID != null) {
				comic = ComicsManager
						.findComic(getContentResolver(), mID, null);

				edit_ean.setText(comic.getEan());
				monthSpinner.setSelection(adapter.getPosition(TextUtilities
						.getPubMonth(comic.getPublicationDate())));
				edit_year.setText(TextUtilities.getPubYear(comic
						.getPublicationDate()));
				edit_artists.setText(TextUtilities.removeBrackets(comic
						.getArtists().toString()));
				edit_characters.setText(TextUtilities.removeBrackets(comic
						.getCharacters().toString()));
			}
		}

		else if (type.contains("gadgets")) {
			setContentView(R.layout.new_blank_gadget);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.gadget_label_plural_small));

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_features = (EditText) findViewById(R.id.edit_features);

			if (mID != null) {
				gadget = GadgetsManager.findGadget(getContentResolver(), mID,
						null);

				edit_ean.setText(gadget.getEan());
				edit_features.setText(TextUtilities.removeBrackets(gadget
						.getFeatures().toString()));
			}
		} else if (type.contains("movies")) {
			setContentView(R.layout.new_blank_movie);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.movie_label_plural_small));

			monthSpinner = (Spinner) findViewById(R.id.month_prompt);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.month_array,
							android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			monthSpinner.setAdapter(adapter);

			monthSpinner
					.setOnItemSelectedListener(new MonthYearSelectedListener());

			month_theatrical_Spinner = (Spinner) findViewById(R.id.month_prompt_theatrical);
			ArrayAdapter<CharSequence> adapter_theatrical = ArrayAdapter
					.createFromResource(this, R.array.month_array,
							android.R.layout.simple_spinner_item);
			adapter_theatrical
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			month_theatrical_Spinner.setAdapter(adapter_theatrical);

			month_theatrical_Spinner
					.setOnItemSelectedListener(new TheatricalMonthYearSelectedListener());

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_actors = (EditText) findViewById(R.id.edit_actors);
			edit_studio = (EditText) findViewById(R.id.edit_studio);
			edit_running_time = (EditText) findViewById(R.id.edit_running_time);
			edit_year = (EditText) findViewById(R.id.edit_year);
			edit_year_theatrical = (EditText) findViewById(R.id.edit_year_theatrical);
			edit_format = (EditText) findViewById(R.id.edit_format);
			edit_audience = (EditText) findViewById(R.id.edit_audience);
			edit_language = (EditText) findViewById(R.id.edit_language);
			edit_features = (EditText) findViewById(R.id.edit_features);

			if (mID != null) {
				movie = MoviesManager
						.findMovie(getContentResolver(), mID, null);

				edit_ean.setText(movie.getEan());
				edit_actors.setText(TextUtilities.removeBrackets(movie
						.getActors().toString()));
				edit_studio.setText(movie.getLabel());
				edit_running_time.setText(movie.getRunningTime());

				monthSpinner.setSelection(adapter.getPosition(TextUtilities
						.getPubMonth(movie.getReleaseDate())));
				edit_year.setText(TextUtilities.getPubYear(movie
						.getReleaseDate()));

				month_theatrical_Spinner.setSelection(adapter
						.getPosition(TextUtilities.getPubMonth(movie
								.getTheatricalDebut())));
				edit_year_theatrical.setText(TextUtilities.getPubYear(movie
						.getTheatricalDebut()));

				edit_format.setText(movie.getFormat());
				edit_audience.setText(movie.getAudience());
				edit_language.setText(TextUtilities.removeBrackets(movie
						.getLanguages().toString()));
				edit_features.setText(TextUtilities.removeBrackets(movie
						.getFeatures().toString()));
			}

		} else if (type.contains("music")) {
			setContentView(R.layout.new_blank_music);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.music_label_plural_small));

			monthSpinner = (Spinner) findViewById(R.id.month_prompt);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.month_array,
							android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			monthSpinner.setAdapter(adapter);

			monthSpinner
					.setOnItemSelectedListener(new MonthYearSelectedListener());

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_label = (EditText) findViewById(R.id.edit_label);
			edit_year = (EditText) findViewById(R.id.edit_year);
			edit_format = (EditText) findViewById(R.id.edit_format);

			if (mID != null) {
				music = MusicManager.findMusic(getContentResolver(), mID, null);

				edit_ean.setText(music.getEan());
				edit_label.setText(music.getLabel());

				monthSpinner.setSelection(adapter.getPosition(TextUtilities
						.getPubMonth(music.getReleaseDate())));
				edit_year.setText(TextUtilities.getPubYear(music
						.getReleaseDate()));

				edit_format.setText(music.getFormat());
			}

		} else if (type.contains("software")) {
			setContentView(R.layout.new_blank_software);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.software_label_plural_small));

			monthSpinner = (Spinner) findViewById(R.id.month_prompt);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.month_array,
							android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			monthSpinner.setAdapter(adapter);

			monthSpinner
					.setOnItemSelectedListener(new MonthYearSelectedListener());

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_year = (EditText) findViewById(R.id.edit_year);
			edit_format = (EditText) findViewById(R.id.edit_format);
			edit_platform = (EditText) findViewById(R.id.edit_platform);

			if (mID != null) {
				software = SoftwareManager.findSoftware(getContentResolver(),
						mID, null);

				edit_ean.setText(software.getEan());

				monthSpinner.setSelection(adapter.getPosition(TextUtilities
						.getPubMonth(software.getReleaseDate())));
				edit_year.setText(TextUtilities.getPubYear(software
						.getReleaseDate()));

				edit_format.setText(software.getFormat());
				edit_platform.setText(TextUtilities.removeBrackets(software
						.getPlatform().toString()));
			}

		} else if (type.contains("tools")) {
			setContentView(R.layout.new_blank_tool);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.tool_label_plural_small));

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_features = (EditText) findViewById(R.id.edit_features);

			if (mID != null) {
				tool = ToolsManager.findTool(getContentResolver(), mID, null);

				edit_ean.setText(tool.getEan());
				edit_features.setText(TextUtilities.removeBrackets(tool
						.getFeatures().toString()));
			}

		} else if (type.contains("toys")) {
			setContentView(R.layout.new_blank_toy);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.toy_label_plural_small));

			monthSpinner = (Spinner) findViewById(R.id.month_prompt);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.month_array,
							android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			monthSpinner.setAdapter(adapter);

			monthSpinner
					.setOnItemSelectedListener(new MonthYearSelectedListener());

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_year = (EditText) findViewById(R.id.edit_year);
			edit_features = (EditText) findViewById(R.id.edit_features);

			if (mID != null) {
				toy = ToysManager.findToy(getContentResolver(), mID, null);

				edit_ean.setText(toy.getEan());
				monthSpinner.setSelection(adapter.getPosition(TextUtilities
						.getPubMonth(toy.getReleaseDate())));
				edit_year
						.setText(TextUtilities.getPubYear(toy.getReleaseDate()));
				edit_features.setText(TextUtilities.removeBrackets(toy
						.getFeatures().toString()));
			}

		} else if (type.contains("videogames")) {
			setContentView(R.layout.new_blank_videogame);
			uri = ShelvesApplication.TYPES_TO_URI
					.get(getString(R.string.videogame_label_plural_small));

			monthSpinner = (Spinner) findViewById(R.id.month_prompt);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.month_array,
							android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			monthSpinner.setAdapter(adapter);

			monthSpinner
					.setOnItemSelectedListener(new MonthYearSelectedListener());

			edit_ean = (EditText) findViewById(R.id.edit_ean);
			edit_year = (EditText) findViewById(R.id.edit_year);
			edit_features = (EditText) findViewById(R.id.edit_features);
			edit_platform = (EditText) findViewById(R.id.edit_platform);
			edit_audience = (EditText) findViewById(R.id.edit_audience);
			edit_genre = (EditText) findViewById(R.id.edit_genre);

			if (mID != null) {
				videogame = VideoGamesManager.findVideoGame(
						getContentResolver(), mID, null);

				edit_ean.setText(videogame.getEan());
				monthSpinner.setSelection(adapter.getPosition(TextUtilities
						.getPubMonth(videogame.getReleaseDate())));
				edit_year.setText(TextUtilities.getPubYear(videogame
						.getReleaseDate()));
				edit_features.setText(TextUtilities.removeBrackets(videogame
						.getFeatures().toString()));
				edit_platform.setText(videogame.getPlatform());
				edit_audience.setText(videogame.getAudience());
				edit_genre.setText(videogame.getGenre());
			}
		} else {
			uri = null;
		}

		final EditText editTitle = (EditText) findViewById(R.id.edit_title);
		final EditText editAuthors = (EditText) findViewById(R.id.edit_authors);
		final EditText editDescription = (EditText) findViewById(R.id.edit_reviews);
		final EditText editNotes = (EditText) findViewById(R.id.edit_notes);
		final RatingBar ratingBar = (RatingBar) findViewById(R.id.edit_rating);
		final EditText editPrice = (EditText) findViewById(R.id.edit_price);
		final EditText editQuantity = (EditText) findViewById(R.id.edit_quantity);

		removeCoverButton = (Button) findViewById(R.id.remove_cover);
		editCover = (ImageView) findViewById(R.id.edit_cover);

		// GJT: Block enter key for new lines
		editTitle.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_ENTER:
					return true;
				default:
					return false;
				}
			}

		});

		ratingBar
				.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
					public void onRatingChanged(RatingBar ratingBar,
							float rating, boolean fromUser) {
						mRating = (int) FloatMath.ceil(rating);
					}
				});

		final Bitmap defaultCover = BitmapFactory.decodeResource(
				this.getResources(), R.drawable.unknown_cover);
		editCover.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				coverChanged = true;
				Intent i = new Intent(BaseManualAddActivity.this,
						LoadImagesActivity.class);
				Bundle b = new Bundle();
				i.putExtras(b);
				startActivityForResult(i, ADD_NEW_COVER);
			}
		});
		editCover.setMaxHeight(Preferences.getHeightForManager());
		editCover.setMaxWidth(Preferences.getWidthForManager());

		removeCoverButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				bitmap = null;
				editCover.setImageBitmap(defaultCover);
			}
		});

		if (mID != null) {
			startingCover = ImageUtilities.getCachedCover(mID,
					new FastBitmapDrawable(defaultCover)).getBitmap();

			editCover.setImageBitmap(startingCover);

			removeCoverButton.setEnabled(true);

			bitmap = startingCover;
		}

		final Bitmap data = (Bitmap) getLastNonConfigurationInstance();
		if (data != null) {
			editCover.setImageBitmap(data); // GJT: To fix orientation change
											// issues
			bitmap = data;
			removeCoverButton.setEnabled(true);
		}

		if (type.contains("apparel")) {
			if (mID != null) {
				editTitle.setText(apparel.getTitle());
				editAuthors.setText(apparel.getAuthors().toString());
				editDescription.setText(TextUtilities.removeBrackets(apparel
						.getDescriptions().toString()));
				editNotes.setText(apparel.getNotes());
				ratingBar.setRating(apparel.getRating());
				editPrice.setText(apparel.getRetailPrice());
				editQuantity.setText(apparel.getQuantity());
			}
		} else if (type.contains("boardgames")) {
			if (mID != null) {
				editTitle.setText(boardgame.getTitle());
				editAuthors.setText(boardgame.getAuthor().toString());
				editDescription.setText(boardgame.getDescriptions().toString());
				editNotes.setText(boardgame.getNotes());
				ratingBar.setRating(boardgame.getRating());
				editQuantity.setText(boardgame.getQuantity());
			}
		} else if (type.contains("books")) {
			if (mID != null) {
				editTitle.setText(book.getTitle());
				editAuthors.setText(TextUtilities.removeBrackets(book
						.getAuthors().toString()));
				editDescription.setText(TextUtilities.removeBrackets(book
						.getDescriptions().toString()));
				editNotes.setText(book.getNotes());
				ratingBar.setRating(book.getRating());
				editPrice.setText(book.getRetailPrice());
				editQuantity.setText(book.getQuantity());
			}
		} else if (type.contains("comics")) {
			if (mID != null) {
				editTitle.setText(comic.getTitle());
				editAuthors.setText(comic.getAuthors().toString());
				editDescription.setText(comic.getDescriptions().toString());
				editNotes.setText(comic.getNotes());
				ratingBar.setRating(comic.getRating());
				editPrice.setText(comic.getRetailPrice());
				editQuantity.setText(comic.getQuantity());
			}
		} else if (type.contains("gadgets")) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.list_item,
					GadgetsProvider.getAllFromColumn(BaseItem.AUTHORS));
			// editAuthors.setAdapter(adapter);

			if (mID != null) {
				editTitle.setText(gadget.getTitle());
				editAuthors.setText(gadget.getAuthors().toString());
				editDescription.setText(TextUtilities.removeBrackets(gadget
						.getDescriptions().toString()));
				editNotes.setText(gadget.getNotes());
				ratingBar.setRating(gadget.getRating());
				editPrice.setText(gadget.getRetailPrice());
				editQuantity.setText(gadget.getQuantity());
			}
		} else if (type.contains("movies")) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.list_item,
					MoviesProvider.getAllFromColumn(BaseItem.AUTHORS));
			// editAuthors.setAdapter(adapter);

			if (mID != null) {
				editTitle.setText(movie.getTitle());
				editAuthors.setText(TextUtilities.removeBrackets(movie
						.getDirectors().toString()));
				editDescription.setText(TextUtilities.removeBrackets(movie
						.getDescriptions().toString()));
				editNotes.setText(movie.getNotes());
				ratingBar.setRating(movie.getRating());
				editPrice.setText(movie.getRetailPrice());
				editQuantity.setText(movie.getQuantity());
			}
		} else if (type.contains("music")) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.list_item,
					MusicProvider.getAllFromColumn(BaseItem.AUTHORS));
			// editAuthors.setAdapter(adapter);

			if (mID != null) {
				editTitle.setText(music.getTitle());
				editAuthors.setText(TextUtilities.removeBrackets(music
						.getAuthors().toString()));
				editDescription.setText(TextUtilities.removeBrackets(music
						.getDescriptions().toString()));
				editNotes.setText(music.getNotes());
				ratingBar.setRating(music.getRating());
				editPrice.setText(music.getRetailPrice());
				editQuantity.setText(music.getQuantity());
			}
		} else if (type.contains("software")) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.list_item,
					SoftwareProvider.getAllFromColumn(BaseItem.AUTHORS));
			// editAuthors.setAdapter(adapter);

			if (mID != null) {
				editTitle.setText(software.getTitle());
				editAuthors.setText(software.getAuthors());
				editDescription.setText(TextUtilities.removeBrackets(software
						.getDescriptions().toString()));
				editNotes.setText(software.getNotes());
				ratingBar.setRating(software.getRating());
				editPrice.setText(software.getRetailPrice());
				editQuantity.setText(software.getQuantity());
			}
		} else if (type.contains("tools")) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.list_item,
					ToolsProvider.getAllFromColumn(BaseItem.AUTHORS));
			// editAuthors.setAdapter(adapter);

			if (mID != null) {
				editTitle.setText(tool.getTitle());
				editAuthors.setText(tool.getAuthors());
				editDescription.setText(TextUtilities.removeBrackets(tool
						.getDescriptions().toString()));
				editNotes.setText(tool.getNotes());
				ratingBar.setRating(tool.getRating());
				editPrice.setText(tool.getRetailPrice());
				editQuantity.setText(tool.getQuantity());
			}
		} else if (type.contains("toys")) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.list_item,
					ToysProvider.getAllFromColumn(BaseItem.AUTHORS));
			// editAuthors.setAdapter(adapter);

			if (mID != null) {
				editTitle.setText(toy.getTitle());
				editAuthors.setText(toy.getAuthors());
				editDescription.setText(TextUtilities.removeBrackets(toy
						.getDescriptions().toString()));
				editNotes.setText(toy.getNotes());
				ratingBar.setRating(toy.getRating());
				editPrice.setText(toy.getRetailPrice());
				editQuantity.setText(toy.getQuantity());
			}
		} else if (type.contains("videogames")) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.list_item,
					VideoGamesProvider.getAllFromColumn(BaseItem.AUTHORS));
			// editAuthors.setAdapter(adapter);

			if (mID != null) {
				editTitle.setText(videogame.getTitle());
				editAuthors.setText(videogame.getAuthors());
				editDescription.setText(TextUtilities.removeBrackets(videogame
						.getDescriptions().toString()));
				editNotes.setText(videogame.getNotes());
				ratingBar.setRating(videogame.getRating());
				editPrice.setText(videogame.getRetailPrice());
				editQuantity.setText(videogame.getQuantity());
			}
		}

		final Button mSaveButton = (Button) findViewById(R.id.save_button);
		mSaveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!TextUtilities.isEmpty(editTitle.getText().toString()
						.trim())) {
					if (bitmap == null && mID != null)
						ImageUtilities.deleteCachedCover(mID);

					ContentValues textValues = new ContentValues();

					if (type.contains("books")) {
						String isbn = edit_isbn.getText().toString();
						textValues.put(BaseItem.ISBN, (isbn));

						if (mID == null) {
							if (!TextUtilities.isEmpty(isbn)) {
								textValues.put(BaseItem.INTERNAL_ID,
										TextUtilities.protectString(isbn)
												+ manualSuffix);
							} else {
								final long rightNow = System
										.currentTimeMillis();
								textValues.put(
										BaseItem.INTERNAL_ID,
										TextUtilities.protectString(String
												.valueOf(rightNow))
												+ manualSuffix);
							}
						}
					} else {
						String ean = edit_ean.getText().toString();
						textValues.put(BaseItem.EAN, (ean));

						if (mID == null) {
							if (!TextUtilities.isEmpty(ean)) {
								textValues.put(BaseItem.INTERNAL_ID,
										TextUtilities.protectString(ean)
												+ manualSuffix);
							} else {
								final long rightNow = System
										.currentTimeMillis();
								textValues.put(
										BaseItem.INTERNAL_ID,
										TextUtilities.protectString(String
												.valueOf(rightNow))
												+ manualSuffix);
							}
						}

					}
					if (type.contains("apparel")) {
						String department = edit_department.getText()
								.toString();
						String fabric = edit_fabric.getText().toString();
						String features = edit_features.getText().toString();

						textValues.put(BaseItem.DEPARTMENT, (department));
						textValues.put(BaseItem.FABRIC, (fabric));
						textValues.put(BaseItem.FEATURES, (features));
					} else if (type.contains("boardgames")) {

						String year = edit_year.getText().toString();
						String minPlayers = edit_minPlayers.getText()
								.toString();
						String maxPlayers = edit_maxPlayers.getText()
								.toString();
						String age = edit_age.getText().toString();
						String playingTime = edit_playingTime.getText()
								.toString();

						textValues.put(BaseItem.PUBLICATION, year);
						textValues.put(BaseItem.MIN_PLAYERS, minPlayers);
						textValues.put(BaseItem.MAX_PLAYERS, maxPlayers);
						textValues.put(BaseItem.AGE, age);
						textValues.put(BaseItem.PLAYING_TIME, playingTime);
					} else if (type.contains("books")) {
						String publishers = edit_publishers.getText()
								.toString();
						String pages = edit_pages.getText().toString();
						String year = edit_year.getText().toString();
						String format = edit_format.getText().toString();
						String edition = edit_edition.getText().toString();
						String languages = edit_languages.getText().toString();
						String dewey = edit_dewey.getText().toString();

						textValues.put(BaseItem.PUBLISHER, (publishers));
						textValues.put(BaseItem.PAGES, (pages));
						textValues.put(BaseItem.PUBLICATION,
								(month + " " + year));
						textValues.put(BaseItem.FORMAT, (format));
						textValues.put(BaseItem.EDITION, (edition));
						textValues.put(BaseItem.LANGUAGE, (languages));
						textValues.put(BaseItem.DEWEY_NUMBER, (dewey));
					} else if (type.contains("comics")) {
						String year = edit_year.getText().toString();
						String artists = edit_artists.getText().toString();
						String characters = edit_characters.getText()
								.toString();

						textValues.put(BaseItem.PUBLICATION,
								(month + " " + year));
						textValues.put(BaseItem.ARTISTS, (artists));
						textValues.put(BaseItem.CHARACTERS, (characters));
					} else if (type.contains("gadgets")) {
						String features = edit_features.getText().toString();

						textValues.put(BaseItem.FEATURES, (features));
					} else if (type.contains("movies")) {
						String actors = edit_actors.getText().toString();
						String studio = edit_studio.getText().toString();
						String running_time = edit_running_time.getText()
								.toString();
						String year = edit_year.getText().toString();
						String year_theatrical = edit_year_theatrical.getText()
								.toString();
						String format = edit_format.getText().toString();
						String audience = edit_audience.getText().toString();
						String language = edit_language.getText().toString();
						String features = edit_features.getText().toString();

						textValues.put(BaseItem.ACTORS, (actors));
						textValues.put(BaseItem.LABEL, (studio));
						textValues.put(BaseItem.RUNNING_TIME, (running_time));
						textValues.put(BaseItem.RELEASE_DATE,
								(month + " " + year));
						textValues.put(BaseItem.THEATRICAL_DEBUT,
								(month_theatrical + " " + year_theatrical));
						textValues.put(BaseItem.FORMAT, (format));
						textValues.put(BaseItem.AUDIENCE, (audience));
						textValues.put(BaseItem.LANGUAGES, (language));
						textValues.put(BaseItem.FEATURES, (features));
					} else if (type.contains("music")) {
						String label = edit_label.getText().toString();
						String year = edit_year.getText().toString();
						String format = edit_format.getText().toString();

						textValues.put(BaseItem.LABEL, (label));
						textValues.put(BaseItem.RELEASE_DATE,
								(month + " " + year));
						textValues.put(BaseItem.FORMAT, (format));
					} else if (type.contains("software")) {
						String year = edit_year.getText().toString();
						String format = edit_format.getText().toString();
						String platform = edit_platform.getText().toString();

						textValues.put(BaseItem.RELEASE_DATE,
								(month + " " + year));
						textValues.put(BaseItem.FORMAT, (format));
						textValues.put(BaseItem.PLATFORM, (platform));
					} else if (type.contains("tools")) {
						String features = edit_features.getText().toString();

						textValues.put(BaseItem.FEATURES, (features));
					} else if (type.contains("toys")) {
						String year = edit_year.getText().toString();
						String features = edit_features.getText().toString();

						textValues.put(BaseItem.RELEASE_DATE,
								(month + " " + year));
						textValues.put(BaseItem.FEATURES, (features));
					} else if (type.contains("videogames")) {
						String year = edit_year.getText().toString();
						String features = edit_features.getText().toString();
						String platform = edit_platform.getText().toString();
						String audience = edit_audience.getText().toString();
						String genre = edit_genre.getText().toString();

						textValues.put(BaseItem.RELEASE_DATE,
								(month + " " + year));
						textValues.put(BaseItem.FEATURES, (features));
						textValues.put(BaseItem.PLATFORM, (platform));
						textValues.put(BaseItem.ESRB, (audience));
						textValues.put(BaseItem.GENRE, (genre));
					}

					String authors = editAuthors.getText().toString();
					textValues.put(BaseItem.AUTHORS, (authors));

					final Editable desc = editDescription.getText();
					if (desc != null) {
						textValues.put(BaseItem.REVIEWS, (desc.toString()));
					}
					final Editable notes = editNotes.getText();
					if (notes != null) {
						textValues.put(BaseItem.NOTES, (notes.toString()));
					}
					final Editable quantity = editQuantity.getText();
					if (quantity != null) {
						textValues.put(BaseItem.QUANTITY, (quantity.toString()));
					}
					textValues.put(BaseItem.RATING, mRating);
					final String title = editTitle.getText().toString();
					textValues.put(BaseItem.TITLE, (title));
					String name = editTitle.getText().toString().trim()
							.toLowerCase();

					if (editPrice != null) {
						final String price = editPrice.getText().toString();
						textValues.put(BaseItem.RETAIL_PRICE, (price));
					}

					if (mKeyPrefixes == null) {
						final Resources resources = getBaseContext()
								.getResources();
						final String[] keyPrefixes = resources
								.getStringArray(R.array.prefixes);
						final int count = keyPrefixes.length;

						mKeyPrefixes = new Pattern[count];
						for (int i = 0; i < count; i++) {
							mKeyPrefixes[i] = Pattern.compile("^"
									+ keyPrefixes[i] + "\\s+");
						}
					}

					if (mKeySuffixes == null) {
						final Resources resources = getBaseContext()
								.getResources();
						final String[] keySuffixes = resources
								.getStringArray(R.array.suffixes);
						final int count = keySuffixes.length;

						mKeySuffixes = new Pattern[count];
						for (int i = 0; i < count; i++) {
							mKeySuffixes[i] = Pattern.compile("\\s*"
									+ keySuffixes[i] + "$");
						}
					}

					textValues.put(BaseItem.SORT_TITLE, TextUtilities.keyFor(
							mKeyPrefixes, mKeySuffixes, name));

					final String id;
					if (mID != null) {
						getContentResolver().update(uri, textValues,
								BaseItem.INTERNAL_ID + "=?",
								new String[] { mID });
						id = mID;
					} else {
						getContentResolver().insert(uri, textValues);
						id = textValues.getAsString(BaseItem.INTERNAL_ID);
					}

					if (bitmap != null && coverChanged) {
						ImageUtilities.deleteCachedCover(id);
						bitmap = ImageUtilities.createCover(bitmap,
								Preferences.getWidthForManager(),
								Preferences.getHeightForManager());
						ImportUtilities.addCoverToCache(id, bitmap);
					}

					if (mID == null)

						UIUtilities.showFormattedImageToast(
								BaseManualAddActivity.this,
								R.string.success_added,
								ImageUtilities
										.getCachedCover(
												id,
												new FastBitmapDrawable(
														BitmapFactory
																.decodeResource(
																		getResources(),
																		R.drawable.unknown_cover))),
								title);

					setResult(RESULT_OK);
					finish();
				} else {
					UIUtilities.showToast(getBaseContext(),
							R.string.edit_needs_title);
				}
			}
		});

		final Button mCancelButton = (Button) findViewById(R.id.cancel_button);
		mCancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		final Bitmap data = bitmap;
		return data;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			switch (requestCode) {
			case ADD_NEW_COVER:
				Bundle extras = data.getExtras();
				bitmap = (Bitmap) extras.get("newCover");
				if (bitmap != null) {
					editCover.setImageBitmap(bitmap);
					removeCoverButton.setEnabled(true);
				}
			}
		}
	}

	public class MonthYearSelectedListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			final String m = parent.getItemAtPosition(pos).toString();
			if (!TextUtilities.isEmpty(m))
				month = m;
		}

		public void onNothingSelected(AdapterView parent) {
			// Do nothing.
		}
	}

	public class TheatricalMonthYearSelectedListener implements
			OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			final String m = parent.getItemAtPosition(pos).toString();
			if (!TextUtilities.isEmpty(m))
				month_theatrical = m;
		}

		public void onNothingSelected(AdapterView parent) {
			// Do nothing.
		}
	}
}