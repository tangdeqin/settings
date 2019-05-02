package com.tcl.telephony.wfc;
/**
 * Created by chenli.gao.hz for XR6507882 on 2018/07/20
 */

import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.Toolbar;

import com.android.internal.app.AlertActivity;
import com.android.settings.R;

/**
 * "Wi-Fi Calling Help" .  This lets you know WifiCalling and go to
 * enable/disable Wi-Fi Calling and change Wi-Fi Calling mode after setupWizard finish.
 */
public class TclWifiCallingHelp extends Activity {
    private static final String TAG = "TclWifiCallingHelp";
    public static final int WFC_BOOTUP_HELP = 1;
    public static final int WFC_SETTINGS_HELP = 2;

    private int mHelpType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mHelpType = intent.getIntExtra("help_type", WFC_BOOTUP_HELP);
        initHelpView();
    }

    public void initHelpView() {
        switch (mHelpType) {
            case WFC_BOOTUP_HELP:
                createBootUpWfcHelpView();
                break;
            case WFC_SETTINGS_HELP:
                CreateWfcSettingsView();
                break;
        }
    }

    public void createBootUpWfcHelpView() {
        log("createBootUpWfcHelpView");
        setContentView(R.xml.wifi_calling_help);
        TextView settingsWfc;
        settingsWfc=(TextView)findViewById(R.id.settings);
        settingsWfc.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View source){
                Intent intent = new Intent("android.settings.WIFI_CALLING_SETTINGS");
                startActivity(intent);
                finish();
            }
        });
    }

    public void CreateWfcSettingsView(){
        log("CreateWfcSettingsView");
        setContentView(R.xml.wifi_calling_help);
        ActionBar mActionBar = this.getActionBar();
        if(mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle(R.string.wificalling_help_title);
        }

        TextView content1 = (TextView) findViewById(R.id.content1);
        content1.setText(R.string.wificalling_help_content_one);
        TextView content2 = (TextView) findViewById(R.id.content2);
        content2.setText(getResources().getString(R.string.wificalling_help_content_three));
        content2.setMovementMethod(LinkMovementMethod.getInstance());
        TextView textView = (TextView)findViewById(R.id.settings);
        textView.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        log("onOptionsItemSelected mHelpType=" + mHelpType);
        switch(item.getItemId()){
            case android.R.id.home:
                if(mHelpType == WFC_SETTINGS_HELP) {
                    finish();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}


