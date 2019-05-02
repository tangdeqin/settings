package com.tcl.telephony.wfc;

/**
 * Created by chenli.gao.hz for XR6507882 on 2018/07/20
 */

import com.android.settings.R;
import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import android.content.DialogInterface;
import android.widget.TextView;
import android.text.method.LinkMovementMethod;

public class TclWifiCallingHelpDialogActivity extends AlertActivity
        implements DialogInterface.OnClickListener {

    private int mDialogType;

    public static final int WFC_HELP_BUTTON_DIALOG = 1;
    public static final int WFC_HELP_BUTTON_POLAND_DIALOG = 2;
    public static final int WFC_SWITCH_FIRST_DIALOG = 3;
    public static final int WFC_SWITCH_FIRST_POLAND_DIALOG = 4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mDialogType = intent.getIntExtra("dialog_type", WFC_HELP_BUTTON_DIALOG);
        createDialog();
        setFinishOnTouchOutside(false);
    }

    public void createDialog() {
        switch(mDialogType) {
            case WFC_HELP_BUTTON_DIALOG:
                createWfcHelpButtonDialog();
                break;
            case WFC_HELP_BUTTON_POLAND_DIALOG:
                createWfcHelpButtonPolandDialog();
                break;
            case WFC_SWITCH_FIRST_DIALOG:
                createWfcSwitchFirstDialog();
                break;
            case WFC_SWITCH_FIRST_POLAND_DIALOG:
                createWfcSwitchFirstPolandDialog();
                break;
        }
    }

    private void createWfcSwitchFirstPolandDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = "";
        p.mView = createWfcSwitchFirstPolandDialogView();
        p.mPositiveButtonText = getString(R.string.wifi_calling_help_button);
        p.mPositiveButtonListener = this;
        setupAlert();
    }

    private View createWfcSwitchFirstPolandDialogView() {
        View view = getLayoutInflater().inflate(R.layout.wifi_calling_help_dialog_activity, null);
        TextView secPartDesc = (TextView) view.findViewById(R.id.dialogSecondPartDesc);
        secPartDesc.setMovementMethod(LinkMovementMethod.getInstance());
        return view;
    }

    private void createWfcSwitchFirstDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = "";
        p.mMessage = getString(R.string.wifi_calling_on_explanation);
        p.mPositiveButtonText = getString(R.string.skip);
        p.mPositiveButtonListener = null;
        p.mNegativeButtonListener = null;
        setupAlert();
    }

    private void createWfcHelpButtonDialog() {
        String msgStr = getString(R.string.wifi_calling_help_explanation);
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.wifi_calling_help);
        p.mMessage = msgStr;
        p.mPositiveButtonText = getString(R.string.accept);
        p.mPositiveButtonListener = null;
        setupAlert();
    }

    private void createWfcHelpButtonPolandDialog() {
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = "";
        p.mView = CreateWfcSettingsView();
        setupAlert();
    }

    public View CreateWfcSettingsView(){
        ActionBar mActionBar = getActionBar();
        if(mActionBar != null){
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setTitle(R.string.wificalling_help_title);
        }
        View view = getLayoutInflater().inflate(R.xml.wifi_calling_help, null);
        TextView content1 = (TextView) view.findViewById(R.id.content1);
        content1.setText(R.string.wificalling_help_content_one);
        TextView content2 = (TextView) view.findViewById(R.id.content2);
        content2.setText(getResources().getString(R.string.wificalling_help_content_three));
        content2.setMovementMethod(LinkMovementMethod.getInstance());
        TextView textView = (TextView) view.findViewById(R.id.settings);
        textView.setVisibility(View.GONE);
        return view;
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE || button == DialogInterface.BUTTON_NEGATIVE) {
            finish();
            return;
        }
    }
}
