package com.example.grocery.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.grocery.activities.ShopDetailsActivity;
import com.example.grocery.models.ModelCartItem;
import com.example.phinh.grocery.R;

import java.util.ArrayList;

import p32929.androideasysql_library.Column;
import p32929.androideasysql_library.EasyDB;

public class AdapterCartItem extends RecyclerView.Adapter<AdapterCartItem.HolderCartItem> {

    private final Context context;
    private final ArrayList<ModelCartItem> cartItems;

    public AdapterCartItem(Context context, ArrayList<ModelCartItem> cartItems) {
        this.context = context;
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public HolderCartItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout row_cartitem.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_cartitem, parent, false);
        return new HolderCartItem(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderCartItem holder, final int position) {
        //get data
        ModelCartItem modelCartItem = cartItems.get(position);
        final String id = modelCartItem.getId();
        String getpId = modelCartItem.getpId();
        String title = modelCartItem.getName();
        final String cost = modelCartItem.getCost();
        String price = modelCartItem.getPrice();
        String quantity = modelCartItem.getQuantity();

        //set data
        holder.itemTitleTv.setText(""+title);
        holder.itemPriceTv.setText(""+cost);
        holder.itemQuantityTv.setText("["+quantity+"]"); //e.g. [3]
        holder.itemPriceEachTv.setText(""+price);

        //handle remove click listener, delete item from cart
        holder.itemRemoveTv.setOnClickListener(v -> {
            //will create table if not exists, but in that case will must exist
            EasyDB easyDB = EasyDB.init(context, "ITEMS_DB")
                    .setTableName("ITEMS_TABLE")
                    .addColumn(new Column("Item_Id", "text", "unique"))
                    .addColumn(new Column("Item_PID", "text", "not null"))
                    .addColumn(new Column("Item_Name", "text", "not null"))
                    .addColumn(new Column("Item_Price_Each", "text", "not null"))
                    .addColumn(new Column("Item_Price", "text", "not null"))
                    .addColumn(new Column("Item_Quantity", "text", "not null"))
                    .doneTableColumn();

            easyDB.deleteRow(1, id); //column Number 1 is Item_Id
            Toast.makeText(context, "Đã xóa khỏi giỏ hàng...", Toast.LENGTH_SHORT).show();

            //refresh list
            cartItems.remove(position);
            notifyItemChanged(position);
            notifyDataSetChanged();

            //adjust the subtotal after product remove
            double subTotalWithoutDiscount = Double.parseDouble((((ShopDetailsActivity)context).allTotalPriceTv.getText().toString().trim().replace("$","")));
            double totalPrice = subTotalWithoutDiscount - Double.parseDouble(cost.replace("$",""));
            double deliveryFee = Double.parseDouble((((ShopDetailsActivity)context).deliveryFee.replace("$","")));
            double sTotalPrice = Double.parseDouble(String.format("%.2f",totalPrice)) - Double.parseDouble(String.format("%.2f",deliveryFee));
            ((ShopDetailsActivity)context).allTotalPrice = 0.00;
            ((ShopDetailsActivity)context).sTotalTv.setText("$"+String.format("%.2f",sTotalPrice));
            ((ShopDetailsActivity)context).allTotalPriceTv.setText("$"+String.format("%.2f",Double.parseDouble(String.format("%.2f",totalPrice))));
            //check if promo code applied
            if (((ShopDetailsActivity)context).isPromoCodeApplied){
                //applied
                if (totalPrice < Double.parseDouble(((ShopDetailsActivity)context).promoMinimumOrderPrice)){
                    //current order price is less then minimum required price
                    Toast.makeText(context, "Mã này hợp lệ cho đơn hàng với số tiền tối thiểu: $"+((ShopDetailsActivity)context).promoMinimumOrderPrice, Toast.LENGTH_SHORT).show();
                    ((ShopDetailsActivity)context).applyBtn.setVisibility(View.GONE);
                    ((ShopDetailsActivity)context).promoDescriptionTv.setVisibility(View.GONE);
                    ((ShopDetailsActivity)context).promoDescriptionTv.setText("");
                    ((ShopDetailsActivity)context).discountTv.setText("$0");
                    ((ShopDetailsActivity)context).isPromoCodeApplied = false;
                    //show new net total after delivery fee
                    ((ShopDetailsActivity)context).allTotalPriceTv.setText("$" + String.format("%.2f", Double.parseDouble(String.format("%.2f" , totalPrice + deliveryFee))));
                }
                else {
                    ((ShopDetailsActivity)context).applyBtn.setVisibility(View.VISIBLE);
                    ((ShopDetailsActivity)context).promoDescriptionTv.setVisibility(View.VISIBLE);
                    ((ShopDetailsActivity)context).promoDescriptionTv.setText(((ShopDetailsActivity)context).promoDescription);
                    //show new total price after adding delivery fee and subtracting promo fee
                    ((ShopDetailsActivity)context).isPromoCodeApplied = true;
                    ((ShopDetailsActivity)context).allTotalPriceTv.setText("$" + String.format("%.2f", Double.parseDouble(String.format("%.2f" , totalPrice + deliveryFee - sTotalPrice))));
                }
            }
            else {
                //not applied
                ((ShopDetailsActivity)context).allTotalPriceTv.setText("$" + String.format("%.2f", Double.parseDouble(String.format("%.2f", totalPrice + deliveryFee))));
            }

            //after removing item from cart, update cart count
            ((ShopDetailsActivity)context).cartCount();

        });

    }

    @Override
    public int getItemCount() {
        return cartItems.size(); //return number of records
    }

    //view holder class
    static class HolderCartItem extends RecyclerView.ViewHolder{

        //ui views of row_cartitems.xml
        private final TextView itemTitleTv;
        private final TextView itemPriceTv;
        private final TextView itemPriceEachTv;
        private final TextView itemQuantityTv;
        private final TextView itemRemoveTv;

        public HolderCartItem(@NonNull View itemView) {
            super(itemView);

            //init views
            itemTitleTv = itemView.findViewById(R.id.itemTitleTv);
            itemPriceTv = itemView.findViewById(R.id.itemPriceTv);
            itemPriceEachTv = itemView.findViewById(R.id.itemPriceEachTv);
            itemQuantityTv = itemView.findViewById(R.id.itemQuantityTv);
            itemRemoveTv = itemView.findViewById(R.id.itemRemoveTv);
        }
    }
}
