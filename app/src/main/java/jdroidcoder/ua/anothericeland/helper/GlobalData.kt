package jdroidcoder.ua.anothericeland.helper

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.annotations.Marker
import jdroidcoder.ua.anothericeland.network.response.Day
import jdroidcoder.ua.anothericeland.network.response.Point
import jdroidcoder.ua.anothericeland.network.response.Trip

object GlobalData {
    var trip: Trip? = null
    var selectedMarker: Marker? = null
    var number: String? = ""
    var password: String? = ""
    var directionsRoute: DirectionsRoute? = null
    var currentDay:Day? = null
    var selectedPoint:Point? = null
}