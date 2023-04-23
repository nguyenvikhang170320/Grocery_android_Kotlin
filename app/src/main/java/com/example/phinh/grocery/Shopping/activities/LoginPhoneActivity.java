package com.example.phinh.grocery.Shopping.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.phinh.grocery.databinding.ActivityLoginPhoneBinding;

public class LoginPhoneActivity extends AppCompatActivity {

    private ActivityLoginPhoneBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}