package net.abaresults.progresspath.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    val url: String
)
