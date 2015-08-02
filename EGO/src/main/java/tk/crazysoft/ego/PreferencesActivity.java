package tk.crazysoft.ego;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.widget.BaseAdapter;

import tk.crazysoft.ego.components.AppThemeWatcher;
import tk.crazysoft.ego.io.ExternalStorage;
import tk.crazysoft.ego.preferences.DataImportPreference;
import tk.crazysoft.ego.preferences.Preferences;
import tk.crazysoft.ego.services.DataImportReceiver;
import tk.crazysoft.ego.services.DataImportService;

public class PreferencesActivity extends ActionBarActivity {
    private AppThemeWatcher themeWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher = new AppThemeWatcher(this, savedInstanceState);
            setTheme(getIntent().getIntExtra("theme", R.style.AppTheme));
            themeWatcher.setOnAppThemeChangedListener(new MainActivity.OnAppThemeChangedListener(this));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, new PreferencesFragment()).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher.onPause();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher.onSaveInstanceState(outState);
        }
    }

    @Override
    public void startActivity(Intent intent) {
        intent.putExtra("theme", themeWatcher.getCurrentAppTheme());
        super.startActivity(intent);
    }

    public static class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        private DataImportReceiver importReceiver;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

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
            ((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
        }
    }
}
