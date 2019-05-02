/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */
//Begin added by lei.ren.hz for XR7102851 on 2018/11/13
package com.android.settingslib;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/**
 * Preference class that supports being disabled by a user restriction
 * set by a device admin.
 */
public class RestrictedScrollPreference extends RestrictedPreference {

    public RestrictedScrollPreference(Context context, AttributeSet attrs,
                                      int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public RestrictedScrollPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RestrictedScrollPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                android.support.v7.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    public RestrictedScrollPreference(Context context) {
        this(context, null);
    }

    //begin add by qing.cao.hz for 7212323 on 2018/12/10
    @Override
    public void performClick() {
        super.performClick();
    }
    //end add by qing.cao.hz for 7212323 on 2018/12/10

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final View scrollSummary = holder.findViewById(android.R.id.summary);
        if (scrollSummary != null && scrollSummary instanceof TextView) {
            ((TextView) scrollSummary).setMaxLines(5);
            (scrollSummary).setScrollbarFadingEnabled(true);
            scrollSummary.setNestedScrollingEnabled(true);
            (scrollSummary).setVerticalScrollBarEnabled(true);
            scrollSummary.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });
            ((TextView) scrollSummary).setMovementMethod(ScrollingMovementMethod.getInstance());
        }
        //begin add by qing.cao.hz for 7212323 on 2018/12/10
        ((TextView)scrollSummary).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                performClick();
            }
        });
        //end add by qing.cao.hz for 7212323 on 2018/12/10
    }
}
//End added by lei.ren.hz for XR7102851 on 2018/11/13
