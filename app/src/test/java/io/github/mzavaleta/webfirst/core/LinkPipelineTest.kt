package io.github.mzavaleta.webfirst.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkPipelineTest {

    private val resolvedUrls = mutableListOf<String>()

    private fun pipeline(resolving: (String) -> String = { it }): LinkPipeline {
        resolvedUrls.clear()
        return LinkPipeline(
            RedirectResolver { url ->
                resolvedUrls += url
                resolving(url)
            },
        )
    }

    @Test
    fun `strips tracking parameters from a canonical video url`() {
        val pipeline = pipeline()

        val result = pipeline.prepare(
            "https://www.tiktok.com/@scout2015/video/6718335390845095173" +
                "?is_from_webapp=1&sender_device=pc&web_id=7390000000000000000",
        )

        assertEquals("https://www.tiktok.com/@scout2015/video/6718335390845095173", result)
        assertTrue("canonical urls must not trigger a network call", resolvedUrls.isEmpty())
    }

    @Test
    fun `keeps a clean url unchanged`() {
        val pipeline = pipeline()

        val result = pipeline.prepare("https://www.tiktok.com/@scout2015/video/6718335390845095173")

        assertEquals("https://www.tiktok.com/@scout2015/video/6718335390845095173", result)
        assertTrue(resolvedUrls.isEmpty())
    }

    @Test
    fun `resolves then cleans a vm short link`() {
        val pipeline = pipeline {
            "https://www.tiktok.com/@scout2015/video/6718335390845095173?is_from_webapp=1"
        }

        val result = pipeline.prepare("https://vm.tiktok.com/ZMabc123/")

        assertEquals("https://www.tiktok.com/@scout2015/video/6718335390845095173", result)
        assertEquals(listOf("https://vm.tiktok.com/ZMabc123/"), resolvedUrls)
    }

    @Test
    fun `resolves then cleans a vt short link`() {
        val pipeline = pipeline { "https://www.tiktok.com/@user/photo/123?share_app_id=1" }

        val result = pipeline.prepare("https://vt.tiktok.com/ZSabc123/")

        assertEquals("https://www.tiktok.com/@user/photo/123", result)
        assertEquals(1, resolvedUrls.size)
    }

    @Test
    fun `resolves then cleans a short code on the canonical host`() {
        val pipeline = pipeline { "https://www.tiktok.com/@user/video/99?utm_source=copy" }

        val result = pipeline.prepare("https://www.tiktok.com/t/ZTabc123/")

        assertEquals("https://www.tiktok.com/@user/video/99", result)
        assertEquals(listOf("https://www.tiktok.com/t/ZTabc123/"), resolvedUrls)
    }

    @Test
    fun `does not touch an unsupported site`() {
        val pipeline = pipeline { "https://example.com/should-not-be-called" }

        val url = "https://youtube.com/watch?v=dQw4w9WgXcQ"
        val result = pipeline.prepare(url)

        assertEquals(url, result)
        assertTrue(resolvedUrls.isEmpty())
    }

    @Test
    fun `does not strip after a redirect leaves the supported site`() {
        val pipeline = pipeline { "https://example.com/landing?utm_source=tiktok" }

        val result = pipeline.prepare("https://vm.tiktok.com/ZMabc123/")

        assertEquals("https://example.com/landing?utm_source=tiktok", result)
    }

    @Test
    fun `falls back to the short link when resolution fails`() {
        val pipeline = pipeline { input -> input }

        val result = pipeline.prepare("https://vm.tiktok.com/ZMabc123/")

        assertEquals("https://vm.tiktok.com/ZMabc123/", result)
        assertEquals(1, resolvedUrls.size)
    }

    @Test
    fun `returns malformed input untouched`() {
        val pipeline = pipeline()

        val result = pipeline.prepare("this is not a url")

        assertEquals("this is not a url", result)
        assertTrue(resolvedUrls.isEmpty())
    }
}
