package tk.crazysoft.ego.components;

import android.content.Context;
import android.location.Location;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.LinkedList;
import java.util.List;

public class EGOLocationOverlay extends MyLocationNewOverlay {
    private final List<IMyLocationConsumer> locationListeners = new LinkedList<>();

    public EGOLocationOverlay(Context context, MapView mapView) {
        super(context, mapView);
    }

    public void addLocationListener(IMyLocationConsumer listener) {
        locationListeners.add(listener);
    }

    @Override
    protected void setLocation(Location location) {
        boolean changed = getLastFix() == null || getLastFix().distanceTo(location) > 1;
        super.setLocation(location);

        if (!isFollowLocationEnabled()) {
            enableFollowLocation();
        }

        if (isFollowLocationEnabled() && location.hasBearing()) {
            mMapView.setMapOrientation(-location.getBearing());
        }

        if (changed) {
            for (IMyLocationConsumer listener : locationListeners) {
                listener.onLocationChanged(location, getMyLocationProvider());
            }
        }
    }
}
