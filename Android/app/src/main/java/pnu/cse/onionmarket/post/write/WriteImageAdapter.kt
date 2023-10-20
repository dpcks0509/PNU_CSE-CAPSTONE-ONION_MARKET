package pnu.cse.onionmarket.post.write

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pnu.cse.onionmarket.databinding.ItemWriteImageBinding

class WriteImageAdapter(private val imageCountTextView: TextView) :
    ListAdapter<WriteImageItem, WriteImageAdapter.ViewHolder>(differ) {

    var imageList = mutableListOf<WriteImageItem>()

    inner class ViewHolder(private val binding: ItemWriteImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WriteImageItem) {
            Glide.with(binding.imageItem)
                .load(item.imageUrl)
                .into(binding.imageItem)

            binding.removeButton.setOnClickListener {
                imageList.remove(item)
                imageCount = imageList.size
                imageCountTextView.text = imageCount.toString()
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        var imageCount = 0

        val differ = object : DiffUtil.ItemCallback<WriteImageItem>() {
            override fun areItemsTheSame(
                oldItem: WriteImageItem,
                newItem: WriteImageItem
            ): Boolean {
                return oldItem.imageId == newItem.imageId
            }

            override fun areContentsTheSame(
                oldItem: WriteImageItem,
                newItem: WriteImageItem
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemWriteImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    fun setPostImageItemList(writeImageItems: List<WriteImageItem>) {
        imageList.addAll(writeImageItems)
        imageCount = imageList.size
        notifyDataSetChanged()
    }
}