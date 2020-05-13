package com.politechnika.advancedmobileapps

import android.Manifest
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.politechnika.advancedmobileapps.location.LocationUpdatesService
import com.politechnika.advancedmobileapps.logger.LogFragment
import com.politechnika.advancedmobileapps.permissions.PermissionRationalActivity
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private lateinit var mActivityTransitionsPendingIntent: PendingIntent
    private lateinit var mTransitionsReceiver : TransitionsReceiver
    private lateinit var mLogFragment: LogFragment
    private var mService: LocationUpdatesService? = null
    private lateinit var transitionsReceiverAction : String
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    private var mBound: Boolean = false
    private var activityTrackingEnabled : Boolean = false
    private var broadcastRegistered : Boolean = false

    private val activityTransitionList = listOf(
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()
    )

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBound = false
            Log.d(TAG, "Service was disconnected.")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: LocationUpdatesService.LocalBinder = service as LocationUpdatesService.LocalBinder
            mService = binder.getService()
            mBound = true
            Log.d(TAG, "Service was bound.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called.")
        super.onCreate(savedInstanceState)
        setViews()
        mTransitionsReceiver = TransitionsReceiver()
        transitionsReceiverAction =
            applicationContext.packageName + ".TRANSITIONS_RECEIVER_ACTION"
        mActivityTransitionsPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(transitionsReceiverAction),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (SharedPrefsStorage.requestingLocationUpdates(this)) {
            if (!checkPermissions()) {
                Log.d(TAG, "Permissions not enabled, requesting...")
                requestPermissions()
            }
        }
        registerBroadcast()
        printToScreen("App initialized.")
    }

    private fun registerBroadcast() {
        val locationIntentFilter = IntentFilter()
        val transitionIntentFilter = IntentFilter()
        transitionIntentFilter.addAction(transitionsReceiverAction)
        locationIntentFilter.addAction(LocationUpdatesService.ACTION_BROADCAST)
        LocalBroadcastManager.getInstance(this).registerReceiver(mTransitionsReceiver, locationIntentFilter)
        registerReceiver(mTransitionsReceiver, transitionIntentFilter)
        printToScreen("Broadcasts registered.")
        broadcastRegistered = true
    }

    private fun unregisterBroadcast() {
        unregisterReceiver(mTransitionsReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTransitionsReceiver)
        broadcastRegistered = false
    }

    private fun setViews() {
        setContentView(R.layout.activity_main)
        val toolbar : Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        mLogFragment = (supportFragmentManager.findFragmentById(R.id.log_fragment)) as LogFragment
    }

    override fun onStart() {
        Log.d(TAG, "onStart called.")
        super.onStart()
        Log.d(TAG, "Binding the service.")
        bindService(
            Intent(this, LocationUpdatesService::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        Log.d(TAG, "onResume called.")
        super.onResume()
        if (!broadcastRegistered) {
            Log.d(TAG, "Registering broadcast receiver.")
            registerBroadcast()
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause called.")
        if (activityTrackingEnabled) {
            disableActivityTransitions()
        }
        unregisterBroadcast()
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop called.")
        if (mBound) {
            Log.d(TAG, "Unbinding the service.")
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
    }

    private fun activityRecognitionPermissionApproved() : Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            true
        }
    }

    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestPermissions() {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        if (shouldProvideRationale) {
            Snackbar.make(
                findViewById(R.id.activity_main),
                R.string.permission_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok
                ) { ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE
                ) }.show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    printToScreen("Permissions not granted. Location will not be gathered.")
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    mService?.requestLocationUpdates()
                    printToScreen("Requesting location updates: " + SharedPrefsStorage.requestingLocationUpdates(this).toString())
                }
                else -> {
                    Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings) {
                            // displays settings
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri: Uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }.show()
                }
            }
        }
    }

    fun onClickSwitchActivityRecognition(view : View) {
        if (activityRecognitionPermissionApproved()) {
            if (activityTrackingEnabled) {
                disableActivityTransitions()
            } else {
                enableActivityTransitions()
            }
        } else {
            startActivity(Intent(this, PermissionRationalActivity::class.java))
        }
    }

    fun onClickSwitchLocationRecognition(view: View) {
        if (!SharedPrefsStorage.requestingLocationUpdates(this)) {
            if (!checkPermissions()) {
                requestPermissions()
            } else {
                mService?.requestLocationUpdates()
                printToScreen("Requesting location updates: " + SharedPrefsStorage.requestingLocationUpdates(this).toString())
            }
        } else {
            mService?.removeLocationUpdates()
            printToScreen("Requesting location updates: " + SharedPrefsStorage.requestingLocationUpdates(this).toString())
        }
    }

    fun onClickClearLogs(view : View) {
        mLogFragment.getLogView().clearLogs()
    }

    private fun enableActivityTransitions() {
        val request = ActivityTransitionRequest(activityTransitionList)
        ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent)
            .addOnSuccessListener {
                activityTrackingEnabled = true
                printToScreen("Transitions Api was successfully registered.")
            }
            .addOnFailureListener {
                printToScreen("Transitions Api could NOT be registered: $it")
            }
    }

    private fun disableActivityTransitions() {
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mActivityTransitionsPendingIntent)
            .addOnSuccessListener {
                activityTrackingEnabled = false
                printToScreen("Transitions successfully unregistered.")
            }
            .addOnFailureListener {
                printToScreen("Transitions could not be unregistered: $it")
            }
    }

    private fun printToScreen(text : String) {
        mLogFragment.getLogView().println(text)
    }

    inner class TransitionsReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Received broadcast: " + intent?.action)
            if (intent?.action.equals(transitionsReceiverAction)) {
                if (!TextUtils.equals(transitionsReceiverAction, intent?.action)) {
                    printToScreen(
                        "Received an unsupported action in TransitionsReceiver: action = " + intent?.action
                    )
                }
                if (ActivityTransitionResult.hasResult(intent)) {
                    val result = ActivityTransitionResult.extractResult(intent)!!
                    for (event in result.transitionEvents) {
                        val info =
                            "Transition: " + toActivityString(event.activityType) +
                                    " (" + toTransitionType(event.transitionType) + ")" + "   " +
                                    SimpleDateFormat("HH:mm:ss", Locale.US)
                                        .format(Date())
                        printToScreen(info)
                    }
                }
            } else if (intent?.action.equals(LocationUpdatesService.ACTION_BROADCAST)) {
                printToScreen("Receiving location.")
                val location: Location? = intent?.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION)
                if (location != null) {
                    printToScreen(LocationUpdatesService.getLocationText(location))
                }
            }
        }

        private fun toActivityString(activity: Int): String? {
            return when (activity) {
                DetectedActivity.STILL -> "STILL"
                DetectedActivity.WALKING -> "WALKING"
                DetectedActivity.IN_VEHICLE -> "IN VEHICLE"
                else -> "UNKNOWN"
            }
        }

        private fun toTransitionType(transitionType: Int): String? {
            return when (transitionType) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
                else -> "UNKNOWN"
            }
        }

    }

}
