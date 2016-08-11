package tk.crazysoft.ego.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Preferences {
    private static final String PREFERENCE_FILE_KEY = "tk.crazysoft.ego.PREFERENCE_FILE_KEY";

    public static final String PREFERENCE_IMPORT_ADDRESSES = "pref_key_import_addresses";
    public static final String PREFERENCE_IMPORT_HOSPITALS = "pref_key_import_hospitals";
    public static final String PREFERENCE_IMPORT_DOCTORS = "pref_key_import_doctors";
    public static final String PREFERENCE_IMPORT_USE_SD = "pref_key_import_use_sd";

    public static final String PREFERENCE_MAP_ADDRESS = "pref_key_map_address";

    public static final String PREFERENCE_NAVIGATION_API = "pref_key_navigation_api";

    public static final String PREFERENCE_INTERNAL_NAVIGATION_ROTATE = "pref_key_internal_navigation_rotate";

    public static final String PREFERENCE_HOSPITALS_DOCTORS_VIEW = "pref_key_hospitals_doctors_view";
    public static final String PREFERENCE_HOSPITALS_DOCTORS_TAKEOVER = "pref_key_hospitals_doctors_takeover";

    public static final int HOSPITALS_DOCTORS_VIEW_HOSPITALS = 1;
    public static final int HOSPITALS_DOCTORS_VIEW_DOCTORS = 2;

    // Map latitude/longitude were replaced by address preference
    public static final String PREFERENCE_LEGACY_MAP_LATITUDE = "pref_key_map_latitude";
    public static final String PREFERENCE_LEGACY_MAP_LONGITUDE = "pref_key_map_longitude";

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
        String coords = preferences.getString(PREFERENCE_MAP_ADDRESS, null);
        if (coords != null) {
            return Double.parseDouble(coords.split(",")[0]);
        }
        return Double.longBitsToDouble(preferences.getLong(PREFERENCE_LEGACY_MAP_LATITUDE, 0));
    }

    public double getMapLongitude() {
        String coords = preferences.getString(PREFERENCE_MAP_ADDRESS, null);
        if (coords != null) {
            return Double.parseDouble(coords.split(",")[1]);
        }
        return Double.longBitsToDouble(preferences.getLong(PREFERENCE_LEGACY_MAP_LONGITUDE, 0));
    }

    public String getNavigationApi() {
        return preferences.getString(PREFERENCE_NAVIGATION_API, null);
    }

    public boolean getInternalNavigationRotate() {
        return preferences.getBoolean(PREFERENCE_INTERNAL_NAVIGATION_ROTATE, true);
    }

    public int getHospitalsDoctorsView() {
        return Integer.parseInt(preferences.getString(PREFERENCE_HOSPITALS_DOCTORS_VIEW, "3"));
    }

    public Time getHospitalsDoctorsTakeover() {
        int preference = preferences.getInt(PREFERENCE_HOSPITALS_DOCTORS_TAKEOVER, 0);
        int hour = preference / 60;
        int minute = preference - hour * 60;
        return new Time(hour, minute);
    }

    public static class Time {
        private int hour;
        private int minute;

        public Time(int hour, int minute) {
            this.hour = hour % 24;
            this.minute = minute % 60;
        }

        public int getHour() {
            return hour;
        }

        public void setHour(int value) {
            hour = value;
        }

        public int getMinute() {
            return minute;
        }

        public void setMinute(int value) {
            minute = value;
        }

        public int toInt() {
            return hour * 60 + minute;
        }

        public Date toDate() {
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return new Date(calendar.getTimeInMillis());
        }

        public static Time fromInt(int value) {
            int hour = value / 60;
            int minute = value - hour * 60;
            return new Time(hour, minute);
        }

        public static Date toDate(int value) {
            Time time = Time.fromInt(value);
            return time.toDate();
        }
    }
}
