package tk.crazysoft.ego;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.acra.ACRA;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import tk.crazysoft.ego.components.AppThemeWatcher;
import tk.crazysoft.ego.components.TabListener;
import tk.crazysoft.ego.io.Environment4;
import tk.crazysoft.ego.preferences.Preferences;

public class MainActivity extends ActionBarActivity implements AddressFragment.OnAddressClickListener {
    private static final String TAG = MainActivity.class.getName();

    public static final String LAYOUT_MODE_PORTRAIT = "portrait";
    public static final String LAYOUT_MODE_LANDSCAPE = "landscape";
    public static final String LAYOUT_MODE_LANDSCAPE_LARGE = "landscape-large";

    private static final String NAVIGON_PUBLIC_INTENT = "android.intent.action.navigon.START_PUBLIC";
    private static final String NAVIGON_PUBLIC_INTENT_EXTRA_LATITUDE = "latitude";
    private static final String NAVIGON_PUBLIC_INTENT_EXTRA_LONGITUDE = "longitude";
    private static final String NAVIGON_PUBLIC_INTENT_EXTRA_FREE_TEXT_ADDRESS = "free_text_address";

    private Stack<Long> displayedHouses;
    private AppThemeWatcher themeWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set media rescan filter for getting current list of devices
        Environment4.setUseReceiver(getApplicationContext(), true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher = new AppThemeWatcher(this, savedInstanceState);
            setTheme(themeWatcher.getCurrentAppTheme());
            themeWatcher.setOnAppThemeChangedListener(new OnAppThemeChangedListener(this));
        }

        // Initialize default preferences if the app is started for the first time
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        String layoutMode = getString(R.string.layout_mode);
        Fragment mapFragment = getSupportFragmentManager().findFragmentByTag("map");

