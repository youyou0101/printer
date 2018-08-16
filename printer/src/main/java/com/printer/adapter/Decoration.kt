package com.printer.adapter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v7.widget.RecyclerView
import com.printer.utils.DensityUtils

/**
 *
 * @author wsl
 * @date 2018/8/9 0009
 */
class Decoration : RecyclerView.ItemDecoration() {

    private val paint: Paint = Paint()

    init {
        paint.color = Color.parseColor("#C7C7CC")
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        super.onDrawOver(c, parent, state)

        val childCount = parent.childCount
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        for (i in 0 until childCount) {
            val view = parent.getChildAt(i)
            val top = view.bottom.toFloat()
            val bottom = (view.bottom + 1).toFloat()
            if (i == childCount - 1) {
                c.drawRect(left.toFloat(), top, right.toFloat(), bottom, this.paint)
            } else {
                val space = DensityUtils.dip2px(parent.context, 16f)
                c.drawRect((left + space).toFloat(), top, (right - space).toFloat(), bottom, this.paint)
            }
        }


    }
}
