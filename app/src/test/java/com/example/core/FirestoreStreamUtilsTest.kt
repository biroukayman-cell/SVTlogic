package com.example.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreStreamUtilsTest {

    @Test
    fun `catchPreservingLast keeps cached emission when upstream fails afterwards`() = runTest(UnconfinedTestDispatcher()) {
        var reportedError: Throwable? = null
        val stream = flow {
            emit(listOf("2Bac SVT 1", "1Bac SE 2"))
            throw IllegalStateException("PERMISSION_DENIED")
        }.catchPreservingLast(
            onError = { reportedError = it }
        )

        val stateFlow = stream.stateIn(backgroundScope, SharingStarted.Eagerly, emptyList())
        assertEquals(listOf("2Bac SVT 1", "1Bac SE 2"), stateFlow.value)
        assertEquals("PERMISSION_DENIED", reportedError?.message)
    }

    @Test
    fun `catchPreservingLast keeps previously loaded state across retry failure`() = runTest(UnconfinedTestDispatcher()) {
        val retryTrigger = MutableStateFlow(0)
        var attempts = 0

        @Suppress("OPT_IN_USAGE")
        val stateFlow = retryTrigger.flatMapLatest {
            attempts += 1
            if (attempts == 1) {
                flow { emit(listOf("Class A")) }
            } else {
                flow<List<String>> { throw IllegalStateException("UNAVAILABLE") }
            }
        }.catchPreservingLast(
            onError = {}
        ).stateIn(backgroundScope, SharingStarted.Eagerly, emptyList())

        assertEquals(listOf("Class A"), stateFlow.value)
        retryTrigger.value = 1
        assertEquals(listOf("Class A"), stateFlow.value)
    }

    @Test
    fun `catchPreservingLast rethrows pure CancellationException`() = runTest(UnconfinedTestDispatcher()) {
        var errorCalled = false
        var caughtCancellation = false
        val stream = flow<List<String>> {
            throw CancellationException("coroutine cancelled")
        }.catchPreservingLast(
            isNonCancellableFirestoreError = { false },
            onError = { errorCalled = true }
        )

        try {
            stream.toList()
        } catch (_: CancellationException) {
            caughtCancellation = true
        }
        assertTrue(caughtCancellation)
        assertEquals(false, errorCalled)
    }
}
