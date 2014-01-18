package tk.crazysoft.ego;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import tk.crazysoft.ego.components.TabListener;
import tk.crazysoft.ego.preferences.Preferences;

public class MainActivity extends ActionBarActivity implements AddressFragment.OnAddressClickListener {
    public static final String LAYOUT_MODE_PORTRAIT = "portrait";
    public static final String LAYOUT_MODE_LANDSCAPE = "landscape";
    public static final String LAYOUT_MODE_LANDSCAPE_LARGE = "landscape-large";

    private static final String NAVIGON_PUBLIC_INTENT = "android.intent.action.navigon.START_PUBLIC";
    private static final String NAVIGON_PUBLIC_INTENT_EXTRA_LATITUDE = "latitude";
    private static final String NAVIGON_PUBLIC_INTENT_EXTRA_LONGITUDE = "longitude";
    private static final String NAVIGON_PUBLIC_INTENT_EXTRA_FREE_TEXT_ADDRESS = "free_text_address";

    private long displayedHouse = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize default preferences if the app is started for the first time
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        String layoutMode = getString(R.string.layout_mode);
        Fragment mapFragment = getSupportFragmentManager().findFragmentByTag("map");

        if (layoutMode.equals(LAYOUT_MODE_PORTRAIT) || layoutMode.equals(LAYOUT_MODE_LANDSCAPE)) {
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_addresses).setTabListener(new TabListener<AddressFragment>(this, "addresses", AddressFragment.class)));
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_map).setTabListener(new TabListener<MapFragment>(this, "map", MapFragment.class)));
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_hospitals_doctors).setTabListener(
                    new TabListener<HospitalsDoctorsFragment>(this, "hospitals_doctors", HospitalsDoctorsFragment.class)));
        } else {
            setContentView(R.layout.multipane_view);
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_addresses).setTabListener(new TabListener<AddressFragment>(this, R.id.contentLeft, "addresses", AddressFragment.class)));
            actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_hospitals_doctors).setTabListener(
                    new TabListener<HospitalsDoctorsFragment>(this, R.id.contentLeft, "hospitals_doctors", HospitalsDoctorsFragment.class)));

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
            displayedHouse = savedInstanceState.getLong("displayedHouse");

            ActionBar.Tab tab = findTabWithTitle(tabTitle);
            if (tab != null) {
                actionBar.selectTab(tab);
            }
            if (displayedHouse >= 0) {
                onAddressClick(displayedHouse);
            }
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", getSupportActionBar().getSelectedTab().getText().toString());
        outState.putLong("displayedHouse", displayedHouse);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_data_management) {
            Intent dataManagementIntent = new Intent(this, PreferencesActivity.class);
            startActivity(dataManagementIntent);
        } else if (item.getItemId() == R.id.action_about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != HouseActivity.RESULT_LAYOUT_MODE_CHANGE) {
            displayedHouse = -1;
        }
    }

    @Override
    public void onAddressClick(long id) {
        if (getString(R.string.layout_mode).equals(LAYOUT_MODE_LANDSCAPE_LARGE)) {
            HouseFragment fragment = (HouseFragment)getSupportFragmentManager().findFragmentByTag("house");
            if (fragment == null) {
                fragment = new HouseFragment();
            }
            getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).replace(R.id.contentRight, fragment, "house").addToBackStack(null).commit();
            fragment.setHouse(id);
        } else {
            Intent houseIntent = new Intent(this, HouseActivity.class);
            houseIntent.putExtra("id", id);
            startActivityForResult(houseIntent, 0);
        }
        displayedHouse = id;
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
            navIntent.putExtra(NAVIGON_PUBLIC_INTENT_EXTRA_FREE_TEXT_ADDRESS, address);
        } else {
            Uri location;
            if (intentPreference.equals(options[1])) {   // google.navigation
                location = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%s", address));
            } else {   // geo_q, geo (not applicable for address search) or unknown option
                location = Uri.parse(String.format(Locale.ENGLISH, "geo:0,0?q=%s", address));
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
            activity.startActivity(navIntent);
        } else {
            Toast.makeText(activity, R.string.error_no_navigation_app, Toast.LENGTH_LONG).show();
        }
    }
}