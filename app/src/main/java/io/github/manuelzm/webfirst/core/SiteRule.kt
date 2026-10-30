package io.github.manuelzm.webfirst.core

/**
 * Describes one site the app knows how to handle.
 *
 * Rules are data, not code. Adding a site means adding an entry to [SiteRules.all]
 * plus the matching hosts in `AndroidManifest.xml`; no new logic is required.
 *
 * @property hostSuffixes suffixes matched against the full host, so `tiktok.com`
 *   covers `www.tiktok.com` and `vm.tiktok.com` but not `eviltiktok.com`.
 * @property shortLinkHosts hosts whose every path is a redirect stub.
 * @property shortLinkPathPrefixes path prefixes that hold redirect stubs on
 *   otherwise canonical hosts.
 */
data class SiteRule(
    val id: String,
    val label: String,
    val hostSuffixes: Set<String>,
    val shortLinkHosts: Set<String> = emptySet(),
    val shortLinkPathPrefixes: Set<String> = emptySet(),
) {

    fun matches(host: String): Boolean =
        hostSuffixes.any { suffix -> host == suffix || host.endsWith(".$suffix") }

    /**
     * Whether [url] must be resolved over the network before it can be cleaned.
     *
     * A short link such as `https://vm.tiktok.com/ZMabc123/` carries no query
     * string at all, so stripping it is a no-op: the tracking parameters only
     * appear after the redirect chain reaches the canonical URL.
     */
    fun isShortLink(host: String, path: String): Boolean =
        host in shortLinkHosts || shortLinkPathPrefixes.any { path.startsWith(it) }
}
