package tct.func;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import tct.func.adapter.AlternativeShortcutsAdapter;
import tct.func.adapter.AlternativeShortcutsAdapter.FuncAddListener;
import tct.func.adapter.DragAdapter;
import tct.func.adapter.DragAdapter.FuncEditItemListener;
import tct.func.view.AlternativeShortcutslistView;
import tct.func.view.DragSortListView;
import tct.func.view.DragSortListView.RemoveListener;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class FuncSettings extends SettingsPreferenceFragment implements
        SwitchBar.OnSwitchChangeListener,Indexable {

    private static final String TAG = "FuncSettings";
    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private final int ShortcutsLen = 5; //modify LEN to ShortcutsLen by yongjun.zou for task 6619992 on 20180817
    private Context mContext;
    private ContentResolver cr;
    private Activity mActivity;
    //private List<Map<String, Object>> choosedLists;
    private List<ShortcutsItem> choosedLists;
    private ScrollView func_scrollview;
    private TextView func_illustrationtv;
    private DragSortListView dragSortlistView;
    private TextView zeroshowingtv;
    private DragAdapter dragAdapter;
    private AlternativeShortcutslistView alternativeShortcutslistview;
    private AlternativeShortcutsAdapter alternativeShortcutsAdapter;
    //private List<Map<String, Object>> alternativeShortcutslists;
    private List<ShortcutsItem> alternativeShortcutslists;
    private TextView addAppshortcutstv;
    private static final String SP_NAME = "funcSettings_preferences";
    private static final String SHOWREMOVEFUNCDIALOG = "showRemoveFuncDialog";
    private boolean mChecked;
    private boolean isShowRemoveFuncDialog;
    private SharedPreferences mSharedPreferences;
    private Dialog mDialog;

    private Toast showToast;
    private PackageIntentReceiver mPackageIntentReceiver = null;
    private Handler mHandler = new Handler();
    private boolean mIsSaved = false;
    //added by zengjie for task 4264377 on 20170410 begin
    private static final int REQUEST_CODE_PICK_CONTACT = 18;
    //added by zengjie for task 4264377 on 20170410 end

    private class PackageIntentReceiver extends BroadcastReceiver {

        void registerReceiver() {
            IntentFilter filter = new IntentFilter(
                    FuncUtilSettings.UNINSTALLACTION);
            mContext.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String actionStr = intent.getAction();
            if (null == actionStr) {
                return;
            }
            if (FuncUtilSettings.UNINSTALLACTION.equals(actionStr)) {
                getData();
                setAdapterAndListenners();
            }
        }

        void unregisterReceiver() {
            mContext.unregisterReceiver(this);
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK
                && requestCode == FuncUtilSettings.GOFuncAppsListActivity) {
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                String appShortcutsPackageName = bundle
                        .getString("packageName");
                String mainclassName = bundle.getString("mainclassName");
                if (!TextUtils.isEmpty(appShortcutsPackageName)) {
                    ShortcutsItem appShortcutsItem = new ShortcutsItem();
                    appShortcutsItem.setId(-1);
                    appShortcutsItem.setName("");
                    appShortcutsItem.setMainClassName(mainclassName);
                    appShortcutsItem.setPackageName(appShortcutsPackageName);
                    appShortcutsItem.setSlected(true);
                    choosedLists.add(appShortcutsItem);
                    saveFuncSettingData();
                    dragAdapter.notifyDataSetChanged();
                    isChoosedListsEmpty();
                    func_scrollview.fullScroll(ScrollView.FOCUS_UP);
                }
            }

        }
        /* add by zhangyong for D4224755 on 20170216 begin */
        else if(resultCode == Activity.RESULT_OK
                && requestCode == REQUEST_CODE_PICK_CONTACT){
            //Modified by yongjun.zou for defect 5851119 on 2018/01/11 begin
            Uri uri = data.getData();
//            String[] contactNumber = data.getStringArrayExtra(ContactsContract.Intents.EXTRA_PHONE_URIS);
//            long[] contactId = data.getLongArrayExtra("com.tct.contacts.list.pickcontactsresult");
//            String contactNumber = data.getStringExtra("func_number");
//            Log.d(TAG,"contactNumber "+contactNumber);
//            Long contactId = data.getLongExtra("func_contactId",0);
            if(uri != null && !TextUtils.isEmpty(uri.toString())){
                Log.d(TAG,"get contact success: uri is " + uri.toString());
                //Settings.System.putString(cr, "func_call_contact_uri", uri.toString());
                FuncUtilSettings.saveInSettingsDatabase(cr,FuncConstant.FUNC_CALL_CONTACT_URI,uri.toString());
            }
            //Modified by yongjun.zou for defect 5851119 on 2018/01/11 end
        }
        /* add by zhangyong for D4224755 on 20170216 end */
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        cr = getContentResolver();
        mActivity = getActivity();

        defaultIds = mContext.getResources().getIntArray(R.array.shortcuts_default_order);

        if (defaultIds != null && defaultIds.length > 0 && defaultIds.length < ShortcutsLen + 1) {
            for (int id : defaultIds) {
                // The id number is
                if (id > 25 || id < -25) {
                    return;
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.funcsettings, container, false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();
        android.util.Log.d(TAG,"onActivityCreated");
        initFuncshortcutsData();
        initView();
        getData();
        setAdapterAndListenners();

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        getData();
        setAdapterAndListenners();
        android.util.Log.d(TAG,"onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mSwitchBar.addOnSwitchChangeListener(this);
        } catch (Exception e) {
            Log.e(TAG,
                    "IllegalStateException: Cannot add twice the same OnSwitchChangeListener");
        }
        mPackageIntentReceiver = new PackageIntentReceiver();
        mPackageIntentReceiver.registerReceiver();
    }

    @Override
    public void onPause() {
        if (null != mPackageIntentReceiver) {
            mPackageIntentReceiver.unregisterReceiver();
            mPackageIntentReceiver = null;
        }
        try {
            if (mSwitchBar != null)
                mSwitchBar.removeOnSwitchChangeListener(this);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Cannot remove OnSwitchChangeListener");
        }
        if (!mIsSaved) {
            saveFuncSettingData();
        }
        mIsSaved = false;
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initView() {
        func_scrollview = (ScrollView) mActivity
                .findViewById(R.id.func_scrollview);
        func_illustrationtv = (TextView) mActivity
                .findViewById(R.id.func_illustrationtv);
        mSharedPreferences = mContext.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        isShowRemoveFuncDialog = mSharedPreferences.getBoolean(
                SHOWREMOVEFUNCDIALOG, true);
        //Begin modified by yongjun.zou for XR P23141 on 2018/09/01
        //begin 20161013 add by xiaojing.wang for task 2825681
        int isFuncOn = mContext.getResources().getBoolean(R.bool.def_FunC_on) == true ? 1 :0;
        if (1 == Settings.System.getInt(mContext.getContentResolver(),
                "isFuncOn", isFuncOn)) {
        //end 20161013 add by xiaojing.wang for task 2825681
        //End modified by yongjun.zou for XR P23141 on 2018/09/01
            mChecked = true;
        } else {
            mChecked = false;
        }
        mSwitchBar.setChecked(mChecked);
        if (mChecked) {
            func_scrollview.setVisibility(View.VISIBLE);
            func_illustrationtv.setVisibility(View.GONE);
        } else {
            func_scrollview.setVisibility(View.GONE);
            func_illustrationtv.setVisibility(View.VISIBLE);
        }
        dragSortlistView = (DragSortListView) mActivity
                .findViewById(android.R.id.list);
        zeroshowingtv = (TextView) mActivity.findViewById(R.id.zeroshowingtv);
        alternativeShortcutslistview = (AlternativeShortcutslistView) mActivity
                .findViewById(R.id.alternativeShortcutslistview);
        addAppshortcutstv = (TextView) mActivity
                .findViewById(R.id.addAppshortcutstv);
        addAppshortcutstv.setOnClickListener(onClickListener);
    }
    private void initFuncshortcutsData(){
        String totalList = FuncUtilSettings.readFromSettingsDatabase(cr,"total_list");
        android.util.Log.d(TAG,"initFuncShortcutsDataNew111,sTotal = "+totalList);
        if (/*true*/TextUtils.isEmpty(totalList)) {
            List<ShortcutsItem> totalLists = new ArrayList<ShortcutsItem>();
            totalLists = FuncUtilSettings.getDefaultShortcutsItem(mContext);
            String sTotal = FuncUtilSettings.saveTotalListJson(mContext,
                    totalLists);
            android.util.Log.d(TAG,"initFuncShortcutsDataNew222,sTotal = "+sTotal);
            //Settings.System.putString(cr, "total_list", sTotal);
            FuncUtilSettings.saveInSettingsDatabase(cr,FuncConstant.FUNC_TOTAL_LIST,sTotal);
        }
    }

    private void setAdapterAndListenners() {
        dragAdapter = new DragAdapter(mContext, choosedLists);
        dragSortlistView.setAdapter(dragAdapter);
        dragSortlistView.setDropListener(onDrop);
        dragSortlistView.setRemoveListener(onRemove);
        // dragSortlistView.setOnItemClickListener(mOnItemClickListener);
        dragAdapter.setFuncEditItemListener(funcEditItemListener);
        alternativeShortcutsAdapter = new AlternativeShortcutsAdapter(mContext,
                alternativeShortcutslists);
        alternativeShortcutslistview.setAdapter(alternativeShortcutsAdapter);
        alternativeShortcutsAdapter.setFuncAddListener(onAddFuncItemListener);
        isChoosedListsEmpty();
    }

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                ShortcutsItem item = dragAdapter.getItem(from);
                choosedLists.remove(item);
                choosedLists.add(to, item);
                saveFuncSettingData();
                dragAdapter.notifyDataSetChanged();
                dragSortlistView.moveCheckState(from, to);
            }
        }
    };

    private FuncEditItemListener funcEditItemListener = new FuncEditItemListener() {

        @Override
        public void editFuncItem(int goType) {
            if (goType == FuncUtilSettings.START_MUSIC_PLAYLIST_ID) {
                //Begin deleted by zengjie for XR5488573 on 11/3/17
//                Intent mIntent = new Intent("com.tct.mix.action.FUNCLOCKSCREEN");
//                mIntent.putExtra("funcSettings", true);
//                mContext.sendBroadcast(mIntent);
                //End deleted by zengjie for XR5488573 on 11/3/17
            } else if (goType == FuncUtilSettings.NAVIGATE_HOME_ID) {
                Intent mIntent = new Intent(
                        "android.settings.NAVIGATE_HOME_SETTINGS");
                startActivity(mIntent);
            } //added by zengjie for task 4264377 on 20170410 begin
            else if (goType == FuncUtilSettings.CALL_A_CONTACT_ID) {
                //Modified by yongjun.zou for defect 5851119 on 2018/01/11 begin
                Intent intent = new Intent(Intent.ACTION_PICK);
//                intent.addCategory("android.intent.category.DEFAULT");
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
//                intent.putExtra("funcParams", 0);
                //Modified by yongjun.zou for defect 5851119 on 2018/01/11 end
                try {
                    startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
                } catch (ActivityNotFoundException e) {
                }
            }
            //added by zengjie for task 4264377 on 20170410 end
        }

    };

    private RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(int which, boolean flag) {

            ShortcutsItem item = dragAdapter.getItem(which);
            if (item != null) {
                int id = (int) item.getId();
                if (id != -1) {
                    //Added by yongjun.zou for defect 4598844 on 2017/04/20 begin
                    if (isShowRemoveFuncDialog && id == FuncUtilSettings.FUNC_SETTINGS_ID) {
                        showRemoveFuncSettingDialog(which);
                    }
                    //Added by yongjun.zou for defect 4598844 on 2017/04/20 end
                    /*
                    if (id == 4 && flag) {
                        showRemoveFuncSettingDialog(which);
                    } else {
                    */
                        item.setSlected(false);
                        choosedLists.remove(item);
                        dragAdapter.notifyDataSetChanged();
                        alternativeShortcutslists.add(0, item);
                        Collections.sort(alternativeShortcutslists, mScItemsIdComparator);
                        alternativeShortcutsAdapter.notifyDataSetChanged();
                    /*
                    }
                    */
                } else {
                    choosedLists.remove(item);
                    dragAdapter.notifyDataSetChanged();
                }
                android.util.Log.d(TAG,"remove action,choosedLists :"+choosedLists
                +",alternativeShortcutslists:"+alternativeShortcutslists);
                saveFuncSettingData();
            }
            isChoosedListsEmpty();
        }
    };

    private Comparator<ShortcutsItem> mScItemsIdComparator = new Comparator<ShortcutsItem>() {

        @Override
        public int compare(ShortcutsItem o1, ShortcutsItem o2) {
            return o1.getId() - o2.getId();
        }
    };

    private FuncAddListener onAddFuncItemListener = new FuncAddListener() {

        @Override
        public void addFuncItem(int position) {
            if (choosedLists.size() < ShortcutsLen) {
                ShortcutsItem item = alternativeShortcutsAdapter
                        .getItem(position);
                // defect 1018427 begin
                int id = (int) item.getId();
                if (id >= 0) {
                    // adjust if it is disabled
                    /*
                    String packageName = ShortcutsUtil.PACKAGE_MAP_LIST[id];
                    */
                    String packageName = FuncUtilSettings.getItemPkgName(id);
                    if (!TextUtils.isEmpty(packageName)) {
                        try {
                            int appEnabled = mContext.getPackageManager()
                                    .getApplicationEnabledSetting(packageName);
                            if (appEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                    || appEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                                toastTips(R.string.shortcuts_app_disable_warning);
                                return;
                            }
                        } catch (IllegalArgumentException e) {
                            // added for defect 1463255 1463310
                            Log.e(TAG, "addFuncItem exception[" + packageName + "]", e.fillInStackTrace());
                            return;
                        }
                    }
                }
                // defect 1018427 end
                item.setSlected(true);
                choosedLists.add(item);
                saveFuncSettingData();
                dragAdapter.notifyDataSetChanged();
                alternativeShortcutslists.remove(item);
                alternativeShortcutsAdapter.notifyDataSetChanged();
                isChoosedListsEmpty();
            } else {
                toastTips(R.string.func_AlternativeShortcuts_warning);
            }

        }
    };

    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (choosedLists.size() < ShortcutsLen) {
                saveFuncSettingData();
                mIsSaved = true;
                Intent intent = new Intent(mContext, FuncAppsListActivity.class);
                startActivityForResult(intent,
                        FuncUtilSettings.GOFuncAppsListActivity);
            } else {
                if (showToast != null) {
                    showToast.cancel();
                }
                Resources resources = mContext.getResources();
                showToast = Toast.makeText(mContext, resources
                        .getString(R.string.func_AlternativeShortcuts_warning),
                        Toast.LENGTH_SHORT);
                showToast.show();
            }
        }
    };

    private void getData() {

        if (choosedLists == null) {
            choosedLists = new ArrayList<ShortcutsItem>();
        }
        choosedLists.clear();
        if (alternativeShortcutslists == null) {
            alternativeShortcutslists = new ArrayList<ShortcutsItem>();
        }
        alternativeShortcutslists.clear();
        //String totalList = Settings.System.getString(cr, "total_list");
        String totalList = FuncUtilSettings.readFromSettingsDatabase(cr, FuncConstant.FUNC_TOTAL_LIST);
        android.util.Log.d(TAG,"getData, totalList :"+totalList);
        choosedLists = FuncUtilSettings.parseToChoosedLists(totalList);

        //Added by yongjun.zou for defect 5859475 on 2018/01/13 begin
        choosedLists = FuncUtilSettings.removeDisableAppFromCloosedList(mContext, choosedLists);
        //Added by yongjun.zou for defect 5859475 on 2018/01/13 end

        alternativeShortcutslists = FuncUtilSettings
                .parseToalternativeShortcutslists(totalList);
        android.util.Log.d(TAG,"getData, choosedLists :"+choosedLists+
        ",\n"+"alternativeShortcutslists :"+alternativeShortcutslists);
    }

    public void isChoosedListsEmpty() {
        if (choosedLists != null && choosedLists.size() <= 0) {
            zeroshowingtv.setVisibility(View.VISIBLE);
            dragSortlistView.setVisibility(View.GONE);
        } else {
            zeroshowingtv.setVisibility(View.GONE);
            dragSortlistView.setVisibility(View.VISIBLE);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (func_scrollview != null)
                    func_scrollview.scrollTo(0, 0);
            }
        });
    }

    private void saveFuncSettingData() {
        List<ShortcutsItem> totalLists = new ArrayList<ShortcutsItem>();
        /*
        totalLists.addAll(choosedLists);
        totalLists.addAll(alternativeShortcutslists);
        */
        Collections.sort(alternativeShortcutslists, mScItemsIdComparator);
        Set<ShortcutsItem> set = new LinkedHashSet<>(choosedLists.size()
                + alternativeShortcutslists.size());
        set.addAll(choosedLists);
        set.addAll(alternativeShortcutslists);
        totalLists.addAll(set);

        int length = totalLists.size();
        String sTotal = FuncUtilSettings.saveTotalListJson(mContext, totalLists);
        android.util.Log.d(TAG,"saveFuncSettingData,before totalLists:"+totalLists+
        ",sTotal :"+sTotal);
        FuncUtilSettings.saveInSettingsDatabase(cr,FuncConstant.FUNC_TOTAL_LIST,sTotal);
        android.util.Log.i(TAG, "saveInSettingsDatabase,  after: "+FuncUtilSettings.readFromSettingsDatabase(cr,"total_list"));
        android.util.Log.d(TAG,"saveFuncSettingData,choosed_list1 :"+choosedLists);
        if (choosedLists != null && choosedLists.size() > 0) {
            String choosed_list = FuncUtilSettings.saveToChoosedlist(mContext, choosedLists);
            //Settings.System.putString(cr, "choosed_list", choosed_list);
            android.util.Log.d(TAG,"saveFuncSettingData,choosed_list2 :"+choosed_list);
            FuncUtilSettings.saveInSettingsDatabase(cr,FuncConstant.FUNC_CHOOSED_LIST,choosed_list);
        } else {
            // modified for defect 1126653
            FuncUtilSettings.saveInSettingsDatabase(cr,FuncConstant.FUNC_CHOOSED_LIST,FuncConstant.CHOOSED_LIST_INITED_STATE);
        }

    }

    private int[] getIds(int num) {
        int[] result = new int[num];
        for (int i = 0; i < num; ++i) {
            result[i] = (Integer) choosedLists.get(i).getId();
        }
        return result;
    }

    public void showRemoveFuncSettingDialog(final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        /* modify by zhangyong for D3336497 on 20161108 begin */
        //builder.setCancelable(true /* cancelable */);
        builder.setCancelable(false /* cancelable */);
        /* modify by zhangyong for D3336497 on 20161108 end */
        // builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(R.string.func_showremovedialogmsg);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mDialog != null && mDialog.isShowing()) {
                            mDialog.dismiss();
                            isShowRemoveFuncDialog = false;
                            /* remove by zhangyong for D3336497 on 20161108 begin */
                            //dragSortlistView.removeItem(position);
                            /* remove by zhangyong for D3336497 on 20161108 end */
                            SharedPreferences.Editor editor = mSharedPreferences
                                    .edit();
                            editor.putBoolean(SHOWREMOVEFUNCDIALOG, false);
                            editor.commit();
                        }
                    }
                });
        // builder.setNegativeButton(android.R.string.cancel, null);
        mDialog = builder.create();
        mDialog.show();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            func_scrollview.setVisibility(View.VISIBLE);
            func_illustrationtv.setVisibility(View.GONE);
        } else {
            func_scrollview.setVisibility(View.GONE);
            func_illustrationtv.setVisibility(View.VISIBLE);
            //Removed by yongjun.zou for defect 4598844 on 2017/04/20 begin
            /* add by zhangyong for D3336497 on 20161108 begin */
            //if(isShowRemoveFuncDialog){
            //    showRemoveFuncSettingDialog(0);
            //}
            /* add by zhangyong for D3336497 on 20161108 end */
            //Removed by yongjun.zou for defect 4598844 on 2017/04/20 end
        }
