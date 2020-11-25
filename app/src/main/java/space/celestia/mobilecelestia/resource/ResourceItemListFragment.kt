/*
 * ResourceItemListFragment.kt
 *
 * Copyright (C) 2001-2020, Celestia Development Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package space.celestia.mobilecelestia.resource

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.celestia.mobilecelestia.core.CelestiaAppCore
import space.celestia.mobilecelestia.resource.model.ResourceAPI
import space.celestia.mobilecelestia.resource.model.ResourceAPIService
import space.celestia.mobilecelestia.resource.model.ResourceCategory
import space.celestia.mobilecelestia.resource.model.ResourceItem
import space.celestia.mobilecelestia.utils.CelestiaString
import space.celestia.mobilecelestia.utils.commonHandler

class ResourceItemListFragment : AsyncListFragment<ResourceItem>() {
    private var category: ResourceCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            val savedCategory = savedInstanceState.getSerializable(ARG_CATEGORY)
            if (savedCategory != null)
                category = savedCategory as ResourceCategory
        } else {
            val savedCategory = arguments?.getSerializable(ARG_CATEGORY)
            category = savedCategory as ResourceCategory
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = category?.name ?: ""
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(ARG_CATEGORY, category)
    }

    override fun refresh(success: (List<ResourceItem>) -> Unit, failure: (String) -> Unit) {
        val categoryID = category?.id ?: return
        val lang = CelestiaAppCore.getLocalizedString("LANGUAGE", "celestia")
        val service = ResourceAPI.shared.create(ResourceAPIService::class.java)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = service.items(lang, categoryID).commonHandler<List<ResourceItem>>(object: TypeToken<ArrayList<ResourceItem>>() {}.type, ResourceAPI.gson)
                withContext(Dispatchers.Main) {
                    success(result)
                }
            } catch (ignored: Throwable) {
                withContext(Dispatchers.Main) {
                    failure(CelestiaString("Failed to load add-ons.", ""))
                }
            }
        }
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        @JvmStatic
        fun newInstance(category: ResourceCategory)  =
            ResourceItemListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CATEGORY, category)
                }
            }
    }
}
