package com.android.settings.display;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;


import com.android.settings.R;
import android.os.AsyncTask;//[DEFECT]-ADD by brian.chen@tcl.com, 2017-02-16, defectId:4056467
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import android.content.pm.ResolveInfo;
import com.android.settings.display.AppListPreference;

/**
 * Available app list for reading mode.
 * Add this file by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
 */
public class AvailableReadingModeAppsListFragment extends SettingsPreferenceFragment {
    private static final String TAG = "AvailableReadingModeAppsListFragment";
    private PackageManager mPackageManager;
    private PackageIntentReceiver mPackageIntentReceiver = null;

    private Context mContext;
    private List<AppListPreference> allList = new ArrayList<AppListPreference>();

    private List<ResolveInfo> appsList ;
    private static final char READING_MODE_SEPARATOR = ';';


    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.available_reading_mode_apps);
        mContext  = getPrefContext();
        
        mPackageIntentReceiver = new PackageIntentReceiver();
        mPackageIntentReceiver.registerReceiver();
        mPackageManager = mContext.getPackageManager();
        appsList = getAllPackages();
    }

    @Override
    public void onResume() {
        super.onResume();

        updatePreferenceViews();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (null != mPackageIntentReceiver) {
            mPackageIntentReceiver.unregisterReceiver();
            mPackageIntentReceiver = null;
        }
        updateEnabledReadingAppsPackage();
    }

    private void updateEnabledReadingAppsPackage(){
         Log.d("SHILEI_READING","updateEnabledReadingAppsPackage begin");
        
        final StringBuilder b = new StringBuilder();
         boolean needsSeparator = false;

        for (ResolveInfo info : appsList) {
            if(Settings.Secure.getInt(mContext.getContentResolver(), info.activityInfo.name, 0) == 1){
                if(info.activityInfo.packageName.equals("com.android.settings")){
                    continue;
                }
                if (needsSeparator) {
                    b.append(READING_MODE_SEPARATOR);
                }
                needsSeparator = true;
                b.append(info.activityInfo.packageName);
            }
        }
        Settings.Secure.putString(mContext.getContentResolver(), "reading_mode_enabled_packagename",b.toString()) ;

        Log.d("SHILEI_READING","updateEnabledReadingAppsPackage end b.toString is "+b.toString());
    }
    private void updatePreferenceViews(){
        allList.clear();

        for (ResolveInfo info : appsList) {
            if(info.activityInfo.packageName.equals("com.android.settings")){
                continue;
             }
            AppListPreference item = new AppListPreference(mContext, info, true);
            allList.add(item);
        }

        getPreferenceScreen().removeAll();
        final int N = (allList == null ? 0 : allList.size());
        for (int i = 0; i < N; ++i) {
            final AppListPreference pref = allList.get(i);
            pref.setOrder(i);
            getPreferenceScreen().addPreference(pref);
            pref.updatePreferenceViews();
        }
    }

    private List<ResolveInfo> getAllPackages() {
        List<ResolveInfo> items = new ArrayList<ResolveInfo>();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        items = mPackageManager.queryIntentActivities(mainIntent, 0);
        items.sort(mResolveInfoComparator);
        return items;
    }

    private Comparator<ResolveInfo> mResolveInfoComparator = new Comparator<ResolveInfo>() {

        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(ResolveInfo obj1, ResolveInfo obj2) {
            ResolveInfo r1 = (ResolveInfo) obj1;
            ResolveInfo r2 = (ResolveInfo) obj2;

            return sCollator.compare((r1.activityInfo.loadLabel(mPackageManager)).toString(),
                    (r2.activityInfo.loadLabel(mPackageManager)).toString());
        }
    };

    private class PackageIntentReceiver extends BroadcastReceiver {

        void registerReceiver() {
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            mContext.registerReceiver(this, filter);
            // Register for events related to sdcard installation.
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            mContext.registerReceiver(this, sdFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String actionStr = intent.getAction();
            if (null == actionStr) {
                return;
            }
            if (Intent.ACTION_PACKAGE_ADDED.equals(actionStr) || Intent.ACTION_PACKAGE_CHANGED.equals(actionStr)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(actionStr)) {
                doPackageChanged();
            } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(actionStr)
                    || Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(actionStr)) {
                String pkgList[] = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
                if (pkgList == null || pkgList.length == 0) {
                    // Ignore
                    return;
                }
                boolean avail = Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(actionStr);
                if (avail) {
                    doPackageChanged();
                }
            }
        }

        void unregisterReceiver() {
            mContext.unregisterReceiver(this);
        }

    }

    private void doPackageChanged() {
        Log.d("SHILEI_PACK","doPackageChanged");
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }
}
