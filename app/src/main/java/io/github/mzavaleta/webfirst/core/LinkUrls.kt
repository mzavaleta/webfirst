package io.github.mzavaleta.webfirst.core

/**
 * Minimal, dependency-free URL helpers.
 *
 * [java.net.URI] and [java.net.URL] are deliberately avoided: both reject or
 * mangle URLs that browsers accept, and cleaning only needs the host, the path
 * and the position of the first query or fragment delimiter.
 */
object LinkUrls {

    private const val SCHEME_SEPARATOR = "://"

    /**
     * Returns the lower-case host, or `null` when [url] has no `scheme://authority`
     * part. Ports, credentials and IPv6 brackets are not preserved.
     */
    fun hostOf(url: String): String? {
        val schemeEnd = url.indexOf(SCHEME_SEPARATOR)
        if (schemeEnd < 0) return null

        val authorityStart = schemeEnd + SCHEME_SEPARATOR.length
        val authorityEnd = firstIndexOfAnyAfter(url, authorityStart, "/?#")
        val authority = url.substring(authorityStart, if (authorityEnd < 0) url.length else authorityEnd)
        val host = authority.substringAfterLast('@').substringBefore(':')

        return host.lowercase().ifEmpty { null }
    }

    /** Returns the path of [url], never `null`; empty when there is no path. */
    fun pathOf(url: String): String {
        val schemeEnd = url.indexOf(SCHEME_SEPARATOR)
        if (schemeEnd < 0) return ""

        val authorityStart = schemeEnd + SCHEME_SEPARATOR.length
        val pathStart = firstIndexOfAnyAfter(url, authorityStart, "/")
        if (pathStart < 0) return ""

        val pathEnd = firstIndexOfAnyAfter(url, pathStart, "?#")
        return url.substring(pathStart, if (pathEnd < 0) url.length else pathEnd)
    }

    /**
     * Drops everything from the first `?` or `#`.
     *
     * Tracking parameters and share tokens live there; the content of a TikTok
     * video lives in the path, which is kept.
     */
    fun stripQueryAndFragment(url: String): String {
        val cut = firstIndexOfAnyAfter(url, 0, "?#")
        return if (cut < 0) url else url.substring(0, cut)
    }

    private fun firstIndexOfAnyAfter(value: String, from: Int, needles: String): Int {
        for (index in from until value.length) {
            if (value[index] in needles) return index
        }
        return -1
    }
}
