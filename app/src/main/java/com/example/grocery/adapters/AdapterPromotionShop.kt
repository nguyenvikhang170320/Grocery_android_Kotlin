package com.example.grocery.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.grocery.activities.AddPromotionCodeActivity
import com.example.grocery.adapters.AdapterPromotionShop.HolderPromotionShop
import com.example.grocery.models.ModelPromotion
import com.example.grocery.R
import com.example.grocery.activities.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Objects

class AdapterPromotionShop(
    private val context: Context,
    private val promotionArrayList: ArrayList<ModelPromotion?>?
) : RecyclerView.Adapter<HolderPromotionShop>() {
    private val progressDialog: ProgressDialog
    private val firebaseAuth: FirebaseAuth

    init {
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Vui lòng đợi")
        progressDialog.setCanceledOnTouchOutside(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPromotionShop {
        //inflate layout row_promotion_shop.xml
        val view = LayoutInflater.from(context).inflate(R.layout.row_promotion_shop, parent, false)
        return HolderPromotionShop(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HolderPromotionShop, position: Int) {
        //get data
        val modelPromotion = promotionArrayList!![position]
        val id = modelPromotion?.id
        val timestamp = modelPromotion?.timestamp
        val description = modelPromotion?.description
        val promoCode = modelPromotion?.promoCode
        val promoPrice = modelPromotion?.promoPrice
        val expireDate = modelPromotion?.expireDate
        val minimumOrderPrice = modelPromotion?.minimumOrderPrice

// Assuming promoPrice and minimumOrderPrice are Strings and might contain "$"
// If they are already Double, you can simplify the parsing part.

        val promoPriceDouble = promoPrice?.replace("$", "")?.toDoubleOrNull() ?: 0.0
        holder.promoPriceTv.text = CurrencyFormatter.format(promoPriceDouble)

        val minimumOrderPriceDouble = minimumOrderPrice?.replace("$", "")?.toDoubleOrNull() ?: 0.0
        holder.minimumOrderPriceTv.text = CurrencyFormatter.format(minimumOrderPriceDouble)

        //set data
        holder.descriptionTv.text = description
        holder.promoCodeTv.text = "Mã số: $promoCode"
        holder.expireDateTv.text = "Ngày hết hạn: $expireDate"

        /*handle click, show Edit/Delete dialog*/holder.itemView.setOnClickListener { view: View? ->
            editDeleteDialog(
                modelPromotion!!,
                holder
            )
        }
    }

    private fun editDeleteDialog(modelPromotion: ModelPromotion, holder: HolderPromotionShop) {
        //options to display in dialog
        val options = arrayOf("Chỉnh sửa", "Xóa")
        //dialog
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Vui lòng chọn các tùy chọn")
            .setItems(options) { dialogInterface: DialogInterface?, i: Int ->
                //handle clicks
                if (i == 0) {
                    //Edit clicked
                    editPromoCode(modelPromotion)
                } else if (i == 1) {
                    //Delete clicked
                    deletePromoCode(modelPromotion)
                }
            }
            .show()
    }

    private fun deletePromoCode(modelPromotion: ModelPromotion) {
        //show progress bar
        progressDialog.setMessage("Xóa mã khuyến mãi...")
        progressDialog.show()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(Objects.requireNonNull(firebaseAuth.uid).toString()).child("Promotions").child(
            modelPromotion.id!!
        )
            .removeValue()
            .addOnSuccessListener { aVoid: Void? ->
                //deleted
                progressDialog.dismiss()
                Toast.makeText(context, "Xóa thành công...", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e: Exception ->
                //failed deleting
                progressDialog.dismiss()
                Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun editPromoCode(modelPromotion: ModelPromotion) {
        //start and pass data to AddPromotionCodeActivity to edit
        val intent = Intent(context, AddPromotionCodeActivity::class.java)
        intent.putExtra("promoId", modelPromotion.id) //will use id to update promo code
        context.startActivity(intent)
    }

    override fun getItemCount(): Int {
        return promotionArrayList!!.size
    }

    //view holder class
    class HolderPromotionShop(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val promoCodeTv: TextView
        val promoPriceTv: TextView
        val minimumOrderPriceTv: TextView
        val expireDateTv: TextView
        val descriptionTv: TextView

        init {

            //init ui views
            //views of row_promotion_shop.xml
            val iconIv = itemView.findViewById<ImageView>(R.id.iconIv)
            promoCodeTv = itemView.findViewById(R.id.promoCodeTv)
            promoPriceTv = itemView.findViewById(R.id.promoPriceTv)
            minimumOrderPriceTv = itemView.findViewById(R.id.minimumOrderPriceTv)
            expireDateTv = itemView.findViewById(R.id.expireDateTv)
            descriptionTv = itemView.findViewById(R.id.descriptionTv)
        }
    }
}
