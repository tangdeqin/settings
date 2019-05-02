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
/* 22/03/2018| zhibin.zhong@tcl.com |       XR6137780, Navigationbar Ergo Dev.on 2018/03/21        |UE Design list|   */
/*           |                      |                      |Navigation bar    */
/* ----------|----------------------|----------------------|----------------- */
/******************************************************************************/
package com.android.settings.navigationbar;

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

public class NavigationbarSettings extends SettingsPreferenceFragment {

    private static final String TAG = "NavigationbarSettings";
    public static final String NAV_BAR_VIEWS = "sysui_nav_bar";
    public static final String NAV_BAR_COLOR = "nav_bar_color";
    private int mCurrentUser;
    private ContentResolver mContentResolver;
    private final static String NAV_HIDE = "nav_hide";
    static private final int DEFAULT_LAYOUT_INDEX = 0;
    private static final int[] BUTTONG_KEYS = {R.id.back_home_recent,
            R.id.recent_home_back,
            R.id.pin_back_home_recent,
            R.id.pin_recent_home_back,
            R.id.back_home_recent_pin,
            R.id.recent_home_back_pin
    };
    private RadioButton[] mNavigationbarButtons;
    private String[] mSysuiNavBars;
    private RadioButton mGeneralButton;
    private RadioButton mGestureButton;
    private int mIndex = MSG_UPDATE_BACK_KEY_INFO;
    private ViewGroup mGestureNavBarIntroduce;
    private ViewFlipper mViewFlipper;
    private GestureDetector mGestureDetector;
    private ViewGroup mGestureNavBarGifTip;
    private ViewGroup mNavBarIconLayout;
    private ViewGroup mNavBarBgLayout;
    private ViewGroup mNavBarDemoTips;
    //begin-zhibin.zhong add for XR  6331663  , Navigationbar Ergo Dev.on 2018/05/18
    private ViewGroup mGeneralStyle;
    private ViewGroup mGestureStyle;
    //end-zhibin.zhong add for XR  6331663  , Navigationbar Ergo Dev.on 2018/05/18
    private TextView mKeyIntroduce;
    private TextView mKeyName;
    private ImageView mPageInd1;
    private ImageView mPageInd2;
    private ImageView mPageInd3;
    private ColorSelectView[] mColorSelectViews;
    private int[] mColorSelectViewIds = {R.id.nav_color_1, R.id.nav_color_2, R.id.nav_color_3, R.id.nav_color_4, R.id.nav_color_5,
            R.id.nav_color_6, R.id.nav_color_7, R.id.nav_color_8, R.id.nav_color_9, R.id.nav_color_10};
    //static private final int[] mSelectViewColors = { 0xfff3f3f3, 0xff454545, 0xfffae5d7, 0xfffafaca, 0xffd7f7da,
    //        0xffd0f9f4, 0xffd3e0f5, 0xfff0e4f7, 0xfffcddf2, 0xfff9e1e1};
    //begin-jianorng.yan modify for P10024234.
    //begin-zhibin.zhong modify for XR  6331663  , Navigationbar Ergo Dev.on 2018/05/18 
    static private final int[] mSelectViewColors = {0xff1f1f1f, 0xfff3f3f3, 0xfffae5d7, 0xfffafaca, 0xffd7f7da,
            0xffd0f9f4, 0xffd3e0f5, 0xfff0e4f7, 0xfffcddf2, 0xfff9e1e1};
    //end-zhibin.zhong modify for XR  6331663  , Navigationbar Ergo Dev.on 2018/05/18
    //end-jianorng.yan modify for P10024234.
    static private final int DEFAULT_COLOR_INDEX = 0;
    private TextView[] mColorTextView;
    private int[] mColorTextViewIds = {R.id.nav_color_tv_1, R.id.nav_color_tv_2,
            R.id.nav_color_tv_3, R.id.nav_color_tv_4, R.id.nav_color_tv_5,
            R.id.nav_color_tv_6, R.id.nav_color_tv_7, R.id.nav_color_tv_8,
            R.id.nav_color_tv_9, R.id.nav_color_tv_10};
    static private final int COLOR_TEXT = 0xff000000;
    static private final int COLOR_TEXT_SELECT = 0xff1bb0f4;
    private static final String TCT_GESTURE_NAV_BAR_ENABLED = "navigation_bar_gesture_enabled";//Settings.Global.TCT_GESTURE_NAV_BAR_ENABLED
    private static final String TCT_NAV_COLOR = "tct_nav_color";//Settings.System.TCT_NAV_COLOR

