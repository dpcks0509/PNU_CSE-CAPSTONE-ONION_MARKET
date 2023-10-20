package pnu.cse.onionmarket.profile

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
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
import pnu.cse.onionmarket.LoginActivity
import pnu.cse.onionmarket.MainActivity
import pnu.cse.onionmarket.MainActivity.Companion.retrofitService
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.chat.ChatItem
import pnu.cse.onionmarket.databinding.FragmentProfileBinding
import pnu.cse.onionmarket.post.PostItem
import pnu.cse.onionmarket.profile.review.ReviewFragment
import pnu.cse.onionmarket.profile.review.ReviewItem
import pnu.cse.onionmarket.profile.selling.SellingFragment
import pnu.cse.onionmarket.service.BlockchainReviewItem
import java.util.*

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private lateinit var binding: FragmentProfileBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)

        val userId = Firebase.auth.currentUser?.uid

        var writerId = arguments?.getString("writerId")
        val postId = arguments?.getString("postId")
        val fromBlockchain = arguments?.getBoolean("fromBlockchain")

        binding.settingButton.setOnClickListener {
            var popupMenu = PopupMenu(context, it)

            popupMenu.menuInflater.inflate(R.menu.menu_profile, popupMenu.menu)
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.profile_edit_button -> {
                        findNavController().navigate(R.id.action_profileFragment_to_profileEditFragment)
                    }

                    R.id.logout_button -> {
                        FirebaseAuth.getInstance().signOut()

                        var signOutIntent = Intent(context, LoginActivity::class.java)
                        signOutIntent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(signOutIntent)
                    }
                    R.id.withdrawal_button -> {
                        findNavController().navigate(R.id.action_profileFragment_to_withdrawalFragment)
                    }
                }
                false
            }
        }

        val mainActivity = activity as MainActivity
        mainActivity.hideBottomNavigation(false)

        binding.backButton.visibility = View.INVISIBLE
        binding.settingButton.visibility = View.VISIBLE
        binding.profileChatButton.visibility = View.GONE

        val sellingTextSize = 20f
        val sellingTextPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sellingTextSize,
            resources.displayMetrics
        )

        binding.sellingPost.setTextSize(TypedValue.COMPLEX_UNIT_PX, sellingTextPx)
        binding.sellingPost.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        val reviewTextSize = 18f
        val reviewTextPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            reviewTextSize,
            resources.displayMetrics
        )

        binding.sellingPost.setTextSize(TypedValue.COMPLEX_UNIT_PX, reviewTextPx)
        binding.review.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))

        var blockchainReviewList = mutableListOf<BlockchainReviewItem>()

        var userReviewSum = 0.0
        var userReviewNumber = 0
        var userReviewStar = 0.0

        if (fromBlockchain == true) {
            binding.profileText.text = "블록체인 프로필"

            var getReview = false
            val reviewJob = CoroutineScope(Dispatchers.IO).async {
                retrofitService.getReviews(writerId!!).execute().let { response ->
                    if (response.isSuccessful) {
                        blockchainReviewList = response.body()?.toMutableList()!!
                        getReview = true
                    }
                }
                getReview
            }

            runBlocking {
                val getResult = reviewJob.await()

                if (getResult) {
                    for (blockchainReview in blockchainReviewList) {
                        val review = ReviewItem(
                            reviewId = blockchainReview.reviewId,
                            createdAt = blockchainReview.createdAt,
                            userId = blockchainReview.userId,
                            userProfile = null,
                            userName = blockchainReview.writerNickname,
                            reviewText = blockchainReview.reviewText,
                            reviewStar = blockchainReview.reviewStar?.toDouble()
                        )
                        userReviewSum += review.reviewStar!!
                        userReviewNumber += 1

                        if (userReviewNumber != 0) {
                            userReviewStar = (userReviewSum / userReviewNumber)
                            userReviewStar =
                                String.format("%.1f", userReviewStar).toDouble()

                        }
                    }
                }
            }
            Firebase.database.reference.child("Users").child(writerId!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userImageUri =
                            snapshot.child("userProfileImage").getValue(String::class.java)
                        if (userImageUri.isNullOrEmpty())
                            Glide.with(binding.userImage)
                                .load(R.drawable.app_logo)
                                .into(binding.userImage)
                        else
                            Glide.with(binding.userImage)
                                .load(userImageUri)
                                .into(binding.userImage)

                        binding.userNickname.text =
                            snapshot.child("userNickname").getValue(String::class.java)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

            binding.userStar.text = userReviewStar.toString()
        } else {
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

                            if (review.userId == userId) {
                                userReviewSum += review.reviewStar!!
                                userReviewNumber += 1
                            }
                        }
                        if (userReviewNumber != 0) {
                            userReviewStar = (userReviewSum / userReviewNumber)
                            userReviewStar =
                                String.format("%.1f", userReviewStar).toDouble()
                        }

                        val update: MutableMap<String, Any> = hashMapOf(
                            "Users/$userId/userStar" to userReviewStar
                        )

                        Firebase.database.reference.updateChildren(update)
                        if (writerId == null)
                            writerId = userId

                        Firebase.database.reference.child("Users").child(writerId!!)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val userImageUri =
                                        snapshot.child("userProfileImage")
                                            .getValue(String::class.java)
                                    if (userImageUri.isNullOrEmpty())
                                        Glide.with(binding.userImage)
                                            .load(R.drawable.app_logo)
                                            .into(binding.userImage)
                                    else
                                        Glide.with(binding.userImage)
                                            .load(userImageUri)
                                            .into(binding.userImage)


                                    binding.userNickname.text =
                                        snapshot.child("userNickname").getValue(String::class.java)
                                    binding.userStar.text =
                                        snapshot.child("userStar").getValue(Double::class.java)
                                            .toString()
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })
        }

        if (writerId.isNullOrEmpty()) {
            val mainActivity = activity as MainActivity
            mainActivity.hideBottomNavigation(false)

            binding.backButton.visibility = View.INVISIBLE
            binding.settingButton.visibility = View.VISIBLE
            binding.profileChatButton.visibility = View.GONE

            binding.sellingPost.setOnClickListener {
                val sellingFragment = SellingFragment.newInstance(null)
                childFragmentManager.beginTransaction()
                    .replace(R.id.myPageFrameLayout, sellingFragment)
                    .commit()

                binding.sellingPost.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.black
                    )
                )
                binding.review.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            }

            binding.review.setOnClickListener {
                val reviewFragment = ReviewFragment.newInstance(null)
                childFragmentManager.beginTransaction()
                    .replace(R.id.myPageFrameLayout, reviewFragment)
                    .commit()

                binding.sellingPost.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.gray
                    )
                )
                binding.review.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
        } else {
            val mainActivity = activity as MainActivity
            mainActivity.hideBottomNavigation(true)

            binding.backButton.visibility = View.VISIBLE
            binding.settingButton.visibility = View.INVISIBLE
            binding.profileChatButton.visibility = View.VISIBLE

            binding.backButton.setOnClickListener {
                if (postId.isNullOrEmpty()) {
                    findNavController().popBackStack()
                } else {
                    val action =
                        ProfileFragmentDirections.actionProfileFragmentToPostDetailFragment(
                            writerId = writerId!!,
                            postId = postId
                        )
                    findNavController().navigate(action)
                }
            }

            if (userId == writerId) {
                binding.profileChatButton.apply {
                    text = "나의채팅"
                    setOnClickListener {
                        val mainActivity = activity as MainActivity
                        mainActivity.hideBottomNavigation(false)
                        findNavController().navigate(R.id.action_profileFragment_to_chatFragment)
                    }
                }
            } else {
                binding.profileChatButton.setOnClickListener {
                    val chatRoomDB =
                        Firebase.database.reference.child("ChatRooms").child(userId!!)
                            .child(writerId!!)
                    var chatRoomId = ""

                    chatRoomDB.get().addOnSuccessListener {

                        if (it.value != null) {
                            val chatRoom = it.getValue(ChatItem::class.java)
                            chatRoomId = chatRoom?.chatRoomId!!

                            val action =
                                ProfileFragmentDirections.actionProfileFragmentToChatDetailFragment(
                                    chatRoomId = chatRoomId,
                                    otherUserId = writerId!!
                                )
                            findNavController().navigate(action)
                        } else {
                            chatRoomId = UUID.randomUUID().toString()
                            Firebase.database.reference.child("Posts").child(postId!!)
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val post = snapshot.getValue(PostItem::class.java)!!

                                        val newChatRoom = ChatItem(
                                            chatRoomId = chatRoomId,
                                            otherUserId = post.writerId,
                                            otherUserProfile = post.writerProfileImage,
                                            otherUserName = post.writerNickname,
                                        )
                                        chatRoomDB.setValue(newChatRoom)
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })

                            val action =
                                ProfileFragmentDirections.actionProfileFragmentToChatDetailFragment(
                                    chatRoomId = chatRoomId,
                                    otherUserId = writerId!!
                                )
                            findNavController().navigate(action)
                        }
                    }
                }
            }
            binding.sellingPost.setOnClickListener {
                val sellingFragment = SellingFragment.newInstance(writerId!!)
                childFragmentManager.beginTransaction()
                    .replace(R.id.myPageFrameLayout, sellingFragment)
                    .commit()

                binding.sellingPost.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.black
                    )
                )
                binding.review.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            }

            binding.review.setOnClickListener {
                val reviewFragment = ReviewFragment.newInstance(writerId)
                childFragmentManager.beginTransaction()
                    .replace(R.id.myPageFrameLayout, reviewFragment)
                    .commit()

                binding.sellingPost.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.gray
                    )
                )
                binding.review.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
        }
        val sellingFragment = SellingFragment.newInstance(writerId)
        childFragmentManager.beginTransaction()
            .replace(R.id.myPageFrameLayout, sellingFragment)
            .commit()
    }
}