//begin 20161013 add by xiaojing.wang for task 2825681
//        Settings.System.putInt(mContext.getContentResolver(), "isFuncOn",
//                (isChecked ? /*1*/2 : 0));
        Settings.System.putInt(mContext.getContentResolver(), "isFuncOn",
                (isChecked ? 1 : 0));
//end 20161013 add by xiaojing.wang for task 2825681
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SECURITY;
    }
    int[] defaultIds;// = {0,1,2,3,4};

    // added for defect 1018427
    void toastTips(int strId){
        if (showToast != null) {
            showToast.cancel();
        }
        Resources resources = mContext.getResources();
        showToast = Toast.makeText(mContext, resources
                .getString(strId),
                Toast.LENGTH_SHORT);
        showToast.show();
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
//                @Override
//                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
//                    ArrayList<SearchIndexableResource> result =
//                            new ArrayList<SearchIndexableResource>();
//
//                    SearchIndexableResource sir = new SearchIndexableResource(context);
//                    sir.xmlResId = R.xml.func_settings;
//                    result.add(sir);
//
//                    return result;
//                }
                //Added by yongjun.zou for defect 5872322 on 2018/01/16 begin
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                        final Resources res = context.getResources();
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.func_settings_title);
                        data.screenTitle = res.getString(R.string.func_settings);
                        data.iconResId = R.drawable.ic_settings_func;
                        result.add(data);
                    return result;
                }
                //Added by yongjun.zou for defect 5872322 on 2018/01/16 end
            };

    public static class ShortcutsItem {
        private int id;
        private String name;
        private String mainClassName;
        private String packageName;
        private boolean slected;


        public ShortcutsItem(int id, String name, String mainclassName, String packageName, Boolean slected) {
            this.id = id;
            this.name = name;
            this.mainClassName = mainclassName;
            this.packageName = packageName;
            this.slected = slected;
        }
        public ShortcutsItem(){

        }

        public void setId(int id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setMainClassName(String mainClassName) {
            this.mainClassName = mainClassName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public void setSlected(Boolean slected) {
            this.slected = slected;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getMainClassName() {
            return mainClassName;
        }

        public String getPackageName() {
            return packageName;
        }

        public boolean isSlected() {
            return slected;
        }
    }
}
