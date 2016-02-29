package tk.crazysoft.ego;

import android.app.Application;

import com.bettervectordrawable.VectorDrawableCompat;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        formUri = "https://acra.kapfer.it/submit.php",
        formUriBasicAuthLogin = "acra", // optional
        formUriBasicAuthPassword = "4St5qySTJa", // optional
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
        resDialogText = R.string.crash_dialog_text,
        resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.
)
public class EGOApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            // The following line triggers the initialization of ACRA
            ACRA.init(this);
        }

        VectorDrawableCompat.enableResourceInterceptionFor(getResources(),
                R.drawable.da_turn_arrive,
                R.drawable.da_turn_arrive_right,
                R.drawable.da_turn_depart,
                R.drawable.da_turn_ferry,
                R.drawable.da_turn_fork_right,
                R.drawable.da_turn_generic_merge,
                R.drawable.da_turn_generic_roundabout,
                R.drawable.da_turn_ramp_right,
                R.drawable.da_turn_right,
                R.drawable.da_turn_roundabout_1,
                R.drawable.da_turn_roundabout_2,
                R.drawable.da_turn_roundabout_3,
                R.drawable.da_turn_roundabout_4,
                R.drawable.da_turn_roundabout_5,
                R.drawable.da_turn_roundabout_6,
                R.drawable.da_turn_roundabout_7,
                R.drawable.da_turn_roundabout_8,
                R.drawable.da_turn_roundabout_exit,
                R.drawable.da_turn_sharp_right,
                R.drawable.da_turn_slight_right,
                R.drawable.da_turn_straight,
                R.drawable.da_turn_unknown,
                R.drawable.da_turn_uturn);
    }
}
