package com.android.settings.applock;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.Override;
import java.lang.String;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.android.settings.password.ConfirmDeviceCredentialBaseFragment;
import com.android.settings.R;
import com.android.settings.privatemode.util.Adapter.CommonAdapter;
import com.android.settings.privatemode.util.Adapter.ViewHolder;

import com.tct.sdk.base.applock.TctAppLockHelper;

public class AppsLockActivity extends ListActivity{
    private static final int CONFIRM_REQUEST = 601;
    private static final int CONFIRM_PASSWORD_REQUEST = 602;
    private Context mContext;
    private List<AppInfoBean> mDataSource = new ArrayList<>();
    private TctAppLockHelper mAppLockHelper;

    private class AppInfoBean {
        ApplicationInfo mInfo;
        boolean mIsLocked;
        boolean mIsTitle = false;
        String mTitle;

        public AppInfoBean(ApplicationInfo info, boolean isLocked){
            this.mInfo = info;
            mIsLocked = isLocked;
        }

        public ApplicationInfo getInfo() {
            return mInfo;
        }

        public boolean isLocked() {
            return mIsLocked;
        }

        public void setIsLocked(boolean v){
            mIsLocked = v;
        }

        public boolean isTitle(){
            return mIsTitle;
        }

        public String getTitle(){
            return mTitle;
        }

        public void setTile(String tile){
            mTitle = tile;
            mIsTitle = true;
        }
    }

