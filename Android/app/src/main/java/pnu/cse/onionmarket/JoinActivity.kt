package pnu.cse.onionmarket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import pnu.cse.onionmarket.databinding.ActivityJoinBinding

class JoinActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJoinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.joinButton.setOnClickListener {
            val email = binding.joinEmail.text.toString()
            val password = binding.joinPassword.text.toString()
            val phone = binding.joinPhone.text.toString()
            val nickname = binding.joinNickname.text.toString()

            if (email.isEmpty() || password.isEmpty() || phone.isEmpty() || nickname.isEmpty()) {
                Toast.makeText(this, "회원가입에 필요한 정보를\n모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Firebase.database.reference.child("Users")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var nicknameExists = false
                        var phoneExits = false
                        for (userSnapshot in snapshot.children) {
                            val userNickname =
                                userSnapshot.child("userNickname").getValue(String::class.java)
                            if (userNickname == nickname) {
                                nicknameExists = true
                                break
                            }
                        }

                        for (userSnapshot in snapshot.children) {
                            val userPhone =
                                userSnapshot.child("userPhone").getValue(String::class.java)
                            if (userPhone == phone) {
                                phoneExits = true
                                break
                            }
                        }
                        if (nicknameExists) {
                            Toast.makeText(
                                applicationContext,
                                "이미 사용 중인 닉네임입니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (phoneExits) {
                            Toast.makeText(
                                applicationContext,
                                "이미 가입된 핸드폰번호입니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            showProgress()
                            Firebase.auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { join ->
                                    val currentUser = Firebase.auth.currentUser
                                    // 회원가입 성공
                                    if (join.isSuccessful && currentUser != null) {
                                        hideProgress()
                                        Toast.makeText(
                                            this@JoinActivity,
                                            "회원가입이 완료되었습니다.\n로그인 해주세요.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        val userId = currentUser.uid


                                        Firebase.messaging.token.addOnCompleteListener {
                                            val token = it.result
                                            val user = UserItem(
                                                userId = userId,
                                                userNickname = nickname,
                                                userPhone = phone,
                                                userStar = 0.0,
                                                userToken = token,
                                            )
                                            Firebase.database.reference.child("Users").child(userId)
                                                .setValue(user)
                                            Firebase.auth.signOut()
                                            startActivity(
                                                Intent(
                                                    this@JoinActivity,
                                                    LoginActivity::class.java
                                                )
                                            )
                                        }
                                    }
                                    // 회원가입 실패
                                    else {
                                        hideProgress()
                                        Toast.makeText(
                                            this@JoinActivity,
                                            "회원가입에 실패하였습니다.\n입력정보를 확인해주세요.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
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