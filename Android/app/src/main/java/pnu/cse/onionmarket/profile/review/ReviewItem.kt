package pnu.cse.onionmarket.profile.review

data class ReviewItem(
    var reviewId: String? = null,
    val createdAt: Long? = null,
    val userId: String? = null,
    val userProfile: String? = null,
    val userName: String? = null,
    val reviewText: String? = null,
    val reviewStar: Double? = null
)
