package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.exception.DomainAlreadyExistsException
import com.gijun.common.exception.DomainConflictException
import com.gijun.common.exception.DomainForbiddenException
import com.gijun.common.exception.DomainInvalidStateException
import com.gijun.common.exception.DomainNotFoundException
import com.gijun.common.exception.DomainUnauthorizedException
import com.gijun.common.exception.DomainValidationException
import com.gijun.common.response.CommonApiResponse
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(private val meterRegistry: MeterRegistry) {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(DomainNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: DomainNotFoundException): CommonApiResponse<Nothing> {
        countError("NOT_FOUND")
        return CommonApiResponse.error(e.message ?: "Resource not found", "NOT_FOUND")
    }

    @ExceptionHandler(DomainValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(e: DomainValidationException): CommonApiResponse<Nothing> {
        countError("VALIDATION_ERROR")
        return CommonApiResponse.error(e.message ?: "Validation failed", "VALIDATION_ERROR")
    }

    @ExceptionHandler(DomainAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleAlreadyExists(e: DomainAlreadyExistsException): CommonApiResponse<Nothing> {
        countError("ALREADY_EXISTS")
        return CommonApiResponse.error(e.message ?: "Resource already exists", "ALREADY_EXISTS")
    }

    @ExceptionHandler(DomainConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(e: DomainConflictException): CommonApiResponse<Nothing> {
        countError("CONFLICT")
        return CommonApiResponse.error(e.message ?: "Resource conflict", "CONFLICT")
    }

    @ExceptionHandler(DomainForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbidden(e: DomainForbiddenException): CommonApiResponse<Nothing> {
        countError("FORBIDDEN")
        return CommonApiResponse.error(e.message ?: "Access forbidden", "FORBIDDEN")
    }

    @ExceptionHandler(DomainUnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(e: DomainUnauthorizedException): CommonApiResponse<Nothing> {
        countError("UNAUTHORIZED")
        return CommonApiResponse.error(e.message ?: "Unauthorized access", "UNAUTHORIZED")
    }

    @ExceptionHandler(DomainInvalidStateException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleInvalidState(e: DomainInvalidStateException): CommonApiResponse<Nothing> {
        countError("INVALID_STATE")
        return CommonApiResponse.error(e.message ?: "Invalid state", "INVALID_STATE")
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneral(e: Exception): CommonApiResponse<Nothing> {
        countError("INTERNAL_ERROR")
        log.error("[{}] Unhandled exception: {}", MDC.get("traceId"), e.message, e)
        return CommonApiResponse.error("Internal server error", "INTERNAL_ERROR")
    }

    private fun countError(type: String) {
        meterRegistry.counter("app.errors", "type", type).increment()
    }
}
