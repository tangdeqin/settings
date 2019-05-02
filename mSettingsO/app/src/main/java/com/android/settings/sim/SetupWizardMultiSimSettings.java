package com.android.settings.sim;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;

import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.settings.ButtonBarHandler;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.SettingsActivity;
import android.widget.ScrollView;
import com.android.setupwizardlib.GlifLayout;

public class SetupWizardMultiSimSettings extends SettingsActivity {
	
	private static final String TAG = "SetupWizardMultiSimSettings";
	
    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
	//setTitleSpaceVisibility(View.GONE);//add for defect 5473595 by liu.zheng.hz at 20171027
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupWizardMultiSimFragment.class.getName().equals(fragmentName);
    }
    
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        if (!modIntent.hasExtra(EXTRA_SHOW_FRAGMENT)) {
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, SetupWizardMultiSimFragment.class.getName());
        }
        return modIntent;
    }
    
    public static class SetupWizardMultiSimFragment extends SimSettings
    	implements View.OnClickListener{
    	private Button mNextButton;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final GlifLayout layout = (GlifLayout) inflater.inflate(
                    R.layout.setup_wizard_multi_sim, container, false);


            View view = super.onCreateView(inflater, container, savedInstanceState);
            ScrollView multiSim = (ScrollView)layout.findViewById(R.id.setupwizard_content);
            multiSim.addView(view);
            //SystemBarHelper.hideSystemBars(getActivity().getWindow());
            return layout;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            //SetupWizardUtils.setImmersiveMode(getActivity());
            mNextButton = (Button) view.findViewById(R.id.next_button);
            mNextButton.setOnClickListener(this);
        }
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.next_button:
                	Activity activity = getActivity();
                    if (activity != null) {
                    	activity.setResult(RESULT_OK);
                    	activity.finish();
                    }
                break;

            }
        }





    }
}
