package tk.crazysoft.ego.components;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.GregorianCalendar;

import tk.crazysoft.ego.R;
import tk.crazysoft.ego.preferences.Preferences;

public class TimeManager {
    private Preferences.Time time;
    private boolean isNow = true;
    private BroadcastReceiver timeChangeReceiver;
    private OnTimeChangedListener changedListener;
    private TimeDialogFragment timeDialogFragment;

    public TimeManager(Bundle savedInstanceState) {
        if (savedInstanceState != null && !savedInstanceState.getBoolean("isNow")) {
            time = Preferences.Time.fromInt(savedInstanceState.getInt("time"));
            isNow = false;
            fireTimeChanged();
        } else {
            setToNow();
        }

        timeDialogFragment = TimeDialogFragment.newInstance(this);
    }

    public void onResume(Activity activity) {
        IntentFilter timeFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
        timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (timeChangeReceiver == null) {
            timeChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Intent.ACTION_TIME_TICK) || intent.getAction().equals(Intent.ACTION_TIME_CHANGED) || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                        if (isNow) {
                            setToNow();
                        }
                    }
                }
            };
        }
        activity.registerReceiver(timeChangeReceiver, timeFilter);
    }

    public void onPause(Activity activity) {
        activity.unregisterReceiver(timeChangeReceiver);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("time", time.toInt());
        outState.putBoolean("isNow", isNow);
    }

    public void setTimeChangedListener(OnTimeChangedListener listener) {
        changedListener = listener;
    }

    public TimeDialogFragment getTimeDialogFragment() {
        return timeDialogFragment;
    }

    public Preferences.Time getTime() {
        return time;
    }

    public boolean isNow() {
        return isNow;
    }

    public void setToNow() {
        Calendar now = GregorianCalendar.getInstance();
        if (time == null) {
            time = new Preferences.Time(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        } else {
            time.setHour(now.get(Calendar.HOUR_OF_DAY));
            time.setMinute(now.get(Calendar.MINUTE));
        }
        isNow = true;
        fireTimeChanged();
    }

    public String getTimeString(Context context) {
        return context.getString(R.string.time_format, time.getHour(), time.getMinute());
    }

    private void fireTimeChanged() {
        if (changedListener != null) {
            changedListener.timeChanged();
        }
    }

    public static class TimeDialogFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
        private TimeManager manager;

        public static TimeDialogFragment newInstance(TimeManager manager) {
            TimeDialogFragment fragment = new TimeDialogFragment();
            fragment.manager = manager;
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new TimePickerDialog(getActivity(), this, manager.time.getHour(), manager.time.getMinute(), true);
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            if (manager.time.getHour() == hourOfDay && manager.time.getMinute() == minute) {
                return;
            }

            manager.time.setHour(hourOfDay);
            manager.time.setMinute(minute);
            Calendar now = GregorianCalendar.getInstance();
            manager.isNow = now.get(Calendar.HOUR_OF_DAY) == hourOfDay && now.get(Calendar.MINUTE) == minute;

            manager.fireTimeChanged();
        }
    }

    public static interface OnTimeChangedListener {
        public void timeChanged();
    }
}
