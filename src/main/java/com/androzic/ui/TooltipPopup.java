package com.androzic.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.androzic.library.R;

public class TooltipPopup
{
	protected WindowManager mWindowManager;

	protected Context mContext;
	protected PopupWindow mWindow;

	private TextView mHelpTextView;
	private View mUpArrowView;
	private View mDownArrowView;
	protected View mView;

	protected Drawable mBackgroundDrawable = null;
	protected ShowListener showListener;

	public TooltipPopup(Context context, String text, int viewResource)
	{
		mContext = context;
		mWindow = new PopupWindow(context);

		mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

		LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		setContentView(layoutInflater.inflate(viewResource, null));

		mHelpTextView = (TextView) mView.findViewById(R.id.text);
		mUpArrowView = mView.findViewById(R.id.arrow_up);
		mDownArrowView = mView.findViewById(R.id.arrow_down);

		mHelpTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
		mHelpTextView.setSelected(true);
		setText(text);
	}

	public TooltipPopup(Context context)
	{
		this(context, "", R.layout.tooltip_popup);

	}

	public TooltipPopup(Context context, String text)
	{
		this(context, text, R.layout.tooltip_popup);
	}

	public void show(View anchor)
	{
		preShow();

		int[] location = new int[2];

		anchor.getLocationOnScreen(location);

		Rect anchorRect = new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1] + anchor.getHeight());

		mView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		int rootHeight = mView.getMeasuredHeight();
		int rootWidth = mView.getMeasuredWidth();

		final int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
		final int screenHeight = mWindowManager.getDefaultDisplay().getHeight();

		boolean onTop = anchorRect.top > screenHeight / 2;

		int whichArrow, requestedX;

		whichArrow = onTop ? R.id.arrow_down : R.id.arrow_up;

		final View arrow = whichArrow == R.id.arrow_up ? mUpArrowView : mDownArrowView;
		View hideArrow = whichArrow == R.id.arrow_up ? mDownArrowView : mUpArrowView;

		arrow.setVisibility(View.VISIBLE);
		hideArrow.setVisibility(View.INVISIBLE);

		arrow.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		final int arrowWidth = arrow.getMeasuredWidth();
		final int arrowHeight = arrow.getMeasuredHeight();

		int yPos = onTop ? anchorRect.top - rootHeight + arrowHeight / 2 : anchorRect.bottom - arrowHeight / 2;
		int xPos;

		if (anchorRect.left + rootWidth + 5 > screenWidth)
		{
			xPos = screenWidth - rootWidth - 5;
		}
		else if (anchorRect.left - rootWidth / 2 < 5)
		{
			xPos = anchorRect.left + 5;
		}
		else
		{
			xPos = anchorRect.centerX() - rootWidth / 2;
		}

		anchor.getRootView().getLocationOnScreen(location);
		xPos -= location[0];
		yPos -= location[1];

		requestedX = anchorRect.width() > arrowWidth * 3 ? anchorRect.left + arrowWidth : anchorRect.centerX();
		ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) arrow.getLayoutParams();
		param.leftMargin = (requestedX - xPos) - arrowWidth / 2;

		if (onTop)
			mHelpTextView.setMaxHeight(anchorRect.top - anchorRect.height());
		else
			mHelpTextView.setMaxHeight(screenHeight - yPos);

		mWindow.setTouchInterceptor(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				dismiss();
				return false;
			}
		});
		mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);

		mView.setAnimation(AnimationUtils.loadAnimation(mContext, R.anim.float_anim));
	}

	protected void preShow()
	{
		if (mView == null)
			throw new IllegalStateException("Anchor view undefined");

		if (showListener != null)
		{
			showListener.onPreShow();
			showListener.onShow();
		}

		if (mBackgroundDrawable == null)
			mWindow.setBackgroundDrawable(new BitmapDrawable());
		else
			mWindow.setBackgroundDrawable(mBackgroundDrawable);

		mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		mWindow.setTouchable(true);
//		mWindow.setFocusable(true);
//		mWindow.setOutsideTouchable(true);

		mWindow.setContentView(mView);
	}

	public void setBackgroundDrawable(Drawable background)
	{
		mBackgroundDrawable = background;
	}

	public void setContentView(View root)
	{
		mView = root;
		mWindow.setContentView(root);
	}

	public void setContentView(int layoutResID)
	{
		LayoutInflater inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setContentView(inflator.inflate(layoutResID, null));
	}

	public void setOnDismissListener(PopupWindow.OnDismissListener listener)
	{
		mWindow.setOnDismissListener(listener);
	}

	public void dismiss()
	{
		mWindow.dismiss();
		if (showListener != null)
			showListener.onDismiss();
	}

	public void setText(String text)
	{
		mHelpTextView.setText(text);
	}

	public static interface ShowListener
	{
		void onPreShow();
		void onDismiss();
		void onShow();
	}

	public void setShowListener(ShowListener showListener)
	{
		this.showListener = showListener;
	}
}
