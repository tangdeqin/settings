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

import android.content.Context;
import android.media.AudioManager;
//Added by yingying.qiao.hz for defect10024579 on 2018-10-22 begin
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
//Added by yingying.qiao.hz for defect10024579 on 2018-10-22 end

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.notification.VolumeSeekBarPreference.Callback;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.R; //add by yeqing.lv for XR6873798 un-use frameworks resource on 2018/08/25

public class AlarmVolumePreferenceController extends
    VolumeSeekBarPreferenceController {

    private static final String KEY_ALARM_VOLUME = "alarm_volume";
    private AudioHelper mHelper;
    //Added by yingying.qiao.hz for defect10024579 on 2018-10-22 begin
    private final H mHandler = new H();
    private final AlarmReceiver mReceiver = new AlarmReceiver();
    private AudioManager mAudioManager;
    //Added by yingying.qiao.hz for defect10024579 on 2018-10-22 end

    public AlarmVolumePreferenceController(Context context, Callback callback,
        Lifecycle lifecycle) {
        this(context, callback, lifecycle, new AudioHelper(context));
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);//Added by yingying.qiao.hz for defect10024579 on 2018-10-22
    }

    @VisibleForTesting
    AlarmVolumePreferenceController(Context context, Callback callback, Lifecycle lifecycle,
        AudioHelper helper) {
        super(context, callback, lifecycle);
        mHelper = helper;
    }

    @Override
    public boolean isAvailable() {
        return !mHelper.isSingleVolume();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALARM_VOLUME;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_ALARM;
    }

    @Override
    public int getMuteIcon() {
        // Begin modify by yeqing.lv for XR6873798 un-use frameworks resource on 2018/08/25
        //return com.android.internal.R.drawable.ic_audio_alarm_mute;
        return R.drawable.ic_audio_alarm_mute;
        //End modify by yeqing.lv for XR6873798 un-use frameworks resource on 2018/08/25
    }

    //Added by yingying.qiao.hz for defect10024579 on 2018-10-22 begin
    private void updatePreferenceIcon() {
        int alarmVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        if (mPreference != null) {
            mPreference.showIcon(alarmVolume == 0 ?R.drawable.ic_audio_alarm_mute : R.drawable.ic_audio_alarm);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        mReceiver.register(true);
        updatePreferenceIcon();
    }

    @Override
    public void onPause() {
        super.onPause();
        mReceiver.register(false);
    }

    private final class H extends Handler {
        private static final int ALARM_VOLUME_UPDATE = 1;

        private H() {
            super(Looper.getMainLooper());
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALARM_VOLUME_UPDATE:
                    updatePreferenceIcon();
                    break;
            }
        }
    }
    private  class AlarmReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        public void register(boolean register) {
            if (mRegistered == register) return;
            if (register) {
                final IntentFilter filter = new IntentFilter();
                filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
                mContext.registerReceiver(this, filter);
            } else {
                mContext.unregisterReceiver(this);
            }
            mRegistered = register;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AudioManager.VOLUME_CHANGED_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(H.ALARM_VOLUME_UPDATE);
            }
        }
    }
    //Added by yingying.qiao.hz for defect10024579 on 2018-10-22 end


}
