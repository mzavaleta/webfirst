package io.github.mzavaleta.webfirst.core

import java.net.HttpURLConnection
import java.net.URL

/**
 * Follows redirect chains manually.
 *
 * Redirects are followed by hand (`instanceFollowRedirects = false`) so the final
 * URL can be inspected before anything is opened, and so that a broken redirect
 * chain cannot bounce the caller in a loop.
 *
 * Every failure mode degrades to returning [resolve]'s input: an unreachable
 * network must not block the user from opening the link.
 *
 * [log] is injected rather than using `android.util.Log`, so this class stays
 * usable — and testable — on a plain JVM.
 */
class HttpRedirectResolver(
    private val maxHops: Int = DEFAULT_MAX_HOPS,
    private val connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val log: (String) -> Unit = {},
) : RedirectResolver {

    override fun resolve(url: String): String {
        var connection: HttpURLConnection? = null
        try {
            var current = URL(url)
            var hops = 0

            while (hops < maxHops) {
                connection = (current.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                }

                val code = connection.responseCode
                log("hop $hops: $current -> $code")
                if (code !in REDIRECT_CODES) return current.toExternalForm()

                val location = connection.getHeaderField(LOCATION_HEADER)
                log("hop $hops location: $location")
                if (location.isNullOrBlank()) return current.toExternalForm()

                current = URL(current, location)
                hops++
                connection.disconnect()
            }

            return current.toExternalForm()
        } catch (error: Exception) {
            log("resolution aborted: ${error.javaClass.simpleName}: ${error.message}")
            return url
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val DEFAULT_MAX_HOPS = 5
        const val DEFAULT_TIMEOUT_MS = 3_000
        const val LOCATION_HEADER = "Location"
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
