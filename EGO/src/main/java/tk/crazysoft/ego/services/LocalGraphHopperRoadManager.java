package tk.crazysoft.ego.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.graphhopper.GHResponse;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.Helper;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;

import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;

import java.io.Closeable;
import java.util.ArrayList;

public class LocalGraphHopperRoadManager extends GraphHopperRoadManager implements Closeable {
    // MapQuest maneuver codes
    public static final int TURN_ROUNDABOUT_LEAVE = -2;
    public static final int TURN_ROUNDABOUT_ENTER = -1;
    public static final int TURN_NONE = 0;
    public static final int TURN_STRAIGHT = 1;
    public static final int TURN_BECOMES = 2;
    public static final int TURN_SLIGHT_LEFT = 3;
    public static final int TURN_LEFT = 4;
    public static final int TURN_SHARP_LEFT = 5;
    public static final int TURN_SLIGHT_RIGHT = 6;
    public static final int TURN_RIGHT = 7;
    public static final int TURN_SHARP_RIGHT = 8;
    public static final int TURN_STAY_LEFT = 9;
    public static final int TURN_STAY_RIGHT = 10;
    public static final int TURN_STAY_STRAIGHT = 11;
    public static final int TURN_UTURN = 12;
    public static final int TURN_UTURN_LEFT = 13;
    public static final int TURN_UTURN_RIGHT = 14;
    public static final int TURN_EXIT_LEFT = 15;
    public static final int TURN_EXIT_RIGHT = 16;
    public static final int TURN_RAMP_LEFT = 17;
    public static final int TURN_RAMP_RIGHT = 18;
    public static final int TURN_RAMP_STRAIGHT = 19;
    public static final int TURN_MERGE_LEFT = 20;
    public static final int TURN_MERGE_RIGHT = 21;
    public static final int TURN_MERGE_STRAIGHT = 22;
    public static final int TURN_ENTERING = 23;
    public static final int TURN_DESTINATION = 24;
    public static final int TURN_DESTINATION_LEFT = 25;
    public static final int TURN_DESTINATION_RIGHT = 26;
    public static final int TURN_ROUNDABOUT1 = 27;
    public static final int TURN_ROUNDABOUT2 = 28;
    public static final int TURN_ROUNDABOUT3 = 29;
    public static final int TURN_ROUNDABOUT4 = 30;
    public static final int TURN_ROUNDABOUT5 = 31;
    public static final int TURN_ROUNDABOUT6 = 32;
    public static final int TURN_ROUNDABOUT7 = 33;
    public static final int TURN_ROUNDABOUT8 = 34;
    public static final int TURN_TRANSIT_TAKE = 35;
    public static final int TURN_TRANSIT_TRANSFER = 36;
    public static final int TURN_TRANSIT_ENTER = 37;
    public static final int TURN_TRANSIT_EXIT = 38;
    public static final int TURN_TRANSIT_REMAIN_ON = 39;

    private Context context;
    private String dataDir;
    private ServiceConnection serviceConnection;
    private RoutingService routingService;
    private OnRoadManagerListener onRoadManagerListener;
    private GHResponse ghResponse;
    private Road calculatedRoad;

    public LocalGraphHopperRoadManager(Context context, String dataDir, boolean withElevation, OnRoadManagerListener listener) {
        super(null);

        this.context = context;
        this.dataDir = dataDir;
        mWithElevation = withElevation;
        onRoadManagerListener = listener;
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                RoutingService.RoutingBinder binder = (RoutingService.RoutingBinder)service;
                routingService = binder.getService();
                boolean result = init();
                if (onRoadManagerListener != null) {
                    onRoadManagerListener.onInitialized(result);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                routingService = null;
            }
        };

        Intent serviceIntent = new Intent(context, RoutingService.class);
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean init() {
        try {
            routingService.startup(dataDir, mWithElevation);
        } catch (RoutingService.NoDataException | RoutingService.StartupException e) {
            return false;
        }
        return true;
    }

    @Override
    public void setElevation(boolean withElevation) {
        throw new IllegalStateException("Elevation must be set on class instantiation");
    }

    @Override
    public Road getRoad(ArrayList<GeoPoint> waypoints) {
        return getRoad(waypoints, null);
    }

