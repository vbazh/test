package com.vbazh.movies.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.util.Log
import android.view.View
import com.vbazh.movies.R
import java.lang.ref.WeakReference

class ImageUtil {

    companion object {

        private var bluredBitmap: Bitmap? = null
        private var rs: RenderScript? = null

        fun blurView(backgroundView: View, targetView: View, context: WeakReference<Context>) {
            rs = RenderScript.create(context.get())
            val backgroundBounds = Rect()
            backgroundView.getHitRect(backgroundBounds)
            if (!targetView.getLocalVisibleRect(backgroundBounds)) {
                return
            }

            val blurredBitmap = captureView(backgroundView)

            val loc = IntArray(2)
            val bgLoc = IntArray(2)
            backgroundView.getLocationInWindow(bgLoc)
            targetView.getLocationInWindow(loc)
            var height = targetView.height
            var y = loc[1] - height
            if (bgLoc[1] >= loc[1]) {
                height -= bgLoc[1] - loc[1]
                if (y < 0) {
                    y = 0
                }
            }

            if (y + height > blurredBitmap!!.height) {
                height = blurredBitmap.height - y
                Log.d("TAG", "Height = " + height)
                if (height <= 0) {
                    //below the screen
                    return
                }
            }
            val matrix = Matrix()

            matrix.setScale(0.5f, 0.5f)

            val bitmapBlured = Bitmap.createBitmap(blurredBitmap,
                    targetView.x.toInt(),
                    targetView.y.toInt(),
                    targetView.measuredWidth,
                    height,
                    matrix,
                    true)

            val d: Drawable?
            if (bitmapBlured != null) {
                d = RoundedBitmapDrawableFactory.create(context.get()!!.resources, bitmapBlured)
                d.cornerRadius = context.get()!!.resources.getDimensionPixelOffset(R.dimen.rounded_corner).toFloat()

            } else {
                d = ContextCompat.getDrawable(context.get()!!, R.drawable.white_background)
            }
            targetView.background = d

        }

        private fun captureView(view: View): Bitmap? {
            if (bluredBitmap != null) {
                return bluredBitmap
            }

            bluredBitmap = Bitmap.createBitmap(view.measuredWidth,
                    view.measuredHeight,
                    Bitmap.Config.ARGB_4444)

            val canvas = Canvas(bluredBitmap)
            view.draw(canvas)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ImageUtil.blurBitmap(rs!!, bluredBitmap!!)
            } else {
                bluredBitmap = ImageUtil.blurBitmap(bluredBitmap, 80, true)
            }
            val paint = Paint()
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            val filter = LightingColorFilter(-0x1, 0x00222222) // lighten
            //ColorFilter filter = new LightingColorFilter(0xFF7F7F7F, 0x00000000);    // darken
            paint.colorFilter = filter
            canvas.drawBitmap(bluredBitmap, 0f, 0f, paint)

            return bluredBitmap
        }

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        private fun blurBitmap(renderScript: RenderScript, bitmap: Bitmap) {

            val input = Allocation.createFromBitmap(renderScript, bitmap)
            val output = Allocation.createTyped(renderScript, input.type)
            val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

            // >0 and <= 25
            script.setRadius(25f)
            script.setInput(input)
            script.forEach(output)
            output.copyTo(bitmap)

        }

        private fun blurBitmap(sentBitmap: Bitmap?, radius: Int, canReuseInBitmap: Boolean): Bitmap? {

            val bitmap: Bitmap
            if (canReuseInBitmap) {
                bitmap = sentBitmap!!
            } else {
                bitmap = sentBitmap!!.copy(sentBitmap.config, true)
            }

            if (radius < 1) {
                return null
            }

            val w = bitmap.width
            val h = bitmap.height

            val pix = IntArray(w * h)
            bitmap.getPixels(pix, 0, w, 0, 0, w, h)

            val wm = w - 1
            val hm = h - 1
            val wh = w * h
            val div = radius + radius + 1

            val r = IntArray(wh)
            val g = IntArray(wh)
            val b = IntArray(wh)
            var rsum: Int
            var gsum: Int
            var bsum: Int
            var x: Int
            var y: Int
            var i: Int
            var p: Int
            var yp: Int
            var yi: Int
            var yw: Int
            val vmin = IntArray(Math.max(w, h))

            var divsum = div + 1 shr 1
            divsum *= divsum
            val dv = IntArray(256 * divsum)
            i = 0
            while (i < 256 * divsum) {
                dv[i] = i / divsum
                i++
            }

            yi = 0
            yw = yi

            val stack = Array(div) { IntArray(3) }
            var stackpointer: Int
            var stackstart: Int
            var sir: IntArray
            var rbs: Int
            val r1 = radius + 1
            var routsum: Int
            var goutsum: Int
            var boutsum: Int
            var rinsum: Int
            var ginsum: Int
            var binsum: Int

            y = 0
            while (y < h) {
                bsum = 0
                gsum = bsum
                rsum = gsum
                boutsum = rsum
                goutsum = boutsum
                routsum = goutsum
                binsum = routsum
                ginsum = binsum
                rinsum = ginsum
                i = -radius
                while (i <= radius) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))]
                    sir = stack[i + radius]
                    sir[0] = p and 0xff0000 shr 16
                    sir[1] = p and 0x00ff00 shr 8
                    sir[2] = p and 0x0000ff
                    rbs = r1 - Math.abs(i)
                    rsum += sir[0] * rbs
                    gsum += sir[1] * rbs
                    bsum += sir[2] * rbs
                    if (i > 0) {
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }
                    i++
                }
                stackpointer = radius

                x = 0
                while (x < w) {

                    r[yi] = dv[rsum]
                    g[yi] = dv[gsum]
                    b[yi] = dv[bsum]

                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum

                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]

                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]

                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm)
                    }
                    p = pix[yw + vmin[x]]

                    sir[0] = p and 0xff0000 shr 16
                    sir[1] = p and 0x00ff00 shr 8
                    sir[2] = p and 0x0000ff

                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]

                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum

                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer % div]

                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]

                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]

                    yi++
                    x++
                }
                yw += w
                y++
            }
            x = 0
            while (x < w) {
                bsum = 0
                gsum = bsum
                rsum = gsum
                boutsum = rsum
                goutsum = boutsum
                routsum = goutsum
                binsum = routsum
                ginsum = binsum
                rinsum = ginsum
                yp = -radius * w
                i = -radius
                while (i <= radius) {
                    yi = Math.max(0, yp) + x

                    sir = stack[i + radius]

                    sir[0] = r[yi]
                    sir[1] = g[yi]
                    sir[2] = b[yi]

                    rbs = r1 - Math.abs(i)

                    rsum += r[yi] * rbs
                    gsum += g[yi] * rbs
                    bsum += b[yi] * rbs

                    if (i > 0) {
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }

                    if (i < hm) {
                        yp += w
                    }
                    i++
                }
                yi = x
                stackpointer = radius
                y = 0
                while (y < h) {
                    // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                    pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum

                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]

                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]

                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w
                    }
                    p = x + vmin[y]

                    sir[0] = r[p]
                    sir[1] = g[p]
                    sir[2] = b[p]

                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]

                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum

                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer]

                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]

                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]

                    yi += w
                    y++
                }
                x++
            }

            bitmap.setPixels(pix, 0, w, 0, 0, w, h)

            return bitmap
        }


    }


}