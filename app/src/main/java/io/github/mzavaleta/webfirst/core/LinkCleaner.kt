package io.github.mzavaleta.webfirst.core

object LinkCleaner {

    /**
     * Removes the query string and the fragment from [url].
     *
     * Only call this for hosts handled by a [SiteRule]. It is intentionally not
     * applied to every link: on YouTube the query string *is* the content
     * (`watch?v=ID`), so a global strip would destroy the link instead of
     * cleaning it.
     */
    fun strip(url: String): String = LinkUrls.stripQueryAndFragment(url)
}
