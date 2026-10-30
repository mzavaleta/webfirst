package io.github.mzavaleta.webfirst

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import io.github.mzavaleta.webfirst.core.SiteRules

/**
 * The only visible screen: explains the app and links to the system setting that
 * makes it the default handler.
 *
 * The app cannot verify the domains it handles — that requires hosting
 * `assetlinks.json` on tiktok.com, which nobody but TikTok can do — so the user
 * has to associate them once by hand.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.supported_sites).text = SiteRules.all.joinToString("\n") { rule ->
            "• ${rule.label} (${rule.hostSuffixes.joinToString(", ")})"
        }

        findViewById<Button>(R.id.open_by_default).setOnClickListener { openDefaultAppsSettings() }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.association_state).text = associationState()
    }

    private fun associationState(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return getString(R.string.state_unknown)

        val manager = getSystemService(DomainVerificationManager::class.java)
            ?: return getString(R.string.state_unknown)
        val state = manager.getDomainVerificationUserState(packageName)
            ?: return getString(R.string.state_unknown)

        val approved = state.hostToStateMap.values.count {
            it == DomainVerificationUserState.DOMAIN_STATE_SELECTED ||
                it == DomainVerificationUserState.DOMAIN_STATE_VERIFIED
        }
        return if (approved > 0) getString(R.string.state_linked) else getString(R.string.state_not_linked)
    }

    private fun openDefaultAppsSettings() {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
        } else {
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
        val intent = Intent(action, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Some OEMs hide this screen; the README documents the manual path.
        }
    }
}
