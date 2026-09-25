package com.denser.june.core.sync.fakes

import com.denser.june.core.data.database.song.SongLibraryDao
import com.denser.june.core.data.database.song.SongLibraryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSongLibraryDao : SongLibraryDao {
    private val entries = mutableMapOf<String, SongLibraryEntity>()
    private val _songsFlow = MutableStateFlow<List<SongLibraryEntity>>(emptyList())

    private fun updateFlow() {
        _songsFlow.value = entries.values.toList()
    }

    override suspend fun upsert(entry: SongLibraryEntity) {
        entries[entry.contentHash] = entry
        updateFlow()
    }

    override suspend fun getByHash(hash: String): SongLibraryEntity? = entries[hash]

    override suspend fun getBySourceUrl(url: String): SongLibraryEntity? = entries.values.find { it.sourceUrl == url }

    override suspend fun getByTitleAndArtist(title: String, artist: String): SongLibraryEntity? =
        entries.values.find {
            it.title.trim().equals(title.trim(), ignoreCase = true) &&
            it.artistName.trim().equals(artist.trim(), ignoreCase = true)
        }

    override fun observeAll(): Flow<List<SongLibraryEntity>> = _songsFlow

    override suspend fun getAll(): List<SongLibraryEntity> = entries.values.toList()

    override suspend fun deleteByHash(hash: String) {
        entries.remove(hash)
        updateFlow()
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
        updateFlow()
    }

    fun clear() {
        entries.clear()
        updateFlow()
    }
}
