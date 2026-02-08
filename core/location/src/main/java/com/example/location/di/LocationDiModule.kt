package com.example.location.di

import com.example.location.api.LocationClient
import com.example.location.api.LocationClientImpl
import com.example.location.config.MotionConfig
import com.example.location.data.dataSource.GpsLocationDataSource
import com.example.location.data.dataSource.RawLocationDataSource
import com.example.location.data.repository.LocationRepositoryImpl
import com.example.location.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton



@Module
@InstallIn(SingletonComponent::class)
abstract class LocationBindings {

    @Binds
    @Singleton
    abstract fun bindLocationClient(impl: LocationClientImpl): LocationClient

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindRawLocationDataSource(impl: GpsLocationDataSource): RawLocationDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object LocationProvides {

    @Provides
    @Singleton
    fun provideMotionConfig(): MotionConfig = MotionConfig()
}

