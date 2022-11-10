package com.ojhdtapp.parabox.extension.auto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ojhdtapp.parabox.extension.auto.domain.model.AppModel
import com.ojhdtapp.parabox.extension.auto.domain.model.AppModelDisabledUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface AppModelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appModel: AppModel): Long

    @Query("SELECT * FROM appmodel WHERE packageName = :packageName LIMIT 1")
    suspend fun queryByPackageName(packageName: String): AppModel?

    @Update(entity = AppModel::class)
    fun updateDisabledState(appModelDisabledUpdate: AppModelDisabledUpdate)

    @Query("SELECT * FROM appmodel")
    fun getAll(): Flow<List<AppModel>>
}