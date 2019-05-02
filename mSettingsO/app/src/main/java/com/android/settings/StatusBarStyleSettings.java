/******************************************************************************/
/*                                                               Date:09/2017 */
/*                                PRESENTATION                                */
/*                                                                            */
/*       Copyright 2017 TCL Communication Technology Holdings Limited.        */
/*                                                                            */
/* This material is company confidential, cannot be reproduced in any form    */
/* without the written permission of TCL Communication Technology Holdings    */
/* Limited.                                                                   */
/*                                                                            */
/* -------------------------------------------------------------------------- */
/*  Author :  zhibin.zhong                                                    */
/*  Email  :  zhibin.zhong@tcl.com                                            */
/*  Role   :                                                                  */
/*  Reference documents :                                                     */
/* -------------------------------------------------------------------------- */
/*  Comments :                                                                */
/*  File     :                                                                */
/*  Labels   :                                                                */
/* -------------------------------------------------------------------------- */
/* ========================================================================== */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* ----------|----------------------|----------------------|----------------- */
/* 11/09/2018| zhibin.zhong@tcl.com | Add for p23734  notch Ergo Dev.on 2018/09/11      |UE Design list|   */
/*           |                      |                      |Notch    */
/* ----------|----------------------|----------------------|----------------- */
/******************************************************************************/
package com.android.settings;

import android.os.Bundle;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.widget.Switch;
import android.widget.CompoundButton;

import com.android.settings.gestures.GifView;

import android.widget.ImageView;
import android.os.Handler;
import android.os.Message;
import android.content.res.ColorStateList;
import android.widget.ViewFlipper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.content.pm.ActivityInfo;
import android.widget.LinearLayout;//zhibin.zhong add for XR  6331663  , Navigationbar Ergo Dev.on 2018/05/18 

public class StatusBarStyleSettings extends SettingsPreferenceFragment {

    private static final String TAG = "StatusBarStyleSettings";
    private int mCurrentUser;
    private ContentResolver mContentResolver;
    private RadioButton mNormalButton;
    private RadioButton mHideButton;
    private static final String TCT_NOTCH_HIDE_ENABLED = "notch_hide_enabled";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        mCurrentUser = ActivityManager.getCurrentUser();
        mContentResolver = getActivity().getContentResolver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        final ViewGroup listContainer = (ViewGroup) root.findViewById(android.R.id.list_container);
        listContainer.removeAllViews();
        final View content = inflater.inflate(R.layout.status_bar_style, listContainer, false);
        listContainer.addView(content);
        mNormalButton = (RadioButton) content.findViewById(R.id.status_bar_normal_btn);
        mHideButton = (RadioButton) content.findViewById(R.id.status_bar_hide_btn);

        boolean isEnabled = Settings.System.getInt(mContentResolver, TCT_NOTCH_HIDE_ENABLED, 0) == 1;

        mHideButton.setChecked(isEnabled);
        mHideButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.System.putInt(mContentResolver, TCT_NOTCH_HIDE_ENABLED, isChecked ? 1 : 0);
                mHideButton.setChecked(isChecked);
                mNormalButton.setChecked(!isChecked);
            }
        });
        mNormalButton.setChecked(!isEnabled);
        mNormalButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.System.putInt(mContentResolver, TCT_NOTCH_HIDE_ENABLED, isChecked ? 0 : 1);
                mNormalButton.setChecked(isChecked);
                mHideButton.setChecked(!isChecked);
            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }


    @Override
    public int getMetricsCategory() {
        return 1;//MetricsEvent.NAVIGATION_DISPLAY_SETTINGS;
    }

}
