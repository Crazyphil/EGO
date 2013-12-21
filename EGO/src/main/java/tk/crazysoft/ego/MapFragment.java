package tk.crazysoft.ego;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
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
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;

import tk.crazysoft.ego.data.QuadTreeTileSource;
import tk.crazysoft.ego.io.ExternalStorage;

public class MapFragment extends Fragment {
    private static final String BASEMAP_PATH = "ego/karten/basemap.sqlite";
    private static final String ORTHOPHOTO_PATH = "ego/karten/orthofoto.sqlite";
    private static final IGeoPoint DEFAULT_CENTER_POINT = new GeoPoint(48.54694331, 14.10540563);

    private MapView mapView;
    private MyLocationNewOverlay gpsOverlay;
    private ImageButton imageButtonGPS;
    private boolean mapFileNotFoundDialogShown = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.map_view, container, false);

        imageButtonGPS = (ImageButton)view.findViewById(R.id.map_imageButtonGPS);
        imageButtonGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gpsOverlay != null) {
                    if (gpsOverlay.isFollowLocationEnabled()) {
                        gpsOverlay.disableFollowLocation();
                    } else {
                        gpsOverlay.enableFollowLocation();
                    }
                }
            }
        });

        String sdPath = ExternalStorage.getSdCardPath();
        File basemapFile = new File(sdPath + BASEMAP_PATH);
        File orthophotoFile = new File(sdPath + ORTHOPHOTO_PATH);

        IArchiveFile basemapArchive = null;
        IArchiveFile orthophotoArchive = null;
        if (!basemapFile.exists()) {
            showMapFilesNotFoundDialog(view, sdPath);
        } else {
            basemapArchive = ArchiveFileFactory.getArchiveFile(new File(sdPath + BASEMAP_PATH));
            if (basemapArchive == null) {
                showMapFilesNotFoundDialog(view, sdPath);
            }
        }
        if (!orthophotoFile.exists()) {
            showMapFilesNotFoundDialog(view, sdPath);
        } else {
            orthophotoArchive = ArchiveFileFactory.getArchiveFile(new File(sdPath + ORTHOPHOTO_PATH));
            if (orthophotoArchive == null) {
                showMapFilesNotFoundDialog(view, sdPath);
            }
        }

        ITileSource basemapSource = new XYTileSource("basemap.at", ResourceProxy.string.offline_mode, 1, 18, 256, ".jpg", "http://maps.wien.gv.at/basemap/geolandbasemap/");
        ITileSource orthophotoSource = new QuadTreeTileSource("geoimage.at", ResourceProxy.string.offline_mode, 5, 17, 256, ".png", "http://srv.doris.at/arcgis/rest/services/");

        IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(view.getContext());
        MapTileFileArchiveProvider basemapProvider = new MapTileFileArchiveProvider(registerReceiver, basemapSource, basemapArchive != null ? new IArchiveFile[] { basemapArchive } : null);
        MapTileFileArchiveProvider ortophotoProvider = new MapTileFileArchiveProvider(registerReceiver, orthophotoSource, orthophotoArchive != null ? new IArchiveFile[] { orthophotoArchive } : null);
        MapTileProviderArray basemapProviderArray = new MapTileProviderArray(basemapSource, registerReceiver, new MapTileModuleProviderBase[] { basemapProvider });
        MapTileProviderArray orthophotoProviderArray = new MapTileProviderArray(orthophotoSource, registerReceiver, new MapTileModuleProviderBase[] { ortophotoProvider });

        ResourceProxy proxy = new DefaultResourceProxyImpl(getActivity().getApplicationContext());
        mapView = new MapView(view.getContext(), 256, proxy, basemapProviderArray);

        TilesOverlay orthophotoOverlay = new TilesOverlay(orthophotoProviderArray, proxy);
        orthophotoOverlay.setLoadingBackgroundColor(getResources().getColor(android.R.color.transparent));
        gpsOverlay = new MyLocationNewOverlay(view.getContext(), new GpsMyLocationProvider(view.getContext()), mapView);
        mapView.getOverlayManager().add(orthophotoOverlay);
        mapView.getOverlayManager().add(gpsOverlay);
        mapView.setUseDataConnection(false);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        ((RelativeLayout)view.findViewById(R.id.map_layoutRoot)).addView(mapView, 0, params);

        return view;
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(17);
        mapView.getController().setCenter(DEFAULT_CENTER_POINT);
        mapView.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (gpsOverlay != null) {
            gpsOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (gpsOverlay != null) {
            gpsOverlay.disableFollowLocation();
            gpsOverlay.disableMyLocation();
        }
    }
}