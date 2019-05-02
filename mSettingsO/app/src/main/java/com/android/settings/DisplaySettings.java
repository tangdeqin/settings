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

package com.android.settings;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.AmbientDisplayPreferenceController;
import com.android.settings.display.AutoBrightnessPreferenceController;
import com.android.settings.display.AutoRotatePreferenceController;
import com.android.settings.display.BrightnessLevelPreferenceController;
import com.android.settings.display.CameraGesturePreferenceController;
import com.android.settings.display.ColorModePreferenceController;
import com.android.settings.display.FontSizePreferenceController;
import com.android.settings.display.LiftToWakePreferenceController;
import com.android.settings.display.NightDisplayPreferenceController;
import com.android.settings.display.NightModePreferenceController;
import com.android.settings.display.ScreenSaverPreferenceController;
import com.android.settings.display.ShowNotificationNumberPreferenceController;
import com.android.settings.display.TapToWakePreferenceController;
import com.android.settings.display.ThemePreferenceController;
import com.android.settings.display.TimeoutPreferenceController;
import com.android.settings.display.VrDisplayPreferenceController;
import com.android.settings.display.WallpaperPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
//begin  zhixiong.liu.hz for XR 6107743 2018/3/14
import com.android.settings.display.LedPreferenceController;
//end  zhixiong.liu.hz for XR 6107743 2018/3/14

import com.android.settings.display.ReadingModePreferenceController;//Add by shilei.zhang for fixed XR7052220 on 2018/10/21
import java.util.ArrayList;
import java.util.List;

//Begin added by miaoliu for XRP23297 on 2018/9/6
import com.android.settings.search.SearchIndexableRaw;
import android.content.res.Resources;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settingslib.drawer.Tile;
//End added by miaoliu for XRP23297 on 2018/9/6

public class DisplaySettings extends DashboardFragment {
    private static final String TAG = "DisplaySettings";

    public static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    public static final String KEY_DISPLAY_SIZE = "screen_zoom";

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_AMBIENT_DISPLAY = "ambient_display";
    //begin  zhixiong.liu.hz for XR 6107743 2018/3/14
    private static final String KEY_LED_INDICTOR = "led_indictor";
    //end  zhixiong.liu.hz for XR 6107743 2018/3/14
    //Begin add by shilei.zhang for fixed XR7052220 on 2018/10/21
    private static final String KEY_READING_MODE = "reading_mode";
    //End add by shilei.zhang for fixed XR7052220 on 2018/10/21
    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DISPLAY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProgressiveDisclosureMixin.setTileLimit(4);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.display_settings;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        //Begin modified by zhibin.zhong for defect 5787238 on 2018/03/28
        controllers.add(new AutoBrightnessPreferenceController(context, KEY_AUTO_BRIGHTNESS,lifecycle));
        //End modified by zhibin.zhong for defect 5787238 on 2018/03/28
        //begin add by wenli for XR 6622604 statusbar ergo development on 20180830
        //controllers.add(new ShowNotificationNumberPreferenceController(context,lifecycle));//delete by wenli for P23398 on 20180904
        //end add by wenli for XR 6622604 statusbar ergo development on 20180830
        controllers.add(new AutoRotatePreferenceController(context, lifecycle));
        controllers.add(new CameraGesturePreferenceController(context));
        controllers.add(new FontSizePreferenceController(context));
        controllers.add(new LiftToWakePreferenceController(context));
        controllers.add(new NightDisplayPreferenceController(context));
        controllers.add(new NightModePreferenceController(context));
        controllers.add(new ScreenSaverPreferenceController(context));
        controllers.add(new AmbientDisplayPreferenceController(
                context,
                new AmbientDisplayConfiguration(context),
                KEY_AMBIENT_DISPLAY));
        controllers.add(new TapToWakePreferenceController(context));
        controllers.add(new TimeoutPreferenceController(context, KEY_SCREEN_TIMEOUT));
        controllers.add(new VrDisplayPreferenceController(context));
        controllers.add(new WallpaperPreferenceController(context));
        controllers.add(new ThemePreferenceController(context));
        controllers.add(new BrightnessLevelPreferenceController(context, lifecycle));
        controllers.add(new ColorModePreferenceController(context));
        //begin  zhixiong.liu.hz for XR 6107743 2018/3/14
        controllers.add(new LedPreferenceController(context,KEY_LED_INDICTOR));
        //end  zhixiong.liu.hz for XR 6107743 2018/3/14
        //Begin add by shilei.zhang for fixed XR7052220 on 2018/10/21
        controllers.add(new ReadingModePreferenceController(context,KEY_READING_MODE));
        //End add by shilei.zhang for fixed XR7052220 on 2018/10/21
        return controllers;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    //begin zhixiong.liu.hz for XR 6107743 2018/3/14
                    //keys.add(KEY_DISPLAY_SIZE);
                    //keys.add(WallpaperPreferenceController.KEY_WALLPAPER);
                    keys.add(KEY_AMBIENT_DISPLAY);//Added by miaoliu for XRP24381 on 2018/9/28
                    //end zhixiong.liu.hz for XR 6107743 2018/3/14
                    return keys;
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context, null);
                }
                //Begin  added by miaoliu for XRP23297 on 2018/9/6
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                    final Resources res = context.getResources();

                    final String screenTitle = res.getString(R.string.display_settings);

                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = screenTitle;
                    data.screenTitle = screenTitle;
                    result.add(data);

                   DashboardFeatureProvider dashboardFeatureProvider = FeatureFactory.getFactory(context).getDashboardFeatureProvider(context);
                   final DashboardCategory category =
                        dashboardFeatureProvider.getTilesForCategory(CategoryKey.CATEGORY_DISPLAY);
                     if (category == null) {
                          return result;
                     }
                     List<Tile> tiles = category.tiles;
                    if (tiles == null) {
                        return result;
                     }
                     for (Tile tile : tiles) {
                         data = new SearchIndexableRaw(context);
                         data.title = tile.title.toString();
                         data.screenTitle = screenTitle;
                         data.intentAction = "com.android.settings.action.EXTRA_SETTINGS";
                         data.intentTargetPackage = tile.intent.getComponent().getPackageName();
                         data.intentTargetClass = tile.intent.getComponent().getClassName();
                         result.add(data);
                     }

                    return result;
                }
                //End added by miaoliu for XRP23297 on 2018/9/6
      
            };
}
