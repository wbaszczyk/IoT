package com.iot.device.management.iot.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.iot.device.management.iot.Logger;
import com.iot.device.management.iot.R;
import com.iot.device.management.iot.coap.DeviceGetRequest;
import com.iot.device.management.iot.coap.client.CoapPostTask;
import com.iot.device.management.iot.coap.server.ServerService;
import com.iot.device.management.iot.device.DeviceProperties;
import com.iot.device.management.iot.device.ProximityDetectorNotification;
import com.squareup.picasso.Picasso;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by sneez on 22.04.16.
 */
public class DeviceDetailActivity  extends Activity implements SeekBar.OnSeekBarChangeListener {
    private String uuid;
    private Integer progressValue = 0;
    private String deviceCoapAddress;

    private ImageView ivDeviceCover;
    private TextView tvTitle;
    private TextView tvVersion;
    private TextView tvUuid;
    private TextView tvProximity;
    private TextView tvRGB;

    private SeekBar bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);


        ivDeviceCover = (ImageView) findViewById(R.id.ivDeviceCover);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvVersion = (TextView) findViewById(R.id.tvVersion);
        tvUuid = (TextView) findViewById(R.id.tvUuid);
        tvProximity = (TextView) findViewById(R.id.tvProximity);
        tvRGB = (TextView) findViewById(R.id.tvRGB);

        DeviceProperties device = (DeviceProperties) getIntent().getSerializableExtra(DeviceListActivity.BOOK_DETAIL_KEY);
        new CoapStatusTask().execute(device.getDeviceCoapAddress() + "status", "empty");

        this.setTitle(device.getTitle());
        Picasso.with(this).load(R.mipmap.device).into(ivDeviceCover);
        tvTitle.setText(device.getTitle());
        tvVersion.setText(device.getVersion());
        tvUuid.setText(device.getUuid());
        tvProximity.setText("Proximity: " + device.getProximity().toString());
        tvRGB.setText("RGB");

        uuid = device.getUuid();
        deviceCoapAddress = device.getDeviceCoapAddress();

        registerReceiver(broadcastReceiver, new IntentFilter(ServerService.BROADCAST_ACTION));

        Button startProximitybtn = (Button)findViewById(R.id.startProximitybtn);
        startProximitybtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CoapPostTask().execute(deviceCoapAddress + "proximity_start", "notify");
            }
        });
        Button stopProximitybtn = (Button)findViewById(R.id.stopProximitybtn);
        stopProximitybtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CoapPostTask().execute(deviceCoapAddress + "proximity_stop", "notify");
            }
        });
        Button calibratebtn = (Button)findViewById(R.id.calibratebtn);
        stopProximitybtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CoapPostTask().execute(deviceCoapAddress + "calibrate", "notify");
            }
        });
        bar = (SeekBar) findViewById(R.id.seekBarGreen);
        bar.setOnSeekBarChangeListener(this);
        bar = (SeekBar) findViewById(R.id.seekBarBlue);
        bar.setOnSeekBarChangeListener(this);
        bar = (SeekBar) findViewById(R.id.seekBarRed);
        bar.setOnSeekBarChangeListener(this);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_detail, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_share) {
//            setShareIntent();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ProximityDetectorNotification proximityDetectorNotification =
                    (ProximityDetectorNotification) intent.getExtras().getSerializable(ServerService.GET_PROXIMITY_DETECTOR_NOTIFICATION_KEY);
            if(proximityDetectorNotification != null){
                if(uuid.equals(proximityDetectorNotification.getUuid())) {
                    tvProximity.setText("Proximity: " + proximityDetectorNotification.getProximity().toString());
                }
            }
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        progressValue = i;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Integer val = (progressValue * 1023) / 100;
        if (seekBar.getId() == R.id.seekBarGreen)
            new CoapPostTask().execute(deviceCoapAddress + "set_g", val.toString());
        else if (seekBar.getId() == R.id.seekBarBlue)
            new CoapPostTask().execute(deviceCoapAddress + "set_b", val.toString());
        else if (seekBar.getId() == R.id.seekBarRed)
            new CoapPostTask().execute(deviceCoapAddress + "set_r", val.toString());
//                Toast.makeText(getActivity(), "Filed to get server handler", Toast.LENGTH_SHORT).show();

    }
    public class CoapStatusTask extends AsyncTask<String, String, CoapResponse> {

        private static final String TAG_R = "r";
        private static final String TAG_G = "g";
        private static final String TAG_B = "b";

        protected void onPreExecute() {
            ((TextView) findViewById(R.id.tvRGB)).setText("Loading...");
        }
        protected CoapResponse doInBackground(String... args) {
            CoapClient client = new CoapClient(args[0]);
            return client.post(args[1], MediaTypeRegistry.TEXT_PLAIN);
        }
        protected void onPostExecute(CoapResponse response) {
            if(response != null) {
                try {
                    JSONObject register = new JSONObject(response.getResponseText());
                    Integer r = register.getInt(TAG_R);
                    Integer g = register.getInt(TAG_G);
                    Integer b = register.getInt(TAG_B);
                    ((SeekBar) findViewById(R.id.seekBarRed)).setProgress((r*100)/1023);
                    ((SeekBar) findViewById(R.id.seekBarBlue)).setProgress((b*100)/1023);
                    ((SeekBar) findViewById(R.id.seekBarGreen)).setProgress((g*100)/1023);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else {
                ((TextView) findViewById(R.id.tvRGB)).setText("No response");
            }
        }
    }

}
