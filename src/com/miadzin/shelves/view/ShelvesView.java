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

package com.miadzin.shelves.view;

import java.io.IOException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.ViewConfiguration;
import android.widget.GridView;

import com.miadzin.shelves.R;
import com.miadzin.shelves.base.BaseItemActivity;
import com.miadzin.shelves.drawable.SpotlightDrawable;
import com.miadzin.shelves.drawable.TransitionDrawable;

public class ShelvesView extends GridView {
	private Bitmap mShelfBackground;
	private int mShelfWidth;
	private int mShelfHeight;

	// private Bitmap mWebLeft;
	// private Bitmap mWebRight;
	// private int mWebRightWidth;

	public ShelvesView(Context context) throws NotFoundException, IOException {
		super(context);
		init(context);
	}

	public ShelvesView(Context context, AttributeSet attrs)
			throws NotFoundException, IOException {
		super(context, attrs);
		load(context, attrs, 0);
		init(context);
	}

	public ShelvesView(Context context, AttributeSet attrs, int defStyle)
			throws NotFoundException, IOException {
		super(context, attrs, defStyle);
		load(context, attrs, defStyle);
		init(context);
	}

	private void load(Context context, AttributeSet attrs, int defStyle) {
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.ShelvesView, defStyle, 0);

		final Resources resources = getResources();
		final int background = a.getResourceId(
				R.styleable.ShelvesView_shelfBackground, 0);
		final Bitmap shelfBackground = BitmapFactory.decodeResource(resources,
				background);
		if (shelfBackground != null) {
			mShelfWidth = shelfBackground.getWidth();
			mShelfHeight = shelfBackground.getHeight();
			mShelfBackground = shelfBackground;
		}

		/*
		 * mWebLeft = BitmapFactory.decodeResource(resources,
		 * R.drawable.web_left);
		 * 
		 * final Bitmap webRight = BitmapFactory.decodeResource(resources,
		 * R.drawable.web_right); mWebRightWidth = webRight.getWidth();
		 * mWebRight = webRight;
		 */
		a.recycle();
	}

	private void init(Context context) throws NotFoundException, IOException {
		StateListDrawable drawable = new StateListDrawable();

		SpotlightDrawable start = new SpotlightDrawable(context, this);
		start.disableOffset();
		SpotlightDrawable end = new SpotlightDrawable(context, this,
				R.drawable.spotlight_blue);
		end.disableOffset();
		TransitionDrawable transition = new TransitionDrawable(start, end);
		drawable.addState(new int[] { android.R.attr.state_pressed },
				transition);

		final SpotlightDrawable normal = new SpotlightDrawable(context, this);
		drawable.addState(new int[] {}, normal);

		normal.setParent(drawable);
		transition.setParent(drawable);

		setSelector(drawable);
		setDrawSelectorOnTop(false);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		final int count = getChildCount();
		final int top = count > 0 ? getChildAt(0).getTop() : 0;
		final int shelfWidth = mShelfWidth;
		final int shelfHeight = mShelfHeight;
		final int width = getWidth();
		final int height = getHeight();
		final Bitmap background = mShelfBackground;

		for (int x = 0; x < width; x += shelfWidth) {
			for (int y = top; y < height; y += shelfHeight) {
				canvas.drawBitmap(background, x, y, null);
			}
		}

		if (count == 0) {
			/*
			 * canvas.drawBitmap(mWebLeft, 0.0f, top + 1, null);
			 * canvas.drawBitmap(mWebRight, width - mWebRightWidth, top +
			 * shelfHeight + 1, null);
			 */
		}

		super.dispatchDraw(canvas);
	}

	@Override
	public void setPressed(boolean pressed) {
		super.setPressed(pressed);

		final Drawable current = getSelector().getCurrent();
		if (current instanceof TransitionDrawable) {
			if (pressed) {
				((TransitionDrawable) current)
						.startTransition(ViewConfiguration
								.getLongPressTimeout());
			} else {
				if (!BaseItemActivity.mMultiSelect)
					((TransitionDrawable) current).resetTransition();
			}
		}
	}
}