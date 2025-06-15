package com.example.grocery.models

class ModelPromotion {
    @JvmField
    var id: String? = null
    @JvmField
    var timestamp: String? = null
    @JvmField
    var description: String? = null
    @JvmField
    var promoCode: String? = null
    @JvmField
    var promoPrice: String? = null
    @JvmField
    var minimumOrderPrice: String? = null
    @JvmField
    var expireDate: String? = null

    constructor()
    constructor(
        id: String?,
        timestamp: String?,
        description: String?,
        promoCode: String?,
        promoPrice: String?,
        minimumOrderPrice: String?,
        expireDate: String?
    ) {
        this.id = id
        this.timestamp = timestamp
        this.description = description
        this.promoCode = promoCode
        this.promoPrice = promoPrice
        this.minimumOrderPrice = minimumOrderPrice
        this.expireDate = expireDate
    }
}
