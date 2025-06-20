package com.example.grocery.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.adapters.AdapterOrderedItem
import com.example.grocery.models.ModelOrderedItem
import com.example.grocery.R
import com.example.grocery.thumucquantrong.CurrencyFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Locale

class OrderDetailsUsersActivity : AppCompatActivity() {
    private var orderTo: String? = null
    private var orderId: String? = null
    private var orderIdTv: TextView? = null
    private var dateTv: TextView? = null
    private var orderStatusTv: TextView? = null
    private var shopNameTv: TextView? = null
    private var totalItemsTv: TextView? = null
    private var amountTv: TextView? = null
    private var addressTv: TextView? = null
    private var itemsRv: RecyclerView? = null
    private var orderedItemArrayList: ArrayList<ModelOrderedItem?>? = null
    private var adapterOrderedItem: AdapterOrderedItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details_users)

        //init views
        //ui views
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        orderIdTv = findViewById(R.id.orderIdTv)
        dateTv = findViewById(R.id.dateTv)
        orderStatusTv = findViewById(R.id.orderStatusTv)
        shopNameTv = findViewById(R.id.shopNameTv)
        totalItemsTv = findViewById(R.id.totalItemsTv)
        amountTv = findViewById(R.id.amountTv)
        addressTv = findViewById(R.id.addressTv)
        itemsRv = findViewById(R.id.itemsRv)
        val writeReviewBtn = findViewById<ImageButton>(R.id.writeReviewBtn)
        val intent = intent
        orderTo =
            intent.getStringExtra("orderTo") //orderTo contains uid of the shop where we placed order
        orderId = intent.getStringExtra("orderId")
        loadShopInfo()
        loadOrderDetails()
        loadOrderedItems()
        backBtn.setOnClickListener { v: View? -> onBackPressed() }

        //handle writeReviewBtn click, start write review activity
        writeReviewBtn.setOnClickListener { v: View? ->
            val intent1 = Intent(this@OrderDetailsUsersActivity, WriteReviewActivity::class.java)
            intent1.putExtra(
                "shopUid",
                orderTo
            ) // to write review to a shop we must have uid of shop
            startActivity(intent1)
        }
    }

    private fun loadOrderedItems() {
        //init list
        orderedItemArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(orderTo!!).child("Orders").child(orderId!!).child("Items")
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    orderedItemArrayList!!.clear() //before loading items clear list
                    for (ds in dataSnapshot.children) {
                        val modelOrderedItem = ds.getValue(
                            ModelOrderedItem::class.java
                        )
                        //add to list
                        orderedItemArrayList!!.add(modelOrderedItem)
                    }
                    //all items added to list
                    //setup adapter
                    adapterOrderedItem =
                        AdapterOrderedItem(this@OrderDetailsUsersActivity, orderedItemArrayList)
                    //set adapter
                    itemsRv!!.adapter = adapterOrderedItem

                    //set items count
                    totalItemsTv!!.text = "" + dataSnapshot.childrenCount
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun loadOrderDetails() {
        //load order details
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(orderTo!!).child("Orders").child(orderId!!)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //get data
                    val orderBy = "" + dataSnapshot.child("orderBy").value
                    val orderCostStr = "" + dataSnapshot.child("orderCost").value // Lấy dưới dạng String
                    val orderId = "" + dataSnapshot.child("orderId").value
                    val orderStatus = "" + dataSnapshot.child("orderStatus").value
                    val orderTime = "" + dataSnapshot.child("orderTime").value
                    val orderTo = "" + dataSnapshot.child("orderTo").value
                    val deliveryFeeStr = "" + dataSnapshot.child("deliveryFee").value // Lấy dưới dạng String
                    val latitude = "" + dataSnapshot.child("latitude").value
                    val longitude = "" + dataSnapshot.child("longitude").value
                    var discountStr = "" + dataSnapshot.child("discount").value // Lấy dưới dạng String

                    // --- BẮT ĐẦU SỬA LỖI ĐỊNH DẠNG ---

                    // Chuyển đổi các giá trị số từ String sang Double an toàn
                    val orderCostDouble = orderCostStr.toDoubleOrNull() ?: 0.0
                    Log.d("Order", orderCostDouble.toString());
                    val deliveryFeeDouble = deliveryFeeStr.toDoubleOrNull() ?: 0.0
                    val discountDouble = discountStr.toDoubleOrNull() ?: 0.0 // Chuyển đổi discount thành Double

                    // Sử dụng CurrencyFormatter.format từ object CurrencyFormatter
                    // Giả định CurrencyFormatter.formatter mà bạn dùng là một instance của NumberFormat
                    // nhưng tốt hơn hết là dùng trực tiếp phương thức format của object như đã sửa trong file trước đó
                    val formattedOrderCost = CurrencyFormatter.format(orderCostDouble)
                    val formattedDeliveryFee = CurrencyFormatter.format(deliveryFeeDouble)
                    Log.d("Order",formattedOrderCost);
                    // Xử lý chuỗi hiển thị giảm giá riêng
                    val discountDisplayString: String = if (discountDouble <= 0.0) {
                        "" // Không hiển thị gì nếu không có giảm giá
                    } else {
                        " - Giảm giá ${CurrencyFormatter.format(discountDouble)}"
                    }

                    // --- KẾT THÚC SỬA LỖI ĐỊNH DẠNG ---

                    //convert timestamp to proper format
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = orderTime.toLong()
                    val formatedDate = DateFormat.format("dd/MM/yyyy hh:mm a", calendar)
                        .toString() //e.g. 20/05/2020 12:01 PM

                    if (orderStatus == "Chưa duyệt") {
                        orderStatusTv!!.setTextColor(resources.getColor(R.color.colorPrimary))
                    } else if (orderStatus == "Đã duyệt") {
                        orderStatusTv!!.setTextColor(resources.getColor(R.color.colorGreen))
                    } else if (orderStatus == "Đã hủy") {
                        orderStatusTv!!.setTextColor(resources.getColor(R.color.colorRed))
                    }

                    //set data
                    orderIdTv!!.text = orderId
                    orderStatusTv!!.text = orderStatus
                    // Cập nhật dòng này để sử dụng các giá trị đã được chuyển đổi và định dạng đúng
                    amountTv!!.text =
                        "$formattedOrderCost [Đã bao gồm phí giao hàng $formattedDeliveryFee$discountDisplayString]"
                    dateTv!!.text = formatedDate
                    findAddress(latitude, longitude)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Log the error for debugging
                    Log.e("OrderDetails", "Failed to load order details: ${databaseError.message}")
                    Toast.makeText(this@OrderDetailsUsersActivity, "Lỗi tải chi tiết đơn hàng: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun loadShopInfo() {
        //get shop info
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(orderTo!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val shopName = "" + dataSnapshot.child("shopName").value
                    shopNameTv!!.text = shopName
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun findAddress(latitude: String, longitude: String) {
        val lat = latitude.toDouble()
        val lon = longitude.toDouble()

        //find address, country, state, city
        val geocoder: Geocoder
        val addresses: List<Address>?
        geocoder = Geocoder(this, Locale.getDefault())
        try {
            addresses = geocoder.getFromLocation(lat, lon, 1)
            val address = addresses!![0].getAddressLine(0) //complete address
            addressTv?.text = address
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
