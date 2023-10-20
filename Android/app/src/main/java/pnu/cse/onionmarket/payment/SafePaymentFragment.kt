package pnu.cse.onionmarket.payment

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.skydoves.balloon.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
import pnu.cse.onionmarket.chat.detail.ChatDetailFragment.Companion.unreadMessage
import pnu.cse.onionmarket.chat.detail.ChatDetailItem
import pnu.cse.onionmarket.databinding.FragmentSafePaymentBinding
import pnu.cse.onionmarket.payment.transaction.TransactionAdapter
import pnu.cse.onionmarket.payment.transaction.TransactionItem
import pnu.cse.onionmarket.payment.workmanager.AfterDeliveredWorker
import pnu.cse.onionmarket.payment.workmanager.WaybillRegistrationWorker
import pnu.cse.onionmarket.post.PostItem
import pnu.cse.onionmarket.service.BlockchainPostItem
import pnu.cse.onionmarket.service.PaymentItem
import pnu.cse.onionmarket.wallet.WalletFragmentDirections
import pnu.cse.onionmarket.wallet.WalletItem
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class SafePaymentFragment : Fragment(R.layout.fragment_safe_payment) {
    private lateinit var binding: FragmentSafePaymentBinding
    private val args: SafePaymentFragmentArgs by navArgs()

    private lateinit var chatDetailAdapter: ChatDetailAdapter
    private lateinit var transactionAdapter: TransactionAdapter

    private var transactionList = mutableListOf<TransactionItem>()

    private val chatDetailItemList = mutableListOf<ChatDetailItem>()

    private var chatRoomId: String = ""
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var otherUserPhone: String = ""
    private var otherUserToken: String = ""
    private var otherUserProfileImage: String = ""
    private var myUserId: String = ""
    private var myUserName: String = ""
    private var myUserProfileImage: String = ""
    private var myUserToken: String = ""
    private var transactionId: String = ""
    private var buyerWalletKey: String = ""
    private var sellerWalletKey: String = ""

    private var userId: String = ""
    private var postId: String = ""
    private var writerId: String = ""

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

    private var mContext: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSafePaymentBinding.bind(view)
        myUserId = Firebase.auth.currentUser?.uid!!

        chatDetailAdapter = ChatDetailAdapter()

        transactionAdapter = TransactionAdapter { item ->
            val action = WalletFragmentDirections.actionWalletFragmentToPostDetailFragment(
                postId = item.postId!!,
                writerId = item.sellerId!!
            )
            findNavController().navigate(action)
        }


        userId = Firebase.auth.currentUser?.uid!!
        postId = args.postId
        writerId = args.writerId

        var postThumbnailImage = ""
        var postTitle = ""
        var postPrice = ""
        var postContent = ""
        var createdAt: Long = 0
        var postImages: List<String>? = emptyList()
        otherUserId = writerId

        Firebase.database.reference.child("Posts").child(postId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val post = snapshot.getValue(PostItem::class.java)
                    postThumbnailImage = post?.postThumbnailUrl!!
                    postTitle = post?.postTitle!!
                    postPrice = post?.postPrice!!
                    postContent = post?.postContent!!
                    postImages = post?.postImagesUrl
                    createdAt = post?.createdAt!!

                    Glide.with(binding.postThumbnail)
                        .load(postThumbnailImage)
                        .into(binding.postThumbnail)

                    binding.postTitle.text = postTitle

                    var priceWithoutCommas = postPrice
                    var formattedPrice = StringBuilder()
                    var commaCounter = 0

                    for (i in priceWithoutCommas.length - 1 downTo 0) {
                        formattedPrice.append(priceWithoutCommas[i])
                        commaCounter++

                        if (commaCounter == 3 && i > 0) {
                            formattedPrice.append(",")
                            commaCounter = 0
                        }
                    }
                    formattedPrice.reverse()
                    binding.postPrice.text = "${formattedPrice}원"

                    val postPrice = post?.postPrice?.toInt()
                    val chargePrice = 1000
                    val totalPrice = postPrice?.plus(chargePrice!!)

                    priceWithoutCommas = postPrice.toString()
                    formattedPrice = StringBuilder()
                    commaCounter = 0

                    for (i in priceWithoutCommas.length - 1 downTo 0) {
                        formattedPrice.append(priceWithoutCommas[i])
                        commaCounter++

                        if (commaCounter == 3 && i > 0) {
                            formattedPrice.append(",")
                            commaCounter = 0
                        }
                    }
                    formattedPrice.reverse()
                    binding.detailPostPrice.text = "${formattedPrice}원"

                    priceWithoutCommas = chargePrice.toString()
                    formattedPrice = StringBuilder()
                    commaCounter = 0

                    for (i in priceWithoutCommas.length - 1 downTo 0) {
                        formattedPrice.append(priceWithoutCommas[i])
                        commaCounter++

                        if (commaCounter == 3 && i > 0) {
                            formattedPrice.append(",")
                            commaCounter = 0
                        }
                    }
                    formattedPrice.reverse()
                    binding.chargePrice.text = "${formattedPrice}원"

                    priceWithoutCommas = totalPrice.toString()
                    formattedPrice = StringBuilder()
                    commaCounter = 0

                    for (i in priceWithoutCommas.length - 1 downTo 0) {
                        formattedPrice.append(priceWithoutCommas[i])
                        commaCounter++

                        if (commaCounter == 3 && i > 0) {
                            formattedPrice.append(",")
                            commaCounter = 0
                        }
                    }
                    formattedPrice.reverse()
                    binding.totalPrice.text = "${formattedPrice}원"
                }

                override fun onCancelled(error: DatabaseError) {}

            })

        Firebase.database.reference.child("Wallets")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val wallet = it.getValue(WalletItem::class.java)
                        wallet ?: return
                        if (wallet.userId == userId) {
                            if (!wallet.walletImage.isNullOrEmpty())
                                Glide.with(binding.walletImage)
                                    .load(wallet.walletImage)
                                    .into(binding.walletImage)
                            binding.walletName.text = wallet.walletName
                            buyerWalletKey = wallet.privateKey!!


                            var walletMoney = "0"
                            var getMoney = false

                            val walletJob = CoroutineScope(Dispatchers.IO).async {
                                retrofitService.getWalletMoney(wallet.privateKey!!).execute()
                                    .let { response ->
                                        if (response.isSuccessful) {
                                            walletMoney =
                                                (response.body().toString().replace("ETH", "")
                                                    .toDouble()).times(2000000).toInt().toString()
                                            getMoney = true
                                        }
                                    }
                                getMoney
                            }

                            runBlocking {
                                val walletResult = walletJob.await()

                                if (walletResult) {

                                    val updates: MutableMap<String, Any> = hashMapOf(
                                        "Wallets/${wallet.walletId}/walletMoney" to walletMoney,
                                    )

                                    Firebase.database.reference.updateChildren(updates)

                                    val priceWithoutCommas = walletMoney
                                    val formattedPrice = StringBuilder()
                                    var commaCounter = 0

                                    for (i in priceWithoutCommas.length - 1 downTo 0) {
                                        formattedPrice.append(priceWithoutCommas[i])
                                        commaCounter++

                                        if (commaCounter == 3 && i > 0) {
                                            formattedPrice.append(",")
                                            commaCounter = 0
                                        }
                                    }
                                    formattedPrice.reverse()
                                    binding.walletMoney.text = "$formattedPrice 원"
                                }
                            }


                            var priceWithoutCommas = wallet.walletMoney.toString()
                            var formattedPrice = StringBuilder()
                            var commaCounter = 0

                            for (i in priceWithoutCommas.length - 1 downTo 0) {
                                formattedPrice.append(priceWithoutCommas[i])
                                commaCounter++

                                if (commaCounter == 3 && i > 0) {
                                    formattedPrice.append(",")
                                    commaCounter = 0
                                }
                            }
                            formattedPrice.reverse()
                            binding.walletMoney.text = "${formattedPrice}원"
                            return@forEach
                        }

                        if (wallet.userId == otherUserId) {
                            sellerWalletKey = wallet.privateKey!!
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}

            })

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        Firebase.database.reference.child("Users").child(userId!!).get()
            .addOnSuccessListener {
                val myUserItem = it.getValue(UserItem::class.java)
                myUserName = myUserItem?.userNickname ?: ""
                myUserProfileImage = myUserItem?.userProfileImage ?: ""
                myUserToken = myUserItem?.userToken ?: ""

                getOtherUserData()
            }

        val itemList =
            listOf(
                "택배사", "CJ대한통운", "우체국택배", "한진택배", "롯데택배", "홈픽", "로젠택배", "GS25편의점택배", "CU 편의점택배",
                "경동택배", "대신택배", "일양로지스"
            )
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, itemList)
        binding.waybillCompany.adapter = adapter

        Firebase.database.reference.child("Transactions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val context = context ?: return

                    snapshot.children.map {
                        val transaction = it.getValue(TransactionItem::class.java)
                        transaction ?: return
                        if (transaction.postId == postId) {
                            binding.waybillText.visibility = View.VISIBLE
                            binding.waybillInfo.visibility = View.VISIBLE
                            binding.waybillTooltip.visibility = View.VISIBLE
                            binding.waybillCheckButton.visibility = View.VISIBLE
                            binding.completeButton.visibility = View.VISIBLE
                            binding.walletText.visibility = View.GONE
                            binding.walletInfo.visibility = View.GONE
                            binding.submitButton.visibility = View.INVISIBLE

                            binding.waybillCompany.apply {
                                isClickable = false
                                isFocusable = false
                                isEnabled = false
                            }

                            binding.waybillNumber.apply {
                                isClickable = false
                                isFocusable = false
                            }

                            transactionId = transaction.transactionId!!

                            if (transaction.deliveryArrived == true) {
                                binding.waybillCheckButton.apply {
                                    text = "배송완료"
                                    isClickable = false
                                    isFocusable = false
                                    backgroundTintList = ContextCompat.getColorStateList(
                                        binding.root.context,
                                        R.color.light_gray
                                    )
                                }

                                val workManager = WorkManager.getInstance(mContext!!)
                                Log.e("Cancel", " deliveryCheckWorker")
                                workManager.cancelAllWorkByTag("deliveryCheckWorker-$transactionId")

                                val afterDeliveredWorker =
                                    OneTimeWorkRequestBuilder<AfterDeliveredWorker>()
                                        .setInitialDelay(72, TimeUnit.HOURS)
                                        .build()

                                AfterDeliveredWorker.setData(transactionId)

                                workManager.enqueueUniqueWork(
                                    "afterDeliveredWorker-$transactionId",
                                    ExistingWorkPolicy.KEEP,
                                    afterDeliveredWorker
                                )

                            }

                            if (transaction.waybillRegistrationWorker == true) {
                                val updates: MutableMap<String, Any> = hashMapOf(
                                    "Transactions/${transactionId}/waybillRegistrationWorker" to false,
                                )

                                Firebase.database.reference.updateChildren(updates)

                                sendChat(
                                    message = "<안전결제 취소 알림>\n" +
                                            "상품명 : [${binding.postTitle.text}]\n\n" +
                                            " 24시간 이내에 운송장 정보가 등록되지않아,\n" +
                                            " 자동으로 안전결제가 취소되었습니다."
                                )
                            }

                            if (transaction.afterDeliveredWorker == true) {
                                val updates: MutableMap<String, Any> = hashMapOf(
                                    "Transactions/${transactionId}/afterDeliveredWorker" to false,
                                )

                                Firebase.database.reference.updateChildren(updates)

                                sendChat(
                                    message = "<구매확정 알림>\n" +
                                            "상품명 : [${binding.postTitle.text}]\n" +
                                            "상품가격 : ${binding.postPrice.text}원\n\n" +
                                            " 판매하신 상품의 거래가 완료되었습니다.\n" +
                                            " 판매금액은 24시간 이내에 정산됩니다."
                                )

                                retrofitService.savePost(
                                    BlockchainPostItem(
                                        userId = otherUserId,
                                        userNickname = otherUserName,
                                        userPhone = otherUserPhone,
                                        postId = postId,
                                        postImageUrl = postImages.toString(),
                                        postTitle = postTitle,
                                        postPrice = postPrice,
                                        postContent = postContent,
                                        createdAt = createdAt
                                    )
                                ).enqueue(object : retrofit2.Callback<String> {
                                    override fun onResponse(
                                        call: retrofit2.Call<String>,
                                        response: retrofit2.Response<String>
                                    ) {
                                        val state = response.body().toString()
                                        Log.e("savePost", state)
                                    }

                                    override fun onFailure(
                                        call: retrofit2.Call<String>,
                                        t: Throwable
                                    ) {
                                        Log.e("savePost", t.toString())
                                    }
                                })
                            }

                            binding.waybillCheckButton.setOnClickListener {

                                var getDeliveryInfo = false
                                var deliveryState = ""
                                val deliveryJob = CoroutineScope(Dispatchers.IO).async {
                                    retrofitService.deliveryCheck(
                                        transaction.waybillCompanyCode!!,
                                        transaction.waybillNumber!!
                                    ).execute().let { response ->
                                        if (response.isSuccessful) {
                                            deliveryState = response.body().toString()
                                            Log.e("deliveryState", deliveryState)
                                            getDeliveryInfo = true
                                        }
                                    }
                                    getDeliveryInfo
                                }

                                runBlocking {
                                    val getResult = deliveryJob.await()

                                    if (getResult) {
                                        if (deliveryState == "배송완료") {
                                            deliveryCheck(true)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "택배가 이동중입니다. 택배상태 : {$deliveryState}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }

                            binding.waybillTooltip.setOnClickListener {
                                // 툴팁
                                val balloon = Balloon.Builder(requireContext())
                                    .setWidth(BalloonSizeSpec.WRAP)
                                    .setHeight(BalloonSizeSpec.WRAP)
                                    .setText(
                                        "택배 도착후 3일 이내에 구매확정 버튼을 누르지 않을시,\n" +
                                                "자동으로 결제가 완료됩니다."
                                    )
                                    .setTextColorResource(R.color.white)
                                    .setTextSize(15f)
                                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                                    .setArrowSize(10)
                                    .setArrowPosition(0.5f)
                                    .setPadding(12)
                                    .setCornerRadius(8f)
                                    .setBackgroundColorResource(R.color.black)
                                    .setBalloonAnimation(BalloonAnimation.ELASTIC)
                                    .setLifecycleOwner(viewLifecycleOwner)
                                    .build()

                                balloon.showAlignBottom(binding.waybillTooltip)
                            }


                            binding.name.apply {
                                isClickable = false
                                isFocusable = false
                                setText(transaction.name)
                            }
                            binding.phone.apply {
                                isClickable = false
                                isFocusable = false
                                setText(transaction.phone)
                            }
                            binding.address.apply {
                                isClickable = false
                                isFocusable = false
                                setText(transaction.address)
                            }

                            if (!transaction.waybillNumber.isNullOrEmpty()) {

                                binding.toolbarText.text = "구매정보"
                                binding.waybillCompany.apply {
                                    isClickable = false
                                    isFocusable = false
                                    isEnabled = false
                                    setSelection(transaction.waybillCompanyPosition!!)
                                }

                                binding.waybillNumber.apply {
                                    isClickable = false
                                    isFocusable = false
                                    setText(transaction.waybillNumber)
                                }
                            }

                            if (transaction.completePayment == true) {
                                binding.submitButton.visibility = View.GONE
                                binding.completeButton.visibility = View.GONE
                                binding.waybillCheckButton.visibility = View.GONE

                                val workManager = WorkManager.getInstance(mContext!!)
                                Log.e("Cancel", " afterDeliveredWorker")
                                workManager.cancelAllWorkByTag("afterDeliveredWorker-$transactionId")
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        binding.submitButton.setOnClickListener {
            val walletMoney =
                binding.walletMoney.text.toString().replace(",", "").replace("원", "").toDouble()
            val totalMoney =
                binding.totalPrice.text.toString().replace(",", "").replace("원", "").toDouble()

            if (binding.name.text.isNullOrEmpty() || binding.phone.text.isNullOrEmpty() || binding.address.text.isNullOrEmpty()) {
                Toast.makeText(context, "배송지 정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (walletMoney < totalMoney) {
                Toast.makeText(context, "거래에 필요한 지갑 잔액이 부족합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val waybillRegistrationWorker =
                OneTimeWorkRequestBuilder<WaybillRegistrationWorker>()
                    .setInitialDelay(24, TimeUnit.HOURS)
                    .addTag("waybillRegistrationWorker")
                    .build()

            WaybillRegistrationWorker.setData(postId, transactionId)

            val workManager = WorkManager.getInstance(mContext!!)
            workManager.enqueueUniqueWork(
                "waybillRegistrationWorker-$transactionId",
                ExistingWorkPolicy.KEEP,
                waybillRegistrationWorker
            )

            val chatRoomDB =
                Firebase.database.reference.child("ChatRooms").child(userId!!).child(writerId)

            chatRoomDB.get().addOnSuccessListener {
                if (it.value != null) {
                    val chatRoom = it.getValue(ChatItem::class.java)
                    chatRoomId = chatRoom?.chatRoomId!!
                } else {
                    chatRoomId = UUID.randomUUID().toString()
                }
                // 메세지 , 알림 보내기
                val message = "<안전결제 알림>\n" +
                        "상품명 : [${binding.postTitle.text}]\n" +
                        "상품가격 : ${binding.postPrice.text}원\n\n" +
                        "-배송지-\n" +
                        "이름 : ${binding.name.text}\n" +
                        "연락처 : ${binding.phone.text}\n" +
                        "주소 : ${binding.address.text}\n\n" +
                        " 배송지로 택배 배송 후 운송장번호를 등록해주세요.\n" +
                        " 24시간 이내에 등록하지 않을시\n" +
                        " 안전결제가 취소됩니다."

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

                unreadMessage += 1

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
                    "ChatRooms/$otherUserId/$userId/unreadMessage" to unreadMessage,
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

                val transactionId = UUID.randomUUID().toString()
                val createdAt = System.currentTimeMillis()
                val transaction = TransactionItem(
                    transactionId = transactionId,
                    createdAt = createdAt,
                    postId = postId,
                    sellerId = writerId,
                    buyerId = myUserId,
                    postThumbnailImage = postThumbnailImage,
                    postTitle = postTitle,
                    postPrice = postPrice,
                    name = binding.name.text.toString(),
                    phone = binding.phone.text.toString(),
                    address = binding.address.text.toString(),
                    waybillCompanyPosition = 0,
                    waybillCompany = "",
                    waybillCompanyCode = "",
                    waybillNumber = "",
                    deliveryArrived = false,
                    completePayment = false,
                    waybillRegistrationWorker = false,
                    deliveryCheckWorker = false,
                    afterDeliveredWorker = false
                )

                Firebase.database.reference.child("Transactions").child(transactionId)
                    .setValue(transaction)
                    .addOnSuccessListener {
                        val updates: MutableMap<String, Any> = hashMapOf(
                            "Posts/$postId/postStatus" to false,
                            "Posts/$postId/buyerId" to myUserId
                        )
                        Firebase.database.reference.updateChildren(updates)
                    }

                val action =
                    SafePaymentFragmentDirections.actionSafePaymentFragmentToChatDetailFragment(
                        chatRoomId = chatRoomId,
                        otherUserId = writerId
                    )
                findNavController().navigate(action)
            }
        }

        binding.postInfo.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.chargePriceTooltip.setOnClickListener {
            // 툴팁
            val balloon = Balloon.Builder(requireContext())
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(BalloonSizeSpec.WRAP)
                .setText("수수료는 건당 1000원 발생하며,\n거래금액이 클수록 수수료 %가 작아집니다.")
                .setTextColorResource(R.color.white)
                .setTextSize(15f)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowSize(10)
                .setArrowPosition(0.5f)
                .setPadding(12)
                .setCornerRadius(8f)
                .setBackgroundColorResource(R.color.black)
                .setBalloonAnimation(BalloonAnimation.ELASTIC)
                .setLifecycleOwner(viewLifecycleOwner)
                .build()

            balloon.showAlignBottom(binding.chargePriceTooltip)
        }

        binding.completeButton.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder
                .setTitle("구매확정")
                .setMessage(
                    "정말로 구매확정하시겠습니까?\n" +
                            "구매확정이 되면 결제가 완료됩니다."
                )
                .setPositiveButton("구매확정",
                    DialogInterface.OnClickListener { dialog, id ->
                        completePayment()
                        retrofitService.savePost(
                            BlockchainPostItem(
                                userId = otherUserId,
                                userNickname = otherUserName,
                                userPhone = otherUserPhone,
                                postId = postId,
                                postImageUrl = postImages.toString(),
                                postTitle = postTitle,
                                postPrice = postPrice,
                                postContent = postContent,
                                createdAt = createdAt
                            )
                        ).enqueue(object : retrofit2.Callback<String> {
                            override fun onResponse(
                                call: retrofit2.Call<String>,
                                response: retrofit2.Response<String>
                            ) {
                                val state = response.body().toString()
                                Log.e("savePost", state)
                            }

                            override fun onFailure(call: retrofit2.Call<String>, t: Throwable) {
                                Log.e("savePost", t.toString())
                            }
                        })

                        retrofitService.makePayment(
                            PaymentItem(
                                receiverPrivateKey = sellerWalletKey,
                                senderPrivateKey = buyerWalletKey,
                                money = (postPrice.toDouble()).div(2000000)
                            )
                        ).enqueue(object : retrofit2.Callback<String> {
                            override fun onResponse(
                                call: retrofit2.Call<String>,
                                response: retrofit2.Response<String>
                            ) {
                                val state = response.body().toString()
                                Log.e("makePayment", state)
                            }

                            override fun onFailure(call: retrofit2.Call<String>, t: Throwable) {
                                Log.e("makePayment", t.toString())
                            }
                        })
                    })
                .setNegativeButton("취소",
                    DialogInterface.OnClickListener { dialog, id -> })

            builder.create()
            builder.show()

        }
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
                otherUserPhone = otherUserItem?.userPhone.toString()
                otherUserToken = otherUserItem?.userToken.orEmpty()
                otherUserProfileImage = otherUserItem?.userProfileImage.orEmpty()
                getChatData()
            }
    }

    fun deliveryCheck(delivered: Boolean) {
        if (delivered) {
            Toast.makeText(
                context, "택배가 배송이 완료되었습니다.\n" +
                        "3일 이내에 구매확정 버튼을 눌러주세요.", Toast.LENGTH_SHORT
            ).show()

            val chatRoomDB =
                Firebase.database.reference.child("ChatRooms").child(userId!!).child(writerId)

            chatRoomDB.get().addOnSuccessListener {
                if (it.value != null) {
                    val chatRoom = it.getValue(ChatItem::class.java)
                    chatRoomId = chatRoom?.chatRoomId!!
                } else {
                    chatRoomId = UUID.randomUUID().toString()
                }
                // 메세지 , 알림 보내기
                val message = "<택배도착 알림>\n" +
                        "상품명 : [${binding.postTitle.text}]\n\n" +
                        " 택배가 배송이 완료되었습니다.\n" +
                        " 3일 이내에 구매확정 버튼을 눌러주세요."

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
                    userId = otherUserId,
                    userProfile = otherUserProfileImage
                )

                Firebase.database.reference.child("ChatRooms")
                    .child(userId).child(otherUserId!!)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists())
                                ChatDetailFragment.unreadMessage =
                                    snapshot.child("unreadMessage")
                                        .getValue(Int::class.java)!!
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })

                unreadMessage += 1

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
                    "ChatRooms/$userId/$otherUserId/lastMessage" to message,
                    "ChatRooms/$userId/$otherUserId/chatRoomId" to chatRoomId,
                    "ChatRooms/$userId/$otherUserId/otherUserId" to otherUserId,
                    "ChatRooms/$userId/$otherUserId/otherUserName" to otherUserName,
                    "ChatRooms/$userId/$otherUserId/otherUserProfile" to otherUserProfileImage,
                    "ChatRooms/$userId/$otherUserId/unreadMessage" to unreadMessage,
                    "ChatRooms/$userId/$otherUserId/lastMessageTime" to lastMessageTime
                )

                Firebase.database.reference.updateChildren(updates)

                val client = OkHttpClient()
                val root = JSONObject()
                val notification = JSONObject()
                notification.put("title", otherUserName)
                notification.put("body", message)
                notification.put("chatRoomId", chatRoomId)
                notification.put("otherUserId", otherUserId)

                root.put("to", myUserToken)
                root.put("priority", "high")
                root.put("data", notification)

                val requestBody =
                    root.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                val request =
                    Request.Builder().post(requestBody)
                        .url("https://fcm.googleapis.com/fcm/send")
                        .header(
                            "Authorization",
                            "key=${getString(R.string.fcm_server_key)}"
                        )
                        .build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {
                    }

                })
            }

            val updates: MutableMap<String, Any> = hashMapOf(
                "Transactions/$transactionId/deliveryArrived" to true
            )

            Firebase.database.reference.updateChildren(updates)
        } else {
            Toast.makeText(context, "택배가 아직 배송중입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun completePayment() {

        if (binding.waybillNumber.text.isNullOrEmpty()) {
            Toast.makeText(
                context,
                "운송장 정보가 등록된 이후에\n구매확정을 할 수 있습니다.",
                Toast.LENGTH_SHORT
            )
                .show()
            return
        }

        val updates: MutableMap<String, Any> = hashMapOf(
            "Transactions/$transactionId/completePayment" to true
        )
        Firebase.database.reference.updateChildren(updates)


        val chatRoomDB =
            Firebase.database.reference.child("ChatRooms").child(userId!!)
                .child(writerId)

        chatRoomDB.get().addOnSuccessListener {
            if (it.value != null) {
                val chatRoom = it.getValue(ChatItem::class.java)
                chatRoomId = chatRoom?.chatRoomId!!
            } else {
                chatRoomId = UUID.randomUUID().toString()
            }
            // 메세지 , 알림 보내기

            val message = "<구매확정 알림>\n" +
                    "상품명 : [${binding.postTitle.text}]\n" +
                    "상품가격 : ${binding.postPrice.text}원\n\n" +
                    " 판매하신 상품의 거래가 완료되었습니다.\n" +
                    " 판매금액은 24시간 이내에 정산됩니다."

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

            Firebase.database.reference.child("ChatRooms").child(otherUserId)
                .child(userId!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists())
                            unreadMessage = snapshot.child("unreadMessage")
                                .getValue(Int::class.java)!!
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })

            unreadMessage += 1

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
                "ChatRooms/$otherUserId/$userId/unreadMessage" to unreadMessage,
                "ChatRooms/$otherUserId/$userId/lastMessageTime" to lastMessageTime
            )

            Firebase.database.reference.updateChildren(updates).addOnSuccessListener {
                val action =
                    SafePaymentFragmentDirections.actionSafePaymentFragmentToChatDetailFragment(
                        chatRoomId = chatRoomId,
                        otherUserId = writerId
                    )
                findNavController().navigate(action)
            }

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
                root.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
            val request =
                Request.Builder().post(requestBody)
                    .url("https://fcm.googleapis.com/fcm/send")
                    .header(
                        "Authorization",
                        "key=${mContext?.getString(R.string.fcm_server_key)}"
                    )
                    .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                override fun onResponse(call: Call, response: Response) {
                }

            })
        }
    }

    fun sendChat(message: String) {
        val chatRoomDB =
            Firebase.database.reference.child("ChatRooms").child(userId!!).child(writerId)

        chatRoomDB.get().addOnSuccessListener {

            if (it.value != null) {
                val chatRoom = it.getValue(ChatItem::class.java)
                chatRoomId = chatRoom?.chatRoomId!!
            } else {
                chatRoomId = UUID.randomUUID().toString()
            }
            // 메세지 , 알림 보내기

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

            unreadMessage += 1

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
                "ChatRooms/$otherUserId/$userId/unreadMessage" to unreadMessage,
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
        }
    }
}



