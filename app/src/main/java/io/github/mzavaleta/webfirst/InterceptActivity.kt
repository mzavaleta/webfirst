package io.github.mzavaleta.webfirst

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.mzavaleta.webfirst.core.HttpRedirectResolver
import io.github.mzavaleta.webfirst.core.LinkPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Invisible entry point for web links.
 *
 * Lifecycle: no UI, no history, excluded from recents. The activity resolves and
 * cleans the link on a background thread, forwards it to the browser and exits.
 * A failed resolution still opens the original URL, so the app can never leave
 * the user with a dead end.
 */
class InterceptActivity : ComponentActivity() {

    private val pipeline by lazy { LinkPipeline(HttpRedirectResolver()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val rawUrl = intent?.dataString

        if (action != Intent.ACTION_VIEW || rawUrl.isNullOrBlank()) {
            // Opened without a link (launcher, adb, or a malformed intent).
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val target = withContext(Dispatchers.IO) { pipeline.prepare(rawUrl) }
            BrowserLauncher.open(this@InterceptActivity, target)
            finish()
        }
    }
}
