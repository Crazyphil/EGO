package tk.crazysoft.ego;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.*;
import tk.crazysoft.ego.io.ExternalStorage;
import tk.crazysoft.ego.preferences.*;
import tk.crazysoft.ego.services.DataImportReceiver;
import tk.crazysoft.ego.services.DataImportService;

public class PreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, new PreferencesFragment()).commit();
        }
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    public static class PreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback
    {
        private DataImportReceiver importReceiver;

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey)
        {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            IntentFilter importFilter = new IntentFilter(DataImportService.BROADCAST_ERROR);
            importFilter.addAction(DataImportService.BROADCAST_PROGRESS);
            importFilter.addAction(DataImportService.BROADCAST_RESULT_IMPORT);
            importFilter.addAction(DataImportService.BROADCAST_RESULT_POSTPROCESS);
            importFilter.addAction(DataImportService.BROADCAST_COMPLETED);
            importReceiver = new DataImportReceiver(savedInstanceState);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(importReceiver, importFilter);
        }

        @Override
        public void onStart() {
            super.onStart();

            CheckBoxPreference useSdPreference = (CheckBoxPreference)findPreference(Preferences.PREFERENCE_IMPORT_USE_SD);
            ListPreference apiPreference = (ListPreference)findPreference(Preferences.PREFERENCE_NAVIGATION_API);
            ListPreference viewPreference = (ListPreference)findPreference(Preferences.PREFERENCE_HOSPITALS_DOCTORS_VIEW);

            refreshImportFiles();
            refreshNavigationApi(apiPreference);
            refreshHospitalsDoctorsView(viewPreference);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            importReceiver.onSaveInstanceState(outState);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(importReceiver);
        }

        @Override
        public Fragment getCallbackFragment() {
            return this;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            if (key.equals(Preferences.PREFERENCE_IMPORT_USE_SD)) {
                refreshImportFiles();
            } else if (key.equals(Preferences.PREFERENCE_NAVIGATION_API)) {
                refreshNavigationApi(pref);
            } else if (key.equals(Preferences.PREFERENCE_HOSPITALS_DOCTORS_VIEW)) {
                refreshHospitalsDoctorsView(pref);
            } else if (key.equals(Preferences.PREFERENCE_HOSPITALS_DOCTORS_TAKEOVER)) {
                refreshHospitalsDoctorsTakeover();
            }
        }

        @Override
        public boolean onPreferenceDisplayDialog(@NonNull PreferenceFragmentCompat caller, Preference pref) {
            PreferenceDialogFragmentCompat fragment = null;
            if (pref.getKey().equals(Preferences.PREFERENCE_MAP_ADDRESS)) {
                fragment = AddressPreferenceDialogFragment.newInstance((AddressPreference) pref);
            } else if (pref.getKey().equals(Preferences.PREFERENCE_HOSPITALS_DOCTORS_TAKEOVER)) {
                fragment = TimePickerPreferenceDialogFragment.newInstance((TimePickerPreference) pref);
            }

            if (fragment != null) {
                fragment.setTargetFragment(this, 0);
                fragment.show(getFragmentManager(), "settingsDialog");
                return true;
            }
            return false;
        }

        private void refreshImportFiles() {
            String sdPath = ExternalStorage.getSdCardPath(getActivity());

            DataImportPreference addressPreference = (DataImportPreference)findPreference(Preferences.PREFERENCE_IMPORT_ADDRESSES);
            DataImportPreference hospitalsPreference = (DataImportPreference)findPreference(Preferences.PREFERENCE_IMPORT_HOSPITALS);
            DataImportPreference doctorsPreference = (DataImportPreference)findPreference(Preferences.PREFERENCE_IMPORT_DOCTORS);

            addressPreference.setSummary(String.format(getString(R.string.preferences_activity_import_addresses_summary), sdPath));
            addressPreference.setBroadcastReceiver(importReceiver);
            hospitalsPreference.setSummary(String.format(getString(R.string.preferences_activity_import_hospitals_summary), sdPath));
            hospitalsPreference.setBroadcastReceiver(importReceiver);
            doctorsPreference.setSummary(String.format(getString(R.string.preferences_activity_import_doctors_summary), sdPath));
            doctorsPreference.setBroadcastReceiver(importReceiver);
        }

        private void refreshNavigationApi(Preference pref) {
            ListPreference apiPreference = (ListPreference)getPreferenceScreen().findPreference(Preferences.PREFERENCE_NAVIGATION_API);
            pref.setSummary(getString(R.string.preferences_activity_navigation_api_summary, apiPreference.getEntry()));
        }

        private void refreshHospitalsDoctorsView(Preference pref) {
            ListPreference viewPreference = (ListPreference)getPreferenceScreen().findPreference(Preferences.PREFERENCE_HOSPITALS_DOCTORS_VIEW);
            pref.setSummary(getString(R.string.preferences_activity_hospitals_doctors_view_summary, viewPreference.getEntry()));
        }

        private void refreshHospitalsDoctorsTakeover() {
            // FIXME: getRootAdapter() not available in PreferenceScreen of support-v7
            //((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
        }
    }
}
