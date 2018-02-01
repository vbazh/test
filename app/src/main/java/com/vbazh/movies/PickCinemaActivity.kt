package com.vbazh.movies

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.vbazh.movies.common.Constants
import kotlinx.android.synthetic.main.activity_pick_cinema.*

class PickCinemaActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_cinema)

        poster.setOnClickListener {
            val intent = Intent(this, CinemaDetail::class.java)

            val screenLocation = IntArray(2)

            poster?.getLocationOnScreen(screenLocation)

            intent.putExtra(Constants.ORIENTATION, resources.configuration.orientation)
            intent.putExtra(Constants.ORIG_LEFT, screenLocation[0])
            intent.putExtra(Constants.ORIG_TOP, screenLocation[1])
            intent.putExtra(Constants.ORIG_WIDTH, poster.width)
            intent.putExtra(Constants.ORIG_HEIGHT, poster.height)
            startActivity(intent)
            overridePendingTransition(0, 0);
        }

    }
}