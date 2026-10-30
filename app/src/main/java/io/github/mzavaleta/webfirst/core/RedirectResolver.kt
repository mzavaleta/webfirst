package io.github.mzavaleta.webfirst.core

/** Resolves the final destination of a URL that is expected to redirect. */
fun interface RedirectResolver {

    /**
     * Returns the final URL reached after following redirects, or [url] itself
     * when resolution is impossible. Implementations must never throw.
     */
    fun resolve(url: String): String
}
