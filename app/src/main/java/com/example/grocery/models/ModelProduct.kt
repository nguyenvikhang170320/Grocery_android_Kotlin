package com.example.grocery.models

class ModelProduct {
    @JvmField
    var productId: String? = null
    @JvmField
    var productTitle: String? = null
    @JvmField
    var productDescription: String? = null
    @JvmField
    var productCategory: String? = null
    @JvmField
    var productQuantity: String? = null
    @JvmField
    var productIcon: String? = null
    @JvmField
    var originalPrice: String? = null
    @JvmField
    var discountPrice: String? = null
    @JvmField
    var discountNote: String? = null
    @JvmField
    var discountAvailable: String? = null
    @JvmField
    var timestamp: String? = null
    @JvmField
    var uid: String? = null

    constructor()
    constructor(
        productId: String?,
        productTitle: String?,
        productDescription: String?,
        productCategory: String?,
        productQuantity: String?,
        productIcon: String?,
        originalPrice: String?,
        discountPrice: String?,
        discountNote: String?,
        discountAvailable: String?,
        timestamp: String?,
        uid: String?
    ) {
        this.productId = productId
        this.productTitle = productTitle
        this.productDescription = productDescription
        this.productCategory = productCategory
        this.productQuantity = productQuantity
        this.productIcon = productIcon
        this.originalPrice = originalPrice
        this.discountPrice = discountPrice
        this.discountNote = discountNote
        this.discountAvailable = discountAvailable
        this.timestamp = timestamp
        this.uid = uid
    }
}
