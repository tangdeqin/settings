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

package com.android.settings.gestures;

import android.content.Context;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//Begin added by miaoliu for Settings ergo 2.0.1 on 2018/7/19
import android.content.SharedPreferences;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
//End added by miaoliu for Settings ergo 2.0.1 on 2018/7/19
public class GestureSettings extends DashboardFragment {

    private static final String TAG = "GestureSettings";

    private static final String KEY_ASSIST = "gesture_assist_input_summary";
    private static final String KEY_SWIPE_DOWN = "gesture_swipe_down_fingerprint_input_summary";
    private static final String KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power_input_summary";
    private static final String KEY_DOUBLE_TWIST = "gesture_double_twist_input_summary";
    private static final String KEY_DOUBLE_TAP_SCREEN = "gesture_double_tap_screen_input_summary";
    private static final String KEY_PICK_UP = "gesture_pick_up_input_summary";

    

    private AmbientDisplayConfiguration mAmbientDisplayConfig;

    //Begin added by miaoliu for Settings ergo 2.0.1 on 2018/7/19
    public static final String PREF_KEY_SUGGESTION_COMPLETE =
            "pref_double_tap_power_suggestion_complete";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        SuggestionFeatureProvider suggestionFeatureProvider = FeatureFactory.getFactory(context)
                .getSuggestionFeatureProvider(context);
        SharedPreferences prefs = suggestionFeatureProvider.getSharedPrefs(context);
        prefs.edit().putBoolean(PREF_KEY_SUGGESTION_COMPLETE, true).apply();
    }
    //End added by miaoliu for Settings ergo 2.0.1 on 2018/7/19
    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_GESTURES;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.gestures;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        if (mAmbientDisplayConfig == null) {
            mAmbientDisplayConfig = new AmbientDisplayConfiguration(context);
        }

        return buildPreferenceControllers(context, getLifecycle(), mAmbientDisplayConfig);
    }

    static List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context, @Nullable Lifecycle lifecycle,
            @NonNull AmbientDisplayConfiguration ambientDisplayConfiguration) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new AssistGestureSettingsPreferenceController(context, lifecycle,
                KEY_ASSIST, false /* assistOnly */));
        controllers.add(new SwipeToNotificationPreferenceController(context, lifecycle,
                KEY_SWIPE_DOWN));
        controllers.add(new DoubleTwistPreferenceController(context, lifecycle, KEY_DOUBLE_TWIST));
        controllers.add(new DoubleTapPowerPreferenceController(context, lifecycle,
                KEY_DOUBLE_TAP_POWER));
        controllers.add(new PickupGesturePreferenceController(context, lifecycle,
                ambientDisplayConfiguration, UserHandle.myUserId(), KEY_PICK_UP));
        controllers.add(new DoubleTapScreenPreferenceController(context, lifecycle,
                ambientDisplayConfiguration, UserHandle.myUserId(), KEY_DOUBLE_TAP_SCREEN));
        //Begin add by yimin.guo for XR6709056 on 8/6/18
        controllers.add(new ThreeFingerPreferenceController(context));
        //End add by yimin.guo for XR6709056 on 8/6/18
        return controllers;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.gestures;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null,
                            new AmbientDisplayConfiguration(context));
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    // Duplicates in summary and details pages.
                    keys.add(KEY_ASSIST);
                    keys.add(KEY_SWIPE_DOWN);
                    //keys.add(KEY_DOUBLE_TAP_POWER);//Deleted by miaoliu for Settings ergo 2.0.1 on 2018/7/19
                    keys.add(KEY_DOUBLE_TWIST);
                    keys.add(KEY_DOUBLE_TAP_SCREEN);
                    keys.add(KEY_PICK_UP);

                    return keys;
                }
            };
}
