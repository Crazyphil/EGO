package tk.crazysoft.ego.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.regex.Pattern;

public abstract class Importer {
    public static final int PROCESS_SUCCESS = 0, PROCESS_IGNORED = -1, PROCESS_ERROR = 1;

    private Context context;
    private SQLiteDatabase db;
    private Pattern csvTrimPattern;
    private OnPostProcessProgressListener listener;

    public Importer(Context context) {
        this(context, null);
    }

    public Importer(Context context, SQLiteDatabase db) {
        this.context = context;
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

    protected boolean isEmpty(String[] line) {
        boolean empty = true;
        for (String field : line) {
            empty &= TextUtils.isEmpty(field);
        }
        return empty;
    }

    public void setOnPostProcessProgressListener(OnPostProcessProgressListener listener) {
        this.listener = listener;
    }

    protected OnPostProcessProgressListener getOnPostProcessProgressListener() {
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

    protected Context getContext() {
        return context;
    }

    public interface OnPostProcessProgressListener {
        public void onProgress(double progressPercent);
        public void onResult(String action, int processed, int modified);
        public void onResult(String action, boolean result);
    }
}
