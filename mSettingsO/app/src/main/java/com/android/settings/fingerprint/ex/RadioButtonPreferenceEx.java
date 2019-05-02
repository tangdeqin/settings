package com.android.settings.fingerprint.ex;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.android.settings.R;

/**
 * Copyright (C) 2017 Tcl Corporation Limited
 * Added by jinlong.lu for XR6618444 on 18-7-31
 **/
public class RadioButtonPreferenceEx extends CheckBoxPreference {
    private final static String VIEW_CHECKBOX_TYPE = "CheckBox";
    private final static String VIEW_RADIOBUTTON_TYPE = "RadioButton";
    private static final String TAG = RadioButtonPreferenceEx.class.getSimpleName();
    private String mStyleType;

    public interface OnClickListener {
        abstract void onRadioButtonClicked(RadioButtonPreferenceEx emiter);
    }

    private OnClickListener mListener = null;

    public RadioButtonPreferenceEx(Context context) {
        super(context, null, 0, R.style.RadioButtonPreferenceEx);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    public RadioButtonPreferenceEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle, R.style.RadioButtonPreferenceEx);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RadioButtonPreferenceEx, defStyle, 0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.RadioButtonPreferenceEx_styleType:
                    Log.d(TAG, "RadioButtonPreferenceEx_styleType");
                    mStyleType = a.getString(attr);
                    break;
                default:
                    mStyleType = VIEW_RADIOBUTTON_TYPE;
                    break;

            }

        }
        a.recycle();
        if (mStyleType.equals(VIEW_CHECKBOX_TYPE)) {
            setWidgetLayoutResource(R.layout.preference_widget_checkbox);
            Log.d(TAG, "setWidgetLayoutResource c");
        } else {
            setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
            Log.d(TAG, "setWidgetLayoutResource r");
        }
    }

    public RadioButtonPreferenceEx(Context context, AttributeSet attrs) {
        super(context, attrs, 0, R.style.RadioButtonPreferenceEx);
        setWidgetLayoutResource(R.layout.preference_widget_checkbox);
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
