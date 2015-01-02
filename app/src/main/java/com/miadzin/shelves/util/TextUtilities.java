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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

import com.miadzin.shelves.base.BaseManualAddActivity;
import com.miadzin.shelves.server.ServerInfo;

public final class TextUtilities {
	private static final String LOG_TAG = "TextUtilities";

	private TextUtilities() {
	}

	public static String join(Collection<?> items, String delimiter) {
		if (items == null || items.isEmpty()) {
			return "";
		}

		final Iterator<?> iter = items.iterator();
		final StringBuilder buffer = new StringBuilder(iter.next().toString());

		while (iter.hasNext()) {
			buffer.append(delimiter).append(iter.next());
		}

		// GJT: Sometimes descriptions have tabs, replace them with spaces
		return buffer.toString().replace("\t", " ").replace("\n", " ");
	}

	// GJT: Added this method to clean up author sorting
	public static String joinAuthors(Collection<?> items, String delimiter) {
		if (items == null || items.isEmpty() || items.size() <= 0) {
			return "";
		}

		final Iterator<?> iter = items.iterator();

		// Grab the first item in the collection
		String firstItem = iter.next().toString();
		final StringBuilder buffer = new StringBuilder(
				flipAuthorName(firstItem));

		// If there are more items, add them normally
		while (iter.hasNext()) {
			buffer.append(delimiter).append(iter.next());
		}

		return buffer.toString();
	}

	public static String flipAuthorName(String authorName) {
		String firstName;
		String lastName;
		// If author name has a space, swap the last and first name...
		if (authorName.contains(" ")) {
			int firstSpace = authorName.indexOf(" ");
			int lastSpace = authorName.lastIndexOf(" ");

			// 1st case: there are initials
			if (authorName.contains(".")) {
				// Like: F. Scott Fitzgerald
				if (authorName.replaceAll("[^.]", "").length() == 1
						&& authorName.indexOf(".") == 1) {
					firstName = authorName.substring(0, lastSpace);
					lastName = authorName.substring(lastSpace + 1);

					authorName = lastName + ", " + firstName;
				}
				// Like: Raymond D. Souza, T. S. Eliot, W. E. B. Du Bois
				else {
					int lastPeriod = authorName.lastIndexOf(".");

					// Here's a new one: Bill Martin Jr. (period at end)
					if (lastPeriod == authorName.length() - 1) {
						firstName = authorName.substring(0, firstSpace);
						lastName = authorName.substring(firstSpace + 1);

						authorName = lastName + ", " + firstName;
					} else {
						firstName = authorName.substring(0, lastPeriod + 1);
						lastName = authorName.substring(lastPeriod + 2);

						authorName = lastName + ", " + firstName;
					}
				}
			}

			// 2nd case: No initals, but, a first, middle, and last name, like
			// Mary Wollstonecraft Shelley
			else if (lastSpace > firstSpace) {
				firstName = authorName.substring(0, lastSpace);
				lastName = authorName.substring(lastSpace + 1);

				authorName = lastName + ", " + firstName;
			}

			// 3rd case: Just a first and last name
			else {
				firstName = authorName.substring(0, firstSpace);
				lastName = authorName.substring(firstSpace + 1);

				authorName = lastName + ", " + firstName;
			}
		}
		// If it doesn't have a space, that means the author only has one name
		// (like Aeschylus), so nothing needs to be done
		return authorName;
	}

	// GJT: Rewrites the URL portion so it doesn't appear in the database
	public static String protectString(String stringToReplace) {
		if (!TextUtilities.isEmpty(stringToReplace)) {
			int firstPos = stringToReplace.indexOf(".");
			int secondPos = stringToReplace.indexOf(".", firstPos + 1);

			if (secondPos < stringToReplace.length() && (secondPos >= 0))
				return ServerInfo.NAME + stringToReplace.substring(secondPos);
			else
				return ServerInfo.NAME + stringToReplace;
		}
		return "";
	}

	public static String unprotectString(String stringToReplace) {
		if (TextUtilities.isEmpty(stringToReplace)) {
			return " ";
		}

		String replaced = stringToReplace;
		int pos = stringToReplace.indexOf(ServerInfo.NAME);
		boolean hasURL = stringToReplace.contains(".com");

		if (pos >= 0 && !hasURL) {
			replaced = stringToReplace
					.substring(pos + ServerInfo.NAME.length());
		} else if (stringToReplace.startsWith(ServerInfo.IMAGE_PHRASE
				+ ServerInfo.NAME)
				|| hasURL) {
			replaced = ServerInfo.IMAGE_START
					+ stringToReplace.substring(pos
							+ ServerInfo.IMAGE_PHRASE.length() + 1);
		}

		return replaced;
	}

	public static List<String> breakString(String item, String delimiter) {
		if (!isEmpty(item)) {
			String[] items = item.split(delimiter);
			return Arrays.asList(items);
		}

		return null;
	}

	// GJT: Added these for checking emptiness
	public static boolean isEmpty(List<String> list) {
		return (list == null || list.isEmpty() || list.size() <= 0)
				|| (list.size() == 1 && list.get(0).equals(""));
	}

