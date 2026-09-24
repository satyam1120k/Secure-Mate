package com.example.securemate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityScanDao {
    @Query("SELECT * FROM security_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<SecurityScanEntity>>

    @Query("SELECT * FROM security_scans ORDER BY timestamp DESC LIMIT 1")
    fun getLatestScan(): Flow<SecurityScanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: SecurityScanEntity): Long

    @Query("DELETE FROM security_scans")
    suspend fun clearAllScans()

    @Query("SELECT COUNT(*) FROM security_scans")
    suspend fun getScanCount(): Int
}
