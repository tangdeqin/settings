package com.android.settings.nfc;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v14.preference.SwitchPreference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.RestrictedPreference;

import com.android.settings.overlay.FeatureFactory;
import android.provider.SearchIndexableResource;
import com.android.settings.dashboard.SummaryLoader;
import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.dashboard.SummaryLoader;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//Begin added by jianhao.zeng for defect:5902768 on 2018/02/09
import android.content.res.Resources;
import com.android.settings.search.SearchIndexableRaw;
//End added by jianhao.zeng for defect:5902768 on 2018/02/09
public class NfcSettings 
        extends SettingsPreferenceFragment 
        implements Indexable {
    private static final String TAG = "NfcSettings";
    private static final String KEY_TOGGLE_NFC = "toggle_nfc";
    private static final String KEY_ANDROID_BEAM = "android_beam_settings";
    
    //private SettingsActivity mActivity;
    private NfcSwitcher mEnabler;
    private NfcAdapter mNfcAdapter;

    private RestrictedPreference mAndroidBeam;
    private SwitchPreference mNfcSwitcher;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_NETWORK_CATEGORY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.android_nfc_settings);

        SettingsActivity activity = (SettingsActivity) getActivity();
        initPreferences();
        mEnabler = new NfcSwitcher(activity, mNfcSwitcher, mAndroidBeam);
        mNfcAdapter = mEnabler.Nfc;
        if (mNfcAdapter == null) {
            Log.e(TAG, "Nfc adapter is null, finish Nfc settings");
            getActivity().finish();
            return;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mEnabler.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mEnabler.pause();
    }

    private void initPreferences() {
        mNfcSwitcher = (SwitchPreference)findPreference(KEY_TOGGLE_NFC);
        mAndroidBeam = (RestrictedPreference)findPreference(KEY_ANDROID_BEAM);
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        /*if (preference.equals(mAndroidBeam)) {
            startFragment(this, "com.android.settings.nfc.AndroidBeam", 0, 0, null);
        } else */if (preference.equals(mNfcSwitcher)) {
            return mEnabler.onPreferenceChange(mNfcSwitcher, mNfcSwitcher.isChecked());
        } 
        return super.onPreferenceTreeClick(preference);
    }
    
    @VisibleForTesting
    static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private final NfcPreferenceController mNfcPreferenceController;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mNfcPreferenceController = new NfcPreferenceController(context);
        }


        @Override
        public void setListening(boolean listening) {
            if (listening) {
                if (mNfcPreferenceController.isAvailable()) {
                    mSummaryLoader.setSummary(this,
                            mContext.getString(R.string.nfc_quick_toggle_title));
                }/* else {
                    mSummaryLoader.setSummary(this, mContext.getString(
                            R.string.connected_devices_dashboard_no_nfc_summary));
                }*/
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {

            return new SummaryProvider(activity, summaryLoader);
        }
    };

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                /*Begin deleted by jianhao.zeng for defect:5902768 on 2018/02/09
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.android_nfc_settings;
                    return Arrays.asList(sir);
                }*/

                /*@Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    PackageManager pm = context.getPackageManager();
                    if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                        keys.add(NfcPreferenceController.KEY_TOGGLE_NFC);
                        keys.add(NfcPreferenceController.KEY_ANDROID_BEAM_SETTINGS);
                    }
                    return keys;
                }
                End deleted by jianhao.zeng for defect:5902768 on 2018/02/09*/
                //Begin added by jianhao.zeng for defect:5902768 on 2018/02/09
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                    final Resources res = context.getResources();
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.nfc_quick_toggle_title);
                    data.screenTitle = res.getString(R.string.nfc_quick_toggle_title);
                    data.iconResId = R.drawable.ic_nfc;
                    result.add(data);
                    return result;
                }
                //End added by jianhao.zeng for defect:5902768 on 2018/02/09
            };

}
