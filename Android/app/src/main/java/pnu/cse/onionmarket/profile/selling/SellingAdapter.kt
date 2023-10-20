package pnu.cse.onionmarket.profile.selling

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

class SellingAdapter(private val onClick: (PostItem) -> Unit) :
    ListAdapter<PostItem, SellingAdapter.ViewHolder>(
        differ
    ) {

    inner class ViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PostItem) {

            Glide.with(binding.postThumbnail)
                .load(item.postThumbnailUrl)
                .into(binding.postThumbnail)

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

            binding.postTitle.text = item.postTitle
            binding.postPrice.text = item.postPrice

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
        return currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }
}