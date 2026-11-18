package org.fossify.notes

import androidx.core.content.ContextCompat
import org.fossify.commons.FossifyApp
import org.fossify.notes.extensions.config

class App : FossifyApp() {
    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()
        applyNoteFlowDefaultBrandTheme()
    }

    private fun applyNoteFlowDefaultBrandTheme() {
        val prefs = getSharedPreferences("noteflow_brand_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_BRAND_THEME_APPLIED, false)) {
            val indigoPrimary = ContextCompat.getColor(this, R.color.noteflow_indigo_primary)
            val canvasBg = ContextCompat.getColor(this, R.color.noteflow_canvas_bg)
            val primaryText = ContextCompat.getColor(this, R.color.noteflow_text_primary)

            config.apply {
                primaryColor = indigoPrimary
                accentColor = indigoPrimary
                appIconColor = indigoPrimary
                backgroundColor = canvasBg
                textColor = primaryText
            }

            prefs.edit().putBoolean(KEY_BRAND_THEME_APPLIED, true).apply()
        }
    }

    companion object {
        private const val KEY_BRAND_THEME_APPLIED = "noteflow_theme_v1_applied"
    }
}
