package jdroidcoder.ua.anothericeland.network.response

import com.google.gson.annotations.SerializedName

class Trip(@SerializedName("name") var name: String?, @SerializedName("description") var description: String?,
           @SerializedName("image") var image: String?, @SerializedName("points") var points: ArrayList<Point>?,
           var days: ArrayList<Day> = ArrayList())

class Point(@SerializedName("type_id") var typeId: Int?, @SerializedName("name") var name: String?,
            @SerializedName("description") var description: String?, @SerializedName("image") var image: String?,
            @SerializedName("phone") var phone: String?, @SerializedName("lat") var lat: Double?,
            @SerializedName("lng") var lng: Double?, var isHotel: Boolean = false, var isDone: Boolean = false)

class Day(var name: String, var points: ArrayList<Point> = ArrayList(), var isDone: Boolean)