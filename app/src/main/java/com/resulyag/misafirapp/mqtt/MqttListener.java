package com.resulyag.misafirapp.mqtt;

public interface MqttListener {
    void onMessage(String msg);
    void connState(boolean isConnected);
}
