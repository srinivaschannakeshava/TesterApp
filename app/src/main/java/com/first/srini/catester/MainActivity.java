package com.first.srini.catester;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements LocationListener {
    String TAG = MainActivity.class.getSimpleName();
    /* GPS Constant Permission */
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
    TextView mDeviceDsn;
    TextView mUploadFrequency;
    TextView mSensorDataView;
    static TextView mPublishPayload;
    Button mSaveButton;
    Button mStartButton;
    Button mPingButton;
    LocationManager mLocationService;
    static Double mLattitude = 12.946332;
    static Double mLogitude = 77.605574;
    Toast mToast;
    String gpsProvider;
    public static String deviceDsn = "8934076379000745900";
    public static int uploadFreq = 20;
    MqttDataPublishScheduler mqttPublishScheduler;

    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
        }

        mDeviceDsn = (TextView) findViewById(R.id.device_dsn);
        mUploadFrequency = (TextView) findViewById(R.id.frequency_upload);
        mSensorDataView = (TextView) findViewById(R.id.sensor_data);
        mPublishPayload = (TextView) findViewById(R.id.publish_data);
        mPingButton = (Button) findViewById(R.id.button_ping);
        mSaveButton = (Button) findViewById(R.id.button_save);
        mStartButton = (Button) findViewById(R.id.button_start);
        mLocationService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnable = mLocationService.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnable) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        gpsProvider = mLocationService.getBestProvider(criteria, true);

        if (gpsProvider != null) {
            mLocationService.requestLocationUpdates(gpsProvider, 2000, 1, this);
            Toast.makeText(this, "Best Provider is " + gpsProvider, Toast.LENGTH_LONG).show();
        }
        mqttPublishScheduler = new MqttDataPublishScheduler(this);

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Button Click Save");
                Log.d(TAG, mDeviceDsn.getText().toString());
                Log.d(TAG, mUploadFrequency.getText().toString());
                if (mUploadFrequency.getText() != null && !mUploadFrequency.getText().toString().matches("")) {
                    uploadFreq = Integer.parseInt(mUploadFrequency.getText().toString());
                }
                if (mDeviceDsn.getText() != null && !mDeviceDsn.getText().toString().matches("")) {
                    deviceDsn = mDeviceDsn.getText().toString();

                }
            }
        });
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Button Click Start");
//                startAlarm(view);
                mqttPublishScheduler.startMqttPublish();

            }
        });

        mPingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Ping Button Clicked");
                mqttPublishScheduler.publishMessage();
            }
        });

       /* Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);*/

    }

  /*  @Override
    protected void onResume() {
        *//*if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },
                    MY_PERMISSION_ACCESS_COARSE_LOCATION );
        }
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION  },
                    MY_PERMISSION_ACCESS_FINE_LOCATION );
        }
        super.onResume();
        mLocationService.requestLocationUpdates(gpsProvider,1000,10,this);*//*

    }*/

    @Override
    public void onLocationChanged(Location location) {
        mLattitude = location.getLatitude();
        mLogitude = location.getLongitude();
        mSensorDataView.setText("");
        mSensorDataView.append(location.getProvider() + "\n");
        mSensorDataView.append("Lat :" + location.getLatitude() + "\n");
        mSensorDataView.append("Lon :" + location.getLongitude() + "\n");
        DisplayToast(mLattitude + " : " + mLogitude);
        Log.d(TAG, location.toString());

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
        Toast.makeText(this, "Enabled new provider " + s,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(this, "Disabled provider " + s,
                Toast.LENGTH_SHORT).show();
    }

    public void DisplayToast(String message) {
        if (mToast != null ) {
            mToast.cancel();
        }
        mToast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public void appendDataToSensorView(String[] sensorData) {
        mSensorDataView.setText("");
        for (String data : sensorData) {
            mSensorDataView.append(data + "\n\n\n");
        }
    }


   /* public void startAlarm(View view) {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 10000, pendingIntent);
        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
    }*/

}
