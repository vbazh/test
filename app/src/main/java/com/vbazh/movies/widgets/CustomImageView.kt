package com.vbazh.movies.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import com.vbazh.movies.R

class CustomImageView : ImageView {

    lateinit var clipPath: Path
    lateinit var rect: RectF
    private var radius: Float? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        init(context)

        val a = context.obtainStyledAttributes(attrs, R.styleable.CustomImageView);
        try {
            radius = a.getFloat(R.styleable.CustomImageView_radius, 24f)
        } finally {
            a.recycle();
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {

        init(context)

        val a = context.obtainStyledAttributes(attrs, R.styleable.CustomImageView);
        try {
            radius = a.getFloat(R.styleable.CustomImageView_radius, 24f)
        } finally {
            a.recycle();
        }
    }

    private fun init(context: Context) {
        clipPath = Path()
        rect = RectF()
    }

    override fun onDraw(canvas: Canvas) {

        rect.set(0f, 0f, this.width.toFloat(), this.height.toFloat())
        clipPath.addRoundRect(rect, radius!!, radius!!, Path.Direction.CW)
        canvas.clipPath(clipPath)

        super.onDraw(canvas)
    }

    companion object {

        var radius = 18.0f
    }
}