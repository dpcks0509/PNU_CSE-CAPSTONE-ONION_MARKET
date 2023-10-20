package pnu.cse.onionmarket.payment.workmanager

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AfterDeliveredWorker(context: Context, workerParams: WorkerParameters) : Worker(
    context,
    workerParams
) {
    override fun doWork(): Result {
        Log.e("AfterDeliveredWorker", "AfterDeliveredWorker")
        if (transactionId != null) {
            val updates: MutableMap<String, Any> = hashMapOf(
                "Transactions/$transactionId/completePayment" to true,
                "Transactions/$transactionId/afterDeliveredWorker" to true
            )
            Firebase.database.reference.updateChildren(updates)
            return Result.success()
        }
        return Result.failure()
    }

    companion object {
        fun setData(transactionId: String) {
            this.transactionId = transactionId
        }

        var transactionId: String? = null
    }
}