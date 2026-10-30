package io.github.manuelzm.webfirst.core

/**
 * Registry of supported sites.
 *
 * Keep this list in sync with the `<data>` hosts declared for
 * `InterceptActivity` in `AndroidManifest.xml`: the manifest decides which links
 * reach the app, this list decides what to do with them.
 */
object SiteRules {

    /**
     * TikTok shares three kinds of link:
     *
     * - `https://www.tiktok.com/@user/video/123?is_from_webapp=1&sender_device=pc`
     *   canonical, cleaned by dropping the query string;
     * - `https://vm.tiktok.com/ZMabc123/` and `https://vt.tiktok.com/ZSabc123/`
     *   redirect stubs, resolved first;
     * - `https://www.tiktok.com/t/ZTabc123/` and `/v/123.html`
     *   short codes on the canonical host, resolved first.
     */
    val TIKTOK = SiteRule(
        id = "tiktok",
        label = "TikTok",
        hostSuffixes = setOf("tiktok.com"),
        shortLinkHosts = setOf("vm.tiktok.com", "vt.tiktok.com"),
        shortLinkPathPrefixes = setOf("/t/", "/v/"),
    )

    val all: List<SiteRule> = listOf(TIKTOK)

    fun ruleFor(host: String): SiteRule? = all.firstOrNull { it.matches(host) }
}
