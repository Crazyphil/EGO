package tk.crazysoft.ego.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
    private static final String PREFERENCE_FILE_KEY = "tk.crazysoft.ego.PREFERENCE_FILE_KEY";

    public static final String PREFERENCE_IMPORT_ADDRESSES = "pref_key_import_addresses";
    public static final String PREFERENCE_IMPORT_HOSPITALS = "pref_key_import_hospitals";
    public static final String PREFERENCE_IMPORT_DOCTORS = "pref_key_import_doctors";

    public static final String PREFERENCE_MAP_LATITUDE = "pref_key_map_latitude";
    public static final String PREFERENCE_MAP_LONGITUDE = "pref_key_map_longitude";

    public static final String PREFERENCE_NAVIGATION_API = "pref_key_navigation_api";

    private final Context context;
    private final SharedPreferences preferences;

    public Preferences(Context context) {
        this.context = context;
        preferences = getPreferences(context);
    }

    public static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
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
}
