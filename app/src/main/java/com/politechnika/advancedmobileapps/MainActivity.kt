package com.politechnika.advancedmobileapps

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.politechnika.advancedmobileapps.logger.LogFragment
import com.politechnika.advancedmobileapps.permissions.PermissionRationalActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mActivityTransitionsPendingIntent: PendingIntent
    private lateinit var mTransitionsReceiver : TransitionsReceiver
    private lateinit var mLogFragment: LogFragment
    private lateinit var transitionsReceiverAction : String
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setViews()
        transitionsReceiverAction = applicationContext.packageName + "TRANSITIONS_RECEIVER_ACTION"
        mActivityTransitionsPendingIntent = PendingIntent.getBroadcast(this, 0, Intent(transitionsReceiverAction), PendingIntent.FLAG_UPDATE_CURRENT)
        mTransitionsReceiver = TransitionsReceiver()
        registerBroadcast()
        printToScreen("App initialized.")
    }

    private fun registerBroadcast() {
        registerReceiver(mTransitionsReceiver, IntentFilter(transitionsReceiverAction))
        broadcastRegistered = true
    }

    private fun unregisterBroadcast() {
        unregisterReceiver(mTransitionsReceiver)
        broadcastRegistered = false
    }

    private fun setViews() {
        setContentView(R.layout.activity_main)
        val toolbar : Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        mLogFragment = (supportFragmentManager.findFragmentById(R.id.log_fragment)) as LogFragment
    }

    override fun onResume() {
        super.onResume()
        if (!broadcastRegistered) {
            registerBroadcast()
        }
    }

    override fun onPause() {
        super.onPause()
        if (activityTrackingEnabled) {
            disableActivityTransitions()
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterBroadcast()
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
            if (!TextUtils.equals(transitionsReceiverAction, intent?.action)) {
                printToScreen(
                    "Received an unsupported action in TransitionsReceiver: action = " + intent!!.action
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