    static private final int MSG_UPDATE_BACK_KEY_INFO = 0;
    static private final int MSG_UPDATE_HOME_KEY_INFO = 1;
    static private final int MSG_UPDATE_RECENTS_KEY_INFO = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        mCurrentUser = ActivityManager.getCurrentUser();
        mContentResolver = getActivity().getContentResolver();
        mSysuiNavBars = getActivity().getResources().getStringArray(R.array.sysui_nav_bar_entries);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        final ViewGroup listContainer = (ViewGroup) root.findViewById(android.R.id.list_container);
        listContainer.removeAllViews();
        final View content = inflater.inflate(R.layout.navigation_bar_mode, listContainer, false);
        listContainer.addView(content);

        int selectIndex = -1;
        String oldStr = Settings.Secure.getStringForUser(mContentResolver, NAV_BAR_VIEWS, mCurrentUser);
        Log.d(TAG, "selectIndex=" + selectIndex + ",oldStr=" + oldStr);
        if (!TextUtils.isEmpty(oldStr)) {
            for (int i = 0; i < mSysuiNavBars.length; i++) {
                if (oldStr.equals(mSysuiNavBars[i])) {
                    selectIndex = i;
                    Log.d(TAG, "selectIndex=" + selectIndex);
                    break;
                }
            }
        }
        if (selectIndex < 0) {
            selectIndex = DEFAULT_LAYOUT_INDEX;
            Settings.Secure.putStringForUser(mContentResolver, NAV_BAR_VIEWS, mSysuiNavBars[selectIndex], mCurrentUser);
            Log.d(TAG, "selectIndex=" + selectIndex + ",str=" + mSysuiNavBars[selectIndex]);
        }
        mNavigationbarButtons = new RadioButton[BUTTONG_KEYS.length];
        for (int i = 0; i < BUTTONG_KEYS.length; i++) {
            mNavigationbarButtons[i] = (RadioButton) content.findViewById(BUTTONG_KEYS[i]);
            mNavigationbarButtons[i].setChecked(i == selectIndex ? true : false);
            mNavigationbarButtons[i].setOnCheckedChangeListener(mRadioListener);
        }

        int selectColor = Settings.System.getIntForUser(mContentResolver, TCT_NAV_COLOR, mSelectViewColors[DEFAULT_COLOR_INDEX], mCurrentUser);
        mColorSelectViews = new ColorSelectView[mColorSelectViewIds.length];
        mColorTextView = new TextView[mColorTextViewIds.length];
        for (int i = 0; i < mColorSelectViewIds.length; i++) {
            mColorSelectViews[i] = (ColorSelectView) content.findViewById(mColorSelectViewIds[i]);
            //begin-zhibin.zhong modify for Defect 7140299 on 2018.12.05
            mColorSelectViews[i].setColor(i == 0 ? 0x00000000 : mSelectViewColors[i]);
            //end-zhibin.zhong modify for Defect 7140299 on 2018.12.05
            mColorTextView[i] = (TextView) content.findViewById(mColorTextViewIds[i]);
            if (selectColor == mSelectViewColors[i]) {
                mColorSelectViews[i].setColorSelected(true);
                mColorTextView[i].setTextColor(COLOR_TEXT_SELECT);
            } else {
                mColorSelectViews[i].setColorSelected(false);
                mColorTextView[i].setTextColor(COLOR_TEXT);
            }
            mColorSelectViews[i].setIndex(i);
            mColorSelectViews[i].setOnClickListener(mColorClickListener);
        }
        mGestureButton = (RadioButton) content.findViewById(R.id.gesture_nav_bar);
        mGeneralButton = (RadioButton) content.findViewById(R.id.general_nav_bar);

        boolean isEnabled = Settings.Global.getInt(mContentResolver, TCT_GESTURE_NAV_BAR_ENABLED, 0) == 1;
        mGestureButton.setChecked(isEnabled);
        mGestureButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.Global.putInt(mContentResolver, TCT_GESTURE_NAV_BAR_ENABLED, isChecked ? 1 : 0);
                mGestureButton.setChecked(isChecked);
                mGeneralButton.setChecked(!isChecked);
                setNavBarGeneralSettingsEnabled();

