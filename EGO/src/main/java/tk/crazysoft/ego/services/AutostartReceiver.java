package tk.crazysoft.ego.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import tk.crazysoft.ego.MainActivity;

public class AutostartReceiver extends BroadcastReceiver {
    private static final String BOOT_COMPLETE_INTENT = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(BOOT_COMPLETE_INTENT)) return;

        if (MainActivity.isDefaultLauncher(context)) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            context.startActivity(launchIntent);
        }
    }
}
