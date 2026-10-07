package com.gijun.lolml

import kotlin.system.exitProcess

/**
 * 추출과 학습은 따로 돈다. 학습은 DB 를 모르고 스냅샷 파일만 읽는다.
 *
 *   ./gradlew run --args="extract"   운영 DB → data/ 스냅샷
 *   ./gradlew run --args="train"     스냅샷 → 피처 → 학습 → 평가
 */
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "extract" -> {
            runExtract()
        }

        "train" -> {
            runTrain()
        }

        else -> {
            System.err.println("usage: extract | train")
            exitProcess(1)
        }
    }
}

private fun runExtract(): Unit = TODO("DbConfig.fromEnv → MatchExtractor → SnapshotWriter")

private fun runTrain(): Unit = TODO("SnapshotReader → MatchCleaner → FeatureBuilder → timeSplit → Standardizer → LogisticRegression → eval")
