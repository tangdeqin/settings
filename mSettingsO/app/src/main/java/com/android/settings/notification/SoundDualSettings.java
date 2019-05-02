package com.android.settings.notification;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.TabActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;

import com.android.settings.R;

// Add for task 6261231 by wei.shen.hz 20180502 start
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
// Add for task 6261231 by wei.shen.hz 20180502 end

/**
 * Created for task 5571741 by yonglin.liao 2017/11/08.
 */

public class SoundDualSettings extends TabActivity {

    private final static String TAG = "SoundDualSettings";

    private TabHost tabHost;
    private FragmentManager fm;
    private SoundDualSIM1 sim1Settings;
    private SoundDualSIM2 sim2Settings;

    private String tab1_text;
    private String tab2_text;

    private final String TAB1 = "tab1";
    private final String TAB2 = "tab2";

    // Add for task 6261231 by wei.shen.hz 20180502 start
    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private SimStateChanged mSimStateChanged; 
    private TabHost.TabSpec page1;
    private TabHost.TabSpec page2;
    // Add for task 6261231 by wei.shen.hz 20180502 end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dual_sound_settings_content);

        // Add for task 6261231 by wei.shen.hz 20180502 start
        mContext = getApplicationContext();
        initSimTab();
        // Add for task 6261231 by wei.shen.hz 20180502 end
         
        tabHost = getTabHost();
        fm = getFragmentManager();

        sim1Settings = (SoundDualSIM1) fm.findFragmentByTag(SoundDualSIM1.class.getName());
        if (sim1Settings == null) {
            sim1Settings = new SoundDualSIM1();
        }
        sim2Settings = (SoundDualSIM2) fm.findFragmentByTag(SoundDualSIM2.class.getName());
        if (sim2Settings == null) {
            sim2Settings = new SoundDualSIM2();
        }

        FragmentTransaction ft1 = fm.beginTransaction();
        if (!sim1Settings.isAdded()) {
            ft1.add(R.id.sound_sim1_settings_container, sim1Settings, SoundDualSIM1.class.getName());
        }
        if (!sim2Settings.isAdded()) {
            ft1.add(R.id.sound_sim2_settings_container, sim2Settings, SoundDualSIM2.class.getName());
        }
        ft1.commit();

        page1 = tabHost.newTabSpec(TAB1)
                .setIndicator(tab1_text)
                .setContent(R.id.sound_sim1_settings_container);
        tabHost.addTab(page1);

        page2 = tabHost.newTabSpec(TAB2)
                .setIndicator(tab2_text)
                .setContent(R.id.sound_sim2_settings_container);
        tabHost.addTab(page2);

        hideAll();
        FragmentTransaction ft2 = fm.beginTransaction();
        if (savedInstanceState != null) {
            String tab = savedInstanceState.getString("tab");
            Log.d(TAG, "savedInstanceState is not null, saved tab is " + tab);
            tabHost.setCurrentTabByTag(tab);
            if (tab.equals(TAB1)) {
                ft2.show(sim1Settings);
            } else if (tab.equals(TAB2)) {
                ft2.show(sim2Settings);
            }
        } else {
            Log.d(TAG, "savedInstanceState is null");
            tabHost.setCurrentTabByTag(TAB1);
            ft2.show(sim1Settings);
        }
        ft2.commit();

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (tabId.equals(TAB1)) {
                    hideAll();
                    FragmentTransaction ft1 = fm.beginTransaction();
                    ft1.show(sim1Settings);
                    ft1.commit();
                } else if (tabId.equals(TAB2)) {
                    hideAll();
                    FragmentTransaction ft2 = fm.beginTransaction();
                    ft2.show(sim2Settings);
                    ft2.commit();
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", tabHost.getCurrentTabTag());
    }

    private void hideAll() {
        FragmentTransaction ft = fm.beginTransaction();
        ft.hide(sim1Settings);
        ft.hide(sim2Settings);
        ft.commit();
    }

// Add for task 6261231 by wei.shen.hz 20180502 start

