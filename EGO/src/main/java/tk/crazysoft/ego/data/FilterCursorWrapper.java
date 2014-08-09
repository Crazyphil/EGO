package tk.crazysoft.ego.data;

import android.database.Cursor;
import android.database.CursorWrapper;

// Source: http://stackoverflow.com/questions/5041499/how-to-hide-an-item-in-a-listview-in-android/18448203#18448203
public abstract class FilterCursorWrapper extends CursorWrapper {
    private int[] index;
    private int count = 0;
    private int pos = 0;

    /**
     * Creates a cursor wrapper.
     *
     * @param cursor The underlying cursor to wrap.
     */
    public FilterCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    /**
     * Initializes this wrapper's data structures.
     * It is normally called on-demand when access methods are called for the first time, but
     * implementers may choose to manually call it beforehand to improve first-access performance.
     */
    protected void initWrapper() {
        if (index != null) return;

        this.count = super.getCount();
        this.index = new int[this.count];
        for (int i = 0; i < this.count; i++) {
            super.moveToPosition(i);
            if (!isHidden())
                this.index[this.pos++] = i;
        }
        this.count = this.pos;
        this.pos = 0;
        super.moveToFirst();
    }

    /**
     * Determines if the row the cursor currently points at is hidden to consumers.
     *
     * Contract: The FilterCursorWrapper ensures that when this method is called, the
     * row to be examined is the current row. The implementer ensures that when this
     * method returns, the current row is the same as before.
     * @return Whether the current row is hidden
     */
    protected abstract boolean isHidden();

    @Override
    public boolean move(int offset) {
        initWrapper();
        return this.moveToPosition(this.pos + offset);
    }

    @Override
    public boolean moveToNext() {
        initWrapper();
        return this.moveToPosition(this.pos + 1);
    }

    @Override
    public boolean moveToPrevious() {
        initWrapper();
        return this.moveToPosition(this.pos - 1);
    }

    @Override
    public boolean moveToFirst() {
        initWrapper();
        return this.moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        initWrapper();
        return this.moveToPosition(this.count - 1);
    }

    @Override
    public boolean moveToPosition(int position) {
        initWrapper();
        if (position >= this.count || position < 0)
            return false;
        boolean result = super.moveToPosition(this.index[position]);
        if (result) {
            this.pos = position;
        }
        return result;
    }

    @Override
    public int getCount() {
        initWrapper();
        return this.count;
    }

    @Override
    public int getPosition() {
        initWrapper();
        return this.pos;
    }
}