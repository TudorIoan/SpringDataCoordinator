package net.abaresults.progresspath.module

import net.abaresults.progresspath.api.PhotosService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkModule {
    @Singleton
    @Provides
    fun providePhotosService(): PhotosService {
        return PhotosService.create()
    }
}