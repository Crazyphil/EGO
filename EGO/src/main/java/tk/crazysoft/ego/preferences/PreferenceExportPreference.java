package tk.crazysoft.ego.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import tk.crazysoft.ego.R;
import tk.crazysoft.ego.io.ExternalStorage;

public class PreferenceExportPreference extends Preference implements Preference.OnPreferenceClickListener {
    private static final String TAG = PreferenceExportPreference.class.getName();

    private ProgressBar progressBar;
    private boolean isExportPreference;
    private CopyFilesTask task = null;

    public PreferenceExportPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPersistent(false);
        setWidgetLayoutResource(R.layout.progress_bar_widget);
        setOnPreferenceClickListener(this);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        progressBar = (ProgressBar)view.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
        return view;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getBoolean(index, false);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        isExportPreference = (Boolean)defaultValue;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (task == null || task.getStatus() == AsyncTask.Status.FINISHED) {
            task = new CopyFilesTask();
            task.execute(isExportPreference);
            return true;
        }
        return false;
    }

    private class CopyFilesTask extends AsyncTask<Boolean, Void, Boolean> {
        private static final String DBS_PATH = "databases";
        private static final String PREFS_PATH = "shared_prefs";
        private static final String EXPORT_PATH = "ego/export";

        private String dbFileName, prefFileName;
        private File sdFile;
        private boolean isExport;

        @Override
        protected void onPreExecute() {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            isExport = params[0];

            dbFileName = getDatabaseName();
            prefFileName = getPreferencesName();
            if (dbFileName == null || prefFileName == null) return false;

            String sdPath = ExternalStorage.getSdCardPath(getContext());
            String dataPath = getDataDir();
            if (dataPath == null) return false;

            boolean result;
            if (isExport) {
                result = doExport(dataPath, sdPath);
            } else {
                result = doImport(sdPath, dataPath);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            Resources res = getContext().getResources();
            if (success) {
                if (isExport) {
                    Toast.makeText(getContext(), String.format(res.getString(R.string.preferences_activity_app_options_export_success), sdFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), String.format(res.getString(R.string.preferences_activity_app_options_import_success), sdFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                }
            } else {
                if (isExport) {
                    Toast.makeText(getContext(), String.format(res.getString(R.string.preferences_activity_app_options_export_failed), sdFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), String.format(res.getString(R.string.preferences_activity_app_options_import_failed), sdFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                }
            }
        }

        private boolean doExport(String dataPath, String sdPath) {
            sdFile = new File(sdPath, EXPORT_PATH);
            if (!sdFile.mkdirs() && !sdFile.isDirectory()) {
                Log.e(TAG, "Cannot make directory path " + sdFile.getAbsolutePath());
                return false;
            }
            return copyDb(new File(dataPath, DBS_PATH), sdFile) && copyPrefs(new File(dataPath, PREFS_PATH), sdFile);
        }

        private boolean doImport(String sdPath, String dataPath) {
            boolean result = copyDb(new File(sdPath, EXPORT_PATH), new File(dataPath, DBS_PATH)) && copyPrefs(new File(sdPath, EXPORT_PATH), new File(dataPath, PREFS_PATH));
            if (result) {
                // Restart app to reload everything
                Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(getContext().getPackageName());
                getContext().getApplicationContext().startActivity(launchIntent);
                System.exit(0);
            }
            return result;
        }

        private boolean copyDb(File sourcePath, File destPath) {
            return copyFile(sourcePath, destPath, dbFileName);
        }

        private boolean copyPrefs(File sourcePath, File destPath) {
            return copyFile(sourcePath, destPath, prefFileName);
        }

        // Source: http://stackoverflow.com/questions/6540906/android-simple-export-and-import-of-sqlite-database/15025308#15025308
        private boolean copyFile(File sourcePath, File destPath, String fileName) {
            try {
                if (destPath.canWrite()) {
                    File sourceFile = new File(sourcePath, fileName);
                    File destFile  = new File(destPath, fileName);

                    FileChannel src = new FileInputStream(sourceFile).getChannel();
                    FileChannel dst = new FileOutputStream(destFile).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    return true;
                }
                Log.e(TAG, "Cannot export file, " + destPath + " not writable");
            } catch (Exception e) {
                Log.e(TAG, "Error copying file", e);
            }
            return false;
        }

        private String getDataDir() {
            ApplicationInfo appInfo = getApplicationInfo();
            if (appInfo == null) return null;
            return appInfo.dataDir;
        }

        private ApplicationInfo getApplicationInfo() {
            try {
                PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
                return packageInfo.applicationInfo;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Could not get application info", e);
                return null;
            }
        }

        private String getAppName() {
            ApplicationInfo appInfo = getApplicationInfo();
            if (appInfo == null) return null;
            return (String)getContext().getPackageManager().getApplicationLabel(appInfo);
        }

        private String getDatabaseName() {
            String appName = getAppName();
            if (appName == null) return null;
            return appName + ".db";
        }

        private String getPreferencesName() {
            return getPreferenceManager().getSharedPreferencesName() + ".xml";
        }
    }
}
