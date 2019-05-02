/******************************************************************************/
/*                                                               Date:2017-06 */
/*                                PRESENTATION                                */
/*                                                                            */
/*       Copyright 2017 TCL Communication Technology Holdings Limited.        */
/*                                                                            */
/* This material is company confidential, cannot be reproduced in any form    */
/* without the written permission of TCL Communication Technology Holdings    */
/* Limited.                                                                   */
/*                                                                            */
/* -------------------------------------------------------------------------- */
/*  Author :  Binjian.TU                                                      */
/*  Email  :  binjian.tu.hz@tcl.com                                           */
/*  Role   :                                                                  */
/*  Reference documents :                                                     */
/* -------------------------------------------------------------------------- */
/* ========================================================================== */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* ----------|----------------------|----------------------|----------------- */
/* 2017-06-21|      Binjian.TU      |      GSMA TS.27      |Certification menu*/
/* ----------|----------------------|----------------------|----------------- */
/******************************************************************************/

package com.tct.nfc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.android.settings.R;
import com.tct.nfc.VNfcConstants;
import com.tct.nfc.VNfcAdapter;
import android.preference.PreferenceActivity;
import android.os.SystemProperties;
import android.widget.Toast;

import java.io.IOException;
import android.provider.Settings;

import android.nfc.cardemulation.CardEmulation;
import com.gsma.services.nfc.*;
import com.gsma.services.utils.InsufficientResourcesException;
import java.lang.reflect.Field;

