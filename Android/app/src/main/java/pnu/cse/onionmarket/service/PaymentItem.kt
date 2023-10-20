package pnu.cse.onionmarket.service

data class PaymentItem(
    var receiverPrivateKey: String? = null, // sellerPrivateKey
    var senderPrivateKey: String? = null, // buyerPrivateKey
    var money: Double? = null
)
