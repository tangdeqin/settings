package com.android.settings.privatemode;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ActionBar;
import android.app.ListActivity;
//import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.System;

import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
//Begin added by jinlong.lu for XR7025241 on 18-9-18
import android.provider.Settings;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;
import com.tct.sdk.base.fingerprint.FingerprintConstants;
//End added by jinlong.lu for XR7025241 on 18-9-18

public class PrivacyAppsActivity extends ListActivity implements OnItemClickListener {
    private static final String ID = "_id";
    private static final String PACKAGE_NAME = "package_name";
    private static final Uri CONTENT_URI = Uri.parse("content://com.tct.privacymode.provider/privacy_apps");

    private static final String CURRENT_PHONE_MODE = "current_phone_mode";

    String sProject[] = new String[] {ID, PACKAGE_NAME};

    private AppsAdapter mAdapter;

    private Context mContext;
    ArrayList<String> mChoiceSetAdd = new ArrayList<String>();
    ArrayList<String> mChoiceSetRemove = new ArrayList<String>();
    ArrayList<String> mChoiceSet = new ArrayList<String>();
    ArrayList<String> mOriSet = new ArrayList<String>();
    private TextView mCountView;
    private List<AppInfo> mApps;
    private List<AppInfo> mPrivacyApps;

    private ContentObserver mModeObserver;
    //Begin added by jinlong.lu for XR7025241 on 18-9-18
    private static final String TAG = "FPLaunchAppData";
    private static final String FUNC_APPS = "func_apps";
    private static final boolean DEBUG = true;
    //End added by jinlong.lu for XR7025241 on 18-9-18
    static class AppInfo {
        ApplicationInfo info;
        String label;
        String name;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(com.android.settings.R.layout.privacy_apps);
        mCountView = (TextView)findViewById(com.android.settings.R.id.privacy_apps_count);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mAdapter = new AppsAdapter();

        mApps = new ArrayList<AppInfo>();
        mPrivacyApps = new ArrayList<AppInfo>();
        getListView().setOnItemClickListener(this);

        mContext = getApplicationContext();
        mModeObserver = new OwnerAndPrivacyModeObserver();
        getContentResolver().registerContentObserver(System.getUriFor(CURRENT_PHONE_MODE), true, mModeObserver);
        //Begin modified by dongchi.chen for XRP23607 on 2018/09/08
        //new AppLoadingTask().execute((Void[]) null);
        //End modified by dongchi.chen for XRP23607 on 2018/09/08
    }

    @Override
    public void onResume() {
        super.onResume();
        //Begin modified by dongchi.chen for XRP23607 on 2018/09/08
        new AppLoadingTask().execute((Void[]) null);
        //End modified by dongchi.chen for XRP23607 on 2018/09/08
        updateCountView();
    }

    public void updateCountView() {
        String s = getString(R.string.private_apps_fm, mChoiceSet.size());
        mCountView.setText(s);
    }

//    public ProgressDialog mProgressDialog;

