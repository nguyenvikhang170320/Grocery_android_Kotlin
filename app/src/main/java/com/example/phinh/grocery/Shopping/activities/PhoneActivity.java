package com.example.phinh.grocery.Shopping.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.example.phinh.grocery.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneActivity extends AppCompatActivity {

    private EditText editTextMobile;
    private TextView textView;
    private Button buttonContinue;

    //firebase auth
    FirebaseAuth mAuth;

    //Log
    public static final String TAG = PhoneActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        mAuth = FirebaseAuth.getInstance();

        editTextMobile = (EditText)findViewById(R.id.editTextMobile);
        textView = (TextView)findViewById(R.id.textView);
        buttonContinue = (Button) findViewById(R.id.buttonContinue);

        //xử lý btn Continue
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mobile = editTextMobile.getText().toString().trim();
                if(mobile.isEmpty() || mobile.length() < 10){
                    editTextMobile.setError("Nhập số điện thoại di động hợp lệ vào đây!!");
                    editTextMobile.requestFocus();
                    return;
                }
                else{
                    onClickVerifyPhoneNumber(mobile); //xử lý sự kiện khi người dùng nhập số đt đúng
                }

            }
        });
    }

    //xử lý số đt
    private void onClickVerifyPhoneNumber(String mobile) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+84" +mobile)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                signInWithPhoneAuthCredential(phoneAuthCredential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Toast.makeText(PhoneActivity.this, "Xác minh không thành công", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(verificationId, forceResendingToken);
                                goToVerifyActivity(mobile,verificationId);
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }



    //xử lý sign in user
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.e(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            // Update UI
                            goToMainUserActivity(user.getPhoneNumber());
//                            goToMainSellerActivity(); //người bán
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(PhoneActivity.this, "Mã xác minh đã nhập không hợp lệ",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    //chuyển vào trang MainUserActivity
    private void goToMainUserActivity(String phoneNumber) {
        Intent intent = new Intent(PhoneActivity.this, MainUserActivity.class);
        intent.putExtra("mobile", phoneNumber);
        startActivity(intent);
    }

    //chuyển qua trang xác thực otp
    private void goToVerifyActivity(String mobile, String verificationId) {
        Intent intent = new Intent(PhoneActivity.this, VerifyPhoneActivity.class);
        intent.putExtra("mobile", mobile);
        intent.putExtra("verification_id", verificationId);
        startActivity(intent);
    }


}