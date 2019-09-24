package jdroidcoder.ua.anothericeland.gps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import android.os.Looper
import android.provider.Settings
import android.support.v7.app.AlertDialog
import com.google.android.gms.location.*
import jdroidcoder.ua.anothericeland.R
import java.lang.Exception

class GPServices(private var context: AppCompatActivity) : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    companion object {
        var location: Location? = null
    }

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null

    fun initGoogleClient() {
        if (!hasGps()) {
            return
        }
        mGoogleApiClient = GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
        mGoogleApiClient?.connect()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        println("Connection failed")
    }

    override fun onConnected(p0: Bundle?) {
        mLocationRequest = LocationRequest.create()
        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest?.interval = 1000
        mLocationRequest?.fastestInterval = 1000
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    10003)
        } else {
            trackLocation()
        }
    }

    private fun displayNoLocationProviderDialog() {
        try {
            AlertDialog.Builder(context)
                    .setMessage(context?.resources?.getString(R.string.need_turn_on_gps))
                    .setPositiveButton(context?.resources?.getString(R.string.turn_on_gps)) { dialog, which ->
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        dialog?.dismiss()
                    }.setNegativeButton(context?.resources?.getString(R.string.close_label)) { dialog, which ->
                        dialog?.dismiss()
                    }.show()
        }catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    fun alertAboutGPS() {
        if (!GPSHelper.checkLocationServicesEnabled(context)) {
            displayNoLocationProviderDialog()
        }
    }

    fun hasGps(): Boolean {
        val mgr = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                ?: return false
        val providers = mgr.allProviders
        return providers != null && providers.contains(LocationManager.GPS_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    fun trackLocation() {
        alertAboutGPS()
        getFusedLocationProviderClient(context).requestLocationUpdates(mLocationRequest, object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {
                location = locationResult?.locations?.get(0)
            }
        }, Looper.myLooper())
    }


    override fun onConnectionSuspended(p0: Int) {
    }
}