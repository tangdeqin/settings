package tct.func;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.LinearLayout;

import com.android.settings.SettingsActivity;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import android.app.FragmentManager;
import com.android.settings.R;
/**
 * Setting entry activity for Func
 * Created by TCTHZ(Jie.Zeng), on 20171211 Solution-
 */

public class FuncSettingsActivity extends SettingsActivity implements
        PreferenceFragment.OnPreferenceStartFragmentCallback {
    private static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";
    private static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";
    private static final String EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String initialFragmentName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);

        if (savedInstanceState == null) {
            if (initialFragmentName != null) {
                setTitleFromIntent(intent);
                Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                CharSequence initialTitle = intent.getCharSequenceExtra(EXTRA_SHOW_FRAGMENT_TITLE);
                switchToFragment(initialFragmentName, initialArguments, initialTitle);
            } else {
                switchToFragment(FuncSettings.class.getName(), null, null);
            }
        } else {
            setTitleFromIntent(intent);
        }
        android.app.ActionBar mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(initialFragmentName != null);
            mActionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        startActivity(onBuildStartFragmentIntent(this, pref.getFragment(), pref.getExtras(), pref.getTitle()));
        return true;
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    private void setTitleFromIntent(Intent intent) {
        final String initialTitle = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
        setTitle(initialTitle);
    }

    private void switchToFragment(String fragmentName, Bundle args, CharSequence title) {
        Fragment fragment = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(fragment,"");
        if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
    }

    private Intent onBuildStartFragmentIntent(Context context, String fragmentName,
                                              Bundle args, CharSequence title) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(context, FuncSettingsActivity.class);
        intent.putExtra(EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, title);
        return intent;
    }
}
