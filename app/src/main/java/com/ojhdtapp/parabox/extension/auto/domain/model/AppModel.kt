package com.ojhdtapp.parabox.extension.auto.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AppModel(
    val packageName: String,
    val appName: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val disabled: Boolean = false,
)

data class AppModelDisabledUpdate(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "disabled") val disabled: Boolean
)