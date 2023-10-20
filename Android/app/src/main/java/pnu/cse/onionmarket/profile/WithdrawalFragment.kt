package pnu.cse.onionmarket.profile

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import pnu.cse.onionmarket.LoginActivity
import pnu.cse.onionmarket.MainActivity
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.FragmentWithdrawalBinding
import pnu.cse.onionmarket.post.PostItem

class WithdrawalFragment : Fragment(R.layout.fragment_withdrawal) {
    private lateinit var binding: FragmentWithdrawalBinding

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
        binding = FragmentWithdrawalBinding.bind(view)

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.withdrawalButton.setOnClickListener {

            val builder = AlertDialog.Builder(context)
            builder
                .setTitle("회원 탈퇴")
                .setMessage(
                    "정말로 회원 탈퇴를 하시겠습니까?\n" +
                            "탈퇴 후 데이터는 복구되지 않습니다."
                )
                .setPositiveButton("탈퇴",
                    DialogInterface.OnClickListener { dialog, id ->
                        // 회원탈퇴
                        val password = binding.withdrawalPassword.text.toString()
                        val passwordConfirm = binding.withdrawalPasswordConfirm.text.toString()
                        if (password != passwordConfirm) {
                            Toast.makeText(context, "두개의 비밀번호가 일치하지않습니다.", Toast.LENGTH_SHORT)
                                .show()
                            return@OnClickListener
                        }

                        val user = Firebase.auth.currentUser
                        val userId = user?.uid
                        val userCredential =
                            EmailAuthProvider.getCredential(user?.email.toString(), password)

                        user?.reauthenticate(userCredential)
                            ?.addOnSuccessListener {
                                user?.delete()
                                    ?.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val postList: MutableList<PostItem> =
                                                mutableListOf()
                                            Firebase.database.reference.child("Posts")
                                                .addValueEventListener(object :
                                                    ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {

                                                        snapshot.children.forEach {
                                                            val post =
                                                                it.getValue(PostItem::class.java)
                                                            post ?: return

                                                            if (post.writerId == userId) {
                                                                postList.add(post)

                                                                // 게시글 삭제
                                                                Firebase.database.reference.child(
                                                                    "Posts"
                                                                )
                                                                    .child(post.postId!!)
                                                                    .removeValue()
//                                                                // 사진 삭제
//                                                                val postImageRef =
//                                                                    Firebase.storage.reference.child(
//                                                                        "posts/${post.postId}"
//                                                                    )
//                                                                postImageRef.listAll()
//                                                                    .addOnSuccessListener { listResult ->
//                                                                        // Delete each image in the list
//                                                                        val deletePromises =
//                                                                            mutableListOf<Task<Void>>()
//                                                                        listResult.items.forEach { item ->
//                                                                            val deletePromise =
//                                                                                item.delete()
//                                                                            deletePromises.add(
//                                                                                deletePromise
//                                                                            )
//                                                                        }
//
//                                                                        Tasks.whenAllComplete(
//                                                                            deletePromises
//                                                                        )
//                                                                            .addOnSuccessListener {
//                                                                            }
//                                                                            .addOnFailureListener { exception ->
//                                                                            }
//                                                                    }

                                                                Firebase.storage.reference.child("profiles/${userId}")
                                                                    .listAll()
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
                                                                    .addOnFailureListener { exception ->
                                                                    }
                                                            }
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {}
                                                })

                                            Firebase.database.reference.child("Users")
                                                .child(userId!!).removeValue()

                                            Firebase.database.reference.child("ChatRooms")
                                                .child(userId).removeValue()

                                            FirebaseAuth.getInstance().signOut()

                                            val signOutIntent =
                                                Intent(context, LoginActivity::class.java)
                                            signOutIntent.flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                            startActivity(signOutIntent)
                                        }
                                    }
                            }
                            ?.addOnFailureListener {
                                Toast.makeText(context, "계정의 비밀번호가 일치하지않습니다.", Toast.LENGTH_SHORT)
                                    .show()
                            }

                    })
                .setNegativeButton("취소",
                    DialogInterface.OnClickListener { dialog, id -> })
            builder.create()
            builder.show()
        }
    }
}