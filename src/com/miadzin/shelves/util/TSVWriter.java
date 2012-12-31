package com.miadzin.shelves.util;

// test
/**
 Copyright 2005 Bytecode Pty Ltd.
 Copyright 2010 Garen J. Torikian

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*
 * The code copied from http://opencsv.sourceforge.net/, then from
 * http://secrets-for-android.googlecode.com
 * 
 * GJT: Rewrote references from CSV to TSV
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class TSVWriter {

	private PrintWriter pw;
	private char separator;
	private char quotechar;
	private char escapechar;
	private String lineEnd;

	/** The character used for escaping quotes. */
	public static final char DEFAULT_ESCAPE_CHARACTER = '"';

	/** The default separator to use if none is supplied to the constructor. */
	public static final char DEFAULT_SEPARATOR = '\t';

	/**
	 * The default quote character to use if none is supplied to the
	 * constructor.
	 */
	public static final char DEFAULT_QUOTE_CHARACTER = '"';

	/** The quote constant to use when you wish to suppress all quoting. */
	public static final char NO_QUOTE_CHARACTER = '\u0000';

	/** The escape constant to use when you wish to suppress all escaping. */
	public static final char NO_ESCAPE_CHARACTER = '\u0000';

	/** Default line terminator uses platform encoding. */
	public static final String DEFAULT_LINE_END = "\n";

	/**
	 * Constructs TSVWriter using a tab for the separator.
	 * 
	 * @param writer
	 *            the writer to an underlying TSV source.
	 */
	public TSVWriter(Writer writer) {
		this(writer, DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER,
				DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
	}

	/**
	 * Constructs TSVWriter with supplied separator, quote char, escape char and
	 * line ending.
	 * 
	 * @param writer
	 *            the writer to an underlying TSV source.
	 * @param separator
	 *            the delimiter to use for separating entries
	 * @param quotechar
	 *            the character to use for quoted elements
	 * @param escapechar
	 *            the character to use for escaping quotechars or escapechars
	 * @param lineEnd
	 *            the line feed terminator to use
	 */
	public TSVWriter(Writer writer, char separator, char quotechar,
			char escapechar, String lineEnd) {
		this.pw = new PrintWriter(writer);
		this.separator = separator;
		this.quotechar = quotechar;
		this.escapechar = escapechar;
		this.lineEnd = lineEnd;
	}

	/**
	 * Writes the next line to the file.
	 * 
	 * @param nextLine
	 *            a string array with each comma-separated element as a separate
	 *            entry.
	 */
	public void writeNext(String[] nextLine) {

		if (nextLine == null)
			return;

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < nextLine.length; i++) {

			if (i != 0) {
				sb.append(separator);
			}

			String nextElement = nextLine[i];
			if (nextElement == null) {
				sb.append(" ");
				continue;
			}
			if (quotechar != NO_QUOTE_CHARACTER)
				sb.append(quotechar);
			if (nextElement.length() == 0)
				sb.append(" ");
			else {
				for (int j = 0; j < nextElement.length(); j++) {
					char nextChar = nextElement.charAt(j);
					if (escapechar != NO_ESCAPE_CHARACTER
							&& nextChar == quotechar) {
						sb.append(escapechar).append(nextChar);
					} else if (escapechar != NO_ESCAPE_CHARACTER
							&& nextChar == escapechar) {
						sb.append(escapechar).append(nextChar);
					} else {
						sb.append(nextChar);
					}
				}
			}
			if (quotechar != NO_QUOTE_CHARACTER)
				sb.append(quotechar);
		}

		sb.append(lineEnd);
		pw.write(sb.toString());

	}

	/**
	 * Flush underlying stream to writer.
	 * 
	 * @throws IOException
	 *             if bad things happen
	 */
	public void flush() throws IOException {
		pw.flush();
	}

	/**
	 * Close the underlying stream writer flushing any buffered content.
	 * 
	 * @throws IOException
	 *             if bad things happen
	 * 
	 */
	public void close() throws IOException {
		pw.flush();
		pw.close();
	}

}