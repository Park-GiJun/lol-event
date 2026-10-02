package com.gijun.main.application.port.out.cache

interface StatsResultCacheQueryPort {
    fun <T> getOrCompute(
        key: String,
        compute: () -> T,
    ): T
}

interface StatsResultCacheCommandPort {
    fun evictAll()

    fun evictByPrefix(prefix: String)
}
