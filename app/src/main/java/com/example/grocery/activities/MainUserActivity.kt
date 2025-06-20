package com.example.grocery.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
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
import com.example.grocery.adapters.AdapterOrderUser
import com.example.grocery.adapters.AdapterShop
import com.example.grocery.models.ModelOrderUser
import com.example.grocery.models.ModelShop
import com.example.grocery.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.util.Objects
import kotlin.math.log

class MainUserActivity : AppCompatActivity() {
    private var nameTv: TextView? = null
    private var emailTv: TextView? = null
    private var phoneTv: TextView? = null
    private var tabShopsTv: TextView? = null
    private var tabOrdersTv: TextView? = null
    private var shopsRl: RelativeLayout? = null
    private var ordersRl: RelativeLayout? = null
    private var profileIv: ImageView? = null
    private var shopsRv: RecyclerView? = null
    private var ordersRv: RecyclerView? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null
    private var shopsList: ArrayList<ModelShop?>? = null
    private var adapterShop: AdapterShop? = null
    private var ordersList: ArrayList<ModelOrderUser?>? = null
    private var adapterOrderUser: AdapterOrderUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_user)
        nameTv = findViewById(R.id.nameTv)
        emailTv = findViewById(R.id.emailTv)
        phoneTv = findViewById(R.id.phoneTv)
        tabShopsTv = findViewById(R.id.tabShopsTv)
        tabOrdersTv = findViewById(R.id.tabOrdersTv)
        profileIv = findViewById(R.id.profileIv)
        val logoutBtn = findViewById<ImageButton>(R.id.logoutBtn)
        shopsRl = findViewById(R.id.shopsRl)
        ordersRl = findViewById(R.id.ordersRl)
        shopsRv = findViewById(R.id.shopsRv)
        ordersRv = findViewById(R.id.ordersRv)
        val settingsBtn = findViewById<ImageButton>(R.id.settingsBtn)
        val editProfileBtn = findViewById<ImageButton>(R.id.editProfileBtn)
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Vui lòng đợi")
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
        dataIntent // xử lý sự kiện truyền intent số điện thoại vào đây.
        //at start show shops ui
        showShopsUI()
        logoutBtn.setOnClickListener { v: View? ->
            //make offline
            //sign out
            //go to login activity
            makeMeOffline()
        }


        // edit profile khi bấm vào ảnh profile
        profileIv?.setOnClickListener(View.OnClickListener { v: View? ->
            startActivity(
                Intent(
                    this@MainUserActivity,
                    ProfileEditUserActivity::class.java
                )
            )
        })

        //edit profile khi bấm button hình viết chì
        editProfileBtn.setOnClickListener {
            startActivity(
                Intent(
                    this@MainUserActivity,
                    ProfileEditUserActivity::class.java
                )
            )
        }
        tabShopsTv?.setOnClickListener(View.OnClickListener { v: View? ->
            //show shops
            showShopsUI()
        })
        tabOrdersTv?.setOnClickListener(View.OnClickListener { v: View? ->
            //show orders
            showOrdersUI()
        })

//        //start settings screen
        settingsBtn.setOnClickListener { view: View? ->
            startActivity(
                Intent(
                    this@MainUserActivity,
                    SettingsActivity::class.java
                )
            )
        }


    }

    private val dataIntent: Unit
        private get() {
            val mobile = intent.getStringExtra("mobile")
            val textView = findViewById<TextView>(R.id.phoneTv)
            textView.text = mobile
        }

    private fun showShopsUI() {
        //show shops ui, hide orders ui
        shopsRl?.visibility = View.VISIBLE
        ordersRl?.visibility = View.GONE
        tabShopsTv?.setTextColor(resources.getColor(R.color.colorBlack))
        tabShopsTv?.setBackgroundResource(R.drawable.shape_rect04)
        tabOrdersTv?.setTextColor(resources.getColor(R.color.colorWhite))
        tabOrdersTv?.setBackgroundColor(resources.getColor(android.R.color.transparent))
    }

    private fun showOrdersUI() {
        //show orders ui, hide shops ui
        shopsRl?.visibility = View.GONE
        ordersRl?.visibility = View.VISIBLE
        tabShopsTv?.setTextColor(resources.getColor(R.color.colorWhite))
        tabShopsTv?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        tabOrdersTv?.setTextColor(resources.getColor(R.color.colorBlack))
        tabOrdersTv?.setBackgroundResource(R.drawable.shape_rect04)
    }

    private fun makeMeOffline() {
        //after logging in, make user online
        progressDialog!!.setMessage("Đăng xuất...")
        val hashMap = HashMap<String, Any>()
        hashMap["online"] = "false"

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
                Toast.makeText(this@MainUserActivity, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUser() {
        val user = firebaseAuth!!.currentUser
        if (user == null) {
            startActivity(Intent(this@MainUserActivity, LoginActivity::class.java))
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
                        //get user data
                        val name = "" + ds.child("name").value
                        val email = "" + ds.child("email").value
                        val phone = "" + ds.child("phone").value
                        val profileImage = "" + ds.child("profileImage").value
                        val accountType = "" + ds.child("accountType").value
                        val city = "" + ds.child("city").value

                        //set user data
                        nameTv!!.text = name
                        emailTv!!.text = email
                        phoneTv!!.text = phone
                        try {
                            print(profileImage);
                            Picasso.get().load(profileImage).placeholder(R.drawable.user)
                                .into(profileIv!!)
                        } catch (e: Exception) {
                            profileIv!!.setImageResource(R.drawable.user)
                        }

                        //load only those shops that are in the city of user
                        loadShops(city)
                        loadOrders()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun loadOrders() {
        ordersList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                ordersList!!.clear()

                val tempList = ArrayList<ModelOrderUser>()

                for (ds in dataSnapshot.children) {
                    val uid = ds.key ?: continue
                    val ordersRef = FirebaseDatabase.getInstance().getReference("Users")
                        .child(uid)
                        .child("Orders")

                    ordersRef.orderByChild("orderBy").equalTo(firebaseAuth!!.uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (orderSnap in snapshot.children) {
                                    val model = orderSnap.getValue(ModelOrderUser::class.java)
                                    if (model != null) {
                                        tempList.add(model)
                                    }
                                }

                                // Sau khi duyệt hết tất cả user -> mới gán lại adapter nếu chưa có
                                ordersList!!.clear()
                                ordersList!!.addAll(tempList)
                                adapterOrderUser =
                                    AdapterOrderUser(this@MainUserActivity, ordersList!!)
                                ordersRv!!.adapter = adapterOrderUser
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    private fun loadShops(myCity: String) {
        //init list
        shopsList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.orderByChild("accountType").equalTo("Seller")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //clear list before adding
                    shopsList!!.clear()
                    for (ds in dataSnapshot.children) {
                        val modelShop = ds.getValue(ModelShop::class.java)
                        val shopCity = "" + ds.child("city").value

                        //show only user city shops
                        if (shopCity == myCity) {
                            shopsList!!.add(modelShop)
                        }

                        //if you want to display all shops, skip the if statement and add this
                        //shopsList.add(modelShop);
                    }
                    //setup adapter
                    adapterShop = AdapterShop(this@MainUserActivity, shopsList)
                    //set adapter to recyclerview
                    shopsRv!!.adapter = adapterShop
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }
}
