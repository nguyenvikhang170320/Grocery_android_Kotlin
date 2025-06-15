package com.example.grocery.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.activities.OrderDetailsSellerActivity
import com.example.grocery.adapters.AdapterOrderShop.HolderOrderShop
import com.example.grocery.filter.FilterOrderShop
import com.example.grocery.models.ModelOrderShop
import com.example.grocery.R
import com.example.grocery.activities.CurrencyFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class AdapterOrderShop(
    private val context: Context,
    var orderShopArrayList: ArrayList<ModelOrderShop?>?
) : RecyclerView.Adapter<HolderOrderShop>(), Filterable {
    var filterList: ArrayList<ModelOrderShop?>?
    private var filter: FilterOrderShop? = null

    init {
        filterList = orderShopArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderOrderShop {
        //inflate layout
        val view = LayoutInflater.from(context).inflate(R.layout.row_order_seller, parent, false)
        return HolderOrderShop(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HolderOrderShop, position: Int) {
        //get data at position
        val modelOrderShop = orderShopArrayList!![position]
        val orderId = modelOrderShop?.orderId
        val orderBy = modelOrderShop?.orderBy
        val orderCost = modelOrderShop?.orderCost
        val orderStatus = modelOrderShop?.orderStatus
        val orderTime = modelOrderShop?.orderTime
        val orderTo = modelOrderShop?.orderTo

        //load user/buyer info
        modelOrderShop?.let { loadUserInfo(it, holder) }
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

        //convert time to proper format e.g. dd/mm/yyyy
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = orderTime!!.toLong()
        val formatedDate = DateFormat.format("dd/MM/yyyy", calendar).toString()
        holder.orderDateTv.text = formatedDate
        holder.itemView.setOnClickListener { v: View? ->
            //open order details
            val intent = Intent(context, OrderDetailsSellerActivity::class.java)
            intent.putExtra("orderId", orderId) //to load order info
            intent.putExtra("orderBy", orderBy) //to load info of the user who placed order
            context.startActivity(intent)
        }
    }

    private fun loadUserInfo(modelOrderShop: ModelOrderShop, holder: HolderOrderShop) {
        //to load email of the user/buyer: modelOrderShop.getOrderBy() contains uid of that user/buyer
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(modelOrderShop.orderBy!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val email = "" + dataSnapshot.child("email").value
                    holder.emailTv.text = email
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    override fun getItemCount(): Int {
        return orderShopArrayList!!.size //return size of list / number of records
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            //init filter
            filter = FilterOrderShop(this, filterList)
        }
        return filter!!
    }

    //view holder class for row_order_seller.xml
    class HolderOrderShop(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //ui views of row_order_seller.xml
        val orderIdTv: TextView
        val orderDateTv: TextView
        val emailTv: TextView
        val amountTv: TextView
        val statusTv: TextView

        init {

            //init ui views
            orderIdTv = itemView.findViewById(R.id.orderIdTv)
            orderDateTv = itemView.findViewById(R.id.orderDateTv)
            emailTv = itemView.findViewById(R.id.emailTv)
            amountTv = itemView.findViewById(R.id.amountTv)
            statusTv = itemView.findViewById(R.id.statusTv)
        }
    }
}
