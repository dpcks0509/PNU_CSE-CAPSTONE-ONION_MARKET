package pnu.cse.onionmarket.payment.workmanager

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import pnu.cse.onionmarket.MainActivity.Companion.retrofitService
import pnu.cse.onionmarket.payment.transaction.TransactionItem

class DeliveryCheckWorker(context: Context, workerParams: WorkerParameters) : Worker(
    context,
    workerParams
) {
    override fun doWork(): Result {
        Log.e("DeliveryCheckWorker", "DeliveryCheckWorker")
        var waybillCompanyCode = ""
        var waybillNumber = ""

        Firebase.database.reference.child("Transactions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.map {
                        val transaction = it.getValue(TransactionItem::class.java)
                        transaction ?: return
                        if (transaction.transactionId == transactionId) {
                            waybillCompanyCode = transaction.waybillCompanyCode!!
                            waybillNumber = transaction.waybillNumber!!
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        var delivered = false

        val deliveryJob = CoroutineScope(Dispatchers.IO).async {
            retrofitService.deliveryCheck(
                waybillCompanyCode,
                waybillNumber
            ).execute().let { response ->
                if (response.isSuccessful) {
                    val state = response.body().toString()
                    Log.e("state", state)
                    if (state == "배송완료") {
                        delivered = true
                    }
                }
            }

            delivered
        }

        val isDelivered = runBlocking {
            val deliveryResult = deliveryJob.await()

            if (deliveryResult) {
                if (transactionId != null) {
                    val updates: MutableMap<String, Any> = hashMapOf(
                        "Transactions/$transactionId/deliveryArrived" to true,
                        "Transactions/$transactionId/deliveryCheckWorker" to true
                    )

                    Firebase.database.reference.updateChildren(updates)

                    Log.e("delivered", "delivered")
                    return@runBlocking true
                } else {
                    return@runBlocking false
                }
            } else {
                Log.e("not delivered", "not delivered")
                return@runBlocking false
            }
        }

        if (isDelivered) {
            return Result.success()
        } else {
            return Result.failure()
        }
    }

    companion object {
        fun setData(transactionId: String) {
            this.transactionId = transactionId
        }

        var transactionId: String? = null
    }
}