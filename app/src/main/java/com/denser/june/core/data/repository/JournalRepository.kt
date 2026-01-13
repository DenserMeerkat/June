package com.denser.june.core.data.repository

import com.denser.june.core.data.database.journal.JournalDao
import com.denser.june.core.data.mappers.toEntity
import com.denser.june.core.data.mappers.toJournal
import com.denser.june.core.domain.data_classes.Journal
import com.denser.june.core.domain.JournalRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class JournalRepository(
    private val localDao: JournalDao
) : JournalRepo {

    override suspend fun insertJournal(journal: Journal): Long {
        return withContext(Dispatchers.IO) {
            localDao.insertJournal(journal.toEntity())
        }
    }

    override fun getJournals(): Flow<List<Journal>> {
        return localDao.getAllJournals().map { entities ->
            entities.map { it.toJournal() }
        }
    }

    override suspend fun getAllJournals(): List<Journal> {
        return localDao.getAllJournals().first().map { it.toJournal() }
    }

    override suspend fun getJournalById(id: Long): Journal? {
        return withContext(Dispatchers.IO) {
            localDao.getJournalById(id)?.toJournal()
        }
    }

    override suspend fun getLatestJournal(): Journal? {
        return withContext(Dispatchers.IO) {
            localDao.getLatestJournal()?.toJournal()
        }
    }

    override suspend fun searchJournals(query: String): Flow<List<Journal>> {
        return localDao.searchJournal(query).map { entities ->
            entities.map { it.toJournal() }
        }
    }

    override suspend fun updateJournal(journal: Journal) {
        withContext(Dispatchers.IO) {
            localDao.updateJournal(journal.toEntity())
        }
    }

    override suspend fun deleteJournal(id: Long) {
        withContext(Dispatchers.IO) {
            localDao.deleteJournal(id)
        }
    }

    override suspend fun deleteAllJournals() {
        withContext(Dispatchers.IO) {
            localDao.deleteAllJournals()
        }
    }

    override fun getJournalsByDateRange(startDate: Long, endDate: Long): Flow<List<Journal>> {
        return localDao.getJournalsByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toJournal() }
        }
    }

    override fun getFilteredJournals(
        query: String,
        isBookmarked: Boolean?,
        isDraft: Boolean?,
        hasLocation: Boolean?,
        hasSong: Boolean?
    ): Flow<List<Journal>> {
        return localDao.getJournals(
            query,
            isBookmarked,
            isDraft,
            hasLocation,
            hasSong
        ).map { entities ->
            entities.map { it.toJournal() }
        }
    }
}
