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
import kotlinx.android.synthetic.main.bottom_sheet.*
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback
import android.support.v4.app.ActivityCompat
import android.view.View
import android.view.animation.AnimationUtils
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
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.squareup.picasso.Picasso
import jdroidcoder.ua.anothericeland.adapter.PlanAdapter
import jdroidcoder.ua.anothericeland.fragment.DetailsFragment
import jdroidcoder.ua.anothericeland.gps.GPServices
import jdroidcoder.ua.anothericeland.helper.GlobalData
import jdroidcoder.ua.anothericeland.helper.Util
import kotlinx.android.synthetic.main.content_layout.*
import java.io.File
import java.lang.Exception

class MapActivity : BaseActivity(), OnMapReadyCallback, MapboxMap.OnMarkerClickListener, MapboxMap.OnMapClickListener {

    private var locationListener: GPServices? = null
    private lateinit var mapboxMap: MapboxMap
    private var behaviorBottomSheet: BottomSheetBehavior<View>? = null
    var navigationMapRoute: NavigationMapRoute? = null

    companion object {
        var markers: HashMap<Marker, jdroidcoder.ua.anothericeland.network.response.Point> = HashMap()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        ButterKnife.bind(this)

        setBottomSheetHeight()

        planList?.layoutManager = LinearLayoutManager(this)
        val points: ArrayList<jdroidcoder.ua.anothericeland.network.response.Point> = ArrayList()
        GlobalData?.trip?.days?.let {
            for (day in it) {
                points.add(
                        jdroidcoder.ua.anothericeland.network.response.Point(
                                3, day.name,
                                "", "", "", null, null, false,
                                if (day.name == "Day 1") {
                                    true
                                } else {
                                    day.isDone
                                }
                        )
                )
                points.addAll(day.points)
            }
        }
        planList?.adapter = PlanAdapter(points)
        behaviorBottomSheet = BottomSheetBehavior.from(bottomSheet)
        behaviorBottomSheet?.setBottomSheetCallback(object : BottomSheetCallback() {
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

        val currentItem = GlobalData?.trip?.points?.firstOrNull { point -> !point?.isDone && point?.typeId != 3 }
        if (currentItem == null) {
            bottomSheetTitle?.text = getString(R.string.trip_completed)
        } else {
            bottomSheetTitle?.text = currentItem?.name
        }
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
            mapboxMap?.locationComponent?.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this@MapActivity, style).build())
            mapboxMap?.locationComponent?.cameraMode = CameraMode.TRACKING
            mapboxMap?.locationComponent?.renderMode = RenderMode.COMPASS
            if (ActivityCompat.checkSelfPermission(this@MapActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mapboxMap?.locationComponent?.isLocationComponentEnabled = true
            }
            initGpsService()
            Thread {
                GlobalData.directionsRoute = Util.loadRoute(this)
                runOnUiThread {
                    if (navigationMapRoute != null) {
                        navigationMapRoute?.updateRouteVisibilityTo(false)
                    } else {
                        navigationMapRoute = NavigationMapRoute(mapView, mapboxMap)
                    }
                    navigationMapRoute?.addRoute(GlobalData.directionsRoute)
                }
            }.start()
        }
        GlobalData.trip?.points?.let {
            for (point in it) {
                val iconFactory = IconFactory.getInstance(this)
                val icon = iconFactory.fromBitmap(Util.buildIcon(this,
                        BitmapFactory.decodeFile(point?.image), R.drawable.ic_pin_inactive))
                markers.put(this.mapboxMap?.addMarker(
                        MarkerOptions()
                                .position(point.lat?.let { it1 -> point.lng?.let { it2 -> LatLng(it1, it2) } })
                                .setIcon(icon)), point)
            }
        }
        this.mapboxMap?.setOnMarkerClickListener(this)
        this.mapboxMap?.addOnMapClickListener(this)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        behaviorBottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
        var temp = markers?.get(GlobalData.selectedMarker)
        if (temp != null) {
            val iconFactory = IconFactory.getInstance(this)
            val icon = iconFactory.fromBitmap(Util.buildIcon(this,
                    BitmapFactory.decodeFile(temp.image), R.drawable.ic_pin_inactive))
            GlobalData.selectedMarker?.icon = icon
            GlobalData.selectedMarker = null
        }
        temp = markers?.get(marker)
        GlobalData.selectedMarker = marker
        if (temp != null) {
            val iconFactory = IconFactory.getInstance(this)
            val icon = iconFactory.fromBitmap(Util.buildIcon(this,
                    BitmapFactory.decodeFile(temp.image), R.drawable.ic_pin_active))
            marker?.icon = icon
            Picasso.get().load(File(temp.image)).into(locationImage)
            locationName?.text = temp.name
            locationShortDescription?.text = temp.description
        }

        if (bottom?.visibility == View.GONE) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.enter_to_up)
            bottom?.visibility = View.VISIBLE
            bottom?.startAnimation(anim)
        }
        return false
    }

    override fun onMapClick(point: LatLng): Boolean {
        val temp = markers?.get(GlobalData.selectedMarker)
        if (temp != null) {
            val iconFactory = IconFactory.getInstance(this)
            val icon = iconFactory.fromBitmap(Util.buildIcon(this,
                    BitmapFactory.decodeFile(temp.image), R.drawable.ic_pin_inactive))
            GlobalData.selectedMarker?.icon = icon
            GlobalData.selectedMarker = null
        }
        if (bottom?.visibility == View.VISIBLE) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.enter_to_down)
            bottom?.visibility = View.GONE
            bottom?.startAnimation(anim)
        }
        return false
    }

    @OnClick(R.id.more, R.id.bottom)
    fun more() {
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
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationListener?.initGoogleClient()
                mapboxMap?.locationComponent?.isLocationComponentEnabled = true
            }
        }
    }
}