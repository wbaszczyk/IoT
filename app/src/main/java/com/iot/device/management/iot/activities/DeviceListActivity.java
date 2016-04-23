package com.iot.device.management.iot.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.iot.device.management.iot.Logger;
import com.iot.device.management.iot.R;
import com.iot.device.management.iot.adapters.DeviceAdapter;
import com.iot.device.management.iot.coap.client.CoapPostTask;
import com.iot.device.management.iot.coap.server.ServerService;
import com.iot.device.management.iot.device.DeviceProperties;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sneez on 22.04.16.
 */
public class DeviceListActivity extends Activity {
    public static final String BOOK_DETAIL_KEY = "book";
    private ListView lvDevices;
    private DeviceAdapter deviceAdapter;
    ArrayList<DeviceProperties> devices = new ArrayList<>();

    private Map<String, DeviceProperties> devicePropertiesMap = new HashMap<>();
    private Map<String, String> ipDevices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        deviceAdapter = new DeviceAdapter(this, devices);
        lvDevices.setAdapter(deviceAdapter);
        setupBookSelectedListener();

        startService(new Intent(this, ServerService.class));
        registerReceiver(broadcastReceiver, new IntentFilter(ServerService.BROADCAST_ACTION));

//        registerDevice(new DeviceProperties("asd", "dsaa", "ver", "1.1.1.1", 123, "mac"));
    }

    private void registerDevice(DeviceProperties deviceProperties){
        if(deviceProperties != null) {
            if (ipDevices.containsKey(deviceProperties.getIpAddress())) {
                devicePropertiesMap.remove(ipDevices.get(deviceProperties.getIpAddress()));
            }
            String uuid = deviceProperties.getUuid();
            ipDevices.put(deviceProperties.getIpAddress(), uuid);
            devicePropertiesMap.put(uuid, deviceProperties);

            devices.clear();
            for(Map.Entry<String, DeviceProperties> entry: devicePropertiesMap.entrySet()) {
                devices.add(entry.getValue());
            }
            deviceAdapter.notifyDataSetChanged();

            new CoapPostTask().execute(deviceProperties.getDeviceCoapAddress() + "uuid", uuid);

        }
    }
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DeviceProperties deviceProperties = (DeviceProperties) intent.getExtras().getSerializable(ServerService.GET_DEVICE_DESCRIPTION_KEY);
            registerDevice(deviceProperties);
        }
    };
    public void setupBookSelectedListener() {
        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Launch the detail view passing book as an extra
            Intent intent = new Intent(DeviceListActivity.this, DeviceDetailActivity.class);
            intent.putExtra(BOOK_DETAIL_KEY, deviceAdapter.getItem(position));
            startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                // Fetch the data remotely
//                fetchBooks(query);
//                // Reset SearchView
//                searchView.clearFocus();
//                searchView.setQuery("", false);
//                searchView.setIconified(true);
//                searchItem.collapseActionView();
//                // Set activity title to search query
//                BookListActivity.this.setTitle(query);
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String s) {
//                return false;
//            }
//        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
