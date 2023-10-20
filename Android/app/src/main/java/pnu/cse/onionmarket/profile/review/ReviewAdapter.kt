package pnu.cse.onionmarket.profile.review

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.ItemReviewBinding

class ReviewAdapter()
    : ListAdapter<ReviewItem, ReviewAdapter.ViewHolder>(differ) {

    inner class ViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReviewItem) {
            if(item.userProfile.isNullOrEmpty())
                Glide.with(binding.profileImage)
                    .load(R.drawable.app_logo)
                    .into(binding.profileImage)
            else
                Glide.with(binding.profileImage)
                    .load(item.userProfile)
                    .into(binding.profileImage)

            binding.nickname.text = item.userName
            binding.reviewText.text = item.reviewText
            binding.starNumber.text = item.reviewStar.toString()
        }
    }

    companion object {
        val differ = object : DiffUtil.ItemCallback<ReviewItem>() {
            override fun areItemsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean {
                return oldItem.reviewId == newItem.reviewId
            }

            override fun areContentsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemReviewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }
}