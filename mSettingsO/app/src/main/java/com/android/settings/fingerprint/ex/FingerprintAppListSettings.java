package com.android.settings.fingerprint.ex;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.android.settings.R;
import com.tct.sdk.base.privacymode.TctPrivacyModeHelper;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Copyright (C) 2017 Tcl Corporation Limited
 * Created  by jinlong.lu for XR6618444 on 18-7-31
 * Fingerprint launch app
 */

public class FingerprintAppListSettings extends Activity implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private static final String TAG = "FingerprintAppsList";

    private ListView mListView = null;
    private ListView mSearchResultsView = null;
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;

    private AppAdapter mAppAdapter = null;
    private List<ResolveInfo> mItems = new ArrayList<ResolveInfo>();
    private PackageManager mPackageManager = null;

    private Context mContext = null;
    private PackageIntentReceiver mPackageIntentReceiver = null;
    public static final String PACKAGE_NAME = "packageName";
    public static final String MAIN_CLASSNAME = "mainclassName";
    public static final String FILTER_DATAS_CHEME = "package";
    private String mChosedPackageName ="";
    private String mChosedClassName="";
    private TctPrivacyModeHelper mTctPrivacyModeHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_app_list_layout);
        mContext = this;
        mTctPrivacyModeHelper = TctPrivacyModeHelper.createHelper(mContext);
        mPackageManager = mContext.getPackageManager();
        mListView = (ListView) findViewById(R.id.apps_list);
        mListView.setOnItemClickListener(mOnItemClickListener);
        mSearchResultsView = (ListView) findViewById(R.id.search_app_list);
        mItems = getAllPackages();
        mAppAdapter = new AppAdapter(mItems);
        mListView.setAdapter(mAppAdapter);
        mPackageIntentReceiver = new PackageIntentReceiver();
        mPackageIntentReceiver.registerReceiver();
        final ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_HOME_AS_UP);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        Intent intent = getIntent();
        if (intent != null) {
            mChosedPackageName = intent.getStringExtra(PACKAGE_NAME);
            mChosedPackageName = intent.getStringExtra(MAIN_CLASSNAME);
        }
    }

    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            if (position < 0) {
                return;
            }

            ListView lv = (ListView) parent;
            ResolveInfo resolveinfo = (ResolveInfo) lv.getAdapter().getItem(
                    position);
            if (resolveinfo != null) {
                String packageName = resolveinfo.activityInfo.packageName;
                String mainClass = resolveinfo.activityInfo.name;

                if (isChoosedShortCuts(mainClass, packageName)) {
                    return;
                }
                Intent intent = getIntent();
                if (intent != null && !TextUtils.isEmpty(packageName)) {
                    Bundle bundle = new Bundle();
                    bundle.putString(PACKAGE_NAME, packageName);
                    bundle.putString(MAIN_CLASSNAME, mainClass);
                    intent.putExtras(bundle);
                    FingerprintAppListSettings.this.setResult(
                            Activity.RESULT_OK, intent);
                    finish();
                }
            }
        }

    };

    /**
     * Receives notifications when applications are added/removed.
     */
    private class PackageIntentReceiver extends BroadcastReceiver {

        void registerReceiver() {
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme(FILTER_DATAS_CHEME);
            mContext.registerReceiver(this, filter);
            // Register for events related to sdcard installation.
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            mContext.registerReceiver(this, sdFilter);
        }

        void unregisterReceiver() {
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            String actionStr = intent.getAction();
            if (null == actionStr) {
                return;
            }
            if (Intent.ACTION_PACKAGE_ADDED.equals(actionStr)
                    || Intent.ACTION_PACKAGE_CHANGED.equals(actionStr)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(actionStr)) {
                doPackageChanged();
            } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE
                    .equals(actionStr)
                    || Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE
                    .equals(actionStr)) {
                // When applications become available or unavailable (perhaps
                // because
                // the SD card was inserted or ejected) we need to refresh the
                // AppInfo with new label, icon and size information as
                // appropriate
                // given the newfound (un)availability of the application.
                // A simple way to do that is to treat the refresh as a package
                // removal followed by a package addition.
                String pkgList[] = intent
                        .getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
                if (pkgList == null || pkgList.length == 0) {
                    // Ignore
                    return;
                }
                boolean avail = Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE
                        .equals(actionStr);
                if (avail) {
                    doPackageChanged();
                }
            }
        }

    }

    private List<ResolveInfo> getAllPackages() {

        List<ResolveInfo> items = new ArrayList<ResolveInfo>();
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        items = mPackageManager.queryIntentActivities(mainIntent, 0);
        try {
            List<ResolveInfo> privateAppIndex = new ArrayList<ResolveInfo>();
            if (mTctPrivacyModeHelper != null && mTctPrivacyModeHelper.isPrivateModeFeatureOn() && mTctPrivacyModeHelper.isPrivacyModeEnable()) {
                ArrayList<String> privateAppPackageList = mTctPrivacyModeHelper.getAllPrivateApp();
                Log.d(TAG, "get private app num: " + privateAppPackageList.size());
                Log.d(TAG, "get all app num: " + items.size());
                for (String packageName : privateAppPackageList) {
                    for (ResolveInfo resolveInfo : items) {
                        if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                            Log.d(TAG, "hide private app :" + resolveInfo.activityInfo.packageName);
                            privateAppIndex.add(resolveInfo);
                            break;
                        }
                    }
                }
                items.removeAll(privateAppIndex);
                Log.d(TAG, "remove private app, all app num: " + items.size());
            }
        }catch (Exception e){
            Log.d(TAG, "deal private app error" + items.size());
        }
        Collections.sort(items, mResolveInfoComparator);
        return items;
    }

    private Comparator<ResolveInfo> mResolveInfoComparator = new Comparator<ResolveInfo>() {

        private final Collator collator = Collator.getInstance();

        @Override
        public int compare(ResolveInfo obj1, ResolveInfo obj2) {
            ResolveInfo r1 = (ResolveInfo) obj1;
            ResolveInfo r2 = (ResolveInfo) obj2;

            return collator.compare(r1.loadLabel(mPackageManager), r2.loadLabel(mPackageManager));
        }
    };

    private void doPackageChanged() {
        mItems.clear();
        mItems = getAllPackages();
        Log.d(TAG, "mItems.size-->" + mItems.size());
        mListView.setAdapter(null);
        mAppAdapter = new AppAdapter(mItems);
        mListView.setAdapter(mAppAdapter);
        mAppAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private boolean isChoosedShortCuts(String className, String packageName) {
        boolean isChoosed = false;
        if (null == mChosedPackageName || null == mChosedClassName) {
            return isChoosed;
        }

        if (mChosedPackageName.equals(packageName)
                && mChosedClassName.equals(className)) {
            isChoosed = true;
        }
        return isChoosed;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mPackageIntentReceiver) {
            mPackageIntentReceiver.unregisterReceiver();
            mPackageIntentReceiver = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.figerprint_app_options_menu, menu);

        mSearchMenuItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();

        if (mSearchMenuItem == null || mSearchView == null) {
            return false;
        }

        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setMaxWidth(1700);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public class ViewHolder {
        public ImageView icon;
        public TextView name;

        public ViewHolder(View v) {
            icon = (ImageView) v.findViewById(R.id.app_icon);
            name = (TextView) v.findViewById(R.id.app_name);
            v.setTag(this);

        }

        public void updateItemView(View view, List<ResolveInfo> items, int position) {
            ActivityInfo info = items.get(position).activityInfo;
            if (isChoosedShortCuts(info.name, info.packageName)) {
                view.setBackgroundColor(Color.LTGRAY);
            } else {
                view.setBackgroundColor(Color.WHITE);
            }
            name.setText(info.loadLabel(mPackageManager));
            Drawable drawable = info.loadIcon(mPackageManager);
            if (drawable != null) {
                icon.setImageDrawable(drawable);
            }
        }
    }

    public class AppAdapter extends BaseAdapter {

        final LayoutInflater mInflater;

        List<ResolveInfo> items = new ArrayList<ResolveInfo>();

        public AppAdapter(List<ResolveInfo> list) {
            mInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            items = list;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).hashCode();
        }

        public View newView(ViewGroup parent) {
            View v = mInflater.inflate(R.layout.fingerprint_app_list_item, parent, false);
            new ViewHolder(v);
            return v;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }

        public void bindView(View view, int position) {
            if (position >= items.size()) {
                // List must have changed since we last reported its
                // size... ignore here, we will be doing a data changed
                // to refresh the entire list.
                return;
            }
            ViewHolder vh = (ViewHolder) view.getTag();
            vh.updateItemView(view, items, position);
        }
    }

    @Override
    public boolean onClose() {
        return false;
    }

    private void setResultsVisibility(boolean visible) {
        if (mSearchResultsView != null) {
            mSearchResultsView
                    .setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //Log.d(TAG, "newText-->" + newText);
        if (TextUtils.isEmpty(newText.trim())) {
            setResultsVisibility(false);
            mListView.clearTextFilter();
            mListView.setVisibility(View.VISIBLE);
        } else {
            setResultsVisibility(true);
            updateSearchResults(newText);
            mListView.setVisibility(View.GONE);
        }
        return true;
    }

    private void updateSearchResults(String text) {
        List<ResolveInfo> aList = new ArrayList<ResolveInfo>();
        for (int i = 0; i < mItems.size(); i++) {
            ActivityInfo aInfo = mItems.get(i).activityInfo;
            String label = (String) aInfo.loadLabel(mPackageManager);
            if ((label.toLowerCase()).contains(text.toLowerCase())) {
                aList.add(mItems.get(i));
            }
        }
        //Log.d(TAG, "aList.size-->" + aList.size());
        AppAdapter adapter = new AppAdapter(aList);
        mSearchResultsView.setAdapter(adapter);
        mSearchResultsView.setOnItemClickListener(mOnItemClickListener);
    }

    @Override
    public boolean onQueryTextSubmit(String str) {
        //Log.d(TAG, "onQueryTextSubmit--->" + str);
        return false;
    }
}