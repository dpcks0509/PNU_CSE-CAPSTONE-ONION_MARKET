package pnu.cse.onionmarket.wallet

import android.app.AlertDialog
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import pnu.cse.onionmarket.MainActivity.Companion.retrofitService
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.ItemWalletBinding

class WalletAdapter() : ListAdapter<WalletItem, WalletAdapter.ViewHolder>(differ) {

    inner class ViewHolder(private val binding: ItemWalletBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WalletItem) {
            if (item.walletImage.isNullOrEmpty())
                Glide.with(binding.walletImage)
                    .load(R.drawable.app_logo)
                    .into(binding.walletImage)
            else
                Glide.with(binding.walletImage)
                    .load(item.walletImage)
                    .into(binding.walletImage)

            binding.walletName.text = item.walletName

            var walletMoney = "0"
            var getMoney = false

            val walletJob = CoroutineScope(Dispatchers.IO).async {
                retrofitService.getWalletMoney(item.privateKey!!).execute().let { response ->
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
                    val updates: MutableMap<String, Any> = hashMapOf(
                        "Wallets/${item.walletId}/walletMoney" to walletMoney,
                    )

                    Firebase.database.reference.updateChildren(updates)

                    val priceWithoutCommas = walletMoney.toString()
                    val formattedPrice = StringBuilder()
                    var commaCounter = 0

                    for (i in priceWithoutCommas.length - 1 downTo 0) {
                        formattedPrice.append(priceWithoutCommas[i])
                        commaCounter++

                        if (commaCounter == 3 && i > 0) {
                            formattedPrice.append(",")
                            commaCounter = 0
                        }
                    }
                    formattedPrice.reverse()
                    binding.walletMoney.text = "$formattedPrice 원"
                }
            }

            val priceWithoutCommas = item.walletMoney.toString()
            val formattedPrice = StringBuilder()
            var commaCounter = 0

            for (i in priceWithoutCommas.length - 1 downTo 0) {
                formattedPrice.append(priceWithoutCommas[i])
                commaCounter++

                if (commaCounter == 3 && i > 0) {
                    formattedPrice.append(",")
                    commaCounter = 0
                }
            }
            formattedPrice.reverse()
            binding.walletMoney.text = "$formattedPrice 원"

            binding.walletEditButton.setOnClickListener {
                var popupMenu = PopupMenu(itemView.context, it)

                popupMenu.menuInflater.inflate(R.menu.menu_wallet, popupMenu.menu)
                popupMenu.show()
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.wallet_edit_button -> {
                            itemView.findNavController()
                                .navigate(R.id.action_walletFragment_to_walletAddFragment)
                        }
                        R.id.wallet_delete_button -> {
                            val builder = AlertDialog.Builder(itemView.context)
                            builder
                                .setTitle("지갑삭제")
                                .setMessage("정말로 지갑을 삭제하시겠습니까?")
                                .setPositiveButton("삭제",
                                    DialogInterface.OnClickListener { dialog, id ->
                                        Firebase.database.reference.child("Wallets")
                                            .child(item.walletId!!).removeValue()
                                        notifyItemRemoved(position)
                                    })
                                .setNegativeButton("취소",
                                    DialogInterface.OnClickListener { dialog, id -> })

                            builder.create()
                            builder.show()
                        }

                    }
                    false
                }
            }
        }
    }

    companion object {
        val differ = object : DiffUtil.ItemCallback<WalletItem>() {
            override fun areItemsTheSame(oldItem: WalletItem, newItem: WalletItem): Boolean {
                return oldItem.walletId == newItem.walletId
            }

            override fun areContentsTheSame(oldItem: WalletItem, newItem: WalletItem): Boolean {
                return oldItem == newItem
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemWalletBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

}