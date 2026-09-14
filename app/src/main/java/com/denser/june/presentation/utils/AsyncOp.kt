package com.denser.june.presentation.utils

sealed interface AsyncOp<out T> {
    data object Idle : AsyncOp<Nothing>
    data object Loading : AsyncOp<Nothing>
    data class Success<T>(val data: T) : AsyncOp<T>
    data class Error(val message: String? = null, val cause: Throwable? = null) : AsyncOp<Nothing>

    val isLoading: Boolean get() = this is Loading
}

fun <T> Result<T>.toAsyncOp(): AsyncOp<T> = fold(
    onSuccess = { AsyncOp.Success(it) },
    onFailure = { AsyncOp.Error(message = it.message, cause = it) }
)
