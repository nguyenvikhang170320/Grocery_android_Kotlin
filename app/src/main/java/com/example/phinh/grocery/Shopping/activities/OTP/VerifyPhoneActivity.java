package com.example.phinh.grocery.Shopping.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

public class VerifyPhoneActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView tvSendOtpAgain;
    private Button buttonSendOTP;
    private EditText editTextCode;
    private ProgressBar progressbar;

    private String mMobile; //giá trị số phone
    private String mVerificationId; //giá trị id
    private PhoneAuthProvider.ForceResendingToken mForceResendingToken; //giá trị phone auth

    //firebase auth
    FirebaseAuth mAuth;

    //Log
    public static final String TAG = VerifyPhoneActivity.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);

        imageView = (ImageView) findViewById(R.id.imageView);
        tvSendOtpAgain = (TextView) findViewById(R.id.tvSendOtpAgain);
        progressbar = (ProgressBar) findViewById(R.id.progressbar);
        buttonSendOTP = (Button) findViewById(R.id.buttonSendOTP);

        getDataIntent();//xử lý dữ liệu intent bên dưới

        tvSendOtpAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //phần xử lý không nhận được mã
                onClickSendOtpAgain();
            }
        });

        //xử lý khi người dùng bấm send
        buttonSendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                * Ở đây sẽ xét điều kiện đăng nhập có 2 trường hợp:
                * Trường hợp 1: khi là người mua thì vào trang người mua theo số đt đăng ký trước đó
                * Trường hợp 2: khi là người bán thì vào trang người bán theo số đt đăng ký trước đó
                * Trường hợp 3: Không xác định được loại người nên sẽ quay về trang login*/
                String mobile = editTextCode.getText().toString().trim();
                onClickSendOTPCode(mobile);
            }
        });
    }

    //xử lý khi ko bấm gửi được
    private void onClickSendOtpAgain() {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(mMobile)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                signInWithPhoneAuthCredential(phoneAuthCredential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Toast.makeText(VerifyPhoneActivity.this, "Xác minh không thành công", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(verificationId, forceResendingToken);
                                mVerificationId = verificationId; //gán lại giá trị id đã truyền trước đó
                                mForceResendingToken = forceResendingToken; // giá trị phone auth
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    //xử lý sự kiện khi dùng bấm send
    private void onClickSendOTPCode(String mobile) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, mobile);
        signInWithPhoneAuthCredential(credential); // xử lý sign in user hàm nằm bên dưới
    }

    //lấy dữ liệu intent bên phone activity qua
    private void getDataIntent(){
        mMobile = getIntent().getStringExtra("mobile");
        mVerificationId = getIntent().getStringExtra("verification_id");
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
                                Toast.makeText(VerifyPhoneActivity.this, "Mã xác minh đã nhập không hợp lệ",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    //chuyển vào trang MainUserActivity
    private void goToMainUserActivity(String phoneNumber) {
        Intent intent = new Intent(VerifyPhoneActivity.this, MainUserActivity.class);
        intent.putExtra("mobile", phoneNumber);
        startActivity(intent);
    }
}