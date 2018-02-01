package com.vbazh.movies

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.view.animation.*
import com.vbazh.movies.common.Constants
import com.vbazh.movies.utils.ImageUtil
import kotlinx.android.synthetic.main.activity_detailed.*
import java.lang.ref.WeakReference

class CinemaDetail : AppCompatActivity() {

    var leftDelta = 0
    var topDelta = 0

    var top = 0
    var left = 0
    var width = 0
    var height = 0

    var widthScale = 0f
    var heightScale = 0f

    var originalOrientation = 0

    val ANIM_DURATION = 200L
    val decelerator = DecelerateInterpolator()
    val linear = LinearInterpolator()
    val overshoot = OvershootInterpolator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed)

        val bundle = intent.extras
        top = bundle.getInt(Constants.ORIG_TOP)
        left = bundle.getInt(Constants.ORIG_LEFT)
        width = bundle.getInt(Constants.ORIG_WIDTH)
        height = bundle.getInt(Constants.ORIG_HEIGHT)
        originalOrientation = bundle.getInt(Constants.ORIENTATION)

        if (savedInstanceState == null) {
            val observer = poster_layout?.viewTreeObserver
            observer?.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    poster_layout?.viewTreeObserver?.removeOnPreDrawListener(this)

                    val screenLocation = IntArray(2)

                    poster?.getLocationOnScreen(screenLocation)

                    leftDelta = left - screenLocation[0]
                    topDelta = top - screenLocation[1]

                    widthScale = width.toFloat() / poster.width.toFloat()
                    heightScale = height.toFloat() / poster.height.toFloat()

                    runEnterAnimation()

                    return true
                }
            })
        }

        closeArrow.setOnClickListener {
            onBackPressed()
        }

        playButton.setOnClickListener {
            startActivity(Intent(this, VideoActivity::class.java))
        }
    }

    private fun runEnterAnimation() {

        playButton.alpha = 0f

        poster_layout.pivotX = 0f
        poster_layout.pivotY = 0f

        poster_layout.scaleX = widthScale
        poster_layout.scaleY = heightScale

        poster_layout.translationX = leftDelta.toFloat()
        poster_layout.translationY = topDelta.toFloat()

        poster_layout.animate().setDuration(ANIM_DURATION).scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .translationY(0f).setInterpolator(decelerator)
                .withEndAction {
                    ImageUtil.blurView(poster, playButton, WeakReference(MainActivity@ this))

                    playButton.scaleX = 0f
                    playButton.scaleY = 0f

                    playButton.animate().setDuration(ANIM_DURATION / 2)
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f).interpolator = decelerator
                }
    }

    private fun runExitAnimation(endAction: Runnable) {

        val fadeOut: Boolean
        if (resources.configuration.orientation != originalOrientation) {
            poster_layout.pivotX = (poster_layout.width / 2).toFloat()
            poster_layout.pivotY = (poster_layout.height / 2).toFloat()

            leftDelta = 0
            topDelta = 0
            fadeOut = true

        } else {
            fadeOut = false
        }

        closeArrow.animate().setInterpolator(decelerator).alpha(0f).scaleY(0f).scaleX(0f)

        infoLayout.animate().setDuration(50).alpha(0f)

        poster_layout.animate().setDuration(200)
                .translationY(300f)
                .setInterpolator(linear).withEndAction {


            poster_layout.animate().setDuration(ANIM_DURATION * 2)
                    .scaleX(widthScale).scaleY(heightScale)
                    .translationX(leftDelta.toFloat())
                    .translationY(topDelta.toFloat())
                    .setInterpolator(overshoot)
                    .withEndAction(endAction)
            playButton.animate().setDuration(ANIM_DURATION / 2)
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f).interpolator = decelerator
            if (fadeOut) poster_layout.animate().alpha(0f)

        }
    }

    override fun onBackPressed() {

        runExitAnimation(Runnable { finish() })
    }

    override fun finish() {
        super.finish()

        overridePendingTransition(0, 0)
    }
}