package com.example.grocery.activities;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.grocery.thumucquantrong.Constants;
import com.example.phinh.grocery.R;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsActivity extends AppCompatActivity {

    private TextView notificationStatusTv;

    private static final String enabledMessage = "Thông báo được bật";
    private static final String disabledMessage = "Thông báo bị tắt";

    private SharedPreferences sp;
    private SharedPreferences.Editor spEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //init ui views
        //ui views
        SwitchCompat fcmSwitch = findViewById(R.id.fcmSwitch);
        notificationStatusTv = findViewById(R.id.notificationStatusTv);
        ImageButton backBtn = findViewById(R.id.backBtn);

        //init shared preferences
        sp = getSharedPreferences("SETTINGS_SP", MODE_PRIVATE);
        //check last selected option; true/false
        boolean isChecked1 = sp.getBoolean("FCM_ENABLED", false);
        fcmSwitch.setChecked(isChecked1);
        if (isChecked1){
            //was enabled
            notificationStatusTv.setText(enabledMessage);
        }
        else {
            //was disabled
            notificationStatusTv.setText(disabledMessage);
        }

        //handle click; goback
        backBtn.setOnClickListener(view -> onBackPressed());

        //add switch check change listener to enable disable notifications
        fcmSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                //checked, enable notifications
                subscribeToTopic();
            } else {
                //unchecked, disable notifications
                unSubscribeToTopic();
            }
        });

    }

    private void subscribeToTopic(){
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.FCM_TOPIC)
                .addOnSuccessListener(aVoid -> {
                    //subscribed successfully
                    //save setting ins shared preferences
                    spEditor = sp.edit();
                    spEditor.putBoolean("FCM_ENABLED", true);
                    spEditor.apply();

                    Toast.makeText(SettingsActivity.this, ""+enabledMessage, Toast.LENGTH_SHORT).show();
                    notificationStatusTv.setText(enabledMessage);
                })
                .addOnFailureListener(e -> {
                    //failed subscribing
                    Toast.makeText(SettingsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void unSubscribeToTopic(){
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.FCM_TOPIC)
                .addOnSuccessListener(aVoid -> {
                    //unsubscribed
                    //save setting ins shared preferences
                    spEditor = sp.edit();
                    spEditor.putBoolean("FCM_ENABLED", false);
                    spEditor.apply();

                    Toast.makeText(SettingsActivity.this, ""+disabledMessage, Toast.LENGTH_SHORT).show();
                    notificationStatusTv.setText(disabledMessage);
                })
                .addOnFailureListener(e -> {
                    //failed unsubscribing
                    Toast.makeText(SettingsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

