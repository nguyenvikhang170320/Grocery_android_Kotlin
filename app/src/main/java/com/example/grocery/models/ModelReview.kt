package com.example.grocery.models

class ModelReview {
    //use same spellings of variables as used in sending to firebase
    @JvmField
    var uid: String? = null
    @JvmField
    var ratings: String? = null
    @JvmField
    var review: String? = null
    @JvmField
    var timestamp: String? = null

    constructor()
    constructor(uid: String?, ratings: String?, review: String?, timestamp: String?) {
        this.uid = uid
        this.ratings = ratings
        this.review = review
        this.timestamp = timestamp
    }
}
