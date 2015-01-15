/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license
 * 
 * Redesigned, fixed bugs and made customizable by Andrey Novikov
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.androzic.ui;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.androzic.library.R;

/**
 * SeekbarPreference class implements seekbar {@link android.preference.DialogPreference} edit.
 * <p>
 * Attributes supported:<br/>
 * <code>android:text</code> - current value display suffix, not required<br/>
 * <code>android:dialogMessage</code> - dialog title, not required<br/>
 * <code>android:defaultValue</code> - default value, integer, default 0<br/>
 * <p>
 * Styled attributes supported:<br/>
 * <code>text</code> - reference to plurals resource to display current value, if set overrides android:text suffix<br/>
 * <code>zeroText</code> - string to show instead of a value if current value equals to 0<br/>
 * <code>min</code> - minimum value, integer, default 0<br/>
 * <code>max</code> - maximum value, integer, default 100<br/>
 * <code>multiplier</code> - multiplier used for value display (note that it will not affect persisted value), default 1<br/>
 * <code>format</code> - format of value display, suitable for {@link java.text.DecimalFormat}, default "0"
 * 
 * @author Andrey Novikov
 */
public class SeekbarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
	private static final String androidns = "http://schemas.android.com/apk/res/android";

	private SeekBar mSeekBar;
	private TextView mValueText;
	private Context mContext;

	private String mDialogMessage, mSuffix, mZeroText;
	private int mDefault, mMin, mMax, mValue = 0, mPluralText;
	private double mMultiplier = 1d;
	private DecimalFormat format;

	public SeekbarPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
		Resources resources = mContext.getResources();
		int resId;

		resId = attrs.getAttributeResourceValue(androidns, "dialogMessage", 0);
		if(resId != 0)
			mDialogMessage = resources.getString(resId);
		else
			mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");

		resId = attrs.getAttributeResourceValue(androidns, "text", 0);
		if(resId != 0)
			mSuffix = resources.getString(resId);
		else
			mSuffix = attrs.getAttributeValue(androidns, "text");

		TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.SeekbarPreference);
		mPluralText =  styledAttributes.getResourceId(R.styleable.SeekbarPreference_text, 0);
		if (mPluralText != 0 && !"plurals".equals(resources.getResourceTypeName(mPluralText)))
			mPluralText = 0;
		mZeroText = styledAttributes.getString(R.styleable.SeekbarPreference_zeroText);
		mMin = styledAttributes.getInt(R.styleable.SeekbarPreference_min, 0);
		mMax = styledAttributes.getInt(R.styleable.SeekbarPreference_max, 100);
		mMultiplier = styledAttributes.getFloat(R.styleable.SeekbarPreference_multiplier, 1);
		String fmt = styledAttributes.getString(R.styleable.SeekbarPreference_format);
		if (fmt == null)
			fmt = "0";
		styledAttributes.recycle();
		format = new DecimalFormat(fmt);
	}

	@Override
	protected void onBindView(@NonNull View view)
	{
		super.onBindView(view);
		getValue();
	}

	@Override
	protected View onCreateDialogView()
	{
		LinearLayout.LayoutParams params;
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		int padding = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 16 : 6;
		padding *= (mContext.getResources().getDisplayMetrics().density + 0.5);
		layout.setPadding(padding, padding, padding, padding);

		if (mDialogMessage != null) {
			TextView splashText = new TextView(mContext);
			splashText.setText(mDialogMessage);
			layout.addView(splashText);
		}

		if (mContext != null) {
			mValueText = new TextView(mContext);
			mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
			mValueText.setTextSize(26);
			params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
			layout.addView(mValueText, params);
		}

		mSeekBar = new SeekBar(mContext);
		layout.addView(mSeekBar, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		if (isPersistent())
			mValue = getPersistedInt(mDefault);

		mSeekBar.setMax(mMax - mMin);
		setProgress(mValue - mMin);
		mSeekBar.setOnSeekBarChangeListener(this);
		return layout;
	}

	@Override
	protected void onBindDialogView(@NonNull View view)
	{
		super.onBindDialogView(view);
		mSeekBar.setMax(mMax - mMin);
		setProgress(mValue - mMin);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
	{
		if (restorePersistedValue)
			mValue = getPersistedInt(0);
		else
			mValue = (Integer) defaultValue;

		if (shouldPersist())
			persistInt(mValue);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index)
	{
		mDefault = a.getInteger(index, 0);
		return mDefault;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if (positiveResult)
		{
			if (callChangeListener(mValue) && shouldPersist())
				persistInt(mValue);
		}
	}

	/**
	 * Called when user changes progress value, stores value in persistent storage if applicable
	 */
	@Override
	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
	{
		mValue = value + mMin;
		if (mValueText != null)
		{
			mValueText.setText(getText(mValue));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seek)
	{
	}

	@Override
	public void onStopTrackingTouch(SeekBar seek)
	{
	}

	/**
	 * Sets real maximum possible value
	 * @param max new maximum
	 */
	public void setMax(int max)
	{
		mMax = max;
	}

	/**
	 * Returns real maximum possible value
	 * @return maximum value
	 */
	public int getMax()
	{
		return mMax;
	}

	/**
	 * Sets real minimum possible value
	 * @param min new minimum
	 */
	public void setMin(int min)
	{
		mMin = min;
	}

	/**
	 * Returns real minimum possible value
	 * @return minimum value
	 */
	public int getMin()
	{
		return mMin;
	}

	/**
	 * Sets fake progress (for internal use)
	 * @param progress fake progress
	 */
	public void setProgress(int progress)
	{
		int value = progress+mMin;
		if (mSeekBar != null)
			mSeekBar.setProgress(progress);
		if (mValueText != null)
		{
			mValueText.setText(getText(value));
		}
	}

	public int getValue()
	{
		if (isPersistent())
		{
			mValue = getPersistedInt(mDefault);
		}
		return mValue;
	}

	public String getText()
	{
		return getText(getValue());
	}
	
	private String getText(int value)
	{
		double v = value * mMultiplier;
		if (v == 0d && mZeroText != null)
			return mZeroText;
		String t = format.format(v);
		if (mPluralText != 0)
			return mContext.getResources().getQuantityString(mPluralText, (int) v, t);
		if (mSuffix != null)
			t = t.concat(" ").concat(mSuffix);
		return t;
	}
}
