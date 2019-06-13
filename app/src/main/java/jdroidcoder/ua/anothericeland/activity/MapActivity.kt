package jdroidcoder.ua.anothericeland.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.location.GpsStatus
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.activity_map.*
import com.mapbox.mapboxsdk.maps.Style
import jdroidcoder.ua.anothericeland.R
import jdroidcoder.ua.anothericeland.adapter.PlanAdapter
import kotlinx.android.synthetic.main.bottom_sheet.*
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback
import android.support.v4.app.ActivityCompat
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import butterknife.ButterKnife
import butterknife.OnClick
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import jdroidcoder.ua.anothericeland.fragment.DetailsFragment
import jdroidcoder.ua.anothericeland.gps.GPServices
import jdroidcoder.ua.anothericeland.helper.Util
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.content_layout.*
import java.lang.Exception

class MapActivity : BaseActivity(), OnMapReadyCallback, MapboxMap.OnMarkerClickListener, MapboxMap.OnMapClickListener {

    private var locationListener: GPServices? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var marker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        ButterKnife.bind(this)

        setBottomSheetHeight()

        val x: ArrayList<Object> = ArrayList()
        for (i in 0..10) {
            x.add(Object())
        }
        planList?.layoutManager = LinearLayoutManager(this)
        planList?.adapter = PlanAdapter(x)
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetHeader?.setBackgroundColor(Color.parseColor("#CCFFFFFF"))
                } else {
                    bottomSheetHeader?.setBackgroundColor(Color.parseColor("#FFFFFF"))
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
        gpsDetector()
        initGpsService()
    }

    private fun initGpsService() {
        if (locationListener == null) {
            locationListener = GPServices(this)
            locationListener?.initGoogleClient()
        }
    }

    private fun gpsDetector() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.addGpsStatusListener { event ->
                when (event) {
                    GpsStatus.GPS_EVENT_STOPPED -> {
                        locationListener?.alertAboutGPS()
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun setBottomSheetHeight() {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val params = bottomSheetHeader?.layoutParams as LinearLayout.LayoutParams
        params.height = (size.y * 0.35).toInt()
        bottomSheetHeader?.layoutParams = params
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        this.mapboxMap?.setStyle(Style.MAPBOX_STREETS) { style ->
            mapboxMap?.locationComponent?.activateLocationComponent(LocationComponentActivationOptions.builder(this@MapActivity, style).build())
            mapboxMap?.locationComponent?.cameraMode = CameraMode.TRACKING
            mapboxMap?.locationComponent?.renderMode = RenderMode.COMPASS
            if (ActivityCompat.checkSelfPermission(this@MapActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mapboxMap?.locationComponent?.isLocationComponentEnabled = true
            }
        }
        val iconFactory = IconFactory.getInstance(this)
        val icon = iconFactory.fromBitmap(Util.buildIcon(this,
                BitmapFactory.decodeResource(resources, R.drawable.ic_login_background), R.drawable.ic_pin_inactive))
        marker = this.mapboxMap?.addMarker(MarkerOptions()
                .position(LatLng(50.619736, 26.251301))
                .setIcon(icon))
        this.mapboxMap?.setOnMarkerClickListener(this)
        this.mapboxMap?.addOnMapClickListener(this)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val iconFactory = IconFactory.getInstance(this)
        val icon = iconFactory.fromBitmap(Util.buildIcon(this,
                BitmapFactory.decodeResource(resources, R.drawable.ic_login_background), R.drawable.ic_pin_active))
        this.marker?.icon = icon
        if (bottom?.visibility == View.GONE) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.enter_to_up)
            bottom?.visibility = View.VISIBLE
            bottom?.startAnimation(anim)
        }
        return false
    }

    override fun onMapClick(point: LatLng): Boolean {
        val iconFactory = IconFactory.getInstance(this)
        val icon = iconFactory.fromBitmap(Util.buildIcon(this,
                BitmapFactory.decodeResource(resources, R.drawable.ic_login_background), R.drawable.ic_pin_inactive))
        marker?.icon = icon
        if (bottom?.visibility == View.VISIBLE) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.enter_to_down)
            bottom?.visibility = View.GONE
            bottom?.startAnimation(anim)
        }
        return false
    }

    @OnClick(R.id.more, R.id.bottom)
    fun more(){
        if (supportFragmentManager?.findFragmentByTag(DetailsFragment.TAG) == null) {
            val fragment = DetailsFragment.newInstance()
            supportFragmentManager?.beginTransaction()
                    ?.setCustomAnimations(R.anim.enter_to_up, 0, 0, R.anim.enter_to_down)
                    ?.replace(android.R.id.content, fragment, DetailsFragment.TAG)
                    ?.addToBackStack(fragment::class.java.name)
                    ?.commit()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationListener?.initGoogleClient()
                mapboxMap?.locationComponent?.isLocationComponentEnabled = true
            }
        }
    }
}