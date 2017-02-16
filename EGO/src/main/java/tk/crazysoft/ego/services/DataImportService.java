package tk.crazysoft.ego.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;
import tk.crazysoft.ego.R;
import tk.crazysoft.ego.components.MultiPartFilenameFilter;
import tk.crazysoft.ego.data.AddressImporter;
import tk.crazysoft.ego.data.AdmittanceImporter;
import tk.crazysoft.ego.data.EGODbHelper;
import tk.crazysoft.ego.data.Importer;
import tk.crazysoft.ego.data.StandbyImporter;
import tk.crazysoft.ego.io.ExternalStorage;
import tk.crazysoft.ego.io.InputPositionReader;

public class DataImportService extends IntentService {
    private static final String TAG = "DataImportService";

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
    public static final String EXTRA_RESULT_RESULT = "tk.crazysoft.ego.services.IMPORT_RESULT_RESULT";
    public static final String BROADCAST_COMPLETED = "tk.crazysoft.ego.services.IMPORT_COMPLETED";
    public static final String EXTRA_COMPLETED_ACTION = "tk.crazysoft.ego.services.IMPORT_COMPLETED_ACTION";

    private static final String IMPORT_PATH = "ego/import/";
    private static final String ADDRESSES_FILE = "adressen.csv";
    private static final String ADMITTANCES_FILE = "aufnahmen.csv";
    private static final String STANDBY_FILE = "bereitschaft.csv";
    private static final char CSV_SEPARATOR = ';';
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
                path = ADDRESSES_FILE;
            } else if (action.equals(ACTION_IMPORT_HOSPITAL_ADMITTANCES)) {
                importer = new AdmittanceImporter(this);
                path = ADMITTANCES_FILE;
            } else if (action.equals(ACTION_IMPORT_DOCTOR_STANDBY)) {
                importer = new StandbyImporter(this);
                path = STANDBY_FILE;
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

                    @Override
                    public void onResult(String action, boolean result) {
                        reportResultPostProcess(action, result);
                    }
                });
                startImport(path, importer);
            }
        }

        reportCompleted(intent.getAction());
        DataImportReceiver.completeWakefulIntent(intent);
    }

    private void startImport(String file, Importer importer) {
        String sdCard = ExternalStorage.getSdCardPath(this);
        if (sdCard == null) {
            reportError(getResources().getString(R.string.service_dataimport_error_nosd));
            return;
        }

        File[] docs = getFiles(sdCard, file);
        if (docs.length == 0) {
            reportError(getResources().getString(R.string.service_dataimport_error_notfound));
            return;
        }

        processFile(docs, importer);
    }

    private File[] getFiles(String sdPath, String fileTemplate) {
        File directory = new File(sdPath + IMPORT_PATH);
        File[] files = directory.listFiles(new MultiPartFilenameFilter(fileTemplate, true));
        if (files == null) {
            Log.d(TAG, String.format("No files to import for template %s", fileTemplate));
            return new File[0];
        }

        Log.d(TAG, String.format("Found %d files to import for template %s", files.length, fileTemplate));
        return files;
    }

    private void processFile(File[] doc, Importer importer) {
        reportProgress(0);
        EGODbHelper helper = new EGODbHelper(getBaseContext(), false);
        db = helper.getWritableDatabase();
        if (db == null) {
            reportError(getResources().getString(R.string.error_db_object_null));
            return;
        }

        db.beginTransaction();
        importer.setDatabase(db);
        importer.preProcess();

        InputPositionReader posRdr;
        CSVReader rdr = null;
        try {
            int numEntries = 0;
            int failedEntries = 0;
            for (int i = 0; i < doc.length; i++) {
                if (i > 0 && !importer.onContinueNextFile()) {
                    // Importer doesn't accept more than one input file, ignore any further data
                    break;
                }

                Log.d(TAG, String.format("Importing %s", doc[i].getAbsolutePath()));
                try {
                    posRdr = new InputPositionReader(new FileReader(doc[i]));
                    rdr = new CSVReader(posRdr, CSV_SEPARATOR);
                } catch (FileNotFoundException e) {
                    // Shouldn't occur, because the caller checks for existence
                    Log.wtf(TAG, "File was not found even though checked before!?", e);
                    return;
                }

                long fileLength = doc[i].length();
                double lastPercent = 0;

                String[] fields = rdr.readNext();
                fields[0] = fields[0].replace("\uFEFF", "");    // Replace UTF-8 BOM in first field of the file if it exists
                while (fields != null) {
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
                    double newPercent = posRdr.getPosition() / (double) fileLength;
                    if (newPercent > lastPercent) {
                        lastPercent = newPercent;
                        reportProgress(lastPercent);
                    }
                    fields = rdr.readNext();
                }
                rdr.close();
            }
            db.setTransactionSuccessful();
            db.endTransaction();

            reportResultImport(numEntries - failedEntries, failedEntries);
            importer.postProcess();
        } catch (IOException e) {
            Log.e(TAG, "Error importing file", e);
            reportError(getResources().getString(R.string.service_dataimport_error_readfail));
        } finally {
            try {
                if (rdr != null) {
                    rdr.close();
                }
            } catch (Exception e) {
                // Nothing that can be done here
            }

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

    private void reportResultPostProcess(String action, boolean result) {
        Intent intent = new Intent(BROADCAST_RESULT_POSTPROCESS).putExtra(EXTRA_RESULT_RESULT, result).putExtra(EXTRA_RESULT_ACTION, action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void reportCompleted(String action) {
        Intent intent = new Intent(BROADCAST_COMPLETED).putExtra(EXTRA_COMPLETED_ACTION, action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
