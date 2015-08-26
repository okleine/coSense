package de.uzl.itm.ncoap.android.server;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.communication.dispatching.server.NotFoundHandler;


public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener,
        SettingsDialog.SettingsDialogListener, StartRegistrationDialog.Listener{

    private Handler handler = new Handler();

    private NetworkStateReceiver networkStateReceiver;

    //Audio Sampling
    private AudioRecord audioRecord;
    private static int sampleRate = 44100;
    private int bufferSize;
    private AudioSamplingTask samplingTask;

    //Sensors
    private SensorManager sensorManager;
    private LightSensorEventListener lightSensorListener;
    private PressureSensorEventListener pressureSensorListener;

    //GPS
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double latitude = Double.POSITIVE_INFINITY;
    private double longitude = Double.POSITIVE_INFINITY;

    //View Elements
    private RadioGroup radGroupServer;
    private TextView txtIP;
    private TextView txtProxy;

    private RadioGroup radGroupLocation;
    private RadioButton radLocationOff;
    private EditText txtLatitude;
    private EditText txtLongitude;

    private RadioGroup radGroupNoise;
    private RadioButton radNoiseOff;

    private EditText txtNoise;
    private ProgressBar prbNoise;

    private RadioGroup radGroupLight;
    private RadioButton radLightOff;
    private EditText txtLight;
    private ProgressBar prbLight;

    private RadioGroup radGroupPressure;
    private RadioButton radPressureOff;
    private EditText txtPressure;
    private ProgressBar prbPressure;

    private CoapServerApplication serverApplication;
    private LightSensorService lightSensorService;
    private LocationService locationService;
    private NoiseSensorService noiseSensorService;
    private PressureSensorService pressureSensorService;

    private SettingsDialog settingsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set view elements
        this.radGroupServer = (RadioGroup) findViewById(R.id.radgroup_server);
        this.txtIP = (TextView) findViewById(R.id.ip_txt);
        this.txtProxy = (TextView) findViewById(R.id.ssp_txt);

        this.radGroupLocation = (RadioGroup) findViewById(R.id.radgroup_gps);
        this.radLocationOff = (RadioButton) findViewById(R.id.rad_gps_off);
        this.txtLatitude = (EditText) findViewById(R.id.lat_txt);
        this.txtLongitude = (EditText) findViewById(R.id.long_txt);

        this.radGroupNoise = (RadioGroup) findViewById(R.id.radgroup_noise);
        this.radNoiseOff = (RadioButton) findViewById(R.id.rad_noise_off);
        this.txtNoise = (EditText) findViewById(R.id.noise_txt);
        this.prbNoise = (ProgressBar) findViewById(R.id.noise_prb);
        this.prbNoise.setMax(32768);

        this.radGroupLight = (RadioGroup) findViewById(R.id.radgroup_light);
        this.radLightOff = (RadioButton) findViewById(R.id.rad_light_off);
        this.txtLight = (EditText) findViewById(R.id.light_txt);
        this.prbLight = (ProgressBar) findViewById(R.id.light_prb);
        this.prbLight.setMax((int) Math.log(200000));

        this.radGroupPressure = (RadioGroup) findViewById(R.id.radgroup_pressure);
        this.radPressureOff = (RadioButton) findViewById(R.id.rad_pressure_off);
        this.txtPressure = (EditText) findViewById(R.id.pressure_txt);
        this.prbPressure = (ProgressBar) findViewById(R.id.pressure_prb);
        this.prbPressure.setMax(100);

        this.networkStateReceiver = new NetworkStateReceiver(this);
        registerReceiver(networkStateReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        this.settingsDialog = null;
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Server
        this.radGroupServer.setOnCheckedChangeListener(this);

        //Sensor Manager (for sensors)
        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Location Manager (for GPS)
        this.locationListener = new MyLocationListener();
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //Noise sampling
        this.bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);

        //Register for RadioGroup "Location"
        this.radGroupLocation.setOnCheckedChangeListener(this);

        //Register for RadioGroup "Noise"
        this.radGroupNoise.setOnCheckedChangeListener(this);

        //Register for RadioGroup "Light"
        this.radGroupLight.setOnCheckedChangeListener(this);

        //Register for RadioGroup "Light"
        this.radGroupPressure.setOnCheckedChangeListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.ssp_settings) {
            if(this.settingsDialog == null){
                this.settingsDialog = new SettingsDialog();
            }

            this.settingsDialog.show(getFragmentManager(), null);
            return true;
        }

        if(id == R.id.ssp_registration){
            new StartRegistrationDialog().show(getFragmentManager(), null);
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(networkStateReceiver);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        //Server
        if(group.getId() == R.id.radgroup_server){
            if(checkedId == R.id.rad_server_on){
                this.serverApplication = new CoapServerApplication(NotFoundHandler.getDefault());
            }
            else{
                this.serverApplication.shutdown();
                this.serverApplication = null;

                //Disable Light Service
                if(!this.radLightOff.isChecked()) {
                    this.radLightOff.setChecked(true);
                }

                //Disable Location Service
                if(!this.radLocationOff.isChecked()){
                    this.radLocationOff.setChecked(true);
                }

                //Disable Noise Service
                if(!this.radNoiseOff.isChecked()){
                    this.radNoiseOff.setChecked(true);
                }

                //Disable Pressure Service
                if(!this.radPressureOff.isChecked()){
                    this.radPressureOff.setChecked(true);
                }
            }
        }

        //Location
        else if(group.getId() == R.id.radgroup_gps){
            if(checkedId == R.id.rad_gps_on){
                this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0,
                        this.locationListener);

                //Create Location Web Service
                if(this.serverApplication != null) {
                    LocationValue initialStatus = new LocationValue(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, null);
                    this.locationService = new LocationService("/location", initialStatus, serverApplication.getExecutor());
                    this.serverApplication.registerService(this.locationService);
                }
                else{
                    this.radLocationOff.setChecked(true);
                    Toast.makeText(this, "Server is not running!", Toast.LENGTH_LONG).show();
                }
            }
            else{
                this.locationManager.removeUpdates(this.locationListener);
                this.txtLatitude.setText("");
                this.txtLongitude.setText("");

                if(this.locationService != null) {
                    this.locationService.shutdown();
                    this.locationService = null;
                }
            }
        }
        //Noise
        else if(group.getId() == R.id.radgroup_noise) {
            if (checkedId == R.id.rad_noise_on) {
                this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                this.audioRecord.startRecording();
                this.samplingTask = new AudioSamplingTask();
                this.handler.post(this.samplingTask);

                //Register Web Service
                if(this.serverApplication != null) {
                    NoiseSensorValue initialStatus = new NoiseSensorValue(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Integer.MIN_VALUE);
                    this.noiseSensorService = new NoiseSensorService("/noise", initialStatus, serverApplication.getExecutor());
                    this.serverApplication.registerService(this.noiseSensorService);
                }
                else{
                    this.radNoiseOff.setChecked(true);
                    Toast.makeText(this, "Server is not running!", Toast.LENGTH_LONG).show();
                }

            } else {
                if (this.audioRecord != null) {
                    this.audioRecord.release();
                    this.audioRecord = null;
                    this.handler.removeCallbacks(samplingTask);
                }
                txtNoise.setText("");
                prbNoise.setProgress(0);
            }
        }

        //Light
        else if(group.getId() == R.id.radgroup_light){
            if(checkedId == R.id.rad_light_on){
                //Check if there is a light sensor available
                Sensor lightSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                if(lightSensor == null){
                    this.radLightOff.setChecked(true);
                    Toast.makeText(this, "No light sensor available!", Toast.LENGTH_LONG).show();
                }
                else {
                    //create light sensor listener and register at sensor manager
                    this.lightSensorListener = new LightSensorEventListener();
                    this.sensorManager.registerListener(this.lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

                    //Create Light Web Service
                    if (this.serverApplication != null) {
                        LightSensorValue initialStatus = new LightSensorValue(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
                        this.lightSensorService = new LightSensorService("/light", initialStatus, serverApplication.getExecutor());
                        this.serverApplication.registerService(this.lightSensorService);
                    } else {
                        this.radLightOff.setChecked(true);
                        Toast.makeText(this, "Server is not running!", Toast.LENGTH_LONG).show();
                    }
                }
            }
            else{
                sensorManager.unregisterListener(this.lightSensorListener);
                this.txtLight.setText("");
                this.prbLight.setProgress(0);
                if(this.lightSensorService != null) {
                    this.lightSensorService.shutdown();
                    this.lightSensorService = null;
                }
            }
        }

        //Pressure
        else if(group.getId() == R.id.radgroup_pressure){
            if(checkedId == R.id.rad_pressure_on){

                Sensor pressureSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

                if(pressureSensor == null){
                    this.radPressureOff.setChecked(true);
                    Toast.makeText(this, "No pressure sensor available!", Toast.LENGTH_LONG).show();
                }
                else {
                    this.pressureSensorListener = new PressureSensorEventListener();
                    this.sensorManager.registerListener(this.pressureSensorListener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);

                    //Create Pressure Web Service
                    if (this.serverApplication != null) {
                        PressureSensorValue initialStatus = new PressureSensorValue(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
                        this.pressureSensorService = new PressureSensorService("/pressure", initialStatus, serverApplication.getExecutor());
                        this.serverApplication.registerService(this.pressureSensorService);
                    } else {
                        this.radPressureOff.setChecked(true);
                        Toast.makeText(this, "Server is not running!", Toast.LENGTH_LONG).show();
                    }
                }
            }
            else{
                sensorManager.unregisterListener(this.pressureSensorListener);
                this.txtPressure.setText("");
                this.prbPressure.setProgress(0);

                if(this.pressureSensorService != null) {
                    this.pressureSensorService.shutdown();
                    this.pressureSensorService = null;
                }
            }
        }
    }

    @Override
    public void onProxyChanged(String sspHost) {
        new AddressResolutionTask().execute(sspHost);
    }


    @Override
    public void registerAtProxy() {
        Toast.makeText(this, "Registration not yet implemented!", Toast.LENGTH_LONG).show();
    }

    private class AddressResolutionTask extends AsyncTask<String, Void, Void>{

        @Override
        public Void doInBackground(String... params) {
            try {
                final InetAddress sspAddress = InetAddress.getByName(params[0]);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtProxy.setText(sspAddress.getHostAddress());
                    }
                });

            }
            catch(final UnknownHostException ex){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

            }

            return null;
        }
    }

    /**
     * Class to sample audio (peak amplitude) from the mic every 200ms
     */
    private class AudioSamplingTask implements Runnable{

        @Override
        public void run() {
            try {
                short[] buffer = new short[bufferSize];
                int bufferReadResult;
                int highestLevel = 0;

                bufferReadResult = audioRecord.read(buffer, 0, bufferSize);

                for (int i = 0; i < bufferReadResult; i++) {
                    if (buffer[i] > highestLevel) {
                        highestLevel = buffer[i];
                    }
                }

                final int finalHighestLevel = highestLevel;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtNoise.setText("" + finalHighestLevel);
                        prbNoise.setProgress(finalHighestLevel);

                        noiseSensorService.setNoiseValue(new NoiseSensorValue(latitude, longitude, finalHighestLevel));
                    }
                });

                handler.postDelayed(this, 200);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*----------Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            txtLatitude.setText("" + location.getLatitude());
            txtLongitude.setText("" + location.getLongitude());

            if(locationService != null) {
                locationService.setResourceStatus(new LocationValue(latitude, longitude, null), 5);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
    }

    private class NetworkStateReceiver extends BroadcastReceiver {

        private ConnectivityManager connectivityManager;
        private WifiManager wifiManager;

        public NetworkStateReceiver(Activity activity){
            this.connectivityManager = (ConnectivityManager) activity.getSystemService(CONNECTIVITY_SERVICE);
            this.wifiManager = (WifiManager) activity.getSystemService(WIFI_SERVICE);
        }

        public void onReceive(Context context, Intent intent) {
            Log.d("app", "Network connectivity change");

            int ip = wifiManager.getConnectionInfo().getIpAddress();
            String ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
            Log.d("app", "IP: " + ipAddress);
            txtIP.setText(ipAddress);
        }
    }

    private class LightSensorEventListener implements SensorEventListener{

        @Override
        public void onSensorChanged(final SensorEvent event) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtLight.setText("" + event.values[0]);
                    prbLight.setProgress((int) Math.log(event.values[0]));

                    if(lightSensorService != null) {
                        lightSensorService.setLightValue(new LightSensorValue(latitude, longitude, (double) event.values[0]));
                    }
                }
            });
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //TODO but I don't know what that means
        }
    }

    private class PressureSensorEventListener implements SensorEventListener{

        @Override
        public void onSensorChanged(final SensorEvent event) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtPressure.setText("" + event.values[0]);
                    prbPressure.setProgress((int) event.values[0] - 950);

                    if(pressureSensorService != null) {
                        pressureSensorService.setPressureValue(new PressureSensorValue(latitude, longitude, (double) event.values[0]));
                    }
                }
            });
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //TODO but I don't know what that means
        }
    }

}

