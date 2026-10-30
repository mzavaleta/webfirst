package io.github.mzavaleta.webfirst

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Hands a URL to the user's browser.
 *
 * Custom Tabs are used on purpose: they bind to a real browser package and can
 * never resolve back to this app. Re-firing a plain `ACTION_VIEW` for a host we
 * intercept would reopen the interceptor and loop forever.
 */
object BrowserLauncher {

    fun open(context: Context, url: String) {
        val uri = Uri.parse(url)
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, uri)
        } catch (e: ActivityNotFoundException) {
            openWithAnyBrowser(context, uri)
        }
    }

    /**
     * Fallback for devices with no Custom Tabs provider: pick any app that can
     * open the URL, explicitly excluding this one.
     */
    private fun openWithAnyBrowser(context: Context, uri: Uri) {
        val intent = browserIntent(uri)
        val browserPackage = context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .firstOrNull { it != context.packageName }
            ?: return

        context.startActivity(intent.setPackage(browserPackage))
    }

    private fun browserIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
