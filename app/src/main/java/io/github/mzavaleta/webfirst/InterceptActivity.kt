package io.github.mzavaleta.webfirst

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import io.github.mzavaleta.webfirst.core.HttpRedirectResolver
import io.github.mzavaleta.webfirst.core.LinkPipeline
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Invisible entry point for web links.
 *
 * Two rules this activity must never break:
 *
 * 1. It always finishes. It sits in a transparent window over the app the link
 *    came from, so a path that does not call [finish] looks to the user like
 *    that other app froze.
 * 2. It never waits on the network. Redirect resolution runs on a worker
 *    thread and races a hard deadline; whichever wins delivers the URL.
 *    Kotlin's `withTimeout` is cooperative and cannot interrupt a blocking
 *    socket call, so the deadline is enforced with a posted callback instead.
 */
class InterceptActivity : ComponentActivity() {

    private val pipeline by lazy { LinkPipeline(HttpRedirectResolver()) }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val delivered = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intercept(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intercept(intent)
    }

    private fun intercept(intent: Intent?) {
        val rawUrl = if (intent?.action == Intent.ACTION_VIEW) intent.dataString else null

        if (rawUrl.isNullOrBlank()) {
            Log.d(TAG, "intent carried no url, opening the main screen")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        Log.d(TAG, "intercepted: $rawUrl")
        delivered.set(false)

        mainHandler.postDelayed(
            { deliver(rawUrl, "deadline reached, opening the original link") },
            RESOLVE_BUDGET_MS,
        )

        resolverExecutor.execute {
            val target = try {
                pipeline.prepare(rawUrl)
            } catch (error: Exception) {
                Log.e(TAG, "resolution failed, falling back to the original link", error)
                rawUrl
            }
            mainHandler.post { deliver(target, "resolved") }
        }
    }

    /** First caller wins; every caller arrives on the main thread. */
    private fun deliver(url: String, reason: String) {
        if (!delivered.compareAndSet(false, true)) return
        mainHandler.removeCallbacksAndMessages(null)

        if (isFinishing || isDestroyed) {
            Log.w(TAG, "activity is gone, dropping $url")
            return
        }

        Log.d(TAG, "delivering ($reason): $url")
        try {
            if (!BrowserLauncher.open(this, url)) {
                Toast.makeText(this, getString(R.string.error_no_browser), Toast.LENGTH_LONG).show()
            }
        } catch (error: Exception) {
            Log.e(TAG, "could not hand the url to a browser", error)
        } finally {
            finish()
        }
    }

    private companion object {
        const val TAG = "WebFirst"
        const val RESOLVE_BUDGET_MS = 1_500L

        /** Daemon: a stuck lookup must never keep the process alive. */
        val resolverExecutor = Executors.newFixedThreadPool(2) { runnable ->
            Thread(runnable, "webfirst-resolver").apply { isDaemon = true }
        }
    }
}
