package jdroidcoder.ua.anothericeland.gps

import android.content.Context
import android.location.LocationManager
import android.os.Build

object GPSHelper {

    fun checkLocationServicesEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (!statusOfGPS)
                statusOfGPS = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            return statusOfGPS
        }
        return false
    }

}