package com.dicereligion.edgecase

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Owns every ad-related concern in EdgeCase. `MainActivity` gains two call sites and no ad logic.
 *
 * **Current state: PREVIEW ONLY.** This installs a [DummyBannerView] so the Plinth's look, footprint
 * and inertness can be evaluated without an AdMob account, a network call, or a new dependency.
 * The class's public shape (`start()` / `destroy()`) and the space-reservation flow are already the
 * final ones, so wiring the real SDK later is a change to [attachBanner] alone — see the marked
 * block there and Docs/Ads.md §7.3.
 *
 * Invariants — see Docs/Ads.md before changing any of these:
 *  • Exactly one banner exists for the Activity's lifetime.
 *  • The banner is the ONLY child of [adFrame]; the stone frame is adFrame's *background*, never a
 *    view drawn over the ad (Docs/Ads.md §1.1 C1, §3.5).
 *  • Nothing here is ever constructed from SidebarService (Docs/Ads.md §4.2).
 *  • Space is reserved from the computed size BEFORE the ad is requested, so the app UI never
 *    shifts under the user's finger (Docs/Ads.md §3.1 item 4).
 */
class AdHost(private val activity: Activity, private val adFrame: FrameLayout) {

    companion object {
        private const val TAG = "EdgeCaseAds"

        /**
         * Nominal height of a large anchored adaptive banner on a typical phone.
         *
         * The real SDK computes this via `AdSize.getLargeAnchoredAdaptiveBannerAdSize(...)`;
         * until it is wired up we approximate. Docs/Ads.md §3.8 puts the realistic range at
         * roughly 90–110dp. **To preview the worst case, set this to 150f** — the format's hard
         * ceiling — and confirm nothing on any screen clips.
         */
        private const val NOMINAL_BANNER_HEIGHT_DP = 100f

        /** The format's hard ceiling: min(150dp, 20% of device height). Docs/Ads.md §3.8. */
        private const val MAX_BANNER_HEIGHT_DP = 150f
        private const val MAX_BANNER_HEIGHT_FRACTION = 0.20f
    }

    private var bannerView: View? = null

    /**
     * Invoked once UMP has finished resolving consent, so the caller can re-evaluate whether the
     * privacy-options entry point should be visible.
     *
     * [isPrivacyOptionsRequired] is meaningless until `requestConsentInfoUpdate` returns, which is
     * after `onCreate` has already run — so the button's visibility cannot be decided once at
     * start-up and left alone. Docs/Ads.md §7.7.
     *
     * In preview mode nothing ever calls this; the button simply stays hidden.
     */
    var onConsentResolved: (() -> Unit)? = null

    // ──────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────

    /**
     * Entry point, called once from `MainActivity.onCreate`.
     *
     * When the real SDK lands this is where UMP consent resolution and off-main-thread
     * `MobileAds.initialize` go, with [attachBanner] invoked only once `canRequestAds()` is
     * true (Docs/Ads.md §7.3). In preview mode there is nothing to consent to, so we attach
     * straight away.
     */
    fun start() {
        attachBanner()
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
        bannerView?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
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
     * REPLACE at Docs/Ads.md Appendix C task B3 with:
     *
     *     ::consentInformation.isInitialized &&
     *         consentInformation.privacyOptionsRequirementStatus ==
     *             ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
     *
     * Returns false in preview mode: there is no consent SDK yet, so there is nothing to reopen
     * and the button correctly stays hidden.
     */
    fun isPrivacyOptionsRequired(): Boolean = false

    /**
     * Reopens Google's consent form so the user can change or withdraw their choice.
     *
     * Note this is **not** a link to a web page — UMP renders Google's own form inside the app,
     * and the choice it records is what the ad request reads. A URL cannot do this.
     *
     * REPLACE at Docs/Ads.md Appendix C task B3 with:
     *
     *     UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
     *         if (formError != null) Log.w(TAG, "Privacy form: ${'$'}{formError.message}")
     *     }
     *
     * Unreachable in preview mode — [isPrivacyOptionsRequired] is false, so the only caller is
     * never shown.
     */
    fun showPrivacyOptionsForm() {
        Log.d(TAG, "showPrivacyOptionsForm(): no consent SDK in this build (Appendix C, B3)")
    }

    fun destroy() {
        bannerView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            // Real AdView: also call view.destroy() here.
        }
        bannerView = null
    }

    // ──────────────────────────────────────────────
    // Banner — reserve space first, then fill it
    // ──────────────────────────────────────────────

    /**
     * Installs the preview banner synchronously.
     *
     * Deliberately does **not** wait for a layout pass. The placeholder is MATCH_PARENT wide, so
     * it needs no measured width, and gating the insertion on a layout callback is a silent
     * failure mode — if the callback is missed the frame just collapses to its padding with no
     * error anywhere.
     *
     * The real SDK *does* need the measured inner width to pick an adaptive size, so that version
     * wraps the marked block below in `adFrame.doOnLayout { … }` (androidx.core.view). Keep the
     * `minimumHeight` reservation outside that callback either way, so the slot is the right size
     * from the very first frame.
     */
    private fun attachBanner() {
        if (activity.isFinishing || activity.isDestroyed) return
        if (bannerView != null) return

        val density = activity.resources.displayMetrics.density
        val heightPx = (resolveBannerHeightDp() * density).toInt()

        // Fixed space allocation: reserve the slot before the creative arrives, so nothing
        // on screen shifts when it does.
        adFrame.minimumHeight = heightPx + adFrame.paddingTop + adFrame.paddingBottom

        // ─────────────────────────────────────────────────────────────────
        // REPLACE THIS BLOCK with the real SDK (Docs/Ads.md §7.3):
        //
        //   adFrame.doOnLayout {
        //       val innerDp = ((adFrame.width - adFrame.paddingLeft - adFrame.paddingRight)
        //           / density).toInt()
        //       val adSize = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, innerDp)
        //       val view = AdView(activity).apply { layoutParams = ... }
        //       adFrame.addView(view); bannerView = view
        //       view.loadAd(BannerAdRequest.Builder(unitId, adSize).build(), callback)
        //   }
        //
        // Everything above and below this block is already final.
        // ─────────────────────────────────────────────────────────────────
        val view = DummyBannerView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heightPx
            )
        }
        adFrame.addView(view)
        bannerView = view
        // ─────────────────────────────────────────────────────────────────

        Log.d(TAG, "Plinth preview banner attached: height=${heightPx}px, " +
            "frame reserved=${adFrame.minimumHeight}px, children=${adFrame.childCount}")
    }

    /** Nominal height, clamped to the format's ceiling for this device. */
    private fun resolveBannerHeightDp(): Float {
        val dm = activity.resources.displayMetrics
        val screenHeightDp = dm.heightPixels / dm.density
        val ceiling = minOf(MAX_BANNER_HEIGHT_DP, screenHeightDp * MAX_BANNER_HEIGHT_FRACTION)
        return minOf(NOMINAL_BANNER_HEIGHT_DP, ceiling)
    }
}