        if (layoutMode.equals(LAYOUT_MODE_PORTRAIT) || layoutMode.equals(LAYOUT_MODE_LANDSCAPE)) {
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_addresses).setTabListener(new TabListener<AddressFragment>(this, "addresses", AddressFragment.class)), false);
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_map).setTabListener(new TabListener<MapFragment>(this, "map", MapFragment.class)), false);
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_hospitals_doctors).setTabListener(
                    new TabListener<HospitalsDoctorsFragment>(this, "hospitals_doctors", HospitalsDoctorsFragment.class)), false);
        } else {
            setContentView(R.layout.multipane_view);
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_addresses).setTabListener(new TabListener<AddressFragment>(this, R.id.contentLeft, "addresses", AddressFragment.class)), false);
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_hospitals_doctors).setTabListener(
                    new TabListener<HospitalsDoctorsFragment>(this, R.id.contentLeft, "hospitals_doctors", HospitalsDoctorsFragment.class)), false);

            if (mapFragment != null && mapFragment.getId() != R.id.contentRight) {
                getSupportFragmentManager().beginTransaction().remove(mapFragment).commit();
                Fragment.SavedState savedState = getSupportFragmentManager().saveFragmentInstanceState(mapFragment);
                mapFragment = new MapFragment();
                mapFragment.setInitialSavedState(savedState);
            } else if (mapFragment == null) {
                mapFragment = new MapFragment();
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.contentRight, mapFragment, "map").commit();
        }

        if (savedInstanceState != null) {
            String tabTitle = savedInstanceState.getString("tab");
            long[] savedHouses = savedInstanceState.getLongArray("displayedHouses");
            if (savedHouses != null) {
                displayedHouses = new Stack<Long>();
                for (long savedHouse : savedHouses) {
                    displayedHouses.push(savedHouse);
                }
            }

            ActionBar.Tab tab = findTabWithTitle(tabTitle);
            if (tab != null) {
                actionBar.selectTab(tab);
            } else {
                actionBar.selectTab(actionBar.getTabAt(0));
            }
            if (displayedHouses != null) {
                onAddressClick(displayedHouses.peek(), false);
            }
        } else {
            actionBar.selectTab(actionBar.getTabAt(0));
        }
    }

    private ActionBar.Tab findTabWithTitle(String title) {
        for (int i = 0; i < getSupportActionBar().getTabCount(); i++) {
            ActionBar.Tab tab = getSupportActionBar().getTabAt(i);
            if (tab.getText().equals(title)) {
                return tab;
            }
        }
        return null;
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

        outState.putString("tab", getSupportActionBar().getSelectedTab().getText().toString());
        if (displayedHouses != null) {
            Long[] savedHouses = new Long[displayedHouses.size()];
            displayedHouses.toArray(savedHouses);
            outState.putLongArray("displayedHouses", toLongArray(savedHouses));
        } else {
            outState.putLongArray("displayedHouses", null);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher.onSaveInstanceState(outState);
        }
    }

    private long[] toLongArray(Long[] array) {
        long[] longs = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            longs[i] = array[i];
        }
        return longs;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove media rescan filter for getting current list of devices
        Environment4.setUseReceiver(getApplicationContext(), false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if (!isDefaultLauncher()) {
            menu.findItem(R.id.action_launcher).setEnabled(false);
            Log.d(TAG, "This is not the default launcher, disabling launch menu item");
        }
        return true;
    }

    private boolean isDefaultLauncher() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);

        List<ComponentName> activities = new ArrayList<ComponentName>();

        getPackageManager().getPreferredActivities(filters, activities, null);
        for (ComponentName activity : activities) {
            if (getPackageName().equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getSupportFragmentManager().popBackStack();
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setHomeButtonEnabled(false);
                displayedHouses = null;
                break;
            case R.id.action_light:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    themeWatcher.toggleAppTheme();
                }
                break;
            case R.id.action_navigation:
                sendNavigationIntent(this, (String)null);
                break;
            case R.id.action_contacts:
                Intent contactsIntent = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
                startActivity(contactsIntent);
                break;
            case R.id.action_data_management:
                Intent dataManagementIntent = new Intent(this, PreferencesActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    dataManagementIntent.putExtra("theme", themeWatcher.getCurrentAppTheme());
                }
                startActivity(dataManagementIntent);
                break;
            case R.id.action_launcher:
                Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
                launcherIntent.addCategory(Intent.CATEGORY_HOME);
                launcherIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(launcherIntent, null));
                break;
            case R.id.action_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    aboutIntent.putExtra("theme", themeWatcher.getCurrentAppTheme());
                }
                startActivity(aboutIntent);
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (displayedHouses != null) {
            displayedHouses.pop();
            if (displayedHouses.size() > 0) {
                onAddressClick(displayedHouses.peek(), false);
                return;
            } else {
                displayedHouses = null;
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setHomeButtonEnabled(false);
            }
        }
        try {
            super.onBackPressed();
        } catch (IllegalStateException e) {
            if (!BuildConfig.DEBUG) {
                ACRA.getErrorReporter().handleSilentException(e);
            }
            Log.e(TAG, "Caught known fragment transaction error", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != HouseActivity.RESULT_LAYOUT_MODE_CHANGE) {
            displayedHouses = null;
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public void onAddressClick(long id) {
        onAddressClick(id, true);
    }

    private void onAddressClick(long id, boolean addToStack) {
        if (getString(R.string.layout_mode).equals(LAYOUT_MODE_LANDSCAPE_LARGE)) {
            HouseFragment fragment = (HouseFragment)getSupportFragmentManager().findFragmentByTag("house");
            if (fragment == null) {
                fragment = new HouseFragment();
            }
            if (!fragment.isVisible()) {
                getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).replace(R.id.contentRight, fragment, "house").addToBackStack(null).commit();
            }
            fragment.setHouse(id);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            Intent houseIntent = new Intent(this, HouseActivity.class);
            houseIntent.putExtra("id", id);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                houseIntent.putExtra("theme", themeWatcher.getCurrentAppTheme());
            }
            startActivityForResult(houseIntent, 0);
        }

        if (addToStack) {
            if (displayedHouses == null) {
                displayedHouses = new Stack<Long>();
            }
            displayedHouses.push(id);
        }
    }

    public static boolean hasDarkTheme(ContextThemeWrapper context) {
        return getThemeId(context) == R.style.AppTheme_Dark;
    }

    private static int getThemeId(ContextThemeWrapper context) {
        int themeResId = 0;
        try {
            Class<?> clazz = ContextThemeWrapper.class;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                Method method = clazz.getMethod("getThemeResId");
                method.setAccessible(true);
                themeResId = (Integer)method.invoke(context);
            } else {
                Field field = clazz.getDeclaredField("mThemeResource");
                field.setAccessible(true);
                themeResId = field.getInt(context);
            }
        } catch (NoSuchMethodException e) {
            Log.e("MainActivity", "Failed to get theme resource ID", e);
        } catch (NoSuchFieldException e) {
            Log.e("MainActivity", "Failed to get theme resource ID", e);
        } catch (IllegalAccessException e) {
            Log.e("MainActivity", "Failed to get theme resource ID", e);
        } catch (IllegalArgumentException e) {
            Log.e("MainActivity", "Failed to get theme resource ID", e);
        } catch (InvocationTargetException e) {
            Log.e("MainActivity", "Failed to get theme resource ID", e);
        }
        return themeResId;
    }

    public static void sendNavigationIntent(Activity activity, double latitude, double longitude) {
        String intentPreference = new Preferences(activity).getNavigationApi();
        String[] options = activity.getResources().getStringArray(R.array.preferences_activity_navigation_api_values);

        // Build the intent
        Intent navIntent;
        if (intentPreference.equals(options[3])) {  // navigon
            navIntent = new Intent(NAVIGON_PUBLIC_INTENT);
            navIntent.putExtra(NAVIGON_PUBLIC_INTENT_EXTRA_LATITUDE, latitude);
            navIntent.putExtra(NAVIGON_PUBLIC_INTENT_EXTRA_LONGITUDE, longitude);
        } else {
            Uri location;
            if (intentPreference.equals(options[2])) {  // geo_q
                location = Uri.parse(String.format(Locale.ENGLISH, "geo:%1$f,%2$f?q=%1$f,%2$f", latitude, longitude));
            } else if (intentPreference.equals(options[1])) {   // google.navigation
                location = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%1$f,%2$f", latitude, longitude));
            } else {   // geo or unknown option
                location = Uri.parse(String.format(Locale.ENGLISH, "geo:%1$f,%2$f", latitude, longitude));
            }
            navIntent = new Intent(Intent.ACTION_VIEW, location);
        }
        sendNavigationIntent(activity, navIntent);
    }

    public static void sendNavigationIntent(Activity activity, String address) {
        String intentPreference = new Preferences(activity).getNavigationApi();
        String[] options = activity.getResources().getStringArray(R.array.preferences_activity_navigation_api_values);

        // Build the intent
        Intent navIntent;
        if (intentPreference.equals(options[3])) {  // navigon
            navIntent = new Intent(NAVIGON_PUBLIC_INTENT);
            if (address != null) {
                navIntent.putExtra(NAVIGON_PUBLIC_INTENT_EXTRA_FREE_TEXT_ADDRESS, address);
            }
        } else {
            Uri location;
            if (intentPreference.equals(options[1])) {   // google.navigation
                if (address != null) {
                    location = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%s", address));
                } else {
                    location = Uri.parse("google.navigation:");
                }
            } else {   // geo_q, geo (not applicable for address search) or unknown option
                if (address != null) {
                    location = Uri.parse(String.format(Locale.ENGLISH, "geo:0,0?q=%s", address));
                } else {
                    location = Uri.parse("geo:0,0");
                }
            }
            navIntent = new Intent(Intent.ACTION_VIEW, location);
        }
        sendNavigationIntent(activity, navIntent);
    }

    private static void sendNavigationIntent(Activity activity, Intent navIntent) {
        // Verify it resolves
        PackageManager packageManager = activity.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(navIntent, 0);
        boolean isIntentSafe = activities.size() > 0;

        // Start an activity if it's safe
        if (isIntentSafe) {
            navIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  // Don't start activity as part of EGO
            activity.startActivity(navIntent);
        } else {
            Toast.makeText(activity, R.string.error_no_navigation_app, Toast.LENGTH_LONG).show();
        }
    }

    public static class OnAppThemeChangedListener implements AppThemeWatcher.OnAppThemeChangedListener {
        private Activity activity;

        public OnAppThemeChangedListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onAppThemeChanged(int newTheme) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                activity.getIntent().putExtra("theme", newTheme);
                activity.recreate();
            }
        }
    }
}