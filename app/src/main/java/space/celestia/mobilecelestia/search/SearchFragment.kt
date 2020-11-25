/*
 * SearchFragment.kt
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia.search

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import space.celestia.mobilecelestia.R
import space.celestia.mobilecelestia.core.CelestiaAppCore

@ExperimentalCoroutinesApi
fun SearchView.textChanges(): Flow<Pair<String, Boolean>> {
    return callbackFlow<Pair<String, Boolean>> {
        val listener = object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                offer(Pair(newText ?: "", false))
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                offer(Pair(query ?: "", true))
                return true
            }
        }
        setOnQueryTextListener(listener)
        awaitClose { setOnQueryTextListener(null) }
    }
}

class SearchFragment : Fragment() {

    private var listener: Listener? = null
    private val listAdapter by lazy { SearchRecyclerViewAdapter(listener) }

    private var searchView: SearchView? = null

    @FlowPreview
    @ExperimentalCoroutinesApi
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_item_list, container, false)

        view.findViewById<View>(R.id.search_container).setOnTouchListener { _, _ -> true }
        val searchView = view.findViewById<SearchView>(R.id.search)
        this.searchView = searchView
        setupSearchSearchView()
        searchView.isIconified = false

        // Set the adapter
        with(view.findViewById<RecyclerView>(R.id.list)) {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }
        return view
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    private fun setupSearchSearchView() {
        val searchView = this.searchView ?: return
        searchView.textChanges()
            .distinctUntilChanged()
            .debounce(300)
            .mapLatest {
                val key = it.first
                val core = CelestiaAppCore.shared()
                val result = if (key.isEmpty()) listOf<String>() else core.simulation.completionForText(key, SEARCH_RESULT_LIMIT)
                Triple(key, result, it.second)
            }
            .onEach {
                withContext(Dispatchers.Main) {
                    val lastSearchText = it.first
                    val lastSearchResultCount = it.second.size
                    listAdapter.updateSearchResults(it.second)
                    listAdapter.notifyDataSetChanged()

                    if (!it.third) return@withContext

                    // Submit, clear focus
                    searchView.clearFocus()
                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(searchView.windowToken, 0)
                    setupSearchSearchView()

                    if (lastSearchText.isNotEmpty() && lastSearchResultCount == 0) {
                        listener?.onSearchItemSubmit(lastSearchText)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(lifecycleScope)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement SearchFragment.Listener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface Listener {
        fun onSearchItemSelected(text: String)
        fun onSearchItemSubmit(text: String)
    }

    companion object {
        const val SEARCH_RESULT_LIMIT = 20

        @JvmStatic
        fun newInstance() = SearchFragment()
    }
}
