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

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;
import com.android.settings.notification.VolumeSeekBarPreference.Callback;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.R;//add by yeqing.lv for XR6873798 un-use frameworks resource on 2018/08/25

//Added by nanbing.zou for Task 6628151 on 2018/07/30 begin
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioSystem;//Added by ronghui.yi for P23285 on 2018/09/22
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.Vibrator;
import com.tct.sdk.base.media.TctAudioManagerUtil;//Added by ronghui.yi for P23285 on 2018/09/22

import java.util.Objects;
//Added by nanbing.zou for Task 6628151 on 2018/07/30 end



public class NotificationVolumePreferenceController extends
        RingVolumePreferenceController {

    private static final String KEY_NOTIFICATION_VOLUME = "notification_volume";
    private AudioHelper mHelper;

    //Added by nanbing.zou for Task 6628151 on 2018/07/30 begin
    private AudioManager mAudioManager;
    private Vibrator mVibrator;
    private int mRingerMode = -1;
    private ComponentName mSuppressor;
    private final RingReceiver mReceiver = new RingReceiver();
    private final H mHandler = new H();
    private boolean isSeparateed = SystemProperties.getBoolean("ro.separate_ring_noti_volume", true);
    //Added by nanbing.zou for Task 6628151 on 2018/07/30 end
    private boolean notificationMuteFlag = false;//Added by ronghui.yi for P23285 on 2018/09/22

    public NotificationVolumePreferenceController(Context context, Callback callback,
                                                  Lifecycle lifecycle) {
        this(context, callback, lifecycle, new AudioHelper(context));
    }

    @VisibleForTesting
    NotificationVolumePreferenceController(Context context,
                                           Callback callback, Lifecycle lifecycle, AudioHelper helper) {
        super(context, callback, lifecycle);
        mHelper = helper;
        //Added by nanbing.zou for Task 6628151 on 2018/07/30 begin
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
        judgeNotificationMuteMode();//Added by ronghui.yi for P23285 on 2018/09/22
        updateRingerMode();
        //Added by nanbing.zou for Task 6628151 on 2018/07/30 end

    }


    @Override
    public boolean isAvailable() {
        //Modified by nanbing.zou for Task 6628151 on 2018/07/30 begin
        if(isSeparateed)
        {
            return true;
        }
        else
        {
            return false;
        }
        //Modified by nanbing.zou for Task 6628151 on 2018/07/30 end
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NOTIFICATION_VOLUME;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_NOTIFICATION;
    }

    @Override
    public int getMuteIcon() {
        return R.drawable.ic_audio_ring_notif_mute;//modify by yeqing.lv for XR6873798 un-use frameworks resource on 2018/08/25
    }


    //Added by nanbing.zou for Task 6628151 on 2018/07/30 begin
    private void updateRingerMode() {
        final int ringerMode = mAudioManager.getRingerModeInternal();
        if (mRingerMode == ringerMode) return;
        mRingerMode = ringerMode;
        updatePreferenceIcon();
    }

    private void updateEffectsSuppressor() {
        final ComponentName suppressor = NotificationManager.from(mContext).getEffectsSuppressor();
        if (Objects.equals(suppressor, mSuppressor)) return;
        mSuppressor = suppressor;
        if (mPreference != null) {
            final String text = SuppressorHelper.getSuppressionText(mContext, suppressor);
            mPreference.setSuppressionText(text);
        }
        updatePreferenceIcon();
    }
    private void updatePreferenceIcon() {
        if (mPreference != null) {
            //Begin added by ronghui.yi for P23285 on 2018/09/22
            mPreference.showIcon(mRingerMode == AudioManager.RINGER_MODE_VIBRATE
                    ? R.drawable.ic_audio_ring_notif_vibrate
                    : (mSuppressor != null || mRingerMode == AudioManager.RINGER_MODE_SILENT||notificationMuteFlag)
                    ? R.drawable.ic_audio_notification_mute
                    : R.drawable.ic_audio_notification);
            notificationMuteFlag = false ;
            //End added by ronghui.yi for P23285 on 2018/09/22
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        mReceiver.register(true);
        judgeNotificationMuteMode();//Added by ronghui.yi for P23285 on 2018/09/22
        updateEffectsSuppressor();
        updatePreferenceIcon();
    }
    @Override
    public void onPause() {
        super.onPause();
        mReceiver.register(false);
    }
    private final class H extends Handler {
        private static final int UPDATE_EFFECTS_SUPPRESSOR = 1;
        private static final int UPDATE_RINGER_MODE = 2;
        private static final int UPDATE_NOTIFICATION_MODE = 3;//Added by ronghui.yi for P23285 on 2018/09/22


        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_EFFECTS_SUPPRESSOR:
                    updateEffectsSuppressor();
                    break;
                case UPDATE_RINGER_MODE:
                    updateRingerMode();
                    break;
                //Begin added by ronghui.yi for P23285 on 2018/09/22
                case UPDATE_NOTIFICATION_MODE:
                    updatePreferenceIcon();
                    break;
                //End added by ronghui.yi for P23285 on 2018/09/22
            }
        }
    }

    private class RingReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        public void register(boolean register) {
            if (mRegistered == register) return;
            if (register) {
                final IntentFilter filter = new IntentFilter();
                filter.addAction(NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED);
                filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                filter.addAction(TctAudioManagerUtil.INTERNAL_NOTIFICATION_MODE_CHANGED_ACTION);//Added by ronghui.yi for P23285 on 2018/09/22
                mContext.registerReceiver(this, filter);
            } else {
                mContext.unregisterReceiver(this);
            }
            mRegistered = register;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED.equals(action)) {
                mHandler.sendEmptyMessage(H.UPDATE_EFFECTS_SUPPRESSOR);
            } else if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(H.UPDATE_RINGER_MODE);
                //Begin added by ronghui.yi for P23285 on 2018/09/22
            } else if (TctAudioManagerUtil.INTERNAL_NOTIFICATION_MODE_CHANGED_ACTION.equals(action)){
                notificationMuteFlag = intent.getBooleanExtra("INTERNAL_NOTIFICATION_CHANGED_MODE_FLAG",false);
                mHandler.sendEmptyMessage(H.UPDATE_NOTIFICATION_MODE);
            }   //End added by ronghui.yi for P23285 on 2018/09/22
        }
    }
    //Added by nanbing.zou for Task 6628151 on 2018/07/30 end

    //Begin added by ronghui.yi for P23285 on 2018/09/22
    private void judgeNotificationMuteMode() {
        int notificationVolume = mAudioManager.getLastAudibleStreamVolume(AudioSystem.STREAM_NOTIFICATION);
        final int ringerModeState = mAudioManager.getRingerModeInternal();
        if (notificationVolume == 0 && ringerModeState != AudioManager.RINGER_MODE_VIBRATE ){
            notificationMuteFlag = true ;
        }
    }
    //End added by ronghui.yi for P23285 on 2018/09/22
}