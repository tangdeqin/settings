/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.wifi.details;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
//Begin added by jiabin.zheng.hz for XR6810309 on 2018/08/18
import com.android.settingslib.core.lifecycle.events.OnStart;
//End added by jiabin.zheng.hz for XR6810309 on 2018/08/18

/**
 * ActionBar lifecycle observer for {@link WifiNetworkDetailsFragment}.
 */
//Begin modified by jiabin.zheng.hz for XR6810309 on 2018/08/18
public class WifiDetailActionBarObserver implements LifecycleObserver, OnStart {
//End modified by jiabin.zheng.hz for XR6810309 on 2018/08/18

    private final Fragment mFragment;
    private final Context mContext;

    public WifiDetailActionBarObserver(Context context, Fragment fragment) {
        mContext = context;
        mFragment = fragment;
    }

    //Begin modified by jiabin.zheng.hz for XR6810309 on 2018/08/18
    // when rotate detail screen, setActionBar don't
    // execute in Activity, so fragment don't getActionBar on Create.
    @Override
    public void onStart() {
    //End modified by jiabin.zheng.hz for XR6810309 on 2018/08/18
        if (mFragment.getActivity() != null) {
            mFragment.getActivity().getActionBar()
                    .setTitle(mContext.getString(R.string.wifi_details_title));
        }
    }
}
