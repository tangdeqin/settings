package com.android.settings;
 
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Button;

 import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.util.WizardManagerHelper;
import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.settings.SetupWizardUtils;
import com.android.settings.R;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.Context;
import com.android.setupwizardlib.GlifLayout;
import com.android.setupwizardlib.util.WizardManagerHelper;
 
 
import android.widget.LinearLayout;
 
 public class DateTimeSetupActivity extends SettingsActivity {
 
 	private static final String TAG = "DateTimeSetupActivity";
     public final static int GOOGLE_SETUP_FLOW = 1000;
     //add by yeqing.lv for defect4573243 at 2017-4-12 begin
     private static final String EXTRA_SCRIPT_URI = "scriptUri";
     private static final String EXTRA_ACTION_ID = "actionId";
     private static final String EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode";
     private static final String EXTRA_IS_FIRST_RUN = "firstRun";
     //add by yeqing.lv for defect4573243 at 2017-4-12 end
 
     @Override
     protected void onCreate(Bundle savedInstance) {
         super.onCreate(savedInstance);
         LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
         layout.setFitsSystemWindows(false);
     }
 
     @Override
     protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
         resid = SetupWizardUtils.getTheme(getIntent());
         super.onApplyThemeResource(theme, resid, first);
     }
 
     @Override
     protected boolean isValidFragment(String fragmentName) {
         return DateTimeSetupActivityFragment.class.getName().equals(fragmentName);
     }
 
     @Override
     public Intent getIntent() {
         Intent modIntent = new Intent(super.getIntent());
         if (!modIntent.hasExtra(EXTRA_SHOW_FRAGMENT)) {
             modIntent.putExtra(EXTRA_SHOW_FRAGMENT, DateTimeSetupActivityFragment.class.getName());
         }
         return modIntent;
     }
 
     public static class DateTimeSetupActivityFragment extends SetupWizardDateTimeSettings
 	     implements View.OnClickListener{
    	 private Button mNextButton;
         @Override
         public View onCreateView(LayoutInflater inflater, ViewGroup container,
                 Bundle savedInstanceState) {
             final GlifLayout layout = (GlifLayout) inflater.inflate(
                     R.layout.setupwizard_date_time_settings, container, false);
 
             //begin add by wt.chentao for defect5959658 at 2018/2/3
             layout.setFocusable(true);
             layout.setFocusableInTouchMode(true);
             layout.requestFocus();
             //end add by wt.chentao for defect5959658 at 2018/2/3

             View view = super.onCreateView(inflater, container, savedInstanceState);
             ScrollView datetime = (ScrollView)layout.findViewById(R.id.setupwizard_content);
             datetime.addView(view);

             return layout;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
        	super.onViewCreated(view, savedInstanceState);
            //SetupWizardUtils.setImmersiveMode(getActivity());
            //add by yeqing.lv for defect4573243 at 2017-4-12 begin
        	mNextButton = (Button) view.findViewById(R.id.next_button);
            mNextButton.setOnClickListener(this);
             TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
             int simState = tm.getSimState();
             int simState1 = tm.getSimState(1);
     	    boolean showGMS = getActivity().getResources().getBoolean(com.android.settings.R.bool.def_show_GMSdatetime_whitout_simcard);
             if (showGMS){
     		if (simState == TelephonyManager.SIM_STATE_READY || simState1 == TelephonyManager.SIM_STATE_READY) {
 
     		} else {
     		     goGmsDateActivity();
     		     finish();
     		}
     	}
         //add by yeqing.lv for defect4573243 at 2017-4-12 end
     }
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.next_button:
                  	 Activity activity = getActivity();
                   	 Intent intent = WizardManagerHelper.getNextIntent(activity.getIntent(), Activity.RESULT_OK); 
                   	 startActivityForResult(intent, 1000);
                break;

            }
        }

     //Task 6103402 by liu.zheng.hz at 20180312 begin
     private void goGmsDateActivity(){
         try {
	     Activity activity = getActivity();
             Intent intent = activity.getIntent();
 	     intent.setAction("com.android.setupwizard.DATE_TIME");
             //intent.setComponent(new ComponentName("com.google.android.setupwizard","com.google.android.setupwizard.time.DateTimeSetupActivity"));
 	     //intent.putExtra("useImmersiveMode", true);
 	     //intent.getBooleanExtra(EXTRA_IS_FIRST_RUN, true);
             //intent.putExtra(EXTRA_SCRIPT_URI, activity.getIntent().getStringExtra(EXTRA_SCRIPT_URI));
             //intent.putExtra(EXTRA_ACTION_ID,activity.getIntent().getStringExtra(EXTRA_ACTION_ID));
             //intent.putExtra(EXTRA_RESULT_CODE, Activity.RESULT_OK);
             removeSelfActivity();
             startActivityForResult(intent, 1000);
         } catch (Exception e) {
             Log.e(TAG, "goGmsDateActivity error");
             e.printStackTrace();
         }
     }
     //Task 6103402 by liu.zheng.hz at 20180312 end

     private void removeSelfActivity(){
         PackageManager pm = getPackageManager();
         ComponentName name = new ComponentName(getActivity(), DateTimeSetupActivity.class);
         pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                 PackageManager.DONT_KILL_APP);
 
     }
   }
 
 }
