package pnu.cse.onionmarket.chat.detail

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import pnu.cse.onionmarket.UserItem
import pnu.cse.onionmarket.databinding.ItemChatDetailBinding

class ChatDetailAdapter() : ListAdapter<ChatDetailItem, ChatDetailAdapter.ViewHolder>(differ) {

    var otherUserItem: UserItem? = null

    private var previousUserId: String? = null

    inner class ViewHolder(private val binding: ItemChatDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatDetailItem) {

            if (!item.userProfile.isNullOrEmpty())
                Glide.with(binding.profileImage)
                    .load(item.userProfile)
                    .into(binding.profileImage)

            Firebase.database.reference.child("Users").child(item.userId!!)
                .addValueEventListener(object :
                    ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userImageUri =
                            snapshot.child("userProfileImage").getValue(String::class.java)

                        if (!userImageUri.isNullOrEmpty())
                            Glide.with(binding.profileImage)
                                .load(userImageUri)
                                .into(binding.profileImage)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })

            binding.message.text = item.message

            if (item.userId == otherUserItem?.userId) {
                binding.profileImage.isVisible = true
                binding.chatItem.gravity = Gravity.START
                if (item.userId == previousUserId) {
                    binding.profileImage.visibility = View.INVISIBLE
                }
            } else {
                binding.profileImage.isVisible = false
                binding.chatItem.gravity = Gravity.END
            }
            previousUserId = item.userId
        }
    }

    companion object {
        val differ = object : DiffUtil.ItemCallback<ChatDetailItem>() {
            override fun areItemsTheSame(
                oldItem: ChatDetailItem,
                newItem: ChatDetailItem
            ): Boolean {
                return oldItem.chatId == newItem.chatId
            }

            override fun areContentsTheSame(
                oldItem: ChatDetailItem,
                newItem: ChatDetailItem
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            ItemChatDetailBinding.inflate(
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