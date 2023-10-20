package pnu.cse.onionmarket.payment.transaction

import pnu.cse.onionmarket.payment.workmanager.AfterDeliveredWorker
import pnu.cse.onionmarket.payment.workmanager.DeliveryCheckWorker
import pnu.cse.onionmarket.payment.workmanager.WaybillRegistrationWorker

data class TransactionItem(
    var transactionId: String? = null,
    var createdAt: Long? = null,
    var postId: String? = null,
    var sellerId: String? = null,
    var buyerId: String? = null,
    var postThumbnailImage: String? = null,
    var postTitle: String? = null,
    var postPrice: String? = null,
    var name: String? = null,
    var phone: String? = null,
    var address: String? = null,
    var waybillCompanyPosition: Int? = null,
    var waybillCompany: String? = null,
    var waybillCompanyCode: String? = null,
    var waybillNumber: String? = null,
    var deliveryArrived: Boolean? = null,
    var completePayment: Boolean? = null,
    var waybillRegistrationWorker: Boolean? = null,
    var deliveryCheckWorker: Boolean? = null,
    var afterDeliveredWorker: Boolean? = null,
)