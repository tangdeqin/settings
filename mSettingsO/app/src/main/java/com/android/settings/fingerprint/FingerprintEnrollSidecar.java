/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.fingerprint;

import android.annotation.Nullable;
import android.app.Activity;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.UserHandle;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.fingerprint.ex.FingerprintUtils;
import com.android.settings.password.ChooseLockSettingsHelper;

import com.android.settings.password.IFingerprintManager;
//Begin added by jinlong.lu for XR6618444 on 18-8-1
import com.tct.sdk.base.fingerprint.FingerprintConstants;
import com.tct.sdk.base.fingerprint.TctFingerprintUtils;
import android.util.Log;
//End added by jinlong.lu for XR6618444 on 18-8-1

import java.util.ArrayList;

/**
 * Sidecar fragment to handle the state around fingerprint enrollment.
 */
public class FingerprintEnrollSidecar extends InstrumentedFragment {

    private int mEnrollmentSteps = -1;
    private int mEnrollmentRemaining = 0;
    private Listener mListener;
    private boolean mEnrolling;
    private CancellationSignal mEnrollmentCancel;
    private Handler mHandler = new Handler();
    private byte[] mToken;
    private boolean mDone;
    private int mUserId;
    private IFingerprintManager mFingerprintManager;
    private ArrayList<QueuedEvent> mQueuedEvents;
    //Begin added by jinlong.lu for XR6618444 on 18-7-31
    private int mEnrollTag;
    private boolean mIsTimeout = false;
    //End added by jinlong.lu for XR6618444 on 18-7-31
    private abstract class QueuedEvent {
        public abstract void send(Listener listener);
    }

    private class QueuedEnrollmentProgress extends QueuedEvent {
        int enrollmentSteps;
        int remaining;
        public QueuedEnrollmentProgress(int enrollmentSteps, int remaining) {
            this.enrollmentSteps = enrollmentSteps;
            this.remaining = remaining;
        }

        @Override
        public void send(Listener listener) {
            listener.onEnrollmentProgressChange(enrollmentSteps, remaining);
        }
    }

    private class QueuedEnrollmentHelp extends QueuedEvent {
        int helpMsgId;
        CharSequence helpString;
        public QueuedEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            this.helpMsgId = helpMsgId;
            this.helpString = helpString;
        }

        @Override
        public void send(Listener listener) {
            listener.onEnrollmentHelp(helpString);
        }
    }

    private class QueuedEnrollmentError extends QueuedEvent {
        int errMsgId;
        CharSequence errString;
        public QueuedEnrollmentError(int errMsgId, CharSequence errString) {
            this.errMsgId = errMsgId;
            this.errString = errString;
        }

        @Override
        public void send(Listener listener) {
            listener.onEnrollmentError(errMsgId, errString);
        }
    }

    public FingerprintEnrollSidecar() {
        mQueuedEvents = new ArrayList<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFingerprintManager = Utils.getFingerprintManagerWrapperOrNull(activity);
        mToken = activity.getIntent().getByteArrayExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        mUserId = activity.getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.USER_NULL);
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        mEnrollTag = activity
                .getIntent()
                .getIntExtra(
                        FingerprintUtils.EXTRA_KEY_ENROLL_FINGERPRINT_TAG, 0);
        //End added by jinlong.lu for XR6618444 on 18-7-31
    }

    @Override
    public void onStart() {
        super.onStart();
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        //add judgment of timeout
        if (!mEnrolling && !mIsTimeout) {
        //End added by jinlong.lu for XR6618444 on 18-7-31
            startEnrollment();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations()) {
            cancelEnrollment();
        }
    }

    private void startEnrollment() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        mEnrollmentSteps = -1;
        mEnrollmentCancel = new CancellationSignal();
        if (mUserId != UserHandle.USER_NULL) {
            mFingerprintManager.setActiveUser(mUserId);
        }
        Log.d("FingerprintEnrollSidecar", "startEnrollment: mToken: " + mToken);
          mFingerprintManager.enroll(mToken, mEnrollmentCancel,
                 0 /* flags */, mUserId, mEnrollmentCallback);
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        TctFingerprintUtils tfu = FingerprintUtils.getTctFingerprintUtils(getContext());
        if (tfu != null) {
            Log.d("FingerprintEnrollSidecar", "startEnrollment: Fingerprint tag: " + mEnrollTag);
            tfu.setTag(mEnrollTag);
        }
        //End added by jinlong.lu for XR6618444 on 18-7-31
        mEnrolling = true;
    }

    boolean cancelEnrollment() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        if (mEnrolling) {
            mEnrollmentCancel.cancel();
            mEnrolling = false;
            mEnrollmentSteps = -1;
            return true;
        }
        return false;
    }

    public void setListener(Listener listener) {
        mListener = listener;
        if (mListener != null) {
            for (int i=0; i<mQueuedEvents.size(); i++) {
                QueuedEvent event = mQueuedEvents.get(i);
                event.send(mListener);
            }
            mQueuedEvents.clear();
        }
    }

    public int getEnrollmentSteps() {
        return mEnrollmentSteps;
    }

    public int getEnrollmentRemaining() {
        return mEnrollmentRemaining;
    }

    public boolean isDone() {
        return mDone;
    }

    private FingerprintManager.EnrollmentCallback mEnrollmentCallback
            = new FingerprintManager.EnrollmentCallback() {

        @Override
        public void onEnrollmentProgress(int remaining) {
            if (mEnrollmentSteps == -1) {
                mEnrollmentSteps = remaining;
            }
            mEnrollmentRemaining = remaining;
            mDone = remaining == 0;
            if (mListener != null) {
                mListener.onEnrollmentProgressChange(mEnrollmentSteps, remaining);
            } else {
                mQueuedEvents.add(new QueuedEnrollmentProgress(mEnrollmentSteps, remaining));
            }
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            if (mListener != null) {
                mListener.onEnrollmentHelp(helpString);
            } else {
                mQueuedEvents.add(new QueuedEnrollmentHelp(helpMsgId, helpString));
            }
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            if (mListener != null) {
                mListener.onEnrollmentError(errMsgId, errString);
            } else {
                mQueuedEvents.add(new QueuedEnrollmentError(errMsgId, errString));
            }
            mEnrolling = false;
            //Begin added by jinlong.lu for XR6618444 on 18-7-31
            if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_TIMEOUT){
                mIsTimeout = true;
            }
            //End added by jinlong.lu for XR6618444 on 18-7-31
        }
    };

    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            cancelEnrollment();
        }
    };

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLL_SIDECAR;
    }

    public interface Listener {
        void onEnrollmentHelp(CharSequence helpString);
        void onEnrollmentError(int errMsgId, CharSequence errString);
        void onEnrollmentProgressChange(int steps, int remaining);
    }

    public boolean isEnrolling() {
        return mEnrolling;
    }
}
