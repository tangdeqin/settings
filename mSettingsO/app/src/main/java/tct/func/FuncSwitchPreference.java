/* ----------|----------------------|----------------------|----------------- */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* -------------------------------------------------------------------------- */
/* 12/10/2014|     guangyu.feng     |      FR-849920       | Going directly   */
/*           |                      |                      | into a specific  */
/*           |                      |                      | function of      */
/*           |                      |                      | an app           */
/*           |                      |                      |                  */
/******************************************************************************/

package tct.func;

import android.content.Context;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.settings.R;

public class FuncSwitchPreference extends Preference {

    private static final String TAG = "FuncSwitchPreference";

    private boolean mChecked = true;
    private Context mContext;
    private CompoundButton.OnCheckedChangeListener mSwitchChangeListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
            mChecked = !mChecked;
            Settings.System.putInt(mContext.getContentResolver(), "isFuncOn", (mChecked ? 1 : 0));
            arg0.setChecked(mChecked);

        }

    };

    public void setmSwitchChangeListener(
            CompoundButton.OnCheckedChangeListener mSwitchChangeListener) {
        this.mSwitchChangeListener = mSwitchChangeListener;
    }

    public FuncSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initSwitchState();
        setLayoutResource(R.layout.func_switch_preference);
    }

    public FuncSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initSwitchState();
        setLayoutResource(R.layout.func_switch_preference);
    }

    public void initSwitchState() {
        Log.v(TAG,"initSwitchState");
        //Begin modified by yongjun.zou for XR P23141 on 2018/09/01
        int isFuncOn = mContext.getResources().getBoolean(R.bool.def_FunC_on) == true ? 1 :0;
        if (1 == Settings.System.getInt(mContext.getContentResolver(), "isFuncOn", isFuncOn)) {
            mChecked = true;
        } else {
            mChecked = false;
        }
        //End modified by yongjun.zou for XR P23141 on 2018/09/01
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Switch switch_ = (Switch) view.findViewById(R.id.switch_);
        Log.v(TAG,"onBindView");
        if (mSwitchChangeListener != null && switch_ != null) {
            switch_.setClickable(true);
            switch_.setChecked(mChecked);
            switch_.setOnCheckedChangeListener(mSwitchChangeListener);
        }
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        notifyChanged();
    }

    public boolean isChecked() {
        return mChecked;
    }
}
