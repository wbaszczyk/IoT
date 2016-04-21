package com.iot.device.management.iot;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.iot.device.management.iot.coap.server.ServerService;
import com.iot.device.management.iot.device.DeviceProperties;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sneez on 21.04.16.
 */
public class RegisterDevices extends ListActivity  implements RegisterDeviceCallback {
    private ServerService serverService;
    private boolean bound = false;

    private static Map<String, DeviceProperties> devicePropertiesMap = new HashMap<>();
    static ArrayList<String> listItems=new ArrayList<String>();
    static ArrayAdapter<String> adapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.content_management);
        adapter=new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);

        startService(new Intent(this, ServerService.class));
    }
    @Override
    protected void onStart() {
        super.onStart();
        // bind to Service
        Intent intent = new Intent(this, ServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            ServerService.LocalBinder binder = (ServerService.LocalBinder) service;
            serverService = binder.getService();
            bound = true;
            serverService.setRegisterDeviceCallback(RegisterDevices.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    @Override
    public void registerDevice(String uuid, DeviceProperties deviceProperties) {
        Logger.appendLog("REGISTERED " + deviceProperties.toString());

        devicePropertiesMap.put(uuid, deviceProperties);
        listItems.add("IP: " + deviceProperties.getIpAddress());
        adapter.notifyDataSetChanged();
        finish();
        startActivity(getIntent());
        new CoapGetTask().execute(deviceProperties.getDeviceCoapAddress() + "uuid", uuid);
    }

    @Override
    public void notifyAboutProximityDetector(String uuid, Integer proximity) {
        Logger.appendLog("PROXIMITY " + proximity);
        listItems.add(", proximity: " + proximity);
        adapter.notifyDataSetChanged();
        finish();
        startActivity(getIntent());
    }

    class CoapGetTask extends AsyncTask<String, String, CoapResponse> {

        protected void onPreExecute() {
            // Loading...
        }

        protected CoapResponse doInBackground(String... args) {
            CoapClient client = new CoapClient(args[0]);
            return client.post(args[1], MediaTypeRegistry.TEXT_PLAIN);
        }

        protected void onPostExecute(CoapResponse response) {
            // OK
        }
    }
}