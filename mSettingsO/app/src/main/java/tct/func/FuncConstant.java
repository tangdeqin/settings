package tct.func;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//[SOLUTION]-Created by TCTNB(Guoqiang.Qiu), 2016-9-20, Solution-2699164

public class FuncConstant {
    public static final String DELIMITER = ";";
    public static final String EXTRA_CHOOSED = "choosed_list";
    public static final String UNINSTALLACTION = "com.android.settings.func_uninstallappaction";
    public static final int PICK_CONTACT_REQUEST = 4; // MODIFIED by song.huan, 2016-10-18,BUG-3088432

    public static final int GOFuncAppsListActivity = 2;
    protected static final String ATTR_SHORTCUTS_ID = "id";
    protected static final String DEFAULT_SHORTCUTS_DOC = "Shortcuts";
    protected static final String ATTR_SHORTCUTS_NAME = "name";
    protected static final String ATTR_SHORTCUTS_MAINCLASS = "mainclassName";
    protected static final String ATTR_SHORTCUTS_PACKAGENAME = "packageName";
    protected static final String ATTR_SHORTCUTS_SELECTED = "selected";
    protected static final String DEFAULT_FUNCSHORTCUTS_NAME_SPACE = "http://schemas.android.com/apk/res-auto/com.tct.func";
    public static final String FUNC_TOTAL_LIST = "total_list";
    public static final String FUNC_CHOOSED_LIST = "choosed_list";
    public static final String CHOOSED_LIST_INITED_STATE = "inited";
    public static final String FUNC_CALL_CONTACT_URI = "func_call_contact_uri";

    public static ArrayList<String> stringToList(String in) {
        if (in != null) {
            return new ArrayList<>(Arrays.asList(in.split(DELIMITER)));
        }
        return null;
    }

    public static String listToString(List<String> in) {
        if (in != null) {
            StringBuilder out = new StringBuilder();
            for(String item : in) {
                out.append(item + DELIMITER);
            }
            return out.toString();
        }
        return null;
    }

}
