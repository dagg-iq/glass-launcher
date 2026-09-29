package com.glass.launcher

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val iconDrawable: Drawable?,
    val isPinnedToDock: Boolean = false,
    var glassIcon: Bitmap? = null
)
