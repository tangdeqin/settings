package com.android.settings.sim.tct;

import android.annotation.Nullable;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/*************************************************************************
 Copyright (C), 2016, TCL communication Co., Ltd.
 File name: MarqueeText.java
 Author: quantai.zhu
 Version: 1.0
 Date: 2017/06/22
 Description:In order to implements Marquee function.
 History: merge from 4933269  on 2017/06/22
 **************************************************************************/
public class MarqueeText extends TextView {

    public MarqueeText(Context context) {
        super(context);
    }

    public MarqueeText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueeText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean isFocused() {
        return true;
    }
}
