package com.example.grocery.models

class ModelShop {
    @JvmField
    var uid: String? = null
    var email: String? = null
    var password: String? = null
    var name: String? = null
    @JvmField
    var shopName: String? = null
    @JvmField
    var phone: String? = null
    var deliveryFee: String? = null
    var country: String? = null
    var state: String? = null
    var city: String? = null
    @JvmField
    var address: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var timestamp: String? = null
    var accountType: String? = null
    @JvmField
    var online: Boolean? = null
    @JvmField
    var shopOpen: Boolean? = null
    @JvmField
    var profileImage: String? = null

    constructor()
    constructor(
        uid: String?,
        email: String?,
        password: String?,
        name: String?,
        shopName: String?,
        phone: String?,
        deliveryFee: String?,
        country: String?,
        state: String?,
        city: String?,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        timestamp: String?,
        accountType: String?,
        online: Boolean?,
        shopOpen: Boolean?,
        profileImage: String?
    ) {
        this.uid = uid
        this.email = email
        this.password = password
        this.name = name
        this.shopName = shopName
        this.phone = phone
        this.deliveryFee = deliveryFee
        this.country = country
        this.state = state
        this.city = city
        this.address = address
        this.latitude = latitude
        this.longitude = longitude
        this.timestamp = timestamp
        this.accountType = accountType
        this.online = online
        this.shopOpen = shopOpen
        this.profileImage = profileImage
    }
}
