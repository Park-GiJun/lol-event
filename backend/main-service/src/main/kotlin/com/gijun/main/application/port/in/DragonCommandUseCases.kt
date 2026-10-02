package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.DragonSyncResult

interface SyncDataDragonUseCase {
    /** 최신 버전을 Data Dragon 에서 받아 DB 에 저장하고 캐시를 갈아 끼운다. */
    fun syncDataDragon(): DragonSyncResult
}

interface InitializeDataDragonUseCase {
    /**
     * 기동 시 한 번 부른다. DB 에 이미 데이터가 있으면 캐시만 채우고 null 을 돌려준다.
     * 처음 기동이라 비어 있으면 동기화하고 그 결과를 돌려준다.
     */
    fun initializeDataDragon(): DragonSyncResult?
}
