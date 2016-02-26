package tk.crazysoft.ego.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.tilesource.ITileSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * This is the OSMdroid style database provider. It's an extremely simply sqlite database schema.
 * CREATE TABLE tiles (key INTEGER PRIMARY KEY, provider TEXT, tile BLOB)
 * where the key is the X/Y/Z coordinates bitshifted using the following algorithm
 * key = ((z << z) + x << z) + y;
 * This class was modified to return tiles regardless of the provider name.
 */
public class DatabaseFileArchive implements IArchiveFile {
    private static final String TAG = "DatabaseFileArchive";
    private static final int MAX_ZOOM_LEVEL = 20;

    public static final String TABLE="tiles";
    private SQLiteDatabase mDatabase;

    public DatabaseFileArchive() {}

    private DatabaseFileArchive(final SQLiteDatabase pDatabase) {
        mDatabase = pDatabase;
    }

    public static DatabaseFileArchive getDatabaseFileArchive(final File pFile) throws SQLiteException {
        return new DatabaseFileArchive(SQLiteDatabase.openDatabase(pFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY));
    }

    public Set<String> getTileSources(){
        Set<String> ret = new HashSet<String>();
        try {
            final Cursor cur = mDatabase.rawQuery("SELECT DISTINCT provider FROM " + TABLE, null);
            while(cur.moveToNext()) {
                ret.add(cur.getString(0));
            }
            cur.close();
        } catch (final Exception e) {
            Log.w(TAG, "Error getting tile sources: ", e);
        }
        return ret;
    }

    @Override
    public void init(File pFile) throws Exception {
        mDatabase = SQLiteDatabase.openDatabase(pFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    public byte[] getImage(final MapTile pTile) {
        try {
            byte[] bits = null;
            final String[] tile = { "tile" };
            final long x = (long)pTile.getX();
            final long y = (long)pTile.getY();
            final long z = (long)pTile.getZoomLevel();
            final long index = ((z << z) + x << z) + y;
            final Cursor cur = mDatabase.query(TABLE, tile, "key = " + index, null, null, null, null);

            if (cur.getCount() != 0) {
                cur.moveToFirst();
                bits = cur.getBlob(0);
            }
            cur.close();
            if (bits != null) {
                return bits;
            }
        } catch(final Throwable e) {
            Log.w(TAG, "Error getting DB stream: " + pTile, e);
        }

        return null;
    }

    @Override
    public InputStream getInputStream(final ITileSource pTileSource, final MapTile pTile) {
        try {
            InputStream ret = null;
            byte[] bits = getImage(pTile);
            if (bits != null)
                ret = new ByteArrayInputStream(bits);
            if (ret != null) {
                return ret;
            }
        } catch (final Throwable e) {
            Log.w(TAG, "Error getting db stream: " + pTile, e);
        }
        return null;
    }

    public int getMinZoomLevel() {
        return getZoomBoundary("MIN");
    }

    public int getMaxZoomLevel() {
        return getZoomBoundary("MAX");
    }

    private int getZoomBoundary(String minMax) {
        final String[] key = { minMax +"(key)" };
        final Cursor cur = mDatabase.query(TABLE, key, null, null, null, null, null);
        long index = 0;
        if (cur.getCount() != 0) {
            cur.moveToFirst();
            index = cur.getLong(0);
        }
        cur.close();

        return calcZoomLevel(index);
    }

    private int calcZoomLevel(long key) {
        for (int level = 0; level <= MAX_ZOOM_LEVEL; level++) {
            if (key <= ((level << level) + (long)Math.pow(2, level) << level) + (long)Math.pow(2, level)) {
                return level;
            }
        }
        return 0;
    }

    @Override
    public void close() {
        mDatabase.close();
    }

    @Override
    public String toString() {
        return "DatabaseFileArchive [mDatabase=" + mDatabase.getPath() + "]";
    }
}
