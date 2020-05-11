package com.politechnika.advancedmobileapps.logger

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.Fragment

class LogFragment : Fragment() {

    private lateinit var mLogView : LogView
    private lateinit var mScrollView : ScrollView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val result = inflateViews()
        mLogView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        return result
    }

    fun getLogView() : LogView {
        return mLogView
    }

    private fun inflateViews() : View {
        mScrollView = ScrollView(activity)
        val scrollParams : ViewGroup.LayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mScrollView.layoutParams = scrollParams
        mLogView = LogView(activity)
        val logParams : ViewGroup.LayoutParams = ViewGroup.LayoutParams(scrollParams)
        logParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mLogView.layoutParams = logParams
        mLogView.isClickable = true
        mLogView.isFocusable = true
        mLogView.typeface = Typeface.MONOSPACE
        val paddingDips = 16
        val scale = resources.displayMetrics.density
        val paddingPixels = (paddingDips * scale + .5).toInt()
        mLogView.setPadding(paddingPixels, paddingPixels, paddingPixels, paddingPixels)
        mLogView.compoundDrawablePadding = paddingPixels
        mLogView.gravity = Gravity.BOTTOM
//        mLogView.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
        mScrollView.addView(mLogView)
        return mScrollView
    }

}