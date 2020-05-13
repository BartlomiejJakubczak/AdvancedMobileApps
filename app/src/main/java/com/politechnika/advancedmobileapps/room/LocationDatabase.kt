package com.politechnika.advancedmobileapps.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Location::class], version = 1, exportSchema = false)
abstract class LocationDatabase: RoomDatabase() {
    abstract fun locationDao(): LocationDao
}