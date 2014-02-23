package tk.crazysoft.ego;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import tk.crazysoft.ego.components.DateManager;
import tk.crazysoft.ego.components.TimeManager;
import tk.crazysoft.ego.data.EGOContactsCursorLoader;
import tk.crazysoft.ego.data.EGOContract;
import tk.crazysoft.ego.preferences.Preferences;

public class HospitalsDoctorsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_HOSPITALS = 0, LOADER_DOCTORS = 1;

    private Preferences preferences;
    private DateManager dateManager;
    private TimeManager timeManager;
    private Button buttonDate, buttonToday, buttonTime, buttonNow;
    private TextView textViewHospitalsEmpty, textViewDoctorsEmpty;
    private ListView listViewHospitals, listViewDoctors;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dateManager = new DateManager(savedInstanceState);
        timeManager = new TimeManager(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.hospitals_doctors_view, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        preferences = new Preferences(getActivity());

        buttonDate = (Button)getView().findViewById(R.id.hospitals_doctors_buttonDate);
        buttonToday = (Button)getView().findViewById(R.id.hospitals_doctors_buttonToday);
        buttonTime = (Button)getView().findViewById(R.id.hospitals_doctors_buttonTime);
        buttonNow = (Button)getView().findViewById(R.id.hospitals_doctors_buttonNow);

        buttonDate.setText(dateManager.getDateString());
        buttonDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateManager.getDateDialogFragment().show(getFragmentManager(), "dateDialog");
            }
        });

        buttonToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateManager.setToToday();
            }
        });
        buttonToday.setVisibility(dateManager.isToday() ? View.GONE : View.VISIBLE);

        buttonTime.setText(timeManager.getTimeString());
        buttonTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeManager.getTimeDialogFragment().show(getFragmentManager(), "timeDialog");
            }
        });

        buttonNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeManager.setToNow();
            }
        });
        buttonNow.setVisibility(timeManager.isNow() ? View.GONE : View.VISIBLE);

        dateManager.setDateChangedListener(new OnDateChangedListener());
        timeManager.setTimeChangedListener(new OnTimeChangedListener());

        listViewHospitals = (ListView)getView().findViewById(R.id.hospitals_doctors_listViewHospitals);
        listViewDoctors = (ListView)getView().findViewById(R.id.hospitals_doctors_listViewDoctors);
        textViewHospitalsEmpty = (TextView)getView().findViewById(R.id.hospitals_doctors_textViewHospitalsEmpty);
        textViewDoctorsEmpty = (TextView)getView().findViewById(R.id.hospitals_doctors_textViewDoctorsEmpty);

        setEmptyText();

        String[] fromColumns = { EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME, EGOContactsCursorLoader.COLUMN_NAME_ADDRESS };
        int[] toViews = { android.R.id.text1, android.R.id.text2 };
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), R.layout.contact_item, null, fromColumns, toViews, 0);
        adapter.setViewBinder(new ContactsViewBinder());

        listViewHospitals.setAdapter(adapter);
        listViewHospitals.setEmptyView(textViewHospitalsEmpty);
        listViewHospitals.setOnItemClickListener(new ContactListItemOnClickListener());

        adapter = new SimpleCursorAdapter(getActivity(), R.layout.contact_item, null, fromColumns, toViews, 0);
        adapter.setViewBinder(new ContactsViewBinder());
        listViewDoctors.setAdapter(adapter);
        listViewDoctors.setEmptyView(textViewDoctorsEmpty);
        listViewDoctors.setOnItemClickListener(new ContactListItemOnClickListener());

        getLoaderManager().initLoader(LOADER_HOSPITALS, null, this);
        getLoaderManager().initLoader(LOADER_DOCTORS, null, this);
    }

    private void setEmptyText() {
        textViewHospitalsEmpty.setText(getString(R.string.hospitals_doctors_view_hospitals_empty, dateManager.getDateString(), timeManager.getTimeString()));
        textViewDoctorsEmpty.setText(getString(R.string.hospitals_doctors_view_doctors_empty, dateManager.getDateString(), timeManager.getTimeString()));
    }

    private void reloadData() {
        setEmptyText();
        getLoaderManager().restartLoader(LOADER_HOSPITALS, null, HospitalsDoctorsFragment.this);
        getLoaderManager().restartLoader(LOADER_DOCTORS, null, HospitalsDoctorsFragment.this);
    }

    @Override
    public void onResume() {
        super.onResume();

        dateManager.onResume(getActivity());
        timeManager.onResume(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();

        dateManager.onPause(getActivity());
        timeManager.onPause(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        dateManager.onSaveInstanceState(outState);
        timeManager.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_HOSPITALS:
                return createHospitalsLoader();
            case LOADER_DOCTORS:
                return createDoctorsLoader();
        }
        return null;
    }

    private Loader<Cursor> createHospitalsLoader() {
        long date = dateManager.getDate().getTimeInMillis() / 1000;
        Preferences.Time takeoverTime = preferences.getHospitalsDoctorsTakeover();
        Preferences.Time time = timeManager.getTime();

        String table = EGOContract.HospitalAdmission.TABLE_NAME + " LEFT JOIN " + EGOContract.NameReplacements.TABLE_NAME + " ON " + EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME + " = " + EGOContract.NameReplacements.COLUMN_NAME_NAME;
        String[] projection = {
                EGOContract.HospitalAdmission.TABLE_NAME + "." + EGOContract.HospitalAdmission._ID,
                EGOContract.HospitalAdmission.COLUMN_NAME_HOSPITAL_NAME + " AS " + EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME,
                EGOContract.NameReplacements.COLUMN_NAME_REPLACEMENT + " AS " + EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME_ALIAS,
                "NULL AS " +  EGOContactsCursorLoader.COLUMN_NAME_ADDRESS
        };
        String sortOrder = EGOContract.HospitalAdmission.COLUMN_NAME_DATE + " DESC, " + EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME_ALIAS + ", " + EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME;
        String selection;
        if (takeoverTime.getHour() > time.getHour() || takeoverTime.getHour() == time.getHour() && takeoverTime.getMinute() > time.getMinute()) {
            // Admittances after midnight but before the takeover time are stored with yesterday's date
            date -= 24 * 60 * 60;
            selection = EGOContract.HospitalAdmission.COLUMN_NAME_DATE + " = 0 OR " +
                    EGOContract.HospitalAdmission.COLUMN_NAME_DATE + " = ? AND " + EGOContract.HospitalAdmission.COLUMN_NAME_TAKEOVER_TIME + " > ?";
        } else {
            selection = EGOContract.HospitalAdmission.COLUMN_NAME_DATE + " = 0 OR " +
                    EGOContract.HospitalAdmission.COLUMN_NAME_DATE + " = ? AND " + EGOContract.HospitalAdmission.COLUMN_NAME_TAKEOVER_TIME + " <= ?";
        }
        String[] selectionArgs = { String.valueOf(date), String.valueOf(timeManager.getTime().toInt()) };

        return new EGOContactsCursorLoader(getActivity(), table, projection, selection, selectionArgs, sortOrder);
    }

    private Loader<Cursor> createDoctorsLoader() {
        long date = dateManager.getDate().getTimeInMillis() / 1000;

        String table = EGOContract.DoctorStandby.TABLE_NAME +
                " LEFT OUTER JOIN " + EGOContract.DoctorStandby.TABLE_NAME + " AS t2 ON t2." + EGOContract.DoctorStandby._ID + " = " + EGOContract.DoctorStandby.TABLE_NAME + "." + EGOContract.DoctorStandby.COLUMN_NAME_NEXT_ID +
                " LEFT JOIN " + EGOContract.NameReplacements.TABLE_NAME + " ON " + EGOContract.DoctorStandby.TABLE_NAME + "." + EGOContract.DoctorStandby.COLUMN_NAME_DOCTOR_NAME + " = " + EGOContract.NameReplacements.COLUMN_NAME_NAME;
        String[] projection = {
                EGOContract.DoctorStandby.TABLE_NAME + "." + EGOContract.DoctorStandby._ID,
                EGOContract.DoctorStandby.TABLE_NAME + "." + EGOContract.DoctorStandby.COLUMN_NAME_DOCTOR_NAME + " AS " + EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME,
                EGOContract.NameReplacements.COLUMN_NAME_REPLACEMENT + " AS " + EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME_ALIAS,
                "CASE " + EGOContract.DoctorStandby.TABLE_NAME + "." + EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO + " WHEN " + (24 * 60) +
                        " THEN t2." + EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO +
                        " ELSE " + EGOContract.DoctorStandby.TABLE_NAME + "." + EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO +
                " END AS " + EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO,
                "NULL AS " +  EGOContactsCursorLoader.COLUMN_NAME_ADDRESS
        };
        String sortOrder = EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME_ALIAS + ", " + EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME;
        String selection = EGOContract.DoctorStandby.TABLE_NAME + "." + EGOContract.DoctorStandby.COLUMN_NAME_DATE + " = ?" +
                " AND ? BETWEEN " + EGOContract.DoctorStandby.TABLE_NAME + "." + EGOContract.DoctorStandby.COLUMN_NAME_TIME_FROM + " AND " + EGOContract.DoctorStandby.TABLE_NAME + "." + EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO;
        String[] selectionArgs = { String.valueOf(date), String.valueOf(timeManager.getTime().toInt()) };

        return new EGOContactsCursorLoader(getActivity(), table, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        CursorAdapter adapter = getAdapterForLoaderId(loader.getId());
        if (adapter != null) {
            adapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        CursorAdapter adapter = getAdapterForLoaderId(loader.getId());
        if (adapter != null) {
            adapter.swapCursor(null);
        }
    }

    private CursorAdapter getAdapterForLoaderId(int loaderId) {
        switch (loaderId) {
            case LOADER_HOSPITALS:
                return (CursorAdapter)listViewHospitals.getAdapter();
            case LOADER_DOCTORS:
                return (CursorAdapter)listViewDoctors.getAdapter();
        }
        return null;
    }

    private class ContactsViewBinder implements SimpleCursorAdapter.ViewBinder {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (view.getId() == android.R.id.text2) {
                // Get LinearLayout in which the ListView containing the item is in
                LinearLayout layout = (LinearLayout)view.getParent().getParent();

                View separator = layout.findViewById(R.id.separator);
                ImageView navigate = (ImageView)layout.findViewById(R.id.navigate);
                if (cursor.getString(columnIndex) == null) {
                    separator.setVisibility(View.GONE);
                    navigate.setVisibility(View.GONE);
                } else {
                    separator.setVisibility(View.VISIBLE);
                    navigate.setVisibility(View.VISIBLE);
                }
            } else if (view.getId() == android.R.id.text1) {
                String name = cursor.getString(columnIndex);
                int aliasColumn = cursor.getColumnIndex(EGOContactsCursorLoader.COLUMN_NAME_CONTACT_NAME_ALIAS);
                if (aliasColumn > -1 && cursor.getString(aliasColumn) != null) {
                    name = cursor.getString(aliasColumn);
                }

                int timeColumn = cursor.getColumnIndex(EGOContract.DoctorStandby.COLUMN_NAME_TIME_TO);
                if (timeColumn > -1) {
                    Date time = Preferences.Time.toDate(cursor.getInt(timeColumn));
                    name = getString(R.string.hospitals_doctors_view_doctors_time, SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(time), name);
                }
                ((TextView)view).setText(name);
                return true;
            }
            return false;
        }
    }

    private class OnDateChangedListener implements DateManager.OnDateChangedListener {
        @Override
        public void dateChanged() {
            buttonDate.setText(dateManager.getDateString());
            if (dateManager.isToday()) {
                buttonToday.setVisibility(View.GONE);
            } else {
                buttonToday.setVisibility(View.VISIBLE);
            }
            reloadData();
        }
    }

    private class OnTimeChangedListener implements TimeManager.OnTimeChangedListener {
        @Override
        public void timeChanged() {
            buttonTime.setText(timeManager.getTimeString());
            if (timeManager.isNow()) {
                buttonNow.setVisibility(View.GONE);
            } else {
                buttonNow.setVisibility(View.VISIBLE);
            }
            reloadData();
        }
    }

    private class ContactListItemOnClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor)parent.getAdapter().getItem(position);
            String address = cursor.getString(cursor.getColumnIndex(EGOContactsCursorLoader.COLUMN_NAME_ADDRESS));
            if (address == null || address.isEmpty()) {
                return;
            }

            MainActivity.sendNavigationIntent(getActivity(), address);
        }
    }
}