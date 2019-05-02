/*[TCT-ROM][SSV] Created by nanbing.zou for Task6703307 on 2018-08-21*/

package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
//import android.os.SsvManager;
import tct.ssv.SsvManager;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.os.SystemProperties;
import android.provider.Settings;

import com.android.settings.R;



import android.database.Cursor;
import android.provider.MediaStore;
import android.media.RingtoneManager;
import android.net.Uri;
import android.content.ContentUris;


public class SoundModuleSSV extends BroadcastReceiver {
	
	private static final String TAG = "SoundModuleSSV";
	
	private Context mContext;
	
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.e(TAG, "onReceive");
    	mContext = context;
    	setSoundSetting();
    }
    
    private void setSoundSetting() {
    	final boolean ssvEnable = SystemProperties.getBoolean("ro.ssv.enabled", false);
    	final String mccmnc = SystemProperties.get("persist.sys.pre.mccmnc", "");
    	Log.d(TAG, "ssvEnable = " + ssvEnable + ", mccmnc = " + mccmnc);
		if (ssvEnable) {
			if (!mccmnc.isEmpty() && SsvManager.getInstance().isAppNeedChange("settings_need_change_ringtone_res")) {
				Log.d(TAG, "mccmnc is not empty & need to change sound module resource!");
				Log.d(TAG, "begin replace SSV resource.");
				int vibrateRing_SSV = mContext.getResources().getBoolean(R.bool.def_ssv_vibrate_ring) ? 1 : 0;
				Log.d(TAG, "vibrateRing_SSV = " + vibrateRing_SSV );
				Settings.System.putInt(mContext.getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, vibrateRing_SSV);

				setSsvDefaultRingtone();
				SsvManager.getInstance().appChangeComplete("settings_need_change_ringtone_res");
			}
		}
	}
    
    private Uri getSsvRingtoneUri(String ringtoneName, int ringtoneType) {
    	Uri ringtoneUri = null;
    	String selection = MediaStore.Audio.AudioColumns.DISPLAY_NAME + "=?";
    	Cursor cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, 
    														new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATA,
    																MediaStore.Audio.Media.IS_RINGTONE, MediaStore.Audio.Media.IS_NOTIFICATION, MediaStore.Audio.Media.IS_ALARM}, 
    														selection, new String[]{ringtoneName}, null);

		Cursor uriCursor = mContext.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		String path = null;
		if (cursor.getCount() > 0) {
			for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				int is_ringtone = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE));
				int is_notification = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION));
				int is_alarm = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_ALARM));
				Log.d(TAG, "getSsvRingtoneUri() cursor position is " + cursor.getPosition());
				Log.d(TAG, "getSsvRingtoneUri() is_ringtone = " + is_ringtone + ", is_notification = " + is_notification + ", is_alarm = " + is_alarm);

				if (ringtoneType == RingtoneManager.TYPE_RINGTONE) {
					if (is_ringtone == 1 && is_notification == 0 && is_alarm == 0) {
						path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
						Log.d(TAG, "getSsvRingtoneUri() default ssv ringtone path = " + path);
						break;
					}
				} else if (ringtoneType == RingtoneManager.TYPE_NOTIFICATION) {
					if (is_ringtone == 0 && is_notification == 1 && is_alarm == 0) {
						path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
						Log.d(TAG, "getSsvRingtoneUri() default ssv notification sound path = " + path);
						break;
					}
				} else if (ringtoneType == RingtoneManager.TYPE_ALARM) {
					if (is_ringtone == 0 && is_notification == 0 && is_alarm == 1) {
						path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
						Log.d(TAG, "getSsvRingtoneUri() default ssv alarm alert path = " + path);
						break;
					}
				}


			}
		}

		if (uriCursor.getCount() > 0 && path != null) {
			for(uriCursor.moveToFirst(); !uriCursor.isAfterLast(); uriCursor.moveToNext()) {
				String tempPath = uriCursor.getString(uriCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
				if (tempPath.equals(path)) {
					int ringtoneID = uriCursor.getInt(uriCursor.getColumnIndex(MediaStore.Audio.Media._ID));
					ringtoneUri = ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, ringtoneID);
					break;
				}
			}
		}

		uriCursor.close();
    	cursor.close();

		Log.e(TAG, "getSsvRingtoneUri() ringtone name is " + ringtoneName);
		Log.e(TAG, "getSsvRingtoneUri() get uri  = " + ringtoneUri);
    	return ringtoneUri;
    }
    
    private void setSsvDefaultRingtone() {
    	String ssv_ringtone_name = mContext.getResources().getString(R.string.def_ssv_ring_ringtone);
    	String ssv_notification_name = mContext.getResources().getString(R.string.def_ssv_notification_ringtone);
    	String ssv_alarm_name = mContext.getResources().getString(R.string.def_ssv_alarm_ringtone);
		Log.e(TAG, "setSsvDefaultRingtone() ssv_ringtone_name  = " + ssv_ringtone_name);
		Log.e(TAG, "setSsvDefaultRingtone() ssv_notification_name  = " + ssv_notification_name);
		Log.e(TAG, "setSsvDefaultRingtone() ssv_alarm_name  = " + ssv_alarm_name);
    	RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, getSsvRingtoneUri(ssv_ringtone_name, RingtoneManager.TYPE_RINGTONE));
    	RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_NOTIFICATION, getSsvRingtoneUri(ssv_notification_name, RingtoneManager.TYPE_NOTIFICATION));
    	RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM, getSsvRingtoneUri(ssv_alarm_name, RingtoneManager.TYPE_ALARM));
    }
}
