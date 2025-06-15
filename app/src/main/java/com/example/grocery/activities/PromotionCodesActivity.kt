package com.example.grocery.activities

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.adapters.AdapterPromotionShop
import com.example.grocery.models.ModelPromotion
import com.example.grocery.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Objects

class PromotionCodesActivity : AppCompatActivity() {
    private var filteredTv: TextView? = null
    private var promoRv: RecyclerView? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var promotionArrayList: ArrayList<ModelPromotion?>? = null
    private var adapterPromotionShop: AdapterPromotionShop? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promotion_codes)

        //init ui views
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        val addPromoBtn = findViewById<ImageButton>(R.id.addPromoBtn)
        filteredTv = findViewById(R.id.filteredTv)
        val filterBtn = findViewById<ImageButton>(R.id.filterBtn)
        promoRv = findViewById(R.id.promoRv)

        //init firebase auth to get current user
        firebaseAuth = FirebaseAuth.getInstance()
        loadAllPromoCodes()

        //handle click, go back
        backBtn.setOnClickListener { view: View? -> onBackPressed() }
        //handle click, open add promo code activity
        addPromoBtn.setOnClickListener { view: View? ->
            startActivity(
                Intent(
                    this@PromotionCodesActivity,
                    AddPromotionCodeActivity::class.java
                )
            )
        }

        //handle filter button click, show filter dialog
        filterBtn.setOnClickListener { view: View? -> filterDialog() }
    }

    @SuppressLint("SetTextI18n")
    private fun filterDialog() {
        //options to display in dialog
        val options = arrayOf("Tất cả", "Hết hạn", "Chưa hết hạn")
        //dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Mã khuyến mãi")
            .setItems(options) { dialogInterface: DialogInterface?, i: Int ->
                //handle item clicks
                if (i == 0) {
                    //All clicked
                    filteredTv!!.text = "Tất cả các mã khuyến mãi"
                    loadAllPromoCodes()
                } else if (i == 1) {
                    //Expired clicked
                    filteredTv!!.text = "Mã khuyến mãi đã hết hạn"
                    loadExpiredPromoCodes()
                } else if (i == 2) {
                    //Not Expired clicked
                    filteredTv!!.text = "Mã khuyến mãi chưa hết hạn"
                    loadNotExpiredPromoCodes()
                }
            }
            .show()
    }

    private fun loadAllPromoCodes() {
        //init list
        promotionArrayList = ArrayList()

        //db reference Users > current user > Promotions > codes data
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(Objects.requireNonNull(firebaseAuth!!.uid).toString()).child("Promotions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list before adding data
                    promotionArrayList!!.clear()
                    for (ds in snapshot.children) {
                        val modelPromotion = ds.getValue(
                            ModelPromotion::class.java
                        )
                        //add to list
                        promotionArrayList!!.add(modelPromotion)
                    }
                    //setup adapter, add list to adapter
                    adapterPromotionShop =
                        AdapterPromotionShop(this@PromotionCodesActivity, promotionArrayList)
                    //set adapter to recyclerview
                    promoRv!!.adapter = adapterPromotionShop
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadExpiredPromoCodes() {
        //get current date
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH] + 1
        val day = calendar[Calendar.DAY_OF_MONTH]
        val todayDate = "$day/$month/$year" //e.g. 29/06/2020

        //init list
        promotionArrayList = ArrayList()

        //db reference Users > current user > Promotions > codes data
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(Objects.requireNonNull<String?>(firebaseAuth!!.uid)).child("Promotions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list before adding data
                    promotionArrayList!!.clear()
                    for (ds in snapshot.children) {
                        val modelPromotion = ds.getValue(
                            ModelPromotion::class.java
                        )!!
                        val expDate = modelPromotion.expireDate

                        /*--------Check for expired-------*/try {
                            @SuppressLint("SimpleDateFormat") val sdformat =
                                SimpleDateFormat("dd/MM/yyyy")
                            val currentDate = sdformat.parse(todayDate)
                            val expireDate = sdformat.parse(expDate)!!
                            if (expireDate.compareTo(currentDate) > 0) {
                                //date 1 occurs after date 2
                            } else if (expireDate.compareTo(currentDate) < 0) {
                                //date 1 occurs before date 2 (i.e. Expired)
                                //add to list
                                promotionArrayList!!.add(modelPromotion)
                            } else if (expireDate.compareTo(currentDate) == 0) {
                                //both date equals
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    //setup adapter, add list to adapter
                    adapterPromotionShop =
                        AdapterPromotionShop(this@PromotionCodesActivity, promotionArrayList)
                    //set adapter to recyclerview
                    promoRv!!.adapter = adapterPromotionShop
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadNotExpiredPromoCodes() {
        //get current date
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH] + 1
        val day = calendar[Calendar.DAY_OF_MONTH]
        val todayDate = "$day/$month/$year" //e.g. 29/06/2020

        //init list
        promotionArrayList = ArrayList()

        //db reference Users > current user > Promotions > codes data
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(Objects.requireNonNull<String?>(firebaseAuth!!.uid)).child("Promotions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list before adding data
                    promotionArrayList!!.clear()
                    for (ds in snapshot.children) {
                        val modelPromotion = ds.getValue(
                            ModelPromotion::class.java
                        )!!
                        val expDate = modelPromotion.expireDate

                        /*--------Check for expired-------*/try {
                            @SuppressLint("SimpleDateFormat") val sdformat =
                                SimpleDateFormat("dd/MM/yyyy")
                            val currentDate = sdformat.parse(todayDate)
                            val expireDate = sdformat.parse(expDate)!!
                            if (expireDate.compareTo(currentDate) > 0) {
                                //date 1 occurs after date 2
                                //add to list
                                promotionArrayList!!.add(modelPromotion)
                            } else if (expireDate.compareTo(currentDate) < 0) {
                                //date 1 occurs before date 2 (i.e. Expired)
                            } else if (expireDate.compareTo(currentDate) == 0) {
                                //both date equals
                                //add to list
                                promotionArrayList!!.add(modelPromotion)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    //setup adapter, add list to adapter
                    adapterPromotionShop =
                        AdapterPromotionShop(this@PromotionCodesActivity, promotionArrayList)
                    //set adapter to recyclerview
                    promoRv!!.adapter = adapterPromotionShop
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}