package com.example.grocery.activities

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.grocery.adapters.AdapterOrderedItem
import com.example.grocery.models.ModelOrderedItem
import com.example.grocery.thumucquantrong.Constants
import com.example.grocery.R
import com.example.grocery.thumucquantrong.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.Objects

class OrderDetailsSellerActivity : AppCompatActivity() {
    private var orderIdTv: TextView? = null
    private var dateTv: TextView? = null
    private var orderStatusTv: TextView? = null
    private var emailTv: TextView? = null
    private var phoneTv: TextView? = null
    private var totalItemsTv: TextView? = null
    private var amountTv: TextView? = null
    private var addressTv: TextView? = null
    private var itemsRv: RecyclerView? = null
    var orderId: String? = null
    var orderBy: String? = null

    //to open destination in map
    var sourceLatitude: String? = null
    var sourceLongitude: String? = null
    var destinationLatitude: String? = null
    var destinationLongitude: String? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var orderedItemArrayList: ArrayList<ModelOrderedItem?>? = null
    private var adapterOrderedItem: AdapterOrderedItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details_seller)

        //init ui views
        //ui views
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        val editBtn = findViewById<ImageButton>(R.id.editBtn)
        val mapBtn = findViewById<ImageButton>(R.id.mapBtn)
        orderIdTv = findViewById(R.id.orderIdTv)
        dateTv = findViewById(R.id.dateTv)
        orderStatusTv = findViewById(R.id.orderStatusTv)
        emailTv = findViewById(R.id.emailTv)
        phoneTv = findViewById(R.id.phoneTv)
        totalItemsTv = findViewById(R.id.totalItemsTv)
        amountTv = findViewById(R.id.amountTv)
        addressTv = findViewById(R.id.addressTv)
        itemsRv = findViewById(R.id.itemsRv)

        //get data from intent
        orderId = intent.getStringExtra("orderId")
        orderBy = intent.getStringExtra("orderBy")
        firebaseAuth = FirebaseAuth.getInstance()
        loadMyInfo()
        loadBuyerInfo()
        loadOrderDetails()
        loadOrderedItems()
        backBtn.setOnClickListener { view: View? ->
            //go back
            onBackPressed()
        }
        mapBtn.setOnClickListener { view: View? -> openMap() }
        editBtn.setOnClickListener { view: View? ->
            //edit order status; In Progress, Completed, Cancelled
            editOrderStatusDialog()
        }
    }

    private fun editOrderStatusDialog() {
        //options to display in dialog
        val options = arrayOf("Chưa duyệt", "Đã duyệt", "Đã hủy")
        //dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chỉnh sửa Trạng thái Đơn hàng")
            .setItems(options) { dialogInterface: DialogInterface?, i: Int ->
                //handle item clikcs
                val selectedOption = options[i]
                editOrderStatus(selectedOption)
            }
            .show()
    }

    private fun editOrderStatus(selectedOption: String) {
        //setup data để cập nhật trạng thái đơn hàng
        val hashMap = HashMap<String, Any>()
        hashMap["orderStatus"] = selectedOption

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        val sellerUid = firebaseAuth!!.uid.toString()

        ref.child(sellerUid).child("Orders").child(orderId!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                val message = "Trạng thái đơn hàng hiện tại: $selectedOption"
                Toast.makeText(this@OrderDetailsSellerActivity, message, Toast.LENGTH_SHORT).show()

                // 🔁 Lấy token của buyer để gửi FCM
                val usersRef = FirebaseDatabase.getInstance().getReference("Users")
                usersRef.child(orderBy!!).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val buyerToken = snapshot.child("fcmToken").value as? String
                        if (!buyerToken.isNullOrEmpty()) {
                            Log.d(TAG, "token: "+buyerToken)
                            prepareNotificationMessage(orderId, message, orderBy!!)
                        } else {
                            Toast.makeText(this@OrderDetailsSellerActivity, "Không tìm thấy token của người mua", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@OrderDetailsSellerActivity, "Lỗi: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@OrderDetailsSellerActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun openMap() {
        //saddr means soruce address
        //daddr means destination address
        val address =
            "https://maps.google.com/maps?saddr=$sourceLatitude,$sourceLongitude&daddr=$destinationLatitude,$destinationLongitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(address))
        startActivity(intent)
    }

    private fun loadMyInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(Objects.requireNonNull(firebaseAuth!!.uid).toString())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    sourceLatitude = "" + dataSnapshot.child("latitude").value
                    sourceLongitude = "" + dataSnapshot.child("longitude").value
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun loadBuyerInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(orderBy!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //get buyer info
                    destinationLatitude = "" + dataSnapshot.child("latitude").value
                    destinationLongitude = "" + dataSnapshot.child("longitude").value
                    val email = "" + dataSnapshot.child("email").value
                    val phone = "" + dataSnapshot.child("phone").value

                    //set info
                    emailTv!!.text = email
                    phoneTv!!.text = phone
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun loadOrderDetails() {
        //load order details
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(Objects.requireNonNull(firebaseAuth!!.uid).toString()).child("Orders").child(orderId!!)
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
                    Log.d("Dia chi", latitude);
                    Log.d("Dia chi", longitude);
                    val latitudeDouble = latitude.cleanAndToDouble()
                    val longitudeDouble = longitude.cleanAndToDouble()
                    Log.d("Dia chi", latitudeDouble.toString());
                    Log.d("Dia chi", longitudeDouble.toString());
                    // Chuyển đổi các giá trị số từ String sang Double an toàn
                    val orderCostDouble = orderCostStr.toDoubleOrNull() ?: 0.0
                    val deliveryFeeDouble = deliveryFeeStr.toDoubleOrNull() ?: 0.0
                    val discountDouble = discountStr.toDoubleOrNull() ?: 0.0 // Chuyển đổi discount thành Double

                    // Sử dụng CurrencyFormatter.format từ object CurrencyFormatter
                    // Giả định CurrencyFormatter.formatter mà bạn dùng là một instance của NumberFormat
                    // nhưng tốt hơn hết là dùng trực tiếp phương thức format của object như đã sửa trong file trước đó
                    val formattedOrderCost = CurrencyFormatter.format(orderCostDouble)
                    val formattedDeliveryFee = CurrencyFormatter.format(deliveryFeeDouble)

                    // Xử lý chuỗi hiển thị giảm giá riêng
                    val discountDisplayString: String = if (discountDouble <= 0.0) {
                        "" // Không hiển thị gì nếu không có giảm giá
                    } else {
                        " - Giảm giá ${CurrencyFormatter.format(discountDouble)}"
                    }
                    val orderTimestamp: Long = orderTime.toLongOrNull() ?: 0L // SỬA: Sử dụng toLongOrNull() và mặc định 0L
                    if (orderTimestamp == 0L) {
                        Log.e("OrderDetailsSeller", "OrderTime is null or invalid: $orderTime")
                        // Bạn có thể hiển thị một thông báo lỗi hoặc giá trị mặc định cho ngày/giờ
                    }

                    // Convert timestamp to proper format
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = orderTimestamp // Sử dụng timestamp đã được kiểm tra
                    val formatedDate = DateFormat.format("dd/MM/yyyy hh:mm a", calendar).toString()


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
                    findAddress(latitudeDouble, longitudeDouble)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Log the error for debugging
                    Log.e("OrderDetails", "Failed to load order details: ${databaseError.message}")
                    Toast.makeText(this@OrderDetailsSellerActivity, "Lỗi tải chi tiết đơn hàng: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    fun String?.cleanAndToDouble(): Double {
        // Đây là định nghĩa đã được kiểm tra và hoạt động tốt
        return this?.replace("[^\\d.]".toRegex(), "") // Loại bỏ tất cả trừ số và dấu chấm
            ?.replace(",", ".") // Thay thế dấu phẩy (nếu có) bằng dấu chấm
            ?.toDoubleOrNull() ?: 0.0}
    private fun findAddress(latitude: Double, longitude: Double) {
        val geocoder: Geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            // Update UI on the main thread
            runOnUiThread {
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0].getAddressLine(0)
                    addressTv!!.text = address
                } else {
                    // Handle case where no address is found
                    addressTv!!.text = "Không tìm thấy địa chỉ"
                    Toast.makeText(this, "Không tìm thấy địa chỉ cho vị trí này.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("OrderDetailsSeller", "Lỗi khi tìm địa chỉ: ${e.message}", e) // Log the full stack trace
            Toast.makeText(this, "Lỗi khi tìm địa chỉ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOrderedItems() {
        //load the products/items of order

        //init list
        orderedItemArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(Objects.requireNonNull(firebaseAuth!!.uid).toString()).child("Orders").child(
            orderId!!
        ).child("Items")
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    orderedItemArrayList!!.clear() //before adding data clear list
                    for (ds in dataSnapshot.children) {
                        val modelOrderedItem = ds.getValue(
                            ModelOrderedItem::class.java
                        )
                        //add to list
                        orderedItemArrayList!!.add(modelOrderedItem)
                    }
                    //setup adapter
                    adapterOrderedItem =
                        AdapterOrderedItem(this@OrderDetailsSellerActivity, orderedItemArrayList)
                    //set adapter to our recyclerview
                    itemsRv!!.adapter = adapterOrderedItem

                    //set total number of items/products in order
                    totalItemsTv!!.text = "" + dataSnapshot.childrenCount
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    // bị lỗi hiện  thông báo order có liên quan đến file MyFiresebaseMessaging
    //Do cơ sở dữ liệu cloud firebase update lên nên code này dùng đã cũ không áp dụng đươc
    //Nên muốn sửa rất đơn giản chỉ code lại vài dòng mà tôi làm biến sửa quá, nên các bạn thông cảm:)))
    private fun prepareNotificationMessage(orderId: String?, message: String, buyerUid: String) {
        Log.d(TAG, "prepareNotificationMessage: Gửi đến buyerUid=$buyerUid")

        val json = JSONObject()
        val sellerUid = firebaseAuth?.uid
        if (sellerUid == null) {
            Toast.makeText(this, "Lỗi: Chưa đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            json.put("buyerUid", buyerUid)
            json.put("sellerUid", sellerUid)
            json.put("orderId", orderId)
            json.put("newStatus", "Đã xác nhận")
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi tạo JSON: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        sendFcmNotification(json)
    }
    private fun sendFcmNotification(notificationJson: JSONObject) {
        val url = "http://172.16.1.61:3000/notify-buyer" // 👉 đổi IP theo server bạn

        val request = object : JsonObjectRequest(Method.POST, url, notificationJson,
            Response.Listener { response ->
                Log.d(TAG, "Notification sent successfully: $response")
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Notification failed: ${error.message}")
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    companion object {
        private const val TAG = "ODER_DETAILS_SELLER"
    }
}