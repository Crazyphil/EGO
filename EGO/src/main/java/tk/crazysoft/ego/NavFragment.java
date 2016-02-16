package tk.crazysoft.ego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.util.ArrayList;
import java.util.List;

import tk.crazysoft.ego.components.EGOLocationOverlay;
import tk.crazysoft.ego.components.EfficientPolyline;
import tk.crazysoft.ego.preferences.Preferences;
import tk.crazysoft.ego.services.LocalGraphHopperRoadManager;

public class NavFragment extends MapFragment {
    public static final int MAX_DISTANCE_FOR_NEXT_DIRECTION = 50;

    private static final String NAV_PATH = "ego/navigation";
    private static final float ROUTE_LINE_WIDTH = 8f;

    protected OnNavEventListener onNavEventListener;
    protected LocalGraphHopperRoadManager roadManager;
    protected GeoPoint origin;
    protected double originHeading;
    protected FolderOverlay routeOverlay;
    protected int currentInstruction = 0;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            onNavEventListener = (OnNavEventListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnNavEventListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        followLocation = true;
        if (savedInstanceState != null) {
            origin = savedInstanceState.getParcelable("origin");
            originHeading = savedInstanceState.getDouble("originHeading");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        view.findViewById(R.id.map_imageButtonGPS).setVisibility(View.GONE);
        view.findViewById(R.id.map_imageButtonDestination).setVisibility(View.GONE);

        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(false);
        mapView.getOverlayManager().remove(gpsOverlay);
        gpsOverlay = new EGOLocationOverlay(view.getContext(), mapView);
        gpsOverlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
                origin = gpsOverlay.getMyLocation();
            }
        });
        ((EGOLocationOverlay)gpsOverlay).addLocationListener(new IMyLocationConsumer() {
            @Override
            public void onLocationChanged(Location location, IMyLocationProvider source) {
                if (roadManager.getRoad() == null) {
                    return;
                }

                int node = roadManager.getNearestRoadNode(location.getLatitude(), location.getLongitude(), MAX_DISTANCE_FOR_NEXT_DIRECTION);
                if (node != currentInstruction && onNavEventListener != null) {
                    currentInstruction = node;
                    onNavEventListener.onEnterRoadNode(roadManager.getRoad(), node);
                }
                if (node == -1) {
                    origin = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getOverlays().remove(routeOverlay);
                    mapView.invalidate();

                    if (onNavEventListener != null) {
                        onNavEventListener.onRecalculate();
                    }
                    calculateRoute();
                }
            }
        });
        mapView.getOverlayManager().add(gpsOverlay);
        mapView.invalidate();

        roadManager = new LocalGraphHopperRoadManager(view.getContext(), sdPath + NAV_PATH, true, new OnRoadManagerListener());
        return view;
    }

    private void showNavFilesNotFoundDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        String message = String.format(getResources().getString(R.string.nav_view_nomaps_text), sdPath + NAV_PATH);
        builder.setTitle(R.string.nav_view_nomaps_title).setMessage(message).setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getActivity().finish();
            }
        }).create().show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("origin", origin);
        outState.putDouble("originHeading", originHeading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        roadManager.close();
    }

    @Override
    protected void setDestination(GeoPoint dest, String title) {
        super.setDestination(dest, title);

        if (imageButtonDestination != null) {
            imageButtonDestination.setVisibility(View.GONE);
        }
    }

    public LocalGraphHopperRoadManager getRoadManager() {
        return roadManager;
    }

    public int getCurrentRoadNode() {
        return currentInstruction;
    }

    public void addLocationListener(IMyLocationConsumer listener) {
        if (gpsOverlay != null) {
            ((EGOLocationOverlay)gpsOverlay).addLocationListener(listener);
        }
    }

    private class OnRoadManagerListener implements LocalGraphHopperRoadManager.OnRoadManagerListener {
        public void onInitialized(boolean result) {
            if (!result) {
                showNavFilesNotFoundDialog(getView());
            } else {
                if (origin == null) {
                    LocationManager manager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
                    Location l = manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    if (l == null) {
                        Preferences p = new Preferences(getActivity());
                        origin = new GeoPoint(p.getMapLatitude(), p.getMapLongitude());
                        originHeading = Double.NaN;
                    } else {
                        origin = new GeoPoint(l.getLatitude(), l.getLongitude(), l.getAltitude());
                        originHeading = l.hasBearing() ? l.getBearing() : Double.NaN;
                    }
                }

                calculateRoute();
            }

            if (onNavEventListener != null) {
                onNavEventListener.onInitialized(result);
            }
        }
    }

    private void calculateRoute() {
        ArrayList<GeoPoint> waypoints = new ArrayList<>(2);
        waypoints.add(origin);
        waypoints.add(destination);
        new CalculateRouteTask().execute(waypoints);
    }

    private class CalculateRouteTask extends AsyncTask<ArrayList<GeoPoint>, Void, Road> {
        @Override
        protected Road doInBackground(ArrayList<GeoPoint>... params) {
            ArrayList<Double> headings = new ArrayList<>(params[0].size());
            headings.add(originHeading);
            for (int i = 1; i < params[0].size(); i++) {
                headings.add(Double.NaN);
            }
            return roadManager.getRoad(params[0], headings);
        }

        @Override
        protected void onPostExecute(Road road) {
            if (road.mStatus == Road.STATUS_OK) {
                TypedArray a = getActivity().getTheme().obtainStyledAttributes(new int[] { R.attr.routeColor });
                int routeColor = a.getColor(0, 0);
                a.recycle();

                routeOverlay = new FolderOverlay(mapView.getContext());
                routeOverlay.add(buildRoadOverlay(road, routeColor, ROUTE_LINE_WIDTH, mapView.getContext()));

                List<GeoPoint> lastPoints = new ArrayList<>(2);
                lastPoints.add(road.mRouteHigh.get(road.mRouteHigh.size() - 1));
                lastPoints.add(destination);
                Polyline wayToDest = new Polyline(mapView.getContext());
                wayToDest.setColor(routeColor);
                wayToDest.setWidth(ROUTE_LINE_WIDTH);
                wayToDest.getPaint().setPathEffect(new DashPathEffect(new float[] { ROUTE_LINE_WIDTH * 2, ROUTE_LINE_WIDTH }, 0));
                wayToDest.setPoints(lastPoints);
                routeOverlay.add(wayToDest);

                List<OverlayItem> items = new ArrayList<>(road.mNodes.size() - 1);
                for (int i = 1; i < road.mNodes.size(); i++) {
                    RoadNode node = road.mNodes.get(i);
                    OverlayItem item = new OverlayItem(null, null, node.mLocation);
                    item.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);
                    items.add(item);
                }

                if (MainActivity.hasDarkTheme(getActivity())) {
                    float[] blueToYellow = {
                            0.5f, 0, 0.5f, 0, 0, //red
                            0, 0.5f, 0.5f, 0, 0, //green
                            0, 0, 0, 0, 0, //blue
                            0, 0, 0, 1.0f, 0 //alpha
                    };
                    ColorFilter filter = new ColorMatrixColorFilter(blueToYellow);
                    Drawable dot = getResources().getDrawable(R.drawable.marker);
                    dot.setColorFilter(filter);
                    routeOverlay.add(new ItemizedIconOverlay<>(items, dot, null, mapView.getResourceProxy()));
                } else {
                    routeOverlay.add(new ItemizedIconOverlay<>(items, getResources().getDrawable(R.drawable.marker), null, mapView.getResourceProxy()));
                }
                mapView.getOverlays().add(mapView.getOverlays().indexOf(gpsOverlay), routeOverlay);
                if (gpsOverlay.getMyLocation() != null) {
                    mapView.getController().setCenter(gpsOverlay.getMyLocation());
                }

                mapView.invalidate();
            } else {
                showRoutingErrorDialog();
            }

            if (onNavEventListener != null) {
                onNavEventListener.onRouteCalculated(road);
            }
        }

        private EfficientPolyline buildRoadOverlay(Road road, int color, float width, Context context){
            EfficientPolyline roadOverlay = new EfficientPolyline(context);
            roadOverlay.setColor(color);
            roadOverlay.setWidth(width);
            if (road != null) {
                ArrayList<GeoPoint> polyline = road.mRouteHigh;
                roadOverlay.setPoints(polyline);
            }
            return roadOverlay;
        }

        private void showRoutingErrorDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getView().getContext());
            String message = getResources().getString(R.string.nav_view_noroute_text);
            builder.setTitle(R.string.nav_view_noroute_title).setMessage(message).setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().finish();
                }
            }).create().show();
        }
    }

    public interface OnNavEventListener {
        void onInitialized(boolean result);
        void onRecalculate();
        void onRouteCalculated(Road road);
        void onEnterRoadNode(Road road, int newRoadNode);
    }
}
