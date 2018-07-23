package tk.crazysoft.ego.preferences;

import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import tk.crazysoft.ego.R;
import tk.crazysoft.ego.data.EGOContract;
import tk.crazysoft.ego.data.EGOCursorLoader;

import java.util.regex.Pattern;

public class AddressPreferenceDialogFragment extends PreferenceDialogFragmentCompat implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String SAVE_STATE_TEXT = "AddressPreferenceDialogFragment.text";
    private static final String SAVE_STATE_LATITUDE = "AddressPreferenceDialogFragment.latitude";
    private static final String SAVE_STATE_LONGITUDE = "AddressPreferenceDialogFragment.longitude";

    private static final String COLUMN_NAME_ADDRESS_COMBINED = "address";
    private static final Pattern REGEX_COORDS = Pattern.compile("^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$");

    private double latitude, longitude;
    private String text;

    private AutoCompleteTextView editText;

    public static AddressPreferenceDialogFragment newInstance(AddressPreference pref) {
        final AddressPreferenceDialogFragment fragment = new AddressPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, pref.getKey());
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            text = getAddressPreference().getCoordinates();
        } else {
            text = savedInstanceState.getString(SAVE_STATE_TEXT);
            latitude = savedInstanceState.getDouble(SAVE_STATE_LATITUDE);
            longitude = savedInstanceState.getDouble(SAVE_STATE_LONGITUDE);
        }
    }

    @SuppressLint("InflateParams")
    @Override
    protected View onCreateDialogView(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.autocomplete_input_dialog, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        editText = view.findViewById(R.id.editText);

        String[] fromColumns = { COLUMN_NAME_ADDRESS_COMBINED };
        int[] toViews = { android.R.id.text1 };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getContext(), android.R.layout.simple_list_item_1, null, fromColumns, toViews, 0);
        adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                return cursor.getString(cursor.getColumnIndex(COLUMN_NAME_ADDRESS_COMBINED));
            }
        });
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return runAutoCompleteQuery(constraint);
            }
        });
        editText.setAdapter(adapter);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                text = s.toString();
                if (REGEX_COORDS.matcher(s).matches()) {
                    setValue(s.toString());
                    setOkButtonEnabled(true);
                } else {
                    setOkButtonEnabled(false);
                }
            }
        });
        editText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) editText.getAdapter().getItem(position);
                latitude = c.getDouble(c.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_LATITUDE));
                longitude = c.getDouble(c.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_LONGITUDE));
                setOkButtonEnabled(true);
            }
        });
        ((AppCompatActivity)getContext()).getSupportLoaderManager().initLoader(0, null, this);

        editText.setText(text);
    }

    @Override
    protected boolean needInputMethod() {
        // We want the input method to show, if possible, when dialog is displayed
        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVE_STATE_TEXT, text);
        outState.putDouble(SAVE_STATE_LATITUDE, latitude);
        outState.putDouble(SAVE_STATE_LONGITUDE, longitude);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            if (getAddressPreference().callChangeListener(text)) {
                getAddressPreference().setCoordinates(latitude, longitude);
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_STREET_NO + " || ', ' || " +
                        EGOContract.Addresses.COLUMN_NAME_ZIP + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_CITY + " AS " + COLUMN_NAME_ADDRESS_COMBINED
        };
        return new EGOCursorLoader(getContext(), EGOContract.Addresses.TABLE_NAME, projection, "latitude = ? AND longitude = ?", new String[] { String.valueOf(latitude), String.valueOf(longitude) }, null, "1");
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            editText.setText("");
            editText.append(data.getString(data.getColumnIndex(COLUMN_NAME_ADDRESS_COMBINED)));
            editText.dismissDropDown();
            setOkButtonEnabled(true);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) { }

    private AddressPreference getAddressPreference() {
        return (AddressPreference) getPreference();
    }

    private Cursor runAutoCompleteQuery(CharSequence filter) {
        String[] projection = {
                EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_STREET_NO + " || ', ' || " +
                        EGOContract.Addresses.COLUMN_NAME_ZIP + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_CITY + " AS " + COLUMN_NAME_ADDRESS_COMBINED,
                EGOContract.Addresses.COLUMN_NAME_LATITUDE,
                EGOContract.Addresses.COLUMN_NAME_LONGITUDE
        };
        String selectionArgs = "%";
        if (filter != null) {
            selectionArgs += filter.toString().replace(' ', '%') + "%";
        }
        EGOCursorLoader loader = new EGOCursorLoader(getContext(), EGOContract.Addresses.TABLE_NAME, projection, "address LIKE ?", new String[] { selectionArgs }, COLUMN_NAME_ADDRESS_COMBINED);
        return loader.loadInBackground();
    }

    private void setValue(String value) {
        String[] coords = value.split(",");
        if (coords.length != 2) return;
        this.latitude = Double.parseDouble(coords[0]);
        this.longitude = Double.parseDouble(coords[1]);
    }

    private void setOkButtonEnabled(boolean state) {
        if (getDialog() != null) {
            ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(state);
        }
    }
}
