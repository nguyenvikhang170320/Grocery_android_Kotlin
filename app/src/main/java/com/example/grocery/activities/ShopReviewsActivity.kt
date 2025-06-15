package com.example.grocery.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.adapters.AdapterReview
import com.example.grocery.models.ModelReview
import com.example.grocery.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class ShopReviewsActivity : AppCompatActivity() {
    private var profileIv: ImageView? = null
    private var shopNameTv: TextView? = null
    private var ratingsTv: TextView? = null
    private var ratingBar: RatingBar? = null
    private var reviewsRv: RecyclerView? = null
    private var reviewArrayList: ArrayList<ModelReview?>? = null //will contain list of all reviews
    private var adapterReview: AdapterReview? = null
    private var shopUid: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_reviews)

        //init ui views
        //ui views
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        profileIv = findViewById(R.id.profileIv)
        shopNameTv = findViewById(R.id.shopNameTv)
        ratingBar = findViewById(R.id.ratingBar)
        ratingsTv = findViewById(R.id.ratingsTv)
        reviewsRv = findViewById(R.id.reviewsRv)

        //get shop uid from intent
        shopUid = intent.getStringExtra("shopUid")
        loadShopDetails() //for shop name, image
        loadReviews() //for reviews list, avg rating
        backBtn.setOnClickListener { v: View? ->
            onBackPressed() //go previous activity
        }
    }

    private var ratingSum = 0f
    private fun loadReviews() {
        //init list
        reviewArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(shopUid!!).child("Ratings")
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n", "DefaultLocale")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //clear list before adding data into it
                    reviewArrayList!!.clear()
                    ratingSum = 0f
                    for (ds in dataSnapshot.children) {
                        val rating = ("" + ds.child("ratings").value).toFloat() //e.g. 4.3
                        ratingSum =
                            ratingSum + rating //for avg rating, add(addition of) all ratings, later will divide it by number of reviews
                        val modelReview = ds.getValue(ModelReview::class.java)
                        reviewArrayList!!.add(modelReview)
                    }
                    //setup adapter
                    adapterReview = AdapterReview(this@ShopReviewsActivity, reviewArrayList)
                    //set to recyclerview
                    reviewsRv!!.adapter = adapterReview
                    val numberOfReviews = dataSnapshot.childrenCount
                    val avgRating = ratingSum / numberOfReviews
                    ratingsTv!!.text = String.format(
                        "%.2f",
                        avgRating
                    ) + " [" + numberOfReviews + "]" //e.g. 4.7 [10]
                    ratingBar!!.rating = avgRating
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun loadShopDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(shopUid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val shopName = "" + dataSnapshot.child("shopName").value
                    val profileImage = "" + dataSnapshot.child("profileImage").value
                    shopNameTv!!.text = shopName
                    try {
                        Picasso.get().load(profileImage).placeholder(R.drawable.shop)
                            .into(profileIv)
                    } catch (e: Exception) {
                        //if anything goes wrong setting image (exception occurs), set default image
                        profileIv!!.setImageResource(R.drawable.shop)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }
}
