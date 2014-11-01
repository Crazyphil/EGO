package tk.crazysoft.ego.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.text.TextUtils;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import java.util.ArrayList;
import java.util.HashMap;

public class AddressImporter extends Importer {
    public static final String ADDRESS_IMPORTER_POSTPOCESS_ACTION = "tk.crazysoft.ego.data.MERGE_STREETS";

    private static final String EAST_COORD_COLUMN = "RW";
    private static final String NORTH_COORD_COLUMN = "HW";
    private static final String ZIP_COLUMN = "PLZ";
    private static final String CITY_COLUMN = "GEMEINDENAME";
    private static final String STREET_COLUMN = "STRASSENNAME";
    private static final String STREETNO_COLUMN = "HNR_ZUSAMMEN";
    private static final String ADDRESSCODE_COLUMN = "ADRCD";
    private static final String MAPSHEET_COLUMN = "KARTENBLATT";

    private static final int MAX_STREET_MERGE_METERS = 1000;

    private AddressColumns columns;
    private CoordinateTransform projection;
    private int lastAddressCode;

    public AddressImporter(Context context) {
        super(context);
    }

    @Override
    public void preProcess() {
        getDatabase().delete(EGOContract.Addresses.TABLE_NAME, null, null);
    }

    @Override
    public int process(String[] line) {
        if (columns == null) {
            columns = findAddressColumnPositions(line);
            return PROCESS_IGNORED;
        }

        try {
            double eastCoord = Double.parseDouble(csvTrim(line[columns.eastCoord]));
            double northCoord = Double.parseDouble(csvTrim(line[columns.northCoord]));
            String zipCode = csvTrim(line[columns.zipCode]);
            String city = csvTrim(line[columns.city]);
            String street = csvTrim(line[columns.street]);
            String streetno = csvTrim(line[columns.streetno]);
            int addrcode = Integer.parseInt(csvTrim(line[columns.addresscode]));
            String mapsheet = "";
            if (columns.mapsheet >= 0) {
                mapsheet = csvTrim(line[columns.mapsheet]);
            }

            if (TextUtils.isEmpty(streetno) || lastAddressCode == addrcode) {
                return PROCESS_IGNORED;
            }
            lastAddressCode = addrcode;

            ProjCoordinate geoCoords = convertEPSG31255toWSG84(eastCoord, northCoord);
            if (geoCoords == null || !insertAddress(geoCoords.y, geoCoords.x, zipCode, city, street, streetno, mapsheet)) {
                return PROCESS_ERROR;
            }
        } catch (NumberFormatException e) {
            return PROCESS_ERROR;
        } catch (ArrayIndexOutOfBoundsException e) {
            return PROCESS_ERROR;
        }
        return PROCESS_SUCCESS;
    }

    private AddressColumns findAddressColumnPositions(String[] fields) {
        AddressColumns columns = new AddressColumns();
        columns.eastCoord = findStringPosInArray(fields, EAST_COORD_COLUMN);
        columns.northCoord = findStringPosInArray(fields, NORTH_COORD_COLUMN);
        columns.zipCode = findStringPosInArray(fields, ZIP_COLUMN);
        columns.city = findStringPosInArray(fields, CITY_COLUMN);
        columns.street = findStringPosInArray(fields, STREET_COLUMN);
        columns.streetno = findStringPosInArray(fields, STREETNO_COLUMN);
        columns.addresscode = findStringPosInArray(fields, ADDRESSCODE_COLUMN);
        columns.mapsheet = findStringPosInArray(fields, MAPSHEET_COLUMN);

        if (columns.eastCoord >= 0 && columns.northCoord >= 0 && columns.zipCode >= 0 && columns.city >= 0 && columns.street >= 0 && columns.streetno >= 0 && columns.addresscode >= 0) {
            return columns;
        }
        return null;
    }

    private ProjCoordinate convertEPSG31255toWSG84(double east, double north) {
        ProjCoordinate src = new ProjCoordinate(east, north);
        if (east < 128 && north < 128) {
            return src;
        }

        if (projection == null) {
            CRSFactory crsFactory = new CRSFactory();
            CoordinateTransformFactory transFactory = new CoordinateTransformFactory();

            CoordinateReferenceSystem sourceProjection = crsFactory.createFromName("EPSG:31255");
            CoordinateReferenceSystem targetProjection = crsFactory.createFromParameters("WSG84", "+proj=latlong +datum=WGS84");
            projection = transFactory.createTransform(sourceProjection, targetProjection);

        }

        ProjCoordinate target = new ProjCoordinate();
        target = projection.transform(src, target);
        return target;
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

        return getDatabase().insert(EGOContract.Addresses.TABLE_NAME, null, values) > -1;
    }

