package tk.crazysoft.ego;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class HouseActivity extends ActionBarActivity {
    public static final int RESULT_LAYOUT_MODE_CHANGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getString(R.string.layout_mode).equals(MainActivity.LAYOUT_MODE_LANDSCAPE_LARGE)) {
            setResult(RESULT_LAYOUT_MODE_CHANGE);
            finish();
            return;
        }

        setContentView(R.layout.house_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            HouseFragment fragment = new HouseFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.house_fragmentContainer, fragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
