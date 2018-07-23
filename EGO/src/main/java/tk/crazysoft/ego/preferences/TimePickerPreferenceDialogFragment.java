package tk.crazysoft.ego.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;
import tk.crazysoft.ego.R;

public class TimePickerPreferenceDialogFragment extends PreferenceDialogFragmentCompat {
    private static final String SAVE_STATE_VALUE = "TimePickerPreferenceDialogFragment.value";

    private int value;

    public static TimePickerPreferenceDialogFragment newInstance(TimePickerPreference pref) {
        TimePickerPreferenceDialogFragment fragment = new TimePickerPreferenceDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEY, pref.getKey());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            value = getTimePickerPreference().getTime();
        } else {
            value = savedInstanceState.getInt(SAVE_STATE_VALUE);
        }
    }

    @SuppressLint("InflateParams")
    @Override
    protected View onCreateDialogView(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.time_picker_dialog, null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TimePicker timePicker = view.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        setTimePickerValue(timePicker, value);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_VALUE, getTimePickerValue());
    }

    private TimePickerPreference getTimePickerPreference() {
        return (TimePickerPreference) getPreference();
    }

    private int getTimePickerValue() {
        TimePicker timePicker = getDialog().findViewById(R.id.timePicker);
        return timePicker.getCurrentHour() * 60 + timePicker.getCurrentMinute();
    }

    private void setTimePickerValue(TimePicker timePicker, int value) {
        int hour = value / 60;
        int minute = value - hour * 60;
        timePicker.setCurrentHour(hour);
        timePicker.setCurrentMinute(minute);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            int value = getTimePickerValue();
            if (getTimePickerPreference().callChangeListener(value))
            getTimePickerPreference().setTime(value);
        }
    }
}
