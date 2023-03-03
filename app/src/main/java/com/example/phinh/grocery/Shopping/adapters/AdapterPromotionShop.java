package com.example.phinh.grocery.Shopping.adapters;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.phinh.grocery.R;
import com.example.phinh.grocery.Shopping.activities.AddPromotionCodeActivity;
import com.example.phinh.grocery.Shopping.models.ModelPromotion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Objects;

public class AdapterPromotionShop extends RecyclerView.Adapter<AdapterPromotionShop.HolderPromotionShop> {

    private final Context context;
    private final ArrayList<ModelPromotion> promotionArrayList;

    private final ProgressDialog progressDialog;
    private final FirebaseAuth firebaseAuth;

    public AdapterPromotionShop(Context context, ArrayList<ModelPromotion> promotionArrayList) {
        this.context = context;
        this.promotionArrayList = promotionArrayList;

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);
    }

    @NonNull
    @Override
    public HolderPromotionShop onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout row_promotion_shop.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_promotion_shop, parent, false);

        return new HolderPromotionShop(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final HolderPromotionShop holder, int position) {
        //get data
        final ModelPromotion modelPromotion = promotionArrayList.get(position);
        String id = modelPromotion.getId();
        String timestamp = modelPromotion.getTimestamp();
        String description = modelPromotion.getDescription();
        String promoCode = modelPromotion.getPromoCode();
        String promoPrice = modelPromotion.getPromoPrice();
        String expireDate = modelPromotion.getExpireDate();
        String minimumOrderPrice = modelPromotion.getMinimumOrderPrice();

        //set data
        holder.descriptionTv.setText(description);
        holder.promoPriceTv.setText(promoPrice);
        holder.minimumOrderPriceTv.setText(minimumOrderPrice);
        holder.promoCodeTv.setText("Code: "+promoCode);
        holder.expireDateTv.setText("Expire Date: "+expireDate);

        /*handle click, show Edit/Delete dialog*/
        holder.itemView.setOnClickListener(view -> editDeleteDialog(modelPromotion, holder));
    }

    private void editDeleteDialog(final ModelPromotion modelPromotion, HolderPromotionShop holder) {
        //options to display in dialog
        String[] options = {"Chỉnh sửa", "Xóa"};
        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Vui lòng chọn các tùy chọn")
                .setItems(options, (dialogInterface, i) -> {
                    //handle clicks
                    if (i==0){
                        //Edit clicked
                        editPromoCode(modelPromotion);
                    }
                    else  if (i==1){
                        //Delete clicked
                        deletePromoCode(modelPromotion);
                    }
                })
                .show();
    }

    private void deletePromoCode(ModelPromotion modelPromotion) {
        //show progress bar
        progressDialog.setMessage("Xóa mã khuyến mãi...");
        progressDialog.show();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(Objects.requireNonNull(firebaseAuth.getUid())).child("Promotions").child(modelPromotion.getId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    //deleted
                    progressDialog.dismiss();
                    Toast.makeText(context, "Xóa thành công...", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    //failed deleting
                    progressDialog.dismiss();
                    Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void editPromoCode(ModelPromotion modelPromotion) {
        //start and pass data to AddPromotionCodeActivity to edit
        Intent intent = new Intent(context, AddPromotionCodeActivity.class);
        intent.putExtra("promoId", modelPromotion.getId()); //will use id to update promo code
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return promotionArrayList.size();
    }

    //view holder class
    static class HolderPromotionShop extends RecyclerView.ViewHolder{

        private final TextView promoCodeTv, promoPriceTv, minimumOrderPriceTv, expireDateTv, descriptionTv;

        public HolderPromotionShop(@NonNull View itemView) {
            super(itemView);

            //init ui views
            //views of row_promotion_shop.xml
            ImageView iconIv = itemView.findViewById(R.id.iconIv);
            promoCodeTv = itemView.findViewById(R.id.promoCodeTv);
            promoPriceTv = itemView.findViewById(R.id.promoPriceTv);
            minimumOrderPriceTv = itemView.findViewById(R.id.minimumOrderPriceTv);
            expireDateTv = itemView.findViewById(R.id.expireDateTv);
            descriptionTv = itemView.findViewById(R.id.descriptionTv);
        }
    }
}
