package com.example.grocery.activities;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.phinh.grocery.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEt;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        ImageButton backBtn = findViewById(R.id.backBtn);
        emailEt = findViewById(R.id.emailEt);
        Button recoverBtn = findViewById(R.id.recoverBtn);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        backBtn.setOnClickListener(v -> onBackPressed());

        recoverBtn.setOnClickListener(v -> recoverPassword());
    }

    private void recoverPassword() {
        String email = emailEt.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Nhập email...", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Gửi hướng dẫn để đặt lại mật khẩu...");
        progressDialog.show();

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    //instructions sent
                    progressDialog.dismiss();
                    Toast.makeText(ForgotPasswordActivity.this, "Đã gửi hướng dẫn đặt lại mật khẩu đến email của bạn...", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    //failed sending instructions
                    progressDialog.dismiss();
                    Toast.makeText(ForgotPasswordActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
