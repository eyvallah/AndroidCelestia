/*
 * InstalledResourceListFragment.kt
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.celestia.mobilecelestia.resource.model.ResourceItem
import space.celestia.mobilecelestia.resource.model.ResourceManager
import space.celestia.mobilecelestia.utils.CelestiaString

class InstalledResourceListFragment : AsyncListFragment<ResourceItem>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = CelestiaString("Installed", "")
    }

    override fun refresh(success: (List<ResourceItem>) -> Unit, failure: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                val installedResources = withContext(Dispatchers.IO) {
                    ResourceManager.shared.installedResources()
                }
                success(installedResources)
            } catch (ignored: Throwable) {}
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = InstalledResourceListFragment()
    }
}
