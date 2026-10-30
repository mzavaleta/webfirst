package io.github.manuelzm.webfirst.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkUrlsTest {

    @Test
    fun `extracts plain host`() {
        assertEquals("tiktok.com", LinkUrls.hostOf("https://tiktok.com/@user/video/1"))
    }

    @Test
    fun `extracts host with subdomain`() {
        assertEquals("vm.tiktok.com", LinkUrls.hostOf("https://vm.tiktok.com/ZMabc123/"))
    }

    @Test
    fun `ignores port`() {
        assertEquals("www.tiktok.com", LinkUrls.hostOf("https://www.tiktok.com:443/@user/video/1"))
    }

    @Test
    fun `ignores credentials`() {
        assertEquals("www.tiktok.com", LinkUrls.hostOf("https://user:pass@www.tiktok.com/@user/video/1"))
    }

    @Test
    fun `lower cases the host`() {
        assertEquals("www.tiktok.com", LinkUrls.hostOf("HTTPS://WWW.TIKTOK.COM/@user/video/1"))
    }

    @Test
    fun `returns null without scheme`() {
        assertNull(LinkUrls.hostOf("www.tiktok.com/@user/video/1"))
    }

    @Test
    fun `extracts path without query and fragment`() {
        assertEquals("/@user/video/1", LinkUrls.pathOf("https://www.tiktok.com/@user/video/1?is_from_webapp=1#top"))
    }

    @Test
    fun `path is empty when there is none`() {
        assertEquals("", LinkUrls.pathOf("https://www.tiktok.com?x=1"))
    }

    @Test
    fun `drops query string`() {
        assertEquals(
            "https://www.tiktok.com/@user/video/1",
            LinkUrls.stripQueryAndFragment("https://www.tiktok.com/@user/video/1?is_from_webapp=1&sender_device=pc"),
        )
    }

    @Test
    fun `drops fragment`() {
        assertEquals(
            "https://www.tiktok.com/@user/video/1",
            LinkUrls.stripQueryAndFragment("https://www.tiktok.com/@user/video/1#comments"),
        )
    }

    @Test
    fun `drops the first delimiter and everything after it`() {
        assertEquals(
            "https://www.tiktok.com/@user/video/1",
            LinkUrls.stripQueryAndFragment("https://www.tiktok.com/@user/video/1#a?b=1"),
        )
    }

    @Test
    fun `leaves clean url untouched`() {
        assertEquals(
            "https://www.tiktok.com/@user/video/1",
            LinkUrls.stripQueryAndFragment("https://www.tiktok.com/@user/video/1"),
        )
    }
}