	public static boolean isEmpty(String text) {
		return (text == null || TextUtils.isEmpty(text) || text.trim().length() == 0);
	}

	public static boolean isEmpty(CharSequence title) {
		return isEmpty(title.toString());
	}

	public static boolean isManualItem(String iID) {
		if (TextUtilities.isEmpty(iID))
			return false;
		else
			return iID.contains(BaseManualAddActivity.manualSuffix);
	}

	public static String removeBrackets(String str) {
		if (TextUtilities.isEmpty(str))
			return " ";
		return str.substring(1, str.length() - 1);
	}

	public static String keyFor(Pattern[] mKeyPrefixes, Pattern[] mKeySuffixes,
			String name) {
		final Locale locale = Locale.getDefault();

		final Pattern[] prefixes = mKeyPrefixes;
		for (Pattern prefix : prefixes) {
			final Matcher matcher = prefix.matcher(name);
			if (matcher.find()) {
				if (prefix.toString().contains("die")
						&& !(locale.equals(Locale.GERMAN) || locale
								.equals(Locale.GERMANY))) {
					// GJT: If it starts with "die," like Die Hard, and this
					// isn't a German locale, ignore it!
				} else {
					name = name.substring(matcher.end());
				}
				break;
			}
		}

		final Pattern[] suffixes = mKeySuffixes;
		for (Pattern suffix : suffixes) {
			final Matcher matcher = suffix.matcher(name);
			if (matcher.find()) {
				name = name.substring(0, matcher.start());
				break;
			}
		}

		return name;
	}

	public static class RevStrComp implements Comparator<String> {
		// Implement the compare() method so that it
		// reverses the order of the string comparison.
		public int compare(String strA, String strB) {
			// Compare strB to strA, rather than strA to strB.
			return strB.compareTo(strA);
		}
	}

	public static String readFileAsString(String filePath)
			throws java.io.IOException {
		StringBuffer sb = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(filePath)), "UTF8"));
			String str;
			while ((str = in.readLine()) != null) {
				sb.append(str + "\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	public static void writeStringToFile(File fileName, String data)
			throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		try {
			// FileWriter always assumes default encoding is OK!
			out.write(data);
		} finally {
			out.close();
		}
	}

	public static int countMatches(String str, String sub) {
		if (isEmpty(str) || isEmpty(sub)) {
			return 0;
		}
		int count = 0;
		int idx = 0;
		while ((idx = str.indexOf(sub, idx)) != -1) {
			count++;
			idx += sub.length();
		}
		return count;
	}

	public static String rtrim(String source) {
		return source.replaceAll("\\s+$", "");
	}

	public static String itrim(String source) {
		return source.replaceAll("\\b\\s{2,}\\b", " ");
	}

	public static String getPubYear(Date pubDate) {
		if (pubDate != null) {
			final String date = new SimpleDateFormat("MMMM yyyy")
					.format(pubDate);
			final int spacePos = date.indexOf(" ");
			if (spacePos >= 0) {
				return date.substring(spacePos + 1);
			} else
				return "";
		} else
			return "";
	}

	public static String getPubMonth(Date pubDate) {
		if (pubDate != null) {
			final String date = new SimpleDateFormat("MMMM yyyy")
					.format(pubDate);
			final int spacePos = date.indexOf(" ");
			if (spacePos >= 0) {
				return date.substring(0, spacePos);
			} else
				return "";
		} else
			return "";
	}

	/**
	 * Formats the given text as a CDATA element to be used in a XML file. This
	 * includes adding the starting and ending CDATA tags. Please notice that
	 * this may result in multiple consecutive CDATA tags.
	 * 
	 * @param unescaped
	 *            the unescaped text to be formatted
	 * @return the formatted text, inside one or more CDATA tags
	 */
	public static String stringAsCData(String unescaped) {
		// "]]>" needs to be broken into multiple CDATA segments, like:
		// "Foo]]>Bar" becomes "<![CDATA[Foo]]]]><![CDATA[>Bar]]>"
		// (the end of the first CDATA has the "]]", the other has ">")
		String escaped = unescaped.replaceAll("]]>", "]]]]><![CDATA[>");
		return "<![CDATA[" + escaped + "]]>";
	}

	public static String getCurrentDate() {
		Format dateFormatter = Preferences.getDateFormat();

		Date date = new Date();
		date.setTime(date.getTime() + 0 * 24L * 3600 * 1000);
		date.setMinutes(0);

		return dateFormatter.format(date);
	}

	public static String capitalizeString(String string) {
		char[] chars = string.toLowerCase().toCharArray();
		boolean found = false;
		for (int i = 0; i < chars.length; i++) {
			if (!found && Character.isLetter(chars[i])) {
				chars[i] = Character.toUpperCase(chars[i]);
				found = true;
			} else if (Character.isWhitespace(chars[i]) || chars[i] == '.'
					|| chars[i] == '\'') { // You can add other chars here
				found = false;
			}
		}
		return String.valueOf(chars);
	}
}
