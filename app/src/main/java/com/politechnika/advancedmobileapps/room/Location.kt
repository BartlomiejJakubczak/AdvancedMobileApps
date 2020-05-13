package com.politechnika.advancedmobileapps.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Location (
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
    @ColumnInfo(name = "timestamp") val timestamp: Long
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}