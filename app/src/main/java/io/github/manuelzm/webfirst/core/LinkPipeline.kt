package io.github.manuelzm.webfirst.core

/**
 * Decides what to open when a link is intercepted.
 *
 * Pure apart from the injected [RedirectResolver], so the whole decision —
 * including *whether* a network call happens — is unit-testable without a device.
 */
class LinkPipeline(
    private val resolver: RedirectResolver,
    private val rules: List<SiteRule> = SiteRules.all,
) {

    /**
     * Returns the URL to open.
     *
     * Unsupported hosts are returned untouched: the app forwards them rather than
     * mangling a link it does not understand.
     */
    fun prepare(rawUrl: String): String {
        val host = LinkUrls.hostOf(rawUrl) ?: return rawUrl
        val rule = rules.firstOrNull { it.matches(host) } ?: return rawUrl

        val resolved = if (rule.isShortLink(host, LinkUrls.pathOf(rawUrl))) {
            resolver.resolve(rawUrl)
        } else {
            rawUrl
        }

        val finalHost = LinkUrls.hostOf(resolved) ?: return resolved

        // A redirect may leave the supported site entirely; stripping at that
        // point would corrupt a URL we have no rule for.
        return if (rules.any { it.matches(finalHost) }) LinkCleaner.strip(resolved) else resolved
    }
}
