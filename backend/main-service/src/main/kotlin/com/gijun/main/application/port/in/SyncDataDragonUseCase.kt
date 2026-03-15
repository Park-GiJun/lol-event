package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.DragonSyncResponse

interface SyncDataDragonUseCase {
    fun sync(): DragonSyncResponse
}
