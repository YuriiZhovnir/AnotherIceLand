package jdroidcoder.ua.anothericeland.network

import jdroidcoder.ua.anothericeland.network.response.Trip
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import rx.Observable

interface Api {
    @POST("test")
    @FormUrlEncoded
    fun getTrip(@Field("booking_number") bookingNumber: String?, @Field("password") password: String?): Observable<Trip>
}