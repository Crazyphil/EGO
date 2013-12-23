package tk.crazysoft.ego;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import tk.crazysoft.ego.data.EGOContract;
import tk.crazysoft.ego.data.EGOCursorLoader;

public class HouseFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private TextView textViewAddress, textViewCity, textViewCoordinates, textViewMapSheet;
    private Button buttonNavigate;
    private MapFragment mapFragment;

    private double latitude, longitude;

    @Override
    public void onStart() {
        super.onStart();

        textViewAddress = (TextView)getView().findViewById(R.id.house_textViewAddress);
        textViewCity = (TextView)getView().findViewById(R.id.house_textViewCity);
        textViewCoordinates = (TextView)getView().findViewById(R.id.house_textViewCoords);
        textViewMapSheet = (TextView)getView().findViewById(R.id.house_textViewMapSheet);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.house_view, container, false);
        if (savedInstanceState == null) {
            mapFragment = new MapFragment();
            getChildFragmentManager().beginTransaction().add(R.id.house_mapContainer, mapFragment).commit();
        }

        buttonNavigate = (Button)view.findViewById(R.id.house_buttonNavigate);
        buttonNavigate.setOnClickListener(new NavigateButtonOnClickListener());

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments()!= null && getArguments().containsKey("id")) {
            setHouse(getArguments().getLong("id"));
        }
    }

    public void setHouse(long id) {
        Bundle loaderArgs = new Bundle(1);
        loaderArgs.putLong("id", id);
        getLoaderManager().restartLoader(0, loaderArgs, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        long id = bundle.getLong("id");
        String[] projection = {
                EGOContract.Addresses._ID,
                EGOContract.Addresses.COLUMN_NAME_STREET + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_STREET_NO + " AS address",
                EGOContract.Addresses.COLUMN_NAME_ZIP + " || ' ' || " + EGOContract.Addresses.COLUMN_NAME_CITY + " AS region",
                EGOContract.Addresses.COLUMN_NAME_LATITUDE,
                EGOContract.Addresses.COLUMN_NAME_LONGITUDE,
                EGOContract.Addresses.COLUMN_NAME_MAP_SHEET
        };
        String selection = EGOContract.Addresses._ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        return new EGOCursorLoader(getActivity(), EGOContract.Addresses.TABLE_NAME, projection, selection, selectionArgs);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor.moveToFirst()) {
            latitude = cursor.getDouble(cursor.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_LATITUDE));
            longitude = cursor.getDouble(cursor.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_LONGITUDE));
            String mapSheet = cursor.getString(cursor.getColumnIndex(EGOContract.Addresses.COLUMN_NAME_MAP_SHEET));

            textViewAddress.setText(cursor.getString(cursor.getColumnIndex("address")));
            textViewCity.setText(cursor.getString(cursor.getColumnIndex("region")));
            textViewCoordinates.setText(String.format("%s, %s", formatLatitude(latitude), formatLongitude(longitude)));
            if (mapSheet != null && !mapSheet.equals("")) {
                textViewMapSheet.setVisibility(View.VISIBLE);
                textViewMapSheet.setText(getResources().getString(R.string.house_view_mapsheet, mapSheet));
            } else {
                textViewMapSheet.setVisibility(View.GONE);
            }

            mapFragment.setDestination(latitude, longitude, cursor.getString(cursor.getColumnIndex("address")));
        }
    }

    private String formatLatitude(double latitude) {
        return String.format("%s%s", formatCoordinate(latitude), latitude > 0 ? getResources().getString(R.string.house_view_coordinate_north) : getResources().getString(R.string.house_view_coordinate_south));
    }

    private String formatLongitude(double longitude) {
        return String.format("%s%s", formatCoordinate(longitude), longitude > 0 ? getResources().getString(R.string.house_view_coordinate_east) : getResources().getString(R.string.house_view_coordinate_west));
    }

    private String formatCoordinate(double coordinate) {
        double minutes = (coordinate - (int)coordinate) * 60;
        double seconds = (minutes - (int)minutes) * 60;
        return String.format("%d°%d'%,.2f\"", (int)coordinate, (int)minutes, seconds);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private class NavigateButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            MainActivity.sendNavigationIntent(getActivity(), latitude, longitude);
        }
    }
}