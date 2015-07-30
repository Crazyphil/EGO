package tk.crazysoft.ego.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;

import java.util.regex.Pattern;

import tk.crazysoft.ego.R;
import tk.crazysoft.ego.data.EGOContract;
import tk.crazysoft.ego.data.EGOCursorLoader;

public class AddressPreference extends DialogPreference implements LoaderManager.LoaderCallbacks<Cursor> {
    private double latitude, longitude;
    private Pattern regexCoords = Pattern.compile("^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$");

    private AutoCompleteTextView editText;

    private static final String COLUMN_NAME_ADDRESS_COMBINED = "address";

    public AddressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogMessage(R.string.preferences_activity_map_address_text);
        setDialogLayoutResource(R.layout.autocomplete_input_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        latitude = 0;
        longitude = 0;

        setDialogIcon(null);
    }

    @Override
    protected View onCreateDialogView() {
        View view = super.onCreateDialogView();
        editText = (AutoCompleteTextView)view.findViewById(R.id.editText);

        String[] fromColumns = { "address"};
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
                if (regexCoords.matcher(s).matches()) {
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

        return view;
    }

    private Cursor runAutoCompleteQuery(CharSequence filter) {
        String[] projection = {
                EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_STREET_NO + " || ', ' || " +
                        EGOContract.Addresses.COLUMN_NAME_ZIP + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_CITY + " AS " + COLUMN_NAME_ADDRESS_COMBINED,
                EGOContract.Addresses.COLUMN_NAME_LATITUDE,
                EGOContract.Addresses.COLUMN_NAME_LONGITUDE
        };
        String sortOrder = COLUMN_NAME_ADDRESS_COMBINED;
        String selectionArgs = "%";
        if (filter != null) {
            selectionArgs += filter.toString().replace(' ', '%') + "%";
        }
        EGOCursorLoader loader = new EGOCursorLoader(getContext(), EGOContract.Addresses.TABLE_NAME, projection, "address LIKE ?", new String[] { selectionArgs }, sortOrder);
        return loader.loadInBackground();
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            String coords = this.getPersistedString("0,0");
            setValue(coords);
        } else {
            // Set default state from the XML attribute
            String[] coords = ((String)defaultValue).split(",");
            double latitude = Double.parseDouble(coords[0]);
            double longitude = Double.parseDouble(coords[1]);

            // Migrate legacy latitude/longitude preferences to single address preference
            Preferences preferences = new Preferences(getContext());
            if (preferences.getMapLatitude() != 0 && preferences.getMapLongitude() != 0) {
                latitude = preferences.getMapLatitude();
                longitude = preferences.getMapLongitude();
            }
            setValue(latitude, longitude, true);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        editText = (AutoCompleteTextView)view.findViewById(R.id.editText);
        ((ActionBarActivity)getContext()).getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            setValue(latitude, longitude, true);
        }
    }

    private void setValue(String value) {
        String[] coords = value.split(",");
        if (coords.length != 2) return;
        setValue(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), false);
    }

    private void setValue(double latitude, double longitude, boolean persist) {
        this.latitude = latitude;
        this.longitude = longitude;
        if (persist) {
            persistString(String.format("%s,%s", latitude, longitude));
        }
    }

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
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        editText.setText("");
        if (data.moveToFirst()) {
            editText.append(data.getString(data.getColumnIndex(COLUMN_NAME_ADDRESS_COMBINED)));

        } else {
            editText.append(String.format("%s,%s", latitude, longitude));
        }
        editText.dismissDropDown();
        setOkButtonEnabled(true);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }

    private void setOkButtonEnabled(boolean state) {
        if (getDialog() != null) {
            ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(state);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent, use superclass state
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current setting value
        myState.value = editText.getText().toString();
        myState.latitude = latitude;
        myState.longitude = longitude;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Set this Preference's widget to reflect the restored state
        latitude = myState.latitude;
        longitude = myState.longitude;
        editText.setText(myState.value);
    }

    private static class SavedState extends BaseSavedState {
        // Members that hold the setting's value
        private double latitude, longitude;
        private String value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            double[] coords = new double[2];
            source.readDoubleArray(coords);
            latitude = coords[0];
            longitude = coords[1];
            value = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeDoubleArray(new double[]{latitude, longitude });
            dest.writeString(value);
        }

        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
