package tk.crazysoft.ego;

import android.app.AlertDialog;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;

import tk.crazysoft.ego.data.QuadTreeTileSource;
import tk.crazysoft.ego.io.ExternalStorage;
import tk.crazysoft.ego.preferences.Preferences;

public class MapFragment extends Fragment {
    private static final String BASEMAP_PATH = "ego/karten/basemap.sqlite";
    private static final String BASEMAP_NIGHT_PATH = "ego/karten/basemap-nacht.sqlite";
    private static final String ORTHOPHOTO_PATH = "ego/karten/orthofoto.sqlite";

    private MapView mapView;
    private ImageButton imageButtonGPS, imageButtonDestination;
    private ToggleButton toggleButtonMapmode;
    private MyLocationNewOverlay gpsOverlay;
    private ItemizedIconOverlay<OverlayItem> destinationOverlay;
    private boolean mapFileNotFoundDialogShown = false;
    private GeoPoint mapCenter;
    private int mapZoom;
    private boolean followLocation = false, showOrtophoto = false;
    private double destinationLatitude, destinationLongitude;
    private String destinationTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mapCenter = savedInstanceState.getParcelable("center");
            mapZoom = savedInstanceState.getInt("zoom");
            followLocation = savedInstanceState.getBoolean("follow");
            showOrtophoto = savedInstanceState.getBoolean("showOrtophoto");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.map_view, container, false);
        createMapView(view);
        return view;
    }

    private void createMapView(View view) {
        String sdPath = ExternalStorage.getSdCardPath(getActivity());

        File basemapFile;
        if (MainActivity.hasDarkTheme(getActivity())) {
            basemapFile = new File(sdPath + BASEMAP_NIGHT_PATH);
            if (!basemapFile.exists()) {
                basemapFile = new File(sdPath + BASEMAP_PATH);
            }
        } else {
            basemapFile = new File(sdPath + BASEMAP_PATH);
        }

        File orthophotoFile = new File(sdPath + ORTHOPHOTO_PATH);

        IArchiveFile basemapArchive = null;
        IArchiveFile orthophotoArchive = null;
        if (!basemapFile.exists()) {
            showMapFilesNotFoundDialog(view, sdPath);
        } else {
            basemapArchive = ArchiveFileFactory.getArchiveFile(basemapFile);
            if (basemapArchive == null) {
                showMapFilesNotFoundDialog(view, sdPath);
            }
        }
        if (!orthophotoFile.exists()) {
            showMapFilesNotFoundDialog(view, sdPath);
        } else {
            orthophotoArchive = ArchiveFileFactory.getArchiveFile(orthophotoFile);
            if (orthophotoArchive == null) {
                showMapFilesNotFoundDialog(view, sdPath);
            }
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int tileSize = (int)(256 * metrics.density);
        ITileSource basemapSource = new XYTileSource("basemap.at", ResourceProxy.string.offline_mode, 1, 17, tileSize, ".jpg", new String[] { "http://maps.wien.gv.at/basemap/geolandbasemap/" });
        ITileSource orthophotoSource = new QuadTreeTileSource("geoimage.at", ResourceProxy.string.offline_mode, 5, 17, tileSize, ".png", "http://srv.doris.at/arcgis/rest/services/");

        IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(view.getContext());
        MapTileFileArchiveProvider basemapProvider = new MapTileFileArchiveProvider(registerReceiver, basemapSource, basemapArchive != null ? new IArchiveFile[] { basemapArchive } : null);
        MapTileFileArchiveProvider ortophotoProvider = new MapTileFileArchiveProvider(registerReceiver, orthophotoSource, orthophotoArchive != null ? new IArchiveFile[] { orthophotoArchive } : null);
        MapTileProviderArray basemapProviderArray = new MapTileProviderArray(basemapSource, registerReceiver, new MapTileModuleProviderBase[] { basemapProvider });
        MapTileProviderArray orthophotoProviderArray = new MapTileProviderArray(orthophotoSource, registerReceiver, new MapTileModuleProviderBase[] { ortophotoProvider });

        ResourceProxy proxy = new DefaultResourceProxyImpl(getActivity().getApplicationContext());
        mapView = new MapView(view.getContext(), 256, proxy, basemapProviderArray);

        TypedArray a = getActivity().getTheme().obtainStyledAttributes(new int[] { R.attr.tilesLoadingBackgroundColor, R.attr.tilesLoadingLineColor });
        mapView.getOverlayManager().getTilesOverlay().setLoadingBackgroundColor(a.getColor(0, 0));
        mapView.getOverlayManager().getTilesOverlay().setLoadingLineColor(a.getColor(1, 0));
        a.recycle();

        TilesOverlay orthophotoOverlay = new TilesOverlay(orthophotoProviderArray, proxy);
        orthophotoOverlay.setLoadingBackgroundColor(getResources().getColor(android.R.color.transparent));
        orthophotoOverlay.setEnabled(showOrtophoto);
        gpsOverlay = new MyLocationNewOverlay(view.getContext(), new GpsMyLocationProvider(view.getContext()), mapView);
        destinationOverlay = new ItemizedIconOverlay<OverlayItem>(view.getContext(), new ArrayList<OverlayItem>(1), null);

        // Note: Overlays stored in member variables are referenced by their index in onStart()
        mapView.getOverlayManager().add(orthophotoOverlay);
        mapView.getOverlayManager().add(gpsOverlay);
        mapView.getOverlayManager().add(destinationOverlay);
        mapView.setUseDataConnection(false);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        ((RelativeLayout)view.findViewById(R.id.map_layoutRoot)).addView(mapView, 0, params);

        Preferences preferences = new Preferences(view.getContext());
        GeoPoint defaultCenter = new GeoPoint(preferences.getMapLatitude(), preferences.getMapLongitude());

        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        if (mapCenter != null) {
            mapView.getController().setZoom(mapZoom);
            mapView.getController().setCenter(mapCenter);
        } else {
            mapView.getController().setZoom(17);
            mapView.getController().setCenter(defaultCenter);
        }

        mapView.invalidate();
    }

    private void showMapFilesNotFoundDialog(View view, String sdPath) {
        if (mapFileNotFoundDialogShown) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        String message = String.format(getResources().getString(R.string.map_view_nomaps_text), sdPath + BASEMAP_PATH, sdPath + ORTHOPHOTO_PATH);
        builder.setTitle(R.string.map_view_nomaps_title).setMessage(message).setPositiveButton(R.string.button_ok, null).create().show();
        mapFileNotFoundDialogShown = true;
    }

    @Override
    public void onStart() {
        super.onStart();

        imageButtonGPS = (ImageButton)getView().findViewById(R.id.map_imageButtonGPS);
        imageButtonGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyLocationNewOverlay gpsOverlay = (MyLocationNewOverlay)mapView.getOverlayManager().get(1);
                if (gpsOverlay.isFollowLocationEnabled()) {
                    gpsOverlay.disableFollowLocation();
                } else {
                    gpsOverlay.enableFollowLocation();
                }
            }
        });

        imageButtonDestination = (ImageButton)getView().findViewById(R.id.map_imageButtonDestination);
        imageButtonDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (destinationOverlay.size() > 0) {
                    MyLocationNewOverlay gpsOverlay = (MyLocationNewOverlay)mapView.getOverlayManager().get(1);
                    ItemizedIconOverlay<OverlayItem> destinationOverlay = (ItemizedIconOverlay<OverlayItem>)mapView.getOverlayManager().get(2);
                    gpsOverlay.disableFollowLocation();
                    mapView.getController().animateTo(destinationOverlay.getItem(0).getPoint());
                }
            }
        });

        toggleButtonMapmode = (ToggleButton)getView().findViewById(R.id.map_toggleButtonMapmode);
        toggleButtonMapmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TilesOverlay orthophotoOverlay = (TilesOverlay) mapView.getOverlayManager().get(0);
                showOrtophoto = !showOrtophoto;
                orthophotoOverlay.setEnabled(showOrtophoto);
                mapView.invalidate();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        gpsOverlay.enableMyLocation();
        if (followLocation || mapCenter == null) {
            gpsOverlay.enableFollowLocation();
        }

        if (destinationTitle != null) {
            setDestination(destinationLatitude, destinationLongitude, destinationTitle);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        followLocation = gpsOverlay.isFollowLocationEnabled();
        mapCenter = (GeoPoint)mapView.getMapCenter();
        mapZoom = mapView.getZoomLevel();

        gpsOverlay.disableFollowLocation();
        gpsOverlay.disableMyLocation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mapView.getTileProvider().clearTileCache();
        mapView.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mapView != null) {
            outState.putParcelable("center", (GeoPoint)mapView.getMapCenter());
            outState.putInt("zoom", mapView.getZoomLevel());
            outState.putBoolean("follow", gpsOverlay.isFollowLocationEnabled());
        } else if (mapCenter != null) {
            outState.putParcelable("center", mapCenter);
            outState.putInt("zoom", mapZoom);
            outState.putBoolean("follow", followLocation);
        }
        outState.putBoolean("showOrtophoto", showOrtophoto);
    }

    public void setDestination(double latitude, double longitude, String title) {
        if (mapView != null) {
            if (destinationOverlay.size() > 0) {
                destinationOverlay.removeAllItems();
            }

            if (gpsOverlay.isFollowLocationEnabled()) {
                gpsOverlay.disableFollowLocation();
            }

            OverlayItem destination = new OverlayItem(title, null, new GeoPoint(latitude, longitude));
            destinationOverlay.addItem(destination);
            mapView.getController().animateTo(destination.getPoint());
            imageButtonDestination.setVisibility(View.VISIBLE);
        } else {
            // Save given data until the map view is created (the fragment is probably currently not yet built)
            destinationLatitude = latitude;
            destinationLongitude = longitude;
            destinationTitle = title;
        }
    }
}
