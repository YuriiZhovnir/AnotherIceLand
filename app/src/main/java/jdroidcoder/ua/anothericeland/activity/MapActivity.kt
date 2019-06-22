package jdroidcoder.ua.anothericeland.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.location.GpsStatus
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
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
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.squareup.picasso.Picasso
import jdroidcoder.ua.anothericeland.adapter.ChangePointListener
import jdroidcoder.ua.anothericeland.adapter.PlanAdapter
import jdroidcoder.ua.anothericeland.adapter.SheetListener
import jdroidcoder.ua.anothericeland.fragment.DetailsFragment
import jdroidcoder.ua.anothericeland.gps.GPServices
import jdroidcoder.ua.anothericeland.helper.GlobalData
import jdroidcoder.ua.anothericeland.helper.Util
import jdroidcoder.ua.anothericeland.network.response.Day
import kotlinx.android.synthetic.main.content_layout.*
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class MapActivity : BaseActivity(), OnMapReadyCallback, MapboxMap.OnMarkerClickListener, MapboxMap.OnMapClickListener {
    private var locationListener: GPServices? = null
    private lateinit var mapboxMap: MapboxMap
    private var behaviorBottomSheet: BottomSheetBehavior<View>? = null
    var navigationMapRoute: NavigationMapRoute? = null
    private var style: Style? = null
    private val CURRENT_ROUTE_LAYER_ID = "current-route-layer-id"
    private val CURRENT_ROUTE_SOURCE_ID = "current-route-source-id"
    private val OTHER_DAY_ROUTE_LAYER_ID = "other-day-route-layer-id"
    private val OTHER_DAY_ROUTE_SOURCE_ID = "other-day-route-source-id"

    companion object {
        var markers: HashMap<Marker, jdroidcoder.ua.anothericeland.network.response.Point> = HashMap()
        var tempMarkers: HashMap<Marker, jdroidcoder.ua.anothericeland.network.response.Point> = HashMap()
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
        planList?.adapter = PlanAdapter(points, object : SheetListener {
            override fun dayChosen(point: jdroidcoder.ua.anothericeland.network.response.Point) {
                val geometrics: MutableList<Feature> = ArrayList()
                val currentDay = GlobalData?.trip?.days?.firstOrNull { day -> !day.isDone }
                var chosenDay: Day? = null
                for (day in GlobalData?.trip?.days!!) {
                    if (day?.points?.contains(point)) {
                        chosenDay = day
                    }
                }
                removeTempMarkers()
                chosenDay?.points?.forEach {
                    val iconFactory = IconFactory.getInstance(this@MapActivity)
                    val icon = try {
                        iconFactory.fromBitmap(
                            Util.buildIcon(
                                this@MapActivity,
                                BitmapFactory.decodeFile(it?.image), R.drawable.ic_pin_grey
                            )
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        null
                    }
                    if (it.lat != null && it.lng != null) {
                        val temp = MarkerOptions()
                            .position(it.lat?.let { it1 -> it.lng?.let { it2 -> LatLng(it1, it2) } })
                            .setIcon(icon)
                        tempMarkers.put(mapboxMap?.addMarker(temp), it)
                    }
                }
                if (chosenDay != null) {
                    if (chosenDay != currentDay) {
                        chosenDay?.direction?.legs()?.let {
                            for (leg in it) {
                                if (leg?.steps()?.isNullOrEmpty() == false) {
                                    for (step in leg.steps()!!)
                                        geometrics?.add(Feature.fromGeometry(step.geometry()?.let { it1 ->
                                            LineString.fromPolyline(
                                                it1,
                                                PRECISION_6
                                            )
                                        }))
                                }
                            }
                        }
                    } else {
                        geometrics.clear()
                    }
                    style?.getSourceAs<GeoJsonSource>(OTHER_DAY_ROUTE_SOURCE_ID)
                        ?.setGeoJson(FeatureCollection.fromFeatures(geometrics))
                }
            }
        }, object : ChangePointListener {
            override fun changePoint(point: jdroidcoder.ua.anothericeland.network.response.Point, isShow: Boolean) {
                if (!isShow) {
                    var tempMarker: Marker? = null
                    for ((marker, tempPoint) in markers) {
                        if (point == tempPoint) {
                            tempMarker = marker
                        }
                    }
                    tempMarker?.remove()
                    markers?.remove(tempMarker)
                } else {
                    val iconFactory = IconFactory.getInstance(this@MapActivity)
                    val pin = if(GlobalData.currentDay?.points?.contains(point) == true){
                        R.drawable.ic_pin_inactive
                    }else{
                        R.drawable.ic_pin_grey
                    }
                    val icon = try {
                        iconFactory.fromBitmap(
                            Util.buildIcon(
                                this@MapActivity,
                                BitmapFactory.decodeFile(point?.image), pin
                            )
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        null
                    }
                    if (point.lat != null && point.lng != null) {
                        val temp = MarkerOptions()
                            .position(point.lat?.let { it1 -> point.lng?.let { it2 -> LatLng(it1, it2) } })
                            .setIcon(icon)
                        if(GlobalData.currentDay?.points?.contains(point) == true) {
                            markers.put(mapboxMap?.addMarker(temp), point)
                        }else{
                            tempMarkers.put(mapboxMap?.addMarker(temp), point)
                        }
                    }
                }
            }

        })
        behaviorBottomSheet = BottomSheetBehavior.from(bottomSheet)
        behaviorBottomSheet?.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetHeader?.setBackgroundColor(Color.parseColor("#CCFFFFFF"))
                    val previousDay = GlobalData.currentDay
                    val newDay = GlobalData?.trip?.days?.firstOrNull { day -> !day.isDone }
                    removeTempMarkers()
                    try{
                        style?.getSourceAs<GeoJsonSource>(OTHER_DAY_ROUTE_SOURCE_ID)
                            ?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
                    }catch (ex:Exception){
                        ex.printStackTrace()
                    }
                    if (previousDay != newDay) {
                        removeMarkers()
                        GlobalData.currentDay = newDay
                        setNextPoint()
                        drawFullRoute()
                        setMarkers()
                    }
                    Thread {
                        GlobalData.trip?.let { Util.saveTrip(this@MapActivity, it) }
                    }.start()
                } else {
                    bottomSheetHeader?.setBackgroundColor(Color.parseColor("#FFFFFF"))
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
        gpsDetector()
        setNextPoint()
    }

    private fun removeMarkers() {
        GlobalData.currentDay?.points?.forEach {
            var tempMarker: Marker? = null
            for ((marker, tempPoint) in markers) {
                if (it == tempPoint) {
                    tempMarker = marker
                }
            }
            tempMarker?.remove()
            markers?.remove(tempMarker)
        }
    }

    private fun removeTempMarkers() {
        tempMarkers?.keys?.forEach {
            it.remove()
        }
        tempMarkers.clear()
    }

    private fun setNextPoint() {
//        val currentItem = GlobalData?.trip?.points?.firstOrNull { point -> !point?.isDone && point?.typeId != 3 }
//        if (currentItem == null) {
//            bottomSheetTitle?.text = getString(R.string.trip_completed)
//        } else {
//            bottomSheetTitle?.text = currentItem?.name
//        }
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
            this@MapActivity.style = style
            setMarkers()
            initSource(style)
            initLayers(style)
            mapboxMap?.locationComponent?.activateLocationComponent(
                LocationComponentActivationOptions.builder(this@MapActivity, style).build()
            )
            mapboxMap?.locationComponent?.cameraMode = CameraMode.TRACKING
            mapboxMap?.locationComponent?.renderMode = RenderMode.COMPASS
            if (ActivityCompat.checkSelfPermission(this@MapActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                mapboxMap?.locationComponent?.isLocationComponentEnabled = true
            }
            initGpsService()
            try {
                navigationMapRoute = try {
                    NavigationMapRoute(mapView, mapboxMap, "com.mapbox.annotations.points")
                } catch (ex: Exception) {
                    NavigationMapRoute(mapView, mapboxMap)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                navigationMapRoute?.updateRouteVisibilityTo(false)
            }
            drawFullRoute()
        }

        this.mapboxMap?.setOnMarkerClickListener(this)
        this.mapboxMap?.addOnMapClickListener(this)
    }

    private fun setMarkers() {
        GlobalData.currentDay?.points?.let {
            for (point in it) {
                if (point.isDone) {
                    continue
                }
                val iconFactory = IconFactory.getInstance(this)
                val icon = try {
                    iconFactory.fromBitmap(
                        Util.buildIcon(
                            this,
                            BitmapFactory.decodeFile(point?.image), R.drawable.ic_pin_inactive
                        )
                    )
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    null
                }
                val temp = MarkerOptions()
                    .position(point.lat?.let { it1 -> point.lng?.let { it2 -> LatLng(it1, it2) } })
                    .setIcon(icon)
                markers.put(this.mapboxMap?.addMarker(temp), point)
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun drawFullRoute() {
//        object : AsyncTask<Void, Void, Boolean>() {
//            override fun doInBackground(vararg params: Void?): Boolean {
//                runOnUiThread {
//        val handler = Handler()
//        handler.postDelayed(object : Runnable {
//            override fun run() {
//                getScooters(false)
//                handler.postDelayed(this, 60000)
//            }
//        }, 60000)
        Handler().post {
            if (GlobalData.currentDay != null) {
                navigationMapRoute?.removeRoute()
                navigationMapRoute?.addRoute(GlobalData.currentDay?.direction)
            }
        }

//                }
//                return true
//            }

//            override fun onPostExecute(result: Boolean?) {
//                super.onPostExecute(result)
//                drawCurrentRoute()
//            }
//        }.execute()
    }

//    private fun drawCurrentRoute() {
//        val geometrics: MutableList<Feature> = ArrayList()
//        val currentDay = GlobalData?.trip?.days?.firstOrNull { day -> !day.isDone }
//        if (currentDay != null) {
//            currentDay?.direction?.legs()?.let {
//                for (leg in it) {
//                    if (leg?.steps()?.isNullOrEmpty() == false) {
//                        for (step in leg.steps()!!)
//                            geometrics?.add(Feature.fromGeometry(step.geometry()?.let { it1 ->
//                                LineString.fromPolyline(
//                                        it1,
//                                        PRECISION_6
//                                )
//                            }))
//                    }
//                }
//            }
//            style?.getSourceAs<GeoJsonSource>(CURRENT_ROUTE_SOURCE_ID)
//                    ?.setGeoJson(FeatureCollection.fromFeatures(geometrics))
//        }
//    }

    private fun initSource(loadedMapStyle: Style) {
        loadedMapStyle?.addSource(
            GeoJsonSource(
                CURRENT_ROUTE_SOURCE_ID,
                FeatureCollection.fromFeatures(ArrayList<Feature>())
            )
        )
        loadedMapStyle?.addSource(
            GeoJsonSource(
                OTHER_DAY_ROUTE_SOURCE_ID,
                FeatureCollection.fromFeatures(ArrayList<Feature>())
            )
        )
    }

    private fun initLayers(loadedMapStyle: Style) {
        var routeLayer = LineLayer(CURRENT_ROUTE_LAYER_ID, CURRENT_ROUTE_SOURCE_ID)
        routeLayer.setProperties(
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
            lineWidth(12f),
            PropertyFactory.fillOutlineColor("#000000"),
            lineColor(Color.parseColor("#000000"))
        )
        loadedMapStyle.addLayerBelow(routeLayer, "com.mapbox.annotations.points")
        routeLayer = LineLayer(OTHER_DAY_ROUTE_LAYER_ID, OTHER_DAY_ROUTE_SOURCE_ID)
        routeLayer.setProperties(
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
            lineWidth(12f),
            PropertyFactory.fillOutlineColor("#008577"),
            lineColor(Color.parseColor("#008577"))
        )
        loadedMapStyle.addLayerBelow(routeLayer, "com.mapbox.annotations.points")
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        behaviorBottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
        var temp = markers?.get(GlobalData.selectedMarker)
        if (temp != null) {
            val iconFactory = IconFactory.getInstance(this)
            val icon = try {
                iconFactory.fromBitmap(
                    Util.buildIcon(
                        this,
                        BitmapFactory.decodeFile(temp.image), R.drawable.ic_pin_inactive
                    )
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
            GlobalData.selectedMarker?.icon = icon
            GlobalData.selectedMarker = null
        }
        temp = markers?.get(marker)
        GlobalData.selectedMarker = marker
        if (temp != null) {
            val iconFactory = IconFactory.getInstance(this)
            val icon = try {
                iconFactory.fromBitmap(
                    Util.buildIcon(
                        this,
                        BitmapFactory.decodeFile(temp.image), R.drawable.ic_pin_active
                    )
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
            marker?.icon = icon
            if (temp.image?.isNullOrEmpty() == false)
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
            val icon = try {
                iconFactory.fromBitmap(
                    Util.buildIcon(
                        this,
                        BitmapFactory.decodeFile(temp.image), R.drawable.ic_pin_inactive
                    )
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
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
        if (GlobalData?.selectedMarker != null)
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
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                locationListener?.initGoogleClient()
                mapboxMap?.locationComponent?.isLocationComponentEnabled = true
            }
        }
    }
}