package tk.crazysoft.ego.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
    private static final String PREFERENCE_FILE_KEY = "tk.crazysoft.ego.PREFERENCE_FILE_KEY";

    public static final String PREFERENCE_IMPORT_ADDRESSES = "pref_key_import_addresses";
    public static final String PREFERENCE_IMPORT_HOSPITALS = "pref_key_import_hospitals";
    public static final String PREFERENCE_IMPORT_DOCTORS = "pref_key_import_doctors";
    public static final String PREFERENCE_IMPORT_USE_SD = "pref_key_import_use_sd";

    public static final String PREFERENCE_MAP_LATITUDE = "pref_key_map_latitude";
    public static final String PREFERENCE_MAP_LONGITUDE = "pref_key_map_longitude";

    public static final String PREFERENCE_NAVIGATION_API = "pref_key_navigation_api";

    public static final String PREFERENCE_HOSPITALS_DOCTORS_TAKEOVER = "pref_key_hospitals_doctors_takeover";

    private final Context context;
    private final SharedPreferences preferences;

    public Preferences(Context context) {
        this.context = context;
        preferences = getPreferences(context);
    }

    public static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean getImportUseSd() {
        return preferences.getBoolean(PREFERENCE_IMPORT_USE_SD, true);
    }

    public double getMapLatitude() {
        return Double.longBitsToDouble(preferences.getLong(PREFERENCE_MAP_LATITUDE, 0));
    }

    public double getMapLongitude() {
        return Double.longBitsToDouble(preferences.getLong(PREFERENCE_MAP_LONGITUDE, 0));
    }

    public String getNavigationApi() {
        return preferences.getString(PREFERENCE_NAVIGATION_API, null);
    }

    public Time getHospitalsDoctorsTakeover() {
        int preference = preferences.getInt(PREFERENCE_HOSPITALS_DOCTORS_TAKEOVER, 0);
        int hour = preference / 60;
        int minute = preference - hour * 60;
        return new Time(hour, minute);
    }

    public class Time {
        private int hour;
        private int minute;

        public Time(int hour, int minute) {
            this.hour = hour % 24;
            this.minute = minute % 60;
        }

        public int getHour() {
            return hour;
        }

        public int getMinute() {
            return minute;
        }

        public int toInt() {
            return hour * 60 + minute;
        }
    }
}
