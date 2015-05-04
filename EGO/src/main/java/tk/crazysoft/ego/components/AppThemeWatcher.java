package tk.crazysoft.ego.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import tk.crazysoft.ego.R;
import tk.crazysoft.ego.preferences.Preferences;

public class AppThemeWatcher extends BroadcastReceiver {
    public static final int THEME_DAY = 0, THEME_NIGHT = 1;

    private final Context context;
    private Location location;
    private int currentTheme;
    private boolean forcedTheme;
    private OnAppThemeChangedListener listener;

    public AppThemeWatcher(Context context, Bundle savedInstanceState) {
        this.context = context;

        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        android.location.Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            this.location = new Location(location.getLatitude(), location.getLongitude());
        } else {
            Preferences preferences = new Preferences(context);
            this.location = new Location(preferences.getMapLatitude(), preferences.getMapLongitude());
        }

        if (savedInstanceState != null) {
            currentTheme = savedInstanceState.getInt("currentTheme");
            forcedTheme = savedInstanceState.getBoolean("forcedTheme");
        } else {
            calculateThemeByTime(true);
        }
    }

    public void onResume() {
        IntentFilter timeFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
        timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        context.registerReceiver(this, timeFilter);
        calculateThemeByTime(false);
    }

    public void onPause() {
        try {
          context.unregisterReceiver(this);
        } catch (IllegalArgumentException e) { }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("currentTheme", currentTheme);
        outState.putBoolean("forcedTheme", forcedTheme);
    }

    public void setOnAppThemeChangedListener(OnAppThemeChangedListener listener) {
        this.listener = listener;
    }

    public int getCurrentAppTheme() {
        int curTheme = forcedTheme ? THEME_NIGHT - currentTheme : currentTheme;
        if (curTheme == THEME_DAY) {
            return R.style.AppTheme;
        }
        return R.style.AppTheme_Dark;
    }

    public void toggleAppTheme() {
        forcedTheme = !forcedTheme;
        fireAppThemeChangedListener();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_TIME_TICK) || intent.getAction().equals(Intent.ACTION_TIME_CHANGED) || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
            calculateThemeByTime(false);
        }
    }

    private void calculateThemeByTime(boolean setBaseline) {
        Calendar now = GregorianCalendar.getInstance();
        SunriseSunsetCalculator calc = new SunriseSunsetCalculator(location, TimeZone.getDefault());
        Calendar sunrise = calc.getOfficialSunriseCalendarForDate(now);
        Calendar sunset = calc.getOfficialSunsetCalendarForDate(now);
        int theme = THEME_DAY;
        if (now.before(sunrise) || now.after(sunset)) {
            theme = THEME_NIGHT;
        }

        if (!setBaseline && theme != currentTheme) {
            forcedTheme = false;
            currentTheme = theme;
            fireAppThemeChangedListener();
        } else if (setBaseline) {
            currentTheme = theme;
        }
    }

    private void fireAppThemeChangedListener() {
        if (listener != null) {
            listener.onAppThemeChanged(getCurrentAppTheme());
        }
    }

    interface OnAppThemeChangedListener {
        void onAppThemeChanged(int newTheme);
    }
}
