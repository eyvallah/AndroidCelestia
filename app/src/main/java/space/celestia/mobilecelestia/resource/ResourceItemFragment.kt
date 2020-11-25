/*
 * ResourceItemFragment.kt
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.celestia.mobilecelestia.R
import space.celestia.mobilecelestia.common.NavigationFragment
import space.celestia.mobilecelestia.common.ProgressButton
import space.celestia.mobilecelestia.core.CelestiaAppCore
import space.celestia.mobilecelestia.resource.model.ResourceAPI
import space.celestia.mobilecelestia.resource.model.ResourceAPIService
import space.celestia.mobilecelestia.resource.model.ResourceItem
import space.celestia.mobilecelestia.resource.model.ResourceManager
import space.celestia.mobilecelestia.utils.CelestiaString
import space.celestia.mobilecelestia.utils.commonHandler
import space.celestia.mobilecelestia.utils.showAlert
import java.io.File
import java.text.DateFormat
import java.util.*

class ResourceItemFragment : NavigationFragment.SubFragment(), ResourceManager.Listener {
    private var item: ResourceItem? = null
    private var progressButton: ProgressButton? = null
    private var currentState: ResourceItemState = ResourceItemState.None

    private var imageView: ImageView? = null
    private var titleLabel: TextView? = null
    private var descriptionLabel: TextView? = null
    private var authorsLabel: TextView? = null
    private var releaseDateLabel: TextView? = null

    private val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())

    class GlideUrlCustomCacheKey(url: String, val key: String) : GlideUrl(url) {
        override fun getCacheKey(): String {
            return key
        }
    }

    enum class ResourceItemState {
        None, Downloading, Installed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ResourceManager.shared.addListener(this)

        arguments?.let {
            item = it.getSerializable(ARG_ITEM) as? ResourceItem
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_resource_item, container, false)
        val title = view.findViewById<TextView>(R.id.title)
        val content = view.findViewById<TextView>(R.id.content)
        val footnote = view.findViewById<TextView>(R.id.footnote)
        val image = view.findViewById<ImageView>(R.id.image)
        val authors = view.findViewById<TextView>(R.id.authors)
        val releaseDate = view.findViewById<TextView>(R.id.publish_time)
        footnote.text = CelestiaString("Note: restarting Celestia is needed to use any new installed add-on.", "")

        val progressButton = view.findViewById<ProgressButton>(R.id.progress_button)
        progressButton.setOnClickListener {
            onProgressButtonClick()
        }
        this.progressButton = progressButton
        this.titleLabel = title
        this.descriptionLabel = content
        this.imageView = image
        this.authorsLabel = authors
        this.releaseDateLabel = releaseDate
        updateContents()
        updateUI()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val item = this.item ?: return

        // Fetch the latest data from server since user might have come from `Installed`
        val lang = CelestiaAppCore.getLocalizedString("LANGUAGE", "celestia")
        val service = ResourceAPI.shared.create(ResourceAPIService::class.java)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = service.item(lang, item.id).commonHandler(ResourceItem::class.java, ResourceAPI.gson)
                withContext(Dispatchers.Main) {
                    this@ResourceItemFragment.item = result
                    updateContents()
                }
            } catch (ignored: Throwable) {}
        }
    }

    override fun onDestroy() {
        ResourceManager.shared.removeListener(this)

        super.onDestroy()
    }

    private fun onProgressButtonClick() {
        val item = this.item ?: return
        val activity = this.activity ?: return

        val dm = ResourceManager.shared

        // Already installed, offer an option for uninstalling
        if (dm.isInstalled(item.id)) {
            activity.showAlert(CelestiaString("Do you want to uninstall this add-on?", "")) {
                var success = false
                try {
                    success = dm.uninstall(item.id)
                } catch (e: Exception) {}
                if (success) {
                    currentState = ResourceItemState.None
                } else {
                    activity.showAlert(CelestiaString("Unable to uninstall add-on.", ""))
                }
                updateUI()
            }
            return
        }

        // Already downloading, allow user to cancel
        if (dm.isDownloading(item.id)) {
            activity.showAlert(CelestiaString("Do you want to cancel this task?", "")) {
                dm.cancel(item.id)
                currentState = ResourceItemState.None
                updateUI()
            }
            return
        }

        // Start downloading
        dm.download(item, File(activity.cacheDir, item.id))
        currentState = ResourceItemState.Downloading
        updateUI()
    }

    override fun onResourceFetchError(identifier: String) {
        if (identifier != item?.id) return
        currentState = ResourceItemState.None
        updateUI()
        activity?.showAlert(CelestiaString("Failed to download or install this add-on.", ""))
    }

    override fun onFileDownloaded(identifier: String) {
        if (identifier != item?.id) return
        updateUI()
    }

    override fun onFileUnzipped(identifier: String) {
        if (identifier != item?.id) return
        currentState = ResourceItemState.Installed
        updateUI()
    }

    override fun onProgressUpdate(identifier: String, progress: Float) {
        val id = item?.id ?: return
        if (identifier != id) { return }

        currentState = ResourceItemState.Downloading
        progressButton?.setProgress(progress * 100f)
        updateUI()
    }

    private fun updateContents() {
        val item = this.item ?: return
        titleLabel?.text = item.name
        descriptionLabel?.text = item.description

        val imageURL = item.image
        val itemID = item.id
        val imageView = this.imageView
        if (imageURL != null && imageView != null) {
            imageView.visibility = View.VISIBLE
            Glide.with(this).load(GlideUrlCustomCacheKey(imageURL, itemID)).into(imageView)
        } else {
            imageView?.visibility = View.GONE
        }

        val authors = item.authors
        if (authors != null && authors.isNotEmpty()) {
            authorsLabel?.visibility = View.VISIBLE
            authorsLabel?.text = CelestiaString("Authors: %s", "").format(authors.joinToString(", "))
        } else {
            authorsLabel?.visibility = View.GONE
        }

        val releaseDate = item.publishTime
        if (releaseDate != null) {
            releaseDateLabel?.visibility = View.VISIBLE
            releaseDateLabel?.text = CelestiaString("Release date: %s", "").format(formatter.format(releaseDate))
        } else {
            releaseDateLabel?.visibility = View.GONE
        }
    }

    private fun updateUI() {
        val button = progressButton ?: return
        val id = item?.id ?: return

        // Ensure we are up to date with these cases
        val dm = ResourceManager.shared
        if (dm.isInstalled(id))
            currentState = ResourceItemState.Installed
        if (dm.isDownloading(id))
            currentState = ResourceItemState.Downloading

        when (currentState) {
            ResourceItemState.None -> {
                button.reset()
                button.setText(CelestiaString("DOWNLOAD", ""))
            }
            ResourceItemState.Downloading -> {
                button.setText(CelestiaString("DOWNLOADING", ""))
            }
            ResourceItemState.Installed -> {
                button.setProgress(100f)
                button.setText(CelestiaString("INSTALLED", ""))
            }
        }
    }

    companion object {
        private const val ARG_ITEM = "item"
        @JvmStatic
        fun newInstance(item: ResourceItem) =
            ResourceItemFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_ITEM, item)
                }
            }
    }
}