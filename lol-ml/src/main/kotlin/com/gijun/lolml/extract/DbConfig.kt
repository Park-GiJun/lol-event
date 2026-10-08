package com.gijun.lolml.extract

/** 읽기 전용 계정의 접속 정보. 코드와 저장소에 값을 적지 않는다. */
data class DbConfig(
    val jdbcUrl: String,
    val user: String,
    val password: String,
) {
    companion object {
        /** `LOL_ML_DB_URL` · `LOL_ML_DB_USER` · `LOL_ML_DB_PASSWORD` */
        fun fromEnv(): DbConfig =
            DbConfig(
                jdbcUrl = env("LOL_ML_DB_URL"),
                user = env("LOL_ML_DB_USER"),
                password = env("LOL_ML_DB_PASSWORD"),
            )

        private fun env(name: String): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: error("환경변수 $name 이 없다")
    }
}
