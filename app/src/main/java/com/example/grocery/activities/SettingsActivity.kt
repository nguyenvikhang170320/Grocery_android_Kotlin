package com.example.grocery.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.grocery.thumucquantrong.Constants
import com.example.grocery.R
import com.google.firebase.messaging.FirebaseMessaging

class SettingsActivity : AppCompatActivity() {
    private var notificationStatusTv: TextView? = null
    private var sp: SharedPreferences? = null
    private var spEditor: SharedPreferences.Editor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //init ui views
        //ui views
        val fcmSwitch = findViewById<SwitchCompat>(R.id.fcmSwitch)
        val notificationStatusTv = findViewById<TextView>(R.id.notificationStatusTv)
        val backBtn = findViewById<ImageButton>(R.id.backBtn)

        //init shared preferences
        sp = getSharedPreferences("SETTINGS_SP", MODE_PRIVATE)
        //check last selected option; true/false
        val isChecked = sp?.getBoolean("FCM_ENABLED", false) ?: false
        fcmSwitch.isChecked = isChecked
        if (isChecked) {
            //was enabled
            notificationStatusTv.setText(enabledMessage)
        } else {
            //was disabled
            notificationStatusTv.setText(disabledMessage)
        }

        //handle click; goback
        backBtn.setOnClickListener { view: View? -> onBackPressed() }

        //add switch check change listener to enable disable notifications
        fcmSwitch.setOnCheckedChangeListener { compoundButton: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                //checked, enable notifications
                subscribeToTopic()
            } else {
                //unchecked, disable notifications
                unSubscribeToTopic()
            }
        }
    }

    private fun subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.FCM_TOPIC)
            .addOnSuccessListener { aVoid: Void? ->
                //subscribed successfully
                //save setting ins shared preferences
                spEditor = sp?.edit()
                spEditor?.putBoolean("FCM_ENABLED", true)
                spEditor?.apply()
                Toast.makeText(this@SettingsActivity, "" + enabledMessage, Toast.LENGTH_SHORT)
                    .show()
                notificationStatusTv?.text = enabledMessage
            }
            .addOnFailureListener { e: Exception ->
                //failed subscribing
                Toast.makeText(this@SettingsActivity, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun unSubscribeToTopic() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.FCM_TOPIC)
            .addOnSuccessListener { aVoid: Void? ->
                //unsubscribed
                //save setting ins shared preferences
                spEditor = sp?.edit()
                spEditor?.putBoolean("FCM_ENABLED", false)
                spEditor?.apply()
                Toast.makeText(this@SettingsActivity, "" + disabledMessage, Toast.LENGTH_SHORT)
                    .show()
                notificationStatusTv?.text = disabledMessage
            }
            .addOnFailureListener { e: Exception ->
                //failed unsubscribing
                Toast.makeText(this@SettingsActivity, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val enabledMessage = "Thông báo được bật"
        private const val disabledMessage = "Thông báo bị tắt"
    }
}
