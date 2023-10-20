package pnu.cse.onionmarket.wallet

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import pnu.cse.onionmarket.MainActivity
import pnu.cse.onionmarket.MainActivity.Companion.retrofitService
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.FragmentWalletAddBinding
import java.util.UUID
import java.util.regex.Pattern

class WalletAddFragment : Fragment(R.layout.fragment_wallet_add) {
    private lateinit var binding: FragmentWalletAddBinding

    override fun onResume() {
        super.onResume()

        val mainActivity = activity as MainActivity
        mainActivity.hideBottomNavigation(true)
    }

    override fun onPause() {
        super.onPause()

        val mainActivity = activity as MainActivity
        mainActivity.hideBottomNavigation(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWalletAddBinding.bind(view)

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }


        binding.submitButton.setOnClickListener {

            val walletPrivateKey = binding.walletKey.text.toString()

            if (binding.walletName.text.isNullOrEmpty() || binding.walletKey.text.isNullOrEmpty()) {
                Toast.makeText(context, "지갑 추가에 필요한 정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val pattern = "^0x[a-fA-F0-9]{64}$"
            val regex = Pattern.compile(pattern)

            if (!regex.matcher(binding.walletKey.text.toString()).matches()) {
                Toast.makeText(context, "지갑주소의 형태가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val walletId = UUID.randomUUID().toString()
            val userId = Firebase.auth.currentUser?.uid
            var walletImage = ""

            Firebase.database.reference.child("Users").child(userId!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.child("userProfileImage").exists())
                            walletImage =
                                snapshot.child("userProfileImage").getValue(String::class.java)!!
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })


            var walletMoney = "0"
            var getMoney = false

            val walletJob = CoroutineScope(Dispatchers.IO).async {
                retrofitService.getWalletMoney(walletPrivateKey!!).execute().let { response ->
                    if (response.isSuccessful) {
                        walletMoney = (response.body().toString().replace("ETH", "")
                            .toDouble()).times(2000000).toInt().toString()
                        getMoney = true
                    }
                }
                getMoney
            }

            runBlocking {
                val walletResult = walletJob.await()

                if (walletResult) {
                    val wallet = WalletItem(
                        walletId = walletId,
                        userId = userId,
                        walletName = binding.walletName.text.toString(),
                        walletImage = walletImage,
                        privateKey = walletPrivateKey,
                        walletMoney = walletMoney,
                        createdAt = System.currentTimeMillis()
                    )

                    Firebase.database.reference.child("Wallets").child(walletId).setValue(wallet)
                        .addOnSuccessListener {}
                }
            }
            findNavController().popBackStack()
        }
    }
}