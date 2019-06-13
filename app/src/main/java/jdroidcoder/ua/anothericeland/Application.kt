package jdroidcoder.ua.anothericeland

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox

class Application:Application(){
    override fun onCreate() {
        super.onCreate()
        Mapbox.getInstance(this, getString(R.string.map_token))
    }
}