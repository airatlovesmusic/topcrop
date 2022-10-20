package com.airatlovesmusic.topcrop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airatlovesmusic.topcrop.databinding.ActivityCropBinding
import com.airatlovesmusic.topcrop.input.CropOptions

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
                ivImage.setImageBitmap(BitmapFactory.decodeStream(it))
            }
        }
        setContentView(binding?.root)
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