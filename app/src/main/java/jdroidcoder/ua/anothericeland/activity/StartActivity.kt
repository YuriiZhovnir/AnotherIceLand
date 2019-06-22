package jdroidcoder.ua.anothericeland.activity

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.WindowManager
import jdroidcoder.ua.anothericeland.R
import kotlinx.android.synthetic.main.activity_splash.*
import jdroidcoder.ua.anothericeland.helper.GlobalData
import jdroidcoder.ua.anothericeland.helper.Util

class StartActivity : BaseActivity() {
    private var frameAnimation: AnimationDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        spinner?.background?.let { showSpinner(it) }
        status?.text = ""
        Thread {
            GlobalData?.trip = Util.getTrip(this)
            if (GlobalData.trip != null) {
                GlobalData.currentDay = GlobalData?.trip?.days?.firstOrNull { day-> !day.isDone }
//                GlobalData.directionsRoute = Util.loadRoute(this@StartActivity)
                startActivity(Intent(this, MapActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }.start()
    }


    private fun showSpinner(drawable: Drawable) {
        try {
            if (frameAnimation == null) {
                window?.setFlags(
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
                frameAnimation = drawable as AnimationDrawable
                frameAnimation?.start()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            window?.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        }
    }
}