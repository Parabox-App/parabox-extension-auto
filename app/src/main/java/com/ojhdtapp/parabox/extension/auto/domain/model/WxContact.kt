package com.ojhdtapp.parabox.extension.auto.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WxContact(
    val name: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)