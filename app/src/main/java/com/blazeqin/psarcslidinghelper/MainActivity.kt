package com.blazeqin.psarcslidinghelper

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var psSlidingHelper:PsSlidingHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        view.post {
            psSlidingHelper = PsSlidingHelper.create(view,object :OnSlidingListener{
                override fun onSliding(angle: Float) {
                    view.rotation += angle
                }
            })
            psSlidingHelper?.enableInertialSliding(true)
            psSlidingHelper?.setOnSlideFinishListener(object :OnSlidingFinishListener{
                override fun onSlidingFinished() {
                    Toast.makeText(baseContext,"finished",Toast.LENGTH_LONG).show()
                }
            })
        }
        window.decorView.setOnTouchListener { _, event ->
            psSlidingHelper?.handleMovement(event)
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        psSlidingHelper?.release()
    }
}
