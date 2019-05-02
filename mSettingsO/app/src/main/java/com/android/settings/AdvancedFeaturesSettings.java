
package com.android.settings;

import android.content.Context;
import android.os.Bundle;
import com.android.settings.search.BaseSearchIndexProvider;
import java.util.ArrayList;
import java.util.List;

import android.provider.SearchIndexableResource;
import com.android.settings.gestures.GesturesSettingPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import android.app.Activity;
import com.android.settings.R;
/*
  Added by miaoliu for XR5429846 on 2017/10/25
*/
import android.os.SystemProperties;
//Begin added by miaoliu for XRP23297 on 2018/9/6
import com.android.settings.search.SearchIndexableRaw;
import android.content.ComponentName;
import android.content.res.Resources;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settingslib.drawer.Tile;
//End added by miaoliu for XRP23297 on 2018/9/6
public class AdvancedFeaturesSettings extends DashboardFragment{  

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //added by zengjie for XR6619992 on 20180727 begin
        //addPreferencesFromResource(R.xml.func_settings);//Deleted by miaoliu for XRP23119 on 2018/9/1
        //added by zengjie for XR6619992 on 20180727 end
    }
   
    private static final String TAG = "AdvancedFeaturesSettings";
     @Override
    public int getMetricsCategory() {
        return 1;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.advanced_features_preference;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new GesturesSettingPreferenceController(context));
        return controllers;
    }
   
    public static boolean isGestureAvailable(Context context){
         return new GesturesSettingPreferenceController(context).isAvailable();
    }

    static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                String summary = ""; 
                //Add Joy touch for summary
                 if (Utils.checkPackageExist(mContext,"com.tct.nowkey")) {
                    final String joytouchSummary = mContext.getString(
                            R.string.advanced_features_dashboard_summary);
                    if("".equals(summary)){
                        summary = joytouchSummary;
                    }else{
                        summary = mContext.getString(R.string.join_many_items_middle, summary, joytouchSummary);
                    }                 
                }
                //Add Gestures for summary
                if(isGestureAvailable(mContext)){
                    final String gestureSummary = mContext.getString(R.string.gesture_preference_title); 
                    if("".equals(summary)){
                         summary = gestureSummary;
                    }else{
                        summary = mContext.getString(R.string.join_many_items_middle, summary, gestureSummary);
                    }
                }   
                //Add One hand mode for summary
                if(Utils.checkActivityExist(mContext,new ComponentName("com.tct","com.tct.onehandmode.OneHandModeActivity"))){
                    final String oneHandModeSummary = mContext.getString(R.string.advanced_features_dashboard_summary_one_hand_mode); 
                    if("".equals(summary)){
                         summary = oneHandModeSummary;
                    }else{
                        summary = mContext.getString(R.string.join_many_items_middle, summary, oneHandModeSummary);
                    }
                }         

                mSummaryLoader.setSummary(this, summary);
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };


    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> result = new ArrayList<SearchIndexableResource>();

            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.advanced_features_preference;
            result.add(sir);
            return result;
        }

        @Override
        public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
            return buildPreferenceControllers(context);
        }
        //Begin  added by miaoliu for XRP23297 on 2018/9/6
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            final String screenTitle = res.getString(R.string.advanced_features_title);

            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            result.add(data);

           DashboardFeatureProvider dashboardFeatureProvider = FeatureFactory.getFactory(context).getDashboardFeatureProvider(context);
           final DashboardCategory category =
                dashboardFeatureProvider.getTilesForCategory(CategoryKey.CATEGORY_ADVANCED_FEATURE);
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

        @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add("gesture_settings");
                    return keys;
                }
        //End added by miaoliu for XRP23297 on 2018/9/6

    };
}
