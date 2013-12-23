package tk.crazysoft.ego;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class HouseActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.house_activity);

        if (savedInstanceState == null) {
            HouseFragment fragment = new HouseFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.house_fragmentContainer, fragment).commit();
        }
    }

}
