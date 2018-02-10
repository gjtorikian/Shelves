package com.miadzin.shelves.util;

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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.os.Environment;

public class IOUtilities {
	public enum inputTypes {
		bulkScanApparel, DLApparel, shelvesApparel, listOfApparel, bulkScanBoardGames, boardGameGeekBoardGames, shelvesBoardGames, listOfBoardGames, bulkScanBooks, DLBooks, googleLibraryBooks, libraryThingBooks, mediaManBooks, shelfariBooks, shelvesBooks, listOfBooks, bulkScanComics, shelvesComics, bulkScanGadgets, DLGadgets, shelvesGadgets, listOfGadgets, bulkScanMovies, DLMovies, mediaManMovies, shelvesMovies, listOfMovies, bulkScanMusic, DLMusic, mediaManMusic, shelvesMusic, listOfMusic, bulkScanSoftware, DLSoftware, shelvesSoftware, listOfSoftware, bulkScanTools, DLTools, shelvesTools, listOfTools, bulkScanToys, DLToys, shelvesToys, listOfToys, bulkScanVideoGames, DLVideoGames, mediaManVideoGames, shelvesVideoGames, listOfVideoGames
	}

	public enum outputTypes {
		DLApparel, shelvesApparel, shelvesBoardGames, DLBooks, googleLibraryBooks, libraryThingBooks, mediaManBooks, shelfariBooks, shelvesBooks, shelvesComics, DLGadgets, shelvesGadgets, DLMovies, mediaManMovies, shelvesMovies, DLMusic, mediaManMusic, shelvesMusic, DLSoftware, shelvesSoftware, DLTools, shelvesTools, DLToys, shelvesToys, DLVideoGames, mediaManVideoGames, shelvesVideoGames
	}

	public static final String PARENT_CACHE_DIRECTORY = "shelves/";
	protected static final String CACHE_DIRECTORY = "shelves/books";

	public static final String FILE_BULK_SCAN_APPAREL = "shelves/Bulk_Scanned_Apparel.txt";
	public static final String FILE_BULK_SCAN_BOARDGAMES = "shelves/Bulk_Scanned_BoardGames.txt";
	public static final String FILE_BULK_SCAN_BOOKS = "shelves/Bulk_Scanned_Books.txt";
	public static final String FILE_BULK_SCAN_COMICS = "shelves/Bulk_Scanned_Comics.txt";
	public static final String FILE_BULK_SCAN_GADGETS = "shelves/Bulk_Scanned_Gadgets.txt";
	public static final String FILE_BULK_SCAN_MOVIES = "shelves/Bulk_Scanned_Movies.txt";
	public static final String FILE_BULK_SCAN_MUSIC = "shelves/Bulk_Scanned_Music.txt";
	public static final String FILE_BULK_SCAN_SOFTWARE = "shelves/Bulk_Scanned_Software.txt";
	public static final String FILE_BULK_SCAN_TOOLS = "shelves/Bulk_Scanned_Tools.txt";
	public static final String FILE_BULK_SCAN_TOYS = "shelves/Bulk_Scanned_Toys.txt";
	public static final String FILE_BULK_SCAN_VIDEOGAMES = "shelves/Bulk_Scanned_VideoGames.txt";

	public static final String FILE_SCAN_APPAREL_RESULTS = "shelves/Import_Apparel_Results.txt";
	public static final String FILE_SCAN_BOARDGAMES_RESULTS = "shelves/Import_BoardGames_Results.txt";
	public static final String FILE_SCAN_BOOKS_RESULTS = "shelves/Import_Books_Results.txt";
	public static final String FILE_SCAN_COMICS_RESULTS = "shelves/Import_Comics_Results.txt";
	public static final String FILE_SCAN_GADGETS_RESULTS = "shelves/Import_Gadgets_Results.txt";
	public static final String FILE_SCAN_MOVIES_RESULTS = "shelves/Import_Movies_Results.txt";
	public static final String FILE_SCAN_MUSIC_RESULTS = "shelves/Import_Music_Results.txt";
	public static final String FILE_SCAN_SOFTWARE_RESULTS = "shelves/Import_Software_Results.txt";
	public static final String FILE_SCAN_TOOLS_RESULTS = "shelves/Import_Tools_Results.txt";
	public static final String FILE_SCAN_TOYS_RESULTS = "shelves/Import_Toys_Results.txt";
	public static final String FILE_SCAN_VIDEOGAMES_RESULTS = "shelves/Import_VideoGames_Results.txt";

	public static final String NO_OP = "NoOp";
	public static final String LOG_TAG = "IOUtilities";

	public static final int IO_BUFFER_SIZE = 4 * 1024;

	public static File getExternalFile(String file) {
		return new File(Environment.getExternalStorageDirectory(), file);
	}

	/**
	 * Copy the content of the input stream into the output stream, using a
	 * temporary byte array buffer whose size is defined by
	 * {@link #IO_BUFFER_SIZE}.
	 * 
	 * @param in
	 *            The input stream to copy from.
	 * @param out
	 *            The output stream to copy to.
	 * 
	 * @throws java.io.IOException
	 *             If any error occurs during the copy.
	 */
	public static void copy(InputStream in, OutputStream out)
			throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}

	/**
	 * Closes the specified stream.
	 * 
	 * @param stream
	 *            The stream to close.
	 */
	public static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				android.util.Log.e(LOG_TAG, "Could not close stream", e);
			}
		}
	}

	public static File getCacheDirectory() {
		return IOUtilities.getExternalFile(CACHE_DIRECTORY);
	}

	public static File ensureCache() throws IOException {
		File cacheDirectory = getCacheDirectory();
		if (!cacheDirectory.exists()) {
			if (!cacheDirectory.mkdirs()) {
				File parentCache = ensureParentCache();
				new File(parentCache, ".nomedia").createNewFile();
				cacheDirectory = getCacheDirectory();
				cacheDirectory.mkdirs();
			}
			new File(cacheDirectory, ".nomedia").createNewFile();
		}
		return cacheDirectory;
	}

	public static File ensureParentCache() throws IOException {
		File cacheDirectory = new File(
				Environment.getExternalStorageDirectory(),
				PARENT_CACHE_DIRECTORY);
		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdirs();
			new File(cacheDirectory, ".nomedia").createNewFile();
		}
		return cacheDirectory;
	}

	public static boolean deleteCache(Context context) {
		File dir = getCacheDirectory();

		return deleteDirectory(dir);
	}

	static public boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	public static String getFileName(String file) {
		return file.substring(file.lastIndexOf("/") + 1);
	}

	public static final void copy(File source, File target) throws Exception {

		if (source.isDirectory()) {
			if (!target.exists()) {
				target.mkdir();
			}
			String[] children = source.list();
			for (int i = 0; i < children.length; i++) {
				copy(new File(source, children[i]), new File(target,
						children[i]));
			}
		} else {
			if (!target.exists())
				target.createNewFile();
			InputStream in = new FileInputStream(source);
			OutputStream out = new FileOutputStream(target);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
			in.close();
			out.close();
		}
	}
}
