package com.gijun.lolml.extract

/** 읽기 전용 계정의 접속 정보. 코드와 저장소에 값을 적지 않는다. */
data class DbConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
) {
    companion object {
        /** `LOL_ML_DB_URL` · `LOL_ML_DB_USER` · `LOL_ML_DB_PASSWORD` */
        fun fromEnv(): DbConfig = TODO()
    }
}
