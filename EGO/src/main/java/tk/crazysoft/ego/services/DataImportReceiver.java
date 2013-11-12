package tk.crazysoft.ego.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import tk.crazysoft.ego.DataManagementActivity;

public class DataImportReceiver extends WakefulBroadcastReceiver {
    private WeakReference<DataManagementActivity> activity;

    public DataImportReceiver(DataManagementActivity activity) {
        super();
        this.activity = new WeakReference<DataManagementActivity>(activity);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        DataManagementActivity activity = this.activity.get();
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (action.equals(DataImportService.BROADCAST_ERROR)) {
            Context appContext = context.getApplicationContext();
            String error = intent.getStringExtra(DataImportService.EXTRA_ERROR_MESSAGE);
            Toast.makeText(appContext, error, Toast.LENGTH_LONG).show();
            if (activity != null) {
                activity.setManagementProgressBarVisibility(false);
            }
        } else if (action.equals(DataImportService.BROADCAST_PROGRESS) && activity != null) {
            double progressPercent = intent.getDoubleExtra(DataImportService.EXTRA_PROGRESS_PERCENT, 0);
            activity.setManagementProgressBarVisibility(true);
            activity.setManagementProgress((int)(progressPercent * 10000));
        } else if (action.equals(DataImportService.BROADCAST_RESULT) && activity != null) {
            activity.setManagementProgressBarVisibility(false);
        }
    }

    public void startServiceIntent(String action) {
        // Start the service, keeping the device awake while the service is
        // launching. This is the Intent to deliver to the service.
        Intent importIntent = new Intent(activity.get(), DataImportService.class);
        importIntent.setAction(action);
        startWakefulService(activity.get(), importIntent);
    }
}
