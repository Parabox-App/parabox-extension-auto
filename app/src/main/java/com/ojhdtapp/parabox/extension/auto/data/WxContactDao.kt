package com.ojhdtapp.parabox.extension.auto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ojhdtapp.parabox.extension.auto.domain.model.WxContact

@Dao
interface WxContactDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(wxContact: WxContact): Long

    @Query("SELECT * FROM wxcontact WHERE name = :name LIMIT 1")
    fun queryByName(name: String): WxContact?
}