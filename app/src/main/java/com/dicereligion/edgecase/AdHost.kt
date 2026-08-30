package com.dicereligion.edgecase

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus
import com.google.android.libraries.ads.mobile.sdk.initialization.OnAdapterInitializationCompleteListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns every ad-related concern in EdgeCase — the banner and the consent flow both.
 * `MainActivity` gains a handful of call sites and no ad logic of its own.
 *
 * **State: complete (B2 + B3 + B4).** The GMA Next-Gen `AdView` loads through the per-build-type
 * unit id, and UMP resolves consent before any ad is requested. Nothing here is stubbed.
 *
 * Invariants — see Docs/Ads.md before changing any of these:
 *  • Exactly one [AdView] exists for the Activity's lifetime.
 *  • The AdView is the ONLY child of [adFrame]; the stone frame is adFrame's *background*, never a
 *    view drawn over the ad (Docs/Ads.md §1.1 C1, §3.5).
 *  • Nothing here is ever constructed from SidebarService (Docs/Ads.md §4.2).
 *  • Space is reserved BEFORE the ad is requested, so the app UI never shifts under the user's
 *    finger (Docs/Ads.md §3.1 item 4).
 *  • No client-side retry on a failed load — that is an invalid-traffic pattern. Refresh is
 *    server-side, fixed at 60s in the AdMob console.
 *  • **No ad is ever requested until `canRequestAds()` is true.** [start] is the only entry point,
 *    and every path into [initializeAndLoad] passes that check.
 */
class AdHost(private val activity: Activity, private val adFrame: FrameLayout) {

    companion object {
        private const val TAG = "EdgeCaseAds"

        /** `MobileAds.initialize` is process-wide; guard it against Activity recreation. */
        private val sdkInitialized = AtomicBoolean(false)

        /**
         * Pre-layout height estimate, used only to reserve the slot before the real [AdSize] is
         * known. The exact size replaces it inside [attachBanner]'s layout pass.
         *
         * Docs/Ads.md §3.8 puts a large anchored adaptive banner at roughly 90–110dp on a phone.
         */
        private const val NOMINAL_BANNER_HEIGHT_DP = 100f

        /** The format's hard ceiling: min(150dp, 20% of device height). Docs/Ads.md §3.8. */
        private const val MAX_BANNER_HEIGHT_DP = 150f
        private const val MAX_BANNER_HEIGHT_FRACTION = 0.20f
    }

    private var adView: AdView? = null
    private var adRequested = false
    private lateinit var consentInformation: ConsentInformation

    /**
     * Invoked once UMP has finished resolving consent, so the caller can re-evaluate whether the
     * privacy-options entry point should be visible.
     *
     * [isPrivacyOptionsRequired] is meaningless until `requestConsentInfoUpdate` returns, which is
     * after `onCreate` has already run — so the button's visibility cannot be decided once at
     * start-up and left alone. Docs/Ads.md §7.7.
     *
     * Nothing calls this until B3; the button simply stays hidden until then.
     */
    var onConsentResolved: (() -> Unit)? = null

    // ──────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────

    /**
     * Entry point, called once from `MainActivity.onCreate`.
     *
     * Resolves consent first and requests an ad only once `canRequestAds()` is true
     * (Docs/Ads.md §7.3). Consent info must be refreshed on **every** launch, not cached across
     * sessions, because the user's choice — or the regime that applies to them — can change.
     *
     * Three paths can reach [initializeAndLoad], and all three check `canRequestAds()` first:
     * the form was shown and dismissed, the update failed but cached consent still permits ads,
     * or a previous session's consent already permits an immediate request. [initializeAndLoad]
     * is idempotent, so overlapping paths are harmless.
     */
    fun start() {
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form: ${formError.errorCode} ${formError.message}")
                    }
                    logConsentState("form dismissed")
                    // The requirement status is only meaningful now, and MainActivity decided the
                    // AD CONSENT button's visibility back in onCreate — so re-run that decision.
                    onConsentResolved?.invoke()
                    if (consentInformation.canRequestAds()) initializeAndLoad()
                }
            },
            { requestError ->
                // Non-fatal: cached consent from a previous session may still permit ads.
                Log.w(TAG, "Consent update failed: ${requestError.errorCode} ${requestError.message}")
                onConsentResolved?.invoke()
                if (consentInformation.canRequestAds()) initializeAndLoad()
            }
        )

        // Cached consent can allow a request immediately, without waiting for the round trip.
        logConsentState("start")
        if (consentInformation.canRequestAds()) initializeAndLoad()
    }

    /**
     * Hides or restores the banner while a modal dialog is on screen.
     *
     * Two reasons, both from Docs/Ads.md:
     *  • A dialog dims everything behind it, the ad included. An obscured ad is an obstruction —
     *    impressions under it are not viewable (§3.5). A dim layer is not transparent, gone, or
     *    invisible, so it does not clear that rule.
     *  • A dialog's action row (RESET / CANCEL / APPLY) lands close to the ad, bypassing the
     *    plinth's whole buffer budget. Ads adjacent to buttons are the top accidental-click
     *    cause Google enforces against (§3.2 #1).
     *
     * INVISIBLE, never GONE: INVISIBLE is one of the three states that explicitly clears the
     * obstruction rule, and it keeps the slot's height so nothing re-lays out behind the dialog.
     */
    fun setAdVisible(visible: Boolean) {
        adView?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    fun destroy() {
        adView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        adView = null
    }

    // ──────────────────────────────────────────────
    // Consent (UMP)
    // ──────────────────────────────────────────────

    /**
     * Whether Google requires a visible "privacy options" entry point for this user.
     *
     * True only where a consent regime applies — the EEA, the UK, Switzerland, and the US states
     * with an applicable law. Everyone else never sees the control. This is a **Google
     * requirement**, not a nicety: where UMP reports `REQUIRED`, the app must offer a way back
     * into the consent form (Docs/Ads.md §7.7), and the published privacy policy §6.2/§6.4 tells
     * users that control exists.
     *
     * The `isInitialized` guard matters: [start] assigns `consentInformation` synchronously, but
     * `MainActivity` calls this from `wireSubScreenButtons()`, and a future reordering that ran
     * the wiring first would otherwise throw on an uninitialised lateinit.
     */
    fun isPrivacyOptionsRequired(): Boolean =
        ::consentInformation.isInitialized &&
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Reopens Google's consent form so the user can change or withdraw their choice.
     *
     * Note this is **not** a link to a web page — UMP renders Google's own form inside the app,
     * and the choice it records is what the ad request reads. A URL cannot do this.
     *
     * Withdrawing consent can change the requirement status, so the dismissal re-syncs the
     * button rather than assuming it stays visible.
     */
    fun showPrivacyOptionsForm() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy form: ${formError.errorCode} ${formError.message}")
            }
            onConsentResolved?.invoke()
        }
    }

    // ──────────────────────────────────────────────
    // SDK initialisation — MUST be off the main thread (ANR risk)
    // ──────────────────────────────────────────────

    /**
     * Initialises the SDK once per process, then attaches the banner.
     *
     * Next-Gen requires initialisation off the main thread. This uses a raw `Thread` to match the
     * codebase's existing idiom (see `MainActivity.preloadApps`) rather than pulling in
     * kotlinx-coroutines for one call.
     */
    private fun initializeAndLoad() {
        if (adRequested) return
        adRequested = true

        if (sdkInitialized.compareAndSet(false, true)) {
            Thread {
                MobileAds.initialize(
                    activity.applicationContext,
                    InitializationConfig.Builder(
                        activity.getString(R.string.admob_app_id)
                    ).build(),
                    object : OnAdapterInitializationCompleteListener {
                        override fun onAdapterInitializationComplete(status: InitializationStatus) {
                            activity.runOnUiThread { attachBanner() }
                        }
                    }
                )
            }.start()
        } else {
            attachBanner()
        }
    }

    // ──────────────────────────────────────────────
    // Banner — reserve space first, then load
    // ──────────────────────────────────────────────

    /**
     * Reserves the slot immediately, then installs the [AdView] once [adFrame] has a measured
     * width.
     *
     * The reservation is deliberately done **twice**. The nominal estimate lands outside the layout
     * callback so the well is the right size from the very first frame — a slot that starts
     * collapsed and grows is exactly the layout shift §3.1 forbids. The exact figure then replaces
     * it inside the callback, once the real [AdSize] is known. Docs/Ads.md §7.3 shows only the
     * second; without the first, the plinth pops on launch.
     *
     * The layout wait itself is unavoidable here (unlike the old placeholder, which was
     * MATCH_PARENT and needed no measurement): the adaptive size is a function of the frame's real
     * inner width in dp.
     */
    private fun attachBanner() {
        if (activity.isFinishing || activity.isDestroyed) return
        if (adView != null) return

        val density = activity.resources.displayMetrics.density

        // First pass: reserve the estimated slot before any measurement exists.
        adFrame.minimumHeight = (resolveNominalHeightDp() * density).toInt() +
            adFrame.paddingTop + adFrame.paddingBottom

        adFrame.doOnLayout {
            if (activity.isFinishing || activity.isDestroyed) return@doOnLayout
            if (adView != null) return@doOnLayout

            val innerPx = (adFrame.width - adFrame.paddingLeft - adFrame.paddingRight)
                .coerceAtLeast(1)
            val widthDp = (innerPx / density).toInt()
            if (widthDp <= 0) return@doOnLayout

            // Current, non-deprecated anchored adaptive API (Docs/Ads.md §3.8).
            val adSize = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, widthDp)

            // Second pass: the exact reservation, from the size the ad will actually be.
            adFrame.minimumHeight = adSize.getHeightInPixels(activity) +
                adFrame.paddingTop + adFrame.paddingBottom

            val view = AdView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            adFrame.addView(view)
            adView = view

            val request = BannerAdRequest.Builder(
                activity.getString(R.string.admob_banner_unit),
                adSize
            ).build()

            view.loadAd(request, object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    Log.d(TAG, "Plinth banner loaded (${adSize.width}×${adSize.height}dp).")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // No retry storm: a failed load leaves a correctly sized empty well, and the
                    // server-side 60s refresh takes the next attempt.
                    Log.w(TAG, "Plinth banner failed: ${adError.code} ${adError.message}")
                }
            })
        }
    }

    /**
     * One line describing where consent landed.
     *
     * This is the readout for the EEA acceptance test in Docs/Ads.md §7.7: set debug geography to
     * EEA for a registered test device in the AdMob console, relaunch, and confirm the form appears
     * and this logs `required=REQUIRED`. Without it the only visible signal is whether a button
     * appeared, which is hard to distinguish from a layout bug.
     */
    private fun logConsentState(phase: String) {
        if (!::consentInformation.isInitialized) return
        Log.d(
            TAG,
            "Consent [$phase]: canRequestAds=${consentInformation.canRequestAds()} " +
                "required=${consentInformation.privacyOptionsRequirementStatus} " +
                "formAvailable=${consentInformation.isConsentFormAvailable}"
        )
    }

    /** Nominal pre-layout height, clamped to the format's ceiling for this device. */
    private fun resolveNominalHeightDp(): Float {
        val dm = activity.resources.displayMetrics
        val screenHeightDp = dm.heightPixels / dm.density
        val ceiling = minOf(MAX_BANNER_HEIGHT_DP, screenHeightDp * MAX_BANNER_HEIGHT_FRACTION)
        return minOf(NOMINAL_BANNER_HEIGHT_DP, ceiling)
    }
}
