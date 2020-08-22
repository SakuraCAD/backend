package app.sakuracad.util

class SecureException(
    private val replace: String,
    private val inner: Throwable
) : Exception(inner.message?.replace(replace, "<censored>"))