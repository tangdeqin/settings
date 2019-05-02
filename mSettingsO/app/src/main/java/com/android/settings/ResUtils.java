package com.android.settings.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

/** @hide */
public class ResUtils {
    private static final String TAG = ResUtils.class.getSimpleName();
    private static final String RES_TYPE_STRING = "string";
    private static final String RES_TYPE_BOOLEAN = "bool";
    private static final String RES_TYPE_INTEGER = "integer";
    private static final String RES_TYPE_STRINGARRAY = "stringarray";
    private static final String RES_TYPE_INTARRAY = "intarray";

    private static final String PACKAGE_NAME = "com.android.settings";
    private static final boolean DEBUG = "eng".equals(Build.TYPE);

    //get Integer res
    public static Integer getInteger(Context context, String resKey, String packageName, int defaultvalue) {
        Object value = getResValue(context, resKey, RES_TYPE_INTEGER, packageName);
        return value instanceof Integer ? (Integer)value : (Integer)defaultvalue;
    }

    public static Integer getInteger(Context context, String resKey, int defaultvalue) {
        return getInteger(context, resKey, PACKAGE_NAME, defaultvalue);
    }

    //get String res
    public static String getString(Context context, String resKey, String packageName, String defaultvalue) {
        Object value = getResValue(context, resKey, RES_TYPE_STRING, packageName);
        return TextUtils.isEmpty((String)value) ? (String)defaultvalue : (String)value;
    }

    public static String getString(Context context, String resKey, String defaultvalue) {
        return getString(context, resKey, PACKAGE_NAME, defaultvalue);
    }

    //get boolean res
    public static boolean getBoolean(Context context, String resKey, String packageName, boolean defaultvalue) {
        Object value = getResValue(context, resKey, RES_TYPE_BOOLEAN, packageName);
        return value instanceof Boolean ? (Boolean)value : (Boolean)defaultvalue;
    }

    public static boolean getBoolean(Context context, String resKey, boolean defaultvalue) {
        return getBoolean(context, resKey, PACKAGE_NAME, defaultvalue);
    }

    //get String Array res
    public static String[] getArray(Context context, String resKey, String packageName, String[] defaultvalue) {
        Object value = getResValue(context, resKey, RES_TYPE_STRINGARRAY, packageName);
        return (String[])value == null ? (String[])defaultvalue : (String[])value;
    }

    public static String[] getArray(Context context, String resKey, String[] defaultvalue) {
        return getArray(context, resKey, PACKAGE_NAME, defaultvalue);
    }

    //get int Array res
    public static int[] getIntArray(Context context, String resKey, String packageName, int[] defaultvalue) {
        Object value = getResValue(context, resKey, RES_TYPE_INTARRAY, packageName);
        return (int[])value == null ? (int[])defaultvalue : (int[])value;
    }

    public static int[] getIntArray(Context context, String resKey, int[] defaultvalue) {
        return getIntArray(context, resKey, PACKAGE_NAME, defaultvalue);
    }

    /**
     * get integer,boolean,string, array resources from context.
     *
     * @param context
     * @param resource key
     * @param resource type, must be one of {@link #RES_TYPE_STRING}
     *                or {@link #RES_TYPE_BOOLEAN}
     *                or {@link #RES_TYPE_INTEGER}
     *                or {@link #RES_TYPE_STRINGARRAY}
     * @param packageName, the package name of resource key
     * @return resource value, null if can not find.
     */
    private static Object getResValue(Context context, String resKey, String resType, String packageName) {
        if (context != null && !TextUtils.isEmpty(resKey) && !TextUtils.isEmpty(packageName)) {      	
            try {
                final Context mPackageContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
                Resources resource = mPackageContext.getResources();
                Object res = getValue(resource, resKey, resType, packageName);
                if(DEBUG) Log.d(TAG, resKey + " value is " + res);
                return res;
            } catch (Exception e) {
                Log.d(TAG, "Exception :" + e);
                return null;
            }
        }
        return null;
    }

    private static Object getValue(Resources resource, String resKey, String resType, String packageName) {
        if (resource != null) {
            try {
                int identifier = 0;
                switch (resType) {
                    case RES_TYPE_STRING:
                    case RES_TYPE_BOOLEAN:
                    case RES_TYPE_INTEGER:
                        identifier = resource.getIdentifier(resKey, resType, packageName);
                        break;
                    case RES_TYPE_STRINGARRAY:
                    case RES_TYPE_INTARRAY:
                        identifier = resource.getIdentifier(resKey, "array", packageName);
                        break;
                    default:
                        identifier = 0;
                }
                if (identifier > 0) {
                    switch (resType) {
                        case RES_TYPE_STRING:
                            return resource.getString(identifier);
                        case RES_TYPE_BOOLEAN:
                            return resource.getBoolean(identifier);
                        case RES_TYPE_INTEGER:
                            return resource.getInteger(identifier);
                        case RES_TYPE_STRINGARRAY:
                            return resource.getStringArray(identifier);
                        case RES_TYPE_INTARRAY:
                            return resource.getIntArray(identifier);
                        default:
                            Log.d(TAG, "SDM Type error");
                            return null;
                    }
                } else {
                    Log.d(TAG, "get SDM identifier error");
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception :" + e);
                return null;
            }
        }
        return null;
    }

    //this two method is added for situation of no context, we can not use context to getResources.
    //get bool res without context
    public static boolean getBoolean(String resKey, boolean defaultvalue) {
        Object value = getValue(Resources.getSystem(), resKey, RES_TYPE_BOOLEAN, PACKAGE_NAME);
        return value instanceof Boolean ? (Boolean)value : (Boolean)defaultvalue;
    }

    //get Integer res without context
    public static Integer getInteger(String resKey, int defaultvalue) {
        Object value = getValue(Resources.getSystem(), resKey, RES_TYPE_INTEGER, PACKAGE_NAME);
        return value instanceof Integer ? (Integer)value : (Integer)defaultvalue;
    }

    //get String res without context
    public static String getString(String resKey, String defaultvalue) {
        Object value = getValue(Resources.getSystem(), resKey, RES_TYPE_STRING, PACKAGE_NAME);
        return TextUtils.isEmpty((String)value) ? (String)defaultvalue : (String)value;
    }


    //get String Array res without context
    public static String[] getArray(String resKey, String[] defaultvalue) {
        Object value = getValue(Resources.getSystem(), resKey, RES_TYPE_STRINGARRAY, PACKAGE_NAME);
        return (String[])value == null ? (String[])defaultvalue : (String[])value;
    }

    //get int Array res without context
    public static int[] getIntArray(String resKey, int[] defaultvalue) {
        Object value = getValue(Resources.getSystem(), resKey, RES_TYPE_INTARRAY, PACKAGE_NAME);
        return (int[])value == null ? (int[])defaultvalue : (int[])value;
    }
}
