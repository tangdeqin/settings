package tct.func.broadcastreceiver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tct.func.FuncSettings;
import tct.func.FuncUtilSettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

public class FuncSettingsReceiver extends BroadcastReceiver {

    private final String TAG = this.getClass().getSimpleName();

    // added for defect 1018427
    private String needUpdatePkgName = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        needUpdatePkgName = "";
        PackageManager pm = context.getPackageManager();
        String packageName = intent.getData().getSchemeSpecificPart();
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_REMOVED)) {
            Log.e(TAG, "Uninstall Success APP:>>" + packageName);
            // added for defect 2148426 BEGIN
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                Log.e(TAG, "Uninstall >> EXTRA_REPLACING");
            } else {
            // added for defect 2148426 END
                // defect 1018427 begin
                // listen the app disabled event
                needUpdatePkgName = packageName;
            }
        } else if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_CHANGED)) {
            try {
                int appEnabledState = pm.getApplicationEnabledSetting(packageName);
                Log.d("Shortcuts", "onRecieved: Intent.ACTION_PACKAGE_CHANGED packageName="
                        + packageName + " appEnable=" + appEnabledState);
                if (appEnabledState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        || appEnabledState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                    needUpdatePkgName = packageName;
                }
            } catch (IllegalArgumentException e) {
                // defect1429899 catch this exception to avoid settings FC when
                // under a abnormal test...
                Log.e(TAG, "Application enabed exceptio", e.fillInStackTrace());
                return;
            }
            // defect 1018427 end
        } else if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_DATA_CLEARED)) {
            // added for defect 1192284
            if (TextUtils.equals("com.android.contacts", packageName)) {
                Settings.System.putInt(context.getContentResolver(),
                        "com_android_contacts_anything_saved", 0);
            }
        }
        Log.d("Shortcuts", ">>>needUpdatePkgName=" + needUpdatePkgName);
        // defect 1018427
        // save the data for both ACTION_PACKAGE_REMOVED and ACTION_PACKAGE_CHANGED
        if (!TextUtils.isEmpty(needUpdatePkgName)) {
            List<FuncSettings.ShortcutsItem> choosedLists;
            List<FuncSettings.ShortcutsItem> alternativeShortcutslists;
            String totalList = Settings.System.getStringForUser(
                    context.getContentResolver(), "total_list", UserHandle.USER_CURRENT);
            if(TextUtils.isEmpty(totalList)){
                //totalList = initTotalList(context);
                List<FuncSettings.ShortcutsItem> totalLists = new ArrayList<FuncSettings.ShortcutsItem>();
                totalLists = FuncUtilSettings.getDefaultShortcutsItem(context);
                String sTotal = FuncUtilSettings.saveTotalListJson(context,
                        totalLists);
            }

            if (!TextUtils.isEmpty(totalList)) {
                choosedLists = FuncUtilSettings.parseToChoosedLists(totalList);
                alternativeShortcutslists = FuncUtilSettings
                        .parseToalternativeShortcutslists(totalList);
                Iterator<FuncSettings.ShortcutsItem> iterator = choosedLists
                        .iterator();
                while (iterator.hasNext()) {
                    FuncSettings.ShortcutsItem choosedItem = iterator.next();
                    Log.d("Shortcuts", "(String) choosedItem.get(\"name\")=" + (String) choosedItem.getName());
                    if (TextUtils.equals(needUpdatePkgName,
                            (String) choosedItem.getPackageName())) {
                        if ((int) choosedItem.getId() != -1) {
                            // handle the pre defined item
                            choosedItem.setSlected(false);
                            alternativeShortcutslists.add(0, choosedItem);
                        }
                        iterator.remove();
                    }
                }
                List<FuncSettings.ShortcutsItem> totalLists = new ArrayList<FuncSettings.ShortcutsItem>();
                totalLists.addAll(choosedLists);
                totalLists.addAll(alternativeShortcutslists);
                String sTotal = FuncUtilSettings.saveTotalListJson(context,
                        totalLists);
//                Settings.System.putString(context.getContentResolver(),
//                        "total_list", sTotal);
                FuncUtilSettings.saveInSettingsDatabase(context.getContentResolver(),
                        "total_list", sTotal);
                if (choosedLists != null && choosedLists.size() > 0) {
                    // save choosed_list
                    String choosed_list = FuncUtilSettings.saveToChoosedlist(
                            context, choosedLists);
//                    Settings.System.putString(context.getContentResolver(),
//                            "choosed_list", choosed_list);
                    FuncUtilSettings.saveInSettingsDatabase(context.getContentResolver(),
                            "choosed_list", choosed_list);
                } else {
                    // modified for defect 1126653
                    Settings.System.putString(context.getContentResolver(),
                            "choosed_list", "inited");
                }
                Intent uninstallAppIntent = new Intent();
                uninstallAppIntent.setAction(FuncUtilSettings.UNINSTALLACTION);
                context.sendBroadcast(uninstallAppIntent);
            }
        }
    }


    private String initTotalList(Context context) {
        String totalList;
        int[] defaultIds = new int[]{};//context.getResources().getIntArray(
                //com.android.internal.R.array.shortcuts_default_order);
        List<FuncSettings.ShortcutsItem> totalLists = new ArrayList<FuncSettings.ShortcutsItem>();
        for (int id : defaultIds) {
            //Added by yongjun.zou for defect 6144265 on 2018/04/02 begin
            if (FuncUtilSettings.whetherToDeleteAppFromFunc(id)) {
                continue;
            }
            //Added by yongjun.zou for defect 6144265 on 2018/04/02 end

            Log.i(TAG, "init func data id=" + id + " name="
                    + FuncUtilSettings.getNameById(context,id));
            FuncSettings.ShortcutsItem item = new FuncSettings.ShortcutsItem();
            item.setId(id);
            item.setMainClassName("");
            item.setPackageName(FuncUtilSettings.getItemPkgName(id));
            item.setSlected(true);
            totalLists.add(item);
        }
        for (int i = 0; i < FuncUtilSettings.TOTAL_ITEMS; i++) {
            //Added by yongjun.zou for defect 6144265 on 2018/04/02 begin
            if (FuncUtilSettings.whetherToDeleteAppFromFunc(i)) {
                continue;
            }
            //Added by yongjun.zou for defect 6144265 on 2018/04/02 end

            FuncSettings.ShortcutsItem item = new FuncSettings.ShortcutsItem();
            item.setId(i);
            item.setMainClassName("");
            item.setPackageName(FuncUtilSettings.getItemPkgName(i));

            // default shortcuts have been added, so ignore
            for (int id : defaultIds) {
                if (id == i) {
                    continue;
                }
            }

            item.setSlected(false);
            totalLists.add(item);
        }
        totalList = FuncUtilSettings.saveTotalListJson(context,
                totalLists);
        Settings.System.putString(context.getContentResolver(), "total_list", totalList);
        return totalList;
    }
}