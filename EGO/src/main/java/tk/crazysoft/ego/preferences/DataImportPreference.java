package tk.crazysoft.ego.preferences;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

import tk.crazysoft.ego.R;
import tk.crazysoft.ego.services.DataImportReceiver;

public class DataImportPreference extends Preference implements Preference.OnPreferenceClickListener {
    private DataImportReceiver receiver;
    private ProgressBar progressBar;
    private boolean registerProgressBar;

    public DataImportPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setWidgetLayoutResource(R.layout.progress_bar_widget);
        setOnPreferenceClickListener(this);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        progressBar = holder.itemView.findViewById(R.id.progressBar);
        if (registerProgressBar) {
            progressBar.setVisibility(View.VISIBLE);
            receiver.registerProgressBar(getIntent().getAction(), progressBar);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current setting value
        if (progressBar != null) {
            myState.value = progressBar.getVisibility() != View.GONE;
        } else {
            myState.value = false;
        }
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState)state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Set this Preference's widget to reflect the restored state
        registerProgressBar = myState.value;
    }

    public void setBroadcastReceiver(DataImportReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (receiver != null) {
            if (receiver.startServiceIntent(getContext(), getIntent().getAction(), progressBar)) {
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            }
        }
        return true;
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        boolean value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeInt(value ? 1 : 0);
        }

        // Standard creator object using an instance of this class
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
