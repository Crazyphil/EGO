package tk.crazysoft.ego.io;

import android.content.Context;
import android.os.Environment;

import tk.crazysoft.ego.preferences.Preferences;

public class ExternalStorage {
    public static final String TAG = "tk.crazysoft.ego.io.ExternalStorage";

    private ExternalStorage() { }

    /**
     * @return True if the external storage is available. False otherwise.
     */
    public static boolean isAvailable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the path to the first available SD Card, with preference for external, removable ones.
     * @return The SD Card storage path with trailing /
     */
    public static String getSdCardPath(Context context) {
        return getSdCardPath(context, false);
    }

    /**
     * Returns the path to the first available SD Card, with preference for external, removable ones.
     * @param alwaysInternal Indicates if the "internal" external storage should always be returned
     * @return The SD Card storage path with trailing /
     */
    public static String getSdCardPath(Context context, boolean alwaysInternal) {
        Preferences preferences = new Preferences(context);
        boolean preferenceUseSd = preferences.getImportUseSd();

        String path = null;
        if (!preferenceUseSd || alwaysInternal) {
            return Environment.getExternalStorageDirectory().getPath() + "/";
        }
        else {
            Environment4.Device[] devices = Environment4.getExternalStorage(context);
            Environment4.Device sdDevice = null, primaryDevice = null;
            for (Environment4.Device device : devices) {
                if (device.getType() == Environment4.TYPE_SD) {
                    sdDevice = device;
                } else if (device.getType() == Environment4.TYPE_PRIMARY) {
                    primaryDevice = device;
                }
            }

            if (sdDevice != null && sdDevice.canRead()) {
                path = sdDevice.getAbsolutePath() + "/";
            }
            else if (primaryDevice != null && primaryDevice.canRead()) {
                path = primaryDevice.getAbsolutePath() + "/";
            }
        }
        return path;
    }

    /**
     * @return True if the external storage is writable. False otherwise.
     */
    public static boolean isWritable() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }
}