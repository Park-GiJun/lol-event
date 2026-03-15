package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.dragon.result.DragonSyncResult

interface SyncDataDragonUseCase {
    fun sync(): DragonSyncResult
}
