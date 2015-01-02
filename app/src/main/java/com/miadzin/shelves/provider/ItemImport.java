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

package com.miadzin.shelves.provider;

import android.os.Parcel;
import android.os.Parcelable;

// Lame helper class for imports
public class ItemImport implements Parcelable {
	public String internalID;
	public String id_one;
	public String id_two;
	public String title;
	public String sort_title;
	public String desc;
	public String tags;
	public String notes;
	public String rating;
	public String loan_date;
	public String loan_to;
	public String event_id;
	public String wishlist;

	public ItemImport() {
		id_one = "";
		id_two = "";
		title = "";
		sort_title = "";
		desc = "";
		tags = "";
		notes = "";
		rating = "0";
		wishlist = "";
	}

	public ItemImport(Parcel source) {
		internalID = source.readString();
		id_one = source.readString();
		id_two = source.readString();
		title = source.readString();
		sort_title = source.readString();
		desc = source.readString();
		tags = source.readString();
		notes = source.readString();
		rating = source.readString();
		loan_date = source.readString();
		loan_to = source.readString();
		event_id = source.readString();
		wishlist = source.readString();
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(internalID);
		dest.writeString(id_one);
		dest.writeString(id_two);
		dest.writeString(title);
		dest.writeString(sort_title);
		dest.writeString(desc);
		dest.writeString(tags);
		dest.writeString(notes);
		dest.writeString(rating);
		dest.writeString(loan_date);
		dest.writeString(loan_to);
		dest.writeString(event_id);
		dest.writeString(wishlist);
	}

	public class MyCreator implements Parcelable.Creator<ItemImport> {
		public ItemImport createFromParcel(Parcel source) {
			return new ItemImport(source);
		}

		public ItemImport[] newArray(int size) {
			return new ItemImport[size];
		}
	}
}
