package com.example.grocery.models

class ModelOrderedItem {
    private var pId: String? = null
    @JvmField
    var name: String? = null
    @JvmField
    var cost: String? = null
    @JvmField
    var price: String? = null
    @JvmField
    var quantity: String? = null

    constructor()
    constructor(pId: String?, name: String?, cost: String?, price: String?, quantity: String?) {
        this.pId = pId
        this.name = name
        this.cost = cost
        this.price = price
        this.quantity = quantity
    }

    fun getpId(): String? {
        return pId
    }

    fun setpId(pId: String?) {
        this.pId = pId
    }
}
