package tk.crazysoft.ego;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import tk.crazysoft.ego.io.ExternalStorage;
import tk.crazysoft.ego.services.DataImportReceiver;
import tk.crazysoft.ego.services.DataImportService;

public class DataManagementActivity extends ActionBarActivity {
    private DataImportReceiver importReceiver;
    private ProgressBar progressBar;

    private static final int ITEM_IMPORT_ADDRESSES = 0;
    private static final int ITEM_IMPORT_HOSPITAL_ADMITTANCES = 1;
    private static final int ITEM_IMPORT_DOCTOR_STANDBY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.data_management_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView listViewImport = (ListView) findViewById(R.id.data_listViewImport);
        listViewImport.setOnItemClickListener(new ImportListOnItemClickListener());

        progressBar = (ProgressBar)findViewById(R.id.data_progressBar);

        IntentFilter importFilter = new IntentFilter(DataImportService.BROADCAST_ERROR);
        importFilter.addAction(DataImportService.BROADCAST_PROGRESS);
        importFilter.addAction(DataImportService.BROADCAST_RESULT_IMPORT);
        importFilter.addAction(DataImportService.BROADCAST_RESULT_MERGE);
        importReceiver = new DataImportReceiver(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(importReceiver, importFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        String sdPath = ExternalStorage.getSdCardPath();
        if (sdPath == null) {
            sdPath = ExternalStorage.getSdCardPath(true);
        }

        ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
        String[] titles = getResources().getStringArray(R.array.data_management_import_titles);
        String[] descs = getResources().getStringArray(R.array.data_management_import_descriptions);
        for (int i = 0; i < titles.length; i++) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("line1", titles[i]);
            map.put("line2", String.format(descs[i], sdPath));
            items.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, items, R.layout.two_line_list_item, new String[] { "line1", "line2" }, new int[] { android.R.id.text1, android.R.id.text2 } );
        ListView view = (ListView)findViewById(R.id.data_listViewImport);
        view.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(importReceiver);
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

    public void setManagementProgressBarVisibility(boolean visible) {
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setManagementProgress(int progress) {
        progressBar.setProgress(progress);
    }

    private class ImportListOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TextView title = (TextView)view.findViewById(android.R.id.text1);
            String[] titles = getResources().getStringArray(R.array.data_management_import_titles);
            String action;
            if (title.getText().equals(titles[ITEM_IMPORT_ADDRESSES])) {
                action = DataImportService.ACTION_IMPORT_ADDRESSES;
            }
            else if (title.getText().equals(titles[ITEM_IMPORT_HOSPITAL_ADMITTANCES])) {
                action = DataImportService.ACTION_IMPORT_HOSPITAL_ADMITTANCES;
            }
            else if (title.getText().equals(titles[ITEM_IMPORT_DOCTOR_STANDBY])) {
                action = DataImportService.ACTION_IMPORT_DOCTOR_STANDBY;
            }
            else {
                throw new IllegalArgumentException("Import action not defined");
            }

            importReceiver.startServiceIntent(action);
        }
    }
}
