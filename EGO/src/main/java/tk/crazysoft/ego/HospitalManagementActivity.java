package tk.crazysoft.ego;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import tk.crazysoft.ego.data.EGOContract;
import tk.crazysoft.ego.data.EGOCursorLoader;
import tk.crazysoft.ego.data.EGODbHelper;

public class HospitalManagementActivity extends ActionBarActivity {
    private static final String ACTION_PERMANENT_ADMITTANCES = "tk.crazysoft.ego.preferences.PERMANENT_ADMITTANCES";
    private static final String ACTION_REPLACEMENT = "tk.crazysoft.ego.preferences.REPLACEMENT";

    private HospitalManagementFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            fragment = new HospitalManagementFragment();
            if (getIntent().getAction().equals(ACTION_REPLACEMENT)) {
                setTitle(R.string.hospital_management_activity_replacement_title);
                fragment.setIsReplacementFragment(true);
            }
        } else {
            fragment = (HospitalManagementFragment)getSupportFragmentManager().findFragmentByTag("fragment");
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment, "fragment").commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.hospital_management, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            /*case android.R.id.home:
                this.finish();
                return true;*/
            case R.id.action_new:
                fragment.addEntry();
                return true;
            case R.id.action_delete:
                fragment.deleteEntries();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class HospitalManagementFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, DialogInterface.OnClickListener {
        private static final String COLUMN_NAME_REPLACEMENT_COMBINED = "entry";
        private static final int LOADER_LIST = 0, LOADER_LOCAL_AUTOCOMPLETE = 1, LOADER_CONTACTS_AUTOCOMPLETE = 2;

        private boolean isReplacementFragment;
        private int itemPos = -1;
        private long itemId = -1;
        private AlertDialog dialog;
        private View dialogContent;
        private SimpleCursorAdapter localAutoCompleteAdapter, contactsAutoCompleteAdapter;
        private InsertOrUpdateRowTask insertOrUpdateTask;
        private DeleteRowTask deleteTask;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState != null) {
                isReplacementFragment = savedInstanceState.getBoolean("isReplacementFragment");
            }
        }

        @Override
        public void onStart() {
            super.onStart();

            String[] fromColumns;
            if (isReplacementFragment) {
                fromColumns = new String[] { COLUMN_NAME_REPLACEMENT_COMBINED };
            } else {
                fromColumns = new String[] { EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME };
            }
            int[] toViews = { android.R.id.text1 };
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(getListView().getContext(), android.R.layout.simple_list_item_multiple_choice, null, fromColumns, toViews, 0);

            setListAdapter(adapter);
            getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            getListView().setOnItemLongClickListener(new HospitalItemOnLongClickListener());

            if (isReplacementFragment) {
                setEmptyText(getString(R.string.hospital_management_activity_replacement_empty));
            } else {
                setEmptyText(getString(R.string.hospital_management_activity_permanent_admittances_empty));
            }

            getLoaderManager().initLoader(LOADER_LIST, null, this);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if (savedInstanceState != null && savedInstanceState.containsKey("dialog")) {
                itemId = savedInstanceState.getLong("itemId");
                showItemDialog(savedInstanceState.getInt("itemPos"), savedInstanceState.getBundle("dialog"));
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            outState.putBoolean("isReplacementFragment", isReplacementFragment);
            if (dialog != null && dialog.isShowing()) {
                outState.putInt("itemPos", itemPos);
                outState.putLong("itemId", itemId);
                outState.putBundle("dialog", dialog.onSaveInstanceState());
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            if (dialog == null || !dialog.isShowing()) {
                return;
            }
            dialog.dismiss();
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_LIST:
                    return createListLoader();
                case LOADER_LOCAL_AUTOCOMPLETE:
                    return createLocalAutoCompleteLoader();
                case LOADER_CONTACTS_AUTOCOMPLETE:
                    return createContactsAutoCompleteLoader();
            }
            return null;
        }

        private Loader<Cursor> createListLoader() {
            String[] projection, selectionArgs;
            String sortOrder, selection;

            if (isReplacementFragment) {
                projection = new String[] {
                        EGOContract.NameReplacements._ID,
                        EGOContract.NameReplacements.COLUMN_NAME_NAME + " || " + " ' \u2192 ' " + " || " + EGOContract.NameReplacements.COLUMN_NAME_REPLACEMENT + " AS " + COLUMN_NAME_REPLACEMENT_COMBINED
                };
                sortOrder = COLUMN_NAME_REPLACEMENT_COMBINED;
                selection = null;
                selectionArgs = null;
            } else {
                projection = new String[] {
                        EGOContract.HospitalAdmission._ID,
                        EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME
                };
                sortOrder = EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME;
                selection =  EGOContract.HospitalAdmission.COLUMN_NAME_DATE + " = ?";
                selectionArgs = new String[] { "0" };
            }

            return new EGOCursorLoader(getActivity(), isReplacementFragment ? EGOContract.NameReplacements.TABLE_NAME : EGOContract.HospitalAdmission.TABLE_NAME, projection, selection, selectionArgs, sortOrder);
        }

        private Loader<Cursor> createLocalAutoCompleteLoader() {
            String[] projection = {
                    "NULL AS " + EGOContract.NameReplacements._ID,
                    EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME + " AS " + EGOContract.NameReplacements.COLUMN_NAME_NAME
            };
            String sortOrder = EGOContract.NameReplacements.COLUMN_NAME_NAME;
            String selection = "1 UNION ALL SELECT DISTINCT " +
                    "NULL AS " + EGOContract.DoctorStandby._ID + ", " + EGOContract.DoctorStandby.COLUMN_NAME_DOCTOR_NAME + " AS " + EGOContract.NameReplacements.COLUMN_NAME_NAME +
                    " FROM " + EGOContract.DoctorStandby.TABLE_NAME;
            return new EGOCursorLoader(getActivity(), EGOContract.HospitalAdmission.TABLE_NAME, projection, selection, null, sortOrder, true);
        }

        private Loader<Cursor> createContactsAutoCompleteLoader() {
            String[] projection = {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
            };
            String sortOrder = ContactsContract.Contacts.DISPLAY_NAME;
            return new CursorLoader(getActivity(), ContactsContract.Contacts.CONTENT_URI, projection, null, null, sortOrder);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            CursorAdapter adapter;
            switch (loader.getId()) {
                case LOADER_LOCAL_AUTOCOMPLETE:
                    adapter = localAutoCompleteAdapter;
                    break;
                case LOADER_CONTACTS_AUTOCOMPLETE:
                    adapter = contactsAutoCompleteAdapter;
                    break;
                default:
                    adapter = (CursorAdapter)getListAdapter();
            }
            if (adapter != null) {
                adapter.swapCursor(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            CursorAdapter adapter;
            switch (loader.getId()) {
                case LOADER_LOCAL_AUTOCOMPLETE:
                    adapter = localAutoCompleteAdapter;
                    break;
                case LOADER_CONTACTS_AUTOCOMPLETE:
                    adapter = contactsAutoCompleteAdapter;
                    break;
                default:
                    adapter = (CursorAdapter)getListAdapter();
            }
            if (adapter != null) {
                adapter.swapCursor(null);
            }
        }

        public void setIsReplacementFragment(boolean replacementFragment) {
            isReplacementFragment = replacementFragment;
        }

        public void addEntry() {
            showItemDialog(-1, null);
        }

        public void editEntry(int itemPos) {
            showItemDialog(itemPos, null);
        }

        private void showItemDialog(int itemPos, Bundle savedInstanceState) {
            this.itemPos = itemPos;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if (isReplacementFragment) {
                builder.setTitle(itemPos < 0 ? R.string.hospital_management_activity_replacement_dialog_add : R.string.hospital_management_activity_replacement_dialog_edit);
            } else {
                builder.setTitle(itemPos < 0 ? R.string.hospital_management_activity_permanent_admittances_dialog_add : R.string.hospital_management_activity_permanent_admittances_dialog_edit);
            }
            builder.setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this);

            if (isReplacementFragment) {
                createReplacementDialogContent(builder, savedInstanceState);
            } else {
                createPermanentAdmittanceDialogContent(builder, savedInstanceState);
            }

            builder.setView(dialogContent);
            dialog = builder.create();
            if (savedInstanceState != null) {
                dialog.onRestoreInstanceState(savedInstanceState);
            }
            dialog.show();
        }

        private void createReplacementDialogContent(AlertDialog.Builder builder, Bundle savedInstanceState) {
            Context context;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                context = builder.getContext();
            } else {
                context = getActivity();
            }
            dialogContent = LayoutInflater.from(context).inflate(R.layout.replacement_dialog, null);

            AutoCompleteTextView editText1 = (AutoCompleteTextView)dialogContent.findViewById(android.R.id.text1);
            AutoCompleteTextView editText2 = (AutoCompleteTextView)dialogContent.findViewById(android.R.id.text2);
            if (itemPos >= 0) {
                if (savedInstanceState == null) {
                    Cursor c = (Cursor)getListView().getAdapter().getItem(itemPos);
                    String[] parts = c.getString(c.getColumnIndex(COLUMN_NAME_REPLACEMENT_COMBINED)).split(" \u2192 ");
                    editText1.setText(parts[0]);
                    editText2.setText(parts[1]);

                    itemId = getListView().getAdapter().getItemId(itemPos);
                }
                dialogContent.setTag(itemId);
            }

            String[] fromColumns = { EGOContract.NameReplacements.COLUMN_NAME_NAME  };
            int[] toViews = { android.R.id.text1 };
            localAutoCompleteAdapter = new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1, null, fromColumns, toViews, 0);
            localAutoCompleteAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
                @Override
                public CharSequence convertToString(Cursor cursor) {
                    return cursor.getString(cursor.getColumnIndex(EGOContract.NameReplacements.COLUMN_NAME_NAME));
                }
            });
            editText1.setAdapter(localAutoCompleteAdapter);
            getLoaderManager().initLoader(LOADER_LOCAL_AUTOCOMPLETE, null, this);

            prepareContactsAutoCompleteAdapter();
            editText2.setAdapter(contactsAutoCompleteAdapter);
            getLoaderManager().initLoader(LOADER_CONTACTS_AUTOCOMPLETE, null, this);
        }

        private void prepareContactsAutoCompleteAdapter() {
            String[] fromColumns = { ContactsContract.Contacts.DISPLAY_NAME  };
            int[] toViews = { android.R.id.text1 };
            contactsAutoCompleteAdapter = new SimpleCursorAdapter(getListView().getContext(), android.R.layout.simple_list_item_1, null, fromColumns, toViews, 0);
            contactsAutoCompleteAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
                @Override
                public CharSequence convertToString(Cursor cursor) {
                    return cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                }
            });
        }

        private void createPermanentAdmittanceDialogContent(AlertDialog.Builder builder, Bundle savedInstanceState) {
            AutoCompleteTextView editText;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                editText = new AutoCompleteTextView(builder.getContext());
            } else {
                editText = new AutoCompleteTextView(getActivity());
            }
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
            editText.setId(android.R.id.text1);

            prepareContactsAutoCompleteAdapter();
            editText.setAdapter(contactsAutoCompleteAdapter);

            dialogContent = editText;
            if (itemPos >= 0) {
                if (savedInstanceState == null) {
                    Cursor c = (Cursor)getListView().getAdapter().getItem(itemPos);
                    String hospital = c.getString(c.getColumnIndex(EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME));
                    editText.setText(hospital);
                    itemId = getListView().getAdapter().getItemId(itemPos);
                }
                editText.setTag(itemId);
            }
            getLoaderManager().initLoader(LOADER_CONTACTS_AUTOCOMPLETE, null, this);
        }

        public void deleteEntries() {
            long[] checkedItems = getListView().getCheckedItemIds();
            Long[] checkedItemIds = new Long[checkedItems.length];
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItemIds[i] = checkedItems[i];
            }

            deleteTask = new DeleteRowTask();
            deleteTask.execute(checkedItemIds);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                insertOrUpdateTask = new InsertOrUpdateRowTask();
                if (isReplacementFragment) {
                    EditText editText1 = (EditText)dialogContent.findViewById(android.R.id.text1);
                    EditText editText2 = (EditText)dialogContent.findViewById(android.R.id.text2);
                    insertOrUpdateTask.execute(new Pair<Long, String>((Long)dialogContent.getTag(), editText1.getText().toString()), new Pair<Long, String>(null, editText2.getText().toString()));
                } else {
                    EditText editText = (EditText)dialogContent;
                    insertOrUpdateTask.execute(new Pair<Long, String>((Long)editText.getTag(), editText.getText().toString()));
                }
            }
            dialogContent = null;
        }

        private class HospitalItemOnLongClickListener implements AdapterView.OnItemLongClickListener {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                editEntry(position);
                return true;
            }
        }

        private class InsertOrUpdateRowTask extends AsyncTask<Pair<Long, String>, Void, Boolean> {
            @Override
            protected Boolean doInBackground(Pair<Long, String>... items) {
                if (isReplacementFragment && items.length != 2 || !isReplacementFragment && items.length != 1) {
                    return false;
                }

                SQLiteDatabase db = null;
                try {
                    db = new EGODbHelper(getActivity()).getWritableDatabase();
                    if (db == null) {
                        return false;
                    }

                    if (items[0].first != null) {
                        ContentValues values;
                        if (isReplacementFragment) {
                            values = new ContentValues(2);
                            values.put(EGOContract.NameReplacements.COLUMN_NAME_NAME, items[0].second);
                            values.put(EGOContract.NameReplacements.COLUMN_NAME_REPLACEMENT, items[1].second);
                        } else {
                            values = new ContentValues(1);
                            values.put(EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME, items[0].second);
                        }

                        String where = (isReplacementFragment ? EGOContract.NameReplacements._ID : EGOContract.HospitalAdmission._ID) + " = ?";
                        String[] whereArgs = { items[0].first.toString() };
                        return db.update(isReplacementFragment ? EGOContract.NameReplacements.TABLE_NAME : EGOContract.HospitalAdmission.TABLE_NAME, values, where, whereArgs) > 0;
                    } else {
                        ContentValues values;
                        if (isReplacementFragment) {
                            values = new ContentValues(2);
                            values.put(EGOContract.NameReplacements.COLUMN_NAME_NAME, items[0].second);
                            values.put(EGOContract.NameReplacements.COLUMN_NAME_REPLACEMENT, items[1].second);
                        } else {
                            values = new ContentValues(3);
                            values.put(EGOContract.HospitalAdmission.COLUMN_NAME_DATE, 0);
                            values.put(EGOContract.HospitalAdmission.COLUMN_NAME_TAKEOVER_TIME, 0);
                            values.put(EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME, items[0].second);
                        }
                        return db.insert(isReplacementFragment ? EGOContract.NameReplacements.TABLE_NAME : EGOContract.HospitalAdmission.TABLE_NAME, null, values) > -1;
                    }
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    getLoaderManager().restartLoader(0, null, HospitalManagementFragment.this);
                }
            }
        }

        private class DeleteRowTask extends AsyncTask<Long, Void, Boolean> {
            @Override
            protected Boolean doInBackground(Long... ids) {
                if (ids.length == 0) {
                    return false;
                }

                SQLiteDatabase db = null;
                try {
                    db = new EGODbHelper(getActivity()).getWritableDatabase();
                    if (db == null) {
                        return false;
                    }

                    String[] whereArgs = new String[ids.length];
                    StringBuilder where = new StringBuilder(ids.length + 3);
                    where.append(isReplacementFragment ? EGOContract.NameReplacements._ID : EGOContract.HospitalAdmission._ID);
                    where.append(" IN (?");
                    whereArgs[0] = ids[0].toString();
                    for (int i = 1; i < ids.length; i++) {
                        where.append(", ?");
                        whereArgs[i] = ids[i].toString();
                    }
                    where.append(")");

                    return db.delete(isReplacementFragment ? EGOContract.NameReplacements.TABLE_NAME : EGOContract.HospitalAdmission.TABLE_NAME, where.toString(), whereArgs) > 0;
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    getLoaderManager().restartLoader(0, null, HospitalManagementFragment.this);
                }
            }
        }
    }
}
