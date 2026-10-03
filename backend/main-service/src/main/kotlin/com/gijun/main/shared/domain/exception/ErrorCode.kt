package com.gijun.main.shared.domain.exception

/**
 * **에러 코드의 정본이다.** 서버가 내려보내는 모든 `errorCode` 는 여기에 있다.
 *
 * 문자열 리터럴로 쓰지 않는 이유는 정본이 갈라지기 때문이다. 예전에는 코드가
 * `GlobalExceptionHandler` 안의 리터럴이었고, 화면은 어떤 코드가 오는지 목록을 알 방법이 없어
 * `errorCode` 를 받고도 버렸다. enum 으로 두면 오타가 컴파일되지 않고 전수 열거가 가능하다.
 *
 * **HTTP status 를 여기 두지 않는다.** status 는 [DomainException] 의 카테고리가 정하고, 한
 * 카테고리에 코드가 여럿 붙는다. 여기에 또 적으면 이중 정본이 된다.
 *
 * ### 추가하는 법
 *
 * 아래에 `CODE_NAME("한 줄 설명"),` 을 추가한다 — 이 형태를 지켜야 화면 쪽 생성기가 읽는다.
 */
enum class ErrorCode(
    val description: String,
) {
    // ── 공통 · 프레임워크 ───────────────────────────────────────────────────
    // 어느 MVC 모듈에서든 프레임워크가 낼 수 있는 것들이다. 이름을 바꾸면 ErrorCodeTest 가 red 다.
    NOT_FOUND("대상이 없다"),
    CONFLICT("지금 상태에서는 할 수 없다"),
    VALIDATION_FAILED("요청 형식이 올바르지 않다"),
    MISSING_PARAMETER("필수 파라미터가 없다"),
    MALFORMED_BODY("요청 본문을 읽을 수 없다"),
    METHOD_NOT_ALLOWED("허용되지 않은 메서드다"),
    UNSUPPORTED_MEDIA_TYPE("지원하지 않는 Content-Type 이다"),
    TYPE_MISMATCH("파라미터 형식이 올바르지 않다"),
    INTERNAL_ERROR("서버 오류가 발생했다"),

    // ── Spring Security ─────────────────────────────────────────────────────
    ACCESS_DENIED("접근 권한이 없다"),

    // ── 멤버 · 라이엇 계정 ──────────────────────────────────────────────────
    DUPLICATE_MEMBER("이미 등록된 멤버다"),
    RIOT_API_KEY_EXPIRED("Riot API 키가 만료됐다"),

    // ── 세션 ────────────────────────────────────────────────────────────────
    INVALID_SESSION_DATE("세션 날짜 형식이 올바르지 않다"),

    // ── 입력 형식 ───────────────────────────────────────────────────────────
    INVALID_IDENTIFIER("식별자 형식이 올바르지 않다"),
    INVALID_GAME_MODE("경기 모드 값이 올바르지 않다"),

    // ── RAG ─────────────────────────────────────────────────────────────────
    RAG_UNAVAILABLE("LLM 서버에 닿지 못했다"),
    RAG_BUSY("다른 질문을 처리하는 중이다"),

    // ── 팀 편성 ─────────────────────────────────────────────────────────────
    INVALID_TEAM_BUILD("팀 편성 조건이 올바르지 않다"),
    ;

    companion object {
        /** 프레임워크가 내는 공통 코드. `ErrorCodeTest` 가 이 목록으로 이름 드리프트를 검사한다. */
        val COMMON: List<ErrorCode> =
            listOf(
                NOT_FOUND,
                CONFLICT,
                VALIDATION_FAILED,
                MISSING_PARAMETER,
                MALFORMED_BODY,
                METHOD_NOT_ALLOWED,
                UNSUPPORTED_MEDIA_TYPE,
                TYPE_MISMATCH,
                INTERNAL_ERROR,
            )
    }
}
