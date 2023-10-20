package pnu.cse.onionmarket.wallet

data class WalletItem(
    var walletId: String? = null,
    var userId: String? = null,
    var walletName: String? = null,
    var walletImage: String? = null,
    var privateKey: String? = null,
    var walletMoney: String? = null,
    var createdAt: Long? = null,
)
