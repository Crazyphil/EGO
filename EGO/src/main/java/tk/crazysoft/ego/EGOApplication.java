package tk.crazysoft.ego;

import android.support.multidex.MultiDexApplication;
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
public class EGOApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            // The following line triggers the initialization of ACRA
            ACRA.init(this);
        }
    }
}
