package io.github.mzavaleta.webfirst

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Hands a URL to the user's browser.
 *
 * Every intent fired from here is **pinned to a browser package**. That is not a
 * detail: `CustomTabsIntent.launchUrl` produces a plain `ACTION_VIEW`, so once
 * this app is the default handler for the host, an unpinned intent resolves back
 * into this app and the interceptor calls itself forever. Resolving a neutral
 * URL is the only reliable way to learn which package is actually the browser.
 */
object BrowserLauncher {

    private const val TAG = "WebFirst"

    /** Never intercepted by this app, so it always resolves to a real browser. */
    private const val NEUTRAL_PROBE_URL = "https://example.com/"

    /**
     * Opens [url] and returns whether anything was launched.
     *
     * Never throws: the caller runs on the interception path and must always be
     * able to finish, otherwise the invisible activity stays on top of whatever
     * app the link came from.
     */
    fun open(context: Context, url: String): Boolean {
        val uri = try {
            Uri.parse(url)
        } catch (error: Exception) {
            Log.e(TAG, "unparseable url: $url", error)
            return false
        }

        val browser = browserPackage(context)
        if (browser == null) {
            Log.e(TAG, "no browser package found")
            return false
        }

        // Informational only: the intent below is pinned with setPackage, so it
        // cannot come back here. Refusing would break the common case of a short
        // link that this app also handles.
        if (wouldResolveToSelf(context, uri)) {
            Log.w(TAG, "$uri would resolve back into this app; the browser package is pinned anyway")
        }

        if (launchCustomTab(context, uri, browser)) return true
        Log.w(TAG, "custom tab failed, opening $browser directly")
        return launchInBrowser(context, uri, browser)
    }

    /**
     * Resolves a URL this app does not handle, so the answer is the user's
     * browser and never this app.
     */
    private fun browserPackage(context: Context): String? {
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse(NEUTRAL_PROBE_URL)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val defaultPackage = try {
            context.packageManager
                .resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        } catch (error: Exception) {
            Log.e(TAG, "could not resolve the default browser", error)
            null
        }

        if (defaultPackage != null && defaultPackage != context.packageName) return defaultPackage

        return try {
            context.packageManager
                .queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo.packageName }
                .firstOrNull { it != context.packageName }
        } catch (error: Exception) {
            Log.e(TAG, "could not list browsers", error)
            null
        }
    }

    private fun wouldResolveToSelf(context: Context, uri: Uri): Boolean = try {
        context.packageManager
            .resolveActivity(Intent(Intent.ACTION_VIEW, uri), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName == context.packageName
    } catch (error: Exception) {
        false
    }

    private fun launchCustomTab(context: Context, uri: Uri, browser: String): Boolean = try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .apply {
                intent.setPackage(browser)
                intent.data = uri
            }
            .launchUrl(context, uri)
        true
    } catch (expected: ActivityNotFoundException) {
        false
    } catch (unexpected: Exception) {
        Log.e(TAG, "custom tab launch failed", unexpected)
        false
    }

    private fun launchInBrowser(context: Context, uri: Uri, browser: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(browser)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            Log.e(TAG, "failed to start $browser", error)
            false
        }
    }
}
