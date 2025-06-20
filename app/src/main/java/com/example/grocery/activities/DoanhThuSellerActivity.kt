package com.example.grocery.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.grocery.databinding.ActivityDoanhThuSellerBinding
import com.example.grocery.models.ModelCartItem
import com.example.grocery.thumucquantrong.CurrencyFormatter
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class DoanhThuSellerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoanhThuSellerBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var thang = 0
    private var nam = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoanhThuSellerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.chonThoiGian.setOnClickListener {
            chonThangNam()
        }

        binding.xoaHet.setOnClickListener {
            binding.tongDonHang.text = ""
            binding.doanhThu.text = ""
            binding.chonThoiGian.text = ""
        }

        binding.thoat.text = "Quay về trang chủ"
        binding.thoat.setOnClickListener {
            finish()
        }
    }

    private fun chonThangNam() {
        val calendar = Calendar.getInstance()
        val thangNow = calendar.get(Calendar.MONTH)
        val namNow = calendar.get(Calendar.YEAR)

        val dialogFragment = MonthYearPickerDialogFragment.getInstance(thangNow, namNow)
        dialogFragment.show(supportFragmentManager, null)

        dialogFragment.setOnDateSetListener { year, month ->
            thang = month + 1 // vì Calendar.MONTH bắt đầu từ 0
            nam = year

            val thoiGian = "$thang/$nam"
            binding.chonThoiGian.text = thoiGian
            truyVanDoanhThu(thang, nam)
        }
    }

    private fun truyVanDoanhThu(thang: Int, nam: Int) {
        val uid = firebaseAuth.uid ?: return
        val ordersRef = FirebaseDatabase.getInstance().getReference("Users/$uid/Orders")

        ordersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var tongTien = 0.0
                var tongSoLuong = 0

                var donHangTonTaiTrongThang = false

                for (orderSnap in snapshot.children) {
                    val orderId = orderSnap.key ?: continue
                    val orderTongTienStr = orderSnap.child("orderCost").getValue(String::class.java)
                    val timestampStr = orderSnap.child("orderTime").getValue(String::class.java) ?: continue

                    val orderTongTien = orderTongTienStr?.toDoubleOrNull() ?: 0.0
                    val timestamp = timestampStr.toLongOrNull() ?: continue

                    val cal = Calendar.getInstance()
                    cal.timeInMillis = timestamp
                    val orderThang = cal.get(Calendar.MONTH) + 1
                    val orderNam = cal.get(Calendar.YEAR)

                    if (orderThang == thang && orderNam == nam) {
                        donHangTonTaiTrongThang = true
                        tongTien += orderTongTien

                        // Truy vấn giỏ hàng để tính tổng sản phẩm
                        FirebaseDatabase.getInstance().getReference("Users/$uid/Orders/$orderId/Items")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(gioHangSnap: DataSnapshot) {
                                    for (itemSnap in gioHangSnap.children) {
                                        val modelCart = itemSnap.getValue(ModelCartItem::class.java)
                                        val quantity = modelCart?.quantity?.toIntOrNull() ?: 0
                                        tongSoLuong += quantity
                                    }

                                    // Cập nhật UI mỗi khi có giỏ hàng được xử lý xong
                                    binding.doanhThu.text = "Sản phẩm: $tongSoLuong"
                                    binding.tongDonHang.text = CurrencyFormatter.format(tongTien)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e(TAG, "Lỗi truy vấn giỏ hàng: ${error.message}")
                                }
                            })
                    }
                }

                if (!donHangTonTaiTrongThang) {
                    binding.doanhThu.text = "Sản phẩm: 0"
                    binding.tongDonHang.text = CurrencyFormatter.format(0.0)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Lỗi truy vấn hóa đơn: ${error.message}")
            }
        })
    }


    companion object {
        private const val TAG = "DoanhThu"
    }
}
