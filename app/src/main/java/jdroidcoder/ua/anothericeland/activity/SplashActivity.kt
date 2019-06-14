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
import android.os.Environment
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import jdroidcoder.ua.anothericeland.helper.GlobalData
import jdroidcoder.ua.anothericeland.helper.Util
import jdroidcoder.ua.anothericeland.network.response.Day
import jdroidcoder.ua.anothericeland.network.response.Point
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

class SplashActivity : BaseActivity() {
    private var frameAnimation: AnimationDrawable? = null
    private var imageDownloadedCount = 0
    val images: ArrayList<String> = ArrayList()
    lateinit var trip: Trip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
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
                == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            download()
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    43)
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
        apiService?.getTrip(GlobalData?.number, GlobalData?.password)
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
                        for (image in images) {
                            downloadImage(image)
                        }
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                })
    }

    private fun downloadImage(imageUrl: String) {
        Picasso.get()
                .load(imageUrl)
                .into(object : Target {
                    override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
                        imageDownloadedCount++
                        imageDownloaded("", "")
                    }

                    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                        try {
                            val pictureFile = getOutputMediaFile() ?: return
                            try {
                                val fos = FileOutputStream(pictureFile)
                                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
                                fos.close()
                                imageDownloaded(imageUrl, pictureFile?.absolutePath)
                            } catch (e: FileNotFoundException) {
                                imageDownloadedCount++
                                imageDownloaded("", "")
                            } catch (e: IOException) {
                                imageDownloadedCount++
                                imageDownloaded("", "")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            imageDownloadedCount++
                            imageDownloaded("", "")
                        }
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

                    }
                })
    }

    private fun imageDownloaded(imageUrl: String, localPath: String) {
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
            Util.saveTrip(this, trip)
            GlobalData.trip = trip
//            hideSpinner()
//            startActivity(Intent(this, MapActivity::class.java))
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun makeDays() {
        trip?.days = ArrayList()
        trip?.points?.let {
            var dayNumber = 1
            val temp: ArrayList<Point> = ArrayList()
            for (point in it) {
                if (!point.isHotel) {
                    temp.add(point)
                } else {
                    temp.add(point)
                    val tempPoints: ArrayList<Point> = ArrayList(temp)
                    trip?.days?.add(Day("Day $dayNumber", tempPoints, false))
                    temp?.clear()
                    dayNumber++
                }
            }
        }
    }

    private fun getOutputMediaFile(): File? {
        val mediaStorageDir = File(Environment.getExternalStorageDirectory().toString()
                + "/Android/data/"
                + applicationContext.packageName
                + "/Files")
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
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
            window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
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