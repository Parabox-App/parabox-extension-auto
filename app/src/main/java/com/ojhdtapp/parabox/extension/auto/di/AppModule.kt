package com.ojhdtapp.parabox.extension.auto.di

import android.app.Application
import androidx.room.Room
import com.ojhdtapp.parabox.extension.auto.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideDatabase(app: Application): AppDatabase =
        Room.databaseBuilder(
            app, AppDatabase::class.java, "main_db"
        ).build()
}