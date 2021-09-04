package com.jcy.ch21_airpollutioninourneighborhood.data.services

import com.jcy.ch21_airpollutioninourneighborhood.BuildConfig
import com.jcy.ch21_airpollutioninourneighborhood.data.models.airquality.AirQualityResponse
import com.jcy.ch21_airpollutioninourneighborhood.data.models.monitoringstation.MonitoringStationsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AirKoreaApiService {

    @GET("B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList" +
        "?serviceKey=${BuildConfig.AIRKOREA_SERVICE_KEY}" +
        "&returnType=json")
    suspend fun getNearByMonitoringStation(
        @Query("tmX") tmX : Double,
        @Query("tmY") tmY: Double
    ): Response<MonitoringStationsResponse>

    @GET("B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty"+
        "?serviceKey=${BuildConfig.AIRKOREA_SERVICE_KEY}" +
        "&returnType=json" +
        "&dataTerm=DAILY" +
        "&ver=1.3")
    suspend fun getRealtimeAirQualities(
        @Query("stationName") stationName:String
    ): Response<AirQualityResponse>
}