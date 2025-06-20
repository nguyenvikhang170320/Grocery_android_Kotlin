package com.example.grocery.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.activities.EditProductActivity
import com.example.grocery.adapters.AdapterProductSeller.HolderProductSeller
import com.example.grocery.filter.FilterProduct
import com.example.grocery.models.ModelProduct
import com.example.grocery.R
import com.example.grocery.thumucquantrong.CurrencyFormatter // Ensure this import is correct
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import java.util.Objects

class AdapterProductSeller(private val context: Context, var productList: ArrayList<ModelProduct?>?) :
    RecyclerView.Adapter<HolderProductSeller>(), Filterable {
    var filterList: ArrayList<ModelProduct?>?
    private var filter: FilterProduct? = null

    init {
        filterList = productList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderProductSeller {
        //inflate layout
        val view = LayoutInflater.from(context).inflate(R.layout.row_product_seller, parent, false)
        return HolderProductSeller(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HolderProductSeller, position: Int) {
        //get data
        val modelProduct = productList!![position]
        val id = modelProduct?.productId
        // val uid = modelProduct?.uid // Not used, can remove
        val discountAvailable = modelProduct?.discountAvailable
        val discountNote = modelProduct?.discountNote
        val discountPrice = modelProduct?.discountPrice
        val productCategory = modelProduct?.productCategory
        val productDescription = modelProduct?.productDescription
        val productIcon = modelProduct?.productIcon
        val quantity = modelProduct?.productQuantity
        val productTitle = modelProduct?.productTitle
        // val timestamp = modelProduct?.timestamp // Not used, can remove
        val originalPrice = modelProduct?.originalPrice

        // Get the CurrencyFormatter instance
        val currencyFormatter = CurrencyFormatter.formatter

        // Safely convert prices to Double
        // Using a regex to remove any non-digit/non-dot characters (like currency symbols or commas)
        // This makes parsing more robust.
        val originalPriceDouble = originalPrice?.replace("[^\\d.]".toRegex(), "")?.toDoubleOrNull() ?: 0.0
        val discountPriceDouble = discountPrice?.replace("[^\\d.]".toRegex(), "")?.toDoubleOrNull() ?: 0.0

        //set data
        holder.descriptionTv.text = "Mô tả: ${productDescription ?: "N/A"}" // Use null-safe Elvis operator
        holder.titleTv.text = "Sản phầm: ${productTitle ?: "N/A"}"
        holder.quantityTv.text = "Số lượng: ${quantity ?: "N/A"}" // Ensure quantity is handled if null

        // Format and display price
        if (discountAvailable == "true") {
            // Product has discount
            holder.discountedPriceTv.text = "Giá giảm giá: "+currencyFormatter.format(discountPriceDouble)
            holder.discountedPriceTv.visibility = View.VISIBLE // Ensure discounted price is visible

            holder.originalPriceTv.text = "Giá gốc: "+currencyFormatter.format(originalPriceDouble)
            holder.originalPriceTv.paintFlags = holder.originalPriceTv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG // Strike through original price
            holder.originalPriceTv.visibility = View.VISIBLE // Ensure original price is visible

            holder.discountedNoteTv.text = "$discountNote%" // Display discount note (percentage)
            holder.discountedNoteTv.visibility = View.VISIBLE // Ensure discount note is visible
        } else {
            // Product does not have discount
            holder.discountedPriceTv.visibility = View.GONE // Hide discounted price
            holder.discountedNoteTv.visibility = View.GONE // Hide discount note

            holder.originalPriceTv.text = "Giá gốc: "+currencyFormatter.format(originalPriceDouble) // Display original price as main price
            holder.originalPriceTv.paintFlags = 0 // Remove strike-through if present
            holder.originalPriceTv.visibility = View.VISIBLE // Ensure original price is visible
        }

        try {
            Picasso.get().load(productIcon).placeholder(R.drawable.store).into(holder.productIconIv)
        } catch (e: Exception) {
            holder.productIconIv.setImageResource(R.drawable.store)
        }
        holder.itemView.setOnClickListener { v: View? ->
            //handle item clicks, show item details (in bottom sheet)
            detailsBottomSheet(modelProduct!!) //here modelProduct contains details of clicked product
        }
    }

    @SuppressLint("SetTextI18n")
    private fun detailsBottomSheet(modelProduct: ModelProduct) {
        //bottom sheet
        val bottomSheetDialog = BottomSheetDialog(context)
        //inflate view for bottomsheet
        @SuppressLint("InflateParams") val view =
            LayoutInflater.from(context).inflate(R.layout.bs_product_details_seller, null)
        //set view to bottomsheet
        bottomSheetDialog.setContentView(view)

        //init views of bottomsheet
        val backBtn = view.findViewById<ImageButton>(R.id.backBtn)
        val deleteBtn = view.findViewById<ImageButton>(R.id.deleteBtn)
        val editBtn = view.findViewById<ImageButton>(R.id.editBtn)
        val productIconIv = view.findViewById<ImageView>(R.id.productIconIv)
        val discountNoteTv = view.findViewById<TextView>(R.id.discountNoteTv)
        val titleTv = view.findViewById<TextView>(R.id.titleTv)
        val descriptionTv = view.findViewById<TextView>(R.id.descriptionTv)
        val categoryTv = view.findViewById<TextView>(R.id.categoryTv)
        val quantityTv = view.findViewById<TextView>(R.id.quantityTv)
        val discountedPriceTv = view.findViewById<TextView>(R.id.discountedPriceTv)
        val originalPriceTv = view.findViewById<TextView>(R.id.originalPriceTv)

        //get data from modelProduct
        val id = modelProduct.productId
        // val uid = modelProduct.uid // Not used
        val discountAvailable = modelProduct.discountAvailable
        val discountNote = modelProduct.discountNote
        val discountPrice = modelProduct.discountPrice
        val productCategory = modelProduct.productCategory
        val productDescription = modelProduct.productDescription
        val icon = modelProduct.productIcon
        val quantity = modelProduct.productQuantity
        val title = modelProduct.productTitle
        // val timestamp = modelProduct.timestamp // Not used
        val originalPrice = modelProduct.originalPrice

        val currencyFormatter = CurrencyFormatter.formatter // Get the formatter instance

        // Safely parse prices to Double
        val originalPriceDouble = originalPrice?.replace("[^\\d.]".toRegex(), "")?.toDoubleOrNull() ?: 0.0
        val discountedPriceDouble = discountPrice?.replace("[^\\d.]".toRegex(), "")?.toDoubleOrNull() ?: 0.0

        // Set common data that doesn't depend on discount
        titleTv.text = "Sản phẩm: "+title
        descriptionTv.text = "Mô tả: "+productDescription
        categoryTv.text = "Loại: "+productCategory
        quantityTv.text = "Số lượng: "+quantity

        // Handle price display based on discount availability
        if (discountAvailable == "true") {
            // Product has discount
            discountNoteTv.text = "$discountNote%" // Display discount percentage
            discountNoteTv.visibility = View.VISIBLE

            discountedPriceTv.text = "Giá giảm giá: "+currencyFormatter.format(discountedPriceDouble)
            discountedPriceTv.visibility = View.VISIBLE

            originalPriceTv.text = "Giá gốc: "+currencyFormatter.format(originalPriceDouble)
            originalPriceTv.paintFlags = originalPriceTv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG // Strike through original price
            originalPriceTv.visibility = View.VISIBLE
        } else {
            // Product does not have discount
            discountNoteTv.visibility = View.GONE // Hide discount note
            discountedPriceTv.visibility = View.GONE // Hide discounted price

            originalPriceTv.text = "Giá gốc: "+currencyFormatter.format(originalPriceDouble) // Display original price as main price
            originalPriceTv.paintFlags = 0 // Remove strike-through
            originalPriceTv.visibility = View.VISIBLE
        }

        try {
            Picasso.get().load(icon).placeholder(R.drawable.store).into(productIconIv)
        } catch (e: Exception) {
            productIconIv.setImageResource(R.drawable.store)
        }

        //show dialog
        bottomSheetDialog.show()

        //edit click
        editBtn.setOnClickListener { v: View? ->
            bottomSheetDialog.dismiss()
            //open edit product activivity, pass id of product
            val intent = Intent(context, EditProductActivity::class.java)
            intent.putExtra("productId", id)
            context.startActivity(intent)
        }
        //delete click
        deleteBtn.setOnClickListener { v: View? ->
            bottomSheetDialog.dismiss()
            //show delete confirm dialog
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm không $title ?")
                .setPositiveButton("DELETE") { dialog: DialogInterface?, which: Int ->
                    //delete
                    deleteProduct(id) //id is the product id
                }
                .setNegativeButton("NO") { dialog: DialogInterface, which: Int ->
                    //cancel, dismiss dialog
                    dialog.dismiss()
                }
                .show()
        }
        //back click
        backBtn.setOnClickListener { v: View? ->
            //dismiss bottom sheet
            bottomSheetDialog.dismiss()
        }
    }

    private fun deleteProduct(id: String?) {
        //delete product using its id
        val firebaseAuth = FirebaseAuth.getInstance()
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(Objects.requireNonNull(firebaseAuth.uid).toString()).child("Products").child(
            id!!
        ).removeValue()
            .addOnSuccessListener { aVoid: Void? ->
                //product deleted
                Toast.makeText(context, "Sản phẩm đã bị xóa...", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e: Exception ->
                //failed deleting product
                Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int {
        return productList!!.size
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterProduct(this, filterList)
        }
        return filter!!
    }

    class HolderProductSeller(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /*holds views of recyclerview*/
        val productIconIv: ImageView
        val discountedNoteTv: TextView
        val titleTv: TextView
        val descriptionTv: TextView
        val quantityTv: TextView
        val discountedPriceTv: TextView
        val originalPriceTv: TextView

        init {
            productIconIv = itemView.findViewById(R.id.productIconIv)
            discountedNoteTv = itemView.findViewById(R.id.discountedNoteTv)
            descriptionTv = itemView.findViewById(R.id.descriptionTv)
            titleTv = itemView.findViewById(R.id.titleTv)
            quantityTv = itemView.findViewById(R.id.quantityTv)
            discountedPriceTv = itemView.findViewById(R.id.discountedPriceTv)
            originalPriceTv = itemView.findViewById(R.id.originalPriceTv)
        }
    }
}