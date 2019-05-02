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
 * limitations under the License.
 */

package com.tct.deviceinfo;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.android.settings.R;
import android.util.Log;
public class UpdatesPreference extends Preference {

    private  boolean isShow;

    public UpdatesPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public UpdatesPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public UpdatesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UpdatesPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_updates);
        isShow = false;
    }

    public void showCircleView(boolean show){
        if(show != isShow){
            isShow = show;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final CircleView mCircleView = (CircleView) holder.itemView.findViewById(R.id.circle_view);
        if(mCircleView != null){
            mCircleView.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }
}

