package com.gijun.common.exception

open class DomainNotFoundException(message: String = "Resource not found") : RuntimeException(message)

open class DomainValidationException(message: String = "Validation failed") : RuntimeException(message)

open class DomainAlreadyExistsException(message: String = "Resource already exists") : RuntimeException(message)

open class DomainConflictException(message: String = "Resource conflict") : RuntimeException(message)

open class DomainForbiddenException(message: String = "Access forbidden") : RuntimeException(message)

open class DomainUnauthorizedException(message: String = "Unauthorized access") : RuntimeException(message)

open class DomainInvalidStateException(message: String = "Invalid state") : RuntimeException(message)