// Supported vendor :
// NXP
public class GSMASettings extends PreferenceActivity implements
        OnPreferenceChangeListener {
    private static final String TAG = "GSMASettings";
    private VNfcAdapter mNxpNfcAdapter;
    
    private static final String ACCESS_CONTROL_ARA = "AccessControl_ARA";
    private static final String ACCESS_CONTROL_ARF = "AccessControl_ARF";
    private static final String ACCESS_CONTROL_FULLACCESS = "AccessControl_FULL";
    private static final String SWITCH_DEFAULT_ROUTE = "SwitchDefaultRoute";
    public static final int ROUTE_ID_HOST = 0x0;
    public static final int ROUTE_ID_UICC = 0x2;

    public static final String FORCE_INIT_AC = "SmartCardService.ForceInitAC";
    public static final String UNICAST_RECEPTION = "NfcService.UnicastReception";
    public static final String SERVICESCACHE_FILTER = "NfcService.ServicesCacheFilter";
    public static final String UPDATE_ROUTE_TABLE_JIT = "NativeNfcManager.JNI.UpdateRoutingTableJIT";
    public static final String MULTI_RECEPTION_ALLOW = "persist.nfc.multi_reception";
    public static final String LIGHTUP_SCREEN = "NfcService.LightUpScreen";
    public static final String ENABLE_MULTIRECEPTION = "NxpNfcController.enableMultiReception";

    public static final String GSMA_INT = "GSMA_INT";

    public static final boolean USER_BUILD = "user".equals(android.os.Build.TYPE);

    private ServiceSeek mSeek;

    private void logAndExit(String error) {
        Log.e(TAG, error);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Intent intent = getIntent();
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mNxpNfcAdapter = VNfcAdapter.instance;
        /*NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if ((adapter != null) && (mNxpNfcAdapter == null)) {
            mNxpNfcAdapter = NxpNfcAdapter.getNxpNfcAdapter(adapter);
            if (null == mNxpNfcAdapter) {
                logAndExit("NXP Adapter is null!");
                return;
            }
        }*/
        View v = inflater.inflate(R.layout.nfc_nonpayment, null);
        setContentView(v);
        mSeek = new ServiceSeek(this);
        loadItems();
        //registerReceiver(mReceiver, mRoutingCommitIntent);
    }

    private static class ServiceSeek {
        private String value;
        public static final String ARA = "useara";
        public static final String ARF = "usearf";
        public static final String FULL = "fullaccess";
        public static final String SERVICE_SEEK = "service.seek";
        public static final String DEFAULT_VALUE = "useara usearf"; 

        public static String serviceSeek(Context context) {
            String level = Settings.Global.getString(context.getContentResolver(), SERVICE_SEEK);
            return level != null ? level.trim() : DEFAULT_VALUE;
            //return SystemProperties.get("persist.service.seek", "useara usearf").trim();
        }

        private static void setServiceSeek(Context context, String value) {
            Settings.Global.putString(context.getContentResolver(), SERVICE_SEEK, value);
            //SystemProperties.set("persist.service.seek", value);
        }

        public ServiceSeek(Context context) {
            value = serviceSeek(context);
        }

        public String get() {
            return value;
        }

        public boolean has(String key) {
            return value.contains(key);
        }

        public static String rearrange(String value) {
            String[] values = value.split(" ");
            if (values == null || values.length == 0) {
                return null;
            }
            String newValue = "";
            for (String e : values) {
                if (null != e && !e.isEmpty() && !e.equals(' ')) {
                    newValue = newValue + ' ' + e;
                }
            }
            return value;
        }

        public void set(Context context, String key, boolean val) {
            value = serviceSeek(context);
            if (value.contains(key) != val) {
                String newValue;
                if (val) {
                    value = value + " " + key;
                } else {
                    value = value.replaceAll(key, "");
                }
                value = value.trim();
                String resort = rearrange(value);
                if (resort != null) {
                    value = resort;
                }
                setServiceSeek(context, value);
            }          
        }
    }

    private void loadAccessControlSettings(CheckBoxPreference ara, CheckBoxPreference arf, CheckBoxPreference full) {
        Log.d(TAG, "service.seek:" + mSeek.get());
        arf.setChecked(mSeek.has(ServiceSeek.ARF));
        ara.setChecked(mSeek.has(ServiceSeek.ARA));
        full.setChecked(mSeek.has(ServiceSeek.FULL));
    }

    private void loadDefaultRoute(SwitchPreference route, boolean onHCE) {
        route.setSwitchTextOn("UICC");
        route.setSwitchTextOff("HCE");
        route.setChecked(!onHCE);
    }

    private void loadForceAccessControl(SwitchPreference forceInit) {
        // BEGIN Modified by binjian.tu for FT-ME-UICC-AC-35 Refresh ACF Appli SP2-Test-Android on 2018/06/13
        boolean force = Settings.Global.getInt(getContentResolver(), FORCE_INIT_AC, 1) != 0;
        // END Modified by binjian.tu for FT-ME-UICC-AC-35 Refresh ACF Appli SP2-Test-Android on 2018/06/13
        forceInit.setChecked(force);
    }

    private void loadMultiReception(CheckBoxPreference multi) {
        boolean checked = 1 == SystemProperties.getInt(MULTI_RECEPTION_ALLOW, 0);
        multi.setChecked(checked);
    }

    private void loadUnicastReception(SwitchPreference unicast) {
        boolean allow = Settings.Global.getInt(getContentResolver(), UNICAST_RECEPTION, USER_BUILD ? 0 : 1) != 0;
        unicast.setChecked(allow);
    }

    private void loadUpdateRoutingTableJIT(SwitchPreference unicast) {
        boolean allow = Settings.Global.getInt(getContentResolver(), UPDATE_ROUTE_TABLE_JIT, USER_BUILD ? 0 : 1) == 1;
        unicast.setChecked(allow);
    }

    private void loadLightUpScreen(SwitchPreference light) {
        boolean allow = Settings.Global.getInt(getContentResolver(), LIGHTUP_SCREEN, 1) == 1;
        light.setChecked(allow);
    }

    private void loadServicesFilter(SwitchPreference filter) {
        final String f = Settings.Global.getString(getContentResolver(), SERVICESCACHE_FILTER);
        filter.setChecked(f != null ? f.contains("com.google.android.gms") : !USER_BUILD);
    }

    private void loadItems() {
        PreferenceManager manager = getPreferenceManager();
        PreferenceScreen screen = manager.createPreferenceScreen(this);
        boolean onHCE = mNxpNfcAdapter.GetDefaultRouteLoc() == ROUTE_ID_HOST;  
        SwitchPreference route = createSwitchPreferenceIfNeeded(screen, SWITCH_DEFAULT_ROUTE, "Default Route", onHCE ? "HCE" : "UICC");
        CheckBoxPreference ara = createCheckBoxPreferenceIfNeeded(screen, ACCESS_CONTROL_ARA, "ARA", "AccessControl Use ARA");
        CheckBoxPreference arf = createCheckBoxPreferenceIfNeeded(screen, ACCESS_CONTROL_ARF, "ARF", "AccessControl Use ARF");
        CheckBoxPreference full = createCheckBoxPreferenceIfNeeded(screen, ACCESS_CONTROL_FULLACCESS, "アクセス", "アクセスコントロール機能無効、任意訪問");               
        SwitchPreference light = createSwitchPreferenceIfNeeded(screen, LIGHTUP_SCREEN, "Light up the screen", "Light up the screen when SE FIELD is both activated and deactivated.");
        SwitchPreference forceInit = createSwitchPreferenceIfNeeded(screen, FORCE_INIT_AC, "Force AccessControl Initialization", "Always initialize AccessControl when openLogicalChannel called");
        SwitchPreference unicast = createSwitchPreferenceIfNeeded(screen, UNICAST_RECEPTION, "Unicast Reception", "Allow unicast reception mode in NfcService");
        SwitchPreference jltUpdateRT = createSwitchPreferenceIfNeeded(screen, UPDATE_ROUTE_TABLE_JIT, "Routing Table JIT", "Update routing table JIT");
        SwitchPreference filter = createSwitchPreferenceIfNeeded(screen, SERVICESCACHE_FILTER, "Google GMS Payment Services Filter", "GmsCore contains Android Pay's Aids which will affect GSMA test.");
        SwitchPreference enableMulti = createSwitchPreferenceIfNeeded(screen, ENABLE_MULTIRECEPTION, "enable multi-reception", "call enableMultiReception.");
        CheckBoxPreference multi = createCheckBoxPreferenceIfNeeded(screen, MULTI_RECEPTION_ALLOW, "Multi-reception pass through", "multi-reception method call pass through.");

        CheckBoxPreference gsma = createCheckBoxPreferenceIfNeeded(screen, GSMA_INT, "GSMA interface", "GSMA interface Test");      

        if (null == jltUpdateRT 
            || null == filter 
            || null == light
            || null == unicast
            || null == route 
            || null == ara 
            || null == arf 
            || null == full 
            || null == forceInit 
            || null == multi
            || null == enableMulti) {
            logAndExit("created Preferences failed?");            
            return;
        }

        loadDefaultRoute(route, onHCE);
        loadAccessControlSettings(ara, arf, full);
        loadLightUpScreen(light);
        loadForceAccessControl(forceInit);
        loadUnicastReception(unicast);
        loadUpdateRoutingTableJIT(jltUpdateRT);
        loadServicesFilter(filter);
        loadMultiReception(multi);
        loadGsmaTest(gsma);
        setPreferenceScreen(screen);
    }

    private NfcController mNfcCore;
    private NfcCallbacks mCallback = new NfcCallbacks();
    private OffHostService mOhs;

    public static String getComponentName(OffHostService service) {
        String result = null;
        try {
            final Class<?> clz = OffHostService.class;
            Field f = clz.getDeclaredField("mPackageName");
            f.setAccessible(true);
            result = (String)f.get(service);
            f = clz.getDeclaredField("mServiceName");
            f.setAccessible(true);
            result = result + "/" + (String)f.get(service);
        } catch (Exception e) {
            Log.e(TAG, "reflection failed!");
            e.printStackTrace();
        }
        return result;
    }

    private class NfcCallbacks implements NfcController.Callbacks {
        public void onGetDefaultController(NfcController controller) {
            mNfcCore = controller;
        }

        public void onEnableNfcController(boolean success) {
            Log.d(TAG, "onEnableNfcController:" + success);
        }

        public void onCardEmulationMode(int status) {
            Log.d(TAG, "onCardEmulationMode:" + status);
        }
    }

    private void loadGsmaTest(CheckBoxPreference cb) {
        NfcController.getDefaultController(this, mCallback);
        OffHostService[] services = mNfcCore.getOffHostServices();
        if (null == services) {
            String text = "getOffHostServices returned null!";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            Log.e(TAG, text);
            return;
        }
        for (OffHostService service : services) {
            if (null == service) {
                Log.e(TAG, "service is null.");
                continue;
            }
            final String name = getComponentName(service);
            AidGroup[] groups = service.getAidGroups();
            if (null == groups) {
                Log.e(TAG, "no AidGroup in service:" + name);
                continue;
            }
            for (AidGroup group : groups) {
                String[] aids = group.getAids();
                if (null == aids) {
                    Log.e(TAG, "no aid in service:" + name);
                    continue;
                }
                for (String aid : aids) {
                    if (null == aid) {
                        Log.e(TAG, "One of the aids is null:" + name);
                        continue;
                    }
                    if ("AAFFFFFFFFFFFFFFFFFFFFFFFFFFFFEE".equals(aid)) {
                        cb.setChecked(true);
                        Toast.makeText(this, "OffHostService found.", Toast.LENGTH_SHORT).show();
                        mOhs = service;
                        return;
                    }
                }
            }
        }
        cb.setChecked(false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference v = (CheckBoxPreference) preference;
            final String key = preference.getKey();
            final boolean on = (boolean)newValue;
            if (ACCESS_CONTROL_ARA.equals(key)) {
                mSeek.set(this, ServiceSeek.ARA, on);
            } else if (ACCESS_CONTROL_ARF.equals(key)) {
                mSeek.set(this, ServiceSeek.ARF, on);
            } else if (ACCESS_CONTROL_FULLACCESS.equals(key)) {
                mSeek.set(this, ServiceSeek.FULL, on);
            } else if (MULTI_RECEPTION_ALLOW.equals(key)) {
                SystemProperties.set(MULTI_RECEPTION_ALLOW, on ? "1" : "0");
            } else if (GSMA_INT.equals(key)) {
                if (on) {
                    if (null != mOhs) {
                        String l = "GSMA interface is set but you try to create another one?";
                        Toast.makeText(this, l, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, l);
                    }
                } else if (null == mOhs) {
                    String l = "GSMA interface is null but you try to delete it?";
                    Toast.makeText(this, l, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, l);
                }
                if (mOhs == null) {
                    mOhs = mNfcCore.defineOffHostService("GSMA Interface test", "SIM1");
                    AidGroup aids = mOhs.defineAidGroup("Settings GSMA test", CardEmulation.CATEGORY_PAYMENT);
                    mOhs.setBanner(getDrawable(R.drawable.ic_nfc));
                    aids.addNewAid("AAFFFFFFFFFFFFFFFFFFFFFFFFFFFFEE");
                    try {
                        mOhs.commit();
                    } catch (InsufficientResourcesException e) {
                        e.printStackTrace();
                    }
                } else {
                    mNfcCore.deleteOffHostService(mOhs);
                    mOhs = null;
                }
            } else {
                logAndExit("onPreferenceChange received " + key + ", no handler, exit.");
                return false;
            }
            return true;
        } else if (preference instanceof SwitchPreference) {
            final String key = preference.getKey();
            boolean allow = (boolean)newValue;
            if (SWITCH_DEFAULT_ROUTE.equals(key)) {
                try {
                    int defaultRoute = mNxpNfcAdapter.GetDefaultRouteLoc();
                    switch (defaultRoute) {
                    case ROUTE_ID_HOST:
                        mNxpNfcAdapter.DefaultRouteSet(VNfcConstants.UICC_ID, true, true, false);
                        preference.setSummary("UICC");
                        Toast.makeText(this, "Default route set to UICC.", Toast.LENGTH_SHORT).show();
                        break;
                    case ROUTE_ID_UICC:
                        mNxpNfcAdapter.DefaultRouteSet(VNfcConstants.HOST_ID, true, false, false);
                        preference.setSummary("HCE");
                        Toast.makeText(this, "Default route set to HCE.", Toast.LENGTH_SHORT).show();
                        break;
                    }
                } catch (IOException e) {
                   e.printStackTrace();
                   return false;
                }
            } else if (FORCE_INIT_AC.equals(key)) {
                Settings.Global.putInt(getContentResolver(), FORCE_INIT_AC, allow ? 1 : 0);
            } else if (LIGHTUP_SCREEN.equals(key)) {
                Settings.Global.putInt(getContentResolver(), LIGHTUP_SCREEN, allow ? 1 : 0);
            } else if (UNICAST_RECEPTION.equals(key)) {
                Settings.Global.putInt(getContentResolver(), UNICAST_RECEPTION, allow ? 1 : 0);
            } else if (UPDATE_ROUTE_TABLE_JIT.equals(key)) {
                Settings.Global.putInt(getContentResolver(), UPDATE_ROUTE_TABLE_JIT, allow ? 1 : 0);
            } else if (SERVICESCACHE_FILTER.equals(key)) {
                Settings.Global.putString(getContentResolver(), SERVICESCACHE_FILTER, allow ? "com.google.android.gms;" : "");
            } else if (ENABLE_MULTIRECEPTION.equals(key)) {
                com.gsma.services.utils.Handset handset = new com.gsma.services.utils.Handset();
                try {
                    handset.enableMultiEvt_transactionReception();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        }
        return false;
    }

    private CheckBoxPreference createCheckBoxPreferenceIfNeeded(PreferenceScreen screen, String key, String title, String summary) {
        Preference preference = screen.findPreference(key);
        if (preference != null) {
            return (CheckBoxPreference)preference;
        }
        CheckBoxPreference cbp = new CheckBoxPreference(this);
        cbp.setKey(key);
        cbp.setTitle(title);
        cbp.setSummary(summary);
        cbp.setOnPreferenceChangeListener(this);
        screen.addPreference(cbp);
        return cbp;
    }

    private SwitchPreference createSwitchPreferenceIfNeeded(PreferenceScreen screen, String key, String title, String summary) {
        Preference preference = screen.findPreference(key);
        if (preference != null) {
            return (SwitchPreference)preference;
        }
        SwitchPreference p = new SwitchPreference(this);
        p.setKey(key);
        p.setTitle(title);
        p.setSummary(summary);
        p.setOnPreferenceChangeListener(this);
        screen.addPreference(p);
        return p;
    }
}
