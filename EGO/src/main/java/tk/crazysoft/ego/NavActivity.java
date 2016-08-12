package tk.crazysoft.ego;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
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
    private ImageView imageViewDirection;
    private CardView panelInstructions;
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

        imageViewDirection = (ImageView)findViewById(R.id.nav_imageViewDirection);
        textViewDirection = (TextView)findViewById(R.id.nav_textViewDirection);
        textViewStreet = (TextView)findViewById(R.id.nav_textViewStreet);
        textViewNextDirection = (TextView)findViewById(R.id.nav_textViewNextDirection);
        textViewTime = (TextView)findViewById(R.id.nav_textViewTime);
        textViewDistance = (TextView)findViewById(R.id.nav_textViewDistance);
        panelInstructions = (CardView)findViewById(R.id.nav_panelInstructions);
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
        panelInstructions.setVisibility(View.VISIBLE);
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
        imageViewDirection.setImageDrawable(getDrawableForManeuver(curNode.mManeuverType));
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
        Drawable drawable;
        switch (maneuver) {
            case LocalGraphHopperRoadManager.TURN_DESTINATION:
            case LocalGraphHopperRoadManager.TURN_DESTINATION_LEFT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_arrive);
            case LocalGraphHopperRoadManager.TURN_DESTINATION_RIGHT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_arrive_right);
            case LocalGraphHopperRoadManager.TURN_TRANSIT_TAKE:
            case LocalGraphHopperRoadManager.TURN_TRANSIT_ENTER:
            case LocalGraphHopperRoadManager.TURN_TRANSIT_REMAIN_ON:
            case LocalGraphHopperRoadManager.TURN_TRANSIT_TRANSFER:
            case LocalGraphHopperRoadManager.TURN_TRANSIT_EXIT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_ferry);
            case LocalGraphHopperRoadManager.TURN_STAY_LEFT:
                drawable = ContextCompat.getDrawable(this, R.drawable.da_turn_fork_right);
                break;
            case LocalGraphHopperRoadManager.TURN_STAY_RIGHT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_fork_right);
            case LocalGraphHopperRoadManager.TURN_MERGE_STRAIGHT:
            case LocalGraphHopperRoadManager.TURN_MERGE_LEFT:
            case LocalGraphHopperRoadManager.TURN_MERGE_RIGHT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_generic_merge);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT_ENTER:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_generic_roundabout);
            case LocalGraphHopperRoadManager.TURN_EXIT_LEFT:
            case LocalGraphHopperRoadManager.TURN_RAMP_LEFT:
                drawable = ContextCompat.getDrawable(this, R.drawable.da_turn_ramp_right);
                break;
            case LocalGraphHopperRoadManager.TURN_EXIT_RIGHT:
            case LocalGraphHopperRoadManager.TURN_RAMP_RIGHT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_fork_right);
            case LocalGraphHopperRoadManager.TURN_LEFT:
                drawable = ContextCompat.getDrawable(this, R.drawable.da_turn_right);
                break;
            case LocalGraphHopperRoadManager.TURN_RIGHT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_right);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT1:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_roundabout_1);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT2:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_roundabout_2);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT3:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_roundabout_3);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT4:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_roundabout_4);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT5:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_roundabout_5);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT6:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_roundabout_6);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT7:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_roundabout_7);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT8:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_roundabout_8);
            case LocalGraphHopperRoadManager.TURN_ROUNDABOUT_LEAVE:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_roundabout_exit);
            case LocalGraphHopperRoadManager.TURN_SHARP_LEFT:
                drawable = ContextCompat.getDrawable(this, R.drawable.da_turn_sharp_right);
                break;
            case LocalGraphHopperRoadManager.TURN_SLIGHT_LEFT:
                drawable = ContextCompat.getDrawable(this, R.drawable.da_turn_slight_right);
                break;
            case LocalGraphHopperRoadManager.TURN_SHARP_RIGHT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_sharp_right);
            case LocalGraphHopperRoadManager.TURN_SLIGHT_RIGHT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_slight_right);
            case LocalGraphHopperRoadManager.TURN_STRAIGHT:
            case LocalGraphHopperRoadManager.TURN_STAY_STRAIGHT:
            case LocalGraphHopperRoadManager.TURN_RAMP_STRAIGHT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_straight);
            case LocalGraphHopperRoadManager.TURN_UTURN:
            case LocalGraphHopperRoadManager.TURN_UTURN_LEFT:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_uturn);
            case LocalGraphHopperRoadManager.TURN_UTURN_RIGHT:
                drawable = ContextCompat.getDrawable(this, R.drawable.da_turn_uturn);
                break;
            default:
                return ContextCompat.getDrawable(this, R.drawable.da_turn_unknown);
        }

        int size = Math.max(imageViewDirection.getWidth(), imageViewDirection.getHeight());
        Bitmap canvasBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(canvasBitmap);
        drawable.setBounds(0, 0, canvasBitmap.getWidth(), canvas.getHeight());
        canvas.translate(canvas.getWidth(), 0);
        canvas.scale(-1, 1);
        drawable.draw(canvas);

        return new BitmapDrawable(getResources(), canvasBitmap);
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
