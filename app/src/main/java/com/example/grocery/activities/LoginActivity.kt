package com.example.grocery.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.grocery.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {
    //UI views
    private var emailEt: EditText? = null
    private var passwordEt: EditText? = null
    private var forgotTv: TextView? = null
    private var noAccountTv: TextView? = null
    private val otpSMS: TextView? = null //otpSMS id xác thực số đt
    private var loginBtn: Button? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //init UI views
        emailEt = findViewById(R.id.emailEt)
        passwordEt = findViewById(R.id.passwordEt)
        forgotTv = findViewById(R.id.forgotTv)
        noAccountTv = findViewById(R.id.noAccountTv)
        loginBtn = findViewById(R.id.loginBtn)
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please wait")
        progressDialog!!.setCanceledOnTouchOutside(false)
        noAccountTv!!.setOnClickListener(View.OnClickListener {
            startActivity(
                Intent(
                    this@LoginActivity,
                    RegisterUserActivity::class.java
                )
            )
        })
        forgotTv!!.setOnClickListener(View.OnClickListener {
            startActivity(
                Intent(
                    this@LoginActivity,
                    ForgotPasswordActivity::class.java
                )
            )
        })
        loginBtn!!.setOnClickListener(View.OnClickListener { loginUser() })
    }

    private var email: String? = null
    private var password: String? = null
    private fun loginUser() {
        email = emailEt!!.text.toString().trim { it <= ' ' }
        password = passwordEt!!.text.toString().trim { it <= ' ' }


//        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
//            Toast.makeText(this, "Invalid email pattern...", Toast.LENGTH_SHORT).show();
//            return;
//        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter password...", Toast.LENGTH_SHORT).show()
            return
        }
        progressDialog!!.setMessage("Logging In...")
        progressDialog!!.show()
        //Đăng nhập bằng số điện thoại đang nghiên cứu
//        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
//        Query query =databaseReference.orderByChild("phone").equalTo(email);
//        query.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()){
//                    for (DataSnapshot ds: snapshot.getChildren()){
//                        ModelShop modelShop = ds.getValue(ModelShop.class);
//                        if (modelShop != null){
//                            if (!modelShop.getPassword().equals(password)){
//                                Toast.makeText(LoginActivity.this, "Nhập lại mật khẩu", Toast.LENGTH_SHORT).show();
//                            }else {
//
//                            }
//                        }
//                    }
//                }
//                else {
//                    Toast.makeText(LoginActivity.this, "Lỗi đăng nhập không thành công", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
        firebaseAuth!!.signInWithEmailAndPassword(email!!, password!!)
            .addOnSuccessListener { //logged in successfully
                makeMeOnline()
            }
            .addOnFailureListener { e -> //failed logging in
                progressDialog!!.dismiss()
                Toast.makeText(this@LoginActivity, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun makeMeOnline() {


        //after logging in, make user online
        progressDialog!!.setMessage("Checking User...")
        val hashMap = HashMap<String, Any>()
        hashMap["online"] = true

        //update value to db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth!!.uid!!).updateChildren(hashMap)
            .addOnSuccessListener { //update successfully
                checkUserType()
            }
            .addOnFailureListener { e -> //failed updating
                progressDialog!!.dismiss()
                Toast.makeText(this@LoginActivity, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserType() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.orderByChild("uid").equalTo(firebaseAuth!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (ds in dataSnapshot.children) {
                        val accountType = "" + ds.child("accountType").value
                        val userId = firebaseAuth!!.uid!!

                        // ✅ Lấy token FCM
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token = task.result
                                    // ✅ Lưu vào Firebase
                                    val update = HashMap<String, Any>()
                                    update["fcmToken"] = token
                                    ref.child(userId).updateChildren(update)
                                }
                            }

                        progressDialog!!.dismiss()
                        if (accountType == "Seller") {
                            Toast.makeText(
                                this@LoginActivity,
                                "Đăng nhập tài khoản người bán",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    MainSellerActivity::class.java
                                )
                            )
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Đăng nhập tài khoản người mua",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    MainUserActivity::class.java
                                )
                            )
                        }
                        finish()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

}
