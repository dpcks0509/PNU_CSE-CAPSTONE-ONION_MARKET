package pnu.cse.onionmarket.profile.review

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
import pnu.cse.onionmarket.MainActivity.Companion.retrofitService
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.UserItem
import pnu.cse.onionmarket.chat.ChatItem
import pnu.cse.onionmarket.chat.detail.ChatDetailAdapter
import pnu.cse.onionmarket.chat.detail.ChatDetailFragment
import pnu.cse.onionmarket.chat.detail.ChatDetailItem
import pnu.cse.onionmarket.databinding.FragmentReviewWriteBinding
import pnu.cse.onionmarket.post.PostItem
import pnu.cse.onionmarket.service.BlockchainReviewItem
import java.io.IOException
import java.util.UUID

class ReviewWriteFragment : Fragment(R.layout.fragment_review_write) {
    private lateinit var binding: FragmentReviewWriteBinding
    private val args: ReviewWriteFragmentArgs by navArgs()

    private lateinit var chatDetailAdapter: ChatDetailAdapter
    private val chatDetailItemList = mutableListOf<ChatDetailItem>()

    private lateinit var postId: String

    private var chatRoomId: String = ""
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var otherUserToken: String = ""
    private var otherUserProfileImage: String = ""
    private var myUserId: String = Firebase.auth.currentUser?.uid!!
    private var myUserName: String = ""
    private var myUserProfileImage: String = ""

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
        binding = FragmentReviewWriteBinding.bind(view)

        chatDetailAdapter = ChatDetailAdapter()

        val reviewId = UUID.randomUUID().toString()
        val userId = Firebase.auth.currentUser?.uid
        val profileUserId = args.profileUserId
        postId = args.postId
        otherUserId = profileUserId
        var reviewStar = 0.0
        var reviewText = ""
        var createdAt = System.currentTimeMillis()

