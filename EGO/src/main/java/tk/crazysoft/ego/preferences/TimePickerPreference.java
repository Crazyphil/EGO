package tk.crazysoft.ego.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import tk.crazysoft.ego.R;

public class TimePickerPreference extends DialogPreference {
    private int value;

    public TimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.time_picker_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        value = 0;

        setDialogIcon(null);
    }

    @Override
    public CharSequence getSummary() {
        int hour = value / 60;
        int minute = value - hour * 60;
        return String.format((String)super.getSummary(), hour, minute);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            value = this.getPersistedInt(0);
        } else {
            // Set default state from the XML attribute
            setValue((Integer)defaultValue);
        }

    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TimePicker timePicker = (TimePicker)view.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        setTimePickerValue(timePicker, value);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == DialogInterface.BUTTON_POSITIVE) {
            value = getTimePickerValue();
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            setValue(value);
        }
    }

    private void setValue(int value) {
        this.value = value;
        persistInt(value);
    }

    private int getTimePickerValue() {
        TimePicker timePicker = (TimePicker)getDialog().findViewById(R.id.timePicker);
        return timePicker.getCurrentHour() * 60 + timePicker.getCurrentMinute();
    }

    private void setTimePickerValue(int value) {
        TimePicker timePicker = (TimePicker)getDialog().findViewById(R.id.timePicker);
        setTimePickerValue(timePicker, value);
    }

    private void setTimePickerValue(TimePicker timePicker, int value) {
        int hour = value / 60;
        int minute = value - hour * 60;
        timePicker.setCurrentHour(hour);
        timePicker.setCurrentMinute(minute);
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
        myState.value = getTimePickerValue();
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
        SavedState myState = (SavedState)state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Set this Preference's widget to reflect the restored state
        value = myState.value;
        setTimePickerValue(value);
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeInt(value);
        }

        // Standard creator object using an instance of this class
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
