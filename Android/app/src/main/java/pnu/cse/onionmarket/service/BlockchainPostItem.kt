package pnu.cse.onionmarket.service

data class BlockchainPostItem(
    var userId: String? = null,
    var userNickname: String? = null,
    var userPhone: String? = null,
    var postId: String? = null,
    var postImageUrl: String? = null,
    var postTitle: String? = null,
    var postPrice: String? = null,
    var postContent: String? = null,
    var createdAt: Long? = null
)
