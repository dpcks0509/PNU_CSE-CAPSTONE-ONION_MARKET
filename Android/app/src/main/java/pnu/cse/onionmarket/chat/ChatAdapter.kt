package pnu.cse.onionmarket.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import pnu.cse.onionmarket.databinding.ItemChatBinding

class ChatAdapter(private val onClick: (ChatItem) -> Unit) :
    ListAdapter<ChatItem, ChatAdapter.ViewHolder>(differ) {

    inner class ViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatItem) {
            var unreadMessage = 0

            if (!item.otherUserProfile.isNullOrEmpty())
                Glide.with(binding.profileImage)
                    .load(item.otherUserProfile)
                    .into(binding.profileImage)

            Firebase.database.reference.child("Users").child(item.otherUserId!!)
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

            val myUserId = Firebase.auth.currentUser?.uid
            Firebase.database.reference.child("ChatRooms").child(myUserId!!)
                .child(item.otherUserId!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.child("unreadMessage").exists())
                            unreadMessage =
                                snapshot.child("unreadMessage").getValue(Int::class.java)!!
                        if (unreadMessage == 0)
                            binding.unreadMessage.isVisible = false
                        else
                            binding.unreadMessage.isVisible = true
                        binding.unreadMessage.text = unreadMessage.toString()

                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })

            binding.nickname.text = item.otherUserName
            binding.lastMessage.text = item.lastMessage

            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    companion object {
        val differ = object : DiffUtil.ItemCallback<ChatItem>() {
            override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
                return oldItem.chatRoomId == newItem.chatRoomId
            }

            override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
                return oldItem == newItem
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemChatBinding.inflate(
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