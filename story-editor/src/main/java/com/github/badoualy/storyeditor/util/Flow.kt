package com.github.badoualy.storyeditor.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal fun <T> Flow<T>.withPrevious(): Flow<Pair<T?, T>> {
    return flow {
        var previousValue: T? = null
        collect { value ->
            emit(previousValue to value)
            previousValue = value
        }
    }
}
