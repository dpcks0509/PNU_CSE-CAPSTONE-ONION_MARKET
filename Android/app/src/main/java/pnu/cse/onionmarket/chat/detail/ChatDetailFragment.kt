package pnu.cse.onionmarket.chat.detail

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import pnu.cse.onionmarket.MainActivity
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.UserItem
import pnu.cse.onionmarket.databinding.FragmentChatDetailBinding
import java.io.IOException

class ChatDetailFragment : Fragment(R.layout.fragment_chat_detail) {
    private lateinit var binding: FragmentChatDetailBinding
    private lateinit var chatDetailAdapter: ChatDetailAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private val chatDetailItemList = mutableListOf<ChatDetailItem>()
    private var chatRoomId: String = ""
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var otherUserToken: String = ""
    private var otherUserProfileImage: String = ""
    private var myUserId: String = ""
    private var myUserName: String = ""
    private var myUserProfileImage: String = ""

    private var exit = false

    companion object {
        var unreadMessage: Int = 0
    }

    private val args: ChatDetailFragmentArgs by navArgs()

    override fun onStop() {
        super.onStop()

        if (!exit) {
            Firebase.database.reference.child("ChatRooms").child(myUserId).child(otherUserId)
                .child("unreadMessage").setValue(0)
        }
    }

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
        binding = FragmentChatDetailBinding.bind(view)

        val notificationManager =
            requireContext().getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(0)

        chatDetailAdapter = ChatDetailAdapter()
        linearLayoutManager = LinearLayoutManager(context)

        binding.chatRecyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = chatDetailAdapter
        }

        chatDetailAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                binding.chatRecyclerView.scrollToPosition(chatDetailAdapter.itemCount - 1)
            }
        })

        chatRoomId = args.chatRoomId
        otherUserId = args.otherUserId
        myUserId = Firebase.auth.currentUser?.uid!!

        Firebase.database.reference.child("Users").child(myUserId!!).get()
            .addOnSuccessListener {
                val myUserItem = it.getValue(UserItem::class.java)
                myUserName = myUserItem?.userNickname ?: ""
                myUserProfileImage = myUserItem?.userProfileImage ?: ""

                getOtherUserData()
            }

        binding.nickname.setOnClickListener {
            val action = ChatDetailFragmentDirections.actionChatDetailFragmentToProfileFragment(
                writerId = otherUserId,
                postId = ""
            )
            findNavController().navigate(action)
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()

            val notificationManager =
                requireContext().getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(0)

            Firebase.database.reference.child("ChatRooms").child(myUserId).child(otherUserId)
                .child("lastMessage")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists())
                            Firebase.database.reference.child("ChatRooms").child(myUserId)
                                .child(otherUserId).removeValue()
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        binding.editButton.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder
                .setTitle("채팅방 나가기")
                .setMessage("채팅방을 나가시겠습니까?")
                .setPositiveButton("나가기",
                    DialogInterface.OnClickListener { dialog, id ->
                        exit = true
                        Firebase.database.reference.child("ChatRooms").child(myUserId).removeValue()
                        findNavController().popBackStack()
                    })
                .setNegativeButton("취소",
                    DialogInterface.OnClickListener { dialog, id -> })

            builder.create()
            builder.show()
        }

        Firebase.database.reference.child("ChatRooms").child(otherUserId).child(myUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.child("unreadMessage").exists())
                        unreadMessage = snapshot.child("unreadMessage").getValue(Int::class.java)!!
                    else {
                        unreadMessage = 0
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        binding.sendButton.setOnClickListener {

            val message = binding.messageEditText.text.toString()

            if (message.isEmpty())
                return@setOnClickListener
            unreadMessage += 1


            val newChatItem = ChatDetailItem(
                message = message,
                userId = myUserId,
                userProfile = myUserProfileImage
            )

            Firebase.database.reference.child("ChatRooms").child(myUserId).child(otherUserId)
                .child("chats").push().apply {
                newChatItem.chatId = key
                setValue(newChatItem)
            }
            Firebase.database.reference.child("ChatRooms").child(otherUserId).child(myUserId)
                .child("chats").push().apply {
                newChatItem.chatId = key
                setValue(newChatItem)
            }

            chatDetailAdapter.submitList(chatDetailItemList.toMutableList())

            val lastMessageTime = System.currentTimeMillis()

            val updates: MutableMap<String, Any> = hashMapOf(
                "ChatRooms/$myUserId/$otherUserId/lastMessage" to message,
                "ChatRooms/$myUserId/$otherUserId/lastMessageTime" to lastMessageTime,
                "ChatRooms/$myUserId/$otherUserId/otherUserProfile" to otherUserProfileImage,
                "ChatRooms/$otherUserId/$myUserId/lastMessage" to message,
                "ChatRooms/$otherUserId/$myUserId/chatRoomId" to chatRoomId,
                "ChatRooms/$otherUserId/$myUserId/otherUserId" to myUserId,
                "ChatRooms/$otherUserId/$myUserId/otherUserName" to myUserName,
                "ChatRooms/$otherUserId/$myUserId/otherUserProfile" to myUserProfileImage,
                "ChatRooms/$otherUserId/$myUserId/unreadMessage" to unreadMessage,
                "ChatRooms/$otherUserId/$myUserId/lastMessageTime" to lastMessageTime
            )

            Firebase.database.reference.updateChildren(updates)

            val client = OkHttpClient()
            val root = JSONObject()
            val notification = JSONObject()
            notification.put("title", myUserName)
            notification.put("body", message)
            notification.put("chatRoomId", chatRoomId)
            notification.put("otherUserId", myUserId)

            root.put("to", otherUserToken)
            root.put("priority", "high")
            root.put("data", notification)

            val requestBody =
                root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request =
                Request.Builder().post(requestBody).url("https://fcm.googleapis.com/fcm/send")
                    .header("Authorization", "key=${getString(R.string.fcm_server_key)}").build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                override fun onResponse(call: Call, response: Response) {
                }

            })

            binding.messageEditText.text.clear()
        }

        val viewTreeObserver = binding.messageEditText.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener {
            // EditText가 그려질 때마다 스크롤 코드 실행
            linearLayoutManager.smoothScrollToPosition(
                binding.chatRecyclerView,
                null,
                chatDetailAdapter.itemCount
            )
        }

    }

    private fun getChatData() {
        Firebase.database.reference.child("ChatRooms").child(myUserId).child(otherUserId)
            .child("chats")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val chatDetailItem = snapshot.getValue(ChatDetailItem::class.java)
                    chatDetailItem ?: return

                    chatDetailItemList.add(chatDetailItem)
                    chatDetailAdapter.submitList(chatDetailItemList.toMutableList())
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onChildRemoved(snapshot: DataSnapshot) {}

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {}

            })
    }

    private fun getOtherUserData() {
        Firebase.database.reference.child("Users").child(otherUserId!!).get()
            .addOnSuccessListener {
                val otherUserItem = it.getValue(UserItem::class.java)
                chatDetailAdapter.otherUserItem = otherUserItem
                otherUserName = otherUserItem?.userNickname.toString()
                otherUserToken = otherUserItem?.userToken.orEmpty()
                otherUserProfileImage = otherUserItem?.userProfileImage.orEmpty()
                binding.nickname.text = otherUserItem?.userNickname

                getChatData()
            }
    }
}