                boolean isEnabled = Settings.Global.getInt(mContentResolver, TCT_GESTURE_NAV_BAR_ENABLED, 0) == 1;
                if (isEnabled) {
                    refreshKeysIntroduceInfo(MSG_UPDATE_BACK_KEY_INFO);
                }
            }
        });
        mGeneralButton.setChecked(!isEnabled);
        mGeneralButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.Global.putInt(mContentResolver, TCT_GESTURE_NAV_BAR_ENABLED, isChecked ? 0 : 1);
                mGeneralButton.setChecked(isChecked);
                mGestureButton.setChecked(!isChecked);
                setNavBarGeneralSettingsEnabled();
            }
        });
        mNavBarIconLayout = (ViewGroup) content.findViewById(R.id.nav_bar_icon_layout);
        mNavBarBgLayout = (ViewGroup) content.findViewById(R.id.nav_bar_bg);
        mGestureNavBarIntroduce = (ViewGroup) content.findViewById(R.id.gesture_nav_bar_introduce);
        mNavBarDemoTips = (ViewGroup) content.findViewById(R.id.gesture_nav_bar_gif_tip);
        //begin-zhibin.zhong add for XR  6331663  , Navigationbar Ergo Dev.on 2018/05/18
        mGestureStyle = (ViewGroup) content.findViewById(R.id.nav_bar_gesture_style);
        mGeneralStyle = (ViewGroup) content.findViewById(R.id.nav_bar_general_style);
        //end-zhibin.zhong add for XR  6331663  , Navigationbar Ergo Dev.on 2018/05/18
        mViewFlipper = (ViewFlipper) content.findViewById(R.id.gesture_nav_bar_flipper);
        GifView backGif = new GifView(getActivity());
        backGif.setMovieResource(R.raw.navigation_bar_back_tip);
        mViewFlipper.addView(backGif);

        GifView homeGif = new GifView(getActivity());
        homeGif.setMovieResource(R.raw.navigation_bar_home_tip);
        mViewFlipper.addView(homeGif);

        GifView recentsGif = new GifView(getActivity());
        recentsGif.setMovieResource(R.raw.navigation_bar_recents_tip);
        mViewFlipper.addView(recentsGif);
        mGestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getX() - e2.getX() > 90) {
                    mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.push_left_in));
                    mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.push_left_out));
                    mViewFlipper.showNext();
                    mIndex = mViewFlipper.getDisplayedChild();
                    refreshKeysIntroduceInfo(mIndex);
                    return true;
                } else if (e1.getX() - e2.getX() < -90) {
                    mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.push_right_in));
                    mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.push_right_out));
                    mViewFlipper.showPrevious();
                    mIndex = mViewFlipper.getDisplayedChild();
                    refreshKeysIntroduceInfo(mIndex);
                    return true;
                }
                return true;
            }
        });
        mNavBarDemoTips.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return NavigationbarSettings.this.mGestureDetector.onTouchEvent(event);
            }
        });

        mKeyIntroduce = (TextView) content.findViewById(R.id.introduce_summery);
        mKeyName = (TextView) content.findViewById(R.id.introduce_key_name);

        mPageInd1 = (ImageView) content.findViewById(R.id.page_indicator1);
        mPageInd2 = (ImageView) content.findViewById(R.id.page_indicator2);
        mPageInd3 = (ImageView) content.findViewById(R.id.page_indicator3);

        setNavBarGeneralSettingsEnabled();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //begin-zhibin.zhong delete for p23720.on 2018/09/11
