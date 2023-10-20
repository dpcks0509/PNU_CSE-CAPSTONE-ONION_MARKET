package pnu.cse.onionmarket.search

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import pnu.cse.onionmarket.R
import pnu.cse.onionmarket.databinding.FragmentSearchBinding
import java.util.UUID

class SearchFragment : Fragment(R.layout.fragment_search) {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var searchAdapter: SearchAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSearchBinding.bind(view)

        searchAdapter = SearchAdapter(binding.noSearch) {
            val searchQuery = it.searchedText.toString().trim()
            val action = SearchFragmentDirections.actionSearchFragmentToHomeFragment(
                searchQuery = searchQuery, firstSearch = true
            )
            findNavController().navigate(action)
        }

        binding.searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = searchAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        }

        binding.searchview.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val imm =
                    context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
                binding.searchview.clearFocus()

                val searchId = UUID.randomUUID().toString()
                searchAdapter.addItem(SearchItem(searchId, query))

                val searchQuery = binding.searchview.query.toString().trim()
                val action = SearchFragmentDirections.actionSearchFragmentToHomeFragment(
                    searchQuery = searchQuery, firstSearch = true
                )

                findNavController().navigate(action)

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }
}