    public void save() {
        if ((mChoiceSetAdd.size() != 0) || (mChoiceSetRemove.size() != 0)) {
//            mProgressDialog = new ProgressDialog(this);
//            mProgressDialog.setIndeterminate(true);
//            mProgressDialog.setCancelable(false);
//            mProgressDialog.show();
            new SaveThread().run();
        }
    }
    //Begin added by jinlong.lu for XR7025241 on 18-9-19
    /***
     * reset fingerprint launch private app settings,When the user
     * sets the APK as a private application, the fingerprint will
     * no longer be able to start it, and the fingerprint startup
     * application settings are reset
     * @param context The context to initialize the application with
     * @param appList private app list
     */
    public void resetFingerprintLaunchAppSettings(Context context, List<String> appList) {
        if (appList != null && appList.size() > 0) {
            Map<Integer, String> launchSettings = getLaunchAppSettings(context);
            int count = launchSettings.size();
            if (count > 0) {
                if (DEBUG) {
                    Log.d(TAG, "Reset app :" + count);
                }
            } else {
                if (DEBUG) {
                    Log.d(TAG, "Do nothing");
                }
                return;
            }
            for (Map.Entry<Integer, String> entry : launchSettings.entrySet()) {
                for (int i = 0; i < appList.size(); i++) {
                    if (appList.get(i).equals(entry.getValue())) {
                        if (DEBUG) {
                            Log.d(TAG, "Reset launch app :   " + entry.getKey() + "th:" + entry.getValue());
                        }
                        //not found,reset fingerprint launch settings
                        System.putString(context.getContentResolver(),
                                FingerprintConstants.TCT_FINGERPRINT_LAUNCH_NUM + entry.getKey(), null);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get fingerprint launch app config
     * @param context The context to initialize the application with
     * @return Map[index,packageName] of fingerprint launch app config
     */
    Map<Integer, String> getLaunchAppSettings(Context context) {

        int mMaxfp = context.getResources().getInteger(
                R.integer.config_fingerprintMaxTemplatesPerUser);
        Map<Integer, String> result = new HashMap<Integer, String>();
        for (int i = 0; i < mMaxfp; i++) {
            String info = System.getString(context.getContentResolver(),
                    FingerprintConstants.TCT_FINGERPRINT_LAUNCH_NUM + i);
            if (DEBUG) {
                Log.d(TAG, "fpInfo:   " + info);
            }
            if (info == null) {
                continue;
            }
            String[] temp = info.split(":");
            if (temp.length > 2) {
                if (temp[1].equals(FUNC_APPS)) {
                    result.put(i, temp[2]);//add app package name
                }
            }
        }
        return result;
    }
    //End added by jinlong.lu for XR7025241 on 18-9-19
    public class SaveThread extends Thread {
        public SaveThread() {
            super();
        }

        public void run() {
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            ContentValues values = new ContentValues(1);
            if (mChoiceSetAdd.size() > 0) {
                //add private
                for (int i = 0; i < mChoiceSetAdd.size(); i++) {
                    values.clear();
                    values.put(PACKAGE_NAME, mChoiceSetAdd.get(i));
                    ops.add(ContentProviderOperation.newInsert(CONTENT_URI)
                            .withValues(values)
                            .build());
                    mOriSet.add(mChoiceSetAdd.get(i));
                }
            }

            if (mChoiceSetRemove.size() > 0) {
                //remove private
                for (int i = 0; i < mChoiceSetRemove.size(); i++) {
                    ops.add(ContentProviderOperation.newDelete(CONTENT_URI)
                            .withSelection(PACKAGE_NAME + "=?", new String[]{mChoiceSetRemove.get(i)})
                            .build());
                    mOriSet.remove(mChoiceSetRemove.get(i));
                }
            }

            //Begin added by jinlong.lu for XR7025241 on 18-9-19
            resetFingerprintLaunchAppSettings(mContext,mChoiceSetAdd);
            //End added by jinlong.lu for XR7025241 on 18-9-19
            mChoiceSetAdd.clear();
            mChoiceSetRemove.clear();

            if (ops.size() > 0) {
                try {
                    mContext.getContentResolver().applyBatch("com.tct.privacymode.provider", ops);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //mProgressDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        save();
    }

    @Override
    public void onDestroy() {
        try {
            if (mModeObserver != null) {
                getContentResolver().unregisterContentObserver(mModeObserver);
                mModeObserver = null;
            }
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void loadApps() {
        mApps.clear();
        //get privacy apps from db
        mPrivacyApps.clear();
        //Begin added by dongchi.chen for XRP23881 on 2018/09/15
        mChoiceSet.clear();
        //End added by dongchi.chen for XRP23881 on 2018/09/15
        ArrayList<String> privacy_package = new ArrayList<String>();
        Cursor c = getContentResolver().query(CONTENT_URI, sProject, null, null, null);

        if (c != null) {
            try {
                PackageManager pm = getPackageManager();
                while (c.moveToNext()) {
                    String packageName = c.getString(1);
                    try {
                        AppInfo appInfo = new AppInfo();
                        appInfo.info = pm.getApplicationInfo(packageName, 0);
                        appInfo.label = appInfo.info.loadLabel(pm).toString();
                        mPrivacyApps.add(appInfo);
                        mChoiceSet.add(packageName);
                        mOriSet.add(packageName);
                        privacy_package.add(appInfo.info.packageName);
                    } catch (NameNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } finally {
                c.close();
            }
        }
        if (mPrivacyApps.size() != 0) {
            Collections.sort(mPrivacyApps, sDisplayNameComparator);
            mApps.addAll(mPrivacyApps);
        }

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> temp;
        temp = getPackageManager().queryIntentActivities(mainIntent, 0);
        List<AppInfo> appsTemp = new ArrayList<AppInfo>();

        for (ResolveInfo app : temp) {
            AppInfo appInfo = new AppInfo();
            appInfo.info = app.activityInfo.applicationInfo;
            appInfo.label = appInfo.info.loadLabel(getPackageManager()).toString();
            appInfo.name = app.activityInfo.name;
            boolean isSystem = ((appInfo.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0);

            if (!isSystem) {
                if (!((privacy_package.size() != 0) && privacy_package.contains(appInfo.info.packageName))) {
                    appsTemp.add(appInfo);
                }
            }
        }
        if (appsTemp.size() != 0) {
            Collections.sort(appsTemp, sDisplayNameComparator);
            mApps.addAll(appsTemp);
        }
    }

    private final static Comparator<AppInfo> sDisplayNameComparator
            = new Comparator<AppInfo>() {
        public final int
        compare(AppInfo a, AppInfo b) {
            return collator.compare(a.label, b.label);
        }

        private final Collator   collator = Collator.getInstance();
    };

    public class AppsAdapter extends BaseAdapter implements OnCheckedChangeListener, OnClickListener {
        public AppsAdapter() {
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(com.android.settings.R.layout.privacy_apps_item, parent, false);
            }

            ImageView appIcon = (ImageView)convertView.findViewById(com.android.settings.R.id.icon);
            TextView appName = (TextView)convertView.findViewById(com.android.settings.R.id.title);
            Switch box = (Switch)convertView.findViewById(com.android.settings.R.id.app_switch);
            box.setOnClickListener(this);

            AppInfo info = mApps.get(position);
            ApplicationInfo appInfo = info.info;

            appName.setText(info.label);
            appIcon.setImageDrawable(appInfo.loadIcon(getPackageManager()));
            if (mChoiceSet.contains(appInfo.packageName)) {
                box.setChecked(true);
            } else {
                box.setChecked(false);
            }

            convertView.setTag(appInfo.packageName);
            box.setTag(appInfo.packageName);

            return convertView;
        }


        public final int getCount() {
            return mApps.size();
        }

        public final Object getItem(int position) {
            return mApps.get(position);
        }

        public final long getItemId(int position) {
            return position;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
        }

        @Override
        public void onClick(View v) {
            boolean isChecked = ((Switch)v).isChecked();
            String packageName = (String) v.getTag();
            if (isChecked) {
                if (!mOriSet.contains(packageName)) {
                    mChoiceSetAdd.add(packageName);
                }
                mChoiceSetRemove.remove(packageName);
                mChoiceSet.add(packageName);
            } else {
                if (mOriSet.contains(packageName)) {
                    mChoiceSetRemove.add(packageName);
                }
                mChoiceSetAdd.remove(packageName);
                mChoiceSet.remove(packageName);
            }
            updateCountView();
        }
    }

    private class AppLoadingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            loadApps();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            updateCountView();
            getListView().setAdapter(mAdapter);
        }

        @Override
        protected void onPreExecute() {
        }
    }

    //destroy screen when phone switch to privacy mode
    private class OwnerAndPrivacyModeObserver extends ContentObserver {
        public OwnerAndPrivacyModeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            if (System.getInt(getContentResolver(), CURRENT_PHONE_MODE, 0) == 1) {
                finish();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Switch box = (Switch) view.findViewById(com.android.settings.R.id.app_switch);
        boolean isChecked = !box.isChecked();
        box.setChecked(isChecked);
        String packageName = (String) view.getTag();
        if (isChecked) {
            if (!mOriSet.contains(packageName)) {
                mChoiceSetAdd.add(packageName);
            }
            mChoiceSetRemove.remove(packageName);
            mChoiceSet.add(packageName);
        } else {
            if (mOriSet.contains(packageName)) {
                mChoiceSetRemove.add(packageName);
            }
            mChoiceSetAdd.remove(packageName);
            mChoiceSet.remove(packageName);
        }
        updateCountView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
