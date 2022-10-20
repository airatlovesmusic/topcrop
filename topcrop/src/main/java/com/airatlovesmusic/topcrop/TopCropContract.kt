package com.airatlovesmusic.topcrop

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.airatlovesmusic.topcrop.input.CropOptions

class TopCropContract: ActivityResultContract<Pair<Uri, CropOptions>, Uri?>() {

    override fun createIntent(
        context: Context,
        input: Pair<Uri, CropOptions>
    ) = TopCropActivity.createIntent(context, input.first, input.second)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data

}