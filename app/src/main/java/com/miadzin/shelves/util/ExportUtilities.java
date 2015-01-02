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

package com.miadzin.shelves.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.base.BaseItem;
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

public final class ExportUtilities extends IOUtilities {
	private static final String LOG_TAG = "ExportUtilities";

	public static String[] header;

	private static final String EXPORT_FILE_DL_APPAREL = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_DL_APPAREL);
	public static final String EXPORT_FILE_SHELVES_APPAREL = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_APPAREL);

	public static final String EXPORT_FILE_SHELVES_BOARDGAMES = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_BOARDGAMES);

	private static final String EXPORT_FILE_DL_BOOKS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_DL_BOOKS);
	private static final String EXPORT_FILE_SHELFARI_BOOKS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELFARI_BOOKS);
	private static final String EXPORT_FILE_GOOGLE_LIBRARY_BOOKS = "Shelves_to_Google_Books.txt";
	private static final String EXPORT_FILE_LIBRARY_THING_BOOKS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_LIBRARY_THING_BOOKS);
	private static final String EXPORT_FILE_MEDIAMAN_BOOKS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_MEDIAMAN_BOOKS);
	public static final String EXPORT_FILE_SHELVES_BOOKS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_BOOKS);

	public static final String EXPORT_FILE_SHELVES_COMICS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_COMICS);

	private static final String EXPORT_FILE_DL_GADGETS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_DL_GADGETS);
	public static final String EXPORT_FILE_SHELVES_GADGETS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_GADGETS);

	private static final String EXPORT_FILE_DL_MOVIES = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_DL_MOVIES);
	public static final String EXPORT_FILE_SHELVES_MOVIES = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_MOVIES);
	private static final String EXPORT_FILE_MEDIAMAN_MOVIES = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_MEDIAMAN_MOVIES);

	private static final String EXPORT_FILE_DL_MUSIC = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_DL_MUSIC);
	public static final String EXPORT_FILE_SHELVES_MUSIC = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_MUSIC);
	private static final String EXPORT_FILE_MEDIAMAN_MUSIC = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_MEDIAMAN_MUSIC);

	private static final String EXPORT_FILE_DL_SOFTWARE = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_DL_SOFTWARE);
	public static final String EXPORT_FILE_SHELVES_SOFTWARE = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_SOFTWARE);

	private static final String EXPORT_FILE_DL_TOOLS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_DL_TOOLS);
	public static final String EXPORT_FILE_SHELVES_TOOLS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_TOOLS);

	private static final String EXPORT_FILE_DL_TOYS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_DL_TOYS);
	public static final String EXPORT_FILE_SHELVES_TOYS = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_TOYS);

	private static final String EXPORT_FILE_DL_VIDEOGAMES = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_DL_VIDEOGAMES);
	public static final String EXPORT_FILE_SHELVES_VIDEOGAMES = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_SHELVES_VIDEOGAMES);
	private static final String EXPORT_FILE_MEDIAMAN_VIDEOGAMES = ShelvesApplication
			.getContext().getString(R.string.EXPORT_FILE_MEDIAMAN_VIDEOGAMES);

	private ExportUtilities() {
	}

	public static String determineShelvesFileName(String nameToMatch) {
		if (ShelvesApplication.getContext()
				.getString(R.string.apparel_label_big).contains(nameToMatch))
			return EXPORT_FILE_SHELVES_APPAREL;
		else if (ShelvesApplication.getContext()
				.getString(R.string.boardgame_label_plural_big)
				.contains(nameToMatch))
			return EXPORT_FILE_SHELVES_BOARDGAMES;
		else if (ShelvesApplication.getContext()
				.getString(R.string.book_label_plural_big)
				.contains(nameToMatch))
			return EXPORT_FILE_SHELVES_BOOKS;
		else if (ShelvesApplication.getContext()
				.getString(R.string.comic_label_plural_big)
				.contains(nameToMatch))
			return EXPORT_FILE_SHELVES_COMICS;
		else if (ShelvesApplication.getContext()
				.getString(R.string.gadget_label_plural_big)
				.contains(nameToMatch))
			return EXPORT_FILE_SHELVES_GADGETS;
		else if (ShelvesApplication.getContext()
				.getString(R.string.movie_label_plural_big)
				.contains(nameToMatch))
			return EXPORT_FILE_SHELVES_MOVIES;
		else if (ShelvesApplication.getContext()
				.getString(R.string.music_label_big).contains(nameToMatch))
			return EXPORT_FILE_SHELVES_MUSIC;
		else if (ShelvesApplication.getContext()
				.getString(R.string.software_label_big).contains(nameToMatch))
			return EXPORT_FILE_SHELVES_SOFTWARE;
		else if (ShelvesApplication.getContext()
				.getString(R.string.tool_label_plural_big)
				.contains(nameToMatch))
			return EXPORT_FILE_SHELVES_TOOLS;
		else if (ShelvesApplication.getContext()
				.getString(R.string.toy_label_plural_big).contains(nameToMatch))
			return EXPORT_FILE_SHELVES_TOYS;
		else if (ShelvesApplication.getContext()
				.getString(R.string.videogame_label_plural_big)
				.contains(nameToMatch))
			return EXPORT_FILE_SHELVES_VIDEOGAMES;
		else
			return "";
	}

	public static boolean exportItems(outputTypes type,
			ContentResolver contentResolver) throws IOException {
		boolean success = false;
		File outportFile = null;

		switch (type) {
		case DLApparel:
			outportFile = IOUtilities.getExternalFile(EXPORT_FILE_DL_APPAREL);
			success = exportingApparelToDL(outportFile, contentResolver);
			break;
		case shelvesApparel:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_APPAREL);
			success = exportingToShelves(outportFile, contentResolver,
					ApparelStore.Apparel.CONTENT_URI);
			break;
		case shelvesBoardGames:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_BOARDGAMES);
			success = exportingToShelves(outportFile, contentResolver,
					BoardGamesStore.BoardGame.CONTENT_URI);
			break;
		case DLBooks:
			outportFile = IOUtilities.getExternalFile(EXPORT_FILE_DL_BOOKS);
			success = exportingBooksToDL(outportFile, contentResolver);
			break;
		case libraryThingBooks:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_LIBRARY_THING_BOOKS);
			success = exportingBooksToLibraryThing(outportFile, contentResolver);
			break;
		case mediaManBooks:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_MEDIAMAN_BOOKS);
			success = exportingBooksToMediaMan(outportFile, contentResolver);
			break;
		case shelfariBooks:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELFARI_BOOKS);
			success = exportingBooksToShelfariBooks(outportFile,
					contentResolver);
			break;
		case googleLibraryBooks:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_GOOGLE_LIBRARY_BOOKS);
			success = exportingBooksToGoogleBooks(outportFile, contentResolver);
			break;
		case shelvesBooks:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_BOOKS);
			success = exportingToShelves(outportFile, contentResolver,
					BooksStore.Book.CONTENT_URI);
			break;
		case shelvesComics:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_COMICS);
			success = exportingToShelves(outportFile, contentResolver,
					ComicsStore.Comic.CONTENT_URI);
			break;
		case DLGadgets:
			outportFile = IOUtilities.getExternalFile(EXPORT_FILE_DL_GADGETS);
			success = exportingGadgetsToDL(outportFile, contentResolver);
			break;
		case shelvesGadgets:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_GADGETS);
			success = exportingToShelves(outportFile, contentResolver,
					GadgetsStore.Gadget.CONTENT_URI);
			break;
		case DLMovies:
			outportFile = IOUtilities.getExternalFile(EXPORT_FILE_DL_MOVIES);
			success = exportingMoviesToDL(outportFile, contentResolver);
			break;
		case mediaManMovies:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_MEDIAMAN_MOVIES);
			success = exportingMoviesToMediaMan(outportFile, contentResolver);
			break;
		case shelvesMovies:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_MOVIES);
			success = exportingToShelves(outportFile, contentResolver,
					MoviesStore.Movie.CONTENT_URI);
			break;
		case DLMusic:
			outportFile = IOUtilities.getExternalFile(EXPORT_FILE_DL_MUSIC);
			success = exportingMusicToDL(outportFile, contentResolver);
			break;
		case mediaManMusic:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_MEDIAMAN_MUSIC);
			success = exportingMusicToMediaMan(outportFile, contentResolver);
			break;
		case shelvesMusic:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_MUSIC);
			success = exportingToShelves(outportFile, contentResolver,
					MusicStore.Music.CONTENT_URI);
			break;
		case DLSoftware:
			outportFile = IOUtilities.getExternalFile(EXPORT_FILE_DL_SOFTWARE);
			success = exportingSoftwareToDL(outportFile, contentResolver);
			break;
		case shelvesSoftware:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_SOFTWARE);
			success = exportingToShelves(outportFile, contentResolver,
					SoftwareStore.Software.CONTENT_URI);
			break;
		case DLTools:
			outportFile = IOUtilities.getExternalFile(EXPORT_FILE_DL_TOOLS);
			success = exportingToolsToDL(outportFile, contentResolver);
			break;
		case shelvesTools:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_TOOLS);
			success = exportingToShelves(outportFile, contentResolver,
					ToolsStore.Tool.CONTENT_URI);
			break;
		case DLToys:
			outportFile = IOUtilities.getExternalFile(EXPORT_FILE_DL_TOYS);
			success = exportingToysToDL(outportFile, contentResolver);
			break;
		case shelvesToys:
			outportFile = IOUtilities.getExternalFile(EXPORT_FILE_SHELVES_TOYS);
			success = exportingToShelves(outportFile, contentResolver,
					ToysStore.Toy.CONTENT_URI);
			break;
		case DLVideoGames:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_DL_VIDEOGAMES);
			success = exportingVideoGamesToDL(outportFile, contentResolver);
			break;
		case mediaManVideoGames:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_MEDIAMAN_VIDEOGAMES);
			success = exportingVideoGamesToMediaMan(outportFile,
					contentResolver);
			break;
		case shelvesVideoGames:
			outportFile = IOUtilities
					.getExternalFile(EXPORT_FILE_SHELVES_VIDEOGAMES);
			success = exportingToShelves(outportFile, contentResolver,
					VideoGamesStore.VideoGame.CONTENT_URI);
			break;
		default:
			break;
		}
		return success;
	}

	private static boolean exportingApparelToDL(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "server link", BaseItem.TITLE,
				BaseItem.AUTHORS, BaseItem.FEATURES, BaseItem.FABRIC,
				BaseItem.DEPARTMENT, BaseItem.RATING };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);
			c = contentResolver.query(ApparelStore.Apparel.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID, BaseItem.TITLE,
							BaseItem.AUTHORS, BaseItem.FEATURES,
							BaseItem.FABRIC, BaseItem.DEPARTMENT,
							BaseItem.RATING }, null, null, null);
			if (c.moveToFirst()) {
				do {
					row[0] = c.getString(0).replace(ServerInfo.NAME, "");
					row[1] = c.getString(1);
					row[2] = c.getString(2);
					row[3] = c.getString(3);
					row[4] = c.getString(4);
					row[5] = c.getString(5);
					row[6] = c.getString(6);

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingBooksToMediaMan(File exportFile,
			ContentResolver contentResolver) throws IOException {

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			c = contentResolver.query(BooksStore.Book.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID }, null, null, null);
			if (c.moveToFirst()) {
				do {
					writer.writeNext(new String[] { c.getString(0).replace(
							ServerInfo.NAME, "") });
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingBooksToLibraryThing(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "'TITLE'", "'AUTHOR (last, first)'",
				"'DATE'", "'ISBN'", "'PUBLICATION INFO'", "'TAGS'", "'RATING'",
				"'REVIEW'", "'ENTRY DATE'" };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), ',', '"',
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);

			c = contentResolver
					.query(BooksStore.Book.CONTENT_URI, new String[] {
							BaseItem.INTERNAL_ID, BaseItem.ISBN, BaseItem.TAGS,
							BaseItem.RATING, }, null, null, null);
			if (c.moveToFirst()) {
				do {
					row[0] = " ";
					row[1] = " ";
					row[2] = " ";
					row[3] = c.getString(1);
					row[4] = " ";
					row[5] = c.getString(2);
					row[6] = c.getString(3);
					row[7] = " ";
					row[8] = " ";

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingBooksToDL(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "server link", BaseItem.ISBN,
				BaseItem.TITLE, BaseItem.AUTHORS, BaseItem.PUBLISHER,
				BaseItem.PAGES, BaseItem.RATING };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);

			c = contentResolver
					.query(BooksStore.Book.CONTENT_URI,
							new String[] { BaseItem.INTERNAL_ID, BaseItem.ISBN,
									BaseItem.TITLE, BaseItem.AUTHORS,
									BaseItem.PUBLISHER, BaseItem.PAGES,
									BaseItem.RATING }, null, null, null);
			if (c.moveToFirst()) {
				do {
					row[0] = c.getString(0).replace(ServerInfo.NAME, "");
					row[1] = c.getString(1);
					row[2] = c.getString(2);
					row[3] = c.getString(3);
					row[4] = c.getString(4);
					row[5] = c.getString(5);
					row[6] = c.getString(6);

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	// GJT: Google books import just takes a list of ISBNs
	private static boolean exportingBooksToGoogleBooks(File exportFile,
			ContentResolver contentResolver) {
		String[] row = new String[] { BaseItem.EAN };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;
		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);
			try {
				c = contentResolver.query(BooksStore.Book.CONTENT_URI,
						new String[] { BaseItem.EAN }, null, null, null);
				if (c.moveToFirst()) {
					do {
						row[0] = c.getString(0);

						writer.writeNext(row);
						success = true;
					} while (c.moveToNext());
				}
			} finally {
				if (c != null)
					c.close();
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, "exportingBooksToGoogleBooks", ex);
		} finally {
			try {
				if (null != writer)
					writer.close();
			} catch (IOException ex) {
			}
		}

		return success;
	}

	// GJT: According to the Shelfari website:
	// Shelfari looks for ISBN numbers, titles, and author names when importing
	// a file.
	private static boolean exportingBooksToShelfariBooks(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { BaseItem.EAN, BaseItem.TITLE,
				BaseItem.AUTHORS };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;
		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);
			try {
				c = contentResolver.query(BooksStore.Book.CONTENT_URI,
						new String[] { BaseItem.EAN, BaseItem.TITLE,
								BaseItem.AUTHORS }, null, null, null);
				if (c.moveToFirst()) {
					do {
						row[0] = c.getString(0);
						row[1] = c.getString(1);
						row[2] = c.getString(2);

						writer.writeNext(row);
						success = true;
					} while (c.moveToNext());
				}
			} finally {
				if (c != null)
					c.close();
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, "exportingBooksToShelfari", ex);
		} finally {
			try {
				if (null != writer)
					writer.close();
			} catch (IOException ex) {
			}
		}

		return success;
	}

	private static boolean exportingGadgetsToDL(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "server link", BaseItem.TITLE,
				BaseItem.AUTHORS, BaseItem.FEATURES, BaseItem.RATING };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);

			c = contentResolver.query(GadgetsStore.Gadget.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID, BaseItem.TITLE,
							BaseItem.AUTHORS, BaseItem.FEATURES,
							BaseItem.RATING }, null, null, null);
			if (c.moveToFirst()) {
				do {
					row[0] = c.getString(0).replace(ServerInfo.NAME, "");
					row[1] = c.getString(1);
					row[2] = c.getString(2);
					row[3] = c.getString(3);
					row[4] = c.getString(4);

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingMoviesToDL(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "server link", BaseItem.TITLE,
				BaseItem.DIRECTORS, BaseItem.ACTORS, BaseItem.LABEL,
				"features", BaseItem.RUNNING_TIME, BaseItem.LANGUAGES,
				BaseItem.RATING };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);

			c = contentResolver.query(MoviesStore.Movie.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID, BaseItem.TITLE,
							BaseItem.DIRECTORS, BaseItem.ACTORS,
							BaseItem.LABEL, BaseItem.FEATURES,
							BaseItem.RUNNING_TIME, BaseItem.LANGUAGES,
							BaseItem.RATING }, null, null, null);
			if (c.moveToFirst()) {
				do {
					row[0] = c.getString(0).replace(ServerInfo.NAME, "");
					row[1] = c.getString(1);
					row[2] = c.getString(2);
					row[3] = c.getString(3);
					row[4] = c.getString(4);
					row[5] = c.getString(5);
					row[6] = c.getString(6);
					row[7] = c.getString(7);
					row[8] = c.getString(8);

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingMoviesToMediaMan(File exportFile,
			ContentResolver contentResolver) throws IOException {

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			c = contentResolver.query(MoviesStore.Movie.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID }, null, null, null);
			if (c.moveToFirst()) {
				do {
					writer.writeNext(new String[] { c.getString(0).replace(
							ServerInfo.NAME, "") });
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingMusicToDL(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "server link", BaseItem.TITLE,
				BaseItem.AUTHORS, BaseItem.LABEL, BaseItem.FORMAT,
				BaseItem.RATING };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);

			c = contentResolver.query(MusicStore.Music.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID, BaseItem.TITLE,
							BaseItem.AUTHORS, BaseItem.LABEL, BaseItem.FORMAT,
							BaseItem.RATING }, null, null, null);
			if (c.moveToFirst()) {
				do {
					row[0] = c.getString(0).replace(ServerInfo.NAME, "");
					row[1] = c.getString(1);
					row[2] = c.getString(2);
					row[3] = c.getString(3);
					row[4] = c.getString(4);
					row[5] = c.getString(5);

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingMusicToMediaMan(File exportFile,
			ContentResolver contentResolver) throws IOException {

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			c = contentResolver.query(MusicStore.Music.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID }, null, null, null);
			if (c.moveToFirst()) {
				do {
					writer.writeNext(new String[] { c.getString(0).replace(
							ServerInfo.NAME, "") });
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingSoftwareToDL(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "server link", BaseItem.TITLE,
				BaseItem.AUTHORS, BaseItem.FORMAT, BaseItem.PLATFORM,
				BaseItem.RATING };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);

			c = contentResolver.query(SoftwareStore.Software.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID, BaseItem.TITLE,
							BaseItem.AUTHORS, BaseItem.FORMAT,
							BaseItem.PLATFORM, BaseItem.RATING }, null, null,
					null);
			if (c.moveToFirst()) {
				do {
					row[0] = c.getString(0).replace(ServerInfo.NAME, "");
					row[1] = c.getString(1);
					row[2] = c.getString(2);
					row[3] = c.getString(3);
					row[4] = c.getString(4);
					row[5] = c.getString(5);

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingToolsToDL(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "server link", BaseItem.TITLE,
				BaseItem.AUTHORS, "features", BaseItem.RATING };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);
			c = contentResolver.query(ToolsStore.Tool.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID, BaseItem.TITLE,
							BaseItem.AUTHORS, BaseItem.FEATURES,
							BaseItem.RATING }, null, null, null);
			if (c.moveToFirst()) {
				do {
					row[0] = c.getString(0).replace(ServerInfo.NAME, "");
					row[1] = c.getString(1);
					row[2] = c.getString(2);
					row[3] = c.getString(3);
					row[4] = c.getString(4);

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingToysToDL(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "server link", BaseItem.TITLE,
				BaseItem.AUTHORS, BaseItem.FEATURES, BaseItem.RATING };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);
			c = contentResolver.query(ToysStore.Toy.CONTENT_URI, new String[] {
					BaseItem.INTERNAL_ID, BaseItem.TITLE, BaseItem.AUTHORS,
					BaseItem.FEATURES, BaseItem.RATING }, null, null, null);
			if (c.moveToFirst()) {
				do {
					row[0] = c.getString(0).replace(ServerInfo.NAME, "");
					row[1] = c.getString(1);
					row[2] = c.getString(2);
					row[3] = c.getString(3);
					row[4] = c.getString(4);

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingVideoGamesToDL(File exportFile,
			ContentResolver contentResolver) throws IOException {
		String[] row = new String[] { "server link", BaseItem.TITLE,
				BaseItem.AUTHORS, BaseItem.ESRB, "features", BaseItem.PLATFORM,
				BaseItem.GENRE, BaseItem.RATING };

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			// Write header row
			writer.writeNext(row);
			c = contentResolver
					.query(VideoGamesStore.VideoGame.CONTENT_URI,
							new String[] { BaseItem.INTERNAL_ID,
									BaseItem.TITLE, BaseItem.AUTHORS,
									BaseItem.ESRB, BaseItem.FEATURES,
									BaseItem.PLATFORM, BaseItem.GENRE,
									BaseItem.RATING }, null, null, null);
			if (c.moveToFirst()) {
				do {
					row[0] = c.getString(0).replace(ServerInfo.NAME, "");
					row[1] = c.getString(1);
					row[2] = c.getString(2);
					row[3] = c.getString(3);
					row[4] = c.getString(4);
					row[5] = c.getString(5);
					row[6] = c.getString(6);
					row[7] = c.getString(7);

					writer.writeNext(row);
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	private static boolean exportingVideoGamesToMediaMan(File exportFile,
			ContentResolver contentResolver) throws IOException {

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			c = contentResolver.query(VideoGamesStore.VideoGame.CONTENT_URI,
					new String[] { BaseItem.INTERNAL_ID }, null, null, null);
			if (c.moveToFirst()) {
				do {
					writer.writeNext(new String[] { c.getString(0).replace(
							ServerInfo.NAME, "") });
					success = true;
				} while (c.moveToNext());
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString());
		} finally {
			if (null != writer)
				writer.close();
			if (c != null)
				c.close();
		}

		return success;
	}

	public static boolean exportingToShelves(File exportFile,
			ContentResolver contentResolver, Uri uri) throws IOException {

		TSVWriter writer = null;
		boolean success = false;
		Cursor c = null;

		try {
			writer = new TSVWriter(new OutputStreamWriter(new FileOutputStream(
					exportFile), "UTF-8"), TSVWriter.DEFAULT_SEPARATOR,
					TSVWriter.NO_QUOTE_CHARACTER,
					TSVWriter.NO_ESCAPE_CHARACTER, TSVWriter.DEFAULT_LINE_END);

			try {
				c = contentResolver.query(uri, null, null, null, null);
				if (c.moveToFirst()) {
					String[] row = c.getColumnNames();
					// Write header row
					writer.writeNext(row);
					do {
						for (int i = 0; i < c.getColumnCount(); i++) {
							row[i] = c.getString(i);
						}
						writer.writeNext(row);
						success = true;
					} while (c.moveToNext());
				}
			} finally {
				if (c != null)
					c.close();
			}
		} catch (Exception ex) {
			Log.e(LOG_TAG, "Export using " + exportFile, ex);
		} finally {
			try {
				if (null != writer)
					writer.close();
			} catch (IOException ex) {
			}
		}

		return success;
	}
}
