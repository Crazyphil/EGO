package tk.crazysoft.ego.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import org.proj4.PJ;
import org.proj4.PJException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import tk.crazysoft.ego.R;
import tk.crazysoft.ego.data.EGOContract;
import tk.crazysoft.ego.data.EGODbHelper;
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
    public static final String BROADCAST_RESULT = "tk.crazysoft.ego.services.IMPORT_RESULT";
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

        if (action.equals(ACTION_IMPORT_ADDRESSES)) {
            importAddresses();
        }
    }

    private void importAddresses() {
        String sdCard = ExternalStorage.getSdCardPath();
        if (sdCard == null) {
            reportError(getResources().getString(R.string.service_dataimport_error_nosd));
            return;
        }

        File doc = new File(sdCard + ADDRESSES_PATH);
        if (!doc.exists() || !doc.isFile() || !doc.canRead()) {
            reportError(getResources().getString(R.string.service_dataimport_error_notfound));
            return;
        }

        processAddressFile(doc);
    }

    private void processAddressFile(File doc) {
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
        db.beginTransaction();
        db.delete(EGOContract.Addresses.TABLE_NAME, null, null);

        try {
            long fileLength = doc.length();
            double lastPercent = 0;
            int numEntries = 0;
            int failedEntries = 0;

            Pattern csvPattern = Pattern.compile(CSV_SEPARATOR);
            String line = rdr.readLine();
            while (line != null) {
                String[] fields = csvPattern.split(line);
                if (!fields[0].startsWith(CSV_COMMENT)) {
                    try {
                        double eastCoord = Double.parseDouble(fields[0].trim());
                        double northCoord = Double.parseDouble(fields[1].trim());
                        String zipCode = fields[2].trim();
                        String city = fields[3].trim();
                        String street = fields[5].trim();
                        String streetno = fields[6].trim();

                        double[] geoCoords = convertEPSG31255toWSG84(eastCoord, northCoord);
                        if (geoCoords == null || !insertAddress(geoCoords[1], geoCoords[0], zipCode, city, street, streetno, "")) {
                            failedEntries++;
                        }
                    } catch (NumberFormatException e) {
                        // First line might be CSV column headers, therefore don't count as failed
                        if (numEntries > 0) {
                            failedEntries++;
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        failedEntries++;
                    } finally {
                        numEntries++;
                    }
                }

                // Because BufferedReader reads chunks from the file, line lengths don't correlate with file position
                if (posRdr.getPosition() / fileLength > lastPercent) {
                    lastPercent = posRdr.getPosition() / fileLength;
                    reportProgress(lastPercent);
                }
                line = rdr.readLine();
            }

            rdr.close();
            doc.delete();
            db.setTransactionSuccessful();
            reportResult(numEntries - failedEntries, failedEntries);
        } catch (IOException e) {
            reportError(getResources().getString(R.string.service_dataimport_error_readfail));
        } finally {
            try {
                rdr.close();
            } catch (Exception e) { }
            db.endTransaction();
        }
    }

    private double[] convertEPSG31255toWSG84(double east, double north) {
        PJ source = new PJ("+init=epsg:31255");
        PJ target = new PJ("+proj=latlong +datum=WGS84");
        double[] coordinates = { east, north };

        try {
            source.transform(target, 2, coordinates, 0, 1);
            return coordinates;
        } catch (PJException e) {
            return null;
        }
    }

    private boolean insertAddress(double latitude, double longitude, String zipCode, String city, String street, String streetno, String mapsheet) {
        ContentValues values = new ContentValues();
        values.put(EGOContract.Addresses.COLUMN_NAME_LATITUDE, latitude);
        values.put(EGOContract.Addresses.COLUMN_NAME_LONGITUDE, longitude);
        values.put(EGOContract.Addresses.COLUMN_NAME_ZIP, zipCode);
        values.put(EGOContract.Addresses.COLUMN_NAME_CITY, city);
        values.put(EGOContract.Addresses.COLUMN_NAME_STREET, street);
        values.put(EGOContract.Addresses.COLUMN_NAME_STREET_NO, streetno);
        values.put(EGOContract.Addresses.COLUMN_NAME_MAP_SHEET, mapsheet);
        return db.insert(EGOContract.Addresses.TABLE_NAME, null, values) > -1;
    }

    private void reportError(String message) {
        Intent intent = new Intent(BROADCAST_ERROR).putExtra(EXTRA_ERROR_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void reportProgress(double progressPercent) {
        Intent intent = new Intent(BROADCAST_PROGRESS).putExtra(EXTRA_PROGRESS_PERCENT, progressPercent);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void reportResult(int imported, int failures) {
        Intent intent = new Intent(BROADCAST_RESULT).putExtra(EXTRA_RESULT_COUNTS, new int[] { imported, failures });
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
