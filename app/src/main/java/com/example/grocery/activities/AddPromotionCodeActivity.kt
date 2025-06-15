package com.example.grocery.activities

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.grocery.R

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.util.Calendar

class AddPromotionCodeActivity : AppCompatActivity() {
    private var backBtn: ImageButton? = null
    private var promoCodeEt: EditText? = null
    private var promoDescriptionEt: EditText? = null
    private var promoPriceEt: EditText? = null
    private var minimumOrderPriceEt: EditText? = null
    private var expireDateTv: TextView? = null
    private var titleTv: TextView? = null
    private var addBtn: Button? = null

    //firebase auth
    var firebaseAuth: FirebaseAuth? = null

    //progress dialog
    var progressDialog: ProgressDialog? = null
    private var promoId: String? = null
    private var isUpdating = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_promotion_code)

        //init ui views
        backBtn = findViewById(R.id.backBtn)
        promoCodeEt = findViewById(R.id.promoCodeEt)
        promoDescriptionEt = findViewById(R.id.promoDescriptionEt)
        promoPriceEt = findViewById(R.id.promoPriceEt)
        minimumOrderPriceEt = findViewById(R.id.minimumOrderPriceEt)
        expireDateTv = findViewById(R.id.expireDateTv)
        addBtn = findViewById(R.id.addBtn)
        titleTv = findViewById(R.id.titleTv)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        //init/setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Vui lòng đợi")
        progressDialog!!.setCanceledOnTouchOutside(false)

        //get promo id from intent
        val intent = intent
        if (intent.getStringExtra("promoId") != null) {
            //came here from adapter to update record
            promoId = intent.getStringExtra("promoId")
            titleTv!!.setText("Cập nhật mã khuyến mãi")
            addBtn!!.setText("Cập nhật")
            isUpdating = true
            loadPromoInfo() //load promotion code info to set in our views, so we can also update single value
        } else {
            //came here from promo codes list activity to add new promo code
            titleTv!!.setText("Thêm mã khuyến mãi")
            addBtn!!.setText("Thêm")
            isUpdating = false
        }

        //handle click, go back
        backBtn?.setOnClickListener(View.OnClickListener { onBackPressed() })
        //handle click, pick date
        expireDateTv?.setOnClickListener(View.OnClickListener { datePickDialog() })
        //handle click, add promotion code to firebase db
        addBtn?.setOnClickListener(View.OnClickListener { inputData() })
    }

    private fun loadPromoInfo() {
        //db path to promo code
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth!!.uid!!).child("Promotions").child(promoId!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get info of promo code
                    val id = "" + snapshot.child("id").value
                    val timestamp = "" + snapshot.child("timestamp").value
                    val description = "" + snapshot.child("description").value
                    val promoCode = "" + snapshot.child("promoCode").value
                    val promoPrice = "" + snapshot.child("promoPrice").value
                    val minimumOrderPrice = "" + snapshot.child("minimumOrderPrice").value
                    val expireDate = "" + snapshot.child("expireDate").value

                    //set data
                    promoCodeEt!!.setText(promoCode)
                    promoDescriptionEt!!.setText(description)
                    promoPriceEt!!.setText(promoPrice)
                    minimumOrderPriceEt!!.setText(minimumOrderPrice)
                    expireDateTv!!.text = expireDate
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun datePickDialog() {
        //Get current date to set on calendar
        val c = Calendar.getInstance()
        val mYear = c[Calendar.YEAR]
        val mMonth = c[Calendar.MONTH]
        val mDay = c[Calendar.DAY_OF_MONTH]

        //date pick dialog
        val datePickerDialog = DatePickerDialog(this, { datePicker, year, monthOfYear, dayOfMonth ->
            var monthOfYear = monthOfYear
            monthOfYear = monthOfYear + 1
            val mFormat = DecimalFormat("00")
            val pDay = mFormat.format(dayOfMonth.toLong())
            val pMonth = mFormat.format(monthOfYear.toLong())
            val pYear = "" + year
            val pDate = "$pDay/$pMonth/$pYear" //e.g. 27/06/2020
            expireDateTv!!.text = pDate
        }, mYear, mMonth, mDay)

        //show dialog
        datePickerDialog.show()
        //disable past dates selection on calendar
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
    }

    private var description: String? = null
    private var promoCode: String? = null
    private var promoPrice: String? = null
    private var minimumOrderPrice: String? = null
    private var expireDate: String? = null
    private fun inputData() {
        //input data
        promoCode = promoCodeEt!!.text.toString().trim { it <= ' ' }
        description = promoDescriptionEt!!.text.toString().trim { it <= ' ' }
        promoPrice = promoPriceEt!!.text.toString().trim { it <= ' ' }
        minimumOrderPrice = minimumOrderPriceEt!!.text.toString().trim { it <= ' ' }
        expireDate = expireDateTv!!.text.toString().trim { it <= ' ' }

        //validate form data
        if (TextUtils.isEmpty(promoCode)) {
            Toast.makeText(this, "Nhập mã giảm giá...", Toast.LENGTH_SHORT).show()
            return  //don't procede further
        }
        if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Nhập mô tả...", Toast.LENGTH_SHORT).show()
            return  //don't procede further
        }
        if (TextUtils.isEmpty(promoPrice)) {
            Toast.makeText(this, "Nhập giá khuyến mãi...", Toast.LENGTH_SHORT).show()
            return  //don't procede further
        }
        if (TextUtils.isEmpty(minimumOrderPrice)) {
            Toast.makeText(this, "Nhập giá đặt hàng tối thiểu...", Toast.LENGTH_SHORT).show()
            return  //don't procede further
        }
        if (TextUtils.isEmpty(expireDate)) {
            Toast.makeText(this, "Chọn ngày hết hạn...", Toast.LENGTH_SHORT).show()
            return  //don't procede further
        }

        //all fields entered, add/update data to db
        if (isUpdating) {
            //update Data
            updateDataToDb()
        } else {
            //add data
            addDataToDb()
        }
    }

    private fun updateDataToDb() {
        progressDialog!!.setMessage("Cập nhật mã khuyến mại...")
        progressDialog!!.show()

        //setup data to add in db
        val hashMap = HashMap<String, Any>()
        hashMap["description"] = "" + description
        hashMap["promoCode"] = "" + promoCode
        hashMap["promoPrice"] = "" + promoPrice
        hashMap["minimumOrderPrice"] = "" + minimumOrderPrice
        hashMap["expireDate"] = "" + expireDate

        //init db reference Users > Current User > Promotions > PromoID > Promo Data
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth!!.uid!!).child("Promotions").child(promoId!!)
            .updateChildren(hashMap)
            .addOnSuccessListener { //updated
                progressDialog!!.dismiss()
                Toast.makeText(
                    this@AddPromotionCodeActivity,
                    "Cập nhật thành công...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e -> //failed updating
                progressDialog!!.dismiss()
                Toast.makeText(this@AddPromotionCodeActivity, "" + e.message, Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun addDataToDb() {
        progressDialog!!.setMessage("Thêm mã khuyến mãi...")
        progressDialog!!.show()
        val timestamp = "" + System.currentTimeMillis()
        //setup data to add in db
        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "" + timestamp
        hashMap["timestamp"] = "" + timestamp
        hashMap["description"] = "" + description
        hashMap["promoCode"] = "" + promoCode
        hashMap["promoPrice"] = "" + promoPrice
        hashMap["minimumOrderPrice"] = "" + minimumOrderPrice
        hashMap["expireDate"] = "" + expireDate

        //init db reference Users > Current User > Promotions > PromoID > Promo Data
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth!!.uid!!).child("Promotions").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener { //code added
                progressDialog!!.dismiss()
                Toast.makeText(
                    this@AddPromotionCodeActivity,
                    "Đã thêm mã khuyến mại...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e -> //adding code failed
                progressDialog!!.dismiss()
                Toast.makeText(this@AddPromotionCodeActivity, "" + e.message, Toast.LENGTH_SHORT)
                    .show()
            }
    }
}