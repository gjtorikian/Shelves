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

package com.miadzin.shelves.base;

import android.database.CharArrayBuffer;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.miadzin.shelves.drawable.CrossFadeDrawable;

public class BaseItemViewHolder {
	public String itemType;

	public TextView title;
	public TextView author;
	public String id;
	public CrossFadeDrawable transition;
	public final CharArrayBuffer buffer = new CharArrayBuffer(64);
	public final CharArrayBuffer loanBuffer = new CharArrayBuffer(64); // GJT:
																		// Added
																		// to
	// store loan
	// info
	public ImageView cover; // GJT: Added, for BookListAdapter to not using
							// setCompoundDrawablesWithIntrinsicBounds
	public boolean queryCover;
	public String sortTitle;
	public String sortAuthors; // GJT: Added for author sorts
	public String tags;
	public TextView loanStatus; // GJT: Added, to display loan info
	public TextView wishlistStatus;
	public RatingBar rateNum; // GJT: Added, for ratings
	public CheckBox mMultiselectCheckbox;
	public TextView quantityText;

	// GJT: Added the following, for sorts
	public String sortPublisher;
	public String sortPages;
	public String sortFormat;
	public String sortPrice;
	public String sortDewey;
	public String sortDepartment;
	public String sortFabric;
	public String sortAudience;
	public String sortLabel;
	public String sortPlatform;
	public String sortEsrb;
	public String sortAge;
	public String sortPlayingTime;
	public String sortMinPlayers;
	public String sortMaxPlayers;
	public String sortIssue;
}