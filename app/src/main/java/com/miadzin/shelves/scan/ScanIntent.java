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

package com.miadzin.shelves.scan;


public final class ScanIntent {
	public static boolean isValidFormat(String scanResultFormat) {
		return true; // GJT: Super hack. People are complaining the barcode
						// stopped working:
						// this is probably why. Fix it in the future...
		// return FORMAT_EAN_13.equals(scanResultFormat)
		// || FORMAT_EAN_12.equals(scanResultFormat);
	}
}
