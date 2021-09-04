package com.jcy.ch21_airpollutioninourneighborhood

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.jcy.ch21_airpollutioninourneighborhood.data.Repository
import com.jcy.ch21_airpollutioninourneighborhood.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var cancellationTokenSource:CancellationTokenSource?= null
    private val binding by lazy{ActivityMainBinding.inflate(layoutInflater)}
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        requestLocationPermissions()
        initVariables()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val locationPermissionGranted =
            requestCode == REQUEST_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        if(!locationPermissionGranted){
            finish()
        }else{ //권한이 있을 때
            fetchAirQualityData()
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
                val monitoringStation = Repository.getNearbyMonitoringStation(location.latitude, location.longitude)

               Log.e("monitoringStation", monitoringStation.toString())
                val measuredValue = Repository.getLatestAirQualityData(monitoringStation!!.stationName!!)

               binding.textView.text= measuredValue.toString()
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
    }
}