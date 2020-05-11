package com.politechnika.advancedmobileapps.logger

import android.app.Activity
import android.content.Context
import androidx.appcompat.widget.AppCompatTextView

class LogView(context: Context?) : AppCompatTextView(context) {

    fun clearLogs() {
        (context as Activity).runOnUiThread(Thread(Runnable { text = "" }))
    }

    fun println(text: String) {
        (context as Activity).runOnUiThread(Thread(Runnable { appendToLog(text) }))
    }

    private fun appendToLog(text : String) {
        append("\n" + text)
    }

}