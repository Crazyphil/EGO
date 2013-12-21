package tk.crazysoft.ego.data;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTile;

public class QuadTreeTileSource extends org.osmdroid.tileprovider.tilesource.QuadTreeTileSource {

    public QuadTreeTileSource(String aName, ResourceProxy.string aResourceId, int aZoomMinLevel, int aZoomMaxLevel, int aTileSizePixels, String aImageFilenameEnding, String... aBaseUrl) {
        super(aName, aResourceId, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding, aBaseUrl);
    }

    @Override
    public String getTileRelativeFilenameString(MapTile tile) {
        final StringBuilder sb = new StringBuilder();
        sb.append(pathBase());
        sb.append('/');
        sb.append(quadTree(tile));
        sb.append(imageFilenameEnding());
        return sb.toString();
    }
}
