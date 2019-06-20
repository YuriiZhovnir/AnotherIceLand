package jdroidcoder.ua.anothericeland.network.response

import com.google.gson.annotations.SerializedName
import com.mapbox.api.directions.v5.models.DirectionsRoute
import java.io.Serializable

class Trip(@SerializedName("name") var name: String?, @SerializedName("description") var description: String?,
           @SerializedName("image") var image: String?, @SerializedName("points") var points: ArrayList<Point>?,
           var days: ArrayList<Day> = ArrayList()) : Serializable

class Point(@SerializedName("type_id") var typeId: Int?, @SerializedName("name") var name: String?,
            @SerializedName("description") var description: String?, @SerializedName("image") var image: String?,
            @SerializedName("phone") var phone: String?, @SerializedName("lat") var lat: Double?,
            @SerializedName("lng") var lng: Double?, var isHotel: Boolean = false, var isDone: Boolean = false) : Serializable

class Day(var name: String, var points: ArrayList<Point> = ArrayList(), var isDone: Boolean, var direction: DirectionsRoute?) : Serializable