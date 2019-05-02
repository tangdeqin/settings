/***
   ** 
   **add by zhijiao.li for adding "*#*#33284#*#*" to turn on/off the usb debug
   ** zhixiong.liu.hz merged for XR7045741  20181010
   ** 
***/
package com.android.settings;  

   import android.content.BroadcastReceiver;
   import android.content.ContentResolver;
   import android.content.Context;
   import android.content.Intent;
   import android.content.res.Resources;
   import android.provider.Settings;
   import android.util.Log;
   import android.widget.Toast;
   import com.android.internal.telephony.TelephonyIntents;

   public class UsbDebugBroadcastReceiver extends BroadcastReceiver {
   
       private static final String TAG="UsbDebugBroadcastReceiver";
   
       private Toast debugOpenToast;
   
       @Override
       public void onReceive(Context context, Intent intent) {
  
           if(TelephonyIntents.SECRET_CODE_ACTION.equals(intent.getAction()) && "33284".equals(intent.getData().getHost())){
               boolean mEnableAdb = false;
               final ContentResolver mContentResolver = context.getContentResolver();
               mEnableAdb = Settings.Global.getInt(mContentResolver,
                   Settings.Global.ADB_ENABLED, 0) > 0;
               Resources resources=context.getResources();
               if (debugOpenToast != null) {
                   debugOpenToast.cancel();
               }
              if(!mEnableAdb){
                   // make sure the ADB_ENABLED setting value matches the current state
                   try {
                       Settings.Global.putInt(mContentResolver,
                           Settings.Global.ADB_ENABLED, 1 );
                       debugOpenToast = Toast.makeText(context,resources.getString(R.string.enable_adb)+" "+resources.getString(R.string.gadget_state_on)
                           +" "+resources.getString(R.string.band_mode_succeeded),
                               Toast.LENGTH_SHORT);
                       debugOpenToast.show();
                   } catch (SecurityException e) {
                       // If UserManager.DISALLOW_DEBUGGING_FEATURES is on, that this setting can't be changed.
                       Log.d(TAG, "ADB_ENABLED is restricted.");
                       debugOpenToast = Toast.makeText(context,resources.getString(R.string.enable_adb)+" "+resources.getString(R.string.gadget_state_on)
                           +" "+resources.getString(R.string.band_mode_failed),
                               Toast.LENGTH_SHORT);
                       debugOpenToast.show();
                   }
               }else{
                  try {
                       Settings.Global.putInt(mContentResolver,
                           Settings.Global.ADB_ENABLED, 0 );
                       debugOpenToast = Toast.makeText(context,resources.getString(R.string.enable_adb)+" "+resources.getString(R.string.gadget_state_off)
                           +" "+resources.getString(R.string.band_mode_succeeded),
                           Toast.LENGTH_SHORT);
                       debugOpenToast.show();
                   } catch (SecurityException e) {
                       // If UserManager.DISALLOW_DEBUGGING_FEATURES is on, that this setting can't be changed.
                       Log.d(TAG, "ADB_DISENABLED is restricted.");
                       debugOpenToast = Toast.makeText(context,resources.getString(R.string.enable_adb)+" "+resources.getString(R.string.gadget_state_off)
                         +" "+resources.getString(R.string.band_mode_failed),
                         Toast.LENGTH_SHORT);
                       debugOpenToast.show();
                   }
               }
           }
       }
   }
