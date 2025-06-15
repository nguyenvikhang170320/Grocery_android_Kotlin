package com.example.grocery.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.activities.OrderDetailsUsersActivity
import com.example.grocery.adapters.AdapterOrderUser.HolderOrderUser
import com.example.grocery.models.ModelOrderUser
import com.example.grocery.R
import com.example.grocery.activities.CurrencyFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class AdapterOrderUser(
    private val context: Context,
    private val orderUserList: ArrayList<ModelOrderUser?>?
) : RecyclerView.Adapter<HolderOrderUser>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderOrderUser {
        //inflate layout
        val view = LayoutInflater.from(context).inflate(R.layout.row_order_user, parent, false)
        return HolderOrderUser(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HolderOrderUser, position: Int) {
        //get data
        val modelOrderUser = orderUserList!![position]
        val orderId = modelOrderUser?.orderId
        val orderBy = modelOrderUser?.orderBy
        val orderCost = modelOrderUser?.orderCost
        val orderStatus = modelOrderUser?.orderStatus
        val orderTime = modelOrderUser?.orderTime
        val orderTo = modelOrderUser?.orderTo

        //get shop info
        loadShopInfo(modelOrderUser!!, holder)
// Chuyển đổi orderCost từ String sang Double an toàn
        val orderCostDouble = orderCost?.toDoubleOrNull() ?: 0.0

        // Định dạng lại chuỗi hiển thị số tiền
        val formattedOrderCost = CurrencyFormatter.format(orderCostDouble)
        //set data
        holder.amountTv.text = "Số lượng: $formattedOrderCost"
        holder.statusTv.text = orderStatus
        holder.orderIdTv.text = "ID đặt hàng: $orderId"
        when (orderStatus) {
            "Chưa duyệt" -> holder.statusTv.setTextColor(context.resources.getColor(R.color.colorPrimary))
            "Đã duyệt" -> holder.statusTv.setTextColor(context.resources.getColor(R.color.colorGreen))
            "Đã hủy" -> holder.statusTv.setTextColor(context.resources.getColor(R.color.colorRed))
        }

        //convert timestamp to proper format
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = orderTime!!.toLong()
        val formatedDate = DateFormat.format("dd/MM/yyyy", calendar).toString() //e.g. 16/06/2020
        holder.dateTv.text = formatedDate
        holder.itemView.setOnClickListener { v: View? ->
            //open order details, we need to keys there, orderId, orderTo
            val intent = Intent(context, OrderDetailsUsersActivity::class.java)
            intent.putExtra("orderTo", orderTo)
            intent.putExtra("orderId", orderId)
            context.startActivity(intent)
        }
    }

    private fun loadShopInfo(modelOrderUser: ModelOrderUser, holder: HolderOrderUser) {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(modelOrderUser.orderTo!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val shopName = "" + dataSnapshot.child("shopName").value
                    holder.shopNameTv.text = shopName
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    override fun getItemCount(): Int {
        return orderUserList!!.size
    }

    //view holder class
    class HolderOrderUser(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //views of layout
        val orderIdTv: TextView
        val dateTv: TextView
        val shopNameTv: TextView
        val amountTv: TextView
        val statusTv: TextView

        init {

            //init views of layout
            orderIdTv = itemView.findViewById(R.id.orderIdTv)
            dateTv = itemView.findViewById(R.id.dateTv)
            shopNameTv = itemView.findViewById(R.id.shopNameTv)
            amountTv = itemView.findViewById(R.id.amountTv)
            statusTv = itemView.findViewById(R.id.statusTv)
        }
    }
}
