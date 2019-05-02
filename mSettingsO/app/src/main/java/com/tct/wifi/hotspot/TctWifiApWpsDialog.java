/**
 * To Customize TCT WiFi Tethering WPS menu
 * add on 7031036
 * @author yucheng.luo
 * @Time 2018-09-28
 */

package com.tct.wifi.hotspot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.tct.sdk.base.wifi.TctWifiSettingsUtils;

import com.android.settings.R;

public class TctWifiApWpsDialog extends AlertDialog
        implements DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener {
    public static final String TAG = "WifiApWpsDialog";
    private static final int PUSH_BUTTON = 0;
    private static final int PIN_FROM_CLIENT = 1;

    private int mWpsMode = 0;
    private Context mContext;
    private View mView;
    private Spinner mWpsModeSpinner;
    private TextView mWpsPushButtonMode;

    public TctWifiApWpsDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate, return dialog");

        mView = getLayoutInflater().inflate(R.layout.wifi_ap_wps_dialog, null);
        setView(mView);
        setInverseBackgroundForced(true);
        setTitle(R.string.wifi_tether_wps_connect_title);

        mWpsModeSpinner = ((Spinner) mView.findViewById(R.id.wps_mode));
        mWpsModeSpinner.setOnItemSelectedListener(this);

        mWpsPushButtonMode = (TextView) mView.findViewById(R.id.textview_wps_mode_push);
        Resources res = mContext.getResources();
        String[] modeEntries = res.getStringArray(R.array.wifi_ap_wps_mode_entries);
        mWpsPushButtonMode.setText(" "+modeEntries[0]);
        setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.wifi_connect), this);
        setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.wifi_cancel), this);
        super.onCreate(savedInstanceState);
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            WpsInfo config = new WpsInfo();
            if (mWpsMode == PUSH_BUTTON) {
                config.setup = WpsInfo.PBC;
                config.BSSID = "any";
            } else if (mWpsMode == PIN_FROM_CLIENT) {
                config.setup = WpsInfo.DISPLAY;
                config.pin = ((EditText) mView.findViewById(R.id.pin_edit)).getText().toString();
            }
            TctWifiSettingsUtils.startTctApWps(getContext(),config);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(mWpsModeSpinner)) {
            mWpsMode = position;
            if (mWpsMode == PUSH_BUTTON) {
                mView.findViewById(R.id.type_pin_field).setVisibility(View.GONE);
            } else if (mWpsMode == PIN_FROM_CLIENT) {
                mView.findViewById(R.id.type_pin_field).setVisibility(View.VISIBLE);
            }
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
