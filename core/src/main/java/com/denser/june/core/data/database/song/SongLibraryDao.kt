package com.denser.june.core.data.database.song

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongLibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SongLibraryEntity)

    @Query("SELECT * FROM song_library WHERE contentHash = :hash")
    suspend fun getByHash(hash: String): SongLibraryEntity?

    @Query("SELECT * FROM song_library WHERE sourceUrl = :url LIMIT 1")
    suspend fun getBySourceUrl(url: String): SongLibraryEntity?

    @Query("SELECT * FROM song_library ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<SongLibraryEntity>>

    @Query("SELECT * FROM song_library ORDER BY addedAt DESC")
    suspend fun getAll(): List<SongLibraryEntity>

    @Query("DELETE FROM song_library WHERE contentHash = :hash")
    suspend fun deleteByHash(hash: String)

    @Query("UPDATE song_library SET title = :title, artistName = :artist, albumName = :album, genre = :genre, localArtPath = :localArtPath WHERE contentHash = :hash")
    suspend fun updateMetadata(
        hash: String,
        title: String,
        artist: String,
        album: String?,
        genre: String?,
        localArtPath: String?
    )
}
