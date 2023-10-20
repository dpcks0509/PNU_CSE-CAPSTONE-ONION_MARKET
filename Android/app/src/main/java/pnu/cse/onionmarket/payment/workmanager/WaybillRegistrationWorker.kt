package pnu.cse.onionmarket.payment.workmanager

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class WaybillRegistrationWorker(context: Context, workerParams: WorkerParameters) : Worker(
    context,
    workerParams
) {
    override fun doWork(): Result {
        Log.e("WaybillRegistrationWorker", "WaybillRegistrationWorker")
        if (transactionId != null) {
            val updates: MutableMap<String, Any> = hashMapOf(
                "Posts/$postId/postStatus" to true,
                "Posts/$postId/buyerId" to "",
                "Transactions/$transactionId/waybillRegistrationWorker" to true
            )
            Firebase.database.reference.updateChildren(updates)

            Firebase.database.reference.child("Transactions").child(transactionId!!).removeValue()
            return Result.success()
        }
        return Result.failure()
    }

    companion object {
        fun setData(postId: String, transactionId: String) {
            this.postId = postId
            this.transactionId = transactionId
        }

        var postId: String? = null
        var transactionId: String? = null
    }
}