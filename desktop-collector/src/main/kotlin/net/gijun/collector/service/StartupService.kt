package net.gijun.collector.service

import java.io.File

/**
 * Windows 시작 프로그램 등록/해제.
 * 레지스트리 HKCU\Software\Microsoft\Windows\CurrentVersion\Run 을 사용.
 */
object StartupService {

    private const val APP_NAME = "LoL 수집기"
    private const val REG_KEY = """HKCU\Software\Microsoft\Windows\CurrentVersion\Run"""

    /**
     * 현재 실행 파일 경로를 반환. 패키징된 EXE일 때만 유효.
     */
    private fun getExePath(): String? {
        val command = ProcessHandle.current().info().command().orElse(null) ?: return null
        // .exe로 실행 중인 경우만 등록 가능 (gradlew run 등 개발 환경 제외)
        if (!command.endsWith(".exe", ignoreCase = true)) return null
        // java.exe / javaw.exe인 경우도 제외
        val name = File(command).name.lowercase()
        if (name == "java.exe" || name == "javaw.exe") return null
        return command
    }

    fun isRegistered(): Boolean {
        return try {
            val process = ProcessBuilder(
                "reg", "query", REG_KEY, "/v", APP_NAME
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            process.exitValue() == 0 && output.contains(APP_NAME)
        } catch (_: Exception) {
            false
        }
    }

    fun register(): Boolean {
        val exePath = getExePath()
        if (exePath == null) {
            // 개발 환경에서는 등록 불가 — 패키징된 앱에서만 동작
            return false
        }
        return try {
            val process = ProcessBuilder(
                "reg", "add", REG_KEY,
                "/v", APP_NAME,
                "/t", "REG_SZ",
                "/d", "\"$exePath\"",
                "/f"
            ).redirectErrorStream(true).start()
            process.waitFor()
            process.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun unregister(): Boolean {
        return try {
            val process = ProcessBuilder(
                "reg", "delete", REG_KEY,
                "/v", APP_NAME,
                "/f"
            ).redirectErrorStream(true).start()
            process.waitFor()
            process.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun toggle(): Boolean {
        return if (isRegistered()) {
            unregister()
            false
        } else {
            register()
            true
        }
    }

    /**
     * 패키징된 앱에서 실행 중인지 확인.
     * 개발 환경(java.exe)에서는 시작 프로그램 등록 메뉴를 숨기는 데 사용.
     */
    fun isPackagedApp(): Boolean = getExePath() != null
}
