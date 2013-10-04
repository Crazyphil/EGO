package tk.crazysoft.ego;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class DataManagementActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_management_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
        String[] titles = getResources().getStringArray(R.array.data_management_import_titles);
        String[] descs = getResources().getStringArray(R.array.data_management_import_descriptions);
        for (int i = 0; i < titles.length; i++) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("line1", titles[i]);
            map.put("line2", descs[i]);
            items.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, items, R.layout.two_line_list_item, new String[] { "line1", "line2" }, new int[] { android.R.id.text1, android.R.id.text2 } );
        ListView view = (ListView)findViewById(R.id.data_listViewImport);
        view.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
