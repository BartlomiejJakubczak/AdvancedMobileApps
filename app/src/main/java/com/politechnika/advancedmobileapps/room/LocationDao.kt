package com.politechnika.advancedmobileapps.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocationDao {

    @Insert
    fun insertAll(vararg locations: Location)

    @Query("SELECT * FROM location")
    fun getAll(): List<Location>

}