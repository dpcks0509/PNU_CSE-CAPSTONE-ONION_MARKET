package pnu.cse.onionmarket.service

data class BlockchainReviewItem(
    var userId: String? = null,
    var reviewId: String? = null,
    var reviewStar: String? = null,
    var reviewText: String? = null,
    var writerNickname: String? = null,
    var createdAt: Long? = null
)