    private AsyncTask<Void, Void, Void> mAppLoadingTask = new AsyncTask<Void, Void, Void>() {

        //modified by dongchi.chen for XRP10026664 on 2018/11/29
        //modified by dongchi.chen for XR7333012 on 20190115
        private final List<String> blacklist =
                new ArrayList<>(Arrays.asList(new String[] {"com.android.settings",
                "com.tct.kidsmode", "com.tct.onetouchbooster", "com.tct.weather"}));//modified by dongchi.chen for XRP23331 on 2018/08/31

        List<String> addedApps = new ArrayList<>();
        private boolean isIgnore(String packageName){
            if(blacklist.contains(packageName)){
                return true;
            }
            if(addedApps.contains(packageName)){
                return true;
            }
            addedApps.add(packageName);
            return false;
        }

        @Override
        protected Void doInBackground(Void... args) {

            ArrayList<String> recommendedApps = mAppLockHelper.getRecommendedApps();
            ArrayList<String> lockedApps = mAppLockHelper.getAllLockedApps();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> allApps = getPackageManager().queryIntentActivities(mainIntent, 0);

            List<AppInfoBean> installed = new ArrayList<>();
            List<AppInfoBean> recommended = new ArrayList<>();
            mDataSource.clear();

            for (ResolveInfo i : allApps) {
                if(!isIgnore(i.activityInfo.applicationInfo.packageName)){
                    boolean isLocked = lockedApps.contains(i.activityInfo.applicationInfo.packageName);
                    boolean isRecommended= recommendedApps.contains(i.activityInfo.applicationInfo.packageName);
                    if(isRecommended) {
                        recommended.add(new AppInfoBean(i.activityInfo.applicationInfo, isLocked));
                    }else {
                        installed.add(new AppInfoBean(i.activityInfo.applicationInfo, isLocked));
                    }
                }
            }

            Comparator<AppInfoBean> appInfoBeanComparator = new Comparator<AppInfoBean>() {
                PackageManager packageManager = mContext.getPackageManager();
                public final int compare(AppInfoBean a, AppInfoBean b) {
                    if(a.isLocked() && !b.isLocked()) {
                        return -1;
                    }
                    if(!a.isLocked() && b.isLocked()) {
                        return 1;
                    }
                    return collator.compare(a.getInfo().loadLabel(packageManager).toString(),
                            b.getInfo().loadLabel(packageManager).toString());
                }

                private final Collator collator = Collator.getInstance();
            };

            AppInfoBean recommendTile = new AppInfoBean(null, false);
            recommendTile.setTile(getString(R.string.apps_lock_recommended));
            if (recommended.size() != 0) {
                Collections.sort(recommended, appInfoBeanComparator);
                mDataSource.add(recommendTile);
                mDataSource.addAll(recommended);
            }
            AppInfoBean installTile = new AppInfoBean(null, false);
            installTile.setTile(getString(R.string.apps_lock_installed));
            mDataSource.add(installTile);
            Collections.sort(installed, appInfoBeanComparator);
            mDataSource.addAll(installed);

            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            super.onPostExecute(o);

            getListView().setAdapter(new CommonAdapter<AppInfoBean>(mContext,
                    mDataSource, R.layout.apps_lock_item) {
                @Override
                public boolean isEnabled(int position) {
                    return !mDataSource.get(position).isTitle();
                }

                @Override
                public void convert(ViewHolder helper, AppInfoBean item) {

                    if (item.isTitle()) {
                        setTitleVisable(helper);

                        TextView title = helper.getView(R.id.category);
                        title.setText(item.getTitle());
                    } else {
                        setItemVisable(helper);

                        ApplicationInfo appInfo = item.getInfo();
                        helper.setText(R.id.title, appInfo.loadLabel(getPackageManager()).toString());
                        helper.setImageDrawable(R.id.icon, appInfo.loadIcon(getPackageManager()));

                        bindSwitchAction((Switch)helper.getView(R.id.app_switch), item);
                    }
                }

                private void bindSwitchAction(Switch swithbox, AppInfoBean i){
                    swithbox.setChecked(i.isLocked());
                    swithbox.setTag(i);
                    swithbox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Switch box = (Switch) v;
                            boolean isChecked = box.isChecked();
                            saveLockAppChangeAndUpdateView(isChecked, (AppInfoBean) v.getTag(), v);
                        }
                    });
                }

                private void setTitleVisable(ViewHolder holder){
                    TextView title = holder.getView(R.id.category);
                    title.setVisibility(View.VISIBLE);
                    ViewGroup itemLayout = holder.getView(R.id.item);
                    itemLayout.setVisibility(View.GONE);
                }

                private void setItemVisable(ViewHolder holder){
                    TextView title = holder.getView(R.id.category);
                    title.setVisibility(View.GONE);
                    ViewGroup itemLayout = holder.getView(R.id.item);
                    itemLayout.setVisibility(View.VISIBLE);
                }
            });
        }
    };

    //Begin modified by dongchi.chen for XRP10025585 on 20181106
    private HomeNRecentKeyListener mHomeNRecentKeyListener;//modified by dongchi.chen for XRP10025585 on 20181106
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != mHomeNRecentKeyListener) {
            mHomeNRecentKeyListener.unregister();
        }
    }
    //End modified by dongchi.chen for XRP10025585 on 20181106

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mAppLockHelper = TctAppLockHelper.createHelper(getApplicationContext());

        if(isConfirmScreenLockAction(getIntent())){
            if(!launchConfirmScreenLock()){
                finish();
            }
            return;
        }

        if(!mAppLockHelper.isAppsLockEnable()){ //modified by dongchi.chen for XRP10025346 on 20181107
            launchChooseAppsLockPattern();
        }else{
            launchConfirmPassword();
        }

        setContentView(com.android.settings.R.layout.apps_lock_list);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Switch box = (Switch) view.findViewById(R.id.app_switch);
                boolean isChecked = !box.isChecked();
                saveLockAppChangeAndUpdateView(isChecked, (AppInfoBean)box.getTag(), box);
            }
        });

        mAppLoadingTask.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ArrayList<String> lockedApps = mAppLockHelper.getAllLockedApps();
        if(lockedApps != null){
            mAppLockHelper.setAppsLockEnable(lockedApps.size() > 0);
        }
    }

    public static Intent createIntent(Context context){
        return new Intent(context, AppsLockActivity.class);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if(item.getItemId() == R.id.settings){
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(this, AppsLockSettingsActivity.class));
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean launchConfirmScreenLock() {
        final Intent intent = new Intent();
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.TITLE_TEXT, getString(R.string.apps_lock));

        if(mAppLockHelper.isFingerprintUnlockAppsLockEnable()) {
            intent.putExtra(ConfirmDeviceCredentialBaseFragment.ALLOW_FP_AUTHENTICATION, true);
        }
        intent.setClassName(ConfirmDeviceCredentialBaseFragment.PACKAGE, ConfirmAppsLockPattern.class.getName());
        intent.putExtra("package_name", getIntent().getStringExtra("package_name"));

        startActivityForResult(intent, CONFIRM_REQUEST);

        return true;
    }

    private boolean launchConfirmPassword() {
        final Intent intent = new Intent();
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.TITLE_TEXT, getString(R.string.apps_lock));
        intent.putExtra(ConfirmAppsLockPattern.CONFIRM_SETTINGS_LOCK_PATTERN, true);
        intent.setClassName(ConfirmDeviceCredentialBaseFragment.PACKAGE, ConfirmAppsLockPattern.class.getName());
        startActivityForResult(intent, CONFIRM_PASSWORD_REQUEST);

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIRM_REQUEST || requestCode == CONFIRM_PASSWORD_REQUEST) {
            if (resultCode == Activity.RESULT_FIRST_USER || resultCode == Activity.RESULT_OK) {
                if(requestCode == CONFIRM_PASSWORD_REQUEST){
                    mHomeNRecentKeyListener = new HomeNRecentKeyListener().register(this); //added by dongchi.chen for XRP10025585 on 20181106
                    return;
                }
                Intent intent = getIntent().getParcelableExtra("intent");
                if(null != intent){
                    String action = intent.getAction();
                    if(null != action && action.equalsIgnoreCase("com.tct.privacymode.action.APPS_LOCK.forIntentSender")){
                        Log.d("AppsLock", "startActivityIntentSender again");
                        Context context = getApplicationContext();
                        IntentSender intentSender = intent.getParcelableExtra("intentSender");
                        Intent fillInIntent = intent.getParcelableExtra("fillInIntent");
                        Bundle opt = intent.getBundleExtra("opt");
                        int flagsMask = intent.getIntExtra("flagsMask", 0);
                        int flagsValues = intent.getIntExtra("flagsValues", 0);
                        try {
                            context.startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues,
                                    0, opt);
                        }catch (IntentSender.SendIntentException e){
                            Log.d("AppsLock", "SendIntentException", e);
                        }

                    }else {
                        if(mIngnoreAddedClearTaskFlagList.contains(getIntent().getStringExtra("package_name"))){
                            getApplicationContext().startActivity(intent);
                        }else {
                            //intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TASK);
                            getApplicationContext().startActivity(intent);
                        }
                    }
                }
            }
        }
        if(requestCode == CHOOSE_APPS_LOCK_REQUEST){
            if (resultCode == Activity.RESULT_FIRST_USER || resultCode == Activity.RESULT_OK) {
                mHomeNRecentKeyListener = new HomeNRecentKeyListener().register(this); //added by dongchi.chen for XRP10025585 on 20181106
                return;
            }
        }
        finish();
    }

    private boolean isConfirmScreenLockAction(Intent intent){
        String action = intent.getAction();
        if(null == action){
            return false;
        }
        return action.compareToIgnoreCase("com.tct.privacymode.action.APPS_LOCK") == 0;
    }

    private void saveLockAppChangeAndUpdateView(boolean isLock, AppInfoBean bean, View v){
        Switch box = (Switch)v;
        if(null != box){
            box.setChecked(isLock);
        }
        if (isLock) {
            mAppLockHelper.insertAppsLockPackPackage(bean.getInfo().packageName);
            bean.setIsLocked(true);
        } else {
            mAppLockHelper.deleteAppsLockPackage(bean.getInfo().packageName);
            bean.setIsLocked(false);
        }
    }

    private static final int CHOOSE_APPS_LOCK_REQUEST = 100;
    private void launchChooseAppsLockPattern() {
        Intent intent = ChooseAppsLockPattern.createIntent(mContext, false);
        startActivityForResult(intent, CHOOSE_APPS_LOCK_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.apps_lock_menu, menu);
        return true;
    }

    private static ArrayList<String> mIngnoreAddedClearTaskFlagList = new ArrayList<>();
    static {
        mIngnoreAddedClearTaskFlagList.add("com.tcl.soundrecorder");
        mIngnoreAddedClearTaskFlagList.add("com.gameloft.android.GloftANPH");
    }
}



