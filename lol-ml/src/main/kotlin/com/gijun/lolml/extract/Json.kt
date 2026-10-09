package com.gijun.lolml.extract

/**
 * JSON 글을 `Map` · `List` · `String` · `Double` · `Boolean` · `null` 로 읽는다.
 * Riot API 응답을 읽는 데 필요한 만큼만 한다 — 의존성을 늘리지 않으려고 직접 둔다.
 */
class Json private constructor(
    private val text: String,
) {
    private var at = 0

    private fun value(): Any? {
        skipSpace()
        return when (peek()) {
            '{' -> obj()
            '[' -> array()
            '"' -> string()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> number()
        }
    }

    private fun obj(): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        at++
        skipSpace()
        if (peek() == '}') {
            at++
            return result
        }
        while (true) {
            skipSpace()
            val key = string()
            skipSpace()
            expect(':')
            result[key] = value()
            skipSpace()
            if (peek() == ',') at++ else break
        }
        expect('}')
        return result
    }

    private fun array(): List<Any?> {
        val result = ArrayList<Any?>()
        at++
        skipSpace()
        if (peek() == ']') {
            at++
            return result
        }
        while (true) {
            result.add(value())
            skipSpace()
            if (peek() == ',') at++ else break
        }
        expect(']')
        return result
    }

    private fun string(): String {
        expect('"')
        val out = StringBuilder()
        while (true) {
            when (val c = next()) {
                '"' -> return out.toString()
                '\\' -> out.append(escaped())
                else -> out.append(c)
            }
        }
    }

    private fun escaped(): Char =
        when (val c = next()) {
            'n' -> {
                '\n'
            }

            't' -> {
                '\t'
            }

            'r' -> {
                '\r'
            }

            'b' -> {
                '\b'
            }

            'f' -> {
                '\u000C'
            }

            'u' -> {
                text
                    .substring(at, at + 4)
                    .toInt(16)
                    .toChar()
                    .also { at += 4 }
            }

            else -> {
                c
            }
        }

    private fun number(): Double {
        val start = at
        while (at < text.length && text[at] in NUMBER_CHARS) at++
        require(at > start) { "JSON 을 읽을 수 없다 ($start 번째 글자): ${text.take(80)}" }
        return text.substring(start, at).toDouble()
    }

    private fun literal(
        word: String,
        value: Any?,
    ): Any? {
        require(text.startsWith(word, at)) { "JSON 을 읽을 수 없다 ($at 번째 글자): ${text.take(80)}" }
        at += word.length
        return value
    }

    private fun skipSpace() {
        while (at < text.length && text[at].isWhitespace()) at++
    }

    private fun peek(): Char = if (at < text.length) text[at] else error("JSON 이 중간에 끊겼다: ${text.take(80)}")

    private fun next(): Char = peek().also { at++ }

    private fun expect(c: Char) = require(next() == c) { "JSON 의 ${at - 1} 번째 글자가 '$c' 가 아니다: ${text.take(80)}" }

    companion object {
        private const val NUMBER_CHARS = "+-.eE0123456789"

        fun parse(text: String): Any? {
            val json = Json(text)
            val value = json.value()
            json.skipSpace()
            require(json.at == text.length) { "JSON 뒤에 남는 글자가 있다: ${text.take(80)}" }
            return value
        }
    }
}
