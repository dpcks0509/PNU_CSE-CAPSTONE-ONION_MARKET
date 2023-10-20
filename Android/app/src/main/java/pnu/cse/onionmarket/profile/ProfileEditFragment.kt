package pnu.cse.onionmarket.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import pnu.cse.onionmarket.MainActivity
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.FragmentProfileEditBinding
import java.util.*

class ProfileEditFragment : Fragment(R.layout.fragment_profile_edit) {
    private lateinit var binding: FragmentProfileEditBinding

    private var selectedUri: Uri? = null
    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedUri = uri
                binding.photoImageView.setImageURI(uri)
                binding.plusButton.isVisible = false
                binding.deleteButton.isVisible = true
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
        binding = FragmentProfileEditBinding.bind(view)

        val userId = Firebase.auth.currentUser?.uid

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.submitButton.setOnClickListener {
            showProgress()


            val imageId = UUID.randomUUID().toString()
            val fileName = "${imageId}.png"

            Firebase.storage.reference.child("profiles/${userId}").listAll()
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

            Firebase.storage.reference.child("profiles/${userId}/").child(fileName)
                .putFile(selectedUri!!)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        task.result?.storage?.downloadUrl?.addOnSuccessListener { downloadUri ->
                            val updateUserProfileImage: MutableMap<String, Any> = hashMapOf(
                                "Users/$userId/userProfileImage" to downloadUri.toString()
                            )

                            Firebase.database.reference.updateChildren(updateUserProfileImage)
                        }
                        hideProgress()
                        findNavController().popBackStack()
                    }
                }
        }

        binding.plusButton.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

            if (selectedUri != null) {
                val photoUri = selectedUri ?: return@setOnClickListener

                Glide.with(binding.photoImageView)
                    .load(photoUri)
                    .into(binding.photoImageView)
            }
        }

        binding.deleteButton.setOnClickListener {
            binding.photoImageView.setImageURI(null)
            selectedUri = null
            binding.deleteButton.isVisible = false
            binding.plusButton.isVisible = true
        }
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