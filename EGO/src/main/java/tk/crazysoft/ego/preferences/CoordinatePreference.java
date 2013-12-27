package tk.crazysoft.ego.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import tk.crazysoft.ego.R;

public class CoordinatePreference extends DialogPreference {
    private double value;

    public CoordinatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.number_input_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        value = 0;

        setDialogIcon(null);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            value = Double.longBitsToDouble(this.getPersistedLong(0));
        } else {
            // Set default state from the XML attribute
            setValue((Double)defaultValue);
        }

    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        try {
            return (double)a.getFloat(index, 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        setEditTextValue(view, value);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_POSITIVE) {
            value = getEditTextValue();
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            setValue(value);
        }
    }

    private void setValue(double value) {
        this.value = value;
        persistLong(Double.doubleToRawLongBits(value));
    }

    private double getEditTextValue() {
        EditText editTextNumber = (EditText)getDialog().findViewById(R.id.editTextNumber);
        double number = 0;
        try {
            String numberStr = editTextNumber.getText().toString();
            number = Double.parseDouble(numberStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (number < -180) {
            number = -180;
        } else if (number > 180) {
            number = 180;
        }
        return number;
    }

    private void setEditTextValue(double value) {
        EditText editTextNumber = (EditText)getDialog().findViewById(R.id.editTextNumber);
        editTextNumber.setText(String.valueOf(value));
    }

    private void setEditTextValue(View view, double value) {
        EditText editTextNumber = (EditText)view.findViewById(R.id.editTextNumber);
        editTextNumber.setText(String.valueOf(value));
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
        myState.value = getEditTextValue();
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
        value = myState.value;
        setEditTextValue(value);
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        double value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readDouble();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeDouble(value);
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
