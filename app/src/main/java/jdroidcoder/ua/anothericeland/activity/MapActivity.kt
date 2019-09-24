package jdroidcoder.ua.anothericeland.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.location.GpsStatus
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.activity_map.*
import com.mapbox.mapboxsdk.maps.Style
import jdroidcoder.ua.anothericeland.R
import kotlinx.android.synthetic.main.bottom_sheet.*
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.OnClick
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
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
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.squareup.picasso.Picasso
import jdroidcoder.ua.anothericeland.adapter.ChangePointListener
import jdroidcoder.ua.anothericeland.adapter.PlanAdapter
import jdroidcoder.ua.anothericeland.adapter.SheetListener
import jdroidcoder.ua.anothericeland.fragment.DetailsFragment
import jdroidcoder.ua.anothericeland.gps.GPSHelper.checkLocationServicesEnabled
import jdroidcoder.ua.anothericeland.gps.GPServices
import jdroidcoder.ua.anothericeland.helper.GlobalData
import jdroidcoder.ua.anothericeland.helper.Util
import jdroidcoder.ua.anothericeland.network.response.Day
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.content_layout.*
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class MapActivity : BaseActivity(), OnMapReadyCallback, MapboxMap.OnMarkerClickListener,
        MapboxMap.OnMapClickListener, NavigationView.OnNavigationItemSelectedListener {

    private var locationListener: GPServices? = null
    private lateinit var mapboxMap: MapboxMap
    private var frameAnimation: AnimationDrawable? = null
    private var behaviorBottomSheet: BottomSheetBehavior<View>? = null
    var navigationMapRoute: NavigationMapRoute? = null
    private var style: Style? = null
    private val CURRENT_ROUTE_LAYER_ID = "current-route-layer-id"
    private val CURRENT_ROUTE_SOURCE_ID = "current-route-source-id"
    private val OTHER_DAY_ROUTE_LAYER_ID = "other-day-route-layer-id"
    private val OTHER_DAY_ROUTE_SOURCE_ID = "other-day-route-source-id"
    private var isMenuShowing = false

    companion object {
        var markers: HashMap<Marker, jdroidcoder.ua.anothericeland.network.response.Point> =
                HashMap()
        var tempMarkers: HashMap<Marker, jdroidcoder.ua.anothericeland.network.response.Point> =
                HashMap()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        ButterKnife.bind(this)
        setBottomSheetHeight()
        showMenu?.visibility = View.VISIBLE

        planList?.layoutManager = GridLayoutManager(this, 5)
        val points: ArrayList<jdroidcoder.ua.anothericeland.network.response.Point> = ArrayList()
        GlobalData?.trip?.days?.let {
            for (day in it) {
                points.add(
                        jdroidcoder.ua.anothericeland.network.response.Point(
                                3, day.name,
                                "", "", "", null, null, false,
                                if (day.name == "יום 1") {
                                    true
                                } else {
                                    day.isDone
                                }
                        )
                )
            }
        }
        planList?.adapter = PlanAdapter(points, object : SheetListener {
            override fun dayChosen(point: jdroidcoder.ua.anothericeland.network.response.Point) {

                try {
                    val chosenDay: Day? =
                            GlobalData.trip?.days?.firstOrNull { p -> p.name == point.name }
                                    ?: return
                    chosenDay?.isDone = false
                    chosenDay?.points?.forEach {
                        it.isDone = false
                    }
                    val previousDay =
                            GlobalData.trip?.days?.firstOrNull { day -> day == GlobalData.currentDay }
                    previousDay?.isDone = true
                    previousDay?.points?.forEach {
                        it.isDone = true
                    }
                    removeTempMarkers()
                    removeMarkers()
                    GlobalData.currentDay = chosenDay
                    setMarkers()
                    if (chosenDay != null) {
                        Handler().post {
                            navigationMapRoute?.removeRoute()
                            if (chosenDay?.direction != null) {
                                try {
                                    navigationMapRoute?.addRoute(chosenDay?.direction)
                                } catch (ex: ArrayIndexOutOfBoundsException) {
                                    ex.printStackTrace()
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }, object : ChangePointListener {
            override fun changePoint(
                    point: jdroidcoder.ua.anothericeland.network.response.Point,
                    isShow: Boolean
            ) {
            }

        })
        behaviorBottomSheet = BottomSheetBehavior.from(bottomSheet)
        behaviorBottomSheet?.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetHeader?.setBackgroundColor(Color.parseColor("#CCFFFFFF"))
                    try {
                        style?.getSourceAs<GeoJsonSource>(OTHER_DAY_ROUTE_SOURCE_ID)
                                ?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    Thread {
                        GlobalData.trip?.let { Util.saveTrip(this@MapActivity, it) }
                    }.start()
                } else {
                    bottomSheetHeader?.setBackgroundColor(Color.parseColor("#FFFFFF"))
                    val temp = markers?.get(GlobalData.selectedMarker)
                    if (temp != null) {
                        val iconFactory = IconFactory.getInstance(this@MapActivity)
                        val icon = try {
                            iconFactory.fromBitmap(
                                    Util.buildIcon(
                                            this@MapActivity,
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
                        val anim =
                                AnimationUtils.loadAnimation(this@MapActivity, R.anim.enter_to_down)
                        bottom?.visibility = View.GONE
                        bottom?.startAnimation(anim)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
        gpsDetector()
        navView?.setNavigationItemSelectedListener(this)
    }

    @OnClick(R.id.showMenu)
    fun showHideMenu() {
        if (!isMenuShowing) {
            drawerLayout.openDrawer(GravityCompat.START)
        } else {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        isMenuShowing = !isMenuShowing
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.important_information -> {
                GlobalData.selectedMarker = null
                onMapClick(LatLng(0.0, 0.0))
                val point = jdroidcoder.ua.anothericeland.network.response.Point(4,
                        getString(R.string.important_info), "  <h2>מספרי חירום</h2>     <p>מוקד טלפוני 24/7 למקרי חירום : 354-8936115</p>     <p>משטרה / אמבולנס / מכבי אש חייגו 102</p>    <H2>מידע חשוב</H2>     <p>ביקור בכל האתרים והפעילויות המוזכרים בתכנית הטיול תלויים במז האוויר. מומלץ לברר את תנאי מזג האוויר והדרכים בתחילת כל יום לפני היציאה לדרך. </p> <p>לאתר תחזית מזג האויר: <a href=\"http://en.vedur.is/\">en.vedur.is</a></p> <p>מידע על נהיגה בטוחה באיסלנד: <a href=\"http://www.safetravel.is/En/\">safetravel.is</a></p> <p>עדכון חי על מצב הכבישים והדרכים: <a href=\"http://www.road.is\">www.road.is</a></p>    דרך צלחה!",
                        null, null, null, null)
                GlobalData.selectedPoint = point
                if (supportFragmentManager?.findFragmentByTag(DetailsFragment.TAG) == null) {
                    val fragment = DetailsFragment.newInstance()
                    supportFragmentManager?.beginTransaction()
                            ?.setCustomAnimations(R.anim.enter_to_up, 0, 0, R.anim.enter_to_down)
                            ?.replace(android.R.id.content, fragment, DetailsFragment.TAG)
                            ?.addToBackStack(fragment::class.java.name)
                            ?.commit()
                }
                showHideMenu()
            }
            R.id.laguns -> {
                if (packageManager?.hasSystemFeature("android.software.webview") == true) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.icelandlagoons.com"))
                    startActivity(browserIntent)
                }
            }
            R.id.logout -> {
                Util.saveTrip(this@MapActivity, null)
                startActivity(Intent(this, StartActivity::class.java))
                finish()
            }
        }
        return false
    }

    private fun removeMarkers() {
        for ((marker, tempPoint) in markers) {

            marker.remove()
        }
        markers?.clear()
    }

    private fun removeTempMarkers() {
        tempMarkers?.keys?.forEach {
            it.remove()
        }
        tempMarkers.clear()
    }

    private fun initGpsService() {
        if (locationListener == null) {
            locationListener = GPServices(this)
            locationListener?.initGoogleClient()
        }
    }

    @OnClick(R.id.showFullTrip)
    fun showFullTrip() {
        if (GlobalData.directionsRoute == null) {
            GlobalData.directionsRoute = Util.loadRoute(this)
        }
        removeTempMarkers()
        removeMarkers()
        GlobalData.trip?.points?.forEach {
            if (!markers?.values?.contains(it)) {
                val iconFactory = IconFactory.getInstance(this@MapActivity)
                val icon = try {
                    iconFactory.fromBitmap(
                            Util.buildIcon(
                                    this@MapActivity,
                                    BitmapFactory.decodeFile(it?.image), R.drawable.ic_pin_inactive
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
                    markers.put(mapboxMap?.addMarker(temp), it)
                }
            }
        }

        Handler().post {
            navigationMapRoute?.removeRoute()
            if (GlobalData?.directionsRoute != null) {
                try {
                    navigationMapRoute?.addRoute(GlobalData?.directionsRoute)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
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
        params.height = (size.y * 0.28).toInt()
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
            if (ActivityCompat.checkSelfPermission(
                            this@MapActivity,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
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
        Handler().post {
            if (GlobalData.currentDay != null) {
                navigationMapRoute?.removeRoute()
                try {
                    navigationMapRoute?.addRoute(GlobalData.currentDay?.direction)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            } else {
                showFullTrip()
            }
        }
    }

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
                PropertyFactory.fillOutlineColor("#008000"),
                lineColor(Color.parseColor("#008000"))
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
            if (bottom?.visibility == View.GONE) {
                val anim = AnimationUtils.loadAnimation(this, R.anim.enter_to_up)
                bottom?.visibility = View.VISIBLE
                bottom?.startAnimation(anim)
            }
        }
        return false
    }

    @OnClick(R.id.startNavigation)
    fun navigateMe() {
        if (isNetworkAvailable(this) == false) {
            alertConnectNetwork()
            return
        } else {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        10015)
                return
            } else {
                if (checkLocationServicesEnabled(this) == false) {
                    alertAccessGps()
                    return
                }
            }
        }

        if (GPServices.location == null) {
            showNotLocationAlert()
            return
        }

        spinner_route.visibility = View.VISIBLE
        spinner_route?.background?.let { showSpinner(it) }


        val point = markers?.get(GlobalData?.selectedMarker) ?: return

        try {
            val origin = GPServices?.location?.longitude?.let {
                GPServices?.location?.latitude?.let { it1 ->
                    com.mapbox.geojson.Point.fromLngLat(
                            it,
                            it1
                    )
                }
            }
            val destination = point.lng?.let {
                point.lat?.let { it1 ->
                    com.mapbox.geojson.Point.fromLngLat(
                            it,
                            it1
                    )
                }
            }
            destination?.let {
                origin?.let { it1 ->
                    NavigationRoute.builder(this@MapActivity)
                            .accessToken(getString(R.string.map_token))
                            .origin(it1)
                            .destination(it)
                            .build()
                            .getRoute(object : Callback<DirectionsResponse> {
                                override fun onResponse(
                                        call: Call<DirectionsResponse>,
                                        response: Response<DirectionsResponse>
                                ) {
                                    hideSpinner()
                                    spinner_route.visibility = View.GONE

                                    if (response.body()?.routes()?.first() != null) {
                                        val simulateRoute = false
                                        val options = NavigationLauncherOptions.builder()
                                                .directionsRoute(response.body()?.routes()?.first())
                                                .shouldSimulateRoute(simulateRoute)
                                                .build()
                                        NavigationLauncher.startNavigation(this@MapActivity, options)
                                    }
                                }

                                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                                    hideSpinner()
                                    spinner_route.visibility = View.GONE
                                }
                            })
                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun showNotLocationAlert() {
        try {
            android.support.v7.app.AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setMessage(getString(R.string.no_location_alert_message))
                    .setPositiveButton(getString(R.string.re_try)) { dialog, which ->
                        navigateMe()
                        dialog?.dismiss()
                    }.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                        dialog?.dismiss()
                    }.show()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun alertAccessGps() {
        try {
            android.support.v7.app.AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setMessage(resources?.getString(R.string.need_turn_on_gps))
                    .setPositiveButton(resources?.getString(R.string.turn_on_gps)) { dialog, which ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        dialog?.dismiss()
                    }.setNegativeButton(resources?.getString(R.string.close_label)) { dialog, which ->
                        dialog?.dismiss()
                    }.show()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
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

    @OnClick(R.id.more)
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

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected

    }


    private fun alertConnectNetwork() {
        try {
            android.support.v7.app.AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setMessage(getString(R.string.internet_alert_checked_title))
                    .setPositiveButton(getString(R.string.re_try)) { dialog, which ->
                        navigateMe()
                        dialog?.dismiss()
                    }.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                        dialog?.dismiss()
                    }.show()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
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

    private fun hideSpinner() {
        try {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            frameAnimation?.stop()
            frameAnimation = null
        } catch (ex: Exception) {
            frameAnimation = null
            ex.printStackTrace()
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

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED
            ) {
                if (requestCode == 10015) {
                    if (GPServices.location == null) {
                        locationListener?.initGoogleClient()
                        mapboxMap?.locationComponent?.isLocationComponentEnabled = true
                    }
                    navigateMe()
                } else {
                    locationListener?.initGoogleClient()
                    mapboxMap?.locationComponent?.isLocationComponentEnabled = true
                }
            }
        }
    }
}