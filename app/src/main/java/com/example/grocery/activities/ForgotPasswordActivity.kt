package com.example.grocery.activities

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.grocery.R
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private var emailEt: EditText? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        emailEt = findViewById(R.id.emailEt)
        val recoverBtn = findViewById<Button>(R.id.recoverBtn)
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Vui lòng đợi")
        progressDialog!!.setCanceledOnTouchOutside(false)
        backBtn.setOnClickListener { v: View? -> onBackPressed() }
        recoverBtn.setOnClickListener { v: View? -> recoverPassword() }
    }

    private fun recoverPassword() {
        val email = emailEt!!.text.toString().trim { it <= ' ' }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Nhập email...", Toast.LENGTH_SHORT).show()
            return
        }
        progressDialog!!.setMessage("Gửi hướng dẫn để đặt lại mật khẩu...")
        progressDialog!!.show()
        firebaseAuth!!.sendPasswordResetEmail(email)
            .addOnSuccessListener { aVoid: Void? ->
                //instructions sent
                progressDialog!!.dismiss()
                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "Đã gửi hướng dẫn đặt lại mật khẩu đến email của bạn...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e: Exception ->
                //failed sending instructions
                progressDialog!!.dismiss()
                Toast.makeText(this@ForgotPasswordActivity, "" + e.message, Toast.LENGTH_SHORT)
                    .show()
            }
    }
}
