package tk.crazysoft.ego.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import tk.crazysoft.ego.R;
import tk.crazysoft.ego.preferences.Preferences;

public class AppThemeWatcher extends BroadcastReceiver implements SensorEventListener {
    public static final int THEME_DAY = 0, THEME_NIGHT = 1;
    // Switch to night theme somewhere between a cloudy day and full moon
    public static final float LIGHT_DARKNESS = 5.0f;

    private final Context context;
    private final SensorManager manager;

    private Location location;
    private int currentTheme;
    private float currentLux;
    private OnAppThemeChangedListener listener;

    public AppThemeWatcher(Context context, Bundle savedInstanceState) {
        this.context = context;
        manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

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
        } else {
            calculateThemeByTime(true);
        }
    }

    public void onResume() {
        Sensor lightSensor = manager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor != null) {
            // Since Android 2.3.3 (level 10), registerListener automatically calls the listener to get an initial value
            manager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            currentLux = -1;
            IntentFilter timeFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
            timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
            timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            context.registerReceiver(this, timeFilter);
            calculateThemeByTime(false);
        }
    }

    public void onPause() {
        try {
            if (currentLux >= 0) {
                manager.unregisterListener(this);
            } else {
                context.unregisterReceiver(this);
            }
        } catch (IllegalArgumentException e) { }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("currentTheme", currentTheme);
    }

    public void setOnAppThemeChangedListener(OnAppThemeChangedListener listener) {
        this.listener = listener;
    }

    public int getCurrentAppTheme() {
        if (currentTheme == THEME_DAY) {
            return R.style.AppTheme;
        }
        return R.style.AppTheme_Dark;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) {
            return;
        }

        currentLux = event.values[0];
        //Log.d("tk.crazysoft.ego.components.AppThemeWatcher", "Brightness changed to " + currentLux + " lux");
        calculateThemeByBrightness();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

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
            currentTheme = theme;
            //Log.d("tk.crazysoft.ego.components.AppThemeWatcher", "Setting theme to " + currentTheme + " because of time");
            fireAppThemeChangedListener();
        } else if (setBaseline) {
            currentTheme = theme;
        }
    }

    private void calculateThemeByBrightness() {
        if (currentTheme == THEME_DAY && currentLux < LIGHT_DARKNESS) {
            currentTheme = THEME_NIGHT;
            //Log.d("tk.crazysoft.ego.components.AppThemeWatcher", "Setting theme to " + currentTheme + " because it's dark");
            fireAppThemeChangedListener();
        } else if (currentTheme == THEME_NIGHT && currentLux >= LIGHT_DARKNESS) {
            currentTheme = THEME_DAY;
            //Log.d("tk.crazysoft.ego.components.AppThemeWatcher", "Setting theme to " + currentTheme + " because it's light");
            fireAppThemeChangedListener();
        }
    }

    private void fireAppThemeChangedListener() {
        if (listener != null) {
            listener.onAppThemeChanged(getCurrentAppTheme());
        }
    }

    public interface OnAppThemeChangedListener {
        public void onAppThemeChanged(int newTheme);
    }
}