        binding.ratingBar.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->
                if (rating < 0.5f) {
                    ratingBar.rating = 0.5f
                }
                reviewStar = rating.toDouble()
            }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        Firebase.database.reference.child("Users").child(userId!!).get()
            .addOnSuccessListener {
                val myUserItem = it.getValue(UserItem::class.java)
                myUserName = myUserItem?.userNickname ?: ""
                myUserProfileImage = myUserItem?.userProfileImage ?: ""

                getOtherUserData()
            }

        var postTitle = ""

        Firebase.database.reference.child("Posts").child(postId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val post = snapshot.getValue(PostItem::class.java)
                    postTitle = post?.postTitle!!

                }

                override fun onCancelled(error: DatabaseError) {}

            })

        binding.submitButton.setOnClickListener {
            if (reviewStar == 0.0 || binding.reviewText.text.toString().isNullOrEmpty()) {
                Toast.makeText(context, "리뷰 작성에 필요한 정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            reviewText = binding.reviewText.text.toString()

            val chatRoomDB =
                Firebase.database.reference.child("ChatRooms").child(userId!!).child(otherUserId)

            chatRoomDB.get().addOnSuccessListener {
                if (it.value != null) {
                    val chatRoom = it.getValue(ChatItem::class.java)
                    chatRoomId = chatRoom?.chatRoomId!!
                } else {
                    chatRoomId = UUID.randomUUID().toString()
                }

                // 메세지 , 알림 보내기
                val message = "<리뷰작성 알림>\n" +
                        "상품명 : [$postTitle]에 대한 리뷰가 작성되었습니다.\n\n" +
                        "작성자 : $myUserName\n" +
                        "리뷰 별점 : $reviewStar\n" +
                        "리뷰 내용 : $reviewText"

                val lastMessageTime = System.currentTimeMillis()

                val newChatRoom = ChatItem(
                    chatRoomId = chatRoomId,
                    otherUserId = otherUserId,
                    otherUserProfile = otherUserProfileImage,
                    otherUserName = otherUserName,
                    lastMessage = message,
                    lastMessageTime = lastMessageTime
                )

                if (it.value == null)
                    chatRoomDB.setValue(newChatRoom)

                val newChatItem = ChatDetailItem(
                    message = message,
                    userId = userId,
                    userProfile = myUserProfileImage
                )

                Firebase.database.reference.child("ChatRooms").child(otherUserId).child(userId!!)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists())
                                ChatDetailFragment.unreadMessage =
                                    snapshot.child("unreadMessage").getValue(Int::class.java)!!
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })

                ChatDetailFragment.unreadMessage += 1

                chatDetailAdapter.submitList(chatDetailItemList.toMutableList())

                Firebase.database.reference.child("ChatRooms").child(userId).child(otherUserId)
                    .child("chats").push().apply {
                    newChatItem.chatId = key
                    setValue(newChatItem)
                }
                Firebase.database.reference.child("ChatRooms").child(otherUserId).child(userId)
                    .child("chats").push().apply {
                    newChatItem.chatId = key
                    setValue(newChatItem)
                }

                val updates: MutableMap<String, Any> = hashMapOf(
                    "ChatRooms/$otherUserId/$userId/lastMessage" to message,
                    "ChatRooms/$otherUserId/$userId/chatRoomId" to chatRoomId,
                    "ChatRooms/$otherUserId/$userId/otherUserId" to userId,
                    "ChatRooms/$otherUserId/$userId/otherUserName" to myUserName,
                    "ChatRooms/$otherUserId/$userId/otherUserProfile" to myUserProfileImage,
                    "ChatRooms/$otherUserId/$userId/unreadMessage" to ChatDetailFragment.unreadMessage,
                    "ChatRooms/$otherUserId/$userId/lastMessageTime" to lastMessageTime
                )

                Firebase.database.reference.updateChildren(updates)

                val client = OkHttpClient()
                val root = JSONObject()
                val notification = JSONObject()
                notification.put("title", myUserName)
                notification.put("body", message)
                notification.put("chatRoomId", chatRoomId)
                notification.put("otherUserId", userId)

                root.put("to", otherUserToken)
                root.put("priority", "high")
                root.put("data", notification)

                val requestBody =
                    root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request =
                    Request.Builder().post(requestBody).url("https://fcm.googleapis.com/fcm/send")
                        .header("Authorization", "key=${getString(R.string.fcm_server_key)}")
                        .build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(call: Call, response: Response) {
                    }

                })


                val update: MutableMap<String, Any> = hashMapOf(
                    "Posts/$postId/reviewWrite" to true
                )

                Firebase.database.reference.updateChildren(update)

                Firebase.database.reference.child("Users").child(userId!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var userName =
                                snapshot.child("userNickname").getValue(String::class.java)
                            var userProfileImage =
                                snapshot.child("userProfileImage").getValue(String::class.java)

                            val review = ReviewItem(
                                reviewId = reviewId,
                                createdAt = createdAt,
                                userId = profileUserId,
                                userProfile = userProfileImage,
                                userName = userName,
                                reviewText = reviewText,
                                reviewStar = reviewStar
                            )

                            Firebase.database.reference.child("Reviews").child(reviewId)
                                .setValue(review)
                                .addOnSuccessListener {

                                    var userReviewSum = 0.0
                                    var userReviewNumber = 0
                                    var userReviewStar = 0.0

                                    // users star 업데이트
                                    Firebase.database.reference.child("Reviews")
                                        .addValueEventListener(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                userReviewSum = 0.0
                                                userReviewNumber = 0
                                                userReviewStar = 0.0

                                                snapshot.children.map {
                                                    val review = it.getValue(ReviewItem::class.java)
                                                    review ?: return

                                                    if (review.userId == profileUserId) {
                                                        userReviewSum += review.reviewStar!!
                                                        userReviewNumber += 1
                                                    }
                                                }
                                                if (userReviewNumber != 0) {
                                                    userReviewStar =
                                                        (userReviewSum / userReviewNumber)
                                                    userReviewStar =
                                                        String.format("%.1f", userReviewStar)
                                                            .toDouble()
                                                }

                                                val update: MutableMap<String, Any> = hashMapOf(
                                                    "Users/$profileUserId/userStar" to userReviewStar
                                                )

                                                Firebase.database.reference.updateChildren(update)
                                                    .addOnSuccessListener {
                                                        val action =
                                                            ReviewWriteFragmentDirections.actionReviewWriteFragmentToChatDetailFragment(
                                                                chatRoomId = chatRoomId,
                                                                otherUserId = otherUserId
                                                            )
                                                        findNavController().navigate(action)
                                                    }
                                            }

                                            override fun onCancelled(error: DatabaseError) {}

                                        })
                                }


                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })
            }

            retrofitService.saveReview(
                BlockchainReviewItem(
                    userId = otherUserId,
                    reviewId = reviewId,
                    reviewStar = reviewStar.toString(),
                    reviewText = reviewText,
                    writerNickname = myUserName,
                    createdAt = createdAt
                )
            ).enqueue(object : retrofit2.Callback<String> {
                override fun onResponse(
                    call: retrofit2.Call<String>,
                    response: retrofit2.Response<String>
                ) {
                    val state = response.body().toString()
                    Log.e("saveReview", state)
                }

                override fun onFailure(call: retrofit2.Call<String>, t: Throwable) {
                    Log.e("saveReview", t.toString())
                }
            })
        }

        val reviewsRef = Firebase.database.reference.child("Reviews")
        val reviewsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                updateStarRating(profileUserId)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                updateStarRating(profileUserId)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                updateStarRating(profileUserId)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Not used in this case
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
            }
        }

        reviewsRef.addChildEventListener(reviewsListener)
    }

    private fun updateStarRating(profileUserId: String) {
        var userReviewSum = 0.0
        var userReviewNumber = 0
        var userReviewStar = 0.0

        val reviewsRef = Firebase.database.reference.child("Reviews")
        reviewsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userReviewSum = 0.0
                userReviewNumber = 0

                snapshot.children.map { reviewSnapshot ->
                    val review = reviewSnapshot.getValue(ReviewItem::class.java)
                    review ?: return@map

                    if (review.userId == profileUserId) {
                        userReviewSum += review.reviewStar!!
                        userReviewNumber += 1
                    }
                }

                if (userReviewNumber != 0) {
                    userReviewStar = userReviewSum / userReviewNumber
                    userReviewStar = String.format("%.1f", userReviewStar).toDouble()
                } else {
                    userReviewStar = 0.0
                }

                val updateUserStar: MutableMap<String, Any> = hashMapOf(
                    "Users/$profileUserId/userStar" to userReviewStar,
                )

                Firebase.database.reference.updateChildren(updateUserStar)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
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
                getChatData()
            }
    }
}