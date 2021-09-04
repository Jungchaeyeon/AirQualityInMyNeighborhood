package com.jcy.ch21_airpollutioninourneighborhood.data

import android.util.Log
import com.jcy.ch21_airpollutioninourneighborhood.BuildConfig
import com.jcy.ch21_airpollutioninourneighborhood.data.models.airquality.MeasuredValue
import com.jcy.ch21_airpollutioninourneighborhood.data.models.monitoringstation.MonitoringStation
import com.jcy.ch21_airpollutioninourneighborhood.data.services.AirKoreaApiService
import com.jcy.ch21_airpollutioninourneighborhood.data.services.KakaoLocalApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object Repository {

    suspend fun getNearbyMonitoringStation(latitude: Double, longitude: Double): MonitoringStation?{
        val tmCoordinates = kakaoLocalApiService
            .getTmCoordinates(longitude, latitude)
            .body()
            ?.documents
            ?.firstOrNull()

        val tmX = tmCoordinates?.x
        val tmY = tmCoordinates?.y

        Log.e("latitude", "$latitude/$longitude")
        Log.e("tmCoordinates", tmCoordinates.toString())

        return airKoreaApiService
            .getNearByMonitoringStation(tmX!!,tmY!!)
            .body()
            ?.response
            ?.body
            ?.monitoringStations
            ?.minByOrNull { it.tm ?:Double.MAX_VALUE } //null인값은 자동으로 후순위로 밀리도록
    }

    suspend fun getLatestAirQualityData(stationName: String): MeasuredValue?=
        airKoreaApiService
            .getRealtimeAirQualities(stationName)
            .body()
            ?.response
            ?.body
            ?.measuredValues
            ?.firstOrNull()

    private  val kakaoLocalApiService:KakaoLocalApiService by lazy{
        Retrofit.Builder()
            .baseUrl(Url.KAKAO_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(buildHttpClient())
            .build()
            .create()
    }
    private  val airKoreaApiService:AirKoreaApiService by lazy{
        Retrofit.Builder()
            .baseUrl(Url.AIRKOREA_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(buildHttpClient())
            .build()
            .create()
    }

    private fun buildHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if(BuildConfig.DEBUG){
                        HttpLoggingInterceptor.Level.BODY
                    }else{
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            ).build()
}