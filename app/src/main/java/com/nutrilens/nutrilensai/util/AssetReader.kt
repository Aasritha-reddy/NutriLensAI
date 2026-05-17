package com.nutrilens.nutrilensai.util

import android.content.Context
import com.nutrilens.nutrilensai.Constants

object AssetReader {
    fun readHealthReport(context: Context): String =
        context.assets.open(Constants.HEALTH_REPORT_ASSET_NAME).bufferedReader().use { it.readText() }
}
