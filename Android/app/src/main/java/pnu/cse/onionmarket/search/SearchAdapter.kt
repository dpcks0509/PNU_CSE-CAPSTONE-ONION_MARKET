package pnu.cse.onionmarket.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pnu.cse.onionmarket.databinding.ItemSearchBinding

class SearchAdapter(private val noSearch: FrameLayout?, private val onClick: (SearchItem) -> Unit) :
    ListAdapter<SearchItem, SearchAdapter.ViewHolder>(differ) {

    init {
        if (searchList.isEmpty()) {
            noSearch?.visibility = View.VISIBLE
        } else {
            noSearch?.visibility = View.GONE
        }
    }

    inner class ViewHolder(private val binding: ItemSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SearchItem) {
            binding.searchedText.text = item.searchedText

            binding.searchedText.setOnClickListener {
                onClick(item)
                addItem(item)
            }

            binding.removeButton.setOnClickListener {
                removeItem(item)

                if (searchList.isEmpty()) {
                    noSearch?.visibility = View.VISIBLE
                } else {
                    noSearch?.visibility = View.GONE
                }
            }

        }
    }

    companion object {
        private val searchList: MutableList<SearchItem> = mutableListOf()

        val differ = object : DiffUtil.ItemCallback<SearchItem>() {
            override fun areItemsTheSame(oldItem: SearchItem, newItem: SearchItem): Boolean {
                return oldItem.searchId == newItem.searchId
            }

            override fun areContentsTheSame(oldItem: SearchItem, newItem: SearchItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSearchBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return searchList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val reversedPosition = itemCount - 1 - position
        holder.bind(searchList[reversedPosition])
    }

    private fun removeItem(item: SearchItem) {
        searchList.remove(item)
        notifyDataSetChanged()
    }

    fun addItem(item: SearchItem) {
        val updatedList = searchList.filter { it.searchedText != item.searchedText }.toMutableList()
        updatedList.add(item)
        searchList.clear()
        searchList.addAll(updatedList)
        notifyDataSetChanged()
    }
}
