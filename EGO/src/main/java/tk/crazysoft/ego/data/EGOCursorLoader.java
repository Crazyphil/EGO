package tk.crazysoft.ego.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class EGOCursorLoader extends SimpleCursorLoader {
    private SQLiteDatabase db;
    private String table;
    private String limit;
    private boolean distinct = false;

    public EGOCursorLoader(Context context, String table) {
        this(context, table, null, null, null, null);
    }

    public EGOCursorLoader(Context context, String table, String[] projection, String selection, String[] selectionArgs) {
        this(context, table, projection, selection, selectionArgs, null);
    }

    public EGOCursorLoader(Context context, String table, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        this(context, table, projection, selection, selectionArgs, sortOrder, null);
    }

    public EGOCursorLoader(Context context, String table, String[] projection, String selection, String[] selectionArgs, String sortOrder, boolean distinct) {
        this(context, table, projection, selection, selectionArgs, sortOrder);
        this.distinct = distinct;
    }

    public EGOCursorLoader(Context context, String table, String[] projection, String selection, String[] selectionArgs, String sortOrder, String limit) {
        super(context, projection, selection, selectionArgs, sortOrder);
        EGODbHelper helper = new EGODbHelper(context);

        try {
            db = helper.getReadableDatabase();
        } catch (SQLiteException e) {
            db = null;
        }

        this.table = table;
        this.limit = limit;
    }

    public boolean isDbReady() {
        return db != null;
    }

    @Override
    public Cursor loadInBackground() {
        if (isDbReady()) {
            return db.query(distinct, table, getProjection(), getSelection(), getSelectionArgs(), null, null, getSortOrder(), limit);
        }
        return null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (isDbReady()) {
            db.close();
        }
        super.finalize();
    }
}
