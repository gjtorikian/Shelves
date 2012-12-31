package com.miadzin.shelves.base;

import android.content.Context;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.miadzin.shelves.R;
import com.miadzin.shelves.drawable.CrossFadeDrawable;
import com.miadzin.shelves.drawable.FastBitmapDrawable;
import com.miadzin.shelves.util.ImageUtilities;
import com.miadzin.shelves.util.TextUtilities;

public class BaseItemAdapter extends CursorAdapter implements
		FilterQueryProvider {
	private static final String LOG_TAG = "BaseItemAdapter";

	protected int mTitleIndex;
	protected int mSortTitleIndex;
	protected int mSortAuthorsIndex; // GJT: Added, for author sort
	protected int mInternalIdIndex;
	protected int mTagsIndex; // GJT: Added, for tags to show up in search
	protected int mLoanIdIndex;
	protected int mWishlistIdIndex;
	protected int mRatingIdIndex;
	protected int mQuantityIndex;

	protected int mAgeIndex;
	protected int mPlayingTimeIndex;
	protected int mMinPlayersIndex;
	protected int mMaxPlayersIndex;

	protected int mPublisherIndex;
	protected int mPagesIndex;
	protected int mFormatIndex;
	protected int mPriceIndex;
	protected int mDeweyIndex;

	private int mDepartmentIndex;
	private int mFabricIndex;

	private int mAudienceIndex;
	private int mLabelIndex;

	private int mPlatformIndex;

	private int mEsrbIndex;

	protected final String mSortOrder;
	protected final String mViewType;

	protected final Bitmap mDefaultCoverBitmap;
	protected final FastBitmapDrawable mDefaultCover;

	protected final LayoutInflater mInflater;
	protected BaseItemActivity mActivity;

	public BaseItemAdapter(Context context, Cursor cursor, boolean requery,
			String sortOrder, String viewType) {
		super(context, cursor, requery);

		mSortOrder = sortOrder;
		mInflater = LayoutInflater.from(context);
		mViewType = viewType;

		if (mViewType.equals(BaseItemActivity.SHELF_VIEW)
				|| mViewType.equals(BaseItemActivity.LIST_VIEW)) {
			mDefaultCoverBitmap = BitmapFactory.decodeResource(
					context.getResources(), R.drawable.unknown_cover);
			mDefaultCover = new FastBitmapDrawable(mDefaultCoverBitmap);
		} else {
			mDefaultCoverBitmap = null;
			mDefaultCover = null;
		}

		final String activityToMatch = context.toString();
		final Cursor c = getCursor();

		mTitleIndex = c.getColumnIndexOrThrow(BaseItem.TITLE);
		mSortTitleIndex = c.getColumnIndexOrThrow(BaseItem.SORT_TITLE);
		mSortAuthorsIndex = c.getColumnIndexOrThrow(BaseItem.AUTHORS);
		mInternalIdIndex = c.getColumnIndexOrThrow(BaseItem.INTERNAL_ID);
		mTagsIndex = c.getColumnIndexOrThrow(BaseItem.TAGS);
		mLoanIdIndex = c.getColumnIndex(BaseItem.LOANED_TO);
		mWishlistIdIndex = c.getColumnIndex(BaseItem.WISHLIST_DATE);
		mRatingIdIndex = c.getColumnIndex(BaseItem.RATING);
		mPriceIndex = c.getColumnIndex(BaseItem.RETAIL_PRICE);
		mQuantityIndex = c.getColumnIndexOrThrow(BaseItem.QUANTITY);

		// GJT: Gadgets, Tools, Toys intentionally absent, due to lack of sort
		// options
		if (activityToMatch.contains("apparel")) {
			mDepartmentIndex = c.getColumnIndex(BaseItem.DEPARTMENT);
			mFabricIndex = c.getColumnIndex(BaseItem.FABRIC);
		} else if (activityToMatch.contains("boardgames")) {
			mAgeIndex = c.getColumnIndexOrThrow(BaseItem.AGE);
			mPlayingTimeIndex = c.getColumnIndexOrThrow(BaseItem.PLAYING_TIME);
			mMinPlayersIndex = c.getColumnIndexOrThrow(BaseItem.MIN_PLAYERS);
			mMaxPlayersIndex = c.getColumnIndexOrThrow(BaseItem.MAX_PLAYERS);
		} else if (activityToMatch.contains("books")) {
			mPublisherIndex = c.getColumnIndex(BaseItem.PUBLISHER);
			mPagesIndex = c.getColumnIndex(BaseItem.PAGES);
			mFormatIndex = c.getColumnIndex(BaseItem.FORMAT);
			mDeweyIndex = c.getColumnIndex(BaseItem.DEWEY_NUMBER);
		} else if (activityToMatch.contains("movies")) {
			mAudienceIndex = c.getColumnIndex(BaseItem.AUDIENCE);
			mFormatIndex = c.getColumnIndex(BaseItem.FORMAT);
			mLabelIndex = c.getColumnIndex(BaseItem.LABEL);
		} else if (activityToMatch.contains("music")) {
			mFormatIndex = c.getColumnIndex(BaseItem.FORMAT);
			mLabelIndex = c.getColumnIndex(BaseItem.LABEL);
		} else if (activityToMatch.contains("software")) {
			mFormatIndex = c.getColumnIndex(BaseItem.FORMAT);
			mPlatformIndex = c.getColumnIndex(BaseItem.PLATFORM);
		} else if (activityToMatch.contains("videogames")) {
			mPlatformIndex = c.getColumnIndex(BaseItem.PLATFORM);
			mEsrbIndex = c.getColumnIndex(BaseItem.ESRB);
		}

		mActivity = (BaseItemActivity) context;

		setFilterQueryProvider(this);
	}

	public FastBitmapDrawable getDefaultCover() {
		return mDefaultCover;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view;
		final BaseItemViewHolder holder = new BaseItemViewHolder();

		if (mViewType.equals(BaseItemActivity.SHELF_VIEW)) {
			view = mInflater.inflate(R.layout.shelf, parent, false);

			final CrossFadeDrawable transition = new CrossFadeDrawable(
					mDefaultCoverBitmap, null);
			transition.setCallback(view);
			transition.setCrossFadeEnabled(true);
			holder.transition = transition;
		} else if (mViewType.equals(BaseItemActivity.LIST_VIEW)) {
			view = mInflater.inflate(R.layout.screen_shelves_list_rows, parent,
					false);

			final ImageView coverView = (ImageView) view
					.findViewById(R.id.cover);
			holder.cover = coverView;

			final TextView authorView = (TextView) view
					.findViewById(R.id.author);
			holder.author = authorView;
		} else {
			view = mInflater.inflate(R.layout.screen_shelves_list_rows_nocover,
					parent, false);
			final TextView authorView = (TextView) view
					.findViewById(R.id.author);
			holder.author = authorView;
		}

		final TextView titleView = (TextView) view.findViewById(R.id.title);
		final TextView loanView = (TextView) view.findViewById(R.id.loan);
		final TextView wishlistView = (TextView) view
				.findViewById(R.id.wishlist);
		final RatingBar rating = (RatingBar) view
				.findViewById(R.id.ratingBarIndicator);
		final CheckBox multiselectView = (CheckBox) view
				.findViewById(R.id.multiSelectCheck);
		final TextView quantity = (TextView) view.findViewById(R.id.quantity);

		holder.title = titleView;

		holder.loanStatus = loanView;
		holder.loanStatus.setText("");
		holder.loanStatus.setVisibility(View.GONE);

		holder.wishlistStatus = wishlistView;
		holder.wishlistStatus.setText("");
		holder.wishlistStatus.setVisibility(View.GONE);

		holder.rateNum = rating;

		holder.quantityText = quantity;
		holder.quantityText.setVisibility(View.GONE);

		holder.mMultiselectCheckbox = multiselectView;
		holder.mMultiselectCheckbox.setVisibility(View.INVISIBLE);
		holder.mMultiselectCheckbox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							BaseItemActivity.multiSelectIds.add(holder.id);
						} else {
							BaseItemActivity.multiSelectIds.remove(holder.id);
						}

					}
				});

		view.setTag(holder);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor c) {
		final BaseItemViewHolder holder = (BaseItemViewHolder) view.getTag();

		final String id = c.getString(mInternalIdIndex);
		holder.id = id;
		holder.sortTitle = c.getString(mSortTitleIndex);
		holder.sortAuthors = c.getString(mSortAuthorsIndex);
		holder.tags = c.getString(mTagsIndex);

		if (mPriceIndex != -1)
			holder.sortPrice = c.getString(mPriceIndex);

		final String activityToMatch = context.toString();

		if (activityToMatch.contains("apparel")) {
			holder.sortDepartment = c.getString(mDepartmentIndex);
			holder.sortFabric = c.getString(mFabricIndex);
		} else if (activityToMatch.contains("boardgames")) {
			holder.sortAge = c.getString(mAgeIndex);
			holder.sortPlayingTime = c.getString(mPlayingTimeIndex);
			holder.sortMinPlayers = c.getString(mMinPlayersIndex);
			holder.sortMaxPlayers = c.getString(mMaxPlayersIndex);
		} else if (activityToMatch.contains("books")) {
			holder.sortPublisher = c.getString(mPublisherIndex);
			holder.sortPages = c.getString(mPagesIndex);
			holder.sortFormat = c.getString(mFormatIndex);
			holder.sortDewey = c.getString(mDeweyIndex);
		} else if (activityToMatch.contains("movies")) {
			holder.sortAudience = c.getString(mAudienceIndex);
			holder.sortFormat = c.getString(mFormatIndex);
			holder.sortLabel = c.getString(mLabelIndex);
		} else if (activityToMatch.contains("music")) {
			holder.sortFormat = c.getString(mFormatIndex);
			holder.sortLabel = c.getString(mLabelIndex);
		} else if (activityToMatch.contains("software")) {
			holder.sortFormat = c.getString(mFormatIndex);
			holder.sortPlatform = c.getString(mPlatformIndex);
		} else if (activityToMatch.contains("videogames")) {
			holder.sortPlatform = c.getString(mPlatformIndex);
			holder.sortEsrb = c.getString(mEsrbIndex);
		}

		if (mViewType.equals(BaseItemActivity.SHELF_VIEW)
				|| mViewType.equals(BaseItemActivity.LIST_VIEW)) {
			final BaseItemActivity activity = mActivity;

			if (mViewType.equals(BaseItemActivity.SHELF_VIEW)) {
				if (activity.isPendingCoversUpdate()) {
					holder.title.setCompoundDrawablesWithIntrinsicBounds(null,
							null, null, mDefaultCover);
					holder.queryCover = true;
				} else {
					holder.title.setCompoundDrawablesWithIntrinsicBounds(null,
							null, null,
							ImageUtilities.getCachedCover(id, mDefaultCover));
					holder.queryCover = false;
				}
			} else {
				if (activity.isPendingCoversUpdate()) {
					holder.cover.setImageDrawable(mDefaultCover);
					holder.queryCover = true;
				} else {
					holder.cover.setImageDrawable(ImageUtilities
							.getCachedCover(id, mDefaultCover));
					holder.queryCover = false;
				}
			}
		}

		if (mViewType.equals(BaseItemActivity.LIST_VIEW)
				|| mViewType.equals(BaseItemActivity.LIST_VIEW_NO_COVER)) {
			holder.author.setText(c.getString(mSortAuthorsIndex));
		}

		final CharArrayBuffer buffer = holder.buffer;
		c.copyStringToBuffer(mTitleIndex, buffer);
		final int size = buffer.sizeCopied;

		holder.loanStatus.setText("");
		holder.loanStatus.setVisibility(View.GONE);

		holder.wishlistStatus.setText("");
		holder.wishlistStatus.setVisibility(View.GONE);

		if (!TextUtilities.isEmpty(BaseItemActivity.multiSelectIds)
				&& BaseItemActivity.multiSelectIds.contains(holder.id)) {
			holder.mMultiselectCheckbox.setVisibility(View.VISIBLE);
			holder.mMultiselectCheckbox.setChecked(true);
			holder.mMultiselectCheckbox.setSelected(true);
			holder.mMultiselectCheckbox.setEnabled(true);
		} else
			holder.mMultiselectCheckbox.setVisibility(View.GONE);

		if (size != 0) {
			holder.title.setText(buffer.data, 0, size);
			holder.rateNum.setRating(c.getInt(mRatingIdIndex));

			if (!TextUtilities.isEmpty(c.getString(mLoanIdIndex))) {
				holder.loanStatus.setText(R.string.loaned_out);
				holder.loanStatus.setVisibility(View.VISIBLE);
				holder.loanStatus.bringToFront();
			} else if (!TextUtilities.isEmpty(c.getString(mWishlistIdIndex))) {
				holder.wishlistStatus.setText(R.string.wishlist_status);
				holder.wishlistStatus.setVisibility(View.VISIBLE);
				holder.wishlistStatus.bringToFront();
			}

			if (!TextUtilities.isEmpty(c.getString(mQuantityIndex))) {
				final String quantityStr = c.getString(mQuantityIndex);
				final int quantityAmt = Integer.parseInt(quantityStr);

				if (quantityAmt > 1) {
					holder.quantityText.setText(quantityStr);
					holder.quantityText.setVisibility(View.VISIBLE);
					holder.quantityText.bringToFront();
				}
			}
		}
	}

	@Override
	public void changeCursor(Cursor cursor) {
		final Cursor oldCursor = getCursor();
		if (oldCursor != null)
			mActivity.stopManagingCursor(oldCursor);
		super.changeCursor(cursor);
	}

	public Cursor runQuery(CharSequence constraint) {
		return BaseItemProvider.runQuery(mActivity, mSortOrder, constraint);
	}
}