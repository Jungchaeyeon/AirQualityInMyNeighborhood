package com.jcy.ch21_airpollutioninourneighborhood

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.jcy.ch21_airpollutioninourneighborhood.data.Repository
import com.jcy.ch21_airpollutioninourneighborhood.data.models.airquality.Grade
import com.jcy.ch21_airpollutioninourneighborhood.data.models.airquality.MeasuredValue
import com.jcy.ch21_airpollutioninourneighborhood.data.models.monitoringstation.MonitoringStation
import com.jcy.ch21_airpollutioninourneighborhood.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var cancellationTokenSource:CancellationTokenSource?= null
    private val binding by lazy{ActivityMainBinding.inflate(layoutInflater)}
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        bindViews()
        initVariables()
        requestLocationPermissions()
    }

    private fun initVariables(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }
    private fun requestLocationPermissions(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_ACCESS_LOCATION_PERMISSIONS
        )
    }
    private fun requestBackgroundLocationPermissions(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val locationPermissionGranted =
            requestCode == REQUEST_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        val backgroundLocationPermissionGranted =
            requestCode == REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if(!backgroundLocationPermissionGranted){
                requestBackgroundLocationPermissions() //권한이 없을 경우 백그라운드 퍼미션 요청
            }else{
                fetchAirQualityData()
            }
        }else{
            if(!locationPermissionGranted){
                finish()
            }else{ //권한이 있을 때
                fetchAirQualityData()
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun fetchAirQualityData(){
        //fetchData
        cancellationTokenSource = CancellationTokenSource()

        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource!!.token
        ).addOnSuccessListener {location->
           scope.launch {//코루틴 돌리기
               binding.errorDescriptionTv.visibility = View.GONE
               try {
                   val monitoringStation =
                       Repository.getNearbyMonitoringStation(location.latitude, location.longitude)
                   val measuredValue =
                       Repository.getLatestAirQualityData(monitoringStation!!.stationName!!)
                   displayAirQualityData(monitoringStation, measuredValue!!)
               }catch (exception:Exception){
                   binding.errorDescriptionTv.visibility = View.VISIBLE
                   binding.contentsLayout.alpha =0F
               }
               finally {
                   binding.progressBar.visibility = View.GONE
                   binding.refresh.isRefreshing = false
               }
           }
        }
    }

    private fun bindViews(){
        binding.refresh.setOnRefreshListener {
            fetchAirQualityData()
        }
    }
    @SuppressLint("SetTextI18n")
    fun displayAirQualityData(monitoringStation: MonitoringStation, measuredValue: MeasuredValue){

        binding.contentsLayout.animate()
            .alpha(1F)
            .start()

        binding.measuringStationNameTv.text = monitoringStation.stationName
        binding.measuringStationAddressTv.text = monitoringStation.addr
        (measuredValue.khaiGrade ?: Grade.UNKNOWN).let { grade ->
            binding.root.setBackgroundColor(grade.colorResId)
            binding.totalGradeEmojiTv.text = grade.emoji
            binding.totalGradeLabelTv.text = grade.label
        }
        with(measuredValue){
            binding.fineDustInfoTv.text =
                "미세먼지: $pm10Value  ㎍/㎥ ${(pm10Grade ?: Grade.UNKNOWN).emoji}"
            binding.ultraFineDustInfoTv.text =
                "초미세먼지;: $pm25Value ㎍/㎥ ${(pm25Grade ?:Grade.UNKNOWN).emoji}"
            with(binding.so2Item){
                labelTv.text ="아황산가스"
                gradeTv.text=(so2Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$so2Value ppm"
            }
            with(binding.coItem){
                labelTv.text ="일산화탄소"
                gradeTv.text=(coGrade ?: Grade.UNKNOWN).toString()
                valueTextView.text =  "$coValue ppm"
            }
            with(binding.o3Item){
                labelTv.text ="오존"
                gradeTv.text=(o3Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text =  "$o3Value ppm"
            }
            with(binding.no2Item){
                labelTv.text ="이산화질소"
                gradeTv.text=(no2Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text =  "$no2Value ppm"
            }

        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSource?.cancel()
        scope.cancel()
    }

    companion object{
        private const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 100
        private const val REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS = 101
    }
}