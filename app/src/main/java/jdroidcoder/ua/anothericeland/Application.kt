package jdroidcoder.ua.anothericeland

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.mapbox.mapboxsdk.Mapbox
import io.fabric.sdk.android.Fabric

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        Mapbox.getInstance(this, getString(R.string.map_token))
    }
}