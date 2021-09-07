package com.jcy.ch21_airpollutioninourneighborhood.appwidget

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.jcy.ch21_airpollutioninourneighborhood.R
import com.jcy.ch21_airpollutioninourneighborhood.data.Repository
import com.jcy.ch21_airpollutioninourneighborhood.data.models.airquality.Grade
import kotlinx.coroutines.launch

class AirQualitySimpleWidgetProvider : AppWidgetProvider(){
    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ContextCompat.startForegroundService(
            context!!,
            Intent(context,UpdateWidgetService::class.java)
        )
    }
    class UpdateWidgetService : LifecycleService(){
        override fun onCreate() {
            super.onCreate()

            createChannelIfNeeded()
            startForeground(
                NOTIFICATION_ID,
                creteNotification()
            )
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            //가장먼저 해야할 것 : 서비스가 시작되면 위치정보를 가져와야 함
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val updateViews = RemoteViews(packageName,R.layout.widget_simple).apply{
                    setTextViewText(
                        R.id.resultTv,
                        "권한 없음"
                    )
                    setViewVisibility(R.id.labelTv, View.GONE)
                    setViewVisibility(R.id.gradeLabelTv, View.GONE)
                }
                updateWidget(updateViews)
                stopSelf() //권한이 없으면 remote뷰에 권한이 없음을 보여주고 service종료
                return super.onStartCommand(intent, flags, startId)
            }
            LocationServices.getFusedLocationProviderClient(this).lastLocation
                .addOnSuccessListener { location->
                    try {
                    lifecycleScope.launch{
                        val nearbyMonitoringStation = Repository.getNearbyMonitoringStation(
                            location.latitude,
                            location.longitude)
                        val measuredValue = Repository.getLatestAirQualityData(nearbyMonitoringStation!!.stationName!!)
                        val updateViews = RemoteViews(packageName, R.layout.widget_simple).apply{
                            setViewVisibility(R.id.labelTv, View.VISIBLE)
                            setViewVisibility(R.id.gradeLabelTv, View.VISIBLE)

                            val currentGrade = (measuredValue?.khaiGrade ?: Grade.UNKNOWN)
                            setTextViewText(R.id.resultTv, currentGrade.emoji)
                            setTextViewText(R.id.gradeLabelTv, currentGrade.label)
                        }
                        updateWidget(updateViews)

                    }
                    }catch (exception: Exception){
                        exception.printStackTrace()
                    }finally{
                        stopSelf()
                        }
                }
            return super.onStartCommand(intent, flags, startId)
        }
        fun createChannelIfNeeded(){
            if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O){
                //api가 26보다 클 때 채널을 만들어야한다.
                (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                    ?.createNotificationChannel(
                        NotificationChannel(
                            WIDGET_REFRESH_CHANNEL_ID,
                            "위젯 갱신 채널",
                            NotificationManager.IMPORTANCE_LOW
                        )
                    )
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            stopForeground(true)
        }
        private fun updateWidget(updateViews: RemoteViews){
            val widgetProvider = ComponentName(this, AirQualitySimpleWidgetProvider::class.java)
            AppWidgetManager.getInstance(this).updateAppWidget(widgetProvider, updateViews)
        }
        private fun creteNotification(): Notification =
            NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_baseline_refresh_24)
                .setChannelId(WIDGET_REFRESH_CHANNEL_ID)
                .build()
    }
    companion object {
        private const val WIDGET_REFRESH_CHANNEL_ID ="WIDGET_REFRESH_CHANNEL_ID"
        private const val NOTIFICATION_ID=2021
    }
}