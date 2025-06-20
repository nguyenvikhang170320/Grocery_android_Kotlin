package com.example.grocery.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.adapters.AdapterOrderShop
import com.example.grocery.adapters.AdapterProductSeller
import com.example.grocery.models.ModelOrderShop
import com.example.grocery.models.ModelProduct
import com.example.grocery.thumucquantrong.Constants
import com.example.grocery.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.util.Objects

class MainSellerActivity : AppCompatActivity() {
    private var nameTv: TextView? = null
    private var shopNameTv: TextView? = null
    private var emailTv: TextView? = null
    private var tabProductsTv: TextView? = null
    private var tabOrdersTv: TextView? = null
    private var filteredProductsTv: TextView? = null
    private var filteredOrdersTv: TextView? = null
    private var profileIv: ImageView? = null
    private var productsRl: RelativeLayout? = null
    private var ordersRl: RelativeLayout? = null
    private var productsRv: RecyclerView? = null
    private var ordersRv: RecyclerView? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null
    private var productList: ArrayList<ModelProduct?>? = null
    private var adapterProductSeller: AdapterProductSeller? = null
    private var orderShopArrayList: ArrayList<ModelOrderShop?>? = null
    private var adapterOrderShop: AdapterOrderShop? = null
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_seller)
        nameTv = findViewById(R.id.nameTv)
        shopNameTv = findViewById(R.id.shopNameTv)
        emailTv = findViewById(R.id.emailTv)
        tabProductsTv = findViewById(R.id.tabProductsTv)
        tabOrdersTv = findViewById(R.id.tabOrdersTv)
        filteredProductsTv = findViewById(R.id.filteredProductsTv)
        val searchProductEt = findViewById<EditText>(R.id.searchProductEt)
        val logoutBtn = findViewById<ImageButton>(R.id.logoutBtn)
        val editProfileBtn = findViewById<ImageButton>(R.id.editProfileBtn)
        val addProductBtn = findViewById<ImageButton>(R.id.addProductBtn)
        val filterProductBtn = findViewById<ImageButton>(R.id.filterProductBtn)
        profileIv = findViewById(R.id.profileIv)
        productsRl = findViewById(R.id.productsRl)
        ordersRl = findViewById(R.id.ordersRl)
        productsRv = findViewById(R.id.productsRv)
        filteredOrdersTv = findViewById(R.id.filteredOrdersTv)
        val filterOrderBtn = findViewById<ImageButton>(R.id.filterOrderBtn)
        ordersRv = findViewById(R.id.ordersRv)
        val moreBtn = findViewById<ImageButton>(R.id.moreBtn)
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please wait")
        progressDialog!!.setCanceledOnTouchOutside(false)
        firebaseAuth = FirebaseAuth.getInstance()

        // 👉 Xin quyền thông báo nếu Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
        checkUser()
        loadAllProducts()
        loadAllOrders()
        showProductsUI()

        //search
        searchProductEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    adapterProductSeller!!.filter.filter(s)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        logoutBtn.setOnClickListener { v: View? ->
            //make offline
            //sign out
            //go to login activity
            makeMeOffline()
        }
        profileIv?.setOnClickListener(View.OnClickListener { v: View? ->
            startActivity(
                Intent(
                    this@MainSellerActivity,
                    ProfileEditSellerActivity::class.java
                )
            )
        })
        editProfileBtn.setOnClickListener { v: View? ->
            startActivity(
                Intent(
                    this@MainSellerActivity,
                    ProfileEditSellerActivity::class.java
                )
            )
        }
        addProductBtn.setOnClickListener { v: View? ->
            //open edit add product activity
            startActivity(Intent(this@MainSellerActivity, AddProductActivity::class.java))
        }
        tabProductsTv?.setOnClickListener(View.OnClickListener { v: View? ->
            //load products
            showProductsUI()
        })
        tabOrdersTv?.setOnClickListener(View.OnClickListener { v: View? ->
            //load orders
            showOrdersUI()
        })
        filterProductBtn.setOnClickListener { v: View? ->
            val builder = AlertDialog.Builder(this@MainSellerActivity)
            builder.setTitle("Sản phẩm:")
                .setItems(Constants.productCategories1) { dialog: DialogInterface?, which: Int ->
                    //get selected item
                    val selected = Constants.productCategories1[which]
                    filteredProductsTv!!.setText(selected)
                    if (selected == "Tất cả") {
                        //load all
                        loadAllProducts()
                    } else {
                        //load filtered
                        loadFilteredProducts(selected)
                    }
                }
                .show()
        }
        filterOrderBtn.setOnClickListener { v: View? ->
            //options to display in dialog
            val options = arrayOf("Tất cả", "Chưa duyệt", "Đã duyệt", "Đã hủy")
            //dialog
            val builder = AlertDialog.Builder(this@MainSellerActivity)
            builder.setTitle("Đơn hàng:")
                .setItems(options) { dialog: DialogInterface?, which: Int ->
                    //handle item clicks
                    if (which == 0) {
                        //All clicked
                        filteredOrdersTv!!.setText("Hiển thị tất cả các đơn đặt hàng")
                        adapterOrderShop!!.filter.filter("") //show all orders
                    } else {
                        val optionClicked = options[which]
                        filteredOrdersTv!!.setText("Hiển thị $optionClicked Đơn hàng") //e.g. Showing Completed Orders
                        adapterOrderShop!!.filter.filter(optionClicked)
                    }
                }
                .show()
        }

        //popup menu
        val popupMenu = PopupMenu(this@MainSellerActivity, moreBtn)
        //add menu items to our menu
        popupMenu.menu.add("Cài đặt")
        popupMenu.menu.add("Đánh giá")
        popupMenu.menu.add("Khuyến mãi")
        popupMenu.menu.add("Doanh thu")
        //handle menu item click
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            if (menuItem.title === "Cài đặt") {
                //start settings screen
                startActivity(Intent(this@MainSellerActivity, SettingsActivity::class.java))
            } else if (menuItem.title === "Đánh giá") {
                //open same reviews activity as used in user main page
                val intent = Intent(this@MainSellerActivity, ShopReviewsActivity::class.java)
                intent.putExtra("shopUid", "" + firebaseAuth!!.uid)
                startActivity(intent)
            } else if (menuItem.title === "Khuyến mãi") {
                //start promotions list screen
                startActivity(Intent(this@MainSellerActivity, PromotionCodesActivity::class.java))
            }
            else if (menuItem.title === "Doanh thu") {
                //start promotions list screen
                startActivity(Intent(this@MainSellerActivity, DoanhThuSellerActivity::class.java))
            }
            true
        }

        //show more options: Settings, Reviews, Promotion Codes
        moreBtn.setOnClickListener { view: View? ->
            //show popup menu
            popupMenu.show()
        }
    }

    private fun loadAllOrders() {
        //init array list
        orderShopArrayList = ArrayList()

        //load orders of shop
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(Objects.requireNonNull(firebaseAuth!!.uid).toString()).child("Orders")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //clear list before adding new data in it
                    orderShopArrayList!!.clear()
                    for (ds in dataSnapshot.children) {
                        val modelOrderShop = ds.getValue(
                            ModelOrderShop::class.java
                        )
                        //add to list
                        orderShopArrayList!!.add(modelOrderShop)
                    }
                    //setup adapter
                    adapterOrderShop = AdapterOrderShop(this@MainSellerActivity, orderShopArrayList)
                    //set adapter to recyclerview
                    ordersRv!!.adapter = adapterOrderShop
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun loadFilteredProducts(selected: String) {
        productList = ArrayList()

        //get all products
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(Objects.requireNonNull(firebaseAuth!!.uid).toString()).child("Products")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //before getting reset list
                    productList!!.clear()
                    for (ds in dataSnapshot.children) {
                        val productCategory = "" + ds.child("productCategory").value

                        //if selected category matches product category then add in list
                        if (selected == productCategory) {
                            val modelProduct = ds.getValue(ModelProduct::class.java)
                            productList!!.add(modelProduct)
                        }
                    }
                    //setup adapter
                    adapterProductSeller =
                        AdapterProductSeller(this@MainSellerActivity, productList)
                    //set adapter
                    productsRv!!.adapter = adapterProductSeller
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun loadAllProducts() {
        productList = ArrayList()

        //get all products
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(Objects.requireNonNull(firebaseAuth!!.uid).toString()).child("Products")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //before getting reset list
                    productList!!.clear()
                    for (ds in dataSnapshot.children) {
                        val modelProduct = ds.getValue(ModelProduct::class.java)
                        productList!!.add(modelProduct)
                    }
                    //setup adapter
                    adapterProductSeller =
                        AdapterProductSeller(this@MainSellerActivity, productList)
                    //set adapter
                    productsRv!!.adapter = adapterProductSeller
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun showProductsUI() {
        //show products ui and hide orders ui
        productsRl?.visibility = View.VISIBLE
        ordersRl?.visibility = View.GONE
        tabProductsTv?.setTextColor(resources.getColor(R.color.colorBlack))
        tabProductsTv?.setBackgroundResource(R.drawable.shape_rect04)
        tabOrdersTv?.setTextColor(resources.getColor(R.color.colorWhite))
        tabOrdersTv?.setBackgroundColor(resources.getColor(android.R.color.transparent))
    }

    private fun showOrdersUI() {
        //show orders ui and hide products ui
        productsRl?.visibility = View.GONE
        ordersRl?.visibility = View.VISIBLE
        tabProductsTv?.setTextColor(resources.getColor(R.color.colorWhite))
        tabProductsTv?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        tabOrdersTv?.setTextColor(resources.getColor(R.color.colorBlack))
        tabOrdersTv?.setBackgroundResource(R.drawable.shape_rect04)
    }

    private fun makeMeOffline() {
        //after logging in, make user online
        progressDialog!!.setMessage("Logging Out...")
        val hashMap = HashMap<String, Any>()
        hashMap["online"] = false

        //update value to db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(Objects.requireNonNull(firebaseAuth!!.uid).toString()).updateChildren(hashMap)
            .addOnSuccessListener { aVoid: Void? ->
                //update successfully
                firebaseAuth!!.signOut()
                checkUser()
            }
            .addOnFailureListener { e: Exception ->
                //failed updating
                progressDialog!!.dismiss()
                Toast.makeText(this@MainSellerActivity, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUser() {
        val user = firebaseAuth!!.currentUser
        if (user == null) {
            startActivity(Intent(this@MainSellerActivity, LoginActivity::class.java))
            finish()
        } else {
            loadMyInfo()
        }
    }

    private fun loadMyInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.orderByChild("uid").equalTo(firebaseAuth!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (ds in dataSnapshot.children) {
                        //get data from db
                        val name = "" + ds.child("name").value
                        val accountType = "" + ds.child("accountType").value
                        val email = "" + ds.child("email").value
                        val shopName = "" + ds.child("shopName").value
                        val profileImage = "" + ds.child("profileImage").value

                        //set data to ui
                        nameTv!!.text = name
                        shopNameTv!!.text = shopName
                        emailTv!!.text = email
                        try {
                            Picasso.get().load(profileImage).placeholder(R.drawable.shop)
                                .into(profileIv!!)
                        } catch (e: Exception) {
                            profileIv!!.setImageResource(R.drawable.shop)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }
}
