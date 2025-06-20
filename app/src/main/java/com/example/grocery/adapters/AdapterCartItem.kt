package com.example.grocery.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.activities.ShopDetailsActivity
import com.example.grocery.adapters.AdapterCartItem.HolderCartItem
import com.example.grocery.models.ModelCartItem
import com.example.grocery.R
import com.example.grocery.thumucquantrong.CurrencyFormatter // Import your CurrencyFormatter
import p32929.androideasysql_library.Column
import p32929.androideasysql_library.EasyDB

class AdapterCartItem(
    private val context: Context,
    private val cartItems: ArrayList<ModelCartItem>
) : RecyclerView.Adapter<HolderCartItem>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCartItem {
        //inflate layout row_cartitem.xml
        val view = LayoutInflater.from(context).inflate(R.layout.row_cartitem, parent, false)
        return HolderCartItem(view)
    }

    override fun onBindViewHolder(holder: HolderCartItem, position: Int) {
        //get data
        val modelCartItem = cartItems[position]
        val id = modelCartItem.id
        // val getpId = modelCartItem.getpId() // Not directly used here
        val title = modelCartItem.name
        val costString = modelCartItem.cost // This is the total cost for this item (cost * quantity)
        val priceEachString = modelCartItem.price // This is the price per unit

        val quantity = modelCartItem.quantity

        // Correctly parse price strings for Vietnamese locale
        // Sử dụng toDoubleOrNull() trực tiếp vì các chuỗi từ DB/ModelProduct đã ở định dạng số sạch ("30000.0")
        val costDouble = costString?.toDoubleOrNull() ?: 0.0
        val priceEachDouble = priceEachString?.toDoubleOrNull() ?: 0.0


        //thiết lập dữ liệu bằng CurrencyFormatter
        holder.itemTitleTv.text = title
        holder.itemPriceTv.text = CurrencyFormatter.format(costDouble) // Total cost for this item (e.g., 3 * 20.000 đ = 60.000 đ)
        holder.itemQuantityTv.text = "[$quantity]" // e.g. [3]
        holder.itemPriceEachTv.text = CurrencyFormatter.format(priceEachDouble) // Price per unit (e.g., 20.000 đ)

        //handle remove click listener, delete item from cart
        holder.itemRemoveTv.setOnClickListener { v: View? ->
            val easyDB = EasyDB.init(context, "ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(Column("Item_Id", "text", "unique"))
                .addColumn(Column("Item_PID", "text", "not null"))
                .addColumn(Column("Item_Name", "text", "not null"))
                .addColumn(Column("Item_Price_Each", "text", "not null"))
                .addColumn(Column("Item_Price", "text", "not null"))
                .addColumn(Column("Item_Quantity", "text", "not null"))
                .doneTableColumn()

            easyDB.deleteRow(1, id)
            Toast.makeText(context, "Đã xóa khỏi giỏ hàng...", Toast.LENGTH_SHORT).show()

            // Remove item + update
            cartItems.removeAt(position)
            notifyItemRemoved(position)

            if (context is ShopDetailsActivity) {
                val currentSubTotalDouble = parseCurrency(context.sTotalTv!!.text.toString())
                val deliveryFeeDouble = context.deliveryFee?.toDoubleOrNull() ?: 0.0
                val costToRemoveDouble = costString?.toDoubleOrNull() ?: 0.0

                val newSubTotal = currentSubTotalDouble - costToRemoveDouble
                context.sTotalTv!!.text = CurrencyFormatter.format(newSubTotal)

                // Nếu có mã khuyến mãi
                if (context.isPromoCodeApplied) {
                    val promoMin = context.promoMinimumOrderPrice?.toDoubleOrNull() ?: 0.0
                    val promoDiscount = context.promoPrice?.toDoubleOrNull() ?: 0.0

                    if (newSubTotal < promoMin) {
                        // Hủy mã khuyến mãi vì không đủ điều kiện
                        Toast.makeText(
                            context,
                            "Mã này hợp lệ cho đơn hàng từ ${CurrencyFormatter.format(promoMin)}",
                            Toast.LENGTH_SHORT
                        ).show()

                        context.isPromoCodeApplied = false
                        context.applyBtn!!.visibility = View.GONE
                        context.promoDescriptionTv!!.visibility = View.GONE
                        context.promoDescriptionTv!!.text = ""
                        context.discountTv!!.text = CurrencyFormatter.format(0.0)
                        val finalTotal = newSubTotal + deliveryFeeDouble
                        context.allTotalPriceTv!!.text = CurrencyFormatter.format(finalTotal)
                        context.allTotalPrice = finalTotal
                    } else {
                        // Mã khuyến mãi vẫn hợp lệ
                        context.promoDescriptionTv!!.visibility = View.VISIBLE
                        context.applyBtn!!.visibility = View.VISIBLE
                        context.discountTv!!.text = CurrencyFormatter.format(promoDiscount)
                        context.promoDescriptionTv!!.text = context.promoDescription
                        val finalTotal = newSubTotal + deliveryFeeDouble - promoDiscount
                        context.allTotalPriceTv!!.text = CurrencyFormatter.format(finalTotal)
                        context.allTotalPrice = finalTotal
                    }
                } else {
                    // Không có mã khuyến mãi
                    val finalTotal = newSubTotal + deliveryFeeDouble
                    context.allTotalPriceTv!!.text = CurrencyFormatter.format(finalTotal)
                    context.allTotalPrice = finalTotal
                }

                // Cập nhật số lượng giỏ hàng
                context.cartCount()
            }
        }

    }
    private fun parseCurrency(value: String): Double {
        return value.replace("[^\\d]".toRegex(), "").toDoubleOrNull() ?: 0.0
    }


    override fun getItemCount(): Int {
        return cartItems.size //return number of records
    }

    //view holder class
    class HolderCartItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //ui views of row_cartitems.xml
        val itemTitleTv: TextView
        val itemPriceTv: TextView // Total price for this specific cart item (cost * quantity)
        val itemPriceEachTv: TextView // Price per unit
        val itemQuantityTv: TextView
        val itemRemoveTv: TextView

        init {
            //init views
            itemTitleTv = itemView.findViewById(R.id.itemTitleTv)
            itemPriceTv = itemView.findViewById(R.id.itemPriceTv)
            itemPriceEachTv = itemView.findViewById(R.id.itemPriceEachTv)
            itemQuantityTv = itemView.findViewById(R.id.itemQuantityTv)
            itemRemoveTv = itemView.findViewById(R.id.itemRemoveTv)
        }
    }
}