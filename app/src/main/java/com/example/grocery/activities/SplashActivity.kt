package com.example.grocery.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.grocery.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Objects

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var firebaseAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //make fullscreen
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash)
        firebaseAuth = FirebaseAuth.getInstance()

        //start login activity after 2sec
        Handler().postDelayed({
            val user = firebaseAuth!!.currentUser
            if (user == null) {
                //user not logged in start login activity
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            } else {
                //user is logger in, check user type
                checkUserType()
            }
        }, 1000)
    }

    private fun checkUserType() {
        //if user is seller, start seller main screen
        //if user is buyer, start user main screen
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(Objects.requireNonNull(firebaseAuth!!.uid).toString())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val accountType = "" + dataSnapshot.child("accountType").value
                    if (accountType == "Seller") {
                        //user is seller
                        startActivity(Intent(this@SplashActivity, MainSellerActivity::class.java))
                    } else {
                        //user is buyer
                        startActivity(Intent(this@SplashActivity, MainUserActivity::class.java))
                    }
                    finish()
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }
}
