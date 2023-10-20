package pnu.cse.onionmarket.service

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RetrofitService {
    @GET("/open-api/rest/tracking/state")
    fun deliveryCheck(
        @Query("t_code") waybillCompanyCode: String,
        @Query("t_invoice") waybillNumber: String
    ): Call<String>

    @GET("/api/ether/balance")
    fun getWalletMoney(
        @Query("privateKey") walletPrivateKey: String,
    ): Call<String>

    @POST("/api/ether/send")
    fun makePayment(
        @Body payment: PaymentItem
    ): Call<String>

    @POST("/api/item/post")
    fun savePost(
        @Body post: BlockchainPostItem
    ): Call<String>

    @POST("/api/review/post")
    fun saveReview(
        @Body review: BlockchainReviewItem
    ): Call<String>

    @GET("/api/item/get")
    fun getPosts(
        @Query("userId") userId: String
    ): Call<List<BlockchainPostItem>>

    @GET("/api/review/get")
    fun getReviews(
        @Query("userId") userId: String
    ): Call<List<BlockchainReviewItem>>
}