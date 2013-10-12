package tk.crazysoft.ego.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EGODbHelper extends SQLiteOpenHelper {
    // TODO: Change the Database Version whenever the schema changes
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "EGO.db";

    public static enum BooleanComposition {
        AND, OR
    }

    public EGODbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(EGOContract.SQL_CREATE_ADRESSES);
        db.execSQL(EGOContract.SQL_CREATE_HOSPITAL_ADMISSION);
        db.execSQL(EGOContract.SQL_CREATE_DOCTOR_STANDBY);
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
}
