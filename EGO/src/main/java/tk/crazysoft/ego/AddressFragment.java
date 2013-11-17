package tk.crazysoft.ego;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.util.ArrayList;

import tk.crazysoft.ego.components.NothingSelectedSpinnerAdapter;
import tk.crazysoft.ego.data.EGOContract;
import tk.crazysoft.ego.data.EGOCursorLoader;
import tk.crazysoft.ego.data.EGODbHelper;

public class AddressFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private Spinner spinnerCity, spinnerZip, spinnerStreet, spinnerStreetNo;
    private String spinnerCitySelected, spinnerZipSelected, spinnerStreetSelected, spinnerStreetNoSelected;
    private ImageButton imageButtonClearCity, imageButtonClearZip, imageButtonClearStreet, imageButtonClearStreetNo;
    private ListView listViewResults;
    private EGOCursorLoader loaderResult, loaderCity, loaderZip, loaderStreet, loaderStreetNo;

    private static final int LOADER_RESULTS = 0;
    private static final int LOADER_CITY = 1;
    private static final int LOADER_ZIP_CODE = 2;
    private static final int LOADER_STREET = 3;
    private static final int LOADER_STREET_NO = 4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.address_view, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        spinnerCity = (Spinner)getView().findViewById(R.id.address_spinnerCity);
        spinnerZip = (Spinner)getView().findViewById(R.id.address_spinnerZip);
        spinnerStreet = (Spinner)getView().findViewById(R.id.address_spinnerStreet);
        spinnerStreetNo = (Spinner)getView().findViewById(R.id.address_spinnerStreetNo);

        imageButtonClearCity = (ImageButton)getView().findViewById(R.id.address_imageButtonClearCity);
        imageButtonClearZip = (ImageButton)getView().findViewById(R.id.address_imageButtonClearZip);
        imageButtonClearStreet = (ImageButton)getView().findViewById(R.id.address_imageButtonClearStreet);
        imageButtonClearStreetNo = (ImageButton)getView().findViewById(R.id.address_imageButtonClearStreetNo);

        setDataOnSpinner(spinnerCity, EGOContract.Addresses.COLUMN_NAME_CITY, true);
        setDataOnSpinner(spinnerZip, EGOContract.Addresses.COLUMN_NAME_ZIP, true);
        setDataOnSpinner(spinnerStreet, EGOContract.Addresses.COLUMN_NAME_STREET, true);
        setDataOnSpinner(spinnerStreetNo, EGOContract.Addresses.COLUMN_NAME_STREET_NO, true);

        imageButtonClearCity.setOnClickListener(new ClearButtonOnClickListener());
        imageButtonClearZip.setOnClickListener(new ClearButtonOnClickListener());
        imageButtonClearStreet.setOnClickListener(new ClearButtonOnClickListener());
        imageButtonClearStreetNo.setOnClickListener(new ClearButtonOnClickListener());

        String[] fromColumns = { "address", "region" };
        int[] toViews = { android.R.id.text1, android.R.id.text2 };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getView().getContext(), android.R.layout.simple_list_item_2, null, fromColumns, toViews, 0);

        ProgressBar progressBar = new ProgressBar(getView().getContext());
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        progressBar.setIndeterminate(true);

        listViewResults = (ListView)getView().findViewById(R.id.address_listViewResults);
        listViewResults.setAdapter(adapter);
        listViewResults.setEmptyView(progressBar);

        getLoaderManager().initLoader(LOADER_RESULTS, null, this);
        getLoaderManager().initLoader(LOADER_CITY, null, this);
        getLoaderManager().initLoader(LOADER_ZIP_CODE, null, this);
        getLoaderManager().initLoader(LOADER_STREET, null, this);
        getLoaderManager().initLoader(LOADER_STREET_NO, null, this);
    }

    private void setDataOnSpinner(Spinner spinner, String element, boolean isEnabled) {
        String[] fromColumns = { element };
        int[] toViews = { android.R.id.text1 };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getView().getContext(), android.R.layout.simple_spinner_item, null, fromColumns, toViews, 0);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(new NothingSelectedSpinnerAdapter(adapter, android.R.layout.simple_spinner_item, getView().getContext()));

        if (!isEnabled) {
            spinner.setEnabled(false);
        }

        spinner.setOnItemSelectedListener(new SpinnerOnItemSelectedListener());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Loader<Cursor> loader = null;
        switch (i) {
            case LOADER_RESULTS:
                loader = createResultsLoader();
                break;
            case LOADER_CITY:
                loader = createCityLoader();
                break;
            case LOADER_ZIP_CODE:
                loader = createZipCodeLoader();
                break;
            case LOADER_STREET:
                loader = createStreetLoader();
                break;
            case LOADER_STREET_NO:
                loader = createStreetNoLoader();
                break;
        }
        return loader;
    }

    private Loader<Cursor> createResultsLoader() {
        String[] projection = {
                EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_STREET_NO + " AS address",
                EGOContract.Addresses.COLUMN_NAME_ZIP + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_CITY + " AS region",
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_CITY + " , " +
                EGOContract.Addresses.COLUMN_NAME_STREET + ", " +
                EGOContract.Addresses.COLUMN_NAME_STREET_NO + "*1, " +
                EGOContract.Addresses.COLUMN_NAME_STREET_NO;

        loaderResult = createLoader(false, projection, sortOrder, "100", null);
        return loaderResult;
    }

    private Loader<Cursor> createCityLoader() {
        String[] projection = {
                "NULL AS " + EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_CITY
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_CITY;
        loaderCity = createLoader(true, projection, sortOrder, null, spinnerCity);
        return loaderCity;
    }

    private Loader<Cursor> createZipCodeLoader() {
        String[] projection = {
                "NULL AS " + EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_ZIP
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_ZIP;
        loaderZip = createLoader(true, projection, sortOrder, null, spinnerZip);
        return loaderZip;
    }

    private Loader<Cursor> createStreetLoader() {
        String[] projection = {
                "NULL AS " + EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_STREET;
        loaderStreet = createLoader(true, projection, sortOrder, null, spinnerStreet);
        return loaderStreet;
    }

    private Loader<Cursor> createStreetNoLoader() {
        String[] projection = {
                "NULL AS " + EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET_NO
        };
        String sortOrder = EGOContract.Addresses.COLUMN_NAME_STREET_NO + "*1, " +
                EGOContract.Addresses.COLUMN_NAME_STREET_NO;
        loaderStreetNo = createLoader(true, projection, sortOrder, null, spinnerStreetNo);
        return loaderStreetNo;
    }

    private EGOCursorLoader createLoader(boolean distinct, String[] projection, String sortOrder, String limit, Spinner spinner) {
        ArrayList<String> selectionArgs = new ArrayList<String>(4);
        String selection = fillSelection(selectionArgs, spinner);
        String[] selectionArgArr = selectionArgs.toArray(new String[selectionArgs.size()]);

        EGOCursorLoader loader;
        if (limit == null) {
            loader = new EGOCursorLoader(getActivity(), EGOContract.Addresses.TABLE_NAME, projection, selection, selectionArgArr, sortOrder, distinct);
        } else {
            loader = new EGOCursorLoader(getActivity(), EGOContract.Addresses.TABLE_NAME, projection, selection, selectionArgArr, sortOrder, limit);
        }
        return loader;
    }

    private String fillSelection(ArrayList<String> selectionArgs, Spinner spinner) {
        ArrayList<String> selectColumns = new ArrayList<String>(4);
        Cursor c;
        if (spinner != spinnerCity && spinnerCity.getSelectedItemPosition() > 0) {
            c = (Cursor)spinnerCity.getSelectedItem();
            if (c != null) {
                selectColumns.add(EGOContract.Addresses.COLUMN_NAME_CITY);
                selectionArgs.add(c.getString(1));
            }
        }
        if (spinner != spinnerZip && spinnerZip.getSelectedItemPosition() > 0) {
            c = (Cursor)spinnerZip.getSelectedItem();
            if (c != null) {
                selectColumns.add(EGOContract.Addresses.COLUMN_NAME_ZIP);
                selectionArgs.add(c.getString(1));
            }
        }
        if (spinner != spinnerStreet && spinnerStreet.getSelectedItemPosition() > 0) {
            c = (Cursor)spinnerStreet.getSelectedItem();
            if (c != null) {
                selectColumns.add(EGOContract.Addresses.COLUMN_NAME_STREET);
                selectionArgs.add(c.getString(1));
            }
        }
        if (spinner != spinnerStreetNo && spinnerStreetNo.getSelectedItemPosition() > 0) {
            c = (Cursor)spinnerStreetNo.getSelectedItem();
            if (c != null) {
                selectColumns.add(EGOContract.Addresses.COLUMN_NAME_STREET_NO);
                selectionArgs.add(c.getString(1));
            }
        }
        return EGODbHelper.createSimpleSelection(selectColumns.toArray(new String[selectColumns.size()]), EGODbHelper.BooleanComposition.AND);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        AdapterView<?> adapterView = getAdapterViewForCursorLoader(cursorLoader);
        CursorAdapter adapter;
        if (adapterView instanceof Spinner) {
            adapter = (CursorAdapter)((NothingSelectedSpinnerAdapter)adapterView.getAdapter()).getAdapter();
            adapter.swapCursor(cursor);
            String selectedItem = getStoredSelectedItem(adapterView);
            if (selectedItem != null) {
                // Set selection listener to null when changing selected item to avoid unnecessary second reload of all cursors
                AdapterView.OnItemSelectedListener listener = adapterView.getOnItemSelectedListener();
                adapterView.setOnItemSelectedListener(null);
                findAndSelect(adapterView, selectedItem);
                adapterView.setOnItemSelectedListener(listener);
            }
        } else {
            adapter = (CursorAdapter)adapterView.getAdapter();
            adapter.swapCursor(cursor);
        }
    }

    private String getStoredSelectedItem(AdapterView<?> spinner) {
        if (spinner.equals(spinnerCity)) {
            return spinnerCitySelected;
        } else if (spinner.equals(spinnerZip)) {
            return spinnerZipSelected;
        } else if (spinner.equals(spinnerStreet)) {
            return spinnerStreetSelected;
        } else if (spinner.equals(spinnerStreetNo)) {
            return spinnerStreetNoSelected;
        }
        return null;
    }

    private void findAndSelect(AdapterView<?> adapterView, String selectedItem) {
        Spinner spinner = (Spinner)adapterView;
        CursorAdapter adapter = (CursorAdapter)((NothingSelectedSpinnerAdapter)adapterView.getAdapter()).getAdapter();
        Cursor c = adapter.getCursor();
        while (c.moveToNext()) {
            if (c.getString(1).equals(selectedItem)) {
                spinner.setSelection(c.getPosition() + NothingSelectedSpinnerAdapter.EXTRA);
                return;
            }
        }
        spinner.setSelection(0);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        AdapterView<?> adapterView = getAdapterViewForCursorLoader(cursorLoader);
        CursorAdapter adapter;
        if (adapterView instanceof Spinner) {
            adapter = (CursorAdapter)((NothingSelectedSpinnerAdapter)adapterView.getAdapter()).getAdapter();
        } else {
            adapter = (CursorAdapter)adapterView.getAdapter();
        }
        adapter.swapCursor(null);
    }

    private AdapterView<?> getAdapterViewForCursorLoader(Loader<Cursor> cursorLoader) {
        Spinner spinner;
        if (cursorLoader.equals(loaderResult)) {
            return listViewResults;
        } else if (cursorLoader.equals(loaderCity)) {
            spinner = spinnerCity;
        } else if (cursorLoader.equals(loaderZip)) {
            spinner = spinnerZip;
        } else if (cursorLoader.equals(loaderStreet)) {
            spinner = spinnerStreet;
        } else {
            spinner = spinnerStreetNo;
        }
        return spinner;
    }

    private class ClearButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == imageButtonClearCity) {
                spinnerCity.setSelection(0);
            } else if (v == imageButtonClearZip) {
                spinnerZip.setSelection(0);
            } else if (v == imageButtonClearStreet) {
                spinnerStreet.setSelection(0);
            } else {
                spinnerStreetNo.setSelection(0);
            }
        }
    }

    private class SpinnerOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            resetLoaders((Spinner)parent);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            resetLoaders((Spinner)parent);
        }

        private void resetLoaders(Spinner spinner) {
            getLoaderManager().restartLoader(LOADER_RESULTS, null, AddressFragment.this);
            if (spinner != spinnerCity) {
                spinnerCitySelected = getSelected(spinnerCity);
                getLoaderManager().restartLoader(LOADER_CITY, null, AddressFragment.this);
            }
            if (spinner != spinnerZip) {
                spinnerZipSelected = getSelected(spinnerZip);
                getLoaderManager().restartLoader(LOADER_ZIP_CODE, null, AddressFragment.this);
            }
            if (spinner != spinnerStreet) {
                spinnerStreetSelected = getSelected(spinnerStreet);
                getLoaderManager().restartLoader(LOADER_STREET, null, AddressFragment.this);
            }
            if (spinner != spinnerStreetNo) {
                spinnerStreetNoSelected = getSelected(spinnerStreetNo);
                getLoaderManager().restartLoader(LOADER_STREET_NO, null, AddressFragment.this);
            }
        }

        private String getSelected(Spinner spinner) {
            if (spinner.getSelectedItemPosition() > 0) {
                Cursor c = (Cursor)spinner.getSelectedItem();
                if (c != null) {
                    return c.getString(1);
                }
            }
            return null;
        }
    }
}
