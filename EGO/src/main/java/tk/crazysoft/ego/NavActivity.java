package tk.crazysoft.ego;

import android.app.ProgressDialog;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import tk.crazysoft.ego.components.AppThemeWatcher;
import tk.crazysoft.ego.services.LocalGraphHopperRoadManager;

public class NavActivity extends ActionBarActivity implements NavFragment.OnNavEventListener {
    public static final String EXTRA_LATITUDE = "tk.crazysoft.ego.EXTRA_LATITUDE";
    public static final String EXTRA_LONGITUDE = "tk.crazysoft.ego.EXTRA_LONGITUDE";

    protected GeoPoint destination;
    protected NavFragment navFragment;
    protected ProgressDialog progressDialog;

    private TextView textViewDirection, textViewStreet, textViewNextDirection, textViewTime, textViewDistance;
    private int currentRoadNode;
    private double distanceLeft, durationLeft;

    private AppThemeWatcher themeWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher = new AppThemeWatcher(this, savedInstanceState);
            setTheme(getIntent().getIntExtra("theme", themeWatcher.getCurrentAppTheme()));
            themeWatcher.setOnAppThemeChangedListener(new MainActivity.OnAppThemeChangedListener(this));
        }

        setContentView(R.layout.nav_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.nav_activity_loading_startup));

        if (savedInstanceState == null) {
            if (getIntent().getExtras() != null) {
                destination = new GeoPoint(getIntent().getDoubleExtra(EXTRA_LATITUDE, 0), getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0));
            }
            navFragment = new NavFragment();
            navFragment.setDestination(destination, null);
            getSupportFragmentManager().beginTransaction().add(R.id.nav_mapContainer, navFragment, "map").commit();
        } else {
            destination = savedInstanceState.getParcelable("destination");
            navFragment = (NavFragment)getSupportFragmentManager().findFragmentByTag("map");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        navFragment.addLocationListener(new IMyLocationConsumer() {
            @Override
            public void onLocationChanged(Location location, IMyLocationProvider source) {
                if (navFragment.getRoadManager().getRoad() == null) {
                    return;
                }

                int roadNode = navFragment.getCurrentRoadNode();
                if (roadNode >= 0) {
                    double progressLeft = 1 - navFragment.getRoadManager().getInstructionProgress(location.getLatitude(), location.getLongitude(), roadNode);
                    RoadNode node = navFragment.getRoadManager().getRoad().mNodes.get(roadNode);
                    setRouteMetrics(node.mLength * progressLeft, distanceLeft + node.mLength * progressLeft, durationLeft + node.mDuration * progressLeft);
                }
            }
        });

        textViewDirection = (TextView)findViewById(R.id.nav_textViewDirection);
        textViewStreet = (TextView)findViewById(R.id.nav_textViewStreet);
        textViewNextDirection = (TextView)findViewById(R.id.nav_textViewNextDirection);
        textViewTime = (TextView)findViewById(R.id.nav_textViewTime);
        textViewDistance = (TextView)findViewById(R.id.nav_textViewDistance);
        progressDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            themeWatcher.onResume();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        outState.putParcelable("destination", destination);
    }

    @Override
    public void onInitialized(boolean result) {
        if (result) {
            progressDialog.setMessage(getString(R.string.nav_activity_loading_routing));
        } else {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onRecalculate() {
        progressDialog.setMessage(getString(R.string.nav_activity_loading_routing));
        progressDialog.show();
    }

    @Override
    public void onRouteCalculated(Road road) {
        progressDialog.dismiss();
        if (road.mStatus != Road.STATUS_OK) {
            return;
        }

        currentRoadNode = -1;   // Set to -1 so that setInstructions() subtracts distance/duration of node 0
        distanceLeft = road.mLength;
        durationLeft = road.mDuration;

        setInstructions(road, 0);
        setRouteMetrics(road.mNodes.get(0).mLength, road.mLength, road.mDuration);

    }

    private void setInstructions(Road road, int instructionId) {
        if (instructionId > currentRoadNode) {
            for (int i = currentRoadNode + 1; i <= instructionId; i++) {
                distanceLeft -= road.mNodes.get(i).mLength;
                durationLeft -= road.mNodes.get(i).mDuration;
            }
        } else if (instructionId < currentRoadNode) {
            for (int i = currentRoadNode + 1; i <= instructionId; i++) {
                distanceLeft += road.mNodes.get(i).mLength;
                durationLeft += road.mNodes.get(i).mDuration;
            }
        }
        currentRoadNode = instructionId;

        RoadNode curNode = road.mNodes.get(Math.min(instructionId + 1, road.mNodes.size() - 1));
        textViewDirection.setCompoundDrawablesWithIntrinsicBounds(null, getDrawableForManeuver(curNode.mManeuverType), null, null);
        textViewStreet.setText(navFragment.getRoadManager().getStreetName(instructionId));

        if (curNode.mLength * 1000 < NavFragment.MAX_DISTANCE_FOR_NEXT_DIRECTION && road.mNodes.size() > instructionId + 1) {
            RoadNode nextNode = road.mNodes.get(instructionId + 1);
            Drawable nextIcon = getDrawableForManeuver(nextNode.mManeuverType);
            nextIcon.setBounds(0, 0, textViewNextDirection.getHeight(), textViewDirection.getHeight());
            textViewNextDirection.setCompoundDrawables(null, null, nextIcon, null);
        } else {
            textViewNextDirection.setVisibility(View.GONE);
        }
    }

    private void setRouteMetrics(double nodeDistance, double totalDistance, double duration) {
        textViewDirection.setText(toDistanceString(nodeDistance));
        textViewDistance.setText(getString(R.string.nav_activity_distance, toDistanceString(totalDistance)));
        textViewTime.setText(getString(R.string.nav_activity_time, toTimeString(duration)));
    }

    private Drawable getDrawableForManeuver(int maneuver) {
        TypedArray a;
        switch (maneuver) {
            case LocalGraphHopperRoadManager.TURN_DESTINATION:
            case LocalGraphHopperRoadManager.TURN_DESTINATION_LEFT:
            case LocalGraphHopperRoadManager.TURN_DESTINATION_RIGHT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnArrive });
                break;
            case LocalGraphHopperRoadManager.TURN_TRANSIT_TAKE:
            case LocalGraphHopperRoadManager.TURN_TRANSIT_ENTER:
            case LocalGraphHopperRoadManager.TURN_TRANSIT_REMAIN_ON:
            case LocalGraphHopperRoadManager.TURN_TRANSIT_TRANSFER:
            case LocalGraphHopperRoadManager.TURN_TRANSIT_EXIT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnFerry });
                break;
            case LocalGraphHopperRoadManager.TURN_STAY_LEFT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnForkLeft });
                break;
            case LocalGraphHopperRoadManager.TURN_STAY_RIGHT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnForkRight });
                break;
            case LocalGraphHopperRoadManager.TURN_MERGE_STRAIGHT:
            case LocalGraphHopperRoadManager.TURN_MERGE_LEFT:
            case LocalGraphHopperRoadManager.TURN_MERGE_RIGHT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnGenericMerge });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT_ENTER:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnGenericRoundabout });
                break;
            case LocalGraphHopperRoadManager.TURN_EXIT_LEFT:
            case LocalGraphHopperRoadManager.TURN_RAMP_LEFT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRampLeft });
                break;
            case LocalGraphHopperRoadManager.TURN_EXIT_RIGHT:
            case LocalGraphHopperRoadManager.TURN_RAMP_RIGHT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRampRight });
                break;
            case LocalGraphHopperRoadManager.TURN_LEFT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnLeft });
                break;
            case LocalGraphHopperRoadManager.TURN_RIGHT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRight });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT1:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRoundabout1 });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT2:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRoundabout2 });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT3:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRoundabout3 });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT4:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRoundabout4 });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT5:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRoundabout5 });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT6:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRoundabout6 });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT7:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRoundabout7 });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT8:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnGenericRoundabout });
                break;
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT_LEAVE:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnRoundaboutExit });
                break;
            case LocalGraphHopperRoadManager.TURN_SHARP_LEFT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnSharpLeft });
                break;
            case LocalGraphHopperRoadManager.TURN_SLIGHT_LEFT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnSlightLeft });
                break;
            case LocalGraphHopperRoadManager.TURN_SHARP_RIGHT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnSharpRight });
                break;
            case LocalGraphHopperRoadManager.TURN_SLIGHT_RIGHT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnSlightRight });
                break;
            case LocalGraphHopperRoadManager.TURN_STRAIGHT:
            case LocalGraphHopperRoadManager.TURN_STAY_STRAIGHT:
            case LocalGraphHopperRoadManager.TURN_RAMP_STRAIGHT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnStraight });
                break;
            case LocalGraphHopperRoadManager.TURN_UTURN:
            case LocalGraphHopperRoadManager.TURN_UTURN_LEFT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnUTurn });
                break;
            case LocalGraphHopperRoadManager.TURN_UTURN_RIGHT:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnUTurnRight });
                break;
            default:
                a = getTheme().obtainStyledAttributes(new int[] { R.attr.turnUnknown });
                break;
        }

        Drawable maneuverIcon = null;
        if (a != null) {
            maneuverIcon = a.getDrawable(0);
            a.recycle();
        }
        return maneuverIcon;
    }

    private String toTimeString(double duration) {
        int seconds = (int)Math.round(duration);
        int hours = seconds / 3600;
        seconds -= hours * 3600;
        int minutes = seconds / 60;
        seconds -= minutes * 60;

        if (hours > 0) {
            return getResources().getString(R.string.nav_activity_time_hours, String.format("%02d:%02d:%02d", hours, minutes, seconds));
        }
        return getResources().getString(R.string.nav_activity_time_minutes, String.format("%02d:%02d", minutes, seconds));
    }

    private String toDistanceString(double distance) {
        if (distance >= 100) {
            return getResources().getString(R.string.nav_activity_distance_kilometers, String.format("%.0f", distance));
        } else if (distance >= 1) {
            return getResources().getString(R.string.nav_activity_distance_kilometers, String.format("%.1f", distance));
        }
        return getResources().getString(R.string.nav_activity_distance_meters, (int)(distance * 1000.0));
    }

    @Override
    public void onEnterRoadNode(Road road, int newRoadNode) {
        if (newRoadNode >= 0) {
            setInstructions(road, Math.min(newRoadNode, road.mNodes.size()-1));
        }
    }
}
