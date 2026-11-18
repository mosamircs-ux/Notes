package org.fossify.notes.activities

import android.os.Bundle
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.notes.BuildConfig
import org.fossify.notes.R
import org.fossify.notes.databinding.ActivityAboutNoteflowBinding

class AboutNoteFlowActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityAboutNoteflowBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.aboutScrollview))
        setupMaterialScrollListener(binding.aboutScrollview, binding.aboutAppbar)

        binding.aboutVersionBadge.text = getString(R.string.noteflow_version_format, BuildConfig.VERSION_NAME)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.aboutAppbar, NavigationIcon.Arrow)
    }
}
