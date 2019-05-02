package  com.android.settings.fingerprint.ex;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.tct.sdk.base.fingerprint.FingerprintConstants;

/**
 * Copyright (C) 2017 Tcl Corporation Limited
 * Created by jinlong.lu on 18-3-22
 */
public class FingerprintNavigateHomeSettings extends SubSettings {
    private static final String TAG = "FingerprintNavigateHomeSettings";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FingerprintNavigateHomeFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintNavigateHomeFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.func_navigate_home);
        setTitle(msg);
    }

    public static class FingerprintNavigateHomeFragment extends SettingsPreferenceFragment implements
            OnClickListener {

        private static final String TAG = "FingerprintNavigateHomeSettings";
        public static final int NAVIGATE_TYPE_CAR = 1;
        public static final int NAVIGATE_TYPE_TRANSIT = 2;
        public static final int NAVIGATE_TYPE_WALK = 3;
        public static final int NAVIGATE_TYPE_BIKING = 4;

        private CheckablelView car;
        private CheckablelView transit;
        private CheckablelView walk;
        private CheckablelView biking;
        private Button cancelButton;
        private Button saveButton;
        private EditText mEditText;
        private TextWatcher mTextWatcher;
        private View carIndicator;
        private View transitIndicator;
        private View walkIndicator;
        private View bikingIndicator;

        private Context mContext;
        private ContentResolver cr;
        private Activity mActivity;
        private String mOldAddressValue;
        private static final String NAVIDATETYPE = FingerprintConstants.TCT_FINGERPRINT_NAVIGATE_TYPE;
        private static final String HOME_ADDRESS = FingerprintConstants.TCT_FINGERPRINT_HOME_ADDRESS;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = getActivity();
            cr = getContentResolver();
            mActivity = getActivity();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            return inflater.inflate(R.layout.fingerprint_navigate_home, container, false);

        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            initView();
        }

        public void initView() {
            car = (CheckablelView) mActivity.findViewById(R.id.car);
            transit = (CheckablelView) mActivity.findViewById(R.id.transit);
            walk = (CheckablelView) mActivity.findViewById(R.id.walk);
            biking = (CheckablelView) mActivity.findViewById(R.id.biking);

            cancelButton = (Button) mActivity.findViewById(R.id.cancel);
            saveButton = (Button) mActivity.findViewById(R.id.save);
            mEditText = (EditText) mActivity
                    .findViewById(R.id.home_address_edit_text);
            carIndicator = mActivity.findViewById(R.id.car_indicator);
            transitIndicator = mActivity.findViewById(R.id.transit_indicator);
            walkIndicator = mActivity.findViewById(R.id.walk_indicator);
            bikingIndicator = mActivity.findViewById(R.id.walk_indicator);

            int navigateType = Settings.System.getInt(cr, NAVIDATETYPE, 1);

            if (navigateType == NAVIGATE_TYPE_CAR) {
                setCar();
            } else if (navigateType == NAVIGATE_TYPE_TRANSIT) {
                setTransit();
            } else if (navigateType == NAVIGATE_TYPE_BIKING) {
                setBiking();
            } else {
                setWalk();
            }
            car.setOnClickListener(this);
            transit.setOnClickListener(this);
            walk.setOnClickListener(this);
            biking.setOnClickListener(this);
            cancelButton.setOnClickListener(this);
            saveButton.setOnClickListener(this);
            mTextWatcher = new TextWatcher() {

                @Override
                public void afterTextChanged(Editable arg0) {
                    if (arg0.toString() == null || "".equals(arg0.toString())) {
                        sealButton();
                    } else {
                        openButton();
                    }

                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1,
                                              int arg2, int arg3) {
                }

                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
                }

            };
            mEditText.addTextChangedListener(mTextWatcher);
            String editContent = Settings.System.getString(cr, HOME_ADDRESS);
            mOldAddressValue = editContent;
            if (editContent != null && !"".equals(editContent)) {
                mEditText.setText(editContent);
                mEditText.setSelection(editContent.length());
            }
            if (mEditText.getText().toString() == null
                    || "".equals(mEditText.getText().toString())) {
                sealButton();
            }

        }

        public void sealButton() {
            saveButton.setClickable(false);
            saveButton.setEnabled(false);
            saveButton.setTextColor(Color.parseColor("#ff9a9a9a"));
        }

        public void openButton() {
            saveButton.setClickable(true);
            saveButton.setEnabled(true);
            saveButton.setTextColor(Color.parseColor("#2196f3"));
        }

        @Override
        public void onClick(View arg0) {
            switch (arg0.getId()) {
                case R.id.car:
                    if (!car.isChecked()) {
                        setCar();
                    }
                    break;
                case R.id.transit:
                    if (!transit.isChecked()) {
                        setTransit();
                    }
                    break;
                case R.id.walk:
                    if (!walk.isChecked()) {
                        setWalk();
                    }
                    break;
                case R.id.biking:
                    if (!biking.isChecked()) {
                        setBiking();
                    }
                    break;
                case R.id.cancel:
                    Settings.System.putString(cr, HOME_ADDRESS, mOldAddressValue);
                    setResult(mActivity.RESULT_OK);
                    mActivity.onBackPressed();
                    break;
                case R.id.save:
                    Settings.System.putString(cr, HOME_ADDRESS, mEditText.getText().toString());
                    setResult(mActivity.RESULT_OK);
                    mActivity.onBackPressed();
                    break;
                default:
                    break;
            }

        }

        public void setCar() {
            car.setImageResource(R.drawable.navigation_car_blue);
            transit.setImageResource(R.drawable.navigation_transi_grey);
            walk.setImageResource(R.drawable.navigation_walk_grey);
            biking.setImageResource(R.drawable.navitation_ic_activity_biking_grey);

            carIndicator.setBackgroundColor(Color.parseColor("#0db6ff"));
            carIndicator.setAlpha(1.0f);
            transitIndicator.setAlpha(0.0f);
            walkIndicator.setAlpha(0.0f);
            bikingIndicator.setAlpha(0.0f);

            car.setChecked(true);
            transit.setChecked(false);
            walk.setChecked(false);
            biking.setChecked(false);

            Settings.System.putInt(cr, NAVIDATETYPE, NAVIGATE_TYPE_CAR);
        }

        public void setTransit() {
            car.setImageResource(R.drawable.navigation_car_grey);
            transit.setImageResource(R.drawable.navigation_transi_blue);
            walk.setImageResource(R.drawable.navigation_walk_grey);
            biking.setImageResource(R.drawable.navitation_ic_activity_biking_grey);

            transitIndicator.setBackgroundColor(Color.parseColor("#0db6ff"));
            transitIndicator.setAlpha(1.0f);
            carIndicator.setAlpha(0.0f);
            walkIndicator.setAlpha(0.0f);
            bikingIndicator.setAlpha(0.0f);

            car.setChecked(false);
            transit.setChecked(true);
            walk.setChecked(false);
            biking.setChecked(false);

            Settings.System.putInt(cr, NAVIDATETYPE, NAVIGATE_TYPE_TRANSIT);
        }

        public void setWalk() {
            car.setImageResource(R.drawable.navigation_car_grey);
            transit.setImageResource(R.drawable.navigation_transi_grey);
            walk.setImageResource(R.drawable.navigation_walk_blue);
            biking.setImageResource(R.drawable.navitation_ic_activity_biking_grey);

            walkIndicator.setBackgroundColor(Color.parseColor("#0db6ff"));
            walkIndicator.setAlpha(1.0f);
            carIndicator.setAlpha(0.0f);
            transitIndicator.setAlpha(0.0f);
            bikingIndicator.setAlpha(0.0f);

            car.setChecked(false);
            transit.setChecked(false);
            walk.setChecked(true);
            biking.setChecked(false);

            Settings.System.putInt(cr, NAVIDATETYPE, NAVIGATE_TYPE_WALK);
        }

        public void setBiking() {
            car.setImageResource(R.drawable.navigation_car_grey);
            transit.setImageResource(R.drawable.navigation_transi_grey);
            walk.setImageResource(R.drawable.navigation_walk_grey);
            biking.setImageResource(R.drawable.navitation_ic_activity_biking_blue);

            bikingIndicator.setBackgroundColor(Color.parseColor("#0db6ff"));
            bikingIndicator.setAlpha(1.0f);
            walkIndicator.setAlpha(0.0f);
            carIndicator.setAlpha(0.0f);
            transitIndicator.setAlpha(0.0f);

            car.setChecked(false);
            transit.setChecked(false);
            walk.setChecked(false);
            biking.setChecked(true);
            Settings.System.putInt(cr, NAVIDATETYPE, NAVIGATE_TYPE_BIKING);
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.FINGERPRINT;
        }
    }

}
