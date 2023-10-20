package pnu.cse.onionmarket

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import pnu.cse.onionmarket.databinding.ActivityMainBinding
import pnu.cse.onionmarket.home.HomeFragmentDirections
import pnu.cse.onionmarket.service.RetrofitService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        val gson: Gson = GsonBuilder().setLenient().create()

        val retrofit = Retrofit.Builder().baseUrl("http://43.201.103.235:8080")
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

        val retrofitService = retrofit.create(RetrofitService::class.java)
    }

    override fun onResume() {
        super.onResume()

        // 알림 제거
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인 상태가 아니면 로그인 창으로 이동
        val currentUser = Firebase.auth.currentUser?.uid
        if (currentUser == null || currentUser.toString()
                .contains("com.google.firebase.auth.internal")
        ) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val myUserId = Firebase.auth.currentUser?.uid
        val chatRoomId = intent.getStringExtra("chatRoomId")
        val otherUserId = intent.getStringExtra("otherUserId")

        if (!chatRoomId.isNullOrEmpty()) {

            Firebase.database.reference.child("ChatRooms").child(myUserId!!).child(otherUserId!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val updates: MutableMap<String, Any> = hashMapOf(
                                "ChatRooms/$myUserId/${otherUserId}/unreadMessage" to 0
                            )
                            Firebase.database.reference.updateChildren(updates)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })

            val action = HomeFragmentDirections.actionHomeFragmentToChatDetailFragment(
                chatRoomId = chatRoomId, otherUserId = otherUserId!!
            )
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(action)
        }

        // navigation 연결
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNavigationview.setupWithNavController(navHostFragment.navController)

        askNotificationPermission()

    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
        } else {
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showPermissionRationalDialog()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showPermissionRationalDialog() {
        AlertDialog.Builder(this).setMessage("채팅 알림을 받기위해서 알림 권한이 필요합니다.")
            .setPositiveButton("알림 허용") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }.setNegativeButton("취소") { dialogInterface, _ -> dialogInterface.cancel() }.show()
    }

    fun hideBottomNavigation(hide: Boolean) {
        if (hide) binding.bottomNavigationview.visibility = View.GONE
        else binding.bottomNavigationview.visibility = View.VISIBLE
    }
}