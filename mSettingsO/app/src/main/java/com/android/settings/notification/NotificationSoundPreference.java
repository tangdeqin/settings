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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;

import com.android.settings.R;
import com.android.settings.RingtonePreference;

import com.tct.sdk.base.media.TctRingtoneManagerUtil;//[TCT-ROM][Calendar] Added by nanbing.zou for P23591 on 2018-09-14

public class NotificationSoundPreference extends RingtonePreference {
    private Uri mRingtone;

    public NotificationSoundPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Uri onRestoreRingtone() {
        return mRingtone;
    }

    public void setRingtone(Uri ringtone) {
        mRingtone = ringtone;
        setSummary("\u00A0");
        updateRingtoneName(mRingtone);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            setRingtone(uri);
            callChangeListener(uri);
        }

        return true;
    }

    private void updateRingtoneName(final Uri uri) {
        AsyncTask ringtoneNameTask = new AsyncTask<Object, Void, CharSequence>() {
            @Override
            protected CharSequence doInBackground(Object... params) {
                if (uri == null) {
                    return getContext().getString(R.string.ringtone_silent);//[TCT-ROM][ resource change]Begin modified by junliang.liu for XR6873798 on 20180828
                } else if (RingtoneManager.isDefault(uri)) {
                    return getContext().getString(R.string.notification_sound_default);
                } else if(ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())) {
                    return getContext().getString(R.string.notification_unknown_sound_title);
                } else {
                    //[TCT-ROM][Calendar] Added by nanbing.zou for P23591 on 2018-09-14 begin
                    Uri tempUri = uri;
                    if (!TctRingtoneManagerUtil.isRingtoneExist(getContext(), tempUri)) {
                        tempUri = RingtoneManager.getActualDefaultRingtoneUri(getContext(), getRingtoneType());
                    }
                    return Ringtone.getTitle(getContext(), tempUri, false /* followSettingsUri */,
                            true /* allowRemote */);
                    //[TCT-ROM][Calendar] Added by nanbing.zou for P23591 on 2018-09-14 end
                }
            }

            @Override
            protected void onPostExecute(CharSequence name) {
                setSummary(name);
            }
        };
        ringtoneNameTask.execute();
    }
}
