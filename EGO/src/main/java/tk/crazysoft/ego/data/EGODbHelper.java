package tk.crazysoft.ego.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.os.Build;
import android.util.Log;

public class EGODbHelper extends SQLiteOpenHelper {
    // TODO: Change the Database Version whenever the schema changes
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "EGO.db";

    public static enum BooleanComposition {
        AND, OR
    }

    public EGODbHelper(Context context) {
        this(context, true);
    }

    public EGODbHelper(Context context, boolean useLeaklessCursor) {
        super(context, DATABASE_NAME, useLeaklessCursor ? new LeaklessCursorFactory() : null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(EGOContract.SQL_CREATE_ADRESSES);
        db.execSQL(EGOContract.SQL_CREATE_HOSPITAL_ADMISSION);
        db.execSQL(EGOContract.SQL_CREATE_DOCTOR_STANDBY);
        db.execSQL(EGOContract.SQL_CREATE_NAME_REPLACEMENTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: Add upgrade code from previous versions when schema changes
    }

    public static String createSimpleSelection(String[] columns, BooleanComposition composition) {
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        for (String column : columns) {
            if (pos > 0) {
                sb.append(" ").append(composition.name()).append(" ");
            }
            sb.append(column).append(" = ?");
            pos++;
        }
        return sb.toString();
    }

    // Source: http://stackoverflow.com/questions/4547461/closing-the-database-in-a-contentprovider/9044791#9044791
    public static class LeaklessCursor extends SQLiteCursor {
        static final String TAG = "LeaklessCursor";

        public LeaklessCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
            super(db, driver, editTable, query);
        }

        public LeaklessCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
            super(driver, editTable, query);
        }

        @Override
        public void close() {
            final SQLiteDatabase db = getDatabase();
            super.close();
            if (db != null) {
                Log.d(TAG, "Closing LeaklessCursor: " + db.getPath());
                db.close();
            }
        }
    }

    public static class LeaklessCursorFactory implements SQLiteDatabase.CursorFactory {
        @Override
        public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                return new LeaklessCursor(masterQuery, editTable, query);
            } else {
                return new LeaklessCursor(db, masterQuery, editTable, query);
            }
        }
    }
}
