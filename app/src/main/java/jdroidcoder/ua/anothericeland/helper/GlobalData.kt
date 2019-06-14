package jdroidcoder.ua.anothericeland.helper

import com.mapbox.mapboxsdk.annotations.Marker
import jdroidcoder.ua.anothericeland.network.response.Trip

object GlobalData {
    var trip: Trip? = null
    var selectedMarker: Marker? = null
    var number: String? = ""
    var password: String? = ""
}