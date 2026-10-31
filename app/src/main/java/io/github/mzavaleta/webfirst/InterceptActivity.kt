package io.github.mzavaleta.webfirst

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.mzavaleta.webfirst.core.HttpRedirectResolver
import io.github.mzavaleta.webfirst.core.LinkPipeline
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Invisible entry point for web links.
 *
 * Contract: this activity always finishes. It sits in a transparent window over
 * whatever app the link came from, so a path that never calls [finish] looks to
 * the user like the other app froze.
 */
class InterceptActivity : ComponentActivity() {

    private val pipeline by lazy { LinkPipeline(HttpRedirectResolver()) }

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

        lifecycleScope.launch {
            val target = resolveWithinBudget(rawUrl)
            Log.d(TAG, "opening: $target")

            try {
                if (!BrowserLauncher.open(this@InterceptActivity, target)) {
                    Toast.makeText(
                        this@InterceptActivity,
                        getString(R.string.error_no_browser),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (unexpected: Exception) {
                Log.e(TAG, "could not hand the url to a browser", unexpected)
            } finally {
                finish()
            }
        }
    }

    /**
     * Resolution is best effort and time boxed: a slow network degrades to
     * opening the original link instead of leaving an invisible activity on
     * screen while a socket blocks.
     */
    private suspend fun resolveWithinBudget(rawUrl: String): String =
        try {
            withTimeoutOrNull(RESOLVE_BUDGET_MS) {
                withContext(Dispatchers.IO) { pipeline.prepare(rawUrl) }
            } ?: rawUrl.also { Log.w(TAG, "resolution budget exceeded, opening the original url") }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(TAG, "resolution failed, opening the original url", error)
            rawUrl
        }

    private companion object {
        const val TAG = "WebFirst"
        const val RESOLVE_BUDGET_MS = 2_500L
    }
}
