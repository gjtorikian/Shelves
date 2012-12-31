/*
 * Copyright (C) 2008 Romain Guy
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.graphics.Bitmap;
import android.util.Log;

import com.miadzin.shelves.R;
import com.miadzin.shelves.ShelvesApplication;
import com.miadzin.shelves.base.BaseItem;
import com.miadzin.shelves.provider.ItemImport;

public final class ImportUtilities extends IOUtilities {

	private static final String IMPORT_FILE_DL_APPAREL = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_DL_APPAREL);
	private static final String IMPORT_FILE_SHELVES_APPAREL = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_APPAREL);
	private static final String IMPORT_FILE_LIST_OF_APPAREL = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIST_OF_APPAREL);

	private static final String IMPORT_FILE_BOARDGAMEGEEK_BOARDGAMES = ShelvesApplication
			.getContext().getString(
					R.string.IMPORT_FILE_BOARDGAMEGEEK_BOARDGAMES);
	private static final String IMPORT_FILE_SHELVES_BOARDGAMES = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_BOARDGAMES);

	private static final String IMPORT_FILE_DL_BOOKS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_DL_BOOKS);
	private static final String IMPORT_FILE_SHELFARI_BOOKS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELFARI_BOOKS);
	private static final String[] IMPORT_FILE_GOOGLE_LIBRARY_BOOKS = {
			"Reviewed.xml", "Favorites.xml", "Reading_now.xml", "To_read.xml",
			"Have_read.xml" };
	private static final String IMPORT_FILE_LIBRARY_THING_BOOKS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIBRARY_THING_BOOKS);
	private static final String IMPORT_FILE_MEDIAMAN_BOOKS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_MEDIAMAN_BOOKS);
	private static final String IMPORT_FILE_SHELVES_BOOKS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_BOOKS);
	private static final String IMPORT_FILE_LIST_OF_BOOKS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIST_OF_BOOKS);

	private static final String IMPORT_FILE_SHELVES_COMICS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_COMICS);

	private static final String IMPORT_FILE_DL_GADGETS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_DL_GADGETS);
	private static final String IMPORT_FILE_SHELVES_GADGETS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_GADGETS);
	private static final String IMPORT_FILE_LIST_OF_GADGETS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIST_OF_GADGETS);

	private static final String IMPORT_FILE_DL_MOVIES = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_DL_MOVIES);
	private static final String IMPORT_FILE_MEDIAMAN_MOVIES = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_MEDIAMAN_MOVIES);
	private static final String IMPORT_FILE_SHELVES_MOVIES = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_MOVIES);
	private static final String IMPORT_FILE_LIST_OF_MOVIES = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIST_OF_MOVIES);

	private static final String IMPORT_FILE_DL_MUSIC = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_DL_MUSIC);
	private static final String IMPORT_FILE_MEDIAMAN_MUSIC = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_MEDIAMAN_MUSIC);
	private static final String IMPORT_FILE_SHELVES_MUSIC = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_MUSIC);
	private static final String IMPORT_FILE_LIST_OF_MUSIC = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIST_OF_MUSIC);

	private static final String IMPORT_FILE_DL_SOFTWARE = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_DL_SOFTWARE);
	private static final String IMPORT_FILE_SHELVES_SOFTWARE = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_SOFTWARE);
	private static final String IMPORT_FILE_LIST_OF_SOFTWARE = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIST_OF_SOFTWARE);

	private static final String IMPORT_FILE_DL_TOOLS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_DL_TOOLS);
	private static final String IMPORT_FILE_SHELVES_TOOLS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_TOOLS);
	private static final String IMPORT_FILE_LIST_OF_TOOLS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIST_OF_TOOLS);

	private static final String IMPORT_FILE_DL_TOYS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_DL_TOYS);
	private static final String IMPORT_FILE_SHELVES_TOYS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_TOYS);
	private static final String IMPORT_FILE_LIST_OF_TOYS = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIST_OF_TOYS);

	private static final String IMPORT_FILE_DL_VIDEOGAMES = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_DL_VIDEOGAMES);
	private static final String IMPORT_FILE_MEDIAMAN_VIDEOGAMES = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_MEDIAMAN_VIDEOGAMES);
	private static final String IMPORT_FILE_SHELVES_VIDEOGAMES = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_SHELVES_VIDEOGAMES);
	private static final String IMPORT_FILE_LIST_OF_VIDEOGAMES = ShelvesApplication
			.getContext().getString(R.string.IMPORT_FILE_LIST_OF_VIDEOGAMES);

	public static String[] header;
	public static List<String> manualItems;

	private static final String LOG_TAG = "ImportUtilities";

	private ImportUtilities() {
	}

	public static ArrayList<ItemImport> loadItems(inputTypes type)
			throws IOException {
		ArrayList<ItemImport> list = new ArrayList<ItemImport>();
		File importFile = null;

		if (type == null) {
			return null;
		}

		switch (type) {
		case bulkScanApparel:
			importFile = IOUtilities.getExternalFile(FILE_BULK_SCAN_APPAREL);
			list = importingListOf(importFile, list);
			break;
		case DLApparel:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_DL_APPAREL);
			list = importingDL(importFile, list);
			break;
		case shelvesApparel:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_SHELVES_APPAREL);
			list = importingShelves(type, importFile, list);
			break;
		case listOfApparel:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_LIST_OF_APPAREL);
			list = importingListOf(importFile, list);
			break;
		case boardGameGeekBoardGames:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_BOARDGAMEGEEK_BOARDGAMES);
			list = importingBoardGameGeek(type, importFile, list);
			break;
		case shelvesBoardGames:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_SHELVES_BOARDGAMES);
			list = importingShelves(type, importFile, list);
			break;
		case bulkScanBooks:
			importFile = IOUtilities.getExternalFile(FILE_BULK_SCAN_BOOKS);
			list = importingListOf(importFile, list);
			break;
		case DLBooks:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_DL_BOOKS);
			list = importingDL(importFile, list);
			break;
		case googleLibraryBooks:
			list = importingGoogleLibraryBooks(list);
			break;
		case libraryThingBooks:
			list = importingLibraryThingBooks(list);
			break;
		case mediaManBooks:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_MEDIAMAN_BOOKS);
			list = importingMediaMan(importFile, list);
			break;
		case shelfariBooks:
			list = importingShelfariBooks(list);
			break;
		case shelvesBooks:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_SHELVES_BOOKS);
			list = importingShelves(type, importFile, list);
			break;
		case listOfBooks:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_LIST_OF_BOOKS);
			list = importingListOf(importFile, list);
			break;
		case shelvesComics:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_SHELVES_COMICS);
			list = importingShelves(type, importFile, list);
			break;
		case bulkScanGadgets:
			importFile = IOUtilities.getExternalFile(FILE_BULK_SCAN_GADGETS);
			list = importingListOf(importFile, list);
			break;
		case DLGadgets:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_DL_GADGETS);
			list = importingDL(importFile, list);
			break;
		case shelvesGadgets:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_SHELVES_GADGETS);
			list = importingShelves(type, importFile, list);
			break;
		case listOfGadgets:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_LIST_OF_GADGETS);
			list = importingListOf(importFile, list);
			break;
		case bulkScanMovies:
			importFile = IOUtilities.getExternalFile(FILE_BULK_SCAN_MOVIES);
			list = importingListOf(importFile, list);
			break;
		case DLMovies:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_DL_MOVIES);
			list = importingDL(importFile, list);
			break;
		case mediaManMovies:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_MEDIAMAN_MOVIES);
			list = importingMediaMan(importFile, list);
			break;
		case shelvesMovies:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_SHELVES_MOVIES);
			list = importingShelves(type, importFile, list);
			break;
		case listOfMovies:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_LIST_OF_MOVIES);
			list = importingListOf(importFile, list);
			break;
		case bulkScanMusic:
			importFile = IOUtilities.getExternalFile(FILE_BULK_SCAN_MUSIC);
			list = importingListOf(importFile, list);
			break;
		case DLMusic:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_DL_MUSIC);
			list = importingDL(importFile, list);
			break;
		case mediaManMusic:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_MEDIAMAN_MUSIC);
			list = importingMediaMan(importFile, list);
			break;
		case shelvesMusic:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_SHELVES_MUSIC);
			list = importingShelves(type, importFile, list);
			break;
		case listOfMusic:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_LIST_OF_MUSIC);
			list = importingListOf(importFile, list);
			break;
		case bulkScanSoftware:
			importFile = IOUtilities.getExternalFile(FILE_BULK_SCAN_SOFTWARE);
			list = importingListOf(importFile, list);
			break;
		case DLSoftware:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_DL_SOFTWARE);
			list = importingDL(importFile, list);
			break;
		case shelvesSoftware:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_SHELVES_SOFTWARE);
			list = importingShelves(type, importFile, list);
			break;
		case listOfSoftware:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_LIST_OF_SOFTWARE);
			list = importingListOf(importFile, list);
			break;
		case bulkScanTools:
			importFile = IOUtilities.getExternalFile(FILE_BULK_SCAN_TOOLS);
			list = importingListOf(importFile, list);
			break;
		case DLTools:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_DL_TOOLS);
			list = importingDL(importFile, list);
			break;
		case shelvesTools:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_SHELVES_TOOLS);
			list = importingShelves(type, importFile, list);
			break;
		case listOfTools:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_LIST_OF_TOOLS);
			list = importingListOf(importFile, list);
			break;
		case bulkScanToys:
			importFile = IOUtilities.getExternalFile(FILE_BULK_SCAN_TOYS);
			list = importingListOf(importFile, list);
			break;
		case DLToys:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_DL_TOYS);
			list = importingDL(importFile, list);
			break;
		case shelvesToys:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_SHELVES_TOYS);
			list = importingShelves(type, importFile, list);
			break;
		case listOfToys:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_LIST_OF_TOYS);
			list = importingListOf(importFile, list);
			break;
		case bulkScanVideoGames:
			importFile = IOUtilities.getExternalFile(FILE_BULK_SCAN_VIDEOGAMES);
			list = importingListOf(importFile, list);
			break;
		case DLVideoGames:
			importFile = IOUtilities.getExternalFile(IMPORT_FILE_DL_VIDEOGAMES);
			list = importingDL(importFile, list);
			break;
		case mediaManVideoGames:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_MEDIAMAN_VIDEOGAMES);
			list = importingMediaMan(importFile, list);
			break;
		case shelvesVideoGames:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_SHELVES_VIDEOGAMES);
			list = importingShelves(type, importFile, list);
			break;
		case listOfVideoGames:
			importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_LIST_OF_VIDEOGAMES);
			list = importingListOf(importFile, list);
			break;
		default:
			break;
		}
		return list;
	}

	private static ArrayList<ItemImport> importingDL(File importFile,
			ArrayList<ItemImport> list) {
		if (!importFile.exists())
			return list;

		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					importFile)), IOUtilities.IO_BUFFER_SIZE);

			String line;

			// Read the TSV headers
			in.readLine();

			while ((line = in.readLine()) != null) {
				if (!TextUtilities.isEmpty(line)) {
					final String[] splitLine = line.split("\t");

					final ItemImport item = new ItemImport();

					int i = 0;
					item.id_one = splitLine[i];
					i++;
					if (i < splitLine.length)
						item.notes = splitLine[i];
					i++;
					if (i < splitLine.length)
						item.rating = splitLine[i];

					list.add(item);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			IOUtilities.closeStream(in);
		}

		return list;
	}

	private static ArrayList<ItemImport> importingBoardGameGeek(
			inputTypes type, File importFile, ArrayList<ItemImport> list) {
		if (!importFile.exists())
			return list;

		BufferedReader in = null;

		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					importFile)), IOUtilities.IO_BUFFER_SIZE);

			String line;
			manualItems = new ArrayList<String>();

			// GJT: Read the CSV headers; store them for manual item adds
			header = in.readLine().split(",");

			int eanPos = -1;
			int ratingPos = -1;
			int wishlistPos = -1;

			// find positions
			for (int i = 0; i < header.length; i++) {
				if (header[i].equals(BaseItem.OBJECTID))
					eanPos = i;
				else if (header[i].equals(BaseItem.RATING))
					ratingPos = i;
				else if (header[i].equals(BaseItem.BGG_WISHLIST))
					wishlistPos = i;
			}

			while ((line = in.readLine()) != null) {
				ItemImport item = new ItemImport();
				// GJT: I only want the EAN--if one exists!
				if (line.indexOf(",") >= 0) {
					line = line.replace("\"", "");
					final String[] splitLine = line.split(",");
					final int splitLineLength = splitLine.length;
					try {
						if (!TextUtilities.isEmpty(splitLine[eanPos])) {
							item.id_one = splitLine[eanPos];
						}

						item.rating = splitLine[ratingPos];

						int rating = (int) Math.ceil(Double
								.parseDouble(item.rating));

						if (rating > 5)
							item.rating = String.valueOf((int) Math
									.ceil(rating / 2));
						else
							item.rating = String.valueOf(rating);

						item.wishlist = splitLine[wishlistPos];

						if (item.wishlist.equals("1"))
							item.wishlist = TextUtilities.getCurrentDate();
						else
							item.wishlist = IOUtilities.NO_OP;

						list.add(item);
					} catch (ArrayIndexOutOfBoundsException e) {
						Log.e(LOG_TAG, splitLine[eanPos] + ": " + e.toString());
					}
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			IOUtilities.closeStream(in);
		}

		return list;
	}

	private static ArrayList<ItemImport> importingGoogleLibraryBooks(
			ArrayList<ItemImport> list) {
		for (int i = 0; i < IMPORT_FILE_GOOGLE_LIBRARY_BOOKS.length; i++) {
			File importFile = IOUtilities
					.getExternalFile(IMPORT_FILE_GOOGLE_LIBRARY_BOOKS[i]);
			if (!importFile.exists())
				continue;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			try {
				// Get an instance of document builder
				DocumentBuilder db = dbf.newDocumentBuilder();

				// Parse using builder to get DOM representation of the XML file
				Document dom = db.parse(importFile);
				Element root = dom.getDocumentElement();

				// Get a nodelist of the <book> elements
				NodeList books = root.getElementsByTagName("book");
				if (books != null && books.getLength() > 0) {
					for (int j = 0; j < books.getLength(); j++) {
						ItemImport item = new ItemImport();
						// Get the ISBN
						Element el = (Element) books.item(j);
						if (getTextValue(el, "value") != null)
							item.id_one = getTextValue(el, "value");

						list.add(item);
					}
				}
			} catch (ParserConfigurationException pce) {
				pce.printStackTrace();
			} catch (SAXException se) {
				se.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		return list;
	}

	private static String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if (nl != null && nl.getLength() > 0) {
			Element el = (Element) nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}

		return textVal;
	}

	private static ArrayList<ItemImport> importingMediaMan(File importFile,
			ArrayList<ItemImport> list) throws IOException {
		if (!importFile.exists())
			return list;
		BufferedReader in = null;

		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					importFile), "UTF16"), IOUtilities.IO_BUFFER_SIZE);

			String line;

			// Read the TSV headers
			final String[] header = in.readLine().split("\t");

			int asinPos = -1;
			int eanPos = -1;
			int isbnPos = -1;
			int tagPos = -1;
			int ratingPos = -1;

			// find positions
			for (int i = 0; i < header.length; i++) {
				if (asinPos == -1 && header[i].equals("ASIN"))
					asinPos = i;
				else if (eanPos == -1 && header[i].equals("EAN"))
					eanPos = i;
				else if (isbnPos == -1 && header[i].equals("ISBN"))
					isbnPos = i;
				else if (tagPos == -1 && header[i].equals("Tag"))
					tagPos = i;
				else if (ratingPos == -1 && header[i].equals("Rating"))
					ratingPos = i;
			}

			while ((line = in.readLine()) != null) {
				ItemImport item = new ItemImport();
				if (line.indexOf("\t") >= 0) {
					final String[] splitLine = line.split("\t");
					final int splitLineLength = splitLine.length;
					try {
						if (asinPos != -1)
							item.id_one = splitLine[asinPos];
						if (TextUtilities.isEmpty(item.id_one)) {
							if (eanPos != -1)
								item.id_one = splitLine[eanPos];
							if (TextUtilities.isEmpty(item.id_one))
								item.id_one = splitLine[isbnPos];
						}
						if (tagPos != -1 && tagPos < splitLineLength)
							item.tags = splitLine[tagPos];
						if (ratingPos != -1 && ratingPos < splitLineLength)
							if (!TextUtilities.isEmpty(splitLine[ratingPos]))
								item.rating = splitLine[ratingPos];
						list.add(item);
					} catch (ArrayIndexOutOfBoundsException e) {
						Log.e(LOG_TAG, splitLine[asinPos] + ": " + e.toString());
					}
				}
			}
		} finally {
			IOUtilities.closeStream(in);
		}

		return list;
	}

	private static ArrayList<ItemImport> importingLibraryThingBooks(
			ArrayList<ItemImport> list) throws IOException {
		File importFile = IOUtilities
				.getExternalFile(IMPORT_FILE_LIBRARY_THING_BOOKS);

		if (!importFile.exists())
			return list;

		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					importFile)), IOUtilities.IO_BUFFER_SIZE);

			String line;
			String ISBN = null;

			// Read the TSV headers
			in.readLine();

			Pattern p = Pattern.compile("[^0-9]");
			Matcher m = p.matcher("");

			Pattern anp = Pattern.compile("[^A-Za-z0-9, ]");
			Matcher anm = anp.matcher("");

			while ((line = in.readLine()) != null) {
				ItemImport item = new ItemImport();

				// GJT: Like Shelfari, this list is always the same format.
				if (line.indexOf("\t") >= 0) {
					final String rowData[] = line.split("\t");
					final String rawISBN = rowData[7];
					m.reset(rawISBN);
					ISBN = m.replaceAll("");

					if (!TextUtilities.isEmpty(ISBN))
						item.id_one = ISBN;

					m.reset(rowData[20]);
					item.rating = m.replaceAll("");

					anm.reset(rowData[22]);
					item.tags = anm.replaceAll("");
					list.add(item);
				}
			}
		} finally {
			IOUtilities.closeStream(in);
		}

		return list;
	}

	private static ArrayList<ItemImport> importingShelfariBooks(
			ArrayList<ItemImport> list) throws IOException {
		File importFile = IOUtilities
				.getExternalFile(IMPORT_FILE_SHELFARI_BOOKS);

		if (!importFile.exists())
			return list;

		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					importFile)), IOUtilities.IO_BUFFER_SIZE);

			String line;
			String ISBN = null;
			// Read the TSV headers
			in.readLine();

			while ((line = in.readLine()) != null) {
				ItemImport item = new ItemImport();
				// GJT: Shelfari is easy; it'll always give the list in the same
				// format. All I really care about is the ISBN, nuts to the
				// rest.
				if (line.indexOf("\t") >= 0) {
					ISBN = line.split("\t", 4)[2];
					ISBN = ISBN.substring(1, ISBN.length() - 1); // GJT: remove
					// quote marks
				}
				if (!TextUtilities.isEmpty(ISBN))
					item.id_one = ISBN;
				list.add(item);
			}
		} finally {
			IOUtilities.closeStream(in);
		}

		return list;
	}

	private static ArrayList<ItemImport> importingShelves(inputTypes type,
			File importFile, ArrayList<ItemImport> list) {
		if (!importFile.exists())
			return list;

		BufferedReader in = null;

		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					importFile)), IOUtilities.IO_BUFFER_SIZE);

			String line;
			manualItems = new ArrayList<String>();

			// GJT: Read the TSV headers; store them for manual item adds
			header = in.readLine().split("\t");

			int eanPos = -1;
			int titlePos = -1;
			int sortTitlePos = -1;
			int descPos = -1;
			int tagPos = -1;
			int ratingPos = -1;
			int notesPos = -1;
			int loanDatePos = -1;
			int loanToPos = -1;
			int eventIDPos = -1;
			int wishlistPos = -1;

			// find positions
			for (int i = 0; i < header.length; i++) {
				if (header[i].equals(BaseItem.EAN))
					eanPos = i;
				else if (header[i].equals(BaseItem.TITLE))
					titlePos = i;
				else if (header[i].equals(BaseItem.SORT_TITLE))
					sortTitlePos = i;
				else if (header[i].equals(BaseItem.REVIEWS))
					descPos = i;
				else if (header[i].equals(BaseItem.TAGS))
					tagPos = i;
				else if (header[i].equals(BaseItem.RATING))
					ratingPos = i;
				else if (header[i].equals(BaseItem.NOTES))
					notesPos = i;
				else if (header[i].equals(BaseItem.LOAN_DATE))
					loanDatePos = i;
				else if (header[i].equals(BaseItem.LOANED_TO))
					loanToPos = i;
				else if (header[i].equals(BaseItem.EVENT_ID))
					eventIDPos = i;
				else if (header[i].equals(BaseItem.WISHLIST_DATE))
					wishlistPos = i;
			}

			while ((line = in.readLine()) != null) {
				ItemImport item = new ItemImport();
				// GJT: For Shelves imports, I only want the EAN--if one exists!
				if (line.indexOf("\t") >= 0) {
					line = line.replace("\"", "");
					final String[] splitLine = line.split("\t");
					final int splitLineLength = splitLine.length;
					try {
						item.internalID = splitLine[1];

						if (!TextUtilities.isEmpty(splitLine[eanPos])) {
							item.id_one = splitLine[eanPos];
						} else {
							item.id_one = splitLine[eanPos + 1];
						}

						if (TextUtilities.isManualItem(item.internalID)) {
							manualItems.add(line);
						}

						item.title = splitLine[titlePos];
						if (sortTitlePos != 0)
							item.sort_title = splitLine[sortTitlePos];

						item.desc = splitLine[descPos];
						item.tags = splitLine[tagPos];
						item.rating = splitLine[ratingPos];

						if (notesPos != -1 && notesPos < splitLineLength)
							item.notes = splitLine[notesPos];

						if (loanToPos != -1 && loanToPos < splitLineLength)
							item.loan_to = splitLine[loanToPos];

						if (loanDatePos != -1 && loanDatePos < splitLineLength)
							item.loan_date = splitLine[loanDatePos];

						if (eventIDPos != -1 && eventIDPos < splitLineLength)
							item.event_id = splitLine[eventIDPos];

						if (wishlistPos != -1 && wishlistPos < splitLineLength)
							item.wishlist = splitLine[wishlistPos];

						list.add(item);
					} catch (ArrayIndexOutOfBoundsException e) {
						Log.e(LOG_TAG, eanPos + ": " + e.toString());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtilities.closeStream(in);
		}

		return list;
	}

	private static ArrayList<ItemImport> importingListOf(File importFile,
			ArrayList<ItemImport> list) throws IOException {
		if (!importFile.exists())
			return list;

		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					importFile)), IOUtilities.IO_BUFFER_SIZE);

			String line;

			// Read the TSV headers
			in.readLine();

			while ((line = in.readLine()) != null) {
				final int index = line.indexOf('\t');
				final int length = line.length();

				final ItemImport item = new ItemImport();
				// Only one field, we grab the entire line
				if (index == -1 && length > 0) {
					item.id_one = line;
					// Only one field, the first one is empty
				} else if (index != length - 1) {
					item.id_one = line.substring(index + 1);

				} else if (index == 0 || line.equals("")) {
					// GJT: Skip it; it's just an empty tab or line
				}
				// We have two fields, or the second one is empty
				else {
					item.id_one = line.substring(0, index);
				}

				list.add(item);
			}
		} finally {
			IOUtilities.closeStream(in);
		}

		return list;
	}

	// GJT: Made this more generic by removing Book references
	public static boolean addCoverToCache(String iId, Bitmap bitmap) {
		File cacheDirectory;

		if (bitmap == null) {
			return false;
		}
		try {
			cacheDirectory = ensureCache();
		} catch (IOException e) {
			Log.e(LOG_TAG, "Could not create cache directory!");
			return false;
		}

		File coverFile = new File(cacheDirectory, iId);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(coverFile);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
		} catch (FileNotFoundException e) {
			return false;
		} finally {
			IOUtilities.closeStream(out);
		}

		return true;
	}

}