    public Road getRoad(ArrayList<GeoPoint> waypoints, ArrayList<Double> headings) {
        Road road = new Road();
        ghResponse = routingService.route(toGHPoints(waypoints), headings, true);
        if (ghResponse.hasErrors()) {
            Log.e(BonusPackHelper.LOG_TAG, ghResponse.getErrors().size() + " error(s) occurred when calculating route:");
            for (int i = 0; i < ghResponse.getErrors().size(); i++) {
                Log.e(BonusPackHelper.LOG_TAG, "Error " + i, ghResponse.getErrors().get(i));
            }
            return new Road(waypoints);
        }

        road.mRouteHigh = toGeoPoints(ghResponse.getBest().getPoints());
        for (Instruction instruction : ghResponse.getBest().getInstructions()) {
            RoadNode node = new RoadNode();
            node.mLocation = new GeoPoint(instruction.getPoints().getLatitude(0), instruction.getPoints().getLongitude(0), instruction.getPoints().getElevation(0));
            node.mLength = instruction.getDistance() / 1000f;
            node.mDuration = instruction.getTime() / 1000f; // Segment duration in seconds.
            node.mManeuverType = getManeuverCode(instruction.getSign());
            node.mInstructions = instruction.getTurnDescription(routingService.getTranslation());
            road.mNodes.add(node);
        }
        road.mLength = ghResponse.getBest().getDistance() / 1000f;
        road.mDuration = ghResponse.getBest().getTime() / 1000f;

        BBox routeBBox = ghResponse.getBest().calcRouteBBox(BBox.createInverse(mWithElevation));
        road.mBoundingBox = new BoundingBoxE6(routeBBox.maxLat, routeBBox.maxLon, routeBBox.minLat, routeBBox.minLon);
        road.mStatus = Road.STATUS_OK;
        road.buildLegs(waypoints);

        calculatedRoad = road;
        return road;
    }

    @Override
    protected int getManeuverCode(int direction) {
        switch (direction) {
            case Instruction.USE_ROUNDABOUT:
                return TURN_ROUNDABOUT_ENTER;
            case Instruction.LEAVE_ROUNDABOUT:
                return TURN_ROUNDABOUT_LEAVE;
            default:
                return super.getManeuverCode(direction);
        }
    }

    public Road getRoad() {
        return calculatedRoad;
    }

    public String getStreetName(int roadNode) {
        if (ghResponse != null) {
            return ghResponse.getBest().getInstructions().get(roadNode).getName();
        }
        return null;
    }

    public int getNearestRoadNode(double lat, double lon, double maxDistance) {
        Instruction inst = ghResponse.getBest().getInstructions().find(lat, lon, maxDistance);
        if (inst != null) {
            for (int i = 0; i < ghResponse.getBest().getInstructions().size(); i++) {
                if (ghResponse.getBest().getInstructions().get(i).equals(inst)) {
                    return Math.max(0, Math.min(i - 1, ghResponse.getBest().getInstructions().size() - 1));
                }
            }
        }
        return -1;
    }

