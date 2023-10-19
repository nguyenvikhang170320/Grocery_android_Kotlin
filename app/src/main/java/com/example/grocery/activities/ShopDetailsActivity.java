package com.example.grocery.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.grocery.thumucquantrong.Constants;
import com.example.phinh.grocery.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.grocery.adapters.AdapterCartItem;
import com.example.grocery.adapters.AdapterProductUser;
import com.example.grocery.models.ModelCartItem;
import com.example.grocery.models.ModelProduct;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import p32929.androideasysql_library.Column;
import p32929.androideasysql_library.EasyDB;

public class ShopDetailsActivity extends AppCompatActivity {

    //declare ui views
    private ImageView shopIv;
    private TextView shopNameTv, phoneTv, emailTv, openCloseTv, deliveryFeeTv, addressTv, filteredProductsTv, cartCountTv;
    private RecyclerView productsRv;
    private RatingBar ratingBar;

    private String shopUid;
    private String myLatitude, myLongitude, myPhone;
    private String shopName, shopEmail, shopPhone, shopAddress, shopLatitude, shopLongitude;
    public String deliveryFee;

    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    private ArrayList<ModelProduct> productsList;
    private AdapterProductUser adapterProductUser;

    //cart
    private ArrayList<ModelCartItem> cartItemList;


