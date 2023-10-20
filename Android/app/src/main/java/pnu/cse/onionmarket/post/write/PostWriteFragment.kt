package pnu.cse.onionmarket.post.write

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import pnu.cse.onionmarket.MainActivity
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.FragmentPostWriteBinding
import pnu.cse.onionmarket.home.HomeFragmentDirections
import pnu.cse.onionmarket.home.HomePostAdapter
import pnu.cse.onionmarket.post.PostItem
import pnu.cse.onionmarket.post.write.WriteImageAdapter.Companion.imageCount
import java.util.UUID

class PostWriteFragment : Fragment(R.layout.fragment_post_write) {
    private lateinit var binding: FragmentPostWriteBinding
    private lateinit var writeImageAdapter: WriteImageAdapter
    private lateinit var homePostAdapter: HomePostAdapter

    private var imageList: MutableList<Uri> = mutableListOf()
    private lateinit var postId: String
    private val writerId = Firebase.auth.currentUser?.uid!!

    private var uploadedUris: MutableList<String> = mutableListOf()
    private var uploadedUrls: MutableList<String> = mutableListOf()

    private var editPost: PostItem = PostItem()

    private val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
            if (uris != null) {
                if (imageList.size + uris.size > 10) {
                    Toast.makeText(context, "사진은 최대 10장까지 선택 가능합니다.", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }
                val writeImageItems = uris.mapIndexed { index, uri ->
                    WriteImageItem(UUID.randomUUID().toString(), uri.toString())
                }
                imageList.addAll(uris)
                writeImageAdapter.setPostImageItemList(writeImageItems)
                binding.imageCount.text = imageCount.toString()
            } else {

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
        binding = FragmentPostWriteBinding.bind(view)

        writeImageAdapter = WriteImageAdapter(binding.imageCount)

        homePostAdapter = HomePostAdapter { post ->
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToPostDetailFragment(
                    post.writerId.orEmpty(), post.postId.orEmpty()
                )
            )
        }

        val args: PostWriteFragmentArgs by navArgs()

        if (args.postId.isNullOrEmpty()) {
            postId = UUID.randomUUID().toString()
        }
        // 게시글 수정
        else {
            postId = args.postId
            binding.toolbarText.text = "게시글 수정"

            Firebase.database.reference.child("Posts").child(postId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        editPost = snapshot.getValue(PostItem::class.java)!!

                        uploadedUris.clear()

                        val imageUris = editPost.postImagesUri!!.map { Uri.parse(it) }

                        val editImageItems =
                            editPost.postImagesUri!!.mapIndexed { index, imageUrl ->
                                WriteImageItem(UUID.randomUUID().toString(), imageUrl)
                            }

                        imageList.addAll(imageUris)
                        writeImageAdapter.setPostImageItemList(editImageItems)

                        binding.imageCount.text = imageCount.toString()
                        binding.writePostTitle.setText(editPost.postTitle)
                        binding.writePostPrice.setText(editPost.postPrice)
                        binding.writePostContent.setText(editPost.postContent)

                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.addImageButton.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.submitButton.setOnClickListener {
            val imm =
                context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)

            if (binding.writePostPrice.text.toString().toInt() < 10000) {
                Toast.makeText(context, "가격을 10000원 이상 설정해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (imageList.isNotEmpty() && !binding.writePostTitle.text.isNullOrBlank()
                && !binding.writePostPrice.text.isNullOrBlank() && !binding.writePostContent.text.isNullOrBlank()
            ) {

                showProgress()

                // 기존 이미지 삭제
                val imageRef =
                    Firebase.storage.reference.child(
                        "posts/${postId}"
                    )
                imageRef.listAll()
                    .addOnSuccessListener { listResult ->
                        // Delete each image in the list
                        val deletePromises =
                            mutableListOf<Task<Void>>()
                        listResult.items.forEach { item ->
                            val deletePromise =
                                item.delete()
                            deletePromises.add(
                                deletePromise
                            )
                        }

                        Tasks.whenAllComplete(
                            deletePromises
                        )
                            .addOnSuccessListener {
                            }
                            .addOnFailureListener { exception ->
                            }
                    }
                    .addOnFailureListener { exception -> }

                val imageUris = writeImageAdapter.imageList.map { Uri.parse(it.imageUrl) }

                uploadImages(imageUris,
                    successHandler = {
                        uploadPost(it, uploadedUrls)
                    },
                    errorHandler = {
                        Toast.makeText(context, "갤러리에 존재하는 이미지만 업로드할 수 있습니다.", Toast.LENGTH_SHORT)
                            .show()
                        hideProgress()
                    })
            } else {
                Toast.makeText(context, "게시글 정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                hideProgress()
            }
        }

        binding.imageRecyclerview.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = writeImageAdapter
        }
    }

    private fun uploadImages(
        uris: List<Uri>,
        successHandler: (List<String>) -> Unit,
        errorHandler: (Throwable?) -> Unit
    ) {

        fun uploadNextImage(index: Int) {
            if (index >= uris.size) {
                successHandler(uploadedUris)
                return
            }

            val uri = uris[index]
            val imageId = UUID.randomUUID().toString() // 각 이미지마다 새로운 UUID 생성
            val fileName = "${imageId}.png"
            Firebase.storage.reference.child("posts/${postId}").child(fileName)
                .putFile(uri)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.storage?.downloadUrl?.addOnSuccessListener {
                            uploadedUrls.add(it.toString())
                            uploadedUris.add(uri.toString())
                            uploadNextImage(index + 1)
                        }
                    } else {
                        task.exception?.printStackTrace()
                        errorHandler(task.exception)
                    }
                }
        }
        uploadNextImage(0)
    }

    private fun uploadPost(photoUri: List<String>, uploadedUrls: MutableList<String>) {
        val addPostList = mutableListOf<PostItem>()

        Firebase.database.reference.child("Users").child(writerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var writerNickname = snapshot.child("userNickname").getValue(String::class.java)
                    var writerPhone = snapshot.child("userPhone").getValue(String::class.java)
                    var writerStar = snapshot.child("userStar").getValue(Double::class.java)

                    val post = PostItem(
                        postId = postId,
                        createdAt = editPost.createdAt ?: System.currentTimeMillis(),
                        postImagesUri = photoUri,
                        postImagesUrl = uploadedUrls,
                        postThumbnailUrl = uploadedUrls[0],
                        postTitle = binding.writePostTitle.text.toString(),
                        postPrice = binding.writePostPrice.text.toString(),
                        postContent = binding.writePostContent.text.toString(),
                        postStatus = true,
                        writerId = writerId,
                        writerNickname = writerNickname,
                        writerPhone = writerPhone,
                        writerStar = writerStar,
                        buyerId = "",
                        reviewWrite = false
                    )

                    Firebase.database.reference.child("Posts").child(postId).setValue(post)
                        .addOnSuccessListener {
                            if (Firebase.auth.currentUser?.uid.isNullOrEmpty())
                                return@addOnSuccessListener
                            addPostList.add(post)
                            homePostAdapter.submitList(addPostList)
                            findNavController().popBackStack()
                            hideProgress()
                        }.addOnFailureListener {
                            Toast.makeText(context, "게시글 정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                            hideProgress()
                        }
                    hideProgress()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showProgress() {
        binding.progressBarLayout.visibility = View.VISIBLE
        animateProgressBar(true)
    }

    private fun hideProgress() {
        binding.progressBarLayout.visibility = View.GONE
        animateProgressBar(false)
    }

    private fun animateProgressBar(show: Boolean) {
        val fadeInDuration = 500L
        val fadeOutDuration = 500L

        val fadeIn = AlphaAnimation(0.2f, 0.8f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = fadeInDuration
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.progressImageView.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation) {
                val fadeOut = AlphaAnimation(0.8f, 0.2f)
                fadeOut.interpolator = AccelerateInterpolator()
                fadeOut.duration = fadeOutDuration
                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}

                    override fun onAnimationEnd(animation: Animation) {
                        binding.progressImageView.visibility = View.GONE
                        if (show)
                            binding.progressImageView.startAnimation(fadeIn)
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })

                binding.progressImageView.startAnimation(fadeOut)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        binding.progressImageView.startAnimation(fadeIn)
    }
}