package com.resulyag.misafirapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.resulyag.misafirapp.mqtt.MqttListener;
import com.resulyag.misafirapp.mqtt.MqttManager;

import pl.droidsonroids.gif.GifImageView;

import static com.resulyag.misafirapp.notification.MyFirebaseMessagingService.sendRegistrationToServer;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MP_MainActivity";

    MqttManager mqttManager;

    private TextView txt_misafirdurum;
    private TextView txt_kapidurum;
    private TextView txt_alarmdurum;
    private Button btn_open;
    private Button btn_alarm;
    private ImageView imgVw_connState;
    private GifImageView gif_image_zil;
    private AlphaAnimation txtAnimation;
    private void init(){
        txt_misafirdurum = findViewById(R.id.txt_misafirdurum);
        txt_kapidurum = findViewById(R.id.txt_kapidurum);
        txt_alarmdurum = findViewById(R.id.txt_alarmdurum);
        btn_open = findViewById(R.id.btn_opendoor);
        btn_alarm = findViewById(R.id.btn_soundalarm);
        imgVw_connState = findViewById(R.id.imgVw_connState);
        gif_image_zil = findViewById(R.id.gif_zil);
        initAnimation();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mqttManager.publishMessage("201");
            }
        });

        btn_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mqttManager.publishMessage("301");
            }
        });

        runtimeEnableAutoInit();

        initMqtt();

        /*
        Button pubButton = findViewById(R.id.publish);
        pubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage("hello IoT");
            }
        });
        */
    }

    public void runtimeEnableAutoInit() {
        // [START fcm_runtime_enable_auto_init]
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        // [END fcm_runtime_enable_auto_init]

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        if(token != null && !token.isEmpty())
                            sendRegistrationToServer(token);

                        // Log and toast
                        //String msg = getString(R.string.msg_token_fmt, token);
                        Log.i(TAG, "getToken: " + token);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean ignoreThis = true;
    private void initMqtt(){

        MqttListener listener = new MqttListener() {
            @Override
            public void onMessage(String msg) {
                if (ignoreThis){
                    ignoreThis = false;
                    return;
                }

                if (msg.equals("101")){ //misafir geldi

                    txt_misafirdurum.setText("Misafir Geldi!");
                    txt_misafirdurum.setAlpha(1f);
                    txt_misafirdurum.startAnimation(txtAnimation);

                    txt_kapidurum.setText("Kapı Kapalı");
                    txt_kapidurum.clearAnimation();

                    txt_alarmdurum.setVisibility(View.INVISIBLE);
                    txt_alarmdurum.clearAnimation();
                    gif_image_zil.setVisibility(View.INVISIBLE);
                } else if (msg.equals("202")){   // kapı aç komutuna karşın kapı açıldı mesajı geldiğinde
                    txt_misafirdurum.setText("Misafir Yok!");
                    txt_misafirdurum.clearAnimation();

                    txt_kapidurum.setText("Kapı Açıldı!");
                    txt_kapidurum.setAlpha(1f);
                    txt_kapidurum.startAnimation(txtAnimation);

                    txt_alarmdurum.setVisibility(View.INVISIBLE);
                    txt_alarmdurum.clearAnimation();
                    gif_image_zil.setVisibility(View.INVISIBLE);
                                    }
                else if(msg.equals("302")){  // alarm çal komutuna karşılık olarak alarm çalıyor mesajı geldiğinde
                    txt_misafirdurum.setText("Misafir Yok!");
                    txt_misafirdurum.clearAnimation();

                    txt_kapidurum.setText("Kapı Kapalı");
                    txt_kapidurum.clearAnimation();

                    txt_alarmdurum.setVisibility(View.VISIBLE);
                    txt_alarmdurum.setAlpha(1f);
                    txt_alarmdurum.startAnimation(txtAnimation);

                    gif_image_zil.setVisibility(View.VISIBLE);

                }
                else if(msg.equals("102")){ // açılan animasyonların  durması için genel bir mesaj, açık olan animasyon durması için 10 saniye sonra arduino tarafından gönderilecek
                    txt_misafirdurum.setText("Misafir Yok!");
                    txt_misafirdurum.clearAnimation();

                    txt_kapidurum.setText("Kapı Kapalı");
                    txt_kapidurum.clearAnimation();

                    txt_alarmdurum.setVisibility(View.INVISIBLE);
                    txt_alarmdurum.clearAnimation();
                    gif_image_zil.setVisibility(View.INVISIBLE);
                    mqttManager.publishMessage("103");
                }
            }

            @Override
            public void connState(boolean isConnected) {
                if (isConnected){
                    imgVw_connState.setImageResource(R.drawable.mqttconn);
                } else {
                    imgVw_connState.setImageResource(R.drawable.connlost);
                }
            }
        };

        mqttManager = new MqttManager(getApplicationContext(), listener);

    }

    private void initAnimation(){
        txtAnimation = new AlphaAnimation(0.2f, 1.0f);
        txtAnimation.setDuration(700);
        txtAnimation.setRepeatCount(Animation.INFINITE);
        txtAnimation.setRepeatMode(Animation.REVERSE);
    }

}
