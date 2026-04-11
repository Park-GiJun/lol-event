package com.gijun.main.application.port.out

interface StatsCachePort {
    fun <T> getOrCompute(key: String, compute: () -> T): T
    fun evictAll()
    fun evictByPrefix(prefix: String)
}
