package com.example.grocery.models

class ModelCartItem {
    var id: String? = null
    var pId: String? = null
    var name: String? = null
    var price: String? = null
    var cost: String? = null
    var quantity: String? = null

    constructor()
    constructor(
        id: String?,
        pId: String?,
        name: String?,
        price: String?,
        cost: String?,
        quantity: String?
    ) {
        this.id = id
        this.pId = pId
        this.name = name
        this.price = price
        this.cost = cost
        this.quantity = quantity
    }

}
