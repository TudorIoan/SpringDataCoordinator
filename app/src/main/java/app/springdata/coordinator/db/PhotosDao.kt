package app.springdata.coordinator.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotosDao {
    @Query("SELECT * FROM photos ORDER BY id")
    fun getPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :photoId")
    fun getPhoto(photoId: String): PhotoEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)
}