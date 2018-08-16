package com.printer.utils

import android.content.Context

/**
 *
 * @author wsl
 * @date 2018/8/9 0009
 */

class DensityUtils {

    companion object {

        fun dip2px(context: Context, dpValue: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (dpValue * scale + 0.5f).toInt()
        }

        fun px2dip(context: Context, pxValue: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (pxValue / scale + 0.5f).toInt()
        }
    }

}
