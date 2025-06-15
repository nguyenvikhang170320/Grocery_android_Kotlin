package com.example.grocery.adapters

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.adapters.AdapterReview.HolderReview
import com.example.grocery.models.ModelReview
import com.example.grocery.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.util.Calendar

class AdapterReview(
    private val context: Context,
    private val reviewArrayList: ArrayList<ModelReview?>?
) : RecyclerView.Adapter<HolderReview>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderReview {
        //inflate layout row_review
        val view = LayoutInflater.from(context).inflate(R.layout.row_review, parent, false)
        return HolderReview(view)
    }

    override fun onBindViewHolder(holder: HolderReview, position: Int) {
        //get data at position
        val modelReview = reviewArrayList!![position]
        val uid = modelReview?.uid
        val ratings = modelReview?.ratings
        val timestamp = modelReview?.timestamp
        val review = modelReview?.review

        //we also need info (profile image, name) of user who wrote the review: we can do it using uid of user
        loadUserDetail(modelReview!!, holder)

        //convert timestamp to proper format dd/MM/yyyy
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp!!.toLong()
        val dateFormat = DateFormat.format("dd/MM/yyyy", calendar).toString()

        //set data
        holder.ratingBar.rating = ratings!!.toFloat()
        holder.reviewTv.text = review
        holder.dateTv.text = dateFormat
    }

    private fun loadUserDetail(modelReview: ModelReview, holder: HolderReview) {
        //uid of user who wrote review
        val uid = modelReview.uid
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //get user info, use same key names as in firebase
                    val name = "" + dataSnapshot.child("name").value
                    val profileImage = "" + dataSnapshot.child("profileImage").value

                    //set data
                    holder.nameTv.text = name
                    try {
                        Picasso.get().load(profileImage).placeholder(R.drawable.user)
                            .into(holder.profileIv)
                    } catch (e: Exception) {
                        //if anything goes wrong setting image (exception occurs), set default image
                        holder.profileIv.setImageResource(R.drawable.user)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    override fun getItemCount(): Int {
        return reviewArrayList!!.size //return list size
    }

    //view holder class, holds/inits views of recyclerview
    class HolderReview(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //ui views of layout row_review
        val profileIv: ImageView
        val nameTv: TextView
        val dateTv: TextView
        val reviewTv: TextView
        val ratingBar: RatingBar

        init {

            //init views of row_review
            profileIv = itemView.findViewById(R.id.profileIv)
            nameTv = itemView.findViewById(R.id.nameTv)
            ratingBar = itemView.findViewById(R.id.ratingBar)
            dateTv = itemView.findViewById(R.id.dateTv)
            reviewTv = itemView.findViewById(R.id.reviewTv)
        }
    }
}
