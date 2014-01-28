package tk.crazysoft.ego.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import tk.crazysoft.ego.R;
import tk.crazysoft.ego.data.AddressImporter;
import tk.crazysoft.ego.data.AdmittanceImporter;
import tk.crazysoft.ego.data.EGODbHelper;
import tk.crazysoft.ego.data.Importer;
import tk.crazysoft.ego.data.StandbyImporter;
import tk.crazysoft.ego.io.ExternalStorage;
import tk.crazysoft.ego.io.InputPositionReader;

public class DataImportService extends IntentService {
    public static final String ACTION_IMPORT_ADDRESSES = "tk.crazysoft.ego.services.IMPORT_ADDRESSES";
    public static final String ACTION_IMPORT_HOSPITAL_ADMITTANCES = "tk.crazysoft.ego.services.IMPORT_ADMITTANCES";
    public static final String ACTION_IMPORT_DOCTOR_STANDBY = "tk.crazysoft.ego.services.IMPORT_STANDBY";

    public static final String BROADCAST_ERROR = "tk.crazysoft.ego.services.IMPORT_ERROR";
    public static final String EXTRA_ERROR_MESSAGE = "tk.crazysoft.ego.services.IMPORT_ERROR_MESSAGE";
    public static final String BROADCAST_PROGRESS = "tk.crazysoft.ego.services.IMPORT_PROGRESS";
    public static final String EXTRA_PROGRESS_PERCENT = "tk.crazysoft.ego.services.IMPORT_PROGRESS_PERCENT";
    public static final String BROADCAST_RESULT_IMPORT = "tk.crazysoft.ego.services.IMPORT_RESULT_IMPORT";
    public static final String BROADCAST_RESULT_POSTPROCESS = "tk.crazysoft.ego.services.IMPORT_RESULT_POSTPROCESS";
    public static final String EXTRA_RESULT_ACTION = "tk.crazysoft.ego.services.IMPORT_RESULT_ACTION";
    public static final String EXTRA_RESULT_COUNTS = "tk.crazysoft.ego.services.IMPORT_RESULT_COUNTS";

    private static final String ADDRESSES_PATH = "ego/import/adressen.csv";
    private static final String ADMITTANCES_PATH = "ego/import/aufnahmen.csv";
    private static final String STANDBY_PATH = "ego/import/bereitschaft.csv";
    private static final String CSV_SEPARATOR = ";";
    private static final String CSV_COMMENT = "#";

    private SQLiteDatabase db = null;

    public DataImportService() {
        super(DataImportService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        if (action != null) {
            Importer importer = null;
            String path = "";
            if (action.equals(ACTION_IMPORT_ADDRESSES)) {
                importer = new AddressImporter(this);
                path = ADDRESSES_PATH;
            } else if (action.equals(ACTION_IMPORT_HOSPITAL_ADMITTANCES)) {
                importer = new AdmittanceImporter(this);
                path = ADMITTANCES_PATH;
            } else if (action.equals(ACTION_IMPORT_DOCTOR_STANDBY)) {
                importer = new StandbyImporter(this);
                path = STANDBY_PATH;
            }
            if (importer != null) {
                importer.setOnPostProcessProgressListener(new Importer.OnPostProcessProgressListener() {
                    @Override
                    public void onProgress(double progressPercent) {
                        reportProgress(progressPercent);
                    }

                    @Override
                    public void onResult(String action, int processed, int modified) {
                        reportResultPostProcess(action, processed, modified);
                    }
                });
                startImport(path, importer);
            }
        }

        DataImportReceiver.completeWakefulIntent(intent);
    }

    private void startImport(String path, Importer importer) {
        String sdCard = ExternalStorage.getSdCardPath(this);
        if (sdCard == null) {
            reportError(getResources().getString(R.string.service_dataimport_error_nosd));
            return;
        }

        File doc = new File(sdCard + path);
        if (!doc.exists() || !doc.isFile() || !doc.canRead()) {
            reportError(getResources().getString(R.string.service_dataimport_error_notfound));
            return;
        }

        processFile(doc, importer);
    }

    private void processFile(File doc, Importer importer) {
        InputPositionReader posRdr;
        BufferedReader rdr;
        try {
            posRdr = new InputPositionReader(new FileReader(doc));
            rdr = new BufferedReader(posRdr);
        } catch (FileNotFoundException e) {
            // Shouldn't occur, because the caller checks for existence
            e.printStackTrace();
            return;
        }

        reportProgress(0);
        EGODbHelper helper = new EGODbHelper(getBaseContext());
        db = helper.getWritableDatabase();
        if (db == null) {
            reportError(getResources().getString(R.string.error_db_object_null));
            return;
        }

        db.beginTransaction();
        importer.setDatabase(db);
        importer.preProcess();

        try {
            long fileLength = doc.length();
            double lastPercent = 0;
            int numEntries = 0;
            int failedEntries = 0;

            Pattern csvPattern = Pattern.compile(CSV_SEPARATOR);
            String line = rdr.readLine();
            while (line != null) {
                String[] fields = csvPattern.split(line, -1);
                if (fields.length > 0 && !fields[0].startsWith(CSV_COMMENT)) {
                    int result = importer.process(fields);
                    if (result != Importer.PROCESS_IGNORED) {
                        numEntries++;
                        if (result == Importer.PROCESS_ERROR) {
                            failedEntries++;
                        }
                    }
                }

                // Because BufferedReader reads chunks from the file, line lengths don't correlate with file position
                double newPercent = posRdr.getPosition() / (double)fileLength;
                if (newPercent > lastPercent) {
                    lastPercent = newPercent;
                    reportProgress(lastPercent);
                }
                line = rdr.readLine();
            }

            rdr.close();
            db.setTransactionSuccessful();
            db.endTransaction();

            reportResultImport(numEntries - failedEntries, failedEntries);
            importer.postProcess();
        } catch (IOException e) {
            reportError(getResources().getString(R.string.service_dataimport_error_readfail));
        } finally {
            try {
                rdr.close();
            } catch (Exception e) { }

            if (db.inTransaction()) {
                db.endTransaction();
            }
            db.close();
        }
    }

    private void reportError(String message) {
        Intent intent = new Intent(BROADCAST_ERROR).putExtra(EXTRA_ERROR_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void reportProgress(double progressPercent) {
        Intent intent = new Intent(BROADCAST_PROGRESS).putExtra(EXTRA_PROGRESS_PERCENT, progressPercent);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void reportResultImport(int imported, int failures) {
        Intent intent = new Intent(BROADCAST_RESULT_IMPORT).putExtra(EXTRA_RESULT_COUNTS, new int[] { imported, failures });
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void reportResultPostProcess(String action, int processed, int modified) {
        Intent intent = new Intent(BROADCAST_RESULT_POSTPROCESS).putExtra(EXTRA_RESULT_COUNTS, new int[] { processed, modified }).putExtra(EXTRA_RESULT_ACTION, action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
