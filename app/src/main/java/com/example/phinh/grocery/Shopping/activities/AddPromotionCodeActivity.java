package com.example.phinh.grocery.Shopping.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.example.phinh.grocery.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;

public class AddPromotionCodeActivity extends AppCompatActivity {

    private ImageButton backBtn;
    private EditText promoCodeEt, promoDescriptionEt, promoPriceEt, minimumOrderPriceEt;
    private TextView expireDateTv, titleTv;
    private Button addBtn;

    //firebase auth
    FirebaseAuth firebaseAuth;
    //progress dialog
    ProgressDialog progressDialog;

    private String promoId;

    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_promotion_code);

        //init ui views
        backBtn = findViewById(R.id.backBtn);
        promoCodeEt = findViewById(R.id.promoCodeEt);
        promoDescriptionEt = findViewById(R.id.promoDescriptionEt);
        promoPriceEt = findViewById(R.id.promoPriceEt);
        minimumOrderPriceEt = findViewById(R.id.minimumOrderPriceEt);
        expireDateTv = findViewById(R.id.expireDateTv);
        addBtn = findViewById(R.id.addBtn);
        titleTv = findViewById(R.id.titleTv);

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        //init/setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        //get promo id from intent
        Intent intent = getIntent();
        if (intent.getStringExtra("promoId") != null){
            //came here from adapter to update record
            promoId = intent.getStringExtra("promoId");

            titleTv.setText("Cập nhật mã khuyến mãi");
            addBtn.setText("Cập nhật");

            isUpdating = true;

            loadPromoInfo(); //load promotion code info to set in our views, so we can also update single value
        }
        else {
            //came here from promo codes list activity to add new promo code
            titleTv.setText("Thêm mã khuyến mãi");
            addBtn.setText("Thêm");

            isUpdating = false;
        }

        //handle click, go back
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        //handle click, pick date
        expireDateTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickDialog();
            }
        });
        //handle click, add promotion code to firebase db
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputData();
            }
        });
    }

    private void loadPromoInfo() {
        //db path to promo code
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Promotions").child(promoId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get info of promo code
                        String id = ""+snapshot.child("id").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String promoCode = ""+snapshot.child("promoCode").getValue();
                        String promoPrice = ""+snapshot.child("promoPrice").getValue();
                        String minimumOrderPrice = ""+snapshot.child("minimumOrderPrice").getValue();
                        String expireDate = ""+snapshot.child("expireDate").getValue();

                        //set data
                        promoCodeEt.setText(promoCode);
                        promoDescriptionEt.setText(description);
                        promoPriceEt.setText(promoPrice);
                        minimumOrderPriceEt.setText(minimumOrderPrice);
                        expireDateTv.setText(expireDate);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void datePickDialog() {
        //Get current date to set on calendar
        Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);

        //date pick dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                monthOfYear = monthOfYear+1;

                DecimalFormat mFormat = new DecimalFormat("00");
                String pDay = mFormat.format(dayOfMonth);
                String pMonth = mFormat.format(monthOfYear);
                String pYear = "" + year;
                String pDate = pDay + "/" + pMonth + "/" + pYear; //e.g. 27/06/2020

                expireDateTv.setText(pDate);

            }
        },mYear, mMonth, mDay);

        //show dialog
        datePickerDialog.show();
        //disable past dates selection on calendar
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
    }

    private String description, promoCode, promoPrice, minimumOrderPrice, expireDate;
    private void inputData(){
        //input data
        promoCode = promoCodeEt.getText().toString().trim();
        description = promoDescriptionEt.getText().toString().trim();
        promoPrice = promoPriceEt.getText().toString().trim();
        minimumOrderPrice = minimumOrderPriceEt.getText().toString().trim();
        expireDate = expireDateTv.getText().toString().trim();

        //validate form data
        if (TextUtils.isEmpty(promoCode)){
            Toast.makeText(this, "Nhập mã giảm giá...", Toast.LENGTH_SHORT).show();
            return;//don't procede further
        }
        if (TextUtils.isEmpty(description)){
            Toast.makeText(this, "Nhập mô tả...", Toast.LENGTH_SHORT).show();
            return;//don't procede further
        }
        if (TextUtils.isEmpty(promoPrice)){
            Toast.makeText(this, "Nhập giá khuyến mãi...", Toast.LENGTH_SHORT).show();
            return;//don't procede further
        }
        if (TextUtils.isEmpty(minimumOrderPrice)){
            Toast.makeText(this, "Nhập giá đặt hàng tối thiểu...", Toast.LENGTH_SHORT).show();
            return;//don't procede further
        }
        if (TextUtils.isEmpty(expireDate)){
            Toast.makeText(this, "Chọn ngày hết hạn...", Toast.LENGTH_SHORT).show();
            return;//don't procede further
        }

        //all fields entered, add/update data to db
        if (isUpdating){
            //update Data
            updateDataToDb();
        }
        else {
            //add data
            addDataToDb();
        }
    }

    private void updateDataToDb() {
        progressDialog.setMessage("Cập nhật mã khuyến mại...");
        progressDialog.show();

        //setup data to add in db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("description", "" + description);
        hashMap.put("promoCode", "" + promoCode);
        hashMap.put("promoPrice", "" + promoPrice);
        hashMap.put("minimumOrderPrice", "" + minimumOrderPrice);
        hashMap.put("expireDate", "" + expireDate);

        //init db reference Users > Current User > Promotions > PromoID > Promo Data
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Promotions").child(promoId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //updated
                        progressDialog.dismiss();
                        Toast.makeText(AddPromotionCodeActivity.this, "Cập nhật thành công...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed updating
                        progressDialog.dismiss();
                        Toast.makeText(AddPromotionCodeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addDataToDb() {
        progressDialog.setMessage("Thêm mã khuyến mãi...");
        progressDialog.show();

        String timestamp = ""+System.currentTimeMillis();
        //setup data to add in db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", "" + timestamp);
        hashMap.put("timestamp", "" + timestamp);
        hashMap.put("description", "" + description);
        hashMap.put("promoCode", "" + promoCode);
        hashMap.put("promoPrice", "" + promoPrice);
        hashMap.put("minimumOrderPrice", "" + minimumOrderPrice);
        hashMap.put("expireDate", "" + expireDate);

        //init db reference Users > Current User > Promotions > PromoID > Promo Data
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Promotions").child(timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //code added
                        progressDialog.dismiss();
                        Toast.makeText(AddPromotionCodeActivity.this, "Đã thêm mã khuyến mại...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //adding code failed
                        progressDialog.dismiss();
                        Toast.makeText(AddPromotionCodeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}