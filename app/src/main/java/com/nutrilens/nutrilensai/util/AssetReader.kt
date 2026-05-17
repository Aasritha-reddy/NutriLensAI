package com.nutrilens.nutrilensai.util

import android.content.Context

object AssetReader {
    fun readHealthReport(context: Context): String =
        context.assets.open("sample_health_report.txt").bufferedReader().use { it.readText() }
}
