package com.example.grocery.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.activities.ShopDetailsActivity
import com.example.grocery.adapters.AdapterShop.HolderShop
import com.example.grocery.models.ModelShop
import com.example.grocery.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class AdapterShop(private val context: Context, var shopsList: ArrayList<ModelShop?>?) :
    RecyclerView.Adapter<HolderShop>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderShop {
        //inflate layout row_shop.xml
        val view = LayoutInflater.from(context).inflate(R.layout.row_shop, parent, false)
        return HolderShop(view)
    }

    override fun onBindViewHolder(holder: HolderShop, position: Int) {
        //get data
        val modelShop = shopsList!![position]
        val address = modelShop?.address
        val online = modelShop?.online
        val phone = modelShop?.phone
        val shopOpen = modelShop?.shopOpen
        val profileImage = modelShop?.profileImage
        val shopName = modelShop?.shopName
        loadReviews(modelShop!!, holder) //load avg rating, set to ratingbar

        //set data
        holder.shopNameTv.text = shopName
        holder.phoneTv.text = phone
        holder.addressTv.text = address
        //check if online
        if (online == true) {
            //shop owner is online
            holder.onlineIv.visibility = View.VISIBLE
        } else {
            //shop owner is offline
            holder.onlineIv.visibility = View.GONE
        }
        //check if shop open
        if (shopOpen == true) {
            //shop open
            holder.shopClosedTv.visibility = View.GONE
        } else {
            //shop closed
            holder.shopClosedTv.visibility = View.VISIBLE
        }
        try {
            Picasso.get().load(profileImage).placeholder(R.drawable.shop).into(holder.shopIv)
        } catch (e: Exception) {
            holder.shopIv.setImageResource(R.drawable.shop)
        }

        //handle click listener, show shop details
        holder.itemView.setOnClickListener { v: View? ->
            val intent = Intent(context, ShopDetailsActivity::class.java)
            intent.putExtra("shopUid", modelShop.uid)
            context.startActivity(intent)
        }
    }

    private var ratingSum = 0f
    private fun loadReviews(modelShop: ModelShop, holder: HolderShop) {
        val shopUid = modelShop.uid
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
                    holder.ratingBar.rating = avgRating
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    override fun getItemCount(): Int {
        return shopsList!!.size //return number of records
    }

    //view holder
    class HolderShop(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //ui views of row_shop.xml
        val shopIv: ImageView
        val onlineIv: ImageView
        val shopClosedTv: TextView
        val shopNameTv: TextView
        val phoneTv: TextView
        val addressTv: TextView
        val ratingBar: RatingBar

        init {

            //init uid views
            shopIv = itemView.findViewById(R.id.shopIv)
            onlineIv = itemView.findViewById(R.id.onlineIv)
            shopClosedTv = itemView.findViewById(R.id.shopClosedTv)
            shopNameTv = itemView.findViewById(R.id.shopNameTv)
            phoneTv = itemView.findViewById(R.id.phoneTv)
            addressTv = itemView.findViewById(R.id.addressTv)
            ratingBar = itemView.findViewById(R.id.ratingBar)
        }
    }
}
