package tk.crazysoft.ego.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class DataImportReceiver extends WakefulBroadcastReceiver {
    private WeakReference<ActionBarActivity> activity;

    public DataImportReceiver(ActionBarActivity activity) {
        super();
        this.activity = new WeakReference<ActionBarActivity>(activity);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ActionBarActivity activity = this.activity.get();
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (action.equals(DataImportService.BROADCAST_ERROR)) {
            Context appContext = context.getApplicationContext();
            String error = intent.getStringExtra(DataImportService.EXTRA_ERROR_MESSAGE);
            Toast.makeText(appContext, error, Toast.LENGTH_LONG).show();
            if (activity != null) {
                activity.setSupportProgressBarVisibility(false);
            }
        } else if (action.equals(DataImportService.BROADCAST_PROGRESS) && activity != null) {
            double progressPercent = intent.getDoubleExtra(DataImportService.EXTRA_PROGRESS_PERCENT, 0);
            activity.setSupportProgressBarVisibility(true);
            activity.setSupportProgress((int)(progressPercent * 10000));
        } else if (action.equals(DataImportService.BROADCAST_RESULT) && activity != null) {
            activity.setSupportProgressBarVisibility(false);
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
