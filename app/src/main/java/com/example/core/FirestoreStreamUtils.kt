package com.example.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Collects [this] flow and forwards every emitted item. If an exception is thrown
 * (for instance after Firestore emits local cached documents and then fails to reach
 * the server or hits a permission error), [onError] is invoked and the stream ends
 * cleanly WITHOUT emitting an empty default value that would wipe cached UI state.
 */
fun <T> Flow<T>.catchPreservingLast(
    isNonCancellableFirestoreError: (Throwable) -> Boolean = { false },
    onError: suspend (Throwable) -> Unit
): Flow<T> = flow {
    try {
        collect { value ->
            emit(value)
        }
    } catch (error: Throwable) {
        if (error is CancellationException && !isNonCancellableFirestoreError(error)) {
            throw error
        }
        onError(error)
    }
}