/*        //begin-zhibin.zhong add for XR  6331663  , Navigationbar Ergo Dev.on 2018/05/18
        try {
            int densityDpi = getActivity().getResources().getConfiguration().densityDpi;
            //android.util.Log.d(TAG,"densityDpi="+densityDpi);
            LinearLayout.LayoutParams generalStyleParams = (LinearLayout.LayoutParams) mGeneralStyle.getLayoutParams();
            LinearLayout.LayoutParams gestureStyleParams = (LinearLayout.LayoutParams) mGestureStyle.getLayoutParams();
            if (densityDpi == 272) {
                generalStyleParams.width = 610;
                gestureStyleParams.width = 610;
            } else if (densityDpi == 320) {
                generalStyleParams.width = 590;
                gestureStyleParams.width = 590;
            } else if (densityDpi == 360) {
                generalStyleParams.width = 570;
                gestureStyleParams.width = 570;
            }
            mGeneralStyle.setLayoutParams(generalStyleParams);
            mGestureStyle.setLayoutParams(gestureStyleParams);
        } catch (Exception e) {
        }
        //end-zhibin.zhong add for XR  6331663  , Navigationbar Ergo Dev.on 2018/05/18*/
        //end-zhibin.zhong modify for p23720.on 2018/09/11
        setNavBarGeneralSettingsEnabled();
        boolean isEnabled = Settings.Global.getInt(mContentResolver, TCT_GESTURE_NAV_BAR_ENABLED, 0) == 1;
        if (isEnabled) {
            refreshKeysIntroduceInfo(mIndex);
        }
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

    private void refreshKeysIntroduceInfo(int index) {
        boolean isEnabled = Settings.Global.getInt(mContentResolver, TCT_GESTURE_NAV_BAR_ENABLED, 0) == 1;
        if (!isEnabled) return;
        Message message = new Message();
        mIndex = index;
        Log.d(TAG, "mIndex=" + mIndex);
        switch (index) {
            case MSG_UPDATE_HOME_KEY_INFO:
                mKeyIntroduce.setText(R.string.nav_bar_gesture_home_key_introduce);
                mKeyName.setText(R.string.nav_bar_gesture_home_key);
                mPageInd1.setImageTintList(ColorStateList.valueOf(getActivity().getColor(R.color.page_ind_unselect_color)));
                mPageInd2.setImageTintList(ColorStateList.valueOf(getActivity().getColor(R.color.page_ind_select_color)));
                mPageInd3.setImageTintList(ColorStateList.valueOf(getActivity().getColor(R.color.page_ind_unselect_color)));
                break;

            case MSG_UPDATE_BACK_KEY_INFO:
                mKeyIntroduce.setText(R.string.nav_bar_gesture_back_key_introduce);
                mKeyName.setText(R.string.nav_bar_gesture_back_key);
                mPageInd1.setImageTintList(ColorStateList.valueOf(getActivity().getColor(R.color.page_ind_select_color)));
                mPageInd2.setImageTintList(ColorStateList.valueOf(getActivity().getColor(R.color.page_ind_unselect_color)));
                mPageInd3.setImageTintList(ColorStateList.valueOf(getActivity().getColor(R.color.page_ind_unselect_color)));
                break;
            case MSG_UPDATE_RECENTS_KEY_INFO:
                mKeyIntroduce.setText(R.string.nav_bar_gesture_recent_key_introduce);
                mKeyName.setText(R.string.nav_bar_gesture_recents_key);
                mPageInd1.setImageTintList(ColorStateList.valueOf(getActivity().getColor(R.color.page_ind_unselect_color)));
                mPageInd2.setImageTintList(ColorStateList.valueOf(getActivity().getColor(R.color.page_ind_unselect_color)));
                mPageInd3.setImageTintList(ColorStateList.valueOf(getActivity().getColor(R.color.page_ind_select_color)));
                break;
            default:
                break;
        }
    }

    private void setNavBarGeneralSettingsEnabled() {
        boolean isEnabled = Settings.Global.getInt(mContentResolver, TCT_GESTURE_NAV_BAR_ENABLED, 0) == 1;
      /*for(int i=0; i<BUTTONG_KEYS.length; i++) {
	    if(mNavigationbarButtons[i] != null){
	       mNavigationbarButtons[i].setEnabled(isEnabled ? false : true);
	    }
       }
      for (int i = 0; i < mColorSelectViewIds.length; i++) {
	  if(mColorSelectViews[i] != null){
             mColorSelectViews[i].setEnabled(isEnabled ? false : true);
	   }
	}*/
        mGestureButton.setChecked(isEnabled);
        mGeneralButton.setChecked(!isEnabled);
        if (mGestureNavBarIntroduce != null) {
            mGestureNavBarIntroduce.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        }
        if (mNavBarIconLayout != null) {
            mNavBarIconLayout.setVisibility(isEnabled ? View.GONE : View.VISIBLE);
        }
        if (mNavBarBgLayout != null) {
            mNavBarBgLayout.setVisibility(isEnabled ? View.GONE : View.VISIBLE);
        }
    }

    private final OnCheckedChangeListener mRadioListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                for (int i = 0; i < mNavigationbarButtons.length; i++) {
                    if (buttonView == mNavigationbarButtons[i]) {
                        Settings.Secure.putStringForUser(mContentResolver, NAV_BAR_VIEWS, mSysuiNavBars[i], mCurrentUser);
                        Log.d(TAG, "i=" + i + ",str=" + mSysuiNavBars[i]);
                    } else {
                        mNavigationbarButtons[i].setChecked(false);
                    }
                }
            }
        }
    };

    private final View.OnClickListener mColorClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = ((ColorSelectView) v).getIndex();
            for (int i = 0; i < mColorSelectViewIds.length; i++) {
                if (i == index) {
                    mColorSelectViews[i].setColorSelected(true);
                    mColorTextView[i].setTextColor(COLOR_TEXT_SELECT);
                } else {
                    mColorSelectViews[i].setColorSelected(false);
                    mColorTextView[i].setTextColor(COLOR_TEXT);
                }
            }
            Log.d(TAG, "TCT_NAV_COLOR click put index=" + index);
            Settings.System.putIntForUser(mContentResolver, TCT_NAV_COLOR, mSelectViewColors[index], mCurrentUser);
        }
    };

    @Override
    public int getMetricsCategory() {
        return 1;//MetricsEvent.NAVIGATION_DISPLAY_SETTINGS;
    }

}
