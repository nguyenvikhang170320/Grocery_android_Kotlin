package com.example.grocery.filter

import android.annotation.SuppressLint
import android.widget.Filter
import com.example.grocery.adapters.AdapterProductSeller
import com.example.grocery.models.ModelProduct
import java.util.Locale

class FilterProduct(
    private val adapter: AdapterProductSeller,
    private val filterList: ArrayList<ModelProduct?>?
) : Filter() {
    override fun performFiltering(constraint: CharSequence): FilterResults {
        var constraint: CharSequence? = constraint
        val results = FilterResults()
        //validate data for search query
        if (constraint != null && constraint.length > 0) {
            //search filed not empty, searching something, perform search

            //change to upper case, to make case insensitive
            constraint = constraint.toString().uppercase(Locale.getDefault())
            //store our filtered list
            val filteredModels = ArrayList<ModelProduct>()
            for (i in filterList!!.indices) {
                //check, search by title and category
                if (filterList!![i]!!.productTitle!!.uppercase(Locale.getDefault())
                        .contains(constraint) ||
                    filterList[i]!!.productCategory!!.uppercase(Locale.getDefault())
                        .contains(constraint)
                ) {
                    //add filtered data to list
                    filteredModels.add(filterList[i]!!)
                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        } else {
            //search filed empty, not searching, return original/all/complete list
            results.count = filterList!!.size
            results.values = filterList
        }
        return results
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        adapter.productList = results.values as ArrayList<ModelProduct?>
        //refresh adapter
        adapter.notifyDataSetChanged()
    }
}
