package tk.crazysoft.ego.preferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import tk.crazysoft.ego.services.DataImportReceiver;

public class DataImportPreference extends Preference implements Preference.OnPreferenceClickListener {
    private DataImportReceiver receiver;

    public DataImportPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnPreferenceClickListener(this);
    }

    public void setBroadcastReceiver(DataImportReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (receiver != null) {
            receiver.startServiceIntent(getIntent().getAction());
        }
        return true;
    }
}
