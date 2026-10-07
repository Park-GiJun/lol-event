package com.gijun.lolml.data

import java.nio.file.Path

/** 스냅샷 파일의 형식을 아는 곳은 이 파일뿐이다. 쓰는 쪽과 읽는 쪽이 같은 컬럼 순서를 본다. */
object SnapshotFormat {
    val DEFAULT_PATH: Path = Path.of("data", "matches.csv")
}

class SnapshotWriter(
    private val path: Path,
) {
    /** 받은 순서 그대로 쓴다. 정렬은 SQL 이 이미 했다. */
    fun write(matches: Sequence<Match>): Unit = TODO()
}

class SnapshotReader(
    private val path: Path,
) {
    /** 파일에 적힌 순서(= 시간순)대로 돌려준다. */
    fun read(): List<Match> = TODO()
}
