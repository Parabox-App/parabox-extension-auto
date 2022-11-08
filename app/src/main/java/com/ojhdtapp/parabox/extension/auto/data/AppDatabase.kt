package com.ojhdtapp.parabox.extension.auto.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ojhdtapp.parabox.extension.auto.domain.model.WxContact

@Database(entities = [WxContact::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val wxContactDao: WxContactDao
}