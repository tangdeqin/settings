package com.android.settings.fingerprint.ex;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageView;

/**
 * Copyright (C) 2017 Tcl Corporation Limited
 * Created by jinlong.lu
 */
//Begin added by jinlong.lu for XR6618444 on 18-7-31
public class CheckablelView extends ImageView implements Checkable {

    private final static String TAG = "CheckablelView";

    private boolean isChecked = false;

    public CheckablelView(Context context) {
        super(context);

    }

    public CheckablelView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public CheckablelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    public CheckablelView(Context context, AttributeSet attrs,
                          int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void setChecked(boolean arg0) {
        isChecked = arg0;
    }

    @Override
    public void toggle() {
        isChecked = !isChecked;
    }

}
//End added by jinlong.lu for XR6618444 on 18-7-31