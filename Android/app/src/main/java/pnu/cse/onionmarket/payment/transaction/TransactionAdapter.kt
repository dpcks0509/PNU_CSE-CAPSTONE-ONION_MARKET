package pnu.cse.onionmarket.payment.transaction


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.ItemTransactionBinding
import pnu.cse.onionmarket.post.PostItem

class TransactionAdapter(
    private val onClick: (TransactionItem) -> Unit,
): ListAdapter<TransactionItem, TransactionAdapter.ViewHolder>(differ)  {
    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionItem) {
            val userId = Firebase.auth.currentUser?.uid
            Glide.with(binding.postThumbnail)
                .load(item.postThumbnailImage)
                .into(binding.postThumbnail)

            binding.postTitle.text = item.postTitle
            val priceWithoutCommas = item.postPrice.toString()
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
            binding.postPrice.text = "${formattedPrice}원"

            binding.transactionStatus.apply {
                if (item.buyerId == userId) {
                    text = "구매"
                    backgroundTintList =
                        ContextCompat.getColorStateList(binding.root.context, R.color.main_color)
                } else {
                    text = "판매"
                    backgroundTintList =
                        ContextCompat.getColorStateList(binding.root.context, R.color.pink)
                }
            }


            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    companion object {
        val differ = object : DiffUtil.ItemCallback<TransactionItem>() {
            override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
                return oldItem.transactionId == newItem.transactionId
            }

            override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemTransactionBinding.inflate(
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




