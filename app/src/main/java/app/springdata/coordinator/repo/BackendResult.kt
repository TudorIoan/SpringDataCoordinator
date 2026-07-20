package app.springdata.coordinator.repo

typealias BackendCallback<T> = (BackendResult<T>) -> Unit

sealed class BackendResult<T> {
    class Success<T>(val data: T) : BackendResult<T>()
    class Error<T>(
        val code: Int,
        val message: String,
        val type: String = ""
    ) : BackendResult<T>()
}

typealias ProgressCallback = (progress: Int) -> Unit