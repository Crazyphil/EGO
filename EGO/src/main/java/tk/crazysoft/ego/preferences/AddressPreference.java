package tk.crazysoft.ego.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

public class AddressPreference extends DialogPreference {
    private static final String COORDS_FORMAT = "%s,%s";

    private double latitude, longitude;

    public AddressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public String getCoordinates() {
        return String.format(COORDS_FORMAT, latitude, longitude);
    }

    public void setCoordinates(String value) {
        String[] coords = value.split(",");
        if (coords.length != 2) return;
        setCoordinates(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
    }

    public void setCoordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        persistString(String.format(COORDS_FORMAT, latitude, longitude));
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setCoordinates(restorePersistedValue ? getPersistedString((String) defaultValue) : (String) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
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
        setCoordinates(myState.latitude, myState.longitude);
    }

    private static class SavedState extends BaseSavedState {
        // Members that hold the setting's value
        private double latitude, longitude;

        private SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            double[] coords = new double[2];
            source.readDoubleArray(coords);
            latitude = coords[0];
            longitude = coords[1];
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeDoubleArray(new double[] { latitude, longitude });
        }

        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
