package tk.crazysoft.ego;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import tk.crazysoft.ego.components.NoDefaultSpinner;

public class AddressFragment extends Fragment/* implements LoaderManager.LoaderCallbacks<Cursor>*/ {
    private NoDefaultSpinner spinnerCity, spinnerZip, spinnerStreet, spinnerStreetNo;
    private ImageButton imageButtonClearCity, imageButtonClearZip, imageButtonClearStreet, imageButtonClearStreetNo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.address_view, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        spinnerCity = (NoDefaultSpinner)getView().findViewById(R.id.address_spinnerCity);
        spinnerZip = (NoDefaultSpinner)getView().findViewById(R.id.address_spinnerZip);
        spinnerStreet = (NoDefaultSpinner)getView().findViewById(R.id.address_spinnerStreet);
        spinnerStreetNo = (NoDefaultSpinner)getView().findViewById(R.id.address_spinnerStreetNo);

        imageButtonClearCity = (ImageButton)getView().findViewById(R.id.address_imageButtonClearCity);
        imageButtonClearZip = (ImageButton)getView().findViewById(R.id.address_imageButtonClearZip);
        imageButtonClearStreet = (ImageButton)getView().findViewById(R.id.address_imageButtonClearStreet);
        imageButtonClearStreetNo = (ImageButton)getView().findViewById(R.id.address_imageButtonClearStreetNo);

        setDataOnSpinner(spinnerCity, R.array.city_array, false);
        setDataOnSpinner(spinnerZip, R.array.zip_array, false);
        setDataOnSpinner(spinnerStreet, R.array.street_array, true);
        setDataOnSpinner(spinnerStreetNo, R.array.streetno_array, true);

        imageButtonClearCity.setOnClickListener(new ClearButtonOnClickListener());
        imageButtonClearZip.setOnClickListener(new ClearButtonOnClickListener());
        imageButtonClearStreet.setOnClickListener(new ClearButtonOnClickListener());
        imageButtonClearStreetNo.setOnClickListener(new ClearButtonOnClickListener());
    }

    private void setDataOnSpinner(Spinner spinner, int dataId, boolean isEnabled) {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getView().getContext(), dataId, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        if (!isEnabled) {
            spinner.setEnabled(false);
        }
    }

    private class ClearButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == imageButtonClearCity) {
                spinnerCity.setSelection(-1);
            }
            else if (v == imageButtonClearZip) {
                spinnerZip.setSelection(-1);
            }
            else if (v == imageButtonClearStreet) {
                spinnerStreet.setSelection(-1);
            }
            else {
                spinnerStreetNo.setSelection(-1);
            }
        }
    }
}