    private EasyDB easyDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_details);

        //init ui views
        shopIv = findViewById(R.id.shopIv);
        shopNameTv = findViewById(R.id.shopNameTv);
        phoneTv = findViewById(R.id.phoneTv);
        emailTv = findViewById(R.id.emailTv);
        openCloseTv = findViewById(R.id.openCloseTv);
        deliveryFeeTv = findViewById(R.id.deliveryFeeTv);
        addressTv = findViewById(R.id.addressTv);
        filteredProductsTv = findViewById(R.id.filteredProductsTv);
        ImageButton callBtn = findViewById(R.id.callBtn);
        ImageButton mapBtn = findViewById(R.id.mapBtn);
        ImageButton cartBtn = findViewById(R.id.cartBtn);
        ImageButton backBtn = findViewById(R.id.backBtn);
        ImageButton filterProductBtn = findViewById(R.id.filterProductBtn);
        EditText searchProductEt = findViewById(R.id.searchProductEt);
        productsRv = findViewById(R.id.productsRv);
        cartCountTv = findViewById(R.id.cartCountTv);
        ImageButton reviewsBtn = findViewById(R.id.reviewsBtn);
        ratingBar = findViewById(R.id.ratingBar);

        //init progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        //get uid of the shop from intent
        shopUid = getIntent().getStringExtra("shopUid");
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        loadMyInfo();
        loadShopDetails();
        loadShopProducts();
        loadReviews();//avg rating, set on ratingbar

        //declare it to class level and init in onCreate
        easyDB = EasyDB.init(this, "ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id", "text", "unique"))
                .addColumn(new Column("Item_PID", "text", "not null"))
                .addColumn(new Column("Item_Name", "text", "not null"))
                .addColumn(new Column("Item_Price_Each", "text", "not null"))
                .addColumn(new Column("Item_Price", "text", "not null"))
                .addColumn(new Column("Item_Quantity", "text", "not null"))
                .doneTableColumn();

        //each shop have its own products and orders so if user add items to cart and go back and open cart in different shop then cart should be different
        //so delete cart data whenever user open this activity
        deleteCartData();
        cartCount();

        //search
        searchProductEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    adapterProductUser.getFilter().filter(s);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        backBtn.setOnClickListener(v -> {
            //go previous activity
            onBackPressed();
        });

        cartBtn.setOnClickListener(v -> {
            //show cart dialog
            showCartDialog();
        });


        callBtn.setOnClickListener(v -> dialPhone());

        mapBtn.setOnClickListener(v -> openMap());

        filterProductBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ShopDetailsActivity.this);
            builder.setTitle("Sản phẩm:")
                    .setItems(Constants.productCategories1, (dialog, which) -> {
                        //get selected item
                        String selected = Constants.productCategories1[which];
                        filteredProductsTv.setText(selected);
                        if (selected.equals("Tất cả")){
                            //load all
                            loadShopProducts();
                        }
                        else {
                            //load filtered
                            adapterProductUser.getFilter().filter(selected);
                        }
                    })
                    .show();
        });

        //handle reviewsBtn click, open reviews activity
        reviewsBtn.setOnClickListener(v -> {
            //pass shop uid to show its reviews
            Intent intent = new Intent(ShopDetailsActivity.this, ShopReviewsActivity.class);
            intent.putExtra("shopUid", shopUid);
            startActivity(intent);
        });


    }

    private float ratingSum = 0;
    private void loadReviews() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).child("Ratings")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //clear list before adding data into it
                        ratingSum = 0;
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            float rating = Float.parseFloat(""+ds.child("ratings").getValue()); //e.g. 4.3
                            ratingSum = ratingSum +rating; //for avg rating, add(addition of) all ratings, later will divide it by number of reviews
                        }

                        long numberOfReviews = dataSnapshot.getChildrenCount();
                        float avgRating = ratingSum/numberOfReviews;

                        ratingBar.setRating(avgRating);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void deleteCartData() {
        easyDB.deleteAllDataFromTable();//delete all records from cart
    }
    @SuppressLint("SetTextI18n")
    public void cartCount(){
        //keep it public so we can access in adapter
        //get cart count
        int count = easyDB.getAllData().getCount();
        if (count<=0){
            //no item in cart, hide cart count textview
            cartCountTv.setVisibility(View.GONE);
        }
        else {
            //have items in cart, show cart count textview and set count
            cartCountTv.setVisibility(View.VISIBLE);
            cartCountTv.setText(""+count);//concatenate with string, because we cant set integer in textview
        }
    }

    public  double allTotalPrice = 0.00;
    //need to access these views in adapter so making pucblic
    public TextView sTotalTv, dFeeTv, allTotalPriceTv, promoDescriptionTv, discountTv;
    public EditText promoCodeEt;
    public Button applyBtn;
    @SuppressLint("SetTextI18n")
    private void showCartDialog() {
        //init list
        cartItemList = new ArrayList<>();

        //inflate cart layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cart, null);
        //init views
        TextView shopNameTv = view.findViewById(R.id.shopNameTv);
        promoCodeEt = view.findViewById(R.id.promoCodeEt);
        promoDescriptionTv = view.findViewById(R.id.promoDescriptionTv);
        discountTv = view.findViewById(R.id.discountTv);
        FloatingActionButton validateBtn = view.findViewById(R.id.validateBtn);
        applyBtn = view.findViewById(R.id.applyBtn);
        RecyclerView cartItemsRv = view.findViewById(R.id.cartItemsRv);
        sTotalTv = view.findViewById(R.id.sTotalTv);
        dFeeTv = view.findViewById(R.id.dFeeTv);
        allTotalPriceTv = view.findViewById(R.id.totalTv);
        Button checkoutBtn = view.findViewById(R.id.checkoutBtn);

        //whenever cart dialog shows, check if promo code is applied or not
        if (isPromoCodeApplied){
            //applied
            promoDescriptionTv.setVisibility(View.VISIBLE);
            applyBtn.setVisibility(View.VISIBLE);
            applyBtn.setText("Đã áp dụng");
            promoCodeEt.setText(promoCode);
            promoDescriptionTv.setText(promoDescription);
        }
        else {
            //not applied
            promoDescriptionTv.setVisibility(View.GONE);
            applyBtn.setVisibility(View.GONE);
            applyBtn.setText("Chưa áp dụng");
        }

        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //set view to dialog
        builder.setView(view);

        shopNameTv.setText(shopName);

        EasyDB easyDB = EasyDB.init(this, "ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id", "text", "unique"))
                .addColumn(new Column("Item_PID", "text", "not null"))
                .addColumn(new Column("Item_Name", "text", "not null"))
                .addColumn(new Column("Item_Price_Each", "text", "not null"))
                .addColumn(new Column("Item_Price", "text", "not null"))
                .addColumn(new Column("Item_Quantity", "text", "not null"))
                .doneTableColumn();

        //get all records from db
        Cursor res = easyDB.getAllData();
        while (res.moveToNext()){
            String id = res.getString(1);
            String pId = res.getString(2);
            String name = res.getString(3);
            String price = res.getString(4);
            String cost = res.getString(5);
            String quantity = res.getString(6);

            allTotalPrice = allTotalPrice + Double.parseDouble(cost);

            ModelCartItem modelCartItem = new ModelCartItem(
                    ""+id,
                    ""+pId,
                    ""+name,
                    ""+price,
                    ""+cost,
                    ""+quantity
            );

            cartItemList.add(modelCartItem);
        }

        //setup adapter
        AdapterCartItem adapterCartItem = new AdapterCartItem(this, cartItemList);
        //set to recyclerview
        cartItemsRv.setAdapter(adapterCartItem);

        if (isPromoCodeApplied){
            priceWithDiscount();
        }
        else {
            priceWithoutDiscount();
        }

        //show dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        //reset total price on dialog dismiss
        dialog.setOnCancelListener(dialog1 -> allTotalPrice = 0.00);


        //place order
        checkoutBtn.setOnClickListener(v -> {
            //first validate delivery address
            if (myLatitude.equals("") || myLatitude.equals("null") || myLongitude.equals("") || myLongitude.equals("null")){
                //user didn't enter address in profile
                Toast.makeText(ShopDetailsActivity.this, "Vui lòng nhập địa chỉ của bạn vào hồ sơ của bạn trước khi đặt hàng...", Toast.LENGTH_SHORT).show();
                return; //don't proceed further
            }
            if (myPhone.equals("") || myPhone.equals("null")){
                //user didn't enter phone number in profile
                Toast.makeText(ShopDetailsActivity.this, "Vui lòng nhập số điện thoại của bạn vào hồ sơ của bạn trước khi đặt hàng...", Toast.LENGTH_SHORT).show();
                return; //don't proceed further
            }
            if (cartItemList.size() == 0){
                //cart list is empty
                Toast.makeText(ShopDetailsActivity.this, "Không có mặt hàng nào trong giỏ hàng", Toast.LENGTH_SHORT).show();
                return; //don't proceed further
            }

            submitOrder();
        });

        //start validating promo code when validate button is pressed
        validateBtn.setOnClickListener(view12 -> {
            /*Flow:
            * 1) Get Code from EditText
                 If not empty: promotion may be applied, otherwise no promotion
            * 2) Check if code is valid i.e. Available id seller's promotion db
            *    If available: promotion may be applied, otherwise no promotion
            * 3) Check if Expired or not
            *    If not expired: promotion may be applied, otherwise no promotion
            * 4) Check if Minimum Order price
            *    If minimumOrderPrice is >= SubTotal Price: promotion available, otherwise no promotion*/
            String promotionCode = promoCodeEt.getText().toString().trim();
            if (TextUtils.isEmpty(promotionCode)){
                Toast.makeText(ShopDetailsActivity.this, "Vui lòng nhập mã khuyến mãi...", Toast.LENGTH_SHORT).show();
            }
            else {
                checkCodeAvailability(promotionCode);
            }
        });

        //apply code if valid, no need to check if valid or not, because this button will be visible only if code is valid
        applyBtn.setOnClickListener(view1 -> {
            isPromoCodeApplied = true;
            applyBtn.setText("Đã áp dụng");

            priceWithDiscount();
        });

    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void priceWithDiscount(){
        discountTv.setText("$"+ promoPrice);
        dFeeTv.setText("$" + deliveryFee);
        sTotalTv.setText("$" + String.format("%.2f", allTotalPrice));
        allTotalPriceTv.setText("$" + (allTotalPrice + Double.parseDouble(deliveryFee.replace("$","")) - Double.parseDouble(promoPrice)));
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void priceWithoutDiscount() {
        discountTv.setText("$0");
        dFeeTv.setText("$" + deliveryFee);
        sTotalTv.setText("$" + String.format("%.2f", allTotalPrice));
        allTotalPriceTv.setText("$" + (allTotalPrice + Double.parseDouble(deliveryFee.replace("$",""))));
    }

    public boolean isPromoCodeApplied = false;
    public String promoId, promoTimestamp, promoCode, promoDescription, promoExpDate, promoMinimumOrderPrice, promoPrice = "0.0";
    @SuppressLint("SetTextI18n")
    private void checkCodeAvailability(String promotionCode){
        //progress bar
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Vui lòng đợi");
        progressDialog.setMessage("Kiểm tra mã khuyến mại...");
        progressDialog.setCanceledOnTouchOutside(false);

        //promo is not applied yet
        isPromoCodeApplied = false;
        applyBtn.setText("Chưa áp dụng");
        priceWithoutDiscount();

        //check promo code availability
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).child("Promotions").orderByChild("promoCode").equalTo(promotionCode)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //check if promo code exists
                        if (snapshot.exists()){
                            //promo code exists
                            progressDialog.dismiss();
                            for (DataSnapshot ds: snapshot.getChildren()){
                                promoId = ""+ds.child("id").getValue();
                                promoTimestamp = ""+ds.child("timestamp").getValue();
                                promoCode = ""+ds.child("promoCode").getValue();
                                promoDescription = ""+ds.child("description").getValue();
                                promoExpDate = ""+ds.child("expireDate").getValue();
                                promoMinimumOrderPrice = ""+ds.child("minimumOrderPrice").getValue();
                                promoPrice = ""+ds.child("promoPrice").getValue();

                                //now check if code is expired or not
                                checkCodeExpireDate();
                            }
                        }
                        else {
                            //entered promo code doesn't exists
                            progressDialog.dismiss();
                            Toast.makeText(ShopDetailsActivity.this, "Mã khuyến mại không hợp lệ", Toast.LENGTH_SHORT).show();
                            applyBtn.setVisibility(View.GONE);
                            promoDescriptionTv.setVisibility(View.GONE);
                            promoDescriptionTv.setText("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void checkCodeExpireDate() {
        //Get current date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;//it starts from 0 instead of 1 thats why did +1
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        //concatenate date
        String todayDate = day +"/"+ month +"/"+ year; //e.g. 11/07/2020

        /*----Check for expiry*/
        try {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdformat = new SimpleDateFormat("dd/MM/yyyy");
            Date currentDate = sdformat.parse(todayDate);
            Date expireDate = sdformat.parse(promoExpDate);
            //compare dates
            assert expireDate != null;
            if (expireDate.compareTo(currentDate) > 0){
                //date 1 occurs after date 2 (i.e. not expire date)
                checkMinimumOrderPrice();
            }
            else if (expireDate.compareTo(currentDate) < 0){
                //date 1 occurs before date 2 (i.e. not expired)
                Toast.makeText(this, "Mã khuyến mãi hết hạn vào "+promoExpDate, Toast.LENGTH_SHORT).show();
                applyBtn.setVisibility(View.GONE);
                promoDescriptionTv.setVisibility(View.GONE);
                promoDescriptionTv.setText("");
            }
            else if (expireDate.compareTo(currentDate) == 0){
                //both dates are equal  (i.e. not expire date)
                checkMinimumOrderPrice();
            }
        }
        catch (Exception e){
            //if anything goes wrong causing exception while comparing current date and expiry date
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            applyBtn.setVisibility(View.GONE);
            promoDescriptionTv.setVisibility(View.GONE);
            promoDescriptionTv.setText("");
        }
    }

    @SuppressLint("DefaultLocale")
    private void checkMinimumOrderPrice() {
        //each promo code have minimum order price requirement, if order price is less then required then don't allow to apply code
        if (Double.parseDouble(String.format("%.2f", allTotalPrice)) < Double.parseDouble(promoMinimumOrderPrice)){
            //current order price is less then minimum order price required by promo code, so don't allow to apply
            Toast.makeText(this, "Mã này hợp lệ cho đơn hàng với số tiền tối thiểu: $"+promoMinimumOrderPrice, Toast.LENGTH_SHORT).show();
            applyBtn.setVisibility(View.GONE);
            promoDescriptionTv.setVisibility(View.GONE);
            promoDescriptionTv.setText("");
        }
        else {
            //current order price is equal to or greater than minimum order price required by promo code, allow to apply code
            applyBtn.setVisibility(View.VISIBLE);
            promoDescriptionTv.setVisibility(View.VISIBLE);
            promoDescriptionTv.setText(promoDescription);
        }
    }


    private void submitOrder() {
        //show progress dialog
        progressDialog.setMessage("Thứ tự sắp xếp...");
        progressDialog.show();

        //for order id and order time
        final String timestamp = ""+System.currentTimeMillis();

        String cost = allTotalPriceTv.getText().toString().trim().replace("$", ""); //remove $ if contains

        //add latitude, longitude of user to each order | delete previous orders from firebase or add manually to them

        //setup oder data
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("orderId", ""+timestamp);
        hashMap.put("orderTime", ""+timestamp);
        hashMap.put("orderStatus", "Chưa duyệt"); //In Progress/Completed/Cancelled
        hashMap.put("orderCost", ""+cost);
        hashMap.put("orderBy", ""+firebaseAuth.getUid());
        hashMap.put("orderTo", ""+shopUid);
        hashMap.put("latitude", ""+myLatitude);
        hashMap.put("longitude", ""+myLongitude);
        hashMap.put("deliveryFee", ""+deliveryFee); //include delivery fee in each order
        if (isPromoCodeApplied){
            //promo applied
            hashMap.put("discount", ""+promoPrice); //include promo price
        }
        else {
            //promo not applied, include price 0
            hashMap.put("discount", "0"); //include promo price
        }


        //add to db
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Orders");
        ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(aVoid -> {
                    //order info added now add order items
                    for ( int i=0; i<cartItemList.size(); i++){
                        String pId = cartItemList.get(i).getpId();
                        String id = cartItemList.get(i).getId();
                        String cost1 = cartItemList.get(i).getCost();
                        String name = cartItemList.get(i).getName();
                        String price = cartItemList.get(i).getPrice();
                        String quantity = cartItemList.get(i).getQuantity();

                        HashMap<String, String> hashMap1 = new HashMap<>();
                        hashMap1.put("pId", pId);
                        hashMap1.put("name", name);
                        hashMap1.put("cost", cost1);
                        hashMap1.put("price", price);
                        hashMap1.put("quantity", quantity);

                        ref.child(timestamp).child("Items").child(pId).setValue(hashMap1);
                    }
                    progressDialog.dismiss();
                    Toast.makeText(ShopDetailsActivity.this, "Đặt hàng đã thành côngy...", Toast.LENGTH_SHORT).show();

                    prepareNotificationMessage(timestamp);

                })
                .addOnFailureListener(e -> {
                    //failed placing order
                    progressDialog.dismiss();
                    Toast.makeText(ShopDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openMap() {
        //saddr means soruce address
        //daddr means destination address
        String address = "https://maps.google.com/maps?saddr=" + myLatitude + "," + myLongitude + "&daddr=" + shopLatitude + "," + shopLongitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(address));
        startActivity(intent);

    }

    private void dialPhone() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+Uri.encode(shopPhone))));
        Toast.makeText(this, ""+shopPhone, Toast.LENGTH_SHORT).show();
    }

    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            //get user data
                            String name = ""+ds.child("name").getValue();
                            String email = ""+ds.child("email").getValue();
                            myPhone = ""+ds.child("phone").getValue();
                            String profileImage = ""+ds.child("profileImage").getValue();
                            String accountType = ""+ds.child("accountType").getValue();
                            String city = ""+ds.child("city").getValue();
                            myLatitude = ""+ds.child("latitude").getValue();
                            myLongitude = ""+ds.child("longitude").getValue();

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    private void loadShopDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //get shop data
                String name = ""+dataSnapshot.child("name").getValue();
                shopName = ""+dataSnapshot.child("shopName").getValue();
                shopEmail = ""+dataSnapshot.child("email").getValue();
                shopPhone = ""+dataSnapshot.child("phone").getValue();
                shopLatitude = ""+dataSnapshot.child("latitude").getValue();
                shopAddress = ""+dataSnapshot.child("address").getValue();
                shopLongitude = ""+dataSnapshot.child("longitude").getValue();
                deliveryFee = ""+dataSnapshot.child("deliveryFee").getValue();
                String profileImage = ""+dataSnapshot.child("profileImage").getValue();
                String shopOpen = ""+dataSnapshot.child("shopOpen").getValue();

                //set data
                shopNameTv.setText(shopName);
                emailTv.setText(shopEmail);
                deliveryFeeTv.setText("Phí giao hàng: $"+deliveryFee);
                addressTv.setText(shopAddress);
                phoneTv.setText(shopPhone);
                if (shopOpen.equals("true")){
                    openCloseTv.setText("Mở");
                }
                else {
                    openCloseTv.setText("Đóng");
                }
                try {
                    Picasso.get().load(profileImage).into(shopIv);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadShopProducts() {
        //init list
        productsList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(shopUid).child("Products")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //clear list before adding items
                        productsList.clear();
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            ModelProduct modelProduct = ds.getValue(ModelProduct.class);
                            productsList.add(modelProduct);
                        }
                        //setup adapter
                        adapterProductUser = new AdapterProductUser(ShopDetailsActivity.this, productsList);
                        //set adapter
                        productsRv.setAdapter(adapterProductUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void prepareNotificationMessage(String orderId){
        //When user places order, send notification to seller

        //prepare data for notification
        String NOTIFICATION_TOPIC = "/topics/" +Constants.FCM_TOPIC; //must be same as subscribed by user
        String NOTIFICATION_TITLE = "New Order "+ orderId;
        String NOTIFICATION_MESSAGE = "Congratulations...! You have new order.";
        String NOTIFICATION_TYPE = "NewOrder";

        //prepare json (what to send and where to send)
        JSONObject notificationJo = new JSONObject();
        JSONObject notificationBodyJo = new JSONObject();
        try {
            //what to send
            notificationBodyJo.put("notificationType", NOTIFICATION_TYPE);
            notificationBodyJo.put("buyerUid", firebaseAuth.getUid()); //since we are logged in as buyer to place order so current user uid is buyer uid
            notificationBodyJo.put("sellerUid", shopUid);
            notificationBodyJo.put("orderId", orderId);
            notificationBodyJo.put("notificationTitle", NOTIFICATION_TITLE);
            notificationBodyJo.put("notificationMessage", NOTIFICATION_MESSAGE);
            //where to send
            notificationJo.put("to", NOTIFICATION_TOPIC); //to all who subscribed to this topic
            notificationJo.put("data", notificationBodyJo);
        }
        catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        sendFcmNotification(notificationJo, orderId);
    }

    private void sendFcmNotification(JSONObject notificationJo, final String orderId) {
        //send volley request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //after sending fcm start order details activity
                Intent intent = new Intent(ShopDetailsActivity.this, OrderDetailsUsersActivity.class);
                intent.putExtra("orderTo", shopUid);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
            }
        }, error -> {
            //if failed sending fcm, still start order details activity
            Intent intent = new Intent(ShopDetailsActivity.this, OrderDetailsUsersActivity.class);
            intent.putExtra("orderTo", shopUid);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
        }){
            @Override
            public Map<String, String> getHeaders() {

                //put required headers
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "key=" + Constants.FCM_KEY);

                return headers;
            }
        };

        //enque the volley request
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }




}
