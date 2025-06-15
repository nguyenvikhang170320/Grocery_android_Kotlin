package com.example.grocery.models

class ModelOrderUser {
    @JvmField
    var orderId: String? = null
    @JvmField
    var orderTime: String? = null
    @JvmField
    var orderStatus: String? = null
    @JvmField
    var orderCost: String? = null
    @JvmField
    var orderBy: String? = null
    @JvmField
    var orderTo: String? = null

    constructor()
    constructor(
        orderId: String?,
        orderTime: String?,
        orderStatus: String?,
        orderCost: String?,
        orderBy: String?,
        orderTo: String?
    ) {
        this.orderId = orderId
        this.orderTime = orderTime
        this.orderStatus = orderStatus
        this.orderCost = orderCost
        this.orderBy = orderBy
        this.orderTo = orderTo
    }
}
