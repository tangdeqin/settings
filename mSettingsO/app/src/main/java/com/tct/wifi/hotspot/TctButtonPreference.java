/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.tct.wifi.hotspot;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.settings.R;

/**
 * To Customize TCT WiFi Tethering menu
 * add on 6897886
 * @author yucheng.luo
 * @Time 2018-09-01
 */
public class TctButtonPreference extends Preference implements
        OnClickListener {
    private String mText;
    private Button mButton;
    //private HotspotClient mHotspotClient;
    private String mHotspotClientaddress;
    private OnButtonClickCallback mCallBack;
    //[TCT-ROM][Unblock Function]Begin modified by yucheng.luo for XR7051529 & 7054243 on 18-10-29
    private boolean isBlock;
    interface OnButtonClickCallback {
        void onClick(View v, String mHotspotClientaddress,boolean block);
    }
    //[TCT-ROM][Unblock Function]Begin modified by yucheng.luo for XR7051529 & 7054243 on 18-10-29

    public TctButtonPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    public TctButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    //[TCT-ROM][Unblock Function]Begin modified by yucheng.luo for XR7051529 & 7054243 on 18-10-29
    public TctButtonPreference(Context context,
                               String hotspotClientAddress, boolean block,OnButtonClickCallback listner) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_button);
        mHotspotClientaddress = hotspotClientAddress;
        isBlock = block;
        mCallBack = listner;
    }
    //[TCT-ROM][Unblock Function]Begin modified by yucheng.luo for XR7051529 & 7054243 on 18-10-29

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mButton = (Button) view.findViewById(R.id.preference_button);
        mButton.setText(mText);
        mButton.setFocusable(false);
        mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mCallBack != null && v != null) {
        //Begin modified by yucheng.luo for XR7304195 on 18-12-29
            if(!isFastClick()){
                mCallBack.onClick(v, mHotspotClientaddress,isBlock); //[TCT-ROM][Unblock Function]Begin modified by yucheng.luo for XR7051529 & 7054243 on 18-10-29
            }
        //End modified by yucheng.luo for XR7304195 on 18-12-29
        }
    }

    public void setButtonText(String text) {
        mText = text;
        notifyChanged();
    }

    public String getMacAddress() {
        return mHotspotClientaddress;
    }

   //Begin added by yucheng.luo for XR7304195 on 18-12-29
    /**
     * The interval between two clicks cannot be less than 500ms
     */
    private static final int MIN_DELAY_TIME= 500;
    private static long lastClickTime = 0;
    public static boolean isFastClick() {
        boolean flag = true;
        long currentClickTime = System.currentTimeMillis();
        if ((currentClickTime - lastClickTime) >= MIN_DELAY_TIME) {
            flag = false;
        }
        lastClickTime = currentClickTime;
        return flag;
    }
   //End added by yucheng.luo for XR7304195 on 18-12-29

}

