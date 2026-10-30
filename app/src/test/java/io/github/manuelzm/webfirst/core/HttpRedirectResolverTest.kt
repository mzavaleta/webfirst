package io.github.manuelzm.webfirst.core

import java.net.ServerSocket
import java.net.Socket
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the real resolver against a local socket server, including the
 * failure modes that once made the original implementation spin forever.
 *
 * `com.sun.net.httpserver` is unavailable on the Android unit-test classpath, so
 * the server is a few lines of raw HTTP over [ServerSocket].
 */
class HttpRedirectResolverTest {

    private var server: StubHttpServer? = null

    @After
    fun tearDown() {
        server?.stop()
    }

    @Test
    fun `follows a redirect chain to the final url`() {
        val server = StubHttpServer { path ->
            when (path) {
                "/a" -> 302 to "/b"
                "/b" -> 301 to "/c"
                else -> 200 to null
            }
        }
        this.server = server

        val result = HttpRedirectResolver().resolve("${server.baseUrl}/a")

        assertEquals("${server.baseUrl}/c", result)
    }

    @Test(timeout = 5_000)
    fun `stops when a redirect has no location header`() {
        val server = StubHttpServer { 302 to null }
        this.server = server

        val result = HttpRedirectResolver().resolve("${server.baseUrl}/loop")

        assertEquals("${server.baseUrl}/loop", result)
    }

    @Test(timeout = 5_000)
    fun `stops after the redirect limit`() {
        val server = StubHttpServer { 302 to "/forever" }
        this.server = server

        val result = HttpRedirectResolver(maxHops = 3).resolve("${server.baseUrl}/start")

        assertTrue("expected the loop to be capped, got $result", result.endsWith("/forever"))
    }

    @Test
    fun `returns the url unchanged when there is no redirect`() {
        val server = StubHttpServer { 200 to null }
        this.server = server

        val result = HttpRedirectResolver().resolve("${server.baseUrl}/plain?x=1")

        assertEquals("${server.baseUrl}/plain?x=1", result)
    }

    @Test(timeout = 5_000)
    fun `returns the input when the host is unreachable`() {
        val unreachable = "http://127.0.0.1:1/nothing-listens-here"

        val result = HttpRedirectResolver(connectTimeoutMs = 500, readTimeoutMs = 500)
            .resolve(unreachable)

        assertEquals(unreachable, result)
    }

    /** Minimal HTTP/1.1 server: one route handler, one response per connection. */
    private class StubHttpServer(routes: (String) -> Pair<Int, String?>) {

        private val socket = ServerSocket(0)
        private val worker = Thread {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { client -> respond(client, routes) }
                } catch (expected: Exception) {
                    return@Thread
                }
            }
        }.apply {
            isDaemon = true
            start()
        }

        val baseUrl: String = "http://127.0.0.1:${socket.localPort}"

        fun stop() {
            socket.close()
            worker.interrupt()
        }

        private fun respond(client: Socket, routes: (String) -> Pair<Int, String?>) {
            val reader = client.getInputStream().bufferedReader()
            val requestLine = reader.readLine()
            if (requestLine.isNullOrBlank()) return

            var header = reader.readLine()
            while (!header.isNullOrEmpty()) header = reader.readLine()

            val path = requestLine.split(" ").getOrElse(1) { "/" }
            val (status, location) = routes(path)

            val response = buildString {
                append("HTTP/1.1 $status\r\n")
                if (location != null) append("Location: $location\r\n")
                append("Content-Length: 0\r\n")
                append("Connection: close\r\n")
                append("\r\n")
            }
            client.getOutputStream().apply {
                write(response.toByteArray())
                flush()
            }
        }
    }
}
