package tk.crazysoft.ego.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class EGOCursorLoader extends SimpleCursorLoader {
    SQLiteDatabase db;
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
        db = helper.getReadableDatabase();
        this.table = table;
        this.limit = limit;
    }

    @Override
    public Cursor loadInBackground() {
        return db.query(distinct, table, getProjection(), getSelection(), getSelectionArgs(), null, null, getSortOrder(), limit);
    }

    @Override
    protected void finalize() throws Throwable {
        db.close();
        super.finalize();
    }
}
