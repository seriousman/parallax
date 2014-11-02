package example.com.parallax;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ObservableScrollView extends ScrollView {

	private static final String TAG = ObservableScrollView.class.getSimpleName();

	private OnScrollListener mCallbacks;
	private int mCurrentScroll;

	public ObservableScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private boolean mFlgFirstScrollReported = false;

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		mFlgFirstScrollReported = true;
		mCurrentScroll = t;
		if (mCallbacks != null) {
			mCallbacks.onScrollChanged(t);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mCallbacks != null) {
			switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				mCallbacks.onDownMotionEvent();
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mCallbacks.onUpOrCancelMotionEvent();
				break;
			}
		}
		return super.onTouchEvent(ev);
	}

	public int getViewWindowTop() {
		return mCurrentScroll;
	}

	public int getViewWindowBottom() {
		return mCurrentScroll + getHeight();
	}

	@Override
	public int computeVerticalScrollRange() {
		return super.computeVerticalScrollRange();
	}

	public void setOnScrollListener(OnScrollListener listener) {
		mCallbacks = listener;
	}

	public static interface OnScrollListener {

		public void onScrollChanged(int scrollY);
		public void onDownMotionEvent();
		public void onUpOrCancelMotionEvent();
	}
}
