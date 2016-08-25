package tk.crazysoft.ego.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoderFactory;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.Translation;
import com.graphhopper.util.shapes.GHPoint;

import java.util.List;
import java.util.Locale;

public class RoutingService extends Service {
    private static final String TAG = RoutingService.class.getName();
    private static final boolean USE_CONTRACTION_HIERARCHIES = false;

    public class NoDataException extends Exception { }
    public class StartupException extends Exception {
        public StartupException(Throwable e) {
            super(e);
        }
    }

    private GraphHopper gh;
    private final IBinder mBinder = new RoutingBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        gh.close();
        gh = null;
    }

    public void startup(String dataDir, boolean loadElevationData) throws NoDataException, StartupException {
        if (dataDir == null) {
            Log.e(TAG, "No external storage available, cannot load GraphHopper data");
            throw new NoDataException();
        }

        gh = new GraphHopper().forMobile();
        boolean result;
        try {
            gh.setEncodingManager(EncodingManager.create(FlagEncoderFactory.DEFAULT, dataDir));
            result = gh.setElevation(loadElevationData).setCHEnabled(true).load(dataDir);
        } catch (Exception e) {
            Log.e(TAG, "GraphHopper initialization failed", e);
            throw new StartupException(e);
        }

        if (!result) {
            Log.e(TAG, "GraphHopper initialization failed");
            throw new StartupException(null);
        }
    }

    public GHResponse route(List<GHPoint> waypoints, List<Double> headings, boolean withInstructions) {
        if (waypoints.size() < 2) {
            throw new IllegalArgumentException("Number of waypoints must be greater or equal 2");
        }

        GHRequest request;
        if (headings != null) {
            request = new GHRequest(waypoints, headings);
            request.setVehicle(FlagEncoderFactory.CAR);
            request.setWeighting("fastest");
            if (!USE_CONTRACTION_HIERARCHIES) {
                request.getHints().put(Parameters.CH.DISABLE, USE_CONTRACTION_HIERARCHIES);
                request.getHints().put("routing.flexibleMode.force", true);
            }
            request.getHints().put(Parameters.CH.FORCE_HEADING, true);  // Allow headings for routes using CH, but may produce artifacts (see https://github.com/graphhopper/graphhopper/pull/434#issuecomment-110275256)
        } else {
            request = new GHRequest(waypoints);
        }
        request.setLocale(Locale.getDefault()).setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);
        if (!withInstructions) {
            request.getHints().put(Parameters.Routing.INSTRUCTIONS, false);
        }
        return gh.route(request);
    }

    public Translation getTranslation() {
        return gh.getTranslationMap().getWithFallBack(Locale.getDefault());
    }

    public class RoutingBinder extends Binder {
        RoutingService getService() {
            // Return this instance of RoutingService so clients can call public methods
            return RoutingService.this;
        }
    }
}
