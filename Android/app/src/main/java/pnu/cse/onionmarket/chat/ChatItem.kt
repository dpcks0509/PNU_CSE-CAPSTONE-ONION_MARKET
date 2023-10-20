package pnu.cse.onionmarket.chat

import pnu.cse.onionmarket.chat.detail.ChatDetailItem

data class ChatItem(
    val chatRoomId: String? = null,
    val otherUserId: String? = null,
    val otherUserProfile: String? = null,
    val otherUserName: String? = null,
    var lastMessage: String? = null,
    var unreadMessage: Int? = 0,
    var lastMessageTime: Long? = null,
    var chats: HashMap<String, ChatDetailItem>? = null,
)