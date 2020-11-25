/*
 * ResourceCategoryListFragment.kt
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
import space.celestia.mobilecelestia.common.NavigationFragment
import space.celestia.mobilecelestia.core.CelestiaAppCore
import space.celestia.mobilecelestia.resource.model.ResourceAPI
import space.celestia.mobilecelestia.resource.model.ResourceAPIService
import space.celestia.mobilecelestia.resource.model.ResourceCategory
import space.celestia.mobilecelestia.utils.CelestiaString
import space.celestia.mobilecelestia.utils.commonHandler

class ResourceCategoryListFragment : AsyncListFragment<ResourceCategory>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = CelestiaString("Categories", "")
        menuItems = listOf(NavigationFragment.MenuItem(MENU_ITEM_MANAGE_INSTALLED, CelestiaString("Installed", "")))
    }

    override fun refresh(success: (List<ResourceCategory>) -> Unit, failure: (String) -> Unit) {
        val lang = CelestiaAppCore.getLocalizedString("LANGUAGE", "celestia")
        val service = ResourceAPI.shared.create(ResourceAPIService::class.java)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = service.categories(lang).commonHandler<List<ResourceCategory>>(object: TypeToken<ArrayList<ResourceCategory>>() {}.type, ResourceAPI.gson)
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
        @JvmStatic
        fun newInstance() = ResourceCategoryListFragment()

        const val MENU_ITEM_MANAGE_INSTALLED = 0
    }
}
