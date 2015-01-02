/*
 * Funambol is a mobile platform developed by Funambol, Inc. 
 * Copyright (C) 2003 - 2007 Funambol, Inc.
 * Copyright (C) 2010 Garen J. Torikian
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission 
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY FUNAMBOL, FUNAMBOL DISCLAIMS THE 
 * WARRANTY OF NON INFRINGEMENT  OF THIRD PARTY RIGHTS.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 * 
 * You can contact Funambol, Inc. headquarters at 643 Bair Island Road, Suite 
 * 305, Redwood City, CA 94063, USA, or at email address info@funambol.com.
 * 
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * "Powered by Funambol" logo. If the display of the logo is not reasonably 
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by Funambol". 
 */

package com.miadzin.shelves.util;

import java.util.Hashtable;

/**
 * This class supplies some methods to escape / unescape special chars according
 * XML specifications
 * 
 */
public class Entities {

	private static final String[][] BASIC_ARRAY = { { "quot", "34" }, // " -
																		// double
																		// quote
			{ "amp", "38" }, // & - ampersand
			{ "lt", "60" }, // < - less-than
			{ "gt", "62" }, // > - greater-than
			{ "apos", "39" }, // XML apostrophe
	};

	/**
	 * <p>
	 * The set of entities supported by standard XML.
	 * </p>
	 */
	public static final Entities XML;

	static {
		XML = new Entities();
		XML.addEntities(BASIC_ARRAY);
	}

	static interface EntityMap {
		void add(String name, int value);

		String name(int value);

		int value(String name);
	}

	static class PrimitiveEntityMap implements EntityMap {
		private Hashtable mapNameToValue = new Hashtable();
		private Hashtable mapValueToName = new Hashtable();

		public void add(String name, int value) {
			mapNameToValue.put(name, Integer.valueOf(value));
			mapValueToName.put(Integer.valueOf(value), name);
		}

		public String name(int value) {
			return (String) mapValueToName.get(Integer.valueOf(value));
		}

		public int value(String name) {
			Object value = mapNameToValue.get(name);
			// GJT: Fixes some bug in Google Books that I don't have the sanity
			// to fix
			if (name.startsWith("quot")) {
				return 34;
			}
			if (value == null) {
				return -1;
			}
			return ((Integer) value).intValue();
		}
	}

	static class LookupEntityMap extends PrimitiveEntityMap {

		private String[] lookupTable;
		private int LOOKUP_TABLE_SIZE = 256;

		@Override
		public String name(int value) {

			if (value < LOOKUP_TABLE_SIZE) {
				return lookupTable()[value];
			}

			return super.name(value);
		}

		private String[] lookupTable() {
			if (lookupTable == null) {
				createLookupTable();
			}
			return lookupTable;
		}

		private void createLookupTable() {
			lookupTable = new String[LOOKUP_TABLE_SIZE];
			for (int i = 0, l = LOOKUP_TABLE_SIZE; i < l; ++i) {
				lookupTable[i] = super.name(i);
			}
		}
	}

	EntityMap map = new Entities.LookupEntityMap();

	public void addEntities(String[][] entityArray) {
		for (int i = 0; i < entityArray.length; ++i) {
			addEntity(entityArray[i][0], Integer.parseInt(entityArray[i][1]));
		}
	}

	public void addEntity(String name, int value) {
		map.add(name, value);
	}

	public String entityName(int value) {
		return map.name(value);
	}

	public int entityValue(String name) {
		return map.value(name);
	}

	/**
	 * <p>
	 * Escapes special characters in a <code>String</code>.
	 * </p>
	 * 
	 * 
	 * @param str
	 *            The <code>String</code> to escape.
	 * @return A escaped <code>String</code>.
	 */
	public String escape(String str) {

		char ch = ' ';

		String entityName = null;
		StringBuffer buf = null;

		int intValue = 0;

		buf = new StringBuffer(str.length() * 2);

		for (int i = 0, l = str.length(); i < l; ++i) {

			ch = str.charAt(i);
			entityName = this.entityName(ch);

			if (entityName == null) {

				if (ch > 0x7F) {

					intValue = ch;
					buf.append("&#");
					buf.append(intValue);
					buf.append(';');

				} else {
					buf.append(ch);
				}
			} else {

				buf.append('&');
				buf.append(entityName);
				buf.append(';');

			}
		}

		return buf.toString();
	}

	/**
	 * <p>
	 * Unescapes special characters in a <code>String</code>.
	 * </p>
	 * 
	 * @param str
	 *            The <code>String</code> to escape.
	 * @return A un-escaped <code>String</code>.
	 */
	public String unescape(String str) {

		StringBuffer buf = null;
		String entityName = null;

		char ch = ' ';
		char charAt1 = ' ';

		int entityValue = 0;

		buf = new StringBuffer(str.length());

		for (int i = 0, l = str.length(); i < l; ++i) {

			ch = str.charAt(i);

			if (ch == '&') {

				// GJT: some crazy situation to back out of emdashes--first
				// noticed with Reading like a Writer
				if (str.length() >= i + 6) {
					if (str.substring(i + 1, i + 6).equals("#8212")) {
						buf.append('Ñ');
						i = i + 5;
						continue;
					}
				}
				if (str.length() >= i + 5) {
					if (str.substring(i + 1, i + 5).equals("#149")) {
						buf.append('*');
						i = i + 4;
						continue;
					}
				}

				int semi = str.indexOf(';', i + 1);

				if (semi == -1) {
					buf.append(ch);
					continue;
				}

				entityName = str.substring(i + 1, semi);

				if (entityName.charAt(0) == '#') {
					charAt1 = entityName.charAt(1);
					if (charAt1 == 'x' || charAt1 == 'X') {
						entityValue = Integer.valueOf(entityName.substring(2),
								16).intValue();
					} else {
						entityValue = Integer.parseInt(entityName.substring(1));
					}
				} else {
					entityValue = this.entityValue(entityName);
				}
				if (entityValue == -1) {
					buf.append('&');
					buf.append(entityName);
					buf.append(';');
				} else {
					buf.append((char) (entityValue));
				}

				i = semi;

			} else {

				buf.append(ch);

			}
		}

		return buf.toString();
	}

}