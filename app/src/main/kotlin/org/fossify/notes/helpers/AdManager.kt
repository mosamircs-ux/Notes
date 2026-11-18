package org.fossify.notes.helpers

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import org.fossify.notes.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private const val TAG = "NoteFlowAdManager"
    private val isMobileAdsInitialized = AtomicBoolean(false)

    fun initialize(context: Context) {
        if (isMobileAdsInitialized.compareAndSet(false, true)) {
            Thread {
                try {
                    MobileAds.initialize(context.applicationContext) {
                        Log.d(TAG, "Google Mobile Ads SDK initialized.")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to initialize Google Mobile Ads SDK", e)
                    isMobileAdsInitialized.set(false)
                }
            }.start()
        }
    }

    /**
     * Gathers GDPR / EEA / UK consent using Google's User Messaging Platform (UMP) SDK
     * before initializing MobileAds and loading the anchored adaptive banner.
     */
    fun gatherConsentAndLoadBanner(
        activity: Activity,
        adContainer: FrameLayout,
        dividerView: View? = null,
        onBannerCreated: (AdView?) -> Unit = {},
    ) {
        val bannerLoaded = AtomicBoolean(false)

        fun tryStartAds() {
            if (activity.isFinishing || activity.isDestroyed) return
            if (bannerLoaded.compareAndSet(false, true)) {
                activity.runOnUiThread {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        val adView = loadAdaptiveBanner(activity, adContainer, dividerView)
                        onBannerCreated(adView)
                    }
                }
            }
        }

        try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            // For returning users who already gave consent in a prior session, load ads immediately.
            if (consentInformation.canRequestAds()) {
                tryStartAds()
            }

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                        if (loadAndShowError != null) {
                            Log.w(TAG, "UMP consent form notice: ${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                        }
                        if (consentInformation.canRequestAds()) {
                            tryStartAds()
                        }
                    }
                },
                { requestConsentError ->
                    Log.w(TAG, "UMP consent info update error: ${requestConsentError.errorCode}: ${requestConsentError.message}")
                    if (consentInformation.canRequestAds()) {
                        tryStartAds()
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "UMP setup fallback: unable to query consent info", e)
            tryStartAds()
        }
    }

    fun loadAdaptiveBanner(
        activity: Activity,
        adContainer: FrameLayout,
        dividerView: View? = null,
    ): AdView? {
        return try {
            initialize(activity.applicationContext)

            val adView = AdView(activity).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
                setAdSize(getAdaptiveBannerSize(activity, adContainer))
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        adContainer.visibility = View.VISIBLE
                        dividerView?.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.w(TAG, "Adaptive banner failed to load: ${adError.message}")
                        adContainer.visibility = View.GONE
                        dividerView?.visibility = View.GONE
                    }
                }
            }

            adContainer.removeAllViews()
            adContainer.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
            adView
        } catch (e: Exception) {
            Log.w(TAG, "Error setting up adaptive banner", e)
            adContainer.visibility = View.GONE
            dividerView?.visibility = View.GONE
            null
        }
    }

    private fun getAdaptiveBannerSize(activity: Activity, adContainer: FrameLayout): AdSize {
        val density = activity.resources.displayMetrics.density
        var adWidthPixels = adContainer.width.toFloat()
        if (adWidthPixels <= 0f) {
            adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.windowManager.currentWindowMetrics.bounds.width().toFloat()
            } else {
                activity.resources.displayMetrics.widthPixels.toFloat()
            }
        }
        val adWidthDp = (adWidthPixels / density).toInt().coerceAtLeast(320)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp)
    }
}
