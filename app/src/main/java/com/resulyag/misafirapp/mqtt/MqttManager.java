package com.resulyag.misafirapp.mqtt;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MqttManager {
    private final String TAG = "MP_MqttManager";

    final private String PUB_TOPIC = "esp-r";
    final private String SUB_TOPIC = "esp-s";

    final String host = "tcp://143.198.71.210:1883";

    MqttAndroidClient mqttAndroidClient;
    MqttListener listener;

    public MqttManager(Context context, final MqttListener mListener) {

        listener = mListener;
        String clientId = "mobil" + new Random().nextInt(100000);

        mqttAndroidClient = new MqttAndroidClient(context, host, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "connection lost");
                listener.connState(false);
                connectionState = false;
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                connectionState = true;
                String receivedMsg = new String(message.getPayload());

                Log.i(TAG, "topic: " + topic + ", msg: " + receivedMsg);

                if (topic.equals(SUB_TOPIC)) {
                    listener.onMessage(receivedMsg);
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG, "msg delivered");
            }
        });

        connectToMqtt();

        checkConnectionTimer();
    }

    boolean connectionState = true;
    private void checkConnectionTimer(){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(!connectionState) {
                    Log.e(TAG,"checkConnectionTimer bağlı değil yeniden deneniyor.");
                    connectToMqtt();
                } else
                    Log.i(TAG,"checkConnectionTimer zaten bağlı.");
            }
        },5000,3000);
    }

    private void connectToMqtt() {

        String userName = "kekstra";
        String passWord = "Resul81Yag";

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(userName);
        mqttConnectOptions.setPassword(passWord.toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "connect succeed");
                    listener.connState(true);
                    connectionState = true;
                    subscribeTopic(SUB_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "connect failed");
                    listener.connState(false);
                    connectionState = false;
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
            listener.connState(false);
            connectionState = false;
        }
    }

    public void subscribeTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "subscribed succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "subscribed failed");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向默认的主题/user/update发布消息
     *
     * @param payload 消息载荷
     */
    public void publishMessage(String payload) {
        try {
            if (!mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(PUB_TOPIC, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "publish succeed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "publish failed!");
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

}
