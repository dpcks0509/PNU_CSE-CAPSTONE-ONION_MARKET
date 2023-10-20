package pnu.cse.onionmarket

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pnu.cse.onionmarket.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일 또는 패스워드가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showProgress()

            Firebase.auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { login ->
                    val currentUser = Firebase.auth.currentUser

                    // 로그인 성공
                    if (login.isSuccessful && currentUser != null) {
                        hideProgress()

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    // 로그인 실패
                    else {
                        hideProgress()
                        Toast.makeText(this, "이메일 또는 패스워드를 다시 확인해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.gotoJoin.setOnClickListener {
            startActivity(Intent(this, JoinActivity::class.java))
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