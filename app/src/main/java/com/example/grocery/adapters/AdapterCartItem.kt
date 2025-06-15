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
import com.example.grocery.activities.CurrencyFormatter // Import your CurrencyFormatter
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
            //will create table if not exists, but in that case will must exist
            val easyDB = EasyDB.init(context, "ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(Column("Item_Id", "text", "unique"))
                .addColumn(Column("Item_PID", "text", "not null"))
                .addColumn(Column("Item_Name", "text", "not null"))
                .addColumn(Column("Item_Price_Each", "text", "not null"))
                .addColumn(Column("Item_Price", "text", "not null")) // This stores total price for item
                .addColumn(Column("Item_Quantity", "text", "not null"))
                .doneTableColumn()

            easyDB.deleteRow(1, id) //column Number 1 is Item_Id
            Toast.makeText(context, "Đã xóa khỏi giỏ hàng...", Toast.LENGTH_SHORT).show()

            // Remove item from the list and update RecyclerView
            cartItems.removeAt(position)
            notifyItemRemoved(position) // Use notifyItemRemoved for better animation
            // notifyDataSetChanged() // No longer strictly needed after notifyItemRemoved for this scenario, but safe if other things change

            // --- Adjust the subtotal after product remove ---
            // Ensure context is ShopDetailsActivity
            if (context is ShopDetailsActivity) {
                // Get current total price from TextView and parse it safely
                val currentSubTotalString = context.allTotalPriceTv!!.text.toString().trim()
                // Sử dụng toDoubleOrNull() trực tiếp
                val currentSubTotalWithoutDiscount = currentSubTotalString.toDoubleOrNull() ?: 0.0

                // Get delivery fee and parse it safely
                // context.deliveryFee là String, parse trực tiếp
                val deliveryFeeDouble = context.deliveryFee?.toDoubleOrNull() ?: 0.0

                // Calculate new total after removing the current item's cost
                // 'costDouble' is the total cost of the item being removed
                var newTotalPrice = currentSubTotalWithoutDiscount - costDouble


                // Update ShopDetailsActivity's allTotalPrice variable
                context.allTotalPrice = newTotalPrice // This now holds the actual total after item removal and including delivery fee

                // Update Subtotal (sTotalTv) - This should be the price of items only, without delivery fee
                // If newTotalPrice currently includes delivery fee, then subtract it to get just the items' subtotal
                val subtotalItemsOnly = newTotalPrice - deliveryFeeDouble // Assuming newTotalPrice includes delivery fee here
                context.sTotalTv!!.text = CurrencyFormatter.format(subtotalItemsOnly)


                // --- Check if promo code applied and recalculate ---
                if (context.isPromoCodeApplied) {
                    // Applied
                    val promoMinimumOrderPriceDouble = context.promoMinimumOrderPrice?.toDoubleOrNull() ?: 0.0
                    // context.promoPrice là String, parse trực tiếp
                    val promoPriceDouble = context.promoPrice?.toDoubleOrNull() ?: 0.0 // Assuming promoPrice is a numeric string


                    if (subtotalItemsOnly < promoMinimumOrderPriceDouble) { // Check subtotal (items only) against minimum order price
                        // Current order price (items only) is less than minimum required price for promo
                        Toast.makeText(
                            context,
                            "Mã này hợp lệ cho đơn hàng với số tiền tối thiểu: ${CurrencyFormatter.format(promoMinimumOrderPriceDouble)}",
                            Toast.LENGTH_SHORT
                        ).show()
                        context.applyBtn!!.visibility = View.GONE
                        context.promoDescriptionTv!!.visibility = View.GONE
                        context.promoDescriptionTv!!.text = "" // Clear description
                        context.discountTv!!.text = CurrencyFormatter.format(0.0) // Show 0 discount
                        context.isPromoCodeApplied = false

                        // Show new net total after delivery fee (no promo discount)
                        val finalTotalWithoutPromo = subtotalItemsOnly + deliveryFeeDouble
                        context.allTotalPriceTv!!.text = CurrencyFormatter.format(finalTotalWithoutPromo)
                    } else {
                        // Promo still applicable
                        context.applyBtn!!.visibility = View.VISIBLE
                        context.promoDescriptionTv!!.visibility = View.VISIBLE
                        context.promoDescriptionTv!!.text = context.promoDescription // Restore description
                        context.discountTv!!.text = CurrencyFormatter.format(promoPriceDouble) // Show discount amount

                        // Show new total price after adding delivery fee and subtracting promo fee
                        val finalTotalWithPromo = subtotalItemsOnly + deliveryFeeDouble - promoPriceDouble
                        context.allTotalPriceTv!!.text = CurrencyFormatter.format(finalTotalWithPromo)
                    }
                } else {
                    // Not applied (no promo)
                    val finalTotalWithoutPromo = subtotalItemsOnly + deliveryFeeDouble
                    context.allTotalPriceTv!!.text = CurrencyFormatter.format(finalTotalWithoutPromo)
                }
            }

            // After removing item from cart, update cart count
            // Also add a safety check for context type here
            if (context is ShopDetailsActivity) {
                context.cartCount()
            }
        }
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