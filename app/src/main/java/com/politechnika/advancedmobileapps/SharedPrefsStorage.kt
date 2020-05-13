package com.politechnika.advancedmobileapps

import android.content.Context
import androidx.preference.PreferenceManager

class SharedPrefsStorage {

    companion object {
        val KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates"

        fun requestingLocationUpdates(context: Context?): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false)
        }

        fun setRequestingLocationUpdates(
            context: Context?,
            requestingLocationUpdates: Boolean
        ) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply()
        }
    }

}