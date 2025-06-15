package com.example.grocery.models

class ModelOrderShop {
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
    var latitude: Double? = null
    var longitude: Double? = null
    var deliveryFee: String? = null

    constructor()
    constructor(
        orderId: String?,
        orderTime: String?,
        orderStatus: String?,
        orderCost: String?,
        orderBy: String?,
        orderTo: String?,
        latitude: Double?,
        longitude: Double?,
        deliveryFee: String?
    ) {
        this.orderId = orderId
        this.orderTime = orderTime
        this.orderStatus = orderStatus
        this.orderCost = orderCost
        this.orderBy = orderBy
        this.orderTo = orderTo
        this.latitude = latitude
        this.longitude = longitude
        this.deliveryFee = deliveryFee
    }
}