    public double getInstructionProgress(double lat, double lon, int roadNode) {
        Instruction instruction = ghResponse.getBest().getInstructions().get(roadNode);

        PointList points = instruction.getPoints();
        if (roadNode < ghResponse.getBest().getInstructions().size() - 1) {
            points = new PointListProxy(instruction, ghResponse.getBest().getInstructions().get(roadNode + 1));
        }

        double prevLat = points.getLatitude(0);
        double prevLon = points.getLongitude(0);
        DistanceCalc distCalc = Helper.DIST_EARTH;
        double foundMinDistance = distCalc.calcDist(lat, lon, prevLat, prevLon);
        int foundPoint = 0;

        for (int i = 1; i < points.size(); i++)
        {
            double currLat = points.getLatitude(i);
            double currLon = points.getLongitude(i);

            // calculate the distance from the point to the edge
            double distance;
            if (distCalc.validEdgeDistance(lat, lon, currLat, currLon, prevLat, prevLon))
            {
                distance = distCalc.calcDenormalizedDist(distCalc.calcNormalizedEdgeDistance(lat, lon, currLat, currLon, prevLat, prevLon));
            } else
            {
                distance = distCalc.calcDist(lat, lon, currLat, currLon);
            }

            if (distance < foundMinDistance)
            {
                foundMinDistance = distance;
                foundPoint = i;
            }

            prevLat = currLat;
            prevLon = currLon;
        }

        if (points.size() > 1) {
            // Handle edge cases for locations after the end of the route or before the beginning of the route
            if (foundPoint == points.size() - 1 && !distCalc.validEdgeDistance(lat, lon, points.getLat(foundPoint - 1), points.getLon(foundPoint - 1), points.getLat(foundPoint), points.getLon(foundPoint))) {
                return 1;
            } else if (foundPoint == 0 && !distCalc.validEdgeDistance(lat, lon, points.getLat(foundPoint), points.getLon(foundPoint), points.getLat(foundPoint + 1), points.getLon(foundPoint + 1))) {
                return 0;
            }
        }

        double distance = 0;
        for (int i = 1; i < foundPoint; i++) {
            distance += distCalc.calcDist(points.getLat(i - 1), points.getLon(i - 1), points.getLat(i), points.getLon(i));
        }

        if (distCalc.validEdgeDistance(lat, lon, points.getLat(foundPoint - 1), points.getLon(foundPoint - 1), points.getLat(foundPoint), points.getLon(foundPoint))) {
            // Current position is on the edge before the found node
            GHPoint snapPoint = distCalc.calcCrossingPointToEdge(lat, lon, points.getLat(foundPoint - 1), points.getLon(foundPoint - 1), points.getLat(foundPoint), points.getLon(foundPoint));
            distance += distCalc.calcDist(points.getLat(foundPoint - 1), points.getLon(foundPoint - 1), snapPoint.getLat(), snapPoint.getLon());
        } else if (distCalc.validEdgeDistance(lat, lon, points.getLat(foundPoint), points.getLon(foundPoint), points.getLat(foundPoint + 1), points.getLon(foundPoint + 1))) {
            // Current position is on the edge after the found node
            GHPoint snapPoint = distCalc.calcCrossingPointToEdge(lat, lon, points.getLat(foundPoint), points.getLon(foundPoint), points.getLat(foundPoint + 1), points.getLon(foundPoint + 1));
            distance += distCalc.calcDist(points.getLat(foundPoint), points.getLon(foundPoint), snapPoint.getLat(), snapPoint.getLon());
        } else {
            // The node itself is the nearest location on the route, add remaining distance from previous node
            distance += distCalc.calcDist(points.getLat(foundPoint - 1), points.getLon(foundPoint - 1), points.getLat(foundPoint), points.getLon(foundPoint));
        }
        return distance / instruction.getDistance();
    }

    private ArrayList<GHPoint> toGHPoints(ArrayList<GeoPoint> waypoints) {
        ArrayList<GHPoint> ghPoints = new ArrayList<>(waypoints.size());
        for (GeoPoint waypoint : waypoints) {
            ghPoints.add(new GHPoint(waypoint.getLatitude(), waypoint.getLongitude()));
        }
        return ghPoints;
    }

    private ArrayList<GeoPoint> toGeoPoints(PointList waypoints) {
        ArrayList<GeoPoint> geoPoints = new ArrayList<>(waypoints.size());
        for (GHPoint3D waypoint : waypoints) {
            geoPoints.add(new GeoPoint(waypoint.getLat(), waypoint.getLon(), mWithElevation ? waypoint.getEle() : 0));
        }
        return geoPoints;
    }

    public void setListener(OnRoadManagerListener l) {
        onRoadManagerListener = l;
    }

    @Override
    public void close() {
        context.unbindService(serviceConnection);
    }

    public interface OnRoadManagerListener {
        void onInitialized(boolean result);
    }

    /**
     * Proxies requests to Instruction PointLists so that they include the first point of the next Instruction
     */
    private class PointListProxy extends PointList {
        PointList firstList;
        PointList secondList;

        public PointListProxy(Instruction first, Instruction second) {
            firstList = first.getPoints();
            secondList = second.getPoints();
        }

        @Override
        public int size() {
            return firstList.size() + 1;
        }

        @Override
        public int getSize() {
            return size();
        }

        @Override
        public double getLat(int index) {
            return getLatitude(index);
        }

        @Override
        public double getLatitude(int index) {
            if (index < firstList.size()) {
                return firstList.getLatitude(index);
            } else {
                return secondList.getLatitude(index - firstList.size());
            }
        }

        @Override
        public double getLon(int index) {
            return super.getLon(index);
        }

        @Override
        public double getLongitude(int index) {
            if (index < firstList.size()) {
                return firstList.getLongitude(index);
            } else {
                return secondList.getLongitude(index - firstList.size());
            }
        }
    }
}
