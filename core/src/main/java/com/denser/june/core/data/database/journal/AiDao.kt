package com.denser.june.core.data.database.journal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImprovement(improvement: ImprovementEntity)

    @Update
    suspend fun updateImprovement(improvement: ImprovementEntity)

    @Query("SELECT * FROM improvements ORDER BY createdAt DESC")
    fun getAllImprovements(): Flow<List<ImprovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMicroWin(microWin: MicroWinEntity)

    @Query("SELECT * FROM micro_wins ORDER BY createdAt DESC")
    fun getAllMicroWins(): Flow<List<MicroWinEntity>>
}
