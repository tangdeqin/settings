package com.android.settings.notification;

import com.android.settings.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings.System;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;//Added by yingying.qiao.hz for defect 4933243 on 2017/6/21
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.os.SystemProperties; // Add for 3109312 by wei.shen.hz 20161014

import java.util.Locale;

/**
 * add by jixiang.li for PR689705 in 2014-05-30
 * @author jixiang.li
 *
 */
public class VibrationDurationSetting extends Activity implements
		OnClickListener, OnSeekBarChangeListener {
	private Button mCancel, mDefault, mConfirm;
	private SeekBar mSeekBar;
	private TextView mTextView;
	private Vibrator mVibrator;
	private  final int mMaxTime = 250;
	
	private int mTempProgress = -1;
	
	private int mDefaultVibrationTime = 40;
	private TextView mVibration_setting;//Added by yingying.qiao.hz for defect 4933243 on 2017/6/21

	@Override
	public void onCreate(Bundle bundle) {
		//Modified by yingying.qiao.hz for defect 4933243 on 2017/6/21 begin
        // Added for defect 4815760 by xiaocong.huang 2017/05/24 begin {@
//        if (Locale.getDefault().getLanguage().equals("ru")) {
//                this.setTheme(R.style.RussianVibrationDurationSettingStyle);
//        }
        // Added for defect 4815760 by xiaocong.huang 2017/05/24 end @}

		super.onCreate(bundle);
		//setTitle(R.string.vibration_dialog_title);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.vibration_duration);

		mVibration_setting =(TextView) findViewById(R.id.vibration_duration_setting);
		mVibration_setting.setText(R.string.vibration_dialog_title);
		//Modified by yingying.qiao.hz for defect 4933243 on 2017/6/21 end
		mTextView = (TextView) findViewById(R.id.vibration_text);
		mCancel = (Button) findViewById(R.id.cancel_btn);
		mDefault = (Button) findViewById(R.id.default_btn);
		mConfirm = (Button) findViewById(R.id.confirm_btn);
		mCancel.setOnClickListener(this);
		mDefault.setOnClickListener(this);
		mConfirm.setOnClickListener(this);
		
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mSeekBar = (SeekBar) findViewById(R.id.seek_vibration);
		mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(mMaxTime);
		
		mDefaultVibrationTime = SystemProperties.getInt("ro.config.def_vibrate_time",40);
		//Added by yingying.qiao.hz for defect4451487：[Language Russian][Sound]The button of "Default" is cropped in "Haptic feedback vibration duration" on 2017/4/7 begin
		if(getResources().getConfiguration().locale.getCountry().equals("RU")){
			mDefault.setTextSize(15);
			mDefault.setPadding(0,0,0,0);
		}
		//Added by yingying.qiao.hz for defect4451487：[Language Russian][Sound]The button of "Default" is cropped in "Haptic feedback vibration duration" on 2017/4/7 end
	}

	@Override
	public void onResume() {
		super.onResume();
		int cur;
		if (mTempProgress != -1){
			cur = mTempProgress;
		} else {
			cur = System.getInt(getContentResolver(), "tct_vibration", mDefaultVibrationTime);
		}
		mSeekBar.setProgress(cur);
		String curString = getString(R.string.vibration_duration_current, cur);
		mTextView.setText(curString);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		if (progress >= 0) {
			mTextView.setText(getString(R.string.vibration_duration_current, progress));
			mVibrator.vibrate(progress);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	// finish move
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		int progress = seekBar.getProgress();
		if (progress >= 0) {
			mTextView.setText(getString(R.string.vibration_duration_current, progress));
			mVibrator.vibrate(progress);	
			mTempProgress = progress;
		}
	}

	@Override
	public void onClick(View v) {
		Intent i = getIntent();
		switch (v.getId()) {
		case R.id.cancel_btn:
			finish();
			break;
		case R.id.default_btn: {
			mSeekBar.setProgress(mDefaultVibrationTime);
			i.putExtra("summary", getString(R.string.vibration_duration_current, mDefaultVibrationTime));
			boolean flag = System.putInt(getContentResolver(), "tct_vibration", mDefaultVibrationTime);
			if (flag)
				setResult(RESULT_OK, i);
			finish();
			break;
		}
		case R.id.confirm_btn: {
			int cur = mSeekBar.getProgress();
			i.putExtra("summary", getString(R.string.vibration_duration_current, cur));
			boolean flag = System.putInt(getContentResolver(), "tct_vibration", cur);
			if(flag)
				setResult(RESULT_OK, i);
			finish();
			break;
		}
		default:
			break;
		}
	}

}
