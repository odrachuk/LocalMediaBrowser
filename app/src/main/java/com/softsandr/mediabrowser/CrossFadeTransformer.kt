package com.softsandr.mediabrowser

import android.support.v4.view.ViewPager
import android.view.View

class CrossFadeTransformer : ViewPager.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        if (position <= -1.0f || position >= 1.0f) {
            page.alpha = 0.0f
            page.visibility = View.GONE
        } else if( position == 0.0f ) {
            page.alpha = 1.0f
            page.visibility = View.VISIBLE
        } else {
            page.alpha = 1.0f - Math.abs(position)
            page.translationX = -position * page.width
            page.visibility = View.VISIBLE
        }
    }
}