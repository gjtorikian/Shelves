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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.miadzin.shelves.licensing.LicenseCheck;
import com.miadzin.shelves.R;

public class UIUtilities {

	private final static String LOG_TAG = "UIUtilities";

	private UIUtilities() {
	}

	public static void showImageToast(Context context, int id, Drawable drawable) {
		final View view = LayoutInflater.from(context).inflate(
				R.layout.book_notification, null);
		((TextView) view.findViewById(R.id.message)).setText(id);
		((ImageView) view.findViewById(R.id.cover)).setImageDrawable(drawable);

		Toast toast = new Toast(context);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(view);

		toast.show();
	}

	public static void showImageToast(Context context, String text,
			Drawable drawable) {
		final View view = LayoutInflater.from(context).inflate(
				R.layout.book_notification, null);
		((TextView) view.findViewById(R.id.message)).setText(text);
		((ImageView) view.findViewById(R.id.cover)).setImageDrawable(drawable);

		Toast toast = new Toast(context);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(view);

		toast.show();
	}

	public static void showToast(Context context, int id) {
		showToast(context, id, false);
	}

	public static void showToast(Context context, int id, boolean longToast) {
		Toast.makeText(context, id,
				longToast ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
	}

	public static void showToast(Context context, String text) {
		showToast(context, text, false);
	}

	public static void showToast(Context context, String text, boolean longToast) {
		Toast.makeText(context, text,
				longToast ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
	}

	public static void showFormattedImageToast(Context context, int id,
			Drawable drawable, Object... args) {

		final View view = LayoutInflater.from(context).inflate(
				R.layout.book_notification, null);
		((TextView) view.findViewById(R.id.message)).setText(String.format(
				context.getText(id).toString(), args));
		((ImageView) view.findViewById(R.id.cover)).setImageDrawable(drawable);

		Toast toast = new Toast(context);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(view);

		toast.show();
	}

	public static void showFormattedToast(Context context, int id,
			Object... args) {
		Toast.makeText(context,
				String.format(context.getText(id).toString(), args),
				Toast.LENGTH_LONG).show();
	}

	public static void showIdentificationDots(TextView identification_dots,
			int pos) {
		String[] dotsArray = { " * ", " * ", " * " };
		dotsArray[pos] = "<font color='red' size='20px'>" + dotsArray[pos]
				+ "</font>";

		StringBuilder dots = new StringBuilder();
		for (String i : dotsArray) {
			dots.append(i);
		}

		identification_dots.setText(Html.fromHtml(dots.toString()),
				TextView.BufferType.SPANNABLE);
	}

	public static boolean isHoneycomb() {
		// Can use static final constants like HONEYCOMB, declared in later
		// versions
		// of the OS since they are inlined at compile time. This is guaranteed
		// behavior.
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean isHoneycombTablet(Context context) {
		// Can use static final constants like HONEYCOMB, declared in later
		// versions
		// of the OS since they are inlined at compile time. This is guaranteed
		// behavior.
		return isHoneycomb()
				&& (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	public static boolean isICS() {
		return Build.VERSION.SDK_INT >= 14;
	}

	public static boolean isGingerbread() {
		return Build.VERSION.SDK_INT >= 9;
	}

	public static Dialog createUnsupportedDialog(Activity a) {
		return new AlertDialog.Builder(a)
				.setMessage(R.string.not_supported_for_this_item)
				.setPositiveButton(R.string.okay_label,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						}).create();
	}

	public static boolean isPaid(ContentResolver cr, Context context) {
        return LicenseCheck.check(context);
	}
}