    @Override
    protected void onResume() {
      super.onResume();
      IntentFilter filter = new IntentFilter();
      filter.addAction(Intent.ACTION_SIM_STATE_CHANGED);

      if (mSimStateChanged == null) {
          mSimStateChanged = new SimStateChanged();
      } 
      registerReceiver(mSimStateChanged,filter);
    }

    @Override
    protected void onPause() {
      super.onPause();
      if (mSimStateChanged != null){
          unregisterReceiver(mSimStateChanged);
      }
    }

    private void initSimTab(){

        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager)mContext.getSystemService(mContext.TELEPHONY_SERVICE);
        }
 
        if (mSubscriptionManager == null){
            mSubscriptionManager = SubscriptionManager.from(mContext);
        }

        //Added by yingying.qiao.hz for P10025838 on 2018-11-13 begin
        if(tab1_text ==null){
            tab1_text = getResources().getString(R.string.sound_sim1_tab);
        }
        if(tab2_text ==null){
            tab2_text = getResources().getString(R.string.sound_sim2_tab);
        }
        //Added by yingying.qiao.hz for P10025838 on 2018-11-13 end

        for (int i = 0 ; (i < 2 && mSubscriptionManager != null && mTelephonyManager != null) ; i++) {
           SubscriptionInfo sir = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(i);
           String  operatorName =  getOperatorName(mTelephonyManager, sir, i);
 
           switch (i) {
             case 0 : 
                      tab1_text = operatorName; 
                      break;
             case 1 : 
                      tab2_text = operatorName;
                      break;
             default : 
                      Log.e(TAG," There is no sim infor for sim card : " + i);
                      break ;
           }
        }
    }

    private String getOperatorName(TelephonyManager telephonyManager, SubscriptionInfo sir , int i){

       String operatorName = getResources().getString(R.string.sound_unkown_sim_tab);
        //[TCT-ROM][ sound]Begin modifiedby junliang.liu for XR7102895 on 20181116
       if (sir != null) {
           CharSequence displayName = sir.getDisplayName();
           if (displayName == null) {
               displayName = " ";
           }
           operatorName = displayName.toString();
//           operatorName = telephonyManager.getSimOperatorName(sir.getSubscriptionId());
           //[TCT-ROM][ sound]Begin modifiedby junliang.liu for XR7102895 on 20181116
           Log.d(TAG,"getOperatorName, spn = " + operatorName);
       } else {
           Log.e(TAG," sir == null operatorName : " + operatorName);
           if (i == 0){
               operatorName = getResources().getString(R.string.sound_sim1_tab);
               Log.e(TAG," There is no sim infor about operatorName for sim card i = 0 : " + operatorName);
           } else if (i == 1) {
               operatorName = getResources().getString(R.string.sound_sim2_tab);
               Log.e(TAG," There is no sim infor about operatorName for sim card i = 1 : " + operatorName);
           }
       }

       return operatorName;
    }

    private class SimStateChanged extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (mTelephonyManager == null ){
                mTelephonyManager = (TelephonyManager)mContext.getSystemService(mContext.TELEPHONY_SERVICE);
            } 

            if (intent.getAction().equals(Intent.ACTION_SIM_STATE_CHANGED)) {
                /*
                 * For state SIM_STATE_ABSENT = 1 : SIM card state: no SIM card is available in the device ;
                 * For state SIM_STATE_READY  = 5 : SIM card state: Ready
                 */
                int state = mTelephonyManager.getSimState();  
                Log.e(TAG," onReceive  state : " + state);

                //Added by yingying.qiao.hz for P10025838 on 2018-11-13 begin
                if (mTelephonyManager != null){
                    mTelephonyManager = null;
                }

                if (mSubscriptionManager != null){
                    mSubscriptionManager = null;
                }
                if (tab1_text != null){
                    tab1_text = null;
                }
                if (tab2_text != null){
                    tab2_text = null;
                }
                initSimTab();
                //Added by yingying.qiao.hz for P10025838 on 2018-11-13 end

                /*
                switch (state) {  
                   case TelephonyManager.SIM_STATE_READY :  
                        break;  

                   case TelephonyManager.SIM_STATE_ABSENT :  
                        break;
  
                   default:  
                     break;  
               }
               */
           }
        }
    }
// Add for task 6261231 by wei.shen.hz 20180502 end

}
