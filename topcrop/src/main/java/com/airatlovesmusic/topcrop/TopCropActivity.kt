package com.airatlovesmusic.topcrop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airatlovesmusic.topcrop.databinding.ActivityCropBinding
import com.airatlovesmusic.topcrop.input.CropOptions
import com.airatlovesmusic.topcrop.view.GridView
import com.airatlovesmusic.topcrop.view.TopCropImageView
import com.airatlovesmusic.topcrop.view.WheelView
import com.google.android.material.tabs.TabLayout
import java.util.*

class TopCropActivity: AppCompatActivity() {

    private var binding: ActivityCropBinding? = null
    private val options: CropOptions by lazy { intent.getParcelableExtra<CropOptions>(ARG_OPTIONS) as CropOptions }
    private val inputUri: Uri by lazy { intent.getParcelableExtra<Uri>(ARG_URI) as Uri }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater).apply {
            setUpCropView()
            setUpAspectRatios()
            setUpRotateWidget()
            setUpScaleWidget()
            setUpBottomNavigationWidget()
            ibClose.setOnClickListener { finish() }
            ibDone.setOnClickListener {
                setResult(Activity.RESULT_OK, Intent().apply { data = inputUri })
                finish()
            }
        }
        setContentView(binding?.root)
    }

    private fun ActivityCropBinding.setUpBottomNavigationWidget() {
        bnvItems.setOnItemSelectedListener {
            flRotate.isVisible = false
            flScale.isVisible = false
            tlAspectRatios.isVisible = false
            when (it.itemId) {
                R.id.item_crop -> tlAspectRatios.isVisible = true
                R.id.item_rotate -> flRotate.isVisible = true
                else -> flScale.isVisible = true
            }
            true
        }
    }

    private fun ActivityCropBinding.setUpScaleWidget() {
        ibPlus.setOnClickListener {
            cropView.zoomImage(1f)
            cropView.setImageFitAspectRatio()
        }
        ibMinus.setOnClickListener {
            cropView.zoomImage(-1f)
            cropView.setImageFitAspectRatio()
        }
        wheelScale.listener = object : WheelView.Listener {
            override fun onScroll(delta: Float, totalDistance: Float) { cropView.zoomImage(delta / 100f) }
            override fun onScrollEnd() { cropView.setImageFitAspectRatio() }
        }
    }

    private fun ActivityCropBinding.setUpRotateWidget() {
        ibRotateLeft.setOnClickListener {
            cropView.postRotate(-90f)
            cropView.setImageFitAspectRatio()
        }
        ibRotateRight.setOnClickListener {
            cropView.postRotate(90f)
            cropView.setImageFitAspectRatio()
        }
        wheelRotation.listener = object : WheelView.Listener {
            override fun onScroll(delta: Float, totalDistance: Float) { cropView.postRotate(delta / ROTATE_WIDGET_SENSITIVITY) }
            override fun onScrollEnd() { cropView.setImageFitAspectRatio() }
        }
    }

    private fun ActivityCropBinding.setUpCropView() {
        contentResolver.openInputStream(inputUri)?.use { cropView.setImageBitmap(BitmapFactory.decodeStream(it)) }
        cropView.listener = object : TopCropImageView.Listener {
            override fun onRotate(angle: Float) { tvRotationValue.text = String.format(Locale.getDefault(), "%.1f°", angle) }
            override fun onScale(scale: Float) { tvScaleValue.text = String.format(Locale.getDefault(), "%d%%", (scale * 100).toInt()) }
        }
        gridView.listener = object: GridView.Listener {
            override fun onCropRectUpdated(cropRect: RectF) { cropView.setCropRect(cropRect) }
        }
    }

    private fun ActivityCropBinding.setUpAspectRatios() {
        options.aspectRatios.forEach {
            tlAspectRatios.addTab(
                tlAspectRatios.newTab()
                    .setCustomView(R.layout.tab_layout)
                    .apply { customView?.findViewById<TextView>(R.id.tv_title)?.text = it.title }
            )
        }
        tlAspectRatios.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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

        private const val ROTATE_WIDGET_SENSITIVITY = 42

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
