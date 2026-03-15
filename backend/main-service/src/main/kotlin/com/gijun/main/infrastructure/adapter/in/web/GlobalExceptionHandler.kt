package com.gijun.main.infrastructure.adapter.`in`.web

import com.gijun.common.exception.DomainAlreadyExistsException
import com.gijun.common.exception.DomainConflictException
import com.gijun.common.exception.DomainForbiddenException
import com.gijun.common.exception.DomainInvalidStateException
import com.gijun.common.exception.DomainNotFoundException
import com.gijun.common.exception.DomainUnauthorizedException
import com.gijun.common.exception.DomainValidationException
import com.gijun.common.response.CommonApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(DomainNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: DomainNotFoundException): CommonApiResponse<Nothing> =
        CommonApiResponse.error(e.message ?: "Resource not found", "NOT_FOUND")

    @ExceptionHandler(DomainValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(e: DomainValidationException): CommonApiResponse<Nothing> =
        CommonApiResponse.error(e.message ?: "Validation failed", "VALIDATION_ERROR")

    @ExceptionHandler(DomainAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleAlreadyExists(e: DomainAlreadyExistsException): CommonApiResponse<Nothing> =
        CommonApiResponse.error(e.message ?: "Resource already exists", "ALREADY_EXISTS")

    @ExceptionHandler(DomainConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(e: DomainConflictException): CommonApiResponse<Nothing> =
        CommonApiResponse.error(e.message ?: "Resource conflict", "CONFLICT")

    @ExceptionHandler(DomainForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbidden(e: DomainForbiddenException): CommonApiResponse<Nothing> =
        CommonApiResponse.error(e.message ?: "Access forbidden", "FORBIDDEN")

    @ExceptionHandler(DomainUnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(e: DomainUnauthorizedException): CommonApiResponse<Nothing> =
        CommonApiResponse.error(e.message ?: "Unauthorized access", "UNAUTHORIZED")

    @ExceptionHandler(DomainInvalidStateException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleInvalidState(e: DomainInvalidStateException): CommonApiResponse<Nothing> =
        CommonApiResponse.error(e.message ?: "Invalid state", "INVALID_STATE")

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneral(e: Exception): CommonApiResponse<Nothing> {
        log.error("Unhandled exception", e)
        return CommonApiResponse.error("Internal server error", "INTERNAL_ERROR")
    }
}
