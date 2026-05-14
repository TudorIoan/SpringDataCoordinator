package net.abaresults.progresspath.view.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.abaresults.progresspath.api.model.UnsplashPhoto

class CarouselPhotosAdapter(fragment: Fragment) :
    FragmentStateAdapter(fragment) {

    private var contentList: MutableList<UnsplashPhoto> = mutableListOf()

    fun add(content: List<UnsplashPhoto>) {
        contentList.addAll(content)
        notifyDataSetChanged()
    }

    fun set(content: List<UnsplashPhoto>) {
        contentList.clear()
        contentList.addAll(content)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return contentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return CarouselPhotoItemFragment(contentList[position])
    }
}