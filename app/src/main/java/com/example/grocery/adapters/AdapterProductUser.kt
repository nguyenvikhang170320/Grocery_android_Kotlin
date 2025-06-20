package com.example.grocery.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.activities.ShopDetailsActivity
import com.example.grocery.adapters.AdapterProductUser.HolderProductUser
import com.example.grocery.filter.FilterProductUser
import com.example.grocery.models.ModelProduct
import com.example.grocery.R
import com.example.grocery.thumucquantrong.CurrencyFormatter
import com.squareup.picasso.Picasso
import p32929.androideasysql_library.Column
import p32929.androideasysql_library.EasyDB

class AdapterProductUser(private val context: Context, var productsList: ArrayList<ModelProduct?>?) :
    RecyclerView.Adapter<HolderProductUser>(), Filterable {

    var filterList: ArrayList<ModelProduct?>?
    private var filter: FilterProductUser? = null

    init {
        filterList = productsList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderProductUser {
        //inflate layout
        val view = LayoutInflater.from(context).inflate(R.layout.row_product_user, parent, false)
        return HolderProductUser(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HolderProductUser, position: Int) {
        //get data
        val modelProduct = productsList!![position]
        val discountAvailable = modelProduct?.discountAvailable
        val discountNote = modelProduct?.discountNote
        val discountPriceString = modelProduct?.discountPrice // Raw string from ModelProduct
        val productIcon = modelProduct?.productIcon
        val quantity = modelProduct?.productQuantity
        val productTitle = modelProduct?.productTitle
        val originalPriceString = modelProduct?.originalPrice // Raw string from ModelProduct

        // Get the CurrencyFormatter instance (already defined as object, so no need to call .formatter if just using for format)
        // val currencyFormatter = CurrencyFormatter.formatter // Can remove this line and just call CurrencyFormatter.format directly

        // *** CRITICAL: Parse incoming ModelProduct strings using toDoubleOrNull() ***
        // Assuming originalPriceString and discountPriceString are "30000" or "30000.0"
        val originalPriceDouble = originalPriceString?.toDoubleOrNull() ?: 0.0
        val discountPriceDouble = discountPriceString?.toDoubleOrNull() ?: 0.0


        //set data
        holder.descriptionTv.text = "Mô tả: ${modelProduct?.productDescription ?: "N/A"}"
        holder.titleTv.text = "Sản phẩm: ${productTitle ?: "N/A"}"
        holder.quantityTv.text = "Số lượng: ${quantity ?: "N/A"}"

        // Định dạng và hiển thị giá
        if (discountAvailable == "true") {
            // Sản phẩm đang giảm giá
            holder.discountedPriceTv.text = "Giá giảm giá: " + CurrencyFormatter.format(discountPriceDouble)
            holder.discountedPriceTv.visibility = View.VISIBLE // Đảm bảo giá giảm hiển thị

            holder.originalPriceTv.text = "Giá gốc: " + CurrencyFormatter.format(originalPriceDouble)
            holder.originalPriceTv.paintFlags = holder.originalPriceTv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG // Gạch ngang giá gốc
            holder.originalPriceTv.visibility = View.VISIBLE // Đảm bảo giá gốc hiển thị

            holder.discountedNoteTv.text = discountNote // Hiển thị ghi chú giảm giá (e.g., "10% OFF")
            holder.discountedNoteTv.visibility = View.VISIBLE // Đảm bảo ghi chú giảm giá hiển thị
        } else {
            // Sản phẩm không giảm giá
            holder.discountedPriceTv.visibility = View.GONE // Ẩn giá giảm
            holder.discountedNoteTv.visibility = View.GONE // Ẩn ghi chú giảm giá

            holder.originalPriceTv.text = "Giá gốc: " + CurrencyFormatter.format(originalPriceDouble) // Hiển thị giá gốc là giá chính
            holder.originalPriceTv.paintFlags = 0 // Đảm bảo không có gạch ngang
            holder.originalPriceTv.visibility = View.VISIBLE // Đảm bảo giá gốc hiển thị
        }
        try {
            Picasso.get().load(productIcon).placeholder(R.drawable.store).into(holder.productIconIv)
        } catch (e: Exception) {
            holder.productIconIv.setImageResource(R.drawable.store)
        }
        holder.addToCartTv.setOnClickListener { v: View? ->
            //add product to cart
            showQuantityDialog(modelProduct!!)
        }
        // No need for itemView.setOnClickListener if it's empty
        // holder.itemView.setOnClickListener { v: View? -> }
    }

    private var cost = 0.0
    private var finalCost = 0.0
    private var quantity = 0

    @SuppressLint("SetTextI18n")
    private fun showQuantityDialog(modelProduct: ModelProduct) {
        //inflate layout for dialog
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_quantity, null)
        //init layout views
        val productIv = view.findViewById<ImageView>(R.id.productIv)
        val titleTv = view.findViewById<TextView>(R.id.titleTv)
        val pQuantityTv = view.findViewById<TextView>(R.id.pQuantityTv)
        val descriptionTv = view.findViewById<TextView>(R.id.descriptionTv)
        val discountedNoteTv = view.findViewById<TextView>(R.id.discountedNoteTv)
        val originalPriceTv = view.findViewById<TextView>(R.id.originalPriceTv)
        val priceDiscountedTv = view.findViewById<TextView>(R.id.priceDiscountedTv) // This is discountedPriceTv in product_details layout
        val finalPriceTv = view.findViewById<TextView>(R.id.finalPriceTv)
        val decrementBtn = view.findViewById<ImageButton>(R.id.decrementBtn)
        val quantityTv = view.findViewById<TextView>(R.id.quantityTv)
        val incrementBtn = view.findViewById<ImageButton>(R.id.incrementBtn)
        val continueBtn = view.findViewById<Button>(R.id.continueBtn)

        //get data from modelProduct
        val productId = modelProduct.productId
        val title = modelProduct.productTitle
        val productQuantity = modelProduct.productQuantity
        val description = modelProduct.productDescription
        val discountNote = modelProduct.discountNote // E.g., "10% OFF"
        val image = modelProduct.productIcon

        // Get the CurrencyFormatter instance (no need for .formatter if just using for format)
        // val currencyFormatter = CurrencyFormatter.formatter

        // *** CRITICAL: Parse incoming ModelProduct strings using toDoubleOrNull() ***
        val originalPriceDialogDouble = modelProduct.originalPrice?.toDoubleOrNull() ?: 0.0
        val discountPriceDialogDouble = modelProduct.discountPrice?.toDoubleOrNull() ?: 0.0

        // Handle price display based on discount availability
        if (modelProduct.discountAvailable == "true") {
            // Product has discount
            discountedNoteTv.text = discountNote // Assign the discount note directly
            discountedNoteTv.visibility = View.VISIBLE

            originalPriceTv.text = CurrencyFormatter.format(originalPriceDialogDouble) // Ensure Double is passed to format
            originalPriceTv.paintFlags = originalPriceTv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG // Strike through
            originalPriceTv.visibility = View.VISIBLE

            priceDiscountedTv.text = CurrencyFormatter.format(discountPriceDialogDouble) // Ensure Double is passed
            priceDiscountedTv.visibility = View.VISIBLE

            cost = discountPriceDialogDouble // Price per item for calculation is discounted price
        } else {
            // Product doesn't have discount
            discountedNoteTv.visibility = View.GONE
            priceDiscountedTv.visibility = View.GONE // Hide discounted price TextView

            originalPriceTv.text = CurrencyFormatter.format(originalPriceDialogDouble)
            originalPriceTv.paintFlags = 0 // Remove strike-through
            originalPriceTv.visibility = View.VISIBLE

            cost = originalPriceDialogDouble // Price per item for calculation is original price
        }

        // Initialize finalCost and quantity
        finalCost = cost // Initial finalCost is cost of 1 product
        quantity = 1 // Initial quantity

        // Dialog setup
        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        // Set data to dialog views
        try {
            Picasso.get().load(image).placeholder(R.drawable.ic_cart_gray).into(productIv)
        } catch (e: Exception) {
            productIv.setImageResource(R.drawable.ic_cart_gray) // Fallback if image loading fails
        }

        titleTv.text = title
        pQuantityTv.text = "Số lượng có sẵn: $productQuantity"
        descriptionTv.text = description

        quantityTv.text = "$quantity" // Current quantity in dialog
        finalPriceTv.text = CurrencyFormatter.format(finalCost) // Initial final price (for 1 item)

        val dialog = builder.create()
        dialog.show()

        // Click listeners
        incrementBtn.setOnClickListener {
            // Add check to not exceed productQuantity if it's a numeric limit
            // For now, it increments without limit.
            finalCost += cost
            quantity++
            finalPriceTv.text = CurrencyFormatter.format(finalCost)
            quantityTv.text = "$quantity"
        }

        decrementBtn.setOnClickListener {
            if (quantity > 1) {
                finalCost -= cost
                quantity--
                finalPriceTv.text = CurrencyFormatter.format(finalCost)
                quantityTv.text = "$quantity"
            }
        }

        continueBtn.setOnClickListener {
            val title1 = titleTv.text.toString().trim()
            // Store the raw double values as strings for EasyDB
            // These will be "30000.0" or "60000.0" etc. which toDoubleOrNull can handle later
            val priceEachForDB = cost.toString()
            val totalPriceForDB = finalCost.toString()

            val quantityStr = quantityTv.text.toString().trim()

            //add to db(SQLite)
            addToCart(productId, title1, priceEachForDB, totalPriceForDB, quantityStr)
            dialog.dismiss()
        }
    }

    private var itemId = 1 // Consider initializing this more robustly if it needs to be unique across sessions

    private fun addToCart(
        productId: String?,
        title: String,
        priceEach: String?, // Now directly accepts the raw numeric string from cost.toString()
        price: String,       // Now directly accepts the raw numeric string from finalCost.toString()
        quantity: String
    ) {
        // Implement logic to increment itemId based on the highest existing Item_Id in DB,
        // or let EasyDB handle auto-increment if it supports it better.
        // For now, keeping your existing simple increment.
        itemId++

        val easyDB = EasyDB.init(context, "ITEMS_DB")
            .setTableName("ITEMS_TABLE")
            .addColumn(Column("Item_Id", "text", "unique"))
            .addColumn(Column("Item_PID", "text", "not null"))
            .addColumn(Column("Item_Name", "text", "not null"))
            .addColumn(Column("Item_Price_Each", "text", "not null"))
            .addColumn(Column("Item_Price", "text", "not null"))
            .addColumn(Column("Item_Quantity", "text", "not null"))
            .doneTableColumn()

        // You might want to add logic here to check if the product already exists in the cart.
        // If it does, you would update the quantity and total price instead of adding a new row.
        // This is a common cart feature not implemented in your current code.
        // For example:
        // val existingItem = easyDB.getOneRow(2, productId) // assuming Item_PID is column 2
        // if (existingItem != null) {
        //    // update quantity and price
        // } else {
        //    // add new item
        // }

        val b = easyDB.addData("Item_Id", itemId)
            .addData("Item_PID", productId)
            .addData("Item_Name", title)
            .addData("Item_Price_Each", priceEach) // These are now clean numerical strings (e.g., "30000.0")
            .addData("Item_Price", price)         // These are now clean numerical strings (e.g., "60000.0")
            .addData("Item_Quantity", quantity)
            .doneDataAdding()

        Toast.makeText(context, "Đã thêm vào giỏ hàng...", Toast.LENGTH_SHORT).show()

        //update cart count
        // Need to cast context to ShopDetailsActivity to call cartCount()
        if (context is ShopDetailsActivity) {
            context.cartCount()
        }
    }

    override fun getItemCount(): Int {
        return productsList!!.size
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterProductUser(this, filterList)
        }
        return filter!!
    }

    class HolderProductUser(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //ui views
        val productIconIv: ImageView
        val discountedNoteTv: TextView
        val titleTv: TextView
        val quantityTv: TextView
        val descriptionTv: TextView
        val addToCartTv: TextView
        val discountedPriceTv: TextView
        val originalPriceTv: TextView

        init {
            //init ui views
            productIconIv = itemView.findViewById(R.id.productIconIv)
            discountedNoteTv = itemView.findViewById(R.id.discountedNoteTv)
            titleTv = itemView.findViewById(R.id.titleTv)
            quantityTv = itemView.findViewById(R.id.quantityTv)
            descriptionTv = itemView.findViewById(R.id.descriptionTv)
            addToCartTv = itemView.findViewById(R.id.addToCartTv)
            discountedPriceTv = itemView.findViewById(R.id.discountedPriceTv)
            originalPriceTv = itemView.findViewById(R.id.originalPriceTv)
        }
    }
}