package com.airatlovesmusic.topcrop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airatlovesmusic.topcrop.databinding.ActivityCropBinding
import com.airatlovesmusic.topcrop.input.CropOptions
import com.airatlovesmusic.topcrop.view.GridView
import com.google.android.material.tabs.TabLayout

class TopCropActivity: AppCompatActivity() {

    private var binding: ActivityCropBinding? = null
    private val options: CropOptions by lazy { intent.getParcelableExtra<CropOptions>(ARG_OPTIONS) as CropOptions }
    private val inputUri: Uri by lazy { intent.getParcelableExtra<Uri>(ARG_URI) as Uri }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater).apply {
            ibBack.setOnClickListener { finish() }
            ibDone.setOnClickListener {
                setResult(Activity.RESULT_OK, Intent().apply { data = inputUri })
                finish()
            }
            contentResolver.openInputStream(inputUri)?.use {
                cropView.setImageBitmap(BitmapFactory.decodeStream(it))
            }
            setUpAspectRatios()
            gridView.listener = object: GridView.Listener {
                override fun onCropRectUpdated(cropRect: RectF) { cropView.setCropRect(cropRect) }
            }
        }
        setContentView(binding?.root)
    }

    private fun ActivityCropBinding.setUpAspectRatios() {
        options.aspectRatios.forEach {
            tlRatios.addTab(
                tlRatios.newTab()
                    .setText(it.title)
            )
        }
        tlRatios.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val currentAspectRatio = options.aspectRatios[tab.position].let { it.y / it.x }
                gridView.setTargetAspectRatio(currentAspectRatio)
                cropView.setTargetAspectRatio(currentAspectRatio)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        gridView.setTargetAspectRatio(options.aspectRatios.first().getValue())
        cropView.setTargetAspectRatio(options.aspectRatios.first().getValue())
    }

    companion object {
        private const val ARG_OPTIONS = BuildConfig.LIBRARY_PACKAGE_NAME + ".options"
        private const val ARG_URI = BuildConfig.LIBRARY_PACKAGE_NAME + ".uri"

        fun createIntent(
            context: Context,
            uri: Uri,
            options: CropOptions,
        ) = Intent(context, TopCropActivity::class.java).apply {
            putExtra(ARG_URI, uri)
            putExtra(ARG_OPTIONS, options)
        }
    }
}