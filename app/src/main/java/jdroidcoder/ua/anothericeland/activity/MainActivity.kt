package jdroidcoder.ua.anothericeland.activity

import android.content.Intent
import android.os.Bundle
import butterknife.ButterKnife
import butterknife.OnClick
import jdroidcoder.ua.anothericeland.R

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
    }

    @OnClick(R.id.loginButton)
    fun login() {
        startActivity(Intent(this, MapActivity::class.java))
        finish()
    }
}
