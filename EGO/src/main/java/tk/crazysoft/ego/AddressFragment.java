package tk.crazysoft.ego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import tk.crazysoft.ego.components.ClearableEditText;
import tk.crazysoft.ego.data.*;

import java.util.ArrayList;
import java.util.HashMap;

public class AddressFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private Button buttonCity, buttonZip, buttonStreet, buttonStreetNo;
    private AlertDialog alertDialogCity, alertDialogZip, alertDialogStreet, alertDialogStreetNo;
    private String selectedCity, selectedZip, selectedStreet, selectedStreetNo;
    private boolean cityHasData, zipHasData, streetHasData, streetNoHasData;
    private ImageButton imageButtonClearCity, imageButtonClearZip, imageButtonClearStreet, imageButtonClearStreetNo;
    private ListView listViewResults;
    private HashMap<AlertDialog, CursorAdapter> adapterMapping = new HashMap<AlertDialog, CursorAdapter>(4);
    private Typeface normalTypeface, italicTypeface;
    private int normalColor, italicColor;

    private OnAddressClickListener onAddressClickListener;

    private static final int LOADER_RESULTS = 0;
    private static final int LOADER_CITY = 1;
    private static final int LOADER_ZIP_CODE = 2;
    private static final int LOADER_STREET = 3;
    private static final int LOADER_STREET_NO = 4;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            onAddressClickListener = (OnAddressClickListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAddressClickListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            selectedCity = savedInstanceState.getString("city");
            cityHasData = selectedCity != null;
            selectedZip = savedInstanceState.getString("zip");
            zipHasData = selectedZip != null;
            selectedStreet = savedInstanceState.getString("street");
            streetHasData = selectedStreet != null;
            selectedStreetNo = savedInstanceState.getString("streetNo");
            streetNoHasData = selectedStreetNo != null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.address_view, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        buttonCity = (Button)getView().findViewById(R.id.address_buttonCity);
        buttonZip = (Button)getView().findViewById(R.id.address_buttonZip);
        buttonStreet = (Button)getView().findViewById(R.id.address_buttonStreet);
        buttonStreetNo = (Button)getView().findViewById(R.id.address_buttonStreetNo);

        italicTypeface = Typeface.create(buttonCity.getTypeface(), Typeface.ITALIC);
        normalTypeface = Typeface.create(italicTypeface, Typeface.NORMAL);

        TypedArray a = getActivity().getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorPrimary, android.R.attr.textColorSecondary });
        normalColor = a.getColor(0, Color.MAGENTA);
        italicColor = a.getColor(1, Color.MAGENTA);
        a.recycle();

        buttonCity.setOnClickListener(new FilterButtonOnClickListener());
        buttonZip.setOnClickListener(new FilterButtonOnClickListener());
        buttonStreet.setOnClickListener(new FilterButtonOnClickListener());
        buttonStreetNo.setOnClickListener(new FilterButtonOnClickListener());

        alertDialogCity = buildFilterAlertDialog(buttonCity, LOADER_CITY, R.string.address_view_city, EGOContract.Addresses.COLUMN_NAME_CITY, true);
        alertDialogZip = buildFilterAlertDialog(buttonZip, LOADER_ZIP_CODE, R.string.address_view_zipcode, EGOContract.Addresses.COLUMN_NAME_ZIP, true);
        alertDialogStreet = buildFilterAlertDialog(buttonStreet, LOADER_STREET, R.string.address_view_street, EGOContract.Addresses.COLUMN_NAME_STREET, true);
        alertDialogStreetNo = buildFilterAlertDialog(buttonStreetNo, LOADER_STREET_NO, R.string.address_view_streetno, EGOContract.Addresses.COLUMN_NAME_STREET_NO, false);

        imageButtonClearCity = (ImageButton)getView().findViewById(R.id.address_imageButtonClearCity);
        imageButtonClearZip = (ImageButton)getView().findViewById(R.id.address_imageButtonClearZip);
        imageButtonClearStreet = (ImageButton)getView().findViewById(R.id.address_imageButtonClearStreet);
        imageButtonClearStreetNo = (ImageButton)getView().findViewById(R.id.address_imageButtonClearStreetNo);

        imageButtonClearCity.setOnClickListener(new ClearButtonOnClickListener());
        imageButtonClearZip.setOnClickListener(new ClearButtonOnClickListener());
        imageButtonClearStreet.setOnClickListener(new ClearButtonOnClickListener());
        imageButtonClearStreetNo.setOnClickListener(new ClearButtonOnClickListener());

        String[] fromColumns = { "address", "region" };
        int[] toViews = { android.R.id.text1, android.R.id.text2 };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getView().getContext(), android.R.layout.simple_list_item_2, null, fromColumns, toViews, 0);

        listViewResults = (ListView)getView().findViewById(R.id.address_listViewResults);
        listViewResults.setAdapter(adapter);
        listViewResults.setEmptyView(getView().findViewById(R.id.address_textViewEmpty));
        listViewResults.setOnItemClickListener(new ResultsListOnItemClickListener());

        restoreButtons();
        getLoaderManager().initLoader(LOADER_RESULTS, null, this);
        getLoaderManager().initLoader(LOADER_CITY, null, this);
        getLoaderManager().initLoader(LOADER_ZIP_CODE, null, this);
        getLoaderManager().initLoader(LOADER_STREET, null, this);
        getLoaderManager().initLoader(LOADER_STREET_NO, null, this);
    }

    private void restoreButtons() {
        if (cityHasData) {
            buttonCity.setText(selectedCity);
            buttonCity.setTypeface(normalTypeface, Typeface.NORMAL);
            buttonCity.setTextColor(normalColor);
        } else {
            buttonCity.setTextColor(italicColor);
        }
        if (zipHasData) {
            buttonZip.setText(selectedZip);
            buttonZip.setTypeface(normalTypeface, Typeface.NORMAL);
            buttonZip.setTextColor(normalColor);
        } else {
            buttonZip.setTextColor(italicColor);
        }
        if (streetHasData) {
            buttonStreet.setText(selectedStreet);
            buttonStreet.setTypeface(normalTypeface, Typeface.NORMAL);
            buttonStreet.setTextColor(normalColor);
        } else {
            buttonStreet.setTextColor(italicColor);
        }
        if (streetNoHasData) {
            buttonStreetNo.setText(selectedStreetNo);
            buttonStreetNo.setTypeface(normalTypeface, Typeface.NORMAL);
            buttonStreetNo.setTextColor(normalColor);
        } else {
            buttonStreetNo.setTextColor(italicColor);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("city", selectedCity);
        outState.putString("zip", selectedZip);
        outState.putString("street", selectedStreet);
        outState.putString("streetNo", selectedStreetNo);
    }

    private AlertDialog buildFilterAlertDialog(Button button, int loaderId, int titleRes, String column, boolean index) {
        String[] fromColumns = { column };
        int[] toViews = { android.R.id.text1 };
        EGOCursorAdapter adapter = new EGOCursorAdapter(getView().getContext(), android.R.layout.simple_list_item_1, null, fromColumns, toViews, 0, index ? 1 : -1);

        AlertDialog.Builder builder = new AlertDialog.Builder(getView().getContext());
        builder.setTitle(titleRes).setAdapter(adapter, new FilterAlertDialogOnClickListener(button));
        final AlertDialog dialog = builder.create();
        adapterMapping.put(dialog, adapter);
        dialog.getListView().setFastScrollEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            dialog.getListView().setFastScrollAlwaysVisible(true);
        }

        final ClearableEditText filterText = new ClearableEditText(dialog.getContext());
        filterText.getText().setHint(R.string.address_view_filter);
        filterText.setPadding(0, 0, (int)(20 * getResources().getDisplayMetrics().density), 0);
        filterText.getText().addTextChangedListener(new SearchBarTextWatcher(loaderId));
        dialog.getListView().addHeaderView(filterText);
        dialog.getListView().setDescendantFocusability(ListView.FOCUS_AFTER_DESCENDANTS);   // Workaround for EditText no longer having focus after list content changes
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                filterText.clearText();
            }
        });
        return dialog;
    }

    private String getFilter(Button button, boolean hasData) {
        if (hasData) {
            return (String)button.getText();
        }
        return null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Loader<Cursor> loader = null;
        String filter = bundle != null ? bundle.getString("filter") : null;
        switch (i) {
            case LOADER_RESULTS:
                loader = createResultsLoader();
                break;
            case LOADER_CITY:
                loader = createCityLoader(filter);
                break;
            case LOADER_ZIP_CODE:
                loader = createZipCodeLoader(filter);
                break;
            case LOADER_STREET:
                loader = createStreetLoader(filter);
                break;
            case LOADER_STREET_NO:
                loader = createStreetNoLoader(filter);
                break;
        }
        return loader;
    }

    private Loader<Cursor> createResultsLoader() {
        String[] projection = {
                EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_STREET_NO + " AS address",
                EGOContract.Addresses.COLUMN_NAME_ZIP + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_CITY + " AS region",
                EGOContract.Addresses.COLUMN_NAME_PARENT_ID
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_CITY + " , " +
                EGOContract.Addresses.COLUMN_NAME_STREET + ", " +
                EGOContract.Addresses.COLUMN_NAME_STREET_NO + "*1, " +
                EGOContract.Addresses.COLUMN_NAME_STREET_NO;

        return createLoader(false, projection, sortOrder, "100", null, null);
    }

    private Loader<Cursor> createCityLoader(String filter) {
        String[] projection = {
                "NULL AS " + EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_CITY
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_CITY;
        return createLoader(true, projection, sortOrder, null, filter, buttonCity);
    }

    private Loader<Cursor> createZipCodeLoader(String filter) {
        String[] projection = {
                "NULL AS " + EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_ZIP
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_ZIP;
        return createLoader(true, projection, sortOrder, null, filter, buttonZip);
    }

    private Loader<Cursor> createStreetLoader(String filter) {
        String[] projection = {
                "NULL AS " + EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_STREET;
        return createLoader(true, projection, sortOrder, null, filter, buttonStreet);
    }

    private Loader<Cursor> createStreetNoLoader(String filter) {
        String[] projection = {
                "NULL AS " + EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET_NO
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_STREET_NO + "*1, " +
                EGOContract.Addresses.COLUMN_NAME_STREET_NO;
        return createLoader(true, projection, sortOrder, null, filter, buttonStreetNo);
    }

    private EGOCursorLoader createLoader(boolean distinct, String[] projection, String sortOrder, String limit, String filter, Button button) {
        ArrayList<String> selectionArgs = new ArrayList<String>(4);
        String selection = fillSelection(selectionArgs, button);
        if (filter != null) {
            if (selection.length() > 0) {
                selection += " AND ";
            }
            selection += projection[1] + " LIKE ?";
            selectionArgs.add(String.format("%s%%", filter));
        }
        String[] selectionArgArr = selectionArgs.toArray(new String[selectionArgs.size()]);

        EGOCursorLoader loader;
        if (limit == null) {
            loader = new EGOCursorLoader(getActivity(), EGOContract.Addresses.TABLE_NAME, projection, selection, selectionArgArr, sortOrder, distinct);
        } else {
            loader = new EGOCursorLoader(getActivity(), EGOContract.Addresses.TABLE_NAME, projection, selection, selectionArgArr, sortOrder, limit);
        }
        return loader;
    }

    private String fillSelection(ArrayList<String> selectionArgs, Button button) {
        ArrayList<String> selectColumns = new ArrayList<String>(4);
        if (button != buttonCity && cityHasData) {
            selectColumns.add(EGOContract.Addresses.COLUMN_NAME_CITY);
            selectionArgs.add(selectedCity);
        }
        if (button != buttonZip && zipHasData) {
            selectColumns.add(EGOContract.Addresses.COLUMN_NAME_ZIP);
            selectionArgs.add(selectedZip);
        }
        if (button != buttonStreet && streetHasData) {
            selectColumns.add(EGOContract.Addresses.COLUMN_NAME_STREET);
            selectionArgs.add(selectedStreet);
        }
        if (button != buttonStreetNo && streetNoHasData) {
            selectColumns.add(EGOContract.Addresses.COLUMN_NAME_STREET_NO);
            selectionArgs.add(selectedStreetNo);
        }
        return EGODbHelper.createSimpleSelection(selectColumns.toArray(new String[selectColumns.size()]), EGODbHelper.BooleanComposition.AND);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        CursorAdapter adapter = getAdapterForCursorLoader(cursorLoader);
        adapter.swapCursor(new ResultFilterCursorWrapper(cursor));
    }

    private CursorAdapter getAdapterForCursorLoader(Loader<Cursor> cursorLoader) {
        CursorAdapter adapter;
        if (cursorLoader.getId() == LOADER_RESULTS) {
            return (CursorAdapter)listViewResults.getAdapter();
        } else if (cursorLoader.getId() == LOADER_CITY) {
            adapter = adapterMapping.get(alertDialogCity);
        } else if (cursorLoader.getId() == LOADER_ZIP_CODE) {
            adapter = adapterMapping.get(alertDialogZip);
        } else if (cursorLoader.getId() == LOADER_STREET) {
            adapter = adapterMapping.get(alertDialogStreet);
        } else {
            adapter = adapterMapping.get(alertDialogStreetNo);
        }
        return adapter;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        CursorAdapter adapter = getAdapterForCursorLoader(cursorLoader);
        adapter.swapCursor(null);
    }

    private void resetLoaders(Button button, boolean hasData) {
        if (button == buttonCity) {
            cityHasData = hasData;
        } else if (button == buttonZip) {
            zipHasData = hasData;
        } else if (button == buttonStreet) {
            streetHasData = hasData;
        } else if (button == buttonStreetNo) {
            streetNoHasData = hasData;
        }

        selectedCity = getFilter(buttonCity, cityHasData);
        selectedZip = getFilter(buttonZip, zipHasData);
        selectedStreet = getFilter(buttonStreet, streetHasData);
        selectedStreetNo = getFilter(buttonStreetNo, streetNoHasData);

        if (!isAdded()) return;

        getLoaderManager().restartLoader(LOADER_RESULTS, null, AddressFragment.this);

        if (button != buttonCity) {
            getLoaderManager().restartLoader(LOADER_CITY, null, AddressFragment.this);
        }
        if (button != buttonZip) {
            getLoaderManager().restartLoader(LOADER_ZIP_CODE, null, AddressFragment.this);
        }
        if (button != buttonStreet) {
            getLoaderManager().restartLoader(LOADER_STREET, null, AddressFragment.this);
        }
        if (button != buttonStreetNo) {
            getLoaderManager().restartLoader(LOADER_STREET_NO, null, AddressFragment.this);
        }
    }

    public interface OnAddressClickListener {
        public void onAddressClick(long id);
    }

    private class FilterButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == buttonCity) {
                show(alertDialogCity);
                findAndScroll(alertDialogCity, selectedCity);
            } else if (v == buttonZip) {
                show(alertDialogZip);
                findAndScroll(alertDialogZip, selectedZip);
            } else if (v == buttonStreet) {
                show(alertDialogStreet);
                findAndScroll(alertDialogStreet, selectedStreet);
            } else {
                show(alertDialogStreetNo);
                findAndScroll(alertDialogStreetNo, selectedStreetNo);
            }
        }

        private void show(AlertDialog dialog) {
            dialog.show();

            // Workaround for keyboard not being show for EditText in dialog even when it is clicked
            // Explanation: http://stackoverflow.com/questions/9102074/android-edittext-in-dialog-doesnt-pull-up-soft-keyboard/9118027#9118027
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }

        private void findAndScroll(AlertDialog dialog, String selectedItem) {
            if (selectedItem == null) {
                return;
            }

            ListView listView = dialog.getListView();
            CursorAdapter adapter = adapterMapping.get(dialog);
            Cursor c = adapter.getCursor();
            while (c.moveToNext()) {
                if (c.getString(1).equals(selectedItem)) {
                    listView.setSelection(c.getPosition());
                    return;
                }
            }
        }
    }

    private class ClearButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == imageButtonClearCity) {
                buttonCity.setText(R.string.address_view_city);
                buttonCity.setTypeface(italicTypeface, Typeface.ITALIC);
                buttonCity.setTextColor(italicColor);
                alertDialogCity.getListView().scrollTo(0, 0);
                resetLoaders(buttonCity, false);
            } else if (v == imageButtonClearZip) {
                buttonZip.setText(R.string.address_view_zipcode);
                buttonZip.setTypeface(italicTypeface, Typeface.ITALIC);
                buttonZip.setTextColor(italicColor);
                alertDialogZip.getListView().scrollTo(0, 0);
                resetLoaders(buttonZip, false);
            } else if (v == imageButtonClearStreet) {
                buttonStreet.setText(R.string.address_view_street);
                buttonStreet.setTypeface(italicTypeface, Typeface.ITALIC);
                buttonStreet.setTextColor(italicColor);
                alertDialogStreet.getListView().scrollTo(0, 0);
                resetLoaders(buttonStreet, false);
            } else {
                buttonStreetNo.setText(R.string.address_view_streetno);
                buttonStreetNo.setTypeface(italicTypeface, Typeface.ITALIC);
                buttonStreetNo.setTextColor(italicColor);
                alertDialogStreetNo.getListView().scrollTo(0, 0);
                resetLoaders(buttonStreetNo, false);
            }
        }
    }

    private class FilterAlertDialogOnClickListener implements AlertDialog.OnClickListener {
        private Button button;

        private FilterAlertDialogOnClickListener(Button button) {
            this.button = button;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            AlertDialog alertDialog = (AlertDialog)dialog;
            Cursor c = (Cursor)alertDialog.getListView().getAdapter().getItem(which);
            button.setText(c.getString(1));
            button.setTypeface(normalTypeface, Typeface.NORMAL);
            button.setTextColor(normalColor);
            resetLoaders(button, true);
        }
    }

    private class SearchBarTextWatcher implements TextWatcher {
        int loaderId;

        public SearchBarTextWatcher(int loaderId) {
            this.loaderId = loaderId;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            if (isAdded()) {
                Bundle args = new Bundle(1);
                args.putString("filter", s.toString());
                getLoaderManager().restartLoader(loaderId, args, AddressFragment.this);
            }
        }
    }

    private class ResultFilterCursorWrapper extends FilterCursorWrapper {
        private Cursor cursor;
        private LongSparseArray<Boolean> selectedRows = null;
        private int idColumn, parentIdColumn;

        public ResultFilterCursorWrapper(Cursor cursor) {
            super(cursor);

            this.cursor = cursor;
            idColumn = getColumnIndex(EGOContract.Addresses._ID);
            parentIdColumn = getColumnIndex(EGOContract.Addresses.COLUMN_NAME_PARENT_ID);
            initWrapper();
        }

        @Override
        protected boolean isHidden() {
            if (selectedRows == null) {
                initSelectedRows();
            }
            if (parentIdColumn > -1) {
                long id = getLong(parentIdColumn);
                if (id > 0) {
                    Boolean isThere = selectedRows.get(id);
                    return isThere != null;
                }
            }
            return false;
        }

        private void initSelectedRows() {
            int currentPos = cursor.getPosition();

            selectedRows = new LongSparseArray<Boolean>();
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                selectedRows.put(cursor.getLong(idColumn), true);
            }
            cursor.moveToPosition(currentPos);
        }
    }

    private class ResultsListOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            onAddressClickListener.onAddressClick(id);
        }
    }
}
