/*
 * Add this file by shilei.zhang for Night mode Dev Schedule switch
 * And fixed XR5429845 on 2017/11/01
 * Settings->Display->Night mode->Schedule plan
 */

package com.android.settings.display;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v4.content.res.TypedArrayUtils;

/**
 *Base on CheckBoxPreference, for Night mode SchedulePlan switch
 */
public class MyRadioButtonPreference extends CheckBoxPreference {

    public interface OnClickListener {
        public abstract void onRadioButtonClicked(MyRadioButtonPreference emiter);
    }

    private OnClickListener mListener = null;
    public MyRadioButtonPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        // TODO Auto-generated constructor stub
    }
    public MyRadioButtonPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                android.support.v7.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }
    public MyRadioButtonPreference(Context context) {
        this(context, null);
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick() {
        if (mListener != null) {
            mListener.onRadioButtonClicked(this);
        }
    }
}

