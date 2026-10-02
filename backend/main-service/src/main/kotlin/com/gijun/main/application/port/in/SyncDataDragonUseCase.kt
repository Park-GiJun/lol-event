package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.DragonSyncResult

interface SyncDataDragonUseCase {
    fun sync(): DragonSyncResult
}
