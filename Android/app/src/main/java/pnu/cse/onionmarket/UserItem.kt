package pnu.cse.onionmarket

data class UserItem(
    val userId: String? = null,
    val userNickname: String? = null,
    val userPhone: String? = null,
    val userStar: Double? = 0.0,
    val userToken: String? = null,
    var userProfileImage: String? = null
)
