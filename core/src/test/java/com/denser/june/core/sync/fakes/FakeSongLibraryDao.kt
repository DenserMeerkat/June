package com.denser.june.core.sync.fakes

import com.denser.june.core.data.database.song.SongLibraryDao
import com.denser.june.core.data.database.song.SongLibraryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeSongLibraryDao : SongLibraryDao {
    private val entries = mutableMapOf<String, SongLibraryEntity>()

    override suspend fun upsert(entry: SongLibraryEntity) {
        entries[entry.contentHash] = entry
    }

    override suspend fun getByHash(hash: String): SongLibraryEntity? = entries[hash]

    override suspend fun getBySourceUrl(url: String): SongLibraryEntity? = entries.values.find { it.sourceUrl == url }

    override fun observeAll(): Flow<List<SongLibraryEntity>> = flowOf(entries.values.toList())

    override suspend fun getAll(): List<SongLibraryEntity> = entries.values.toList()

    override suspend fun deleteByHash(hash: String) {
        entries.remove(hash)
    }

    override suspend fun updateMetadata(
        hash: String,
        title: String,
        artist: String,
        album: String?,
        genre: String?,
        localArtPath: String?
    ) {
        val existing = entries[hash] ?: return
        entries[hash] = existing.copy(
            title = title,
            artistName = artist,
            albumName = album,
            genre = genre,
            localArtPath = localArtPath
        )
    }

    fun clear() {
        entries.clear()
    }
}
