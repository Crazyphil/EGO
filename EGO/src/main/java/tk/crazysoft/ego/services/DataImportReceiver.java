package tk.crazysoft.ego.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class DataImportReceiver extends BroadcastReceiver {
    private WeakReference<ActionBarActivity> activity;

    public DataImportReceiver(ActionBarActivity activity) {
        super();
        this.activity = new WeakReference<ActionBarActivity>(activity);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(DataImportService.BROADCAST_ERROR)) {
            Context appContext = context.getApplicationContext();
            String error = intent.getStringExtra(DataImportService.EXTRA_ERROR_MESSAGE);
            Toast.makeText(appContext, error, Toast.LENGTH_LONG).show();
            activity.get().setSupportProgressBarVisibility(false);
        }
        else if (intent.getAction().equals(DataImportService.BROADCAST_PROGRESS) && activity.get() != null) {
            double progressPercent = intent.getDoubleExtra(DataImportService.EXTRA_PROGRESS_PERCENT, 0);
            ActionBarActivity activity = this.activity.get();
            activity.setSupportProgressBarVisibility(true);
            activity.setSupportProgress((int)(progressPercent * 10000));
        }
        else if (intent.getAction().equals(DataImportService.BROADCAST_RESULT) && activity.get() != null) {
            activity.get().setSupportProgressBarVisibility(false);
        }
    }
}
