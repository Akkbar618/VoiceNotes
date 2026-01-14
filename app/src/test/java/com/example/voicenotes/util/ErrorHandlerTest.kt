package com.example.voicenotes.util

import com.example.voicenotes.ai.AiProvider
import com.example.voicenotes.ai.MissingApiKeyException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for ErrorHandler.
 * Tests error type conversion from exceptions.
 */
class ErrorHandlerTest {

    @Test
    fun `fromException returns NoInternet for UnknownHostException`() {
        val exception = UnknownHostException("Unable to resolve host")
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.NoInternet)
    }

    @Test
    fun `fromException returns Timeout for SocketTimeoutException`() {
        val exception = SocketTimeoutException("Connection timed out")
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.Timeout)
    }

    @Test
    fun `fromException returns Timeout for IOException with timeout message`() {
        val exception = IOException("Socket timeout occurred")
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.Timeout)
    }

    @Test
    fun `fromException returns NoInternet for generic IOException`() {
        val exception = IOException("Network unreachable")
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.NoInternet)
    }

    @Test
    fun `fromException returns ApiUnauthorized for 401 error`() {
        val exception = RuntimeException("HTTP 401 Unauthorized")
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.ApiUnauthorized)
    }

    @Test
    fun `fromException returns ApiRateLimit for 429 error`() {
        val exception = RuntimeException("HTTP 429 Too Many Requests")
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.ApiRateLimit)
    }

    @Test
    fun `fromException returns ApiServerError for 500 error`() {
        val exception = RuntimeException("HTTP 500 Internal Server Error")
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.ApiServerError)
    }

    @Test
    fun `fromException returns ApiServerError for 503 error`() {
        val exception = RuntimeException("HTTP 503 Service Unavailable")
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.ApiServerError)
    }

    @Test
    fun `fromException returns ApiKeyMissing for MissingApiKeyException with GEMINI`() {
        val exception = MissingApiKeyException(AiProvider.GEMINI)
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.ApiKeyMissing)
        assertEquals(AiProvider.GEMINI, (result as AppError.ApiKeyMissing).provider)
    }

    @Test
    fun `fromException returns ApiKeyMissing for MissingApiKeyException with OPENAI`() {
        val exception = MissingApiKeyException(AiProvider.OPENAI)
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.ApiKeyMissing)
        assertEquals(AiProvider.OPENAI, (result as AppError.ApiKeyMissing).provider)
    }

    @Test
    fun `fromException returns Unknown for generic exception`() {
        val exception = IllegalArgumentException("Something went wrong")
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.Unknown)
        assertEquals("Something went wrong", (result as AppError.Unknown).message)
    }

    @Test
    fun `fromException handles null message gracefully`() {
        val exception = NullPointerException()
        val result = ErrorHandler.fromException(exception)
        assertTrue(result is AppError.Unknown)
    }
}
