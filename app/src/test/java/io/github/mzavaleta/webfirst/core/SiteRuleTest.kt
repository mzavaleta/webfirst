package io.github.mzavaleta.webfirst.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteRuleTest {

    private val rule = SiteRules.TIKTOK

    @Test
    fun `matches the bare domain and its subdomains`() {
        assertTrue(rule.matches("tiktok.com"))
        assertTrue(rule.matches("www.tiktok.com"))
        assertTrue(rule.matches("m.tiktok.com"))
        assertTrue(rule.matches("vt.tiktok.com"))
    }

    @Test
    fun `does not match lookalike domains`() {
        assertFalse(rule.matches("eviltiktok.com"))
        assertFalse(rule.matches("tiktok.com.evil.io"))
        assertFalse(rule.matches("youtube.com"))
    }

    @Test
    fun `resolves short link hosts`() {
        assertTrue(rule.isShortLink("vm.tiktok.com", "/ZMabc123/"))
        assertTrue(rule.isShortLink("vt.tiktok.com", "/ZSabc123/"))
    }

    @Test
    fun `resolves short code paths on the canonical host`() {
        assertTrue(rule.isShortLink("www.tiktok.com", "/t/ZTabc123/"))
        assertTrue(rule.isShortLink("www.tiktok.com", "/v/123.html"))
    }

    @Test
    fun `does not resolve canonical video urls`() {
        assertFalse(rule.isShortLink("www.tiktok.com", "/@scout2015/video/6718335390845095173"))
    }

    @Test
    fun `lookup finds the tiktok rule`() {
        assertSame(SiteRules.TIKTOK, SiteRules.ruleFor("www.tiktok.com"))
    }

    @Test
    fun `lookup ignores unsupported hosts`() {
        assertNull(SiteRules.ruleFor("example.com"))
    }
}
