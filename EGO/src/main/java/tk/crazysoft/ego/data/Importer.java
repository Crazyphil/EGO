package tk.crazysoft.ego.data;

import android.database.sqlite.SQLiteDatabase;

import java.util.regex.Pattern;

public abstract class Importer {
    private SQLiteDatabase db;
    private Pattern csvTrimPattern;
    private OnProgressListener listener;

    public Importer() { }

    public Importer(SQLiteDatabase db) {
        this.db = db;
    }

    public void preProcess() { }

    public abstract int process(String[] line);

    public void postProcess() { }

    protected int findStringPosInArray(String[] array, String string) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].trim().compareToIgnoreCase(string) == 0) {
                return i;
            }
        }
        return -1;
    }

    protected String csvTrim(String field) {
        if (csvTrimPattern == null) {
            csvTrimPattern = Pattern.compile("^\"\\s*|\\s*\"$|^\\s+|\\s+$");
        }
        return csvTrimPattern.matcher(field).replaceAll("");
    }

    public void setOnProgressListener(OnProgressListener listener) {
        this.listener = listener;
    }

    protected OnProgressListener getOnProgressListener() {
        return listener;
    }

    public void setDatabase(SQLiteDatabase db) {
        this.db = db;
    }

    protected SQLiteDatabase getDatabase() {
        if (db == null) {
            throw new IllegalStateException("The database must be set before calling any processing method");
        }
        return db;
    }

    public interface OnProgressListener {
        public void onProgress(double progressPercent);
        public void onResult(String action, int processed, int modified);
    }
}