    @Override
    public void postProcess() {
        if (getOnPostProcessProgressListener() != null) {
            getOnPostProcessProgressListener().onProgress(0);
        }

        Cursor result = getDatabase().rawQuery(EGOContract.Addresses.SQL_SELECT_DUPLICATE_STREETS, null);
        getDatabase().beginTransaction();
        int mergedStreets = 0;
        double count = (double)result.getCount();
        while (result.moveToNext()) {
            if (getOnPostProcessProgressListener() != null) {
                getOnPostProcessProgressListener().onProgress(result.getPosition() / count);
            }

            int lastAddressId = result.getInt(result.getColumnIndex("last_id"));
            String street = result.getString(result.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_STREET));
            ArrayList<Address> addresses = getAddressList(street);
            if (mergeStreet(addresses, lastAddressId)) {
                mergedStreets++;
            }
        }
        result.close();
        getDatabase().setTransactionSuccessful();

        if (getOnPostProcessProgressListener() != null) {
            getOnPostProcessProgressListener().onResult(ADDRESS_IMPORTER_POSTPOCESS_ACTION, result.getCount(), mergedStreets);
        }
    }

    private ArrayList<Address> getAddressList(String street) {
        String[] projection = new String[] {
                EGOContract.Addresses.COLUMN_NAME_ZIP,
                EGOContract.Addresses.COLUMN_NAME_CITY,
                EGOContract.Addresses.COLUMN_NAME_STREET_NO,
                EGOContract.Addresses.COLUMN_NAME_LATITUDE,
                EGOContract.Addresses.COLUMN_NAME_LONGITUDE
        };
        String[] selectionArgs = new String[] { street };
        Cursor result = getDatabase().query(EGOContract.Addresses.TABLE_NAME, projection, EGOContract.Addresses.COLUMN_NAME_STREET + " = ?", selectionArgs, null, null, null);

        HashMap<String, Address> streets = new HashMap<String, Address>();
        ArrayList<Address> addresses = new ArrayList<Address>();
        while (result.moveToNext()) {
            String zipCode = result.getString(result.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_ZIP));
            String city = result.getString(result.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_CITY));
            String streetNo = result.getString(result.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_STREET_NO));
            double latitude = result.getDouble(result.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_LATITUDE));
            double longitude = result.getDouble(result.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_LONGITUDE));

            if (!streets.containsKey(zipCode)) {
                Address address = new Address(zipCode, city, street);

                addresses.add(address);
                streets.put(zipCode, address);
            }
            streets.get(zipCode).getAddresses().add(new Address.House(streetNo, new ProjCoordinate(longitude, latitude)));
        }

        result.close();
        return addresses;
    }

    private boolean mergeStreet(ArrayList<Address> addresses, int lastAddressId) {
        boolean merged = false;
        for (Address address : addresses) {
            for (Address compareAddress : addresses) {
                if (address.equals(compareAddress)) {
                    continue;
                }

                if (address.isDistinct(compareAddress)) {
                    double nearestDistance = address.getNearestDistance(compareAddress);
                    if (nearestDistance < MAX_STREET_MERGE_METERS) {
                        if (copyStreet(compareAddress, address, lastAddressId)) {
                            merged = true;
                        }
                    }
                }
            }
        }
        return merged;
    }

    private boolean copyStreet(Address source, Address target, int lastAddressId) {
        SQLiteStatement sql = getDatabase().compileStatement(EGOContract.Addresses.SQL_COPY_STREET);
        sql.bindString(1, target.getZipCode());
        sql.bindString(2, target.getCity());
        sql.bindLong(3, lastAddressId);     // Avoids duplicating already copied entries
        sql.bindString(4, source.getZipCode());
        sql.bindString(5, source.getCity());
        sql.bindString(6, source.getStreet());

        int affectedRows = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            affectedRows = sql.executeUpdateDelete();
        } else {
            sql.execute();
        }
        sql.close();
        return affectedRows > 0;
    }

    private class AddressColumns {
        public int eastCoord;
        public int northCoord;
        public int zipCode;
        public int city;
        public int street;
        public int streetno;
        public int addresscode;
        public int mapsheet;
    }
}
