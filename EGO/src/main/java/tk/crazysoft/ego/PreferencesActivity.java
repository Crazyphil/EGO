package tk.crazysoft.ego;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.BaseAdapter;

import tk.crazysoft.ego.components.AppThemeWatcher;
import tk.crazysoft.ego.io.ExternalStorage;
import tk.crazysoft.ego.preferences.DataImportPreference;
import tk.crazysoft.ego.preferences.Preferences;
import tk.crazysoft.ego.services.DataImportReceiver;
import tk.crazysoft.ego.services.DataImportService;

public class PreferencesActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private DataImportReceiver importReceiver;
    private AppThemeWatcher themeWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher = new AppThemeWatcher(this, savedInstanceState);
            setTheme(getIntent().getIntExtra("theme", R.style.AppTheme));
            themeWatcher.setOnAppThemeChangedListener(new MainActivity.OnAppThemeChangedListener(this));
        }

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        IntentFilter importFilter = new IntentFilter(DataImportService.BROADCAST_ERROR);
        importFilter.addAction(DataImportService.BROADCAST_PROGRESS);
        importFilter.addAction(DataImportService.BROADCAST_RESULT_IMPORT);
        importFilter.addAction(DataImportService.BROADCAST_RESULT_POSTPROCESS);
        importFilter.addAction(DataImportService.BROADCAST_COMPLETED);
        importReceiver = new DataImportReceiver(savedInstanceState);
        LocalBroadcastManager.getInstance(this).registerReceiver(importReceiver, importFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        CheckBoxPreference useSdPreference = (CheckBoxPreference)findPreference(Preferences.PREFERENCE_IMPORT_USE_SD);
        ListPreference apiPreference = (ListPreference)findPreference(Preferences.PREFERENCE_NAVIGATION_API);

        refreshImportFiles();
        refreshImportUseSd(getPreferenceScreen().getSharedPreferences(), useSdPreference);
        refreshNavigationApi(apiPreference);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
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
        importReceiver.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(importReceiver);
        super.onDestroy();
    }

    @Override
    public void startActivity(Intent intent) {
        intent.putExtra("theme", themeWatcher.getCurrentAppTheme());
        super.startActivity(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (key.equals(Preferences.PREFERENCE_IMPORT_USE_SD)) {
            refreshImportUseSd(sharedPreferences, pref);
            refreshImportFiles();
        } else if (key.equals(Preferences.PREFERENCE_NAVIGATION_API)) {
            refreshNavigationApi(pref);
        } else if (key.equals(Preferences.PREFERENCE_HOSPITALS_DOCTORS_TAKEOVER)) {
            refreshHospitalsDoctorsTakeover();
        }
    }

    private void refreshImportFiles() {
        String sdPath = ExternalStorage.getSdCardPath(this);
        if (sdPath == null) {
            sdPath = ExternalStorage.getSdCardPath(this, true);
        }

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

    private void refreshImportUseSd(SharedPreferences sharedPreferences, Preference pref) {
        if (sharedPreferences.getBoolean(Preferences.PREFERENCE_IMPORT_USE_SD, true)) {
            pref.setSummary(getString(R.string.preferences_activity_import_use_sd_summary_checked));
        } else {
            pref.setSummary(getString(R.string.preferences_activity_import_use_sd_summary_unchecked));
        }
    }

    private void refreshNavigationApi(Preference pref) {
        ListPreference apiPreference = (ListPreference)getPreferenceScreen().findPreference(Preferences.PREFERENCE_NAVIGATION_API);
        pref.setSummary(getString(R.string.preferences_activity_navigation_api_summary, apiPreference.getEntry()));
    }

    private void refreshHospitalsDoctorsTakeover() {
        ((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
    }
}
