package tk.crazysoft.ego;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import tk.crazysoft.ego.components.TabListener;

public class MainActivity extends ActionBarActivity implements AddressFragment.OnAddressClickListener {
    private static final String NAVIGON_PUBLIC_INTENT = "android.intent.action.navigon.START_PUBLIC";
    private static final String NAVIGON_PUBLIC_INTENT_EXTRA_LATITUDE = "latitude";
    private static final String NAVIGON_PUBLIC_INTENT_EXTRA_LONGITUDE = "longitude";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_addresses).setTabListener(new TabListener<AddressFragment>(this, "addresses", AddressFragment.class)));
        actionBar.addTab(actionBar.newTab().setText(R.string.main_activity_tab_map).setTabListener(new TabListener<MapFragment>(this, "map", MapFragment.class)));

        if (savedInstanceState != null) {
            actionBar.selectTab(actionBar.getTabAt(savedInstanceState.getInt("tab")));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getSupportActionBar().getSelectedTab().getPosition());
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
            Intent dataManagementIntent = new Intent(this, DataManagementActivity.class);
            startActivity(dataManagementIntent);
        } else if (item.getItemId() == R.id.action_about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
        }
        return true;
    }

    @Override
    public void onAddressClick(long id) {
        Intent houseIntent = new Intent(this, HouseActivity.class);
        houseIntent.putExtra("id", id);
        startActivity(houseIntent);
    }

    public static void sendNavigationIntent(Activity activity, double latitude, double longitude) {
        // Build the intent
        //Uri location = Uri.parse(String.format(Locale.ENGLISH, "geo:%1$f,%2$f?q=%1$f,%2$f", latitude, longitude));
        //Uri location = Uri.parse(String.format(Locale.ENGLISH, "geo:%1$f,%2$f", latitude, longitude));
        Uri location = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%1$f,%2$f", latitude, longitude));
        Intent navIntent = new Intent(Intent.ACTION_VIEW, location);

        /*Intent navIntent = new Intent(NAVIGON_PUBLIC_INTENT);
        navIntent.putExtra(NAVIGON_PUBLIC_INTENT_EXTRA_LATITUDE, latitude);
        navIntent.putExtra(NAVIGON_PUBLIC_INTENT_EXTRA_LONGITUDE, longitude);*/

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