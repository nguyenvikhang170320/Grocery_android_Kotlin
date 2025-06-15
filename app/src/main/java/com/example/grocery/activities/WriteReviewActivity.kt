package com.example.grocery.activities

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.grocery.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.util.Objects

class WriteReviewActivity : AppCompatActivity() {
    private var profileIv: ImageView? = null
    private var shopNameTv: TextView? = null
    private var ratingBar: RatingBar? = null
    private var reviewEt: EditText? = null
    private var shopUid: String? = null
    private var firebaseAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_review)

        //init ui views
        //ui views
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        profileIv = findViewById(R.id.profileIv)
        shopNameTv = findViewById(R.id.shopNameTv)
        ratingBar = findViewById(R.id.ratingBar)
        reviewEt = findViewById(R.id.reviewEt)
        val submitBtn = findViewById<FloatingActionButton>(R.id.submitBtn)

        //get shop uid from intent
        shopUid = intent.getStringExtra("shopUid")
        firebaseAuth = FirebaseAuth.getInstance()
        //load shopf info: shop name, shop image
        loadShopInfo()
        //if user has written review to this shop, load it
        loadMyReview()

        //go back to previous activity
        backBtn.setOnClickListener { v: View? -> onBackPressed() }

        //input data
        submitBtn.setOnClickListener { v: View? -> inputData() }
    }

    private fun loadShopInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(shopUid!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //get shop info
                val shopName = "" + dataSnapshot.child("shopName").value
                val shopImage = "" + dataSnapshot.child("profileImage").value

                //set shop info to ui
                shopNameTv!!.text = shopName
                try {
                    Picasso.get().load(shopImage).placeholder(R.drawable.shop).into(profileIv)
                } catch (e: Exception) {
                    profileIv!!.setImageResource(R.drawable.shop)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun loadMyReview() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(shopUid!!).child("Ratings").child(
            Objects.requireNonNull(
                firebaseAuth!!.uid
            ).toString()
        )
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        //my review is available in this shop

                        //get review details
                        val uid = "" + dataSnapshot.child("uid").value
                        val ratings = "" + dataSnapshot.child("ratings").value
                        val review = "" + dataSnapshot.child("review").value
                        val timestamp = "" + dataSnapshot.child("timestamp").value

                        //set review details to our ui
                        val myRating = ratings.toFloat()
                        ratingBar!!.rating = myRating
                        reviewEt!!.setText(review)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun inputData() {
        val ratings = "" + ratingBar!!.rating
        val review = reviewEt!!.text.toString().trim { it <= ' ' }

        //for time of review
        val timestamp = "" + System.currentTimeMillis()

        //setup data in hashmap
        val hashMap = HashMap<String, Any>()
        hashMap["uid"] = "" + firebaseAuth!!.uid
        hashMap["ratings"] = "" + ratings //e.g. 4.6
        hashMap["review"] = "" + review //e.g. Good service
        hashMap["timestamp"] = "" + timestamp

        //put to db: DB > Users > ShopUid > Ratings
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(shopUid!!).child("Ratings").child(
            Objects.requireNonNull(
                firebaseAuth!!.uid
            ).toString()
        ).updateChildren(hashMap)
            .addOnSuccessListener { aVoid: Void? ->
                //review added to db
                Toast.makeText(
                    this@WriteReviewActivity,
                    "Bài đánh giá đã xuất bản thành công...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e: Exception ->
                //failed adding review to db
                Toast.makeText(this@WriteReviewActivity, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }
}
