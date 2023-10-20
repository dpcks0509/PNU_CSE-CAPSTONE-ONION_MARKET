package pnu.cse.onionmarket.profile.review

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
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
import pnu.cse.onionmarket.MainActivity.Companion.retrofitService
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.FragmentReviewBinding
import pnu.cse.onionmarket.service.BlockchainReviewItem

class
ReviewFragment : Fragment(R.layout.fragment_review) {
    private lateinit var binding: FragmentReviewBinding
    private lateinit var reviewAdapter: ReviewAdapter

    private var reviewList = mutableListOf<ReviewItem>()
    private var blockchainReviewList = mutableListOf<BlockchainReviewItem>()

    private var profileUserId: String? = null

    companion object {
        fun newInstance(profileUserId: String?): ReviewFragment {
            val fragment = ReviewFragment()
            val args = Bundle()
            args.putString("profileUserId", profileUserId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentReviewBinding.bind(view)

        arguments?.let {
            // Retrieve the writerId from the arguments bundle
            profileUserId = it.getString("profileUserId")
        }

        reviewAdapter = ReviewAdapter()

        binding.reviewRecyclerview.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = reviewAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        }

        if (profileUserId == null) {
            profileUserId = Firebase.auth.currentUser?.uid
            reviewList = mutableListOf()
            blockchainReviewList = mutableListOf()

            Firebase.database.reference.child("Reviews")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        reviewList = mutableListOf()

                        snapshot.children.map {
                            val review = it.getValue(ReviewItem::class.java)
                            review ?: return
                            if (review.userId == profileUserId)
                                reviewList.add(review)
                        }
                        reviewList.sortByDescending { it.createdAt }
                        reviewAdapter.submitList(reviewList)

                        if (reviewAdapter.currentList.isEmpty()) {
                            binding.noReview.visibility = View.VISIBLE
                        } else {
                            binding.noReview.visibility = View.GONE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })

            // users star 업데이트
            Firebase.database.reference.child("Reviews")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var userReviewSum = 0.0
                        var userReviewNumber = 0
                        var userReviewStar = 0.0

                        snapshot.children.map {
                            val review = it.getValue(ReviewItem::class.java)
                            review ?: return

                            if (review.userId == profileUserId) {
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
                            "Users/$profileUserId/userStar" to userReviewStar
                        )

                        Firebase.database.reference.updateChildren(update)


                    }

                    override fun onCancelled(error: DatabaseError) {}

                })

            Firebase.database.reference.child("Reviews")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        reviewList = mutableListOf()

                        snapshot.children.map {
                            val review = it.getValue(ReviewItem::class.java)
                            review ?: return
                            if (review.userId == profileUserId)
                                reviewList.add(review)
                        }
                        reviewList.sortByDescending { it.createdAt }
                        reviewAdapter.submitList(reviewList)

                        if (reviewAdapter.currentList.isEmpty()) {
                            binding.noReview.visibility = View.VISIBLE
                        } else {
                            binding.noReview.visibility = View.GONE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })

            // users star 업데이트
            Firebase.database.reference.child("Reviews")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var userReviewSum = 0.0
                        var userReviewNumber = 0
                        var userReviewStar = 0.0

                        snapshot.children.map {
                            val review = it.getValue(ReviewItem::class.java)
                            review ?: return

                            if (review.userId == profileUserId) {
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
                            "Users/$profileUserId/userStar" to userReviewStar
                        )

                        Firebase.database.reference.updateChildren(update)


                    }

                    override fun onCancelled(error: DatabaseError) {}

                })
        } else {
            reviewList = mutableListOf()
            blockchainReviewList = mutableListOf()

            var getReview = false
            val reviewJob = CoroutineScope(Dispatchers.IO).async {
                retrofitService.getReviews(profileUserId!!).execute().let { response ->
                    if (response.isSuccessful) {
                        blockchainReviewList = response.body()?.toMutableList()!!
                        getReview = true
                    }
                }
                getReview
            }

            var userReviewSum = 0.0
            var userReviewNumber = 0
            var userReviewStar = 0.0

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

                        reviewList.add(review)
                    }
                }
                reviewList.sortByDescending { it.createdAt }
                reviewAdapter.submitList(reviewList)

                if (reviewAdapter.currentList.isEmpty()) {
                    binding.noReview.visibility = View.VISIBLE
                } else {
                    binding.noReview.visibility = View.GONE
                }
            }
        }
    }
}