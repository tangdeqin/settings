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

package com.android.settings.notification;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.settings.R; //add by yeqing.lv for XR6873798 un-use frameworks resource on 2018/08/25


public class SuppressorHelper {
    private static final String TAG = "SuppressorHelper";

    public static String getSuppressionText(Context context, ComponentName suppressor) {
        //modify by yeqing.lv for XR6873798 un-use frameworks resource on 2018/08/25
        return suppressor != null ?
                context.getString(R.string.muted_by,
                        getSuppressorCaption(context, suppressor)) : null;
    }

    @VisibleForTesting
    static String getSuppressorCaption(Context context, ComponentName suppressor) {
        final PackageManager pm = context.getPackageManager();
        try {
            final ServiceInfo info = pm.getServiceInfo(suppressor, 0);
            if (info != null) {
                final CharSequence seq = info.loadLabel(pm);
                if (seq != null) {
                    final String str = seq.toString().trim();
                    if (str.length() > 0) {
                        return str;
                    }
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "Error loading suppressor caption", e);
        }
        return suppressor.getPackageName();
    }

}
