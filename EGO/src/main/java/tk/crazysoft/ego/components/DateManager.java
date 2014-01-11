package tk.crazysoft.ego.components;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateManager {
    private Calendar date;
    private boolean isToday = true;
    private BroadcastReceiver dateChangeReceiver;
    private OnDateChangedListener changedListener;
    private DateDialogFragment dateDialogFragment;

    public DateManager(Bundle savedInstanceState) {
        if (savedInstanceState != null && !savedInstanceState.getBoolean("isToday")) {
            date = GregorianCalendar.getInstance();
            date.setTimeInMillis(savedInstanceState.getLong("date"));
            isToday = false;
            fireDateChanged();
        } else {
            setToToday();
        }

        dateDialogFragment = DateDialogFragment.newInstance(this);
    }

    public void onResume(Activity activity) {
        IntentFilter timeFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        timeFilter.addAction(Intent.ACTION_TIME_CHANGED);
        timeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (dateChangeReceiver == null) {
            dateChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Intent.ACTION_TIME_TICK) || intent.getAction().equals(Intent.ACTION_TIME_CHANGED) || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                        if (isToday) {
                            setToToday();
                        }
                    }
                }
            };
        }
        activity.registerReceiver(dateChangeReceiver, timeFilter);
    }

    public void onPause(Activity activity) {
        activity.unregisterReceiver(dateChangeReceiver);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("date", date.getTimeInMillis());
        outState.putBoolean("isToday", isToday);
    }

    public void setDateChangedListener(OnDateChangedListener listener) {
        changedListener = listener;
    }

    public DateDialogFragment getDateDialogFragment() {
        return dateDialogFragment;
    }

    public Calendar getDate() {
        return date;
    }

    public String getDateString() {
        return SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG).format(date.getTime());
    }

    public boolean isToday() {
        return isToday;
    }

    public void setToToday() {
        date = GregorianCalendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        isToday = true;
        fireDateChanged();
    }

    private void fireDateChanged() {
        if (changedListener != null) {
            changedListener.dateChanged();
        }
    }

    public static class DateDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        public DateManager manager;

        public static DateDialogFragment newInstance(DateManager manager) {
            DateDialogFragment fragment = new DateDialogFragment();
            fragment.manager = manager;
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, manager.date.get(Calendar.YEAR), manager.date.get(Calendar.MONTH), manager.date.get(Calendar.DATE));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                dialog.getDatePicker().setCalendarViewShown(true);
                dialog.getDatePicker().setSpinnersShown(false);
            }
            return dialog;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            int currentYear = manager.date.get(Calendar.YEAR), currentMonth = manager.date.get(Calendar.MONTH), currentDay = manager.date.get(Calendar.DATE);
            if (currentYear == year && currentMonth == monthOfYear && currentDay == dayOfMonth) {
                return;
            }

            manager.date.set(year, monthOfYear, dayOfMonth);
            Calendar now = GregorianCalendar.getInstance();
            currentYear = now.get(Calendar.YEAR);
            currentMonth = now.get(Calendar.MONTH);
            currentDay = now.get(Calendar.DATE);
            manager.isToday = currentYear == year && currentMonth == monthOfYear && currentDay == dayOfMonth;

            manager.fireDateChanged();
        }
    }

    public static interface OnDateChangedListener {
        public void dateChanged();
    }
}