package com.example.grocery.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.grocery.adapters.AdapterCartItem
import com.example.grocery.adapters.AdapterProductUser
import com.example.grocery.models.ModelCartItem
import com.example.grocery.models.ModelProduct
import com.example.grocery.thumucquantrong.Constants
import com.example.grocery.R
import com.example.grocery.thumucquantrong.CurrencyFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import p32929.androideasysql_library.Column
import p32929.androideasysql_library.EasyDB
import java.text.SimpleDateFormat
import java.util.Calendar

class ShopDetailsActivity : AppCompatActivity() {
    //declare ui views
    private var shopIv: ImageView? = null
    private var shopNameTv: TextView? = null
    private var phoneTv: TextView? = null
    private var emailTv: TextView? = null
    private var openCloseTv: TextView? = null
    private var deliveryFeeTv: TextView? = null
    private var addressTv: TextView? = null
    private var filteredProductsTv: TextView? = null
    private var cartCountTv: TextView? = null
    private var productsRv: RecyclerView? = null
    private var ratingBar: RatingBar? = null
    private var shopUid: String? = null
    private var myLatitude: Double = 0.0
    private var myLongitude: Double = 0.0
    private var myPhone: String? = null
    private var shopName: String? = null
    private var shopEmail: String? = null
    private var shopPhone: String? = null
    private var shopAddress: String? = null
    private var shopLatitude: Double = 0.0
    private var shopLongitude: Double = 0.0
    var deliveryFee: String? = null
    private var firebaseAuth: FirebaseAuth? = null

    //progress dialog
    private var progressDialog: ProgressDialog? = null
    private var productsList: ArrayList<ModelProduct?>? = null
    private var adapterProductUser: AdapterProductUser? = null

    //cart
    private var cartItemList: ArrayList<ModelCartItem>? = null
    private var easyDB: EasyDB? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_details)

        //init ui views
        shopIv = findViewById(R.id.shopIv)
        shopNameTv = findViewById(R.id.shopNameTv)
        phoneTv = findViewById(R.id.phoneTv)
        emailTv = findViewById(R.id.emailTv)
        openCloseTv = findViewById(R.id.openCloseTv)
        deliveryFeeTv = findViewById(R.id.deliveryFeeTv)
        addressTv = findViewById(R.id.addressTv)
        val filteredProductsTv = findViewById<TextView>(R.id.filteredProductsTv)
        val callBtn = findViewById<ImageButton>(R.id.callBtn)
        val mapBtn = findViewById<ImageButton>(R.id.mapBtn)
        val cartBtn = findViewById<ImageButton>(R.id.cartBtn)
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        val filterProductBtn = findViewById<ImageButton>(R.id.filterProductBtn)
        val searchProductEt = findViewById<EditText>(R.id.searchProductEt)
        productsRv = findViewById(R.id.productsRv)
        cartCountTv = findViewById(R.id.cartCountTv)
        val reviewsBtn = findViewById<ImageButton>(R.id.reviewsBtn)
        ratingBar = findViewById(R.id.ratingBar)

        //init progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Vui lòng đợi")
        progressDialog!!.setCanceledOnTouchOutside(false)

        //get uid of the shop from intent
        shopUid = intent.getStringExtra("shopUid")
        firebaseAuth = FirebaseAuth.getInstance()
        val fUser = FirebaseAuth.getInstance().currentUser
        loadMyInfo()
        loadShopDetails()
        loadShopProducts()
        loadReviews() //avg rating, set on ratingbar

        //declare it to class level and init in onCreate
        easyDB = EasyDB.init(this, "ITEMS_DB")
            .setTableName("ITEMS_TABLE")
            .addColumn(Column("Item_Id", "text", "unique"))
            .addColumn(Column("Item_PID", "text", "not null"))
            .addColumn(Column("Item_Name", "text", "not null"))
            .addColumn(Column("Item_Price_Each", "text", "not null"))
            .addColumn(Column("Item_Price", "text", "not null"))
            .addColumn(Column("Item_Quantity", "text", "not null"))
            .doneTableColumn()

        //each shop have its own products and orders so if user add items to cart and go back and open cart in different shop then cart should be different
        //so delete cart data whenever user open this activity
        deleteCartData()
        cartCount()

        //search
        searchProductEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    adapterProductUser!!.filter.filter(s)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        backBtn.setOnClickListener { v: View? ->
            //go previous activity
            onBackPressed()
        }
        cartBtn.setOnClickListener { v: View? ->
            //show cart dialog
            showCartDialog()
        }
        callBtn.setOnClickListener { v: View? -> dialPhone() }
        mapBtn.setOnClickListener { v: View? -> openMap() }
        filterProductBtn.setOnClickListener { v: View? ->
            val builder = AlertDialog.Builder(this@ShopDetailsActivity)
            builder.setTitle("Sản phẩm:")
                .setItems(Constants.productCategories1) { dialog: DialogInterface?, which: Int ->
                    //get selected item
                    val selected = Constants.productCategories1[which]
                    filteredProductsTv.setText(selected)
                    if (selected == "Tất cả") {
                        //load all
                        loadShopProducts()
                    } else {
                        //load filtered
                        adapterProductUser!!.filter.filter(selected)
                    }
                }
                .show()
        }

        //handle reviewsBtn click, open reviews activity
        reviewsBtn.setOnClickListener { v: View? ->
            //pass shop uid to show its reviews
            val intent = Intent(this@ShopDetailsActivity, ShopReviewsActivity::class.java)
            intent.putExtra("shopUid", shopUid)
            startActivity(intent)
        }
    }

    private var ratingSum = 0f
    private fun loadReviews() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(shopUid!!).child("Ratings")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //clear list before adding data into it
                    ratingSum = 0f
                    for (ds in dataSnapshot.children) {
                        val rating = ("" + ds.child("ratings").value).toFloat() //e.g. 4.3
                        ratingSum =
                            ratingSum + rating //for avg rating, add(addition of) all ratings, later will divide it by number of reviews
                    }
                    val numberOfReviews = dataSnapshot.childrenCount
                    val avgRating = ratingSum / numberOfReviews
                    ratingBar!!.rating = avgRating
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun deleteCartData() {
        easyDB!!.deleteAllDataFromTable() //delete all records from cart
    }

    @SuppressLint("SetTextI18n")
    fun cartCount() {
        //keep it public so we can access in adapter
        //get cart count
        val count = easyDB!!.allData.count
        if (count <= 0) {
            //no item in cart, hide cart count textview
            cartCountTv!!.visibility = View.GONE
        } else {
            //have items in cart, show cart count textview and set count
            cartCountTv!!.visibility = View.VISIBLE
            cartCountTv!!.text =
                "" + count //concatenate with string, because we cant set integer in textview
        }
    }
// ... (Your existing imports and class definition) ...

    var allTotalPrice = 0.0
    var finalTotalPrice=0.0
    //need to access these views in adapter so making pucblic
    var sTotalTv: TextView? = null
    var dFeeTv: TextView? = null
    var allTotalPriceTv: TextView? = null
    var promoDescriptionTv: TextView? = null
    var discountTv: TextView? = null
    var promoCodeEt: EditText? = null // This is a public field, but also a local in showCartDialog
    var applyBtn: Button? = null // This is a public field, but also a local in showCartDialog

    @SuppressLint("SetTextI18n")
    private fun showCartDialog() {
        //init list
        cartItemList = ArrayList()

        //inflate cart layout
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_cart, null)

        // IMPORTANT: Make sure these local variables are NOT shadowing the public fields if you intend
        // to update the public fields later in the activity.
        // I'll assume you want to use the public fields for allTotalPrice, sTotalTv, etc.
        // So I will remove 'val' from initializations where they shadow public fields.

        val shopNameTv = view.findViewById<TextView>(R.id.shopNameTv)
        // Removed 'val' for promoCodeEt, promoDescriptionTv, discountTv, applyBtn to use public fields
        promoCodeEt = view.findViewById(R.id.promoCodeEt) as? EditText // Cast to EditText
        promoDescriptionTv = view.findViewById(R.id.promoDescriptionTv)
        discountTv = view.findViewById(R.id.discountTv)
        val validateBtn = view.findViewById<FloatingActionButton>(R.id.validateBtn)
        applyBtn = view.findViewById(R.id.applyBtn)
        val cartItemsRv = view.findViewById<RecyclerView>(R.id.cartItemsRv)
        sTotalTv = view.findViewById(R.id.sTotalTv)
        dFeeTv = view.findViewById(R.id.dFeeTv)
        allTotalPriceTv = view.findViewById(R.id.totalTv)
        val checkoutBtn = view.findViewById<Button>(R.id.checkoutBtn)

        // Reset allTotalPrice before recalculating from DB
        allTotalPrice = 0.0

        //whenever cart dialog shows, check if promo code is applied or not
        if (isPromoCodeApplied) {
            //applied
            promoDescriptionTv!!.visibility = View.VISIBLE
            applyBtn!!.visibility = View.VISIBLE
            applyBtn!!.text = "Đã áp dụng"
            promoCodeEt!!.setText(promoCode)
            promoDescriptionTv!!.text = promoDescription
            // Ensure discount is shown correctly when dialog is first opened and promo is applied
            discountTv!!.text = CurrencyFormatter.format(promoPrice.toDoubleOrNull() ?: 0.0) // Assume promoPrice is a numeric string
        } else {
            //not applied
            promoDescriptionTv!!.visibility = View.GONE
            applyBtn!!.visibility = View.GONE
            applyBtn!!.text = "Chưa áp dụng"
            discountTv!!.text = CurrencyFormatter.format(0.0) // Show 0 discount if not applied
        }

        //dialog
        val builder = AlertDialog.Builder(this)
        //set view to dialog
        builder.setView(view)
        shopNameTv.text = shopName

        val easyDB = EasyDB.init(this, "ITEMS_DB")
            .setTableName("ITEMS_TABLE")
            .addColumn(Column("Item_Id", "text", "unique"))
            .addColumn(Column("Item_PID", "text", "not null"))
            .addColumn(Column("Item_Name", "text", "not null"))
            .addColumn(Column("Item_Price_Each", "text", "not null"))
            .addColumn(Column("Item_Price", "text", "not null"))
            .addColumn(Column("Item_Quantity", "text", "not null"))
            .doneTableColumn()

        //get all records from db
        val res = easyDB!!.allData
        res.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(1)
                val pId = cursor.getString(2)
                val name = cursor.getString(3)
                val priceEachString = cursor.getString(4) // Price of single item (e.g., "30000")
                val costString = cursor.getString(5) // Total cost of current item (priceEach * quantity, e.g., "60000")
                val quantity = cursor.getString(6)

                // THÊM LOG ĐỂ KIỂM TRA GIÁ TRỊ STRING GỐC TỪ DB
                Log.d("DEBUG_SHOP_CART", "DB String - priceEachString: $priceEachString, costString: $costString")

                // CRITICAL PART: Use toDoubleOrNull() directly for parsing
                val actualCost = costString?.toDoubleOrNull() ?: 0.0
                val actualPriceEach = priceEachString?.toDoubleOrNull() ?: 0.0 // Also parse price each

                allTotalPrice += actualCost // Accumulate to total price
                Log.d("DEBUG_SHOP_CART", "Parsed Double - actualPriceEach: $actualPriceEach, actualCost: $actualCost")

                val modelCartItem = ModelCartItem(
                    "" + id,
                    "" + pId,
                    "" + name,
                    actualPriceEach.toString(), // Store actualPriceEach (Double) as String for ModelCartItem
                    actualCost.toString(),     // Store actualCost (Double) as String for ModelCartItem
                    "" + quantity
                )
                cartItemList!!.add(modelCartItem)
                Log.d("DEBUG_SHOP_CART", "ModelCartItem created - price: ${modelCartItem.price}, cost: ${modelCartItem.cost}")
            }
        }

        //setup adapter
        val adapterCartItem = AdapterCartItem(this, cartItemList!!)
        //set to recyclerview
        cartItemsRv.adapter = adapterCartItem

        // This is where priceWithDiscount/priceWithoutDiscount are called.
        // They will now use the globally accessible allTotalPrice, which has been accumulated correctly.
        if (isPromoCodeApplied) {
            priceWithDiscount()
        } else {
            priceWithoutDiscount()
        }

        //show dialog
        val dialog = builder.create()
        dialog.show()

        //reset total price on dialog dismiss
        dialog.setOnCancelListener { dialog1: DialogInterface? ->
            allTotalPrice = 0.00
            // Also ensure the UI elements are reset if needed
            sTotalTv?.text = CurrencyFormatter.format(0.0)
            dFeeTv?.text = CurrencyFormatter.format(0.0) // Assuming default delivery fee is 0 or handled elsewhere
            allTotalPriceTv?.text = CurrencyFormatter.format(0.0)
            discountTv?.text = CurrencyFormatter.format(0.0)
            promoDescriptionTv?.text = ""
            promoCodeEt?.setText("")
            applyBtn?.visibility = View.GONE
            // Reset promo flags
            isPromoCodeApplied = false
            promoCode = ""
            promoDescription = ""
            promoMinimumOrderPrice = ""
            promoPrice = ""
        }


        //place order
        checkoutBtn.setOnClickListener { v: View? ->
            //first validate delivery address
            if (myLatitude == 0.0 || myLatitude == 0.0 || myLongitude == 0.0 || myLongitude == 0.0) {
                //user didn't enter address in profile
                Toast.makeText(
                    this@ShopDetailsActivity,
                    "Vui lòng nhập địa chỉ của bạn vào hồ sơ của bạn trước khi đặt hàng...",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener  //don't proceed further
            }
            if (myPhone == "" || myPhone == "null") {
                //user didn't enter phone number in profile
                Toast.makeText(
                    this@ShopDetailsActivity,
                    "Vui lòng nhập số điện thoại của bạn vào hồ sơ của bạn trước khi đặt hàng...",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener  //don't proceed further
            }
            if (cartItemList!!.size == 0) {
                //cart list is empty
                Toast.makeText(
                    this@ShopDetailsActivity,
                    "Không có mặt hàng nào trong giỏ hàng",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener  //don't proceed further
            }
            submitOrder()
        }

        //start validating promo code when validate button is pressed
        validateBtn.setOnClickListener { view12: View? ->
            /*Flow:
            * 1) Get Code from EditText
                 If not empty: promotion may be applied, otherwise no promotion
            * 2) Check if code is valid i.e. Available id seller's promotion db
            * If available: promotion may be applied, otherwise no promotion
            * 3) Check if Expired or not
            * If not expired: promotion may be applied, otherwise no promotion
            * 4) Check if Minimum Order price
            * If minimumOrderPrice is >= SubTotal Price: promotion available, otherwise no promotion*/
            val promotionCode = promoCodeEt!!.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(promotionCode)) {
                Toast.makeText(
                    this@ShopDetailsActivity,
                    "Vui lòng nhập mã khuyến mãi...",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                checkCodeAvailability(promotionCode)
            }
        }

        //apply code if valid, no need to check if valid or not, because this button will be visible only if code is valid
        applyBtn!!.setOnClickListener(View.OnClickListener { view1: View? ->
            isPromoCodeApplied = true
            applyBtn!!.text = "Đã áp dụng"
            priceWithDiscount()
        })
    }

    @SuppressLint("SetTextI19n")
    private fun priceWithDiscount() {
        // Assuming promoPrice and deliveryFee are Strings that might contain "$"
        // We need to clean them and convert to Double before formatting

        val promoPriceDouble = promoPrice.replace("$", "").toDoubleOrNull() ?: 0.0
        val deliveryFeeDouble = deliveryFee?.replace("$", "")?.toDoubleOrNull() ?: 0.0

        discountTv!!.text = CurrencyFormatter.format(promoPriceDouble)
        dFeeTv!!.text = CurrencyFormatter.format(deliveryFeeDouble)

        // allTotalPrice is likely already a Double, so we can directly format it
        sTotalTv!!.text = CurrencyFormatter.format(allTotalPrice)

        finalTotalPrice = allTotalPrice + deliveryFeeDouble - promoPriceDouble
        Log.d("priceWithDiscount",finalTotalPrice.toString())
        allTotalPriceTv!!.text = CurrencyFormatter.format(finalTotalPrice)
    }

    @SuppressLint("SetTextI18n")
    private fun priceWithoutDiscount() {
        // Format the discount as 0
        discountTv!!.text = CurrencyFormatter.format(0.0)

        // Parse and format the delivery fee
        val deliveryFeeDouble = deliveryFee?.replace("$", "")?.toDoubleOrNull() ?: 0.0
        dFeeTv!!.text = CurrencyFormatter.format(deliveryFeeDouble)

        // Format the subtotal (allTotalPrice is assumed to be a Double)
        sTotalTv!!.text = CurrencyFormatter.format(allTotalPrice)

        // Calculate and format the total price
        finalTotalPrice = allTotalPrice + deliveryFeeDouble
        Log.d("priceWithoutDiscount",finalTotalPrice.toString())
        allTotalPriceTv!!.text = CurrencyFormatter.format(finalTotalPrice)
    }

    var isPromoCodeApplied = false
    var promoId: String? = null
    var promoTimestamp: String? = null
    var promoCode: String? = null
    var promoDescription: String? = null
    var promoExpDate: String? = null
    var promoMinimumOrderPrice: String? = null
    var promoPrice = "0.0"
    @SuppressLint("SetTextI18n")
    private fun checkCodeAvailability(promotionCode: String) {
        //progress bar
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Vui lòng đợi")
        progressDialog.setMessage("Kiểm tra mã khuyến mại...")
        progressDialog.setCanceledOnTouchOutside(false)

        //promo is not applied yet
        isPromoCodeApplied = false
        applyBtn!!.text = "Chưa áp dụng"
        priceWithoutDiscount()

        //check promo code availability
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(shopUid!!).child("Promotions").orderByChild("promoCode").equalTo(promotionCode)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //check if promo code exists
                    if (snapshot.exists()) {
                        //promo code exists
                        progressDialog.dismiss()
                        for (ds in snapshot.children) {
                            promoId = "" + ds.child("id").value
                            promoTimestamp = "" + ds.child("timestamp").value
                            promoCode = "" + ds.child("promoCode").value
                            promoDescription = "" + ds.child("description").value
                            promoExpDate = "" + ds.child("expireDate").value
                            promoMinimumOrderPrice = "" + ds.child("minimumOrderPrice").value
                            promoPrice = "" + ds.child("promoPrice").value

                            //now check if code is expired or not
                            checkCodeExpireDate()
                        }
                    } else {
                        //entered promo code doesn't exists
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@ShopDetailsActivity,
                            "Mã khuyến mại không hợp lệ",
                            Toast.LENGTH_SHORT
                        ).show()
                        applyBtn!!.visibility = View.GONE
                        promoDescriptionTv!!.visibility = View.GONE
                        promoDescriptionTv!!.text = ""
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun checkCodeExpireDate() {
        //Get current date
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH] + 1 //it starts from 0 instead of 1 thats why did +1
        val day = calendar[Calendar.DAY_OF_MONTH]
        //concatenate date
        val todayDate = "$day/$month/$year" //e.g. 11/07/2020

        /*----Check for expiry*/try {
            @SuppressLint("SimpleDateFormat") val sdformat = SimpleDateFormat("dd/MM/yyyy")
            val currentDate = sdformat.parse(todayDate)
            val expireDate = sdformat.parse(promoExpDate)!!
            if (expireDate.compareTo(currentDate) > 0) {
                //date 1 occurs after date 2 (i.e. not expire date)
                checkMinimumOrderPrice()
            } else if (expireDate.compareTo(currentDate) < 0) {
                //date 1 occurs before date 2 (i.e. not expired)
                Toast.makeText(this, "Mã khuyến mãi hết hạn vào $promoExpDate", Toast.LENGTH_SHORT)
                    .show()
                applyBtn!!.visibility = View.GONE
                promoDescriptionTv!!.visibility = View.GONE
                promoDescriptionTv!!.text = ""
            } else if (expireDate.compareTo(currentDate) == 0) {
                //both dates are equal  (i.e. not expire date)
                checkMinimumOrderPrice()
            }
        } catch (e: Exception) {
            //if anything goes wrong causing exception while comparing current date and expiry date
            Toast.makeText(this, "" + e.message, Toast.LENGTH_SHORT).show()
            applyBtn!!.visibility = View.GONE
            promoDescriptionTv!!.visibility = View.GONE
            promoDescriptionTv!!.text = ""
        }
    }

    @SuppressLint("DefaultLocale")
    private fun checkMinimumOrderPrice() {
        //each promo code have minimum order price requirement, if order price is less then required then don't allow to apply code

        // Để đảm bảo toDouble() hoạt động, chúng ta phải tin rằng promoMinimumOrderPrice
        // đã là một chuỗi số hợp lệ (ví dụ: "30000" hoặc "30000.0").
        // Nếu nó có dạng "30.000" hoặc "30.000,00", toDouble() sẽ lỗi.
        val promoMinimumOrderPriceDouble = promoMinimumOrderPrice?.toDoubleOrNull() ?: 0.0

        // Log để kiểm tra giá trị sau khi parse
        Log.d("PROMO_DEBUG", "allTotalPrice: $allTotalPrice") // allTotalPrice đã là Double
        Log.d("PROMO_DEBUG", "promoMinimumOrderPrice (string from Firebase): $promoMinimumOrderPrice")
        Log.d("PROMO_DEBUG", "promoMinimumOrderPrice (parsed to Double): $promoMinimumOrderPriceDouble")

        if (allTotalPrice < promoMinimumOrderPriceDouble) {
            //current order price is less then minimum order price required by promo code, so don't allow to apply
            Toast.makeText(
                this,
                "Mã này hợp lệ cho đơn hàng với số tiền tối thiểu: ${CurrencyFormatter.format(promoMinimumOrderPriceDouble)}",
                Toast.LENGTH_SHORT
            ).show()
            applyBtn!!.visibility = View.GONE
            promoDescriptionTv!!.visibility = View.GONE
            promoDescriptionTv!!.text = ""
        } else {
            //current order price is equal to or greater than minimum order price required by promo code, allow to apply code
            applyBtn!!.visibility = View.VISIBLE
            promoDescriptionTv!!.visibility = View.VISIBLE
            promoDescriptionTv!!.text = promoDescription
        }
    }
    private fun submitOrder() {
        //show progress dialog
        progressDialog!!.setMessage("Thứ tự sắp xếp...")
        progressDialog!!.show()

        //for order id and order time
        val timestamp = "" + System.currentTimeMillis()

        val cost = finalTotalPrice
        Log.d("submitOrder",cost.toString())

        //add latitude, longitude of user to each order | delete previous orders from firebase or add manually to them

        //setup oder data
        val hashMap = HashMap<String, Any>()
        hashMap["orderId"] = "" + timestamp
        hashMap["orderTime"] = "" + timestamp
        hashMap["orderStatus"] = "Chưa duyệt" //In Progress/Completed/Cancelled
        hashMap["orderCost"] = "" + cost
        hashMap["orderBy"] = "" + firebaseAuth!!.uid
        hashMap["orderTo"] = "" + shopUid
        hashMap["latitude"] = myLatitude
        hashMap["longitude"] = myLongitude
        hashMap["deliveryFee"] = "" + deliveryFee //include delivery fee in each order
        if (isPromoCodeApplied) {
            //promo applied
            hashMap["discount"] = "" + promoPrice //include promo price
        } else {
            //promo not applied, include price 0
            hashMap["discount"] = "0" //include promo price
        }


        //add to db
        val ref =
            FirebaseDatabase.getInstance().getReference("Users").child(shopUid!!).child("Orders")
        ref.child(timestamp).setValue(hashMap)
            .addOnSuccessListener { aVoid: Void? ->
                //order info added now add order items
                for (i in cartItemList!!.indices) {
                    val pId = cartItemList!![i].pId
                    val id = cartItemList!![i].id
                    val cost1 = cartItemList!![i].cost
                    val name = cartItemList!![i].name
                    val price = cartItemList!![i].price
                    val quantity = cartItemList!![i].quantity
                    val hashMap1 = HashMap<String, Any?>()
                    hashMap1["pId"] = pId
                    hashMap1["name"] = name
                    hashMap1["cost"] = cost1
                    hashMap1["price"] = price
                    hashMap1["quantity"] = quantity
                    ref.child(timestamp).child("Items").child(pId!!).setValue(hashMap1)
                }
                progressDialog!!.dismiss()
                Toast.makeText(
                    this@ShopDetailsActivity,
                    "Đặt hàng đã thành công...",
                    Toast.LENGTH_SHORT
                ).show()
                deleteCartData()
                prepareNotificationMessage(timestamp)

            }
            .addOnFailureListener { e: Exception ->
                //failed placing order
                progressDialog!!.dismiss()
                Toast.makeText(this@ShopDetailsActivity, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun openMap() {
        //saddr means soruce address
        //daddr means destination address
        val address =
            "https://maps.google.com/maps?saddr=$myLatitude,$myLongitude&daddr=$shopLatitude,$shopLongitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(address))
        startActivity(intent)
    }

    private fun dialPhone() {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(shopPhone))))
        Toast.makeText(this, "" + shopPhone, Toast.LENGTH_SHORT).show()
    }
    // Hàm cleanAndToDouble() (đảm bảo nó có sẵn và đúng như đã thảo luận)
    fun String?.cleanAndToDouble(): Double {
        // Đây là định nghĩa đã được kiểm tra và hoạt động tốt
        return this?.replace("[^\\d.]".toRegex(), "") // Loại bỏ tất cả trừ số và dấu chấm
            ?.replace(",", ".") // Thay thế dấu phẩy (nếu có) bằng dấu chấm
            ?.toDoubleOrNull() ?: 0.0}

    private fun loadMyInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.orderByChild("uid").equalTo(firebaseAuth!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (ds in dataSnapshot.children) {
                        //get user data
                        val name = "" + ds.child("name").value
                        val email = "" + ds.child("email").value
                        myPhone = "" + ds.child("phone").value
                        val profileImage = "" + ds.child("profileImage").value
                        val accountType = "" + ds.child("accountType").value
                        val city = "" + ds.child("city").value
                        myLatitude = ds.child("latitude").value.toString().cleanAndToDouble()
                        myLongitude = ds.child("longitude").value.toString().cleanAndToDouble()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun loadShopDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(shopUid!!).addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //get shop data
                val name = "" + dataSnapshot.child("name").value
                shopName = "" + dataSnapshot.child("shopName").value
                shopEmail = "" + dataSnapshot.child("email").value
                shopPhone = "" + dataSnapshot.child("phone").value
                shopLatitude = dataSnapshot.child("latitude").value.toString().cleanAndToDouble()
                shopAddress = "" + dataSnapshot.child("address").value
                shopLongitude = dataSnapshot.child("longitude").value.toString().cleanAndToDouble()
                deliveryFee = "" + dataSnapshot.child("deliveryFee").value
                val profileImage = "" + dataSnapshot.child("profileImage").value
                val shopOpen = "" + dataSnapshot.child("shopOpen").value
                val orderdeliveryFeeDouble = deliveryFee?.toDoubleOrNull() ?: 0.0

                // Định dạng lại chuỗi hiển thị số tiền
                val formattedOrderCost = CurrencyFormatter.format(orderdeliveryFeeDouble)
                //set data
                shopNameTv!!.text = shopName
                emailTv!!.text = shopEmail
                deliveryFeeTv!!.text = "Phí giao hàng: $formattedOrderCost"
                addressTv!!.text = shopAddress
                phoneTv!!.text = shopPhone
                if (shopOpen == "true") {
                    openCloseTv!!.text = "Mở"
                } else {
                    openCloseTv!!.text = "Đóng"
                }
                try {
                    Picasso.get().load(profileImage).into(shopIv)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun loadShopProducts() {
        //init list
        productsList = ArrayList()
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(shopUid!!).child("Products")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //clear list before adding items
                    productsList!!.clear()
                    for (ds in dataSnapshot.children) {
                        val modelProduct = ds.getValue(ModelProduct::class.java)
                        productsList!!.add(modelProduct)
                    }
                    //setup adapter
                    adapterProductUser = AdapterProductUser(this@ShopDetailsActivity, productsList)
                    //set adapter
                    productsRv!!.adapter = adapterProductUser
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun prepareNotificationMessage(orderId: String) {
        val url = "http://172.16.1.61:3000/notify-seller" // 🔁 Thay bằng IP/server thật của bạn

        val requestBody = JSONObject()
        Log.d("NOTIFICATION", "Gửi notification : $requestBody")
        try {
            requestBody.put("buyerUid", firebaseAuth!!.uid)
            requestBody.put("sellerUid", shopUid)
            requestBody.put("orderId", orderId)
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi tạo dữ liệu gửi notification", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, requestBody,
            Response.Listener { response ->
                Log.d("NOTIFICATION", "Gửi notification thành công: $response")

                // Chuyển sang màn hình chi tiết đơn hàng
                val intent = Intent(this@ShopDetailsActivity, OrderDetailsUsersActivity::class.java)
                intent.putExtra("orderTo", shopUid)
                intent.putExtra("orderId", orderId)
                startActivity(intent)
            },
            Response.ErrorListener { error ->
                Log.e("NOTIFICATION", "Lỗi gửi notification: ${error.message}")
                Toast.makeText(this, "Lỗi gửi thông báo", Toast.LENGTH_SHORT).show()

                // Vẫn chuyển sang OrderDetails
                val intent = Intent(this@ShopDetailsActivity, OrderDetailsUsersActivity::class.java)
                intent.putExtra("orderTo", shopUid)
                intent.putExtra("orderId", orderId)
                startActivity(intent)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

}
