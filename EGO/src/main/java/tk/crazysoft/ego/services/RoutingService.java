package tk.crazysoft.ego.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.util.Translation;
import com.graphhopper.util.shapes.GHPoint;

import java.util.List;
import java.util.Locale;

public class RoutingService extends Service {
    private static final String TAG = RoutingService.class.getName();

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
            result = gh.setElevation(loadElevationData).load(dataDir);
        } catch (Exception e) {
            Log.e(TAG, "GraphHopper initialization failed", e);
            throw new StartupException(e);
        }

        if (!result) {
            Log.e(TAG, "GraphHopper initialization failed");
            throw new StartupException(null);
        }
    }


    public GHResponse route(List<GHPoint> waypoints, boolean withInstructions) {
        if (waypoints.size() < 2) {
            throw new IllegalArgumentException("Number of waypoints must be greater or equal 2");
        }

        GHRequest request = new GHRequest(waypoints.get(0), waypoints.get(waypoints.size() - 1));
        for (int i = 1; i < waypoints.size() - 1; i++) {
            request.addPoint(waypoints.get(i));
        }
        request.setLocale(Locale.getDefault()).setAlgorithm(AlgorithmOptions.DIJKSTRA_BI);
        if (!withInstructions) {
            request.getHints().put("instructions", "false");
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
