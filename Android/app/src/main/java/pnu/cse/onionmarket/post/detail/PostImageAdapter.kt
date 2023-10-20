package pnu.cse.onionmarket.post.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.asksira.loopingviewpager.LoopingPagerAdapter
import com.bumptech.glide.Glide
import pnu.cse.onionmarket.R

class PostImageAdapter(
    itemList: List<String>,
    isInfinite: Boolean
) : LoopingPagerAdapter<String>(itemList, isInfinite) {
    override fun inflateView(viewType: Int, container: ViewGroup, listPosition: Int): View {
        return LayoutInflater.from(container.context)
            .inflate(R.layout.item_post_image, container, false)
    }

    override fun bindView(convertView: View, listPosition: Int, viewType: Int) {
        val image = convertView.findViewById<ImageView>(R.id.detail_image)
        val url = itemList?.get(listPosition)
        Glide.with(convertView.context)
            .load(url)
            .into(image)
    }
}