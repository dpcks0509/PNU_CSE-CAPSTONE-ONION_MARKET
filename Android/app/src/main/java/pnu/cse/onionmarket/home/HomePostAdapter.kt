package pnu.cse.onionmarket.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.ItemPostBinding
import pnu.cse.onionmarket.payment.transaction.TransactionItem
import pnu.cse.onionmarket.post.PostItem

class HomePostAdapter(
    private val onClick: (PostItem) -> Unit,
) : ListAdapter<PostItem, HomePostAdapter.ViewHolder>(differ) {

    private var originalList: MutableList<PostItem> = mutableListOf()
    private var filteredList: MutableList<PostItem> = mutableListOf()
    private var onSearching: Boolean = false

    inner class ViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PostItem) {
            Glide.with(binding.postThumbnail)
                .load(item.postThumbnailUrl)
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

            binding.postPrice.text = "$formattedPrice 원"
            binding.postStatus.apply {
                if (item.postStatus == true) {
                    text = "판매중"
                    backgroundTintList =
                        ContextCompat.getColorStateList(binding.root.context, R.color.main_color)

                } else {
                    var onPayment = true
                    Firebase.database.reference.child("Transactions").addValueEventListener(object :
                        ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.children.map {
                                val transaction =
                                    it.getValue(TransactionItem::class.java)
                                transaction ?: return
                                if (transaction.postId == item.postId && transaction.completePayment == true)
                                    onPayment = false

                                if (onPayment) {
                                    text = "거래중"
                                    backgroundTintList =
                                        ContextCompat.getColorStateList(
                                            binding.root.context,
                                            R.color.pink
                                        )
                                } else {
                                    text = "판매완료"
                                    backgroundTintList =
                                        ContextCompat.getColorStateList(
                                            binding.root.context,
                                            R.color.gray
                                        )
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }

                    })

                }
            }

            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    companion object {
        val differ = object : DiffUtil.ItemCallback<PostItem>() {
            override fun areItemsTheSame(oldItem: PostItem, newItem: PostItem): Boolean {
                return oldItem.postId == newItem.postId
            }

            override fun areContentsTheSame(oldItem: PostItem, newItem: PostItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPostBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return if (onSearching) filteredList.size
        else originalList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sortedList = if (onSearching) filteredList.sortedByDescending { it.createdAt }
        else originalList.sortedByDescending { it.createdAt }

        holder.bind(sortedList[position])
    }

    fun setOriginalList(list: MutableList<PostItem>) {
        originalList.clear()
        originalList.addAll(list)
        notifyDataSetChanged()
    }

    fun setFilteredList(list: MutableList<PostItem>, onSearching: Boolean) {
        filteredList.clear()
        filteredList.addAll(list)
        this.onSearching = onSearching
        notifyDataSetChanged()
    }
}
