package pnu.cse.onionmarket.chat

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.FragmentChatBinding

class ChatFragment : Fragment(R.layout.fragment_chat) {
    private lateinit var binding: FragmentChatBinding
    private lateinit var chatAdapter: ChatAdapter

    private val userId = Firebase.auth.currentUser?.uid

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)

        chatAdapter = ChatAdapter { item ->
            val chatRoomDB = Firebase.database.reference.child("ChatRooms").child(userId!!)
                .child(item.otherUserId!!)

            val action =
                ChatFragmentDirections.actionChatFragmentToChatDetailFragment(
                    chatRoomId = item.chatRoomId!!,
                    otherUserId = item.otherUserId
                )

            findNavController().navigate(action)
        }

        binding.chatListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = chatAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        }

        val chatRoomsDB = Firebase.database.reference.child("ChatRooms").child(userId!!)

        chatRoomsDB.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatRoomList = snapshot.children.mapNotNull {
                    it.getValue(ChatItem::class.java)
                }.sortedByDescending { it.lastMessageTime } // 최근 메시지 시간을 기준으로 내림차순 정렬
                chatAdapter.submitList(chatRoomList)

                if (chatAdapter.currentList.isEmpty()) {
                    binding.noChat.visibility = VISIBLE
                } else {
                    binding.noChat.visibility = GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }
}
