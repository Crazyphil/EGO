package tk.crazysoft.ego.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.provider.ContactsContract;

public class EGOContactsCursorLoader extends EGOCursorLoader {
    public static final String COLUMN_NAME_CONTACT_NAME = "contactname";
    public static final String COLUMN_NAME_CONTACT_NAME_ALIAS = "contactnamealias";
    public static final String COLUMN_NAME_ADDRESS = "address";

    private String[] projection;
    private String selection;
    private String sortOrder;

    public EGOContactsCursorLoader(Context context, String table, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        super(context, table, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public Cursor loadInBackground() {
        SQLiteCursor cursor = (SQLiteCursor)super.loadInBackground();
        int nameColumn = cursor.getColumnIndex(COLUMN_NAME_CONTACT_NAME);
        int nameAliasColumn = cursor.getColumnIndex(COLUMN_NAME_CONTACT_NAME_ALIAS);
        int addressColumn = cursor.getColumnIndex(COLUMN_NAME_ADDRESS);
        if (nameColumn == -1 || addressColumn == -1) {
            return cursor;
        }

        while (cursor.moveToNext()) {
            String name = cursor.getString(nameAliasColumn);
            if (name == null) {
                name = cursor.getString(nameColumn);
            }
            String[] contact = getContact(name);
            if (contact != null) {
                cursor.getWindow().putString(contact[0], cursor.getPosition(), nameColumn);
                cursor.getWindow().putString(contact[1], cursor.getPosition(), addressColumn);
            }
        }
        cursor.moveToPosition(-1);
        return cursor;
    }

    private String[] getContact(String dbName) {
        if (projection == null) {
            projection = new String[] {
                    ContactsContract.CommonDataKinds.StructuredPostal._ID,
                    ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
            };
            selection = ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME + " = ?";
            sortOrder = ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME;
        }
        String[] selectionArgs = { dbName };
        Cursor cursor = getContext().getContentResolver().query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, projection, selection, selectionArgs, sortOrder);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME));
            String address = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
            cursor.close();
            return new String[] { contactName, address };
        }
        cursor.close();
        return null;
    }
}
