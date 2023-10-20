package pnu.cse.onionmarket.profile.selling

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
import pnu.cse.onionmarket.databinding.FragmentSellingBinding
import pnu.cse.onionmarket.post.PostItem
import pnu.cse.onionmarket.profile.ProfileFragmentDirections
import pnu.cse.onionmarket.service.BlockchainPostItem

class SellingFragment : Fragment(R.layout.fragment_selling) {
    private lateinit var binding: FragmentSellingBinding
    private lateinit var sellingAdapter: SellingAdapter

    private var sellingList = mutableListOf<PostItem>()
    private var blockchainSellingList = mutableListOf<BlockchainPostItem>()
    private val userId = Firebase.auth.currentUser?.uid

    private var writerId: String? = null

    companion object {
        fun newInstance(writerId: String?): SellingFragment {
            val fragment = SellingFragment()
            val args = Bundle()
            args.putString("writerId", writerId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSellingBinding.bind(view)

        arguments?.let {
            writerId = it.getString("writerId")
        }

        sellingAdapter = SellingAdapter { post ->
            val action = ProfileFragmentDirections.actionProfileFragmentToPostDetailFragment(
                post.writerId.orEmpty(),
                post.postId.orEmpty(),
                true
            )
            findNavController().navigate(action)
        }

        if (writerId == null) {
            sellingList = mutableListOf()
            blockchainSellingList = mutableListOf()

            Firebase.database.reference.child("Posts")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        sellingList = mutableListOf<PostItem>()

                        snapshot.children.forEach {
                            val post = it.getValue(PostItem::class.java)
                            post ?: return

                            if (post.writerId == userId)
                                sellingList.add(post)
                        }
                        sellingList.sortByDescending { it.createdAt }
                        sellingAdapter.submitList(sellingList)

                        if (sellingList.isEmpty()) {
                            binding.noSelling.visibility = View.VISIBLE
                        } else {
                            binding.noSelling.visibility = View.GONE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        } else {
            sellingList = mutableListOf()
            blockchainSellingList = mutableListOf()

            var getPost = false
            val postJob = CoroutineScope(Dispatchers.IO).async {
                retrofitService.getPosts(writerId!!).execute().let { response ->
                    if (response.isSuccessful) {
                        blockchainSellingList = response.body()?.toMutableList()!!
                        getPost = true
                    }
                }
                getPost
            }

            runBlocking {
                val getResult = postJob.await()

                if (getResult) {
                    for (blockchainPost in blockchainSellingList) {
                        val postImageUrl = blockchainPost.postImageUrl?.removeSurrounding("[", "]")
                            ?.split(", ")
                            ?.map { it.trim() }


                        val post = PostItem(
                            postId = blockchainPost.postId,
                            createdAt = blockchainPost.createdAt,
                            postImagesUri = null,
                            postImagesUrl = postImageUrl,
                            postThumbnailUrl = postImageUrl?.get(0),
                            postTitle = blockchainPost.postTitle,
                            postPrice = blockchainPost.postPrice,
                            postContent = blockchainPost.postContent,
                            postStatus = null,
                            writerId = blockchainPost.userId,
                            writerNickname = blockchainPost.userNickname,
                            writerPhone = blockchainPost.userPhone,
                            writerStar = null,
                            buyerId = "",
                            reviewWrite = null
                        )
                        sellingList.add(post)
                    }
                }
            }
        }

        sellingList.sortByDescending { it.createdAt }
        sellingAdapter.submitList(sellingList)

        if (sellingList.isEmpty()) {
            binding.noSelling.visibility = View.VISIBLE
        } else {
            binding.noSelling.visibility = View.GONE
        }

        binding.sellingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = sellingAdapter
        }
    }
}