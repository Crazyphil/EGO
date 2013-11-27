package tk.crazysoft.ego.data;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;

public class EGOCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {
    private static final String ALPHABETIC_INDEX = "ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ";
    private static final String NUMERIC_INDEX = "0123456789";

    private AlphabetIndexer indexer;

    public EGOCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        this(context, layout, c, from, to, flags, -1);
    }

    public EGOCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags, int indexColumn) {
        this(context, layout, c, from, to, flags, indexColumn, false);
    }

    public EGOCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags, int indexColumn, boolean numericIndex) {
        super(context, layout, c, from, to, flags);
        if (indexColumn >= 0) {
            indexer = new AlphabetIndexer(c, indexColumn, numericIndex ? NUMERIC_INDEX : ALPHABETIC_INDEX);
        } else {
            indexer = null;
        }
    }

    @Override
    public void changeCursor(Cursor cursor) {
        if (indexer != null) {
            indexer.setCursor(cursor);
        }
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        if (indexer != null) {
            indexer.setCursor(c);
        }
        return super.swapCursor(c);
    }

    @Override
    public Object[] getSections() {
        if (indexer != null) {
            return indexer.getSections();
        }
        return new Object[0];
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (indexer != null) {
            return indexer.getPositionForSection(sectionIndex);
        }
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        if (indexer != null) {
            return indexer.getSectionForPosition(position);
        }
        return 0;
    }
}
