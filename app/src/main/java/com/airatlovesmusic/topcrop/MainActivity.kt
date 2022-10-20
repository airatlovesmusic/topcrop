package com.airatlovesmusic.topcrop

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.airatlovesmusic.topcrop.input.ASPECT_RATIO_ORIGINAL
import com.airatlovesmusic.topcrop.input.AspectRatio
import com.airatlovesmusic.topcrop.input.CropOptions

class MainActivity: AppCompatActivity() {

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            cropLauncher.launch(uri to CropOptions.Builder()
                .addAspectRatio(AspectRatio(3f, 4f))
                .addAspectRatio(AspectRatio(ASPECT_RATIO_ORIGINAL, ASPECT_RATIO_ORIGINAL, "Original"))
                .build())
        }
    }

    private val cropLauncher = registerForActivityResult(TopCropContract()) {
        if (it != null) Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_start).setOnClickListener { galleryLauncher.launch("image/*") }
    }

}