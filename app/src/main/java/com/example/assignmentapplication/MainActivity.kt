
package com.example.assignmentapplication

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings.*
import android.view.View
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.Debug.getLocation
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.concurrent.timer
import android.annotation.SuppressLint as SuppressLint1


class MainActivity : AppCompatActivity() {
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val permissionId = 2
    private var videoView: VideoView? = null

    private var mediaControls: MediaController? = null

    @SuppressLint1("HardwareIds", "SetTextI18n", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        videoView = findViewById<View>(R.id.videoView) as VideoView

        if (mediaControls == null) {
            // Creating an object of media controller class
            mediaControls = MediaController(this)

            // set the anchor view for the video view
            mediaControls!!.setAnchorView(this.videoView)
        }
        // set the absolute pah of the video file which is going to be played
        videoView!!.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.mobile_a_shooting_star))
        videoView!!.requestFocus()


        // Starting the video
        videoView!!.setOnPreparedListener { mp ->
            mp.isLooping = true
        }

        videoView!!.start()

        // FOR SETTING UP IMEI
        val tx = findViewById<View>(R.id.eDTtvIMEI) as TextView
        val deviceID = Secure.getString(
            this.contentResolver,
            Secure.ANDROID_ID
        )
        tx.text = deviceID.toString()

        //Connection Status
        val connectionStatus = findViewById<View>(R.id.tvconnectionStatus) as TextView
        if (checkForInternet(this)){
            connectionStatus.text = "ON"
            connectionStatus.setTextColor(Color.GREEN)
        }else{
            connectionStatus.text = "OFF"
            connectionStatus.setTextColor(Color.RED)
        }

        // for battery information
        //call battery manager service
        val batteryStat = findViewById<View>(R.id.tvBatteryStatus) as TextView
        val batteryPercnt = findViewById<View>(R.id.tvbatteryPercentage) as TextView
        val bm = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager

        // get battery percentage
        val batLevel: Int = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        for (i in batLevel..100){
        batteryPercnt.text = "$batLevel%"
        }

        //battery charging info
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

            if (isCharging){
                if (batLevel<101){
                    batteryStat.text = "Charging :"
                    batteryStat.setTextColor(Color.GREEN)
                    batteryPercnt.setTextColor(Color.GREEN)
                    batteryPercnt.text = "$batLevel%"
                }
                Toast.makeText(applicationContext, "Charging", Toast.LENGTH_LONG).show()
            }else{
                batteryStat.setTextColor(Color.WHITE)
                batteryPercnt.setTextColor(Color.YELLOW)
                Toast.makeText(applicationContext,"Not Charging", Toast.LENGTH_LONG).show()
            }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location : Location? = task.result
                    if (location != null) {
                        val myloc = findViewById<TextView>(R.id.tvLocationCity)
                        myloc.text = "Latitude :" + location.latitude + "  " + "Longitude:" + location.longitude
                        myloc.setTextColor(Color.YELLOW)
                    }
                }
            } else {
                Toast.makeText(this, "Please turn on the location", Toast.LENGTH_SHORT).show()
                val intent = Intent(ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }

        val timestamp = findViewById<TextView>(R.id.tvCurrentTimestamp)
        timestamp.text = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss:SSSSSS")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())

//        if(DateTimeFormatter.ISO_TIME.format(Instant.now()) == (DateTimeFormatter.ISO_TIME.format(Instant.now().plusSeconds(900)))){
//            val mintent = intent
//            finish()
//            startActivity(mintent)
//        }

    }

    override fun onPause() {
        super.onPause()
        videoView!!.stopPlayback()
    }

    override fun onResume() {
        super.onResume()
        videoView!!.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView!!.stopPlayback()
    }



//function for checking internet connectivity and connection type
    private fun checkForInternet(context: Context): Boolean {
            // register activity with connection manager service
             val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // if the android version is equal to M or greater we need to use the NetworkCapabilities to check what type of network has the internet connection
    // return the network object corresponding to currently active default data network
    val network = connectivityManager.activeNetwork ?: return false
    //representation of the capabilities of an active network.
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

    return when {
        //indicates the network has wifi
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

        //indicate cellular network
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

        else -> false
    }
}

    // functions for permissions and location
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }
    @SuppressLint1("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }
}