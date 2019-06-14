package jdroidcoder.ua.anothericeland.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.OnClick
import jdroidcoder.ua.anothericeland.R
import jdroidcoder.ua.anothericeland.helper.GlobalData
import jdroidcoder.ua.anothericeland.helper.Util
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalData?.trip = Util.getTrip(this)
        if (GlobalData.trip != null) {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        }
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
    }

    @OnClick(R.id.loginButton)
    fun login() {
        GlobalData.number = number?.text?.toString()
        GlobalData.password = password?.text?.toString()
        startActivityForResult(Intent(this, SplashActivity::class.java), 1456)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == 1456 && resultCode == Activity.RESULT_OK) {
                startActivity(Intent(this, MapActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, getString(R.string.invalid_data), Toast.LENGTH_LONG).show()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
