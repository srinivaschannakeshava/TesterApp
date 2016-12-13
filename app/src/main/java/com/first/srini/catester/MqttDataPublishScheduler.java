package com.first.srini.catester;

import android.content.Context;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Created by srini on 12/13/2016.
 */

public class MqttDataPublishScheduler {
    String TAG = MqttDataPublishScheduler.class.getSimpleName();
    ScheduledExecutorService scheduleTaskExecutor;
    ScheduledFuture<?> scheduleFuture;
    MqttAndroidClient mqttAndroidClient;
    private Context appContext;

    public MqttDataPublishScheduler(Context context) {
        appContext = context;
    }

    final String serverUri = "";

    final String clientId = "ExampleAndroidClient";
    final String subscriptionTopic = "";
    final String publishTopic = "";


    public void mqttConnect() {
        if (mqttAndroidClient == null || !mqttAndroidClient.isConnected()) {
            mqttAndroidClient = new MqttAndroidClient(appContext, serverUri, clientId);
            mqttAndroidClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Toast.makeText(appContext, "Mqtt Connection Lost", Toast.LENGTH_SHORT);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    MainActivity.mPublishPayload.append("Message Received : " + message.toString() + "\n");

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    MainActivity.mPublishPayload.setText("");
                    try {
                        MainActivity.mPublishPayload.append("Message published : " + token.getMessage().toString() + "\n");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

            });
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(true);
            try {
                mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Toast.makeText(appContext, "Mqtt Connection successfull", Toast.LENGTH_SHORT);
                        subscribeToTopic();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Toast.makeText(appContext, "Mqtt Connection Failure", Toast.LENGTH_LONG);

                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

    public void publishMessage() {


        try {
            Time time = new Time();
            time.setToNow();

            JSONObject pubData = new JSONObject().put("lis", 1).put("dtt", time.toMillis(false)).put("btc", 97).put("lat", MainActivity.mLattitude).put("lon", MainActivity.mLogitude);

            MqttMessage message = new MqttMessage();

            message.setPayload(pubData.toString().getBytes());
            if (mqttAndroidClient != null) {
                if (mqttAndroidClient.isConnected()) {
                    mqttAndroidClient.publish(publishTopic + MainActivity.deviceDsn, message);
                } else {
                    mqttConnect();
                    mqttAndroidClient.publish(publishTopic + MainActivity.deviceDsn, message);
                }
            } else {
                mqttConnect();
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
//            e.printStackTrace();
        }
    }

    public void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic + MainActivity.deviceDsn, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(appContext, "Mqtt Subscription successfull", Toast.LENGTH_SHORT);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(appContext, "Mqtt Subscription Failure", Toast.LENGTH_SHORT);

                }
            });


        } catch (MqttException ex) {
            Log.d(TAG, ex.getMessage());
//            ex.printStackTrace();
        }
    }

    public void startMqttPublish() {
        mqttConnect();
        if(scheduleFuture!=null ){
            scheduleFuture.cancel(true);
        }
        if(scheduleTaskExecutor!=null && !scheduleTaskExecutor.isShutdown()){
            scheduleTaskExecutor.shutdownNow();
        }
        scheduleTaskExecutor = Executors.newScheduledThreadPool(1);
        scheduleFuture=scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                publishMessage();
            }
        }, 10, MainActivity.uploadFreq, TimeUnit.SECONDS);
    }


}
