package io.github.mzavaleta.webfirst

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
 * Custom Tabs are used on purpose: they bind to a real browser package and can
 * never resolve back to this app. Re-firing a plain `ACTION_VIEW` for a host we
 * intercept would reopen the interceptor and loop forever.
 */
object BrowserLauncher {

    private const val TAG = "WebFirst"

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

        if (launchCustomTab(context, uri)) return true
        Log.w(TAG, "no Custom Tabs provider, falling back to any browser")
        return launchAnyBrowser(context, uri)
    }

    private fun launchCustomTab(context: Context, uri: Uri): Boolean = try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
        true
    } catch (expected: ActivityNotFoundException) {
        false
    } catch (unexpected: Exception) {
        Log.e(TAG, "custom tab launch failed", unexpected)
        false
    }

    /**
     * Fallback for devices with no Custom Tabs provider: pick any app that can
     * open the URL, explicitly excluding this one.
     */
    private fun launchAnyBrowser(context: Context, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val candidates = try {
            context.packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo.packageName }
                .filter { it != context.packageName }
        } catch (error: Exception) {
            Log.e(TAG, "could not list browsers", error)
            return false
        }

        val browserPackage = candidates.firstOrNull() ?: run {
            Log.e(TAG, "no browser found for $uri")
            return false
        }

        return try {
            context.startActivity(intent.setPackage(browserPackage))
            true
        } catch (error: Exception) {
            Log.e(TAG, "failed to start $browserPackage", error)
            false
        }
    }
}
