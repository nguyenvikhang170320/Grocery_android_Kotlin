package com.example.phinh.grocery.Shopping.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.phinh.grocery.R;
import com.example.phinh.grocery.Shopping.adapters.AdapterPromotionShop;
import com.example.phinh.grocery.Shopping.models.ModelPromotion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class PromotionCodesActivity extends AppCompatActivity {

    private TextView filteredTv;
    private RecyclerView promoRv;

    private FirebaseAuth firebaseAuth;

    private ArrayList<ModelPromotion> promotionArrayList;
    private AdapterPromotionShop adapterPromotionShop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion_codes);

        //init ui views
        ImageButton backBtn = findViewById(R.id.backBtn);
        ImageButton addPromoBtn = findViewById(R.id.addPromoBtn);
        filteredTv = findViewById(R.id.filteredTv);
        ImageButton filterBtn = findViewById(R.id.filterBtn);
        promoRv = findViewById(R.id.promoRv);

        //init firebase auth to get current user
        firebaseAuth = FirebaseAuth.getInstance();
        loadAllPromoCodes();

        //handle click, go back
        backBtn.setOnClickListener(view -> onBackPressed());
        //handle click, open add promo code activity
        addPromoBtn.setOnClickListener(view -> startActivity(new Intent(PromotionCodesActivity.this, AddPromotionCodeActivity.class)));

        //handle filter button click, show filter dialog
        filterBtn.setOnClickListener(view -> filterDialog());
    }

    @SuppressLint("SetTextI18n")
    private void filterDialog() {
        //options to display in dialog
        String[] options = {"Tất cả", "Hết hạn", "Chưa hết hạn"};
        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Mã khuyến mãi")
                .setItems(options, (dialogInterface, i) -> {
                    //handle item clicks
                    if (i==0){
                        //All clicked
                        filteredTv.setText("Tất cả các mã khuyến mãi");
                        loadAllPromoCodes();
                    }
                    else  if (i==1){
                        //Expired clicked
                        filteredTv.setText("Mã khuyến mãi đã hết hạn");
                        loadExpiredPromoCodes();
                    }
                    else if (i==2){
                        //Not Expired clicked
                        filteredTv.setText("Mã khuyến mãi chưa hết hạn");
                        loadNotExpiredPromoCodes();
                    }
                })
                .show();
    }

    private void loadAllPromoCodes(){
        //init list
        promotionArrayList = new ArrayList<>();

        //db reference Users > current user > Promotions > codes data
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Promotions")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear list before adding data
                        promotionArrayList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            ModelPromotion modelPromotion = ds.getValue(ModelPromotion.class);
                            //add to list
                            promotionArrayList.add(modelPromotion);
                        }
                        //setup adapter, add list to adapter
                        adapterPromotionShop = new AdapterPromotionShop(PromotionCodesActivity.this, promotionArrayList);
                        //set adapter to recyclerview
                        promoRv.setAdapter(adapterPromotionShop);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadExpiredPromoCodes(){
        //get current date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day =  calendar.get(Calendar.DAY_OF_MONTH);
        final String todayDate = day +"/"+ month +"/"+ year; //e.g. 29/06/2020

        //init list
        promotionArrayList = new ArrayList<>();

        //db reference Users > current user > Promotions > codes data
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Promotions")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear list before adding data
                        promotionArrayList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            ModelPromotion modelPromotion = ds.getValue(ModelPromotion.class);

                            assert modelPromotion != null;
                            String expDate = modelPromotion.getExpireDate();

                            /*--------Check for expired-------*/
                            try {
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdformat = new SimpleDateFormat("dd/MM/yyyy");
                                Date currentDate = sdformat.parse(todayDate);
                                Date expireDate = sdformat.parse(expDate);
                                assert expireDate != null;
                                if (expireDate.compareTo(currentDate) > 0){
                                    //date 1 occurs after date 2
                                }
                                else if (expireDate.compareTo(currentDate) < 0){
                                    //date 1 occurs before date 2 (i.e. Expired)
                                    //add to list
                                    promotionArrayList.add(modelPromotion);
                                }
                                else if (expireDate.compareTo(currentDate) == 0){
                                    //both date equals
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }


                        }
                        //setup adapter, add list to adapter
                        adapterPromotionShop = new AdapterPromotionShop(PromotionCodesActivity.this, promotionArrayList);
                        //set adapter to recyclerview
                        promoRv.setAdapter(adapterPromotionShop);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadNotExpiredPromoCodes(){
        //get current date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day =  calendar.get(Calendar.DAY_OF_MONTH);
        final String todayDate = day +"/"+ month +"/"+ year; //e.g. 29/06/2020

        //init list
        promotionArrayList = new ArrayList<>();

        //db reference Users > current user > Promotions > codes data
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Promotions")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear list before adding data
                        promotionArrayList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            ModelPromotion modelPromotion = ds.getValue(ModelPromotion.class);

                            assert modelPromotion != null;
                            String expDate = modelPromotion.getExpireDate();

                            /*--------Check for expired-------*/
                            try {
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdformat = new SimpleDateFormat("dd/MM/yyyy");
                                Date currentDate = sdformat.parse(todayDate);
                                Date expireDate = sdformat.parse(expDate);
                                assert expireDate != null;
                                if (expireDate.compareTo(currentDate) > 0){
                                    //date 1 occurs after date 2
                                    //add to list
                                    promotionArrayList.add(modelPromotion);
                                }
                                else if (expireDate.compareTo(currentDate) < 0){
                                    //date 1 occurs before date 2 (i.e. Expired)
                                }
                                else if (expireDate.compareTo(currentDate) == 0){
                                    //both date equals
                                    //add to list
                                    promotionArrayList.add(modelPromotion);
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }


                        }
                        //setup adapter, add list to adapter
                        adapterPromotionShop = new AdapterPromotionShop(PromotionCodesActivity.this, promotionArrayList);
                        //set adapter to recyclerview
                        promoRv.setAdapter(adapterPromotionShop);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}