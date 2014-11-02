package example.com.parallax;

import android.content.Context;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import org.parceler.Parcel;
import org.parceler.Parcels;

public class ParallaxHolder {

	/** Interface for listening parallax progress (e.g. for setting transparency on items */
	public interface OnParallaxScrollListener {

		public void onParallaxScroll(float progress);
	}

	@InjectView(R.id.content_scroll_view) ObservableScrollView mContentScrollView;
	@InjectView(R.id.parallax_container) FrameLayout mParallaxContainer;
	@InjectView(R.id.content_container) FrameLayout mContentContainer;

	private static final String TAG = ParallaxHolder.class.getSimpleName();

	private final Context mContext;
	private final View mRootView;

	private OnParallaxScrollListener mListener;

	private View mContentView;
	private View mParallaxView;
	private View mHeaderView;

	// View size parameters
	private int mHeaderHeight;
	private int mParallaxContentHeight;

	// Parallax scroll parameters
	private float mParallaxProgress;
	private int mParallaxMaxScroll;

	private State mState = new State();
	@Parcel public static class State {

		int mScrollY;
	}

	public ParallaxHolder(Context context, ViewGroup container) {
		mContext = context;
		mRootView = LayoutInflater.from(context).inflate(R.layout.parallax_content_view, container, false);
		ButterKnife.inject(this, mRootView);
		mContentScrollView.setOnScrollListener(mScrollListener);
	}

	public View getRoot() {
		return mRootView;
	}

	public Parcelable saveState() {
		mState.mScrollY = mContentScrollView.getScrollY();
		return Parcels.wrap(mState);
	}

	public void restoreState(Parcelable savedInstanceState) {
		State state = Parcels.unwrap(savedInstanceState);
		if (state != null) mState = state;
	}

	public float getParallaxProgress() {
		return mParallaxProgress;
	}

	public void setParallaxListener(OnParallaxScrollListener listener) {
		mListener = listener;
	}

	public ParallaxHolder setOffsetForHeader(final View headerView) {
		mHeaderView = headerView;
		mHeaderHeight = headerView.getHeight();
		return this;
	}

	public ParallaxHolder setParallaxImage(@DrawableRes int drawableResId) {
		// Create image view
		ImageView imageView = new ImageView(mContext);
		imageView.setImageResource(drawableResId);
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		imageView.setLayoutParams(layoutParams);
		imageView.setAdjustViewBounds(true);
		mParallaxView = imageView;
		return this;
	}

	public ParallaxHolder setParallaxLayout(@LayoutRes int layoutResId) {
		mParallaxView = LayoutInflater.from(mContext).inflate(layoutResId, mParallaxContainer, false);
		return this;
	}

	public ParallaxHolder setParallaxView(View parallaxView) {
		mParallaxView = parallaxView;
		return this;
	}

	public ParallaxHolder setContentLayout(@LayoutRes int layoutResId) {
		mContentView = LayoutInflater.from(mContext).inflate(layoutResId, mContentContainer, false);
		return this;
	}

	public ParallaxHolder setContentText(@StringRes int stringResId) {
		TextView textView = new TextView(mContext);
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		textView.setLayoutParams(layoutParams);
		textView.setText(stringResId);
		mContentView = textView;
		return this;
	}

	public ParallaxHolder setContentText(Spanned spanned) {
		TextView textView = new TextView(mContext);
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		textView.setLayoutParams(layoutParams);
		textView.setText(spanned);
		mContentView = textView;
		return this;
	}

	public ParallaxHolder setContentView(View contentView) {
		mContentView = contentView;
		return this;
	}

	public ParallaxHolder build() {
		if (mContentView == null)
			throw new IllegalStateException("Empty content. Set content using setContentXXX() method");
		if (mParallaxView == null)
			throw new IllegalStateException("Empty parallax view. Set using setParallaxXXX() method");

		mContentContainer.removeAllViews();
		mParallaxContainer.removeAllViews();

		mContentContainer.addView(mContentView);
		mParallaxContainer.addView(mParallaxView);

		mRootView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);

		return this;
	}

	private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

		private int mPassNumber = 0;

		@Override public void onGlobalLayout() {
			switch (mPassNumber) {
			case 0:
				// First pass. Get content height and set layout margins
				mParallaxContentHeight = mParallaxContainer.getHeight();
				mHeaderHeight = mHeaderView == null ? 0 : mHeaderView.getHeight();

				mContentContainer.setPadding(0, mParallaxContentHeight + mHeaderHeight, 0, 0);
				((ViewGroup.MarginLayoutParams) mParallaxContainer.getLayoutParams()).setMargins(0, mHeaderHeight, 0, 0);
				mParallaxMaxScroll = mParallaxContentHeight;
				mParallaxContainer.requestLayout();
				mContentContainer.requestLayout();
				break;
			case 1:
				// Second pass, after setting layout margins we can set scroll
				mContentScrollView.setScrollY(mState.mScrollY);

				// Disconnect observer, reset pass number
				mPassNumber = 0;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				} else {
					mRootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
				return;
			}
			++mPassNumber;
		}
	};

	private ObservableScrollView.OnScrollListener mScrollListener = new ObservableScrollView.OnScrollListener() {

		@Override public void onScrollChanged(int rawScrollY) {
			int scrollY = Math.max(0, Math.min(mParallaxMaxScroll, rawScrollY));
			float parallaxProgress = ((float) scrollY) / mParallaxMaxScroll;

			mParallaxView.setTranslationY(scrollY / 2);
			mParallaxContainer.setTranslationY(-scrollY);

			if (mListener != null && parallaxProgress != mParallaxProgress) {
				mListener.onParallaxScroll(parallaxProgress);
			}

			mParallaxProgress = parallaxProgress;
		}
		@Override public void onDownMotionEvent() {}
		@Override public void onUpOrCancelMotionEvent() {}
	};
}
