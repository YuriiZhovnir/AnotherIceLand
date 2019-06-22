package jdroidcoder.ua.anothericeland.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.WindowManager
import jdroidcoder.ua.anothericeland.R
import jdroidcoder.ua.anothericeland.network.Api
import jdroidcoder.ua.anothericeland.network.response.Trip
import jdroidcoder.ua.apiservice.initializer.ApiServiceInitializer
import jdroidcoder.ua.apiservice.network.RetrofitSubscriber
import kotlinx.android.synthetic.main.activity_splash.*
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Environment
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import jdroidcoder.ua.anothericeland.helper.GlobalData
import jdroidcoder.ua.anothericeland.helper.Util
import jdroidcoder.ua.anothericeland.network.response.Day
import jdroidcoder.ua.anothericeland.network.response.Point
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

class SplashActivity : BaseActivity() {
    private var frameAnimation: AnimationDrawable? = null
    private var imageDownloadedCount = 0
    val images: ArrayList<String> = ArrayList()
    lateinit var trip: Trip
    private var dayCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        status?.text = getString(R.string.receiving_data)
        if (GlobalData.trip == null) {
            checkPermission()
        } else {
            hideSpinner()
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        }
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            download()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                43
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 43 && (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            download()
        } else {
            checkPermission()
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun download() {
        spinner?.background?.let { showSpinner(it) }
        val apiService: Api? = ApiServiceInitializer.init("http://18.184.47.87/api/")?.create(Api::class.java)
//        GlobalData?.password
        apiService?.getTrip(GlobalData?.number, null)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.unsubscribeOn(Schedulers.io())
            ?.subscribe(object : RetrofitSubscriber<Trip>() {
                override fun onNext(response: Trip) {
                    trip = response
                    if (response?.image?.isNullOrEmpty() == false) {
                        response?.image?.let {
                            images.add(it)
                        }
                    }
                    response?.points?.forEach {
                        if (it.image?.isNullOrEmpty() == false) {
                            it.image?.let { it1 ->
                                images.add(it1)
                            }
                        }
                        it.isHotel = it.typeId?.let { it1 -> Util.isHotel(it1) } ?: false
                    }
                    if (!images?.isEmpty()) {
                        GetBitmapFromURLAsync().execute(images?.get(0))
                    } else {
                        imageDownloaded("", "")
                    }
                }

                override fun onError(e: Throwable) {
                    super.onError(e)
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            })
    }

    private inner class GetBitmapFromURLAsync : AsyncTask<String, Void, Bitmap?>() {
        var imageUrl = ""
        override fun doInBackground(vararg params: String): Bitmap? {
            imageUrl = params[0]
            println("startImage = ${imageUrl}")
            return try {
                getBitmapFromURL(params[0])
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            runOnUiThread {
                try {
                    val pictureFile = getOutputMediaFile()
                    try {
                        val fos = FileOutputStream(pictureFile)
                        bitmap?.compress(Bitmap.CompressFormat.PNG, 90, fos)
                        fos.close()
                        imageDownloaded(imageUrl, pictureFile?.absolutePath!!)
                        println("finishImage = ${imageUrl}")
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        imageDownloaded("", "")
                    } catch (e: IOException) {
                        e.printStackTrace()
                        imageDownloaded("", "")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageDownloaded("", "")
                }
                imageUrl = ""
            }
        }
    }

    fun getBitmapFromURL(src: String): Bitmap? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun imageDownloaded(imageUrl: String, localPath: String) {
        println("imageDownloaded = ${imageUrl}, localPath = ${localPath}")
        if (trip.image == imageUrl) {
            trip.image = localPath
        } else {
            try {
                trip?.points?.firstOrNull { item -> item.image == imageUrl }?.image = localPath
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        imageDownloadedCount++
        if (imageDownloadedCount >= images?.count()) {
            makeDays()
            status?.text = getString(R.string.building_routes)
            getDayRoutes(dayCount)
        } else {
            GetBitmapFromURLAsync().execute(images?.get(imageDownloadedCount))
        }
    }

    private fun getDayRoutes(dayIndex: Int) {
        println("start route $dayCount")
        val firstPoint = trip?.days?.get(dayIndex)?.points?.first()
        val lastPoint = trip?.days?.get(dayIndex)?.points?.last()
        val origin =
            firstPoint?.lng?.let { firstPoint?.lat?.let { it1 -> com.mapbox.geojson.Point.fromLngLat(it, it1) } }
        val destination =
            lastPoint?.lng?.let { lastPoint?.lat?.let { it1 -> com.mapbox.geojson.Point.fromLngLat(it, it1) } }
        origin?.let { it1 ->
            destination?.let { it2 ->
                val builder = NavigationRoute.builder(this)
                    .accessToken(getString(R.string.map_token))
                    .origin(it1)
                    .destination(it2)
                    .profile(DirectionsCriteria.PROFILE_DRIVING)

                trip?.days?.get(dayIndex)?.points?.let { it4 ->
                    for (point in it4) {
                        if (point != firstPoint && point != lastPoint) {
                            point.lng?.let { it2 ->
                                point.lat?.let { it3 ->
                                    com.mapbox.geojson.Point.fromLngLat(it2, it3)
                                }
                            }?.let { it3 ->
                                builder?.addWaypoint(it3)
                            }
                        }
                    }
                }
                builder?.build()?.getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        if (response.body() == null) {
                            dayCount++
                            if (dayCount >= trip?.days?.count()) {
                                GlobalData.trip = trip
                                getRoute()
                            } else {
                                getDayRoutes(dayCount)
                            }
                            return
                        } else if (response.body()?.routes()?.size!! < 1) {
                            dayCount++
                            if (dayCount >= trip?.days?.count()) {
                                GlobalData.trip = trip
                                getRoute()
                            } else {
                                getDayRoutes(dayCount)
                            }
                            return
                        }
                        trip?.days?.get(dayIndex)?.direction = response.body()?.routes()?.first()
                        dayCount++
                        println("finish route $dayCount")
                        if (dayCount >= trip?.days?.count()) {
                            GlobalData.trip = trip
                            runOnUiThread {
                                status?.text = getString(R.string.yet_not_much)
                            }
                            GlobalData.currentDay = GlobalData?.trip?.days?.firstOrNull { day -> !day.isDone }
                            getRoute()
                        } else {
                            getDayRoutes(dayCount)
                        }
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                        dayCount++
                        if (dayCount >= trip?.days?.count()) {
                            GlobalData.trip = trip
                            getRoute()
                        }
                    }
                })
            }
        }
    }

    private fun getRoute() {
        val firstPoint = trip?.points?.first()
        val lastPoint = trip?.points?.last()
        val origin =
            firstPoint?.lng?.let { firstPoint?.lat?.let { it1 -> com.mapbox.geojson.Point.fromLngLat(it, it1) } }
        val destination =
            lastPoint?.lng?.let { lastPoint?.lat?.let { it1 -> com.mapbox.geojson.Point.fromLngLat(it, it1) } }

        origin?.let {
            destination?.let { it1 ->
                val builder = NavigationRoute.builder(this)
                    .accessToken(getString(R.string.map_token))
                    .origin(it)
                    .destination(it1)
                    .profile(DirectionsCriteria.PROFILE_DRIVING)

                trip?.points?.let {
                    for (point in it) {
                        if (point != firstPoint && point != lastPoint) {
                            point.lng?.let { it2 ->
                                point.lat?.let { it3 -> com.mapbox.geojson.Point.fromLngLat(it2, it3) }
                            }?.let { it3 ->
                                builder?.addWaypoint(it3)
                            }
                        }
                    }
                }
                builder?.build()?.getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        if (response.body() == null) {
                            startActivity(Intent(this@SplashActivity, MapActivity::class.java))
                            setResult(Activity.RESULT_OK)
                            finish()
                            return
                        } else if (response.body()?.routes()?.size!! < 1) {
                            startActivity(Intent(this@SplashActivity, MapActivity::class.java))
                            setResult(Activity.RESULT_OK)
                            finish()
                            return
                        }
                        val currentRoute: DirectionsRoute? = response.body()?.routes()?.first()
                        Thread {
                            runOnUiThread {
                                status?.text = getString(R.string.saving_data)
                            }
//                            GlobalData.directionsRoute = currentRoute
                            Util.saveRoute(this@SplashActivity, currentRoute)
                            Util.saveTrip(this@SplashActivity, trip)
                            startActivity(Intent(this@SplashActivity, MapActivity::class.java))
                            setResult(Activity.RESULT_OK)
                            finish()
                        }.start()
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                        startActivity(Intent(this@SplashActivity, MapActivity::class.java))
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                })
            }
        }
    }

    private fun makeDays() {
        trip?.days = ArrayList()
        trip?.points?.let {
            var dayNumber = 1
            val temp: ArrayList<Point> = ArrayList()
            var lastHotel:Point? = null
            for (point in it) {
                lastHotel?.let { it1 -> temp.add(it1) }
//                if (!trip?.days?.isNullOrEmpty()) {
//                    val tempHotel = trip?.days?.get(dayNumber - 2)?.points?.firstOrNull { p -> p.isHotel }
//                    if (tempHotel != null) {
//                        temp.add(tempHotel)
//                    }
//                }
                if (!point.isHotel && point != it.lastOrNull()) {
                    temp.add(point)
                } else {
                    lastHotel = point
                    temp.add(point)
                    val tempPoints: ArrayList<Point> = ArrayList(temp)
                    trip?.days?.add(Day("יום $dayNumber", tempPoints, false, null))
                    temp?.clear()
                    dayNumber++
                }
            }
        }
    }

    private fun getOutputMediaFile(): File? {
        val mediaStorageDir = File(
            Environment.getExternalStorageDirectory().toString()
                    + "/Android/data/"
                    + applicationContext.packageName
                    + "/Files"
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmssS", Locale.getDefault()).format(Date())
        val mediaFile: File
        val mImageName = "MI_$timeStamp.jpg"
        mediaFile = File(mediaStorageDir.path + File.separator + mImageName)
        return mediaFile
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
}