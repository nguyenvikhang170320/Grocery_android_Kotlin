package com.example.grocery.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.adapters.AdapterOrderedItem.HolderOrderedItem
import com.example.grocery.models.ModelOrderedItem
import com.example.grocery.R
import com.example.grocery.activities.CurrencyFormatter
import java.util.Formatter

class AdapterOrderedItem(
    private val context: Context,
    private val orderedItemArrayList: ArrayList<ModelOrderedItem?>?
) : RecyclerView.Adapter<HolderOrderedItem>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderOrderedItem {
        //inflate layout
        val view = LayoutInflater.from(context).inflate(R.layout.row_ordereditem, parent, false)
        return HolderOrderedItem(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HolderOrderedItem, position: Int) {

        //get data at position
        val modelOrderedItem = orderedItemArrayList!![position]
        val getpId = modelOrderedItem?.getpId()
        val name = modelOrderedItem?.name
        val cost = modelOrderedItem?.cost
        val price = modelOrderedItem?.price
        val quantity = modelOrderedItem?.quantity
        var currentFormatter = CurrencyFormatter.formatter
        val orderCostDouble = cost?.toDoubleOrNull() ?: 0.0
        val orderPriceDouble = price?.toDoubleOrNull() ?: 0.0

        // Định dạng lại chuỗi hiển thị số tiền
        val formattedOrderCost = CurrencyFormatter.format(orderCostDouble)
        val formattedOrderPrice = CurrencyFormatter.format(orderPriceDouble)
        //set data
        holder.itemTitleTv.text = name
        holder.itemPriceEachTv.text = formattedOrderPrice
        holder.itemPriceTv.text = formattedOrderCost
        holder.itemQuantityTv.text = "[$quantity]"
    }

    override fun getItemCount(): Int {
        return orderedItemArrayList!!.size //return list size
    }

    //view holder class
    class HolderOrderedItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //views of row_ordereditem.xml
        val itemTitleTv: TextView
        val itemPriceTv: TextView
        val itemPriceEachTv: TextView
        val itemQuantityTv: TextView

        init {

            //init views
            itemTitleTv = itemView.findViewById(R.id.itemTitleTv)
            itemPriceTv = itemView.findViewById(R.id.itemPriceTv)
            itemPriceEachTv = itemView.findViewById(R.id.itemPriceEachTv)
            itemQuantityTv = itemView.findViewById(R.id.itemQuantityTv)
        }
    }
}
