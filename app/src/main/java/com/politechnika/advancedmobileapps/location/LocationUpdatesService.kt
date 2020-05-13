package com.politechnika.advancedmobileapps.location

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.gms.location.*
import com.politechnika.advancedmobileapps.MainActivity
import com.politechnika.advancedmobileapps.R
import com.politechnika.advancedmobileapps.SharedPrefsStorage
import com.politechnika.advancedmobileapps.room.LocationDatabase
import java.text.DateFormat
import java.util.*


class LocationUpdatesService : Service() {

    companion object {
        private val PACKAGE_NAME: String? = "com.politechnika.advancedmobileapps.location"
        val ACTION_BROADCAST: String? = PACKAGE_NAME + ".broadcast"
        val EXTRA_LOCATION: String? = PACKAGE_NAME + ".location"
        fun getLocationText(location: Location?): String {
            return if (location == null) "Unknown location" else "(" + location.latitude
                .toString() + ", " + location.longitude.toString() + ")"
        }
        fun getLocationTitle(context: Context): String {
            return context.getString(
                R.string.location_updated,
                DateFormat.getDateTimeInstance().format(Date())
            )
        }
    }
    private val NOTIFICATION_ID: Int = 2137
    private val EXTRA_STARTED_FROM_NOTIFICATION: String? = PACKAGE_NAME + ".started_from_notification"
    private val CHANNEL_ID: String? = "channel_01"
    private val TAG = LocationUpdatesService::class.java.simpleName
    private var mChangingConfiguration: Boolean = false

    private var mLocation: Location? = null

    private val mBinder: IBinder = LocalBinder()
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mServiceHandler: Handler
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mDatabase: LocationDatabase

    override fun onBind(intent: Intent?): IBinder? {
        stopForeground(true)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind called.")
        stopForeground(true)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind called.")
        if (!mChangingConfiguration && SharedPrefsStorage.requestingLocationUpdates(this)) {
            startForeground(NOTIFICATION_ID, getNotification())
        }
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called.")
        mServiceHandler.removeCallbacksAndMessages(null)
        mDatabase.close()
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate called.")
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }
        mDatabase = Room.databaseBuilder(
            applicationContext,
            LocationDatabase::class.java,
            "db"
        ).build()
        createLocationRequest()
        getLastLocation()
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            // Create the channel for the notification
            val mChannel =
                NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH)
            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startedFromNotification = intent!!.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)
        if (startedFromNotification) {
            removeLocationUpdates()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    fun requestLocationUpdates() {
        SharedPrefsStorage.setRequestingLocationUpdates(this, true)
        startService(Intent(applicationContext, LocationUpdatesService::class.java))
        //TODO opcjonalny try-catch z permissionami
        Log.d(TAG, "Requesting location updates.")
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
            mLocationCallback, Looper.myLooper())
    }

    fun removeLocationUpdates() {
        //TODO opcjonalny try-catch Lost location permission. Could not remove updates
        Log.d(TAG, "Removing location updates.")
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        SharedPrefsStorage.setRequestingLocationUpdates(this, false)
        stopSelf()
    }

    private fun onNewLocation(location: Location) {
        mLocation = location
        val locationEntity = com.politechnika.advancedmobileapps.room.Location(mLocation?.latitude, mLocation?.longitude, System.currentTimeMillis()/1000)
        Thread {
            mDatabase.locationDao().insertAll(locationEntity)
        }.start()
        // Notify anyone listening for broadcasts about the new location.
        val intent = Intent(ACTION_BROADCAST)
        intent.putExtra(EXTRA_LOCATION, location)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification())
        }
    }

    private fun getNotification(): Notification {
        val intent = Intent(this, LocationUpdatesService::class.java)
        val text: CharSequence = getLocationText(mLocation)
        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
        // The PendingIntent that leads to a call to onStartCommand() in this service.
        val servicePendingIntent = PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // The PendingIntent to launch activity.
        val mMainActivityIntent = Intent(this, MainActivity::class.java)
        intent.action = Intent.ACTION_MAIN;
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0,
            mMainActivityIntent, 0
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID!!)
            .addAction(
                R.drawable.ic_launch, getString(R.string.launch_activity),
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_cancel, getString(R.string.remove_location_updates),
                servicePendingIntent
            )
            .setContentText(text)
            .setContentTitle(getLocationTitle(this))
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(text)
            .setWhen(System.currentTimeMillis())

        return builder.build()
    }

    private fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }

    private fun getLastLocation() {
        //TODO opcjonalny try-catch do tracenia pozwolen
        mFusedLocationClient.lastLocation
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    Log.d(TAG, "Receiving new location: " + mLocation.toString())
                    mLocation = task.result
                } else {
                    //TODO moze wyswietlenie ze cos poszlo nie tak?
                }
            }
    }

    private fun createLocationRequest() {
        //TODO logika przekazywania interwalow w bundle, na razie domyslne zeby dzialalo
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 15 * 1000 //milisekundy
        mLocationRequest.fastestInterval = mLocationRequest.interval / 2
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    inner class LocalBinder : Binder() {
        fun getService() : LocationUpdatesService {
            return this@LocationUpdatesService
        }
    }

}