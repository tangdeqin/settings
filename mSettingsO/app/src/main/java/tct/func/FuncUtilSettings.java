package tct.func;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import com.android.settings.R;
import android.os.Build;
import android.os.SystemProperties;

import tct.func.util.XMLParserUtils;

public class FuncUtilSettings {

    public static final String TAG = "FuncUtilSettings";


    public static final int TORCH_ID = 0;
    public static final int START_SOUND_RECORD_ID = 1;
    public static final int SET_TIMER_ID = 2;
    public static final int CALCULATOR_ID = 3;
    public static final int GOOGLE_VOICE_SEARCH_ID = 4;
    public static final int COMPOSE_MESSAGE_ID = 5;
    public static final int COMPOSE_EMAIL_ID = 6;
    public static final int CALL_A_CONTACT_ID = 7;
    public static final int ADD_CONTACT_ID = 8;
    public static final int ADD_EVENT_ID = 9;
    public static final int NAVIGATE_HOME_ID = 10;
    public static final int SET_ALARM_ID = 11;
    public static final int STOP_WATCH = 12;

    public static final int RECENT_CALLS_ID = 13;
    public static final int WALLSHUFFLE_SETTINGS_ID = 14;
    public static final int YAHOO_SEARCH_ID = 15;
    public static final int FUNC_SETTINGS_ID = 16;
    public static final int START_MUSIC_PLAYLIST_ID = 17;
    public static final int CAMERA_ID =18;
    public static final int SELFIE_ID = 19;
    //public static final int RECORD_A_MICRO_VIDEO = 19;

    public static int TOTAL_ITEMS = 13;

    public static final int LEN = 5;

    public static final int GOFuncAppsListActivity = 2;

    public static final String UNINSTALLACTION = "com.android.settings.func_uninstallappaction";

    public static Drawable getIconById(Context context, int id) {
        Drawable mDrawable = null;

        switch (id) {
            case SELFIE_ID:
                mDrawable = context.getDrawable(R.drawable.func_selfie);
                break;
            case GOOGLE_VOICE_SEARCH_ID:
                mDrawable = context.getDrawable(R.drawable.func_google_voice_search);
                break;
            case YAHOO_SEARCH_ID:
                mDrawable = context.getDrawable(R.drawable.func_yahoo_search);
                break;
            case CAMERA_ID:
                mDrawable = context.getDrawable(R.drawable.func_camera);
                break;
            case SET_TIMER_ID:
                mDrawable = context.getDrawable(R.drawable.func_timer);
                break;
            case TORCH_ID:
                mDrawable = context.getDrawable(R.drawable.func_torch);
                break;
            case RECENT_CALLS_ID:
                mDrawable = context.getDrawable(R.drawable.func_recent_calls);
                break;
            case FUNC_SETTINGS_ID:
                mDrawable = context.getDrawable(R.drawable.func_funcsettings);
                break;
            case WALLSHUFFLE_SETTINGS_ID:
                mDrawable = context.getDrawable(R.drawable.func_unlocksettings);
                break;
            case START_MUSIC_PLAYLIST_ID:
                mDrawable = context.getDrawable(R.drawable.func_playlist);
                break;
            case COMPOSE_MESSAGE_ID:
                mDrawable = context.getDrawable(R.drawable.func_messag);
                break;
            case COMPOSE_EMAIL_ID:
                mDrawable = context.getDrawable(R.drawable.func_emailadd);
                break;
            case ADD_CONTACT_ID:
                mDrawable = context.getDrawable(R.drawable.func_contactsadd);
                break;
            case ADD_EVENT_ID:
                mDrawable = context.getDrawable(R.drawable.func_addagenda);
                break;
            case START_SOUND_RECORD_ID:
                mDrawable = context.getDrawable(R.drawable.func_recorder);
                break;
            case NAVIGATE_HOME_ID:
                mDrawable = context.getDrawable(R.drawable.func_home);
                break;
            case STOP_WATCH:
            case SET_ALARM_ID:
                mDrawable = context.getDrawable(R.drawable.func_alarm);
                break;
            case CALCULATOR_ID:
                mDrawable = context.getDrawable(R.drawable.func_calculator);
                break;
            //added by zengjie for task 4264377 on 20170410 begin
            case CALL_A_CONTACT_ID:
                mDrawable = context.getDrawable(R.drawable.func_call_a_contact);
                break;
//            case RECORD_A_MICRO_VIDEO:
//                mDrawable = context.getDrawable(R.drawable.ic_micro_video_green);
            //added by zengjie for task 4264377 on 20170410 end
            default:
                break;
        }

        return mDrawable;
    }

    public static String getNameById(Context context, int id) {

        Resources mRes = context.getResources();
        String mName = "";

        switch (id) {
            case SELFIE_ID:
                mName = mRes.getString(R.string.func_selfie);
                break;
            case GOOGLE_VOICE_SEARCH_ID:
                mName = mRes.getString(R.string.func_google_voice_search);
                break;
            case YAHOO_SEARCH_ID:
                mName = mRes.getString(R.string.func_yahoo_search);
                break;
            case CAMERA_ID:
                mName = mRes.getString(R.string.func_camera);
                break;
            case SET_TIMER_ID:
                mName = mRes.getString(R.string.func_set_timer);
                break;
            case TORCH_ID:
                mName = mRes.getString(R.string.func_torch);
                break;
            case RECENT_CALLS_ID:
                mName = mRes.getString(R.string.func_recent_calls);
                break;
            case WALLSHUFFLE_SETTINGS_ID:
                mName = mRes.getString(R.string.func_unlocksettings);
                break;
            case FUNC_SETTINGS_ID:
                mName = mRes.getString(R.string.func_settings);
                break;
            case START_MUSIC_PLAYLIST_ID:
                mName = mRes.getString(R.string.func_start_music_playlist);
                break;
            case COMPOSE_MESSAGE_ID:
                mName = mRes.getString(R.string.func_new_message);
                break;
            case COMPOSE_EMAIL_ID:
                mName = mRes.getString(R.string.func_new_email);
                break;
            case ADD_CONTACT_ID:
                mName = mRes.getString(R.string.func_add_contact);
                break;
            case ADD_EVENT_ID:
                mName = mRes.getString(R.string.func_add_agenda);
                break;
            case START_SOUND_RECORD_ID:
                mName = mRes.getString(R.string.func_start_sound_record);
                break;
            case NAVIGATE_HOME_ID:
                mName = mRes.getString(R.string.func_navigate_home);
                break;
            case SET_ALARM_ID:
                mName = mRes.getString(R.string.func_set_alarm);
                break;
            case CALCULATOR_ID:
                mName = mRes.getString(R.string.func_calculator);
                break;
            case CALL_A_CONTACT_ID:
                mName = mRes.getString(R.string.func_call_a_contact);
                break;
            case STOP_WATCH:
                mName = mRes.getString(R.string.func_stop_watch);
            default:
                break;
        }
        return mName;
    }

    public static int[] DB2int(String s) {

        String[] sp = s.split(";");
        int[] num = new int[sp.length];
        for (int i = 0; i < sp.length; ++i) {
            num[i] = Integer.valueOf(sp[i]);
        }
        return num;
    }

    public static String int2DB(int[] num) {
        String s = "";
        for (int i = 0; i < num.length; ++i) {
            s += String.valueOf(num[i]) + ";";
        }
        String result = s.substring(0, s.length() - 1);
        return result;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int getDisplayHeight(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        int displayWidth = metrics.widthPixels;
        int displayHeight = metrics.heightPixels;

        return displayHeight;
    }

    // porting from defect 987432 begin
    public static Drawable getAppShortcutsDrawable(Context context,
            String packagename, String mainClassName) {
        Drawable drawable = null;
        try {
            ActivityInfo info = context.getPackageManager().getActivityInfo(
                    new ComponentName(packagename, mainClassName),
                    PackageManager.GET_META_DATA);
            String title = info.loadLabel(context.getPackageManager())
                    .toString();
            drawable = info.loadIcon(context.getPackageManager());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return drawable;
    }

    public static String getAppShortcutsName(Context context,
            String packagename, String mainClassName) {
        String title = "";
        try {
            ActivityInfo info = context.getPackageManager().getActivityInfo(new ComponentName(packagename,mainClassName),
                    PackageManager.GET_META_DATA);
            title = info.loadLabel(context.getPackageManager()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return title;
    }
    // porting from defect 987432 end

    public static String buildTotalListJson(Context context) {

        JSONObject jsonObj = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            for (int i = 0; i < 12; i++) {
                JSONObject jsondataObj = new JSONObject();
                jsondataObj.put("id", i);
                /* modified for defect 1018427
                jsondataObj.put("name", getNameById(context, i));

                jsondataObj.put("name", ShortcutsUtil.PACKAGE_MAP_LIST[i]);
                */
                jsondataObj.put("name", getItemPkgName(i));
                if (i < 6) {
                    jsondataObj.put("selected", true);
                } else if (i == 6) {
                    jsondataObj.put("id", -1);
                    jsondataObj.put("selected", true);
                    jsondataObj.put("name", "com.tct.weather");
                } else {
                    jsondataObj.put("selected", false);
                }
                jsonArray.put(jsondataObj);
            }
            jsonObj.put("objlist", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String list = jsonObj.toString();
        return list;
    }

    public static List<Map<String, Object>> parseTotalListJson(String json) {
        List<Map<String, Object>> listData = new ArrayList<Map<String, Object>>();
        try {
            JSONObject jsonObj = new JSONObject(json);
            JSONArray jsonArray = jsonObj.getJSONArray("objlist");
            for (int i = 0; i < jsonArray.length(); i++) {
                Map<String, Object> map = new HashMap<String, Object>();
                JSONObject jsondataObj = jsonArray.optJSONObject(i);
                int id = jsondataObj.getInt("id");
                String name = jsondataObj.getString("name");
                boolean selected = jsondataObj.getBoolean("selected");
                map.put("id", id);
                map.put("name", name);
                map.put("selected", selected);
                listData.add(map);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return listData;
    }

    public static List<FuncSettings.ShortcutsItem> parseToChoosedLists(String json) {
        List<FuncSettings.ShortcutsItem> choosedLists = new ArrayList<FuncSettings.ShortcutsItem>();
        try {
            JSONObject jsonObj = new JSONObject(json);
            JSONArray jsonArray = jsonObj.getJSONArray("objlist");
            android.util.Log.d("zengjie","parseToChoosedLists,jsonArray.length()"+jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                android.util.Log.d("zengjie","parseToChoosedLists222");
                JSONObject jsondataObj = jsonArray.optJSONObject(i);
                android.util.Log.d("zengjie","parseToChoosedLists333,toString:"+jsondataObj.toString());
                int id = jsondataObj.getInt("id");
                String name = jsondataObj.getString("name");
                String mainclassName = jsondataObj.getString("mainclassName");
                String packageName = jsondataObj.getString("packageName");
                boolean selected = jsondataObj.getBoolean("selected");
                FuncSettings.ShortcutsItem item = new FuncSettings.ShortcutsItem();
                item.setId(id);
                item.setMainClassName(mainclassName);
                item.setPackageName(packageName);
                item.setName(name);
                item.setSlected(selected);
                if (selected) {
                    choosedLists.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            android.util.Log.d("zengjie","parseToChoosedLists exception:"+e);
        }
        return choosedLists;
    }

    //Added by yongjun.zou for defect 6144265 on 2018/04/02 begin
    public static boolean whetherToDeleteAppFromFunc(int resId) {
        String ro_vendor_product_device_id = SystemProperties.get("ro.vendor.product.device");
        if ((resId == FuncUtilSettings.START_SOUND_RECORD_ID)
                && ("Rise_54".equals(Build.DEVICE) || "a3a_orange".equals(ro_vendor_product_device_id) || "Dive_73".equals(Build.DEVICE))) {
            return true;
        }
        if (resId == FuncUtilSettings.WALLSHUFFLE_SETTINGS_ID) {
            return true;
        }

        if ((resId == FuncUtilSettings.NAVIGATE_HOME_ID)
                && ("Rise_54".equals(Build.DEVICE) || ("U3A_PLUS_4G".equals(Build.DEVICE)))) {
            return true;
        }

        return false;
    }
    //Added by yongjun.zou for defect 6144265 on 2018/04/02 end

    //Added by yongjun.zou for XR P10024995 on 2018/10/26 begin
    public static boolean whichFuncAppDeleteFromPlf(Context context, String name) {
        String appNameId = context.getResources().getString(R.string.def_func_which_app_delete);
        String[] appIds = null;

        if (!TextUtils.isEmpty(appNameId) && !"@".equals(appNameId)) {
            if (appNameId.contains("@")) {
                appIds = appNameId.split("@");
            } else {
                appIds = new String[]{appNameId};
            }
        }

        try {
            for (int i = 0; i < appIds.length; i++) {
                if (name.equalsIgnoreCase(appIds[i])) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    //Added by yongjun.zou for XR P10024995 on 18/10/26 end

    //Added by yongjun.zou for defect 5859475 on 2018/01/13 begin
    public static List<FuncSettings.ShortcutsItem> removeDisableAppFromCloosedList(Context context, List<FuncSettings.ShortcutsItem> choosedList) {
        Iterator<FuncSettings.ShortcutsItem> iterator = choosedList.iterator();
        while (iterator.hasNext()) {
            FuncSettings.ShortcutsItem tempMap = iterator.next();
            int id = tempMap.getId();
            if ((id < 0 || id >= TOTAL_ITEMS) && !checkAPPEnable(context, (String)tempMap.getPackageName())) {
                iterator.remove();
                Log.d(TAG,"removeDisableAppFromCloosedList, id = " + id);
            }
        }

        return choosedList;
    }

    public static boolean checkAPPEnable(Context context, String packageName) {
        //Begin modified by yongjun.zou for XR P10024995 on 2018/10/26
        if ("".equals(packageName))
            return true;
        //End modified by yongjun.zou for XR P10024995 on 2018/10/26
        try {
            int appEnabled = context.getPackageManager()
                    .getApplicationEnabledSetting(packageName);
            if (appEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || appEnabled == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                Log.d(TAG, "checkAPPEnable [" + packageName + "] disable");
                return false;
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "checkAPPEnable exception[" + packageName + "]", e.fillInStackTrace());
            return false;
        }
        return true;
    }
    //Added by yongjun.zou for defect 5859475 on 2018/01/13 end

    public static List<FuncSettings.ShortcutsItem> parseToalternativeShortcutslists(
            String json) {
        List<FuncSettings.ShortcutsItem> alternativeShortcutslists = new ArrayList<FuncSettings.ShortcutsItem>();
        try {
            JSONObject jsonObj = new JSONObject(json);
            JSONArray jsonArray = jsonObj.getJSONArray("objlist");
            android.util.Log.d("zengjie","parseToalternativeShortcutslists,jsonArray.length()"+jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                Map<String, Object> map = new HashMap<String, Object>();
                JSONObject jsondataObj = jsonArray.optJSONObject(i);
                int id = jsondataObj.getInt("id");
                String name = jsondataObj.getString("name");
                String mainclassName = jsondataObj.getString("mainclassName");
                String packageName = jsondataObj.getString("packageName");
                boolean selected = jsondataObj.getBoolean("selected");
                FuncSettings.ShortcutsItem item = new FuncSettings.ShortcutsItem();
                item.setId(id);
                item.setName(name);
                item.setMainClassName(mainclassName);
                item.setPackageName(packageName);
                item.setSlected(selected);
                android.util.Log.d("zengjie","parseToalternativeShortcutslists22,id :"+id+
                ",selected :"+selected);

                if (!selected) {
                    alternativeShortcutslists.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return alternativeShortcutslists;
    }

//    public static String saveTotalListJson(Context context,
//            List<FuncSettings.ShortcutsItem> totalLists) {
//        JSONObject jsonObj = new JSONObject();
//        JSONArray jsonArray = new JSONArray();
//        try {
//            for (int i = 0; i < totalLists.size(); i++) {
//                JSONObject jsondataObj = new JSONObject();
//                int id = (int) totalLists.get(i).getId();
//                String mainclassName = (String) totalLists.get(i).getMainClassName();
//
//                jsondataObj.put("id", (int) totalLists.get(i).getId());
//                jsondataObj.put("name", totalLists.get(i).getName());
//                if (id != -1) {
//                    jsondataObj.put("packageName", getItemPkgName(id));
//                } else {
//                    jsondataObj.put("packageName", totalLists.get(i).getPackageName());
//                }
//                jsondataObj.put("mainclassName", mainclassName);
//                boolean selected = (boolean) totalLists.get(i).isSlected();
//                jsondataObj.put("selected", selected);
//
//                jsonArray.put(jsondataObj);
//            }
//            jsonObj.put("objlist", jsonArray);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        String list = jsonObj.toString();
//        return list;
//    }

    public static String saveToChoosedlist(Context context,
            List<FuncSettings.ShortcutsItem> totalLists) {
        JSONObject jsonObj = new JSONObject();
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < totalLists.size(); i++) {
                JSONObject jsondataObj = new JSONObject();
                int id = (int) totalLists.get(i).getId();
                String mainclassName = (String) totalLists.get(i).getMainClassName();
                jsondataObj.put("id", (int) totalLists.get(i).getId());
                jsondataObj.put("name", totalLists.get(i).getName());
                if (id != -1) {
                    jsondataObj.put("packageName", getItemPkgName(id));
                } else {
                    jsondataObj.put("packageName", totalLists.get(i).getPackageName());
                }
                jsondataObj.put("mainclassName", mainclassName);
                jsonArray.put(jsondataObj);
            }
            jsonObj.put("objlist", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String choosedlist = jsonObj.toString();
        return choosedlist;
    }

    public static List<Map<String, String>> parseChoosedJsonListForApp(Context context){
        //String jsonStr = Settings.System.getString(context.getContentResolver(), "total_list");
        String jsonStr = FuncUtilSettings.readFromSettingsDatabase(context.getContentResolver(), FuncConstant.FUNC_TOTAL_LIST);
        List<Map<String, String>> mainClassList = new ArrayList<Map<String, String>>(); 
        Log.d("FuncAppsListActivity", "jsonStr--->"+jsonStr);
       //Added by jinlong.lu for Defect 3379696 on 16-11-8 begin
        if(jsonStr ==null) return mainClassList;
       //Added by jinlong.lu for Defect 3379696 on 16-11-8 end
        try{
            JSONObject jsonObj = new JSONObject(jsonStr);
            if(null != jsonObj){
                JSONArray jsonArray = jsonObj.getJSONArray("objlist");
                if (jsonArray != null) { //Added by jinlong.lu for Defect 3379696 on 16-11-8 end
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonDataObj = jsonArray.getJSONObject(i);
                        int id = jsonDataObj.getInt("id");
                        if (id == -1) {
                            String pkgName = jsonDataObj.getString("packageName");
                            String className = jsonDataObj.getString("mainclassName");
                            Map<String, String> mainClass = new HashMap<String, String>();
                            mainClass.put("pkg", pkgName);
                            mainClass.put("class", className);
                            if (null != mainClass) {
                                mainClassList.add(mainClass);
                            }
                        }
                    }
                }
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
        Log.d("FuncAppsListActivity","mainClassList.size--->"+mainClassList.size());
        return mainClassList;
    }

    public static String getItemPkgName(int scId) {
        String pkgName;
        switch (scId) {
            case SELFIE_ID:
            case CAMERA_ID:
                pkgName = "com.mediatek.hz.camera";
                break;

				
            case GOOGLE_VOICE_SEARCH_ID:
                String ro_product_board_id = SystemProperties.get("ro.product.board.id");
                if(("U5A_PLUS_4G".equals(Build.DEVICE))&&(!ro_product_board_id.equals("5059D"))
                        || ("U3A_PLUS_4G".equals(Build.DEVICE))
                        || ("Rise_54".equals(Build.DEVICE))){
                    pkgName = "com.google.android.apps.searchlite";
                }else{
                    pkgName = "com.google.android.googlequicksearchbox";
                }
                break;
            case START_MUSIC_PLAYLIST_ID:
                //Begin modified by zengjie for XR50111111 on 10/28/17
                //pkgName = "com.alcatel.music5";
                pkgName = "com.google.android.music";
                //End modified by zengjie for XR50111111 on 10/28/17
                break;
            case CALCULATOR_ID: {
                //Begin modified by yongjun.zou for XR 7060803 on 2018/11/09
//                pkgName = "com.google.android.calculator";
                pkgName = "com.tct.calculator";
                //End modified by yongjun.zou for XR 7060803 on 2018/11/09
                break;
            }
            case ADD_EVENT_ID: {
                pkgName = "com.google.android.calendar";
                break;
            }
            case ADD_CONTACT_ID: {
                //Begin modified by zengjie for XR5488573 on 11/3/17
                //pkgName = "com.android.contacts";
                pkgName = "com.tct.contacts";
                //End modified by zengjie for XR5488573 on 11/3/17
                break;
            }
            case COMPOSE_EMAIL_ID: {
                pkgName = "";
                break;
            }
            case NAVIGATE_HOME_ID: {
                pkgName = "";
                break;
            }
            case START_SOUND_RECORD_ID: {
                pkgName = "com.tcl.soundrecorder"; //Modified by yongjun.zou for defect 5858366 on 2018/01/08
                break;
            }
            case SET_TIMER_ID:
            case STOP_WATCH:
            case SET_ALARM_ID:
                pkgName = "com.google.android.deskclock"; //"com.android.deskclock";
                //pkgName = "com.google.android.deskclock";
                break;

            case RECENT_CALLS_ID: {
                //Begin modified by zengjie for XR5488573 on 11/3/17
                //pkgName = "com.android.dialer";
                pkgName = "com.tct.dialer";
                //End modified by zengjie for XR5488573 on 11/3/17
                break;
            }

            case FUNC_SETTINGS_ID: {
                pkgName = "com.android.settings";
                break;
            }
            //added by zengjie for task 4264377 on 20170410 begin
            case CALL_A_CONTACT_ID:
            {
                //Begin modified by zengjie for XR5488573 on 11/3/17
                //pkgName = "com.android.contacts";
                pkgName = "com.tct.contacts";
                //End modified by zengjie for XR5488573 on 11/3/17
                break;
            }
            //added by zengjie for task 4264377 on 20170410 end
            default: {
                pkgName = "";
                break;
            }
        }
        return pkgName;
    }

    public static String saveTotalListJson(Context context,
                                           List<FuncSettings.ShortcutsItem> totalLists) {
        JSONObject jsonObj = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            for (FuncSettings.ShortcutsItem shortcutsItem : totalLists){
                JSONObject jsondataObj = new JSONObject();
                int id = shortcutsItem.getId();
                String itemName = shortcutsItem.getName();
                String mainclassName = shortcutsItem.getMainClassName();
                boolean selected = shortcutsItem.isSlected();

                jsondataObj.put("id", id);
                jsondataObj.put("mainclassName", mainclassName);
                jsondataObj.put("name",itemName);
                if (id != -1) {
                    jsondataObj.put("packageName", getItemPkgName(id));
                } else {
                    jsondataObj.put("packageName", shortcutsItem.getPackageName());
                }
                jsondataObj.put("selected", selected);
                jsonArray.put(jsondataObj);
            }
            jsonObj.put("objlist", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String list = jsonObj.toString();
        return list;
    }
    public static void saveInSettingsDatabase(ContentResolver cr,String dataName,String value){
        try {
            Settings.System.putString(cr, dataName,value);
        }catch (Exception e){
            e.printStackTrace();
            android.util.Log.i("zengjie", "saveInSettingsDatabase,  exception: "+e.toString());
        }
        android.util.Log.i("zengjie", "saveInSettingsDatabase,  dataName: "+dataName+",value :"+value);
        android.util.Log.i("jiedebug", "saveInSettingsDatabase, call stack: "+android.os.Debug.getCallers(5));
    }
    public static String readFromSettingsDatabase(ContentResolver cr,String dataName){
        String value = null;
        try {
            value = Settings.System.getString(cr, dataName);
        }catch (Exception e){
            e.printStackTrace();
            android.util.Log.i("zengjie", "readFromSettingsDatabase,  exception: "+e.toString());
        }
        android.util.Log.i("jiedebug", "readFromSettingsDatabase,call stack: "+android.os.Debug.getCallers(5));
        return value;
    }
    /***************
     * get default shortcuts info
     *
     * @return ShortcutsItem list
     */
    public static List<FuncSettings.ShortcutsItem> getDefaultShortcutsItem(Context context) {
        List<FuncSettings.ShortcutsItem> result = new ArrayList<>();
        try {
            XmlResourceParser parser = null;
            final Resources res = context.getResources();
            parser = res.getXml(R.xml.tct_default_shortcuts_item);
            XMLParserUtils.beginDocument(parser, FuncConstant.DEFAULT_SHORTCUTS_DOC);
            final int depth = parser.getDepth();
            int type;
            while (((type = parser.next()) != XmlPullParser.END_TAG || parser
                    .getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                int id = XMLParserUtils.getAttributeIntValue(parser, FuncConstant.DEFAULT_FUNCSHORTCUTS_NAME_SPACE, FuncConstant.ATTR_SHORTCUTS_ID);
                String name = XMLParserUtils.getAttributeValue(parser, FuncConstant.DEFAULT_FUNCSHORTCUTS_NAME_SPACE, FuncConstant.ATTR_SHORTCUTS_NAME);
                String mainClassName = XMLParserUtils.getAttributeValue(parser, FuncConstant.DEFAULT_FUNCSHORTCUTS_NAME_SPACE, FuncConstant.ATTR_SHORTCUTS_MAINCLASS);
                String packageName = XMLParserUtils.getAttributeValue(parser, FuncConstant.DEFAULT_FUNCSHORTCUTS_NAME_SPACE, FuncConstant.ATTR_SHORTCUTS_PACKAGENAME);
                boolean selected = XMLParserUtils.getAttributeBoolean(parser, FuncConstant.DEFAULT_FUNCSHORTCUTS_NAME_SPACE, FuncConstant.ATTR_SHORTCUTS_SELECTED);
                Log.d(TAG, "get  default shortcuts item from xml , id :" + id + ", name: " + name
                        + ", mainClassName :" + mainClassName + ",packageName :" + packageName
                        + ", selected :" + selected);
                //Added by yongjun.zou for XR P10024995 on 2018/10/26 begin
                if (whichFuncAppDeleteFromPlf(context, name) || !checkAPPEnable(context, packageName)) {
                    Log.d(TAG,"getDefaultShortcutsItem remove id = " + id);
                    continue;
                }
                //Added by yongjun.zou for XR P10024995 on 2018/10/26 end
                if (name != null && mainClassName != null) {
                    result.add(new FuncSettings.ShortcutsItem(id, name, mainClassName,packageName, selected));
                }
            }
            return result;
        } catch (XmlPullParserException e) {
            Log.w(TAG, "get xml info exception parsing default picture.", e);
            return null;
        } catch (IOException e) {
            Log.w(TAG, "et xml info exception parsing default picture.", e);
            return null;
        }

    }
}