package tk.crazysoft.ego;

import android.app.AlertDialog;
import android.content.res.TypedArray;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import tk.crazysoft.ego.data.DatabaseFileArchive;
import tk.crazysoft.ego.io.ExternalStorage;
import tk.crazysoft.ego.preferences.Preferences;

public class MapFragment extends Fragment {
    private static final String TAG = "MapFragment";
    private static final String MAP_PATH = "ego/karten/";
    private static final String BASEMAP_FILE = "basemap.sqlite";
    private static final String ORTHOPHOTO_FILE = "orthofoto.sqlite";
    private static final int TILE_SIZE = 256;

    protected MapView mapView;
    protected ImageButton imageButtonGPS, imageButtonDestination;
    protected ToggleButton toggleButtonMapmode;
    protected TilesOverlay orthophotoOverlay;
    protected MyLocationNewOverlay gpsOverlay;
    protected ItemizedIconOverlay<OverlayItem> destinationOverlay;
    protected boolean mapFileNotFoundDialogShown = false;
    protected GeoPoint mapCenter;
    protected int mapZoom;
    protected boolean followLocation = true, showOrtophoto = false;
    protected GeoPoint destination;
    protected String destinationTitle;

    protected String sdPath;

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

        sdPath = ExternalStorage.getSdCardPath(getActivity());
        createMapView(view);
        return view;
    }

    private void createMapView(View view) {
        Preferences preferences = new Preferences(view.getContext());

        IArchiveFile[] basemapArchives = getArchiveFiles(BASEMAP_FILE);
        if (basemapArchives.length == 0) {
            showMapFilesNotFoundDialog(view, sdPath);
        }

        IArchiveFile[] orthophotoArchives = getArchiveFiles(ORTHOPHOTO_FILE);
        if (orthophotoArchives.length == 0) {
            Log.e(TAG, "Ortophoto archive file(s) could not be loaded");
        }

        // The TileSource objects are not used in our custom DatabaseFileArchive implementation, but they are needed by the provider
        ITileSource basemapSource = new XYTileSource("basemap.at", getMinZoomLevel(basemapArchives), getMaxZoomLevel(basemapArchives), TILE_SIZE,
                ".jpg", new String[] { "http://maps.wien.gv.at/basemap/geolandbasemap/" });
        IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(view.getContext());
        MapTileFileArchiveProvider basemapProvider = new MapTileFileArchiveProvider(registerReceiver, basemapSource, basemapArchives);
        MapTileProviderArray basemapProviderArray = new MapTileProviderArray(basemapSource, registerReceiver, new MapTileModuleProviderBase[] { basemapProvider });

        ResourceProxy proxy = new DefaultResourceProxyImpl(getActivity().getApplicationContext());
        mapView = new MapView(view.getContext(), proxy, basemapProviderArray);

        // Set background for loading/missing tiles depending on day/night mode and optionally enable night mode (invert colors)
        TypedArray a = getActivity().getTheme().obtainStyledAttributes(new int[] { R.attr.tilesLoadingBackgroundColor, R.attr.tilesLoadingLineColor });
        mapView.getOverlayManager().getTilesOverlay().setLoadingBackgroundColor(a.getColor(0, 0));
        mapView.getOverlayManager().getTilesOverlay().setLoadingLineColor(a.getColor(1, 0));
        if (MainActivity.hasDarkTheme(getActivity())) {
            mapView.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        }
        a.recycle();

        if (orthophotoArchives.length > 0) {
            ITileSource orthophotoSource = new XYTileSource("geoimage.at", getMinZoomLevel(orthophotoArchives), getMaxZoomLevel(orthophotoArchives), TILE_SIZE,
                    ".jpg", new String[]{"http://maps.wien.gv.at/basemap/bmaporthofoto30cm/"});
            MapTileFileArchiveProvider ortophotoProvider = new MapTileFileArchiveProvider(registerReceiver, orthophotoSource, orthophotoArchives);
            MapTileProviderArray orthophotoProviderArray = new MapTileProviderArray(orthophotoSource, registerReceiver, new MapTileModuleProviderBase[]{ortophotoProvider});

            orthophotoOverlay = new TilesOverlay(orthophotoProviderArray, proxy);
            orthophotoOverlay.setLoadingBackgroundColor(getResources().getColor(android.R.color.transparent));
            orthophotoOverlay.setEnabled(showOrtophoto);
            mapView.getOverlayManager().add(orthophotoOverlay);
        }

        gpsOverlay = new MyLocationNewOverlay(view.getContext(), mapView);
        destinationOverlay = new ItemizedIconOverlay<OverlayItem>(view.getContext(), new ArrayList<OverlayItem>(1), null);

        // Note: Overlays stored in member variables are referenced by their index in onStart()
        mapView.getOverlayManager().add(gpsOverlay);
        mapView.getOverlayManager().add(destinationOverlay);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        ((RelativeLayout)view.findViewById(R.id.map_layoutRoot)).addView(mapView, 0, params);

        GeoPoint defaultCenter = new GeoPoint(preferences.getMapLatitude(), preferences.getMapLongitude());

        mapView.setUseDataConnection(false);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.setTilesScaledToDpi(true);

        if (mapCenter != null) {
            mapView.getController().setZoom(mapZoom);
            mapView.getController().setCenter(mapCenter);
        } else {
            mapView.getController().setZoom(17);
            mapView.getController().setCenter(defaultCenter);
        }

        mapView.invalidate();
    }

    private IArchiveFile[] getArchiveFiles(String mapTemplate) {
        File mapDirectory = new File(sdPath + MAP_PATH);
        File[] files = mapDirectory.listFiles(new MapFilenameFilter(mapTemplate));
        if (files == null) {
            return new IArchiveFile[0];
        }

        ArrayList<IArchiveFile> archiveFiles = new ArrayList<IArchiveFile>(files.length);
        for (File file : files) {
            try {
                IArchiveFile archive = DatabaseFileArchive.getDatabaseFileArchive(file);
                archiveFiles.add(archive);
                Log.d(TAG, String.format("Adding %s to tile archive array", file.getName()));
            } catch (final SQLiteException e) {
                Log.e(TAG, "Error opening SQL file", e);
            }
        }
        return archiveFiles.toArray(new IArchiveFile[archiveFiles.size()]);
    }

    private int getMinZoomLevel(IArchiveFile[] archiveFiles) {
        int minZoom = Integer.MAX_VALUE;
        for (IArchiveFile archiveFile : archiveFiles) {
            minZoom = Math.min(minZoom, ((DatabaseFileArchive)archiveFile).getMinZoomLevel());
        }
        return minZoom;
    }

    private int getMaxZoomLevel(IArchiveFile[] archiveFiles) {
        int maxZoom = 0;
        for (IArchiveFile archiveFile : archiveFiles) {
            maxZoom = Math.max(maxZoom, ((DatabaseFileArchive) archiveFile).getMaxZoomLevel());
        }
        return maxZoom;
    }

    private void showMapFilesNotFoundDialog(View view, String sdPath) {
        if (mapFileNotFoundDialogShown) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        String message = String.format(getResources().getString(R.string.map_view_nomaps_text), sdPath + MAP_PATH + BASEMAP_FILE, sdPath + MAP_PATH +ORTHOPHOTO_FILE);
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
                    gpsOverlay.disableFollowLocation();
                    mapView.getController().animateTo(destinationOverlay.getItem(0).getPoint());
                }
            }
        });

        toggleButtonMapmode = (ToggleButton)getView().findViewById(R.id.map_toggleButtonMapmode);
        if (orthophotoOverlay != null) {  // The overlay manager only contains exactly 3 overlays if an orthophoto file was found
            toggleButtonMapmode.setVisibility(View.VISIBLE);
            toggleButtonMapmode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showOrtophoto = !showOrtophoto;
                    orthophotoOverlay.setEnabled(showOrtophoto);
                    mapView.invalidate();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        gpsOverlay.enableMyLocation();
        if (followLocation || mapCenter == null) {
            gpsOverlay.enableFollowLocation();
        }

        if (destination != null) {
            setDestination(destination, destinationTitle);
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
        mapView = null;
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
        setDestination(new GeoPoint(latitude, longitude), title);
    }

    protected void setDestination(GeoPoint dest, String title) {
        if (mapView != null) {
            if (destinationOverlay.size() > 0) {
                destinationOverlay.removeAllItems();
            }

            if (title != null && gpsOverlay.isFollowLocationEnabled()) {
                gpsOverlay.disableFollowLocation();
            }

            OverlayItem destination = new OverlayItem(title, null, dest);
            destinationOverlay.addItem(destination);

            if (title != null) {
                mapView.getController().animateTo(destination.getPoint());
            }
            imageButtonDestination.setVisibility(View.VISIBLE);
        } else {
            // Save given data until the map view is created (the fragment is probably currently not yet built)
            destination = dest;
            destinationTitle = title;
        }
    }

    private class MapFilenameFilter implements FilenameFilter {
        private final String templateName;
        private final String templateExt;

        public MapFilenameFilter(String template) {
            int lastDot = template.lastIndexOf('.');

            if (lastDot == -1) {
                this.templateName = template;
                templateExt = null;
            } else {
                this.templateName = template.substring(0, lastDot);
                this.templateExt = template.substring(lastDot);
            }
        }

        @Override
        public boolean accept(File dir, String filename) {
            int firstDot = filename.indexOf('.');
            if (firstDot == -1) {
                return templateExt == null && templateName.equals(filename);
            }

            int lastDot = filename.lastIndexOf('.');
            String name = filename.substring(0, firstDot);
            String ext = filename.substring(lastDot);

            return templateName.equals(name) && templateExt.equals(ext);
        }
    }
}
