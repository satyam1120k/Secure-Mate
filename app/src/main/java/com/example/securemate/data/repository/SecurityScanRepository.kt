package com.example.securemate.data.repository

import com.example.securemate.data.local.SecurityScanDao
import com.example.securemate.data.local.SecurityScanEntity
import com.example.securemate.domain.model.SecurityScanRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SecurityScanRepository {
    fun getAllScans(): Flow<List<SecurityScanRecord>>
    fun getLatestScan(): Flow<SecurityScanRecord?>
    suspend fun saveScan(record: SecurityScanRecord): Long
    suspend fun clearAllScans()
}

class SecurityScanRepositoryImpl(
    private val dao: SecurityScanDao
) : SecurityScanRepository {

    override fun getAllScans(): Flow<List<SecurityScanRecord>> {
        return dao.getAllScans().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLatestScan(): Flow<SecurityScanRecord?> {
        return dao.getLatestScan().map { it?.toDomain() }
    }

    override suspend fun saveScan(record: SecurityScanRecord): Long {
        return dao.insertScan(SecurityScanEntity.fromDomain(record))
    }

    override suspend fun clearAllScans() {
        dao.clearAllScans()
    }
}
