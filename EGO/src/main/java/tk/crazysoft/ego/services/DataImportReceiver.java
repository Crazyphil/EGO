package tk.crazysoft.ego.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.widget.Toast;

import tk.crazysoft.ego.PreferencesActivity;
import tk.crazysoft.ego.R;
import tk.crazysoft.ego.data.AddressImporter;

public class DataImportReceiver extends WakefulBroadcastReceiver {
    private PreferencesActivity activity;

    public DataImportReceiver(PreferencesActivity activity) {
        super();
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PreferencesActivity activity = this.activity;
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        Context appContext = context.getApplicationContext();
        if (action.equals(DataImportService.BROADCAST_ERROR)) {
            String error = intent.getStringExtra(DataImportService.EXTRA_ERROR_MESSAGE);
            Toast.makeText(appContext, error, Toast.LENGTH_LONG).show();
            activity.setManagementProgressBarVisibility(false);
        } else if (action.equals(DataImportService.BROADCAST_PROGRESS)) {
            double progressPercent = intent.getDoubleExtra(DataImportService.EXTRA_PROGRESS_PERCENT, 0);
            activity.setManagementProgressBarVisibility(true);
            activity.setManagementProgress((int)(progressPercent * 10000));
        } else if (action.equals(DataImportService.BROADCAST_RESULT_IMPORT)) {
            activity.setManagementProgressBarVisibility(false);

            int[] counts = intent.getIntArrayExtra(DataImportService.EXTRA_RESULT_COUNTS);
            if (counts == null || counts.length != 2) {
                return;
            }
            String message = String.format((String)context.getResources().getText(R.string.service_dataimport_result_import), counts[0], counts[1]);
            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
        } else if (action.equals(DataImportService.BROADCAST_RESULT_POSTPROCESS)) {
            activity.setManagementProgressBarVisibility(false);

            int[] counts = intent.getIntArrayExtra(DataImportService.EXTRA_RESULT_COUNTS);
            if (counts == null || counts.length != 2) {
                return;
            }

            if (intent.getStringExtra(AddressImporter.ADDRESS_IMPORTER_POSTPOCESS_ACTION) != null) {
                String message = String.format((String)context.getResources().getText(R.string.service_dataimport_result_merge), counts[0], counts[1]);
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void startServiceIntent(String action) {
        // Start the service, keeping the device awake while the service is
        // launching. This is the Intent to deliver to the service.
        Intent importIntent = new Intent(activity, DataImportService.class);
        importIntent.setAction(action);
        startWakefulService(activity, importIntent);
    }
}
