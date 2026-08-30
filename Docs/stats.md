# EdgeCase — Project Status & Blueprint

> **Last Updated:** 2026-08-30
> **Project Root:** `/Users/anumey/Work/Android/EdgeCase`
> **Package:** `com.dicereligion.edgecase`
> **App Name:** EdgeCase
> **Version:** 1.4.1 (versionCode 3; the UI label reads `ΕΚΔ. 1.4.1`, sourced from `BuildConfig.VERSION_NAME`)
>
> **Latest change (2026-08-30) — ads are real: B1, B2, B3 and B4 all landed.**
> The Plinth stopped being a preview. The GMA Next-Gen SDK and UMP are on the classpath, a real
> `AdView` serves the band, and UMP resolves consent before any ad is requested. What changed:
>
> - **B1 — dependencies & manifest.** `ads-mobile-sdk:1.4.0` + `user-messaging-platform:4.0.0`;
>   `APPLICATION_ID` meta-data reading `@string/admob_app_id`; ad IDs resolved **per build type**,
>   so a debug build can never reach a live unit. Needed `resValues = true` — AGP 9 gates
>   `resValue` exactly as it gates `buildConfig`, which `Docs/Ads.md` §7.2 does not mention.
> - **B2 — the real banner.** `AdHost.attachBanner()` builds a large anchored adaptive `AdView`
>   from `adFrame`'s measured inner width, with `MobileAds.initialize` off the main thread. Space
>   is reserved **twice** — a nominal figure before layout so the well never pops, then the exact
>   `AdSize` — which is one step beyond §7.3, whose single in-callback reservation would let the
>   plinth grow on launch. The ADVERTISING credit on the Credits screen is un-hidden.
> - **B3/B4 — consent.** `requestConsentInfoUpdate` on every launch → `loadAndShowConsentFormIfRequired`
>   → the `canRequestAds()` gate on all three paths into `initializeAndLoad()`. Both consent stubs
>   have real bodies, and `showPrivacyOptionsForm` re-fires `onConsentResolved` on dismissal because
>   withdrawing consent can change the requirement status. **This closes the one published-policy
>   commitment the code had not met** (§9).
>
> **Two things are done but unproven, and both gate the release.** The EEA consent path has never
> rendered — the test device reports `NOT_REQUIRED`, so the form and the AD CONSENT slab have not
> been seen (Appendix C, group C). And the banner measured **411×128dp** on a Pixel 9 Pro XL, not
> the ~100dp every document assumed, so the Plinth's real cost is **174dp, not 146dp** and the
> small-screen pass must be re-run against it (§6.1).
>
> **Verify SDK symbols against the artifact, not `Docs/Ads.md` §7.3.** Its code listing has drifted
> from the shipped SDK; three errors were found at B2 and are recorded on the B3 row in Appendix C.
> `javap` over the AAR in `~/.gradle/caches` is the authority. UMP 4.0.0, by contrast, matched
> the doc exactly.
>
> **Previous change (2026-08-29) — the AD CONSENT entry point and a legal-basis section.**
> `btnAdConsent` now sits below PRIVACY on the Credits screen, wired to
> `AdHost.showPrivacyOptionsForm()`. It is **not a link** — UMP renders Google's own consent form
> in-process, and the choice it records is what the ad request reads. It is `GONE` in XML and stays
> hidden at runtime unless `AdHost.isPrivacyOptionsRequired()` is true, which is the EEA, the UK,
> Switzerland and the applicable US states only; until the consent SDK lands (Appendix C, **B3**)
> that is always false, so nothing is user-visible yet. Both AdHost methods and the
> `onConsentResolved` callback exist with final signatures and stub bodies, so B4 is a body swap.
> Verified on device with the flag forced true, at native and 360×640dp, then reverted.
>
> The hosted policy also gained a **legal-basis** section (§6.3), the one concrete GDPR Article 13
> gap in the house template; the same section was backported to the live Mach2 policy, whose
> effective date moved to 29 August 2026 as its own §11 requires. **EdgeCase §6 renumbered** —
> the consent control is now cited as §6.2/§6.4.
>
> **Before that (2026-08-29) — the legal surface, written and audited against this code.**
> EdgeCase's privacy policy and data-deletion page now exist, hosted on the Anumey's Lair site
> beside the Mach2 and BOTCH policies:
>
> - **`https://anumey.xyz/legal/edgecase/privacy`** — already wired into `strings.xml`, so the
>   Credits screen's PRIVACY button opens the real document.
> - **`https://anumey.xyz/legal/edgecase/delete-data`** — required because the Data safety form
>   will declare collection (Advertising ID); Google demands a *Delete data URL* whenever it does.
>
> Both were **written against this source tree, not adapted from the sibling apps**, and every
> factual claim was checked against the code. Three claims in the first draft were wrong and were
> corrected — see the *Claims the policy makes about this code* note in §9.
>
> **The policy is now a constraint on the code.** Six specific properties are asserted publicly;
> breaking any of them silently makes a published legal document false. They are listed in §9.
>
> **Previous change (2026-08-29) — the Credits screen.** The main menu's third slab was a
> `DUMMY` stub that only raised a toast. It is now **CREDITS**, opening a fourth virtual screen:
>
> - **Attributions** — Dice Religion (the maker), the two bundled OFL fonts, and the AndroidX
>   libraries. An **ADVERTISING** block is written but `gone`: the Plinth still holds a placeholder,
>   so crediting AdMob today would be untrue. It goes visible with the real SDK (Appendix C, B2).
> - **The Seal** — the 512×512 gold line-art Dice Religion mark set in a stone frame
>   (`bg_dev_seal.xml`), tapping through to the Play Store developer page. The frame is a deliberate
>   third visual register: the slab reads raised, the Plinth reads recessed, and this reads as a
>   raised frame around a dark niche.
> - **PRIVACY** — a slab button beside BACK, opening the privacy policy.
> - `url_privacy_policy` now points at the **real, written** policy at
>   `https://anumey.xyz/legal/edgecase/privacy`; `url_developer_page` is still a placeholder.
> - `applyStoneButtonBehavior` is now generic over `View` so the Seal presses like a slab; the body
>   scrolls with a fading edge while the action bar and the Plinth stay put. Verified at
>   360×640dp — every element reachable, nothing clipped.
>
> **Before that (2026-08-29) — the A track.** Overlay suspension while the Activity is foreground,
> a two-row floor for the Altar, dust/crack effects raised above the slab buttons, 14 instrumented
> tests, dead-resource pruning and palette reconciliation, and backup rules for `EdgeCasePrefs`.
> See Appendix C group A.
>
> **Before that (2026-08-29) — the Ad Plinth, preview build.** The bottom ad band from
> `Docs/Ads.md` §5–§6 is now built and running, with a **placeholder banner** in place of a real ad.
> There is **no ad SDK, no new dependency, no manifest change, and no AdMob account** involved —
> this is a visual/spatial preview so the band's footprint and inertness can be judged before any
> integration work. What landed:
>
> - **`AdHost`** — the single owner of every ad concern, with the final public shape (`start()` /
>   `destroy()` / `setAdVisible()`). Swapping in the real SDK is a change to one marked block.
> - **`DummyBannerView`** — a light-coloured placeholder that reports its own measured dp size.
> - **The Plinth** — `layout_ad_plinth.xml` + `bg_ad_plinth.xml`: meander separator, a 10dp dead
>   gap, and a recessed obsidian well. Nothing in it is clickable, focusable, or animated.
> - **`activity_main.xml` restructured** into a vertical column so the band is pinned below every
>   screen and never scrolls or moves.
> - **Banner hidden under modal dialogs** — both dialogs now call `setAdVisible(false)`, because a
>   dialog dims the ad *and* lands its action row beside it.
> - **Two small-screen defects found and fixed** (see §10): below roughly 800dp of screen height the
>   plinth's ~146dp cost was clipping buttons off the main menu and the Shortcuts action bar.
>
> **Follow-up (same day) — the "A track" from Appendix C is complete.** Six housekeeping and
> correctness items, none of which need an AdMob account:
>
> - **A1 — overlay suspension.** The sliver now detaches while `MainActivity` is in the foreground
>   and returns when you leave. This was `Docs/Ads.md` Phase 5 and is the app's single biggest
>   ad-compliance item; it also stops the fang floating over the app's own settings screens.
> - **A2 — the Altar keeps two draggable rows** on short screens via `altar_min_height`.
> - **A3 — dust and crack effects now draw above the slab buttons** rather than behind them.
> - **A4 — 14 instrumented tests** for `SliverShape` and `SliverConfig`, all passing.
> - **A5 — dead resources pruned**, and 18 colour literals in the custom views replaced with
>   `@color` references. `Theme.EdgeCase` is now actually applied by the manifest.
> - **A6 — backup rules configured** for `EdgeCasePrefs`, so a reinstall keeps the user's setup.
>
> **Previous revision (2026-08-29):** Full re-audit of the source tree. The previous revision of this
> document described **v1.3.5** and stated that the "Obsidian Serpent" overhaul was *planned but not
> implemented*. That is no longer true — **the overhaul shipped as v1.4.0/v1.4.1**, and its blueprint
> (`Docs/NewTheme.md`) has been retired to `Docs/Legacy/`. Everything below now describes the code as
> it actually stands. The substantive changes since the last revision:
>
> - **Three new custom views:** `ObsidianCrackView` (animated fractured-obsidian background with
>   pulsing emerald gems, on every screen), `CrackFlashView` (slab fractures at the touch point),
>   and `ServiceEyeView` (the Serpent's Eyes service indicator in the main-menu lintel).
> - **Shared temple-lintel header** (`layout_temple_header.xml`) replacing the three bespoke headers.
> - **Bundled fonts** — Cinzel Black and GFS Neohellenic — driving three new text styles.
> - **Predictive-back migration:** the legacy `onBackPressed()` override is gone, replaced by an
>   `OnBackPressedCallback` enabled only on sub-screens. This **fixes** the old limitation #11.
> - **Configurable app-drawer size:** `trayWidthDp`/`trayHeightDp` joined `SliverConfig`, severing the
>   old "tray height = sliver height × 7" coupling. Style keys went from 13 to 15.
> - **Expanded positioning canvas** (fit-inside sizing, Greek-key restricted zones, chiseled pediment)
>   plus a draggable **tracking arrow** that stays visible at 0% sliver opacity.
> - **App-list preloading** on a background thread so the Shortcuts screen opens instantly.
> - App-wide de-rounding, blocky limestone slab buttons, distinct Start/Stop button treatments,
>   gem-socket checkboxes and SeekBar thumbs, serpent pillars, twin-fang dividers.
>
> **Also new since the last revision:** `Docs/Ads.md` (2026-08-28) — a full AdMob integration plan
> written against this codebase. *(It was a plan when written; groups A and B1–B4 have since been
> built. See Appendix B and Appendix C.)*

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture & Blueprint](#2-architecture--blueprint)
3. [Complete Directory Tree](#3-complete-directory-tree)
4. [Build Configuration](#4-build-configuration)
5. [Source Code — Full Reference](#5-source-code--full-reference)
   - [5.1 MainActivity.kt](#51-mainactivitykt)
   - [5.2 SidebarService.kt](#52-sidebarservicekt)
   - [5.3 ShortcutStateManager.kt](#53-shortcutstatemanagerkt)
   - [5.4 AppInfoData.kt](#54-appinfodatakt)
   - [5.5 ActiveShortcutsAdapter.kt](#55-activeshortcutsadapterkt)
   - [5.6 AvailableAppsAdapter.kt](#56-availableappsadapterkt)
   - [5.7 ShortcutDragCallback.kt](#57-shortcutdragcallbackkt)
   - [5.8 ArcSliverView.kt](#58-arcsliverviewkt)
   - [5.9 PositioningView.kt](#59-positioningviewkt)
   - [5.10 DustParticleView.kt](#510-dustparticleviewkt)
   - [5.11 SliverConfig.kt](#511-sliverconfigkt)
   - [5.12 SliverShape.kt](#512-slivershapekt)
   - [5.13 SliverCustomizeDialog.kt](#513-slivercustomizedialogkt)
   - [5.14 SliverPreviewView.kt](#514-sliverpreviewviewkt)
   - [5.15 LabeledSeekBar.kt](#515-labeledseekbarkt)
   - [5.16 ObsidianCrackView.kt](#516-obsidiancrackviewkt)
   - [5.17 CrackFlashView.kt](#517-crackflashviewkt)
   - [5.18 ServiceEyeView.kt](#518-serviceeyeviewkt)
   - [5.19 AdHost.kt](#519-adhostkt)
   - [5.20 DummyBannerView.kt](#520-dummybannerviewkt)
6. [Resources — Complete Reference](#6-resources--complete-reference)
   - [6.1 Layouts](#61-layouts)
   - [6.2 Drawables](#62-drawables)
   - [6.3 Values (Colors, Dimensions, Strings, Styles, Themes)](#63-values)
   - [6.4 Fonts](#64-fonts)
   - [6.5 Mipmaps & App Icon](#65-mipmaps--app-icon)
   - [6.6 XML Configuration](#66-xml-configuration)
7. [Feature Inventory](#7-feature-inventory)
8. [Data Flow & State Management](#8-data-flow--state-management)
9. [Permissions](#9-permissions)
10. [Known Limitations & Future Work](#10-known-limitations--future-work)

---

## 1. Project Overview

EdgeCase is an Android edge-launcher application themed with a **Hellenic / Obsidian Serpent**
aesthetic. It provides a persistent, floating sidebar overlay (the "Sliver") that lives on the left or
right screen edge; swiping it inward reveals a tray of shortcut icons for launching apps. A separate
configuration activity lets the user choose which apps appear, reorder them, reposition the sliver, and
customize the sliver's appearance and geometry.

### Core Concept

- **Sliver (Fangs)** — a single continuous shape rendered at the screen edge. The spine (screen-edge
  side) is a flat vertical line; the inward-facing edge carries two sharp fang protrusions with a
  central recess ("gums") between them. It is fully user-customizable: color (default grey `#808080`
  or a custom hue), opacity, eight fang-geometry knobs, and its size (default 27dp × 38dp) — all
  persisted and hot-applied to the live overlay. Defaults reproduce the original 50%-grey angular
  shape. See `SliverConfig`/`SliverShape` (§5.11–5.12) and `Docs/SliverAnatomy.md`.
- **Tray (App Drawer)** — a scrollable panel that unfurls (scales in from the edge) when the sliver is
  swiped. Contains desaturated app icons (20% desaturation for the ancient-theme look) over a serpent-
  scale backdrop. Tapping an icon launches the app. **Its width and height are user-configurable**
  (defaults 80dp × 266dp).
- **Configuration** — a four-screen activity (Main Menu → Shortcuts / Positioning / Credits). The
  Positioning screen also hosts the **Customize Sliver** popup; the Credits screen carries the
  attributions, the Play Store link and the privacy-policy link.
- **The Plinth** — a persistent band pinned below all four screens, holding one banner slot inside
  a recessed stone well. Currently filled with a placeholder (§5.19–5.20). It never scrolls, never
  moves between screens, and nothing in it responds to touch.
- **Living theme** — every screen sits on `ObsidianCrackView`: fractured black obsidian with emerald
  gems pulsing inside the cracks. Buttons are cracked-limestone slabs that fracture under the thumb.
  Nothing in the visual language is rounded; every corner is cut stone.

---

## 2. Architecture & Blueprint

```
┌─────────────────────────────────────────────────────────────────┐
│                         MainActivity                            │
│      (AppCompatActivity, 4 virtual screens via visibility)      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────┐  ┌───────────────┐  ┌────────────────────────┐  │
│  │ Screen 1   │  │ Screen 2      │  │ Screen 3               │  │
│  │ MAIN MENU  │  │ SHORTCUTS     │  │ POSITIONING            │  │
│  │            │  │               │  │                        │  │
│  │ temple     │  │ temple lintel │  │ temple lintel          │  │
│  │ lintel     │  │ "SHORTCUTS"   │  │ "SLIVER POSITION"      │  │
│  │ + Serpent's│  │┌─────────────┐│  │┌──────────────────────┐│  │
│  │   Eyes     │  ││ Altar (0.38)││  ││ PositioningView      ││  │
│  │            │  ││ drag-reorder││  ││ marble stele +       ││  │
│  │ • SHORTCUTS│  │└─────────────┘│  ││ draggable sliver +   ││  │
│  │ • POSITION │  │  twin fangs   │  ││ tracking arrow       ││  │
│  │ • CREDITS  │  │┌─────────────┐│  │└──────────────────────┘│  │
│  │ ~ divider  │  ││Archives(.42)││  │ tvPositionInfo         │  │
│  │ • START    │  │└─────────────┘│  │ [BACK] [CUSTOMIZE]     │  │
│  │ • STOP     │  │ [BACK][SAVE]  │  │        └→ dialog       │  │
│  └────────────┘  └───────────────┘  └────────────────────────┘  │
│  ┌──────────────────────────────┐                               │
│  │ Screen 4  CREDITS            │                               │
│  │ temple lintel "CREDITS"      │  the body scrolls; the action │
│  │┌────────────────────────────┐│  bar and the Plinth stay put  │
│  ││ DICE RELIGION — prose      ││                               │
│  ││ [ Seal ] ──→ Play Store    ││                               │
│  ││  twin fangs                ││                               │
│  ││ LETTERING / LIBRARIES      ││                               │
│  │└────────────────────────────┘│                               │
│  │ [BACK] [PRIVACY] ──→ browser │                               │
│  └──────────────────────────────┘                               │
│                                                                 │
│  Every screen: ObsidianCrackView backdrop + serpent pillars     │
│  Main menu only: DustParticleView + CrackFlashView overlays     │
├─────────────────────────────────────────────────────────────────┤
│  THE PLINTH  (outside screenContainer — showScreen() never       │
│               touches it; hidden under modal dialogs)           │
│   meander trim → 10dp dead gap → recessed well → banner slot    │
└─────────────────────────────────────────────────────────────────┘

                    ║ (SharedPreferences: EdgeCasePrefs)
                    ║

┌─────────────────────────────────────────────────────────────────┐
│                        SidebarService                           │
│         (Foreground Service, TYPE_APPLICATION_OVERLAY)          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐         ┌──────────────────────────────┐   │
│  │  ArcSliverView  │  ────→  │  Tray (LinearLayout)         │   │
│  │  config-sized   │  swipe  │  config-sized (80×266 dflt)  │   │
│  │  (27×38 dflt)   │         │                              │   │
│  │  • Sharp fangs  │         │  • Meander border (inward)   │   │
│  │  • Config fill  │         │  • Serpent-scale backdrop    │   │
│  │  • Swipe detect │         │  • Desaturated icons         │   │
│  │  • Gesture excl.│         │  • Unfurl animation          │   │
│  └─────────────────┘         └──────────────────────────────┘   │
│                                                                 │
│  companion object { @Volatile var isRunning }  ← read by the    │
│                                                  Serpent's Eyes │
│  Hot-reload via Intents:                                        │
│  • ACTION_UPDATE_SHORTCUTS → refreshTrayUiElements()            │
│  • ACTION_UPDATE_POSITION  → applySliverUpdate()                │
│  • ACTION_UPDATE_STYLE     → applySliverUpdate()                │
│    (in-place: recolor + updateViewLayout, no recreate)          │
└─────────────────────────────────────────────────────────────────┘
```

The Sliver's appearance and geometry come from a `SliverConfig` loaded from prefs. `ArcSliverView`
(live) and both previews (`PositioningView`, `SliverPreviewView`) render through the single builder
`SliverShape.buildPath`.

### Data Model

```
AppInfoData
├── appName: String
├── packageName: String
└── icon: Drawable

ShortcutStateManager
├── allApps: List<AppInfoData>          (immutable, all installed launchable apps)
├── altarItems: MutableList<AltarItem>  (working set during editing)
│   └── AltarItem(packageName, isSelected)
└── committedList: List<String>         (snapshot from SharedPreferences)
```

### SharedPreferences Keys (`EdgeCasePrefs`)

All 19 keys live in one file, written by `MainActivity`/`ShortcutStateManager`/`SliverConfig` and read
by both the activity and the service.

| Key | Type | Description |
|-----|------|-------------|
| `saved_shortcuts_order` | String (CSV) | Ordered list of selected package names |
| `saved_shortcuts` | Set<String> | Legacy set of selected package names (written for compatibility) |
| `sliver_side` | String | `"left"` or `"right"` |
| `sliver_y_bias` | Float | 0.0–1.0, vertical position within the valid zone |
| `sliver_opacity` | Float | 0.0–1.0 overall sliver transparency |
| `sliver_color_mode` | String | `"DEFAULT"` (grey) or `"CUSTOM"` (hue) |
| `sliver_color_hue` | Float | 0–360 hue (used when mode = CUSTOM) |
| `sliver_t1_thickness` / `sliver_t2_thickness` | Float | top/bottom fang thickness (fraction) |
| `sliver_t1_length` / `sliver_t2_length` | Float | top/bottom fang inward length (fraction) |
| `sliver_t1_tipy` / `sliver_t2_tipy` | Float | top/bottom fang tip vertical position (angle) |
| `sliver_gums_depth` | Float | gums/bridge depth (fraction) |
| `sliver_gap` | Float | gap between the fangs (fraction) |
| `sliver_width_dp` / `sliver_height_dp` | Float | sliver size in dp (defaults 27 / 38) |
| `tray_width_dp` / `tray_height_dp` | Float | app-drawer size in dp (defaults 80 / 266) |

The 15 style/geometry keys (`sliver_opacity` … `tray_height_dp`) are read and written exclusively
through the `SliverConfig` model (§5.11). The four remaining keys are handled directly by
`MainActivity` (position) and `ShortcutStateManager` (shortcuts).

### Sliver Config Data Model

```
SliverConfig  (persisted in EdgeCasePrefs; defaults reproduce the original look)
├── opacity: Float                     (0..1, default 0.5)
├── colorMode: ColorMode {DEFAULT|CUSTOM}
├── customHue: Float                   (0..360, default 210)
├── tooth1Thickness / tooth2Thickness  (default 0.114 / 0.113)
├── tooth1Length / tooth2Length        (default 0.60 / 0.60)
├── tooth1TipY / tooth2TipY            (default 0.20 / 0.80)
├── gumsDepth                          (default 0.07)
├── gap                                (default 0.44)
├── widthDp / heightDp                 (default 27 / 38)
└── trayWidthDp / trayHeightDp         (default 80 / 266)   ← drawer, NOT a sliver property
    • baseColor()  → grey or HSV(hue,1,1)
    • fillColor()  → baseColor with alpha = opacity·255
```

**Drawer-height migration.** `trayHeightDp` deliberately has no naive default on first read. If the
`tray_height_dp` key is absent, `load()` seeds it from the legacy formula `sliver_height_dp × 7`, so an
existing install that had enlarged its sliver keeps the drawer it already had. After the first Apply
the key exists and the coupling is severed permanently.

### Inter-Component Communication

```
MainActivity ──startService(action intent)──→ SidebarService
             ←──SidebarService.isRunning (static)──
             ──→MainActivity.isForeground (static)──
```

Commands travel one way via `startService()` with action-bearing Intents, processed in
`onStartCommand()`. Two `@Volatile` statics carry state the other direction:
`SidebarService.isRunning`, which `MainActivity.onResume()` reads to sync the Serpent's Eyes, and
`MainActivity.isForeground`, which `SidebarService.onCreate()` reads so a service started *from* the
settings screen does not attach its overlay on top of our own UI.

`ACTION_UPDATE_STYLE` (sent when the Customize dialog is applied) and `ACTION_UPDATE_POSITION` both
route to `applySliverUpdate()`, which updates the existing overlay view in place.

---

## 3. Complete Directory Tree

```
EdgeCase/
├── build.gradle.kts                          # Root build script (AGP + Kotlin plugins, apply false)
├── settings.gradle.kts                       # Project settings, repositories, module include
├── gradle.properties                         # JVM args, Kotlin code style
├── local.properties                          # Local SDK path (machine-specific, git-ignored)
│
├── gradlew, gradlew.bat                      # Gradle wrapper scripts
├── gradle/
│   ├── gradle-daemon-jvm.properties          # JDK path for Gradle daemon
│   ├── libs.versions.toml                    # Version catalog (AGP 9.2.1, Kotlin 2.2.10)
│   └── wrapper/                              # Gradle wrapper jar + properties
│
├── Docs/
│   ├── stats.md                              # ← this document
│   ├── Ads.md                                # AdMob integration plan (2026-08-28, not implemented)
│   ├── Dimensions.md                         # Stable ID/dimension addressing per page & element
│   ├── SliverAnatomy.md                      # Fang geometry deep-dive (knobs → vertices)
│   ├── Publisher.md                          # Google Play publication roadmap
│   ├── fonts_licenses/
│   │   ├── Cinzel-OFL.txt
│   │   └── GFSNeohellenic-OFL.txt
│   └── Legacy/                               # Superseded planning docs
│       ├── NewTheme.md                       # v1.4.0 Obsidian Serpent blueprint (now IMPLEMENTED)
│       ├── EdgeCaseTD.md
│       ├── EdgecaseTheme.md
│       ├── EdgeNextPDP.md
│       └── IMPLEMENTATION_PLAN.md
│
└── app/
    ├── build.gradle.kts                      # Module build config
    ├── proguard-rules.pro                    # ProGuard rules (stock, unused)
    ├── .gitignore                            # Ignores /build
    │
    └── src/
        ├── test/java/com/dicereligion/edgecase/          # (empty — no JVM unit tests)
        ├── androidTest/java/com/dicereligion/edgecase/
        │   ├── SliverShapeTest.kt                        # Fang geometry contract (6 tests)
        │   └── SliverConfigTest.kt                       # Prefs + colour contract (8 tests)
        └── main/
            ├── AndroidManifest.xml
            │
            ├── java/com/dicereligion/edgecase/
            │   ├── ActiveShortcutsAdapter.kt
            │   ├── AdHost.kt                     # Owns every ad concern: banner + UMP consent
            │   ├── AppInfoData.kt
            │   ├── ArcSliverView.kt
            │   ├── AvailableAppsAdapter.kt
            │   ├── CrackFlashView.kt             # Slab fracture-flash on button press
            │   ├── DummyBannerView.kt            # DEAD since B2 — delete at B5
            │   ├── DustParticleView.kt
            │   ├── LabeledSeekBar.kt             # Reusable label+slider+value row
            │   ├── MainActivity.kt
            │   ├── ObsidianCrackView.kt          # Animated obsidian/emerald-gem background
            │   ├── PositioningView.kt
            │   ├── ServiceEyeView.kt             # Serpent's Eye service-state indicator
            │   ├── ShortcutDragCallback.kt
            │   ├── ShortcutStateManager.kt
            │   ├── SidebarService.kt
            │   ├── SliverConfig.kt               # Sliver + drawer model, prefs I/O
            │   ├── SliverCustomizeDialog.kt      # "Customize Sliver" popup controller
            │   ├── SliverPreviewView.kt          # Live sliver preview inside the dialog
            │   └── SliverShape.kt                # Shared parametric fang-path builder
            │
            └── res/
                ├── drawable/
                │   ├── bg_ad_plinth.xml              # Recessed well behind the banner slot
                │   ├── bg_dark_seaweed_panel.xml
                │   ├── bg_dev_seal.xml               # Stone frame around the Dice Religion mark
                │   ├── bg_dev_seal_pressed.xml       # …hairline lights emerald on press
                │   ├── bg_icon_socket.xml            # Square gem socket for altar app icons
                │   ├── bg_serpent_scales.xml         # Tray backdrop: diamond snakeskin
                │   ├── bg_start_button.xml           # Emerald-tinged limestone slab
                │   ├── bg_start_button_pressed.xml
                │   ├── bg_stone_button.xml           # Cracked limestone slab
                │   ├── bg_stone_button_pressed.xml
                │   ├── bg_stop_button.xml            # Cold obsidian slab
                │   ├── bg_stop_button_pressed.xml
                │   ├── bg_temple_lintel.xml          # Header slab w/ ziggurat corner notches
                │   ├── bg_temple_panel.xml           # Dialog window background
                │   ├── ic_checkbox_gem.xml           # Checked: seated emerald in socket
                │   ├── ic_checkbox_socket.xml        # Unchecked: empty socket
                │   ├── ic_divider_fangs.xml          # Twin-fang lintel divider
                │   ├── ic_gem_thumb.xml              # SeekBar thumb (emerald octagon)
                │   ├── ic_launcher_background.xml
                │   ├── ic_launcher_foreground.xml
                │   ├── ic_meander_border.xml         # Vertical Greek key (tray edge)
                │   ├── ic_meander_horizontal.xml     # Horizontal Greek key (under lintel)
                │   ├── ic_pillar_serpent_left.xml    # Serpent-wrapped Doric column
                │   ├── ic_texture_cracks.xml         # Crack overlay on slab faces
                │   ├── icon_round.png                # 512×512 PNG, the app icon
                │   ├── selector_dev_seal.xml
                │   ├── selector_gem_checkbox.xml
                │   ├── selector_start_button.xml
                │   ├── selector_stone_button.xml
                │   └── selector_stop_button.xml
                │
                ├── drawable-xxhdpi/
                │   └── ic_dice_religion.png          # 512×512 developer mark (gold line-art)
                │
                ├── font/
                │   ├── cinzel_black.ttf              # Roman-inscription capitals (weight 900)
                │   └── gfs_neohellenic.ttf           # Greek display face (Bold)
                │
                ├── layout/
                │   ├── activity_main.xml             # Vertical column: screens + plinth
                │   ├── dialog_customize_sliver.xml
                │   ├── layout_ad_plinth.xml          # The ad band — wholly non-interactive
                │   ├── layout_item_available_app.xml
                │   ├── layout_item_shortcut_tile.xml
                │   ├── layout_screen_credits_container.xml
                │   ├── layout_screen_main_menu.xml
                │   ├── layout_screen_positioning_container.xml
                │   ├── layout_screen_shortcuts_container.xml
                │   └── layout_temple_header.xml      # Shared lintel, included by all 4 screens
                │
                ├── mipmap-anydpi/
                │   ├── ic_launcher.xml               (adaptive icon)
                │   └── ic_launcher_round.xml         (adaptive round icon)
                ├── mipmap-mdpi/    ic_launcher.png, ic_launcher_round.png    (48×48)
                ├── mipmap-hdpi/    ic_launcher.png, ic_launcher_round.png    (72×72)
                ├── mipmap-xhdpi/   ic_launcher.png, ic_launcher_round.png    (96×96)
                ├── mipmap-xxhdpi/  ic_launcher.png, ic_launcher_round.png    (144×144)
                ├── mipmap-xxxhdpi/ ic_launcher.png, ic_launcher_round.png    (192×192)
                │
                ├── values/
                │   ├── colors.xml
                │   ├── dimens.xml
                │   ├── strings.xml
                │   ├── styles.xml
                │   └── themes.xml
                │
                └── xml/
                    ├── backup_rules.xml
                    └── data_extraction_rules.xml
```

---

## 4. Build Configuration

### Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
```

### `settings.gradle.kts`

- **Root project name:** `EdgeCase`
- **Module:** `:app`
- **Plugin repositories:** Google — narrowed by `includeGroupByRegex` to `com.android.*`,
  `com.google.*`, `androidx.*` — then Maven Central and the Gradle Plugin Portal
- **Dependency repositories:** Google and Maven Central, with
  `repositoriesMode = FAIL_ON_PROJECT_REPOS` (modules may not declare their own)
- **Toolchain resolver:** `org.gradle.toolchains.foojay-resolver-convention` v1.0.0

### `gradle/libs.versions.toml` (Version Catalog)

| Identifier | Version |
|---|---|
| AGP | 9.2.1 |
| Kotlin | 2.2.10 |
| AndroidX Core KTX | 1.10.1 |
| AndroidX AppCompat | 1.7.0 |
| AndroidX RecyclerView | 1.3.2 |
| JUnit | 4.13.2 |
| AndroidX Test JUnit | 1.1.5 |
| Espresso Core | 3.5.1 |

### `app/build.gradle.kts`

- **Plugins:** `libs.plugins.android.application` **only**. The Kotlin Android plugin is *not* applied
  in the module — AGP 9 provides built-in Kotlin support. The catalog still declares
  `kotlin-android` and the root script still aliases it `apply false`; both are vestigial.
- **namespace / applicationId:** `com.dicereligion.edgecase`
- **compileSdk:** 36 (with `minorApiLevel = 1`)
- **minSdk:** 30 · **targetSdk:** 36
- **versionCode:** 3 · **versionName:** `"1.4.1"`
- **Java:** 11 (source & target)
- **buildFeatures:** `buildConfig = true` — required by the version label, which reads
  `BuildConfig.VERSION_NAME` rather than a hardcoded string
- **Minification:** disabled (`isMinifyEnabled = false`)
- **testInstrumentationRunner:** `androidx.test.runner.AndroidJUnitRunner` — used by the **14
  instrumented tests** in `app/src/androidTest/` (`SliverShapeTest` 6, `SliverConfigTest` 8; see
  Appendix C, A4). The JVM source set `app/src/test/` is still an empty package tree: both classes
  under test need `Path`, `Color` and `Context`, so they run on a device rather than pulling in
  Robolectric
- **buildFeatures:** also `resValues = true` — **AGP 9 disables `resValue` by default**, exactly as it
  does `buildConfig`, and the per-build-type AdMob IDs are resValues. Without it the build fails at
  configuration time with *"Build Type debug contains custom resource values, but the feature is
  disabled."* `Docs/Ads.md` §7.2 predates this and does not mention it
- **Ad IDs per build type** (B1, `Docs/Ads.md` §7.2). `debug` resolves Google's test IDs
  (`…3940256099942544~3347511713` / `…/9214589741`, the *anchored adaptive* unit); `release` resolves
  the live Dice Religion pair (`…4587702028307036~3708305513` / `…/8470994251`). A debug build can
  therefore never reach a live unit. These IDs are not secrets — they ship in every APK
- **Dependencies:** `androidx.appcompat`, `androidx.recyclerview`, `androidx.core.ktx`,
  **`ads-mobile-sdk:1.4.0`**, **`user-messaging-platform:4.0.0`**;
  `junit` (test); `androidx.espresso.core`, `androidx.junit` (androidTest)

> **The ad stack is complete (B1–B4).** The GMA Next-Gen SDK and UMP are on the classpath, the
> `APPLICATION_ID` meta-data is in the manifest, a real `AdView` serves the Plinth, and UMP gates
> every ad request. The SDK drags in Cronet and `androidx.work` transitively; the debug APK is
> **22 MB**, which R8 (still disabled — `Publisher.md` §2.1) will cut substantially for release.

---

## 5. Source Code — Full Reference

### 5.1 `MainActivity.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/MainActivity.kt` · **655 lines**
**Extends:** `AppCompatActivity`

**Responsibilities:**
- Hosts the four-screen UI via visibility toggling (no fragments)
- Retitles the shared temple lintel per screen, and lights the Serpent's Eyes on the main menu
- Wires slab-button press behaviour (translationY + haptics + dust burst + crack flash)
- Manages the overlay and battery-optimization permission flow; starts/stops the foreground service
- Preloads the installed-app list on a background thread
- Initializes the bipartite Shortcuts screen (Altar + Archives)
- Initializes the Positioning screen, applying the saved `SliverConfig` to the preview
- Opens the **Customize Sliver** dialog and, on Apply, refreshes the preview + hot-reloads the overlay
- Handles back navigation through `OnBackPressedDispatcher` (predictive-back compatible)
- Owns the [AdHost] that fills the Plinth, and hides the banner while a modal dialog is up
- Hands the Credits screen's two outbound URLs to the system (`openUrl`)
- Shows or hides the Credits screen's AD CONSENT slab from UMP's requirement status
  (`syncAdConsentButton`)

**Key State:**

| Field | Type | Purpose |
|---|---|---|
| `screenMainMenu` / `screenShortcuts` / `screenPositioning` / `screenCredits` | View | The four screen containers |
| `stateManager` | ShortcutStateManager? | Altar/Archives dual-list state |
| `altarAdapter` / `archiveAdapter` | adapters | RecyclerView adapters |
| `shortcutsInitialized` / `positioningInitialized` | Boolean | Lazy-init guards |
| `cachedApps` | List<AppInfoData>? | Preloaded app list (guarded by `synchronized(this)`) |
| `appsLoading` | Boolean | True while a background load is in flight |
| `positioningView` | PositioningView? | Custom view for sliver placement |
| `dustView` | DustParticleView? | Particle burst overlay |
| `crackView` | CrackFlashView? | Fracture-flash overlay, layered above the dust |
| `serviceEyes` | MutableList<ServiceEyeView> | The two main-menu lintel eyes |
| `vibrator` | Vibrator? | Haptic engine |
| `adHost` | AdHost? | Owns the Plinth's banner; created in `onCreate`, released in `onDestroy` |
| `currentScreen` | Screen | Drives the back callback's routing |

**Scoped-ID resolution (important).** `layout_temple_header.xml` is `<include>`d by all four screens,
so `tvTempleTitle`, `serviceEyeLeft`, and `serviceEyeRight` each exist **four times** in one view
tree. `MainActivity` therefore resolves them **scoped to a screen** — `screenShortcuts.findViewById(…)`,
never `findViewById(…)` on the Activity, which would return whichever copy the traversal hit first. The
main-menu copy keeps its XML default title (`ΞDGΞCΛSΞ` at `header_title_size`); the three sub-screens
are retitled to "SHORTCUTS" / "SLIVER POSITION" / "CREDITS" at `header_title_size_sub`. The eyes are
`gone` in XML and only the main-menu pair is made `VISIBLE`.

**`applyStoneButtonBehavior` is generic over `View`.** It was typed to `Button`; the Credits screen's
Seal is a `FrameLayout` wrapping an `ImageView` and has to feel identical to a slab, so the helper is
now `<T : View> (view: T): T`. Every existing call site is unchanged.

**`openUrl(url)`** wraps `ACTION_VIEW` with `FLAG_ACTIVITY_NEW_TASK`, so the Play Store or browser
opens in its **own** task rather than stacking inside EdgeCase's history — returning to the app lands
back on the Credits screen. `ActivityNotFoundException` is caught (a device with no browser at all)
and reported as a toast rather than crashing. Leaving the app fires `onPause`, which correctly hands
the edge back to the overlay.

**Screen Routing:**

```kotlin
private enum class Screen { MAIN_MENU, SHORTCUTS, POSITIONING, CREDITS }
```

- `showScreen(screen)` toggles visibility of all four screens, lazily initializes Shortcuts or
  Positioning on first entry (refreshing Shortcuts state on re-entry), and sets
  `backCallback.isEnabled = (screen != Screen.MAIN_MENU)`. Credits needs no lazy init — its content
  is entirely static.

**Back navigation (predictive-back).** `backCallback` is an `OnBackPressedCallback(false)` registered
in `onCreate`. It is **disabled on the main menu**, so the OS handles back natively there (predictive
back-to-home); it is enabled on sub-screens so that the gesture, the 3-button back, and the on-screen
BACK button all route through the same dirty-check logic. `btnBackToMenu` calls
`onBackPressedDispatcher.onBackPressed()`; `btnBackToMenuFromPosition` and
`btnBackToMenuFromCredits` call `showScreen(MAIN_MENU)` directly (neither screen has unsaved state
to guard).

**Button Map:**

| Button ID | Action |
|---|---|
| `btnShortcuts` | Navigate to Shortcuts screen |
| `btnPosition` | Navigate to Positioning screen |
| `btnCredits` | Navigate to Credits screen |
| `btnStartService` | Check permissions → start SidebarService → open the Serpent's Eyes |
| `btnStopService` | `stopService(...)` → close the Serpent's Eyes |
| `btnBackToMenu` | Dispatch back (dirty check → discard dialog) |
| `btnSaveShortcuts` | Commit shortcuts, notify service, toast "CARVED IN STONE" |
| `btnCustomizeSliver` | Open the Customize Sliver dialog |
| `btnBackToMenuFromCredits` | `showScreen(MAIN_MENU)` |
| `btnDeveloperSeal` | `openUrl(url_developer_page)` — the Play Store developer page |
| `btnPrivacyPolicy` | `openUrl(url_privacy_policy)` — the privacy policy, in a browser |
| `btnAdConsent` | `adHost.showPrivacyOptionsForm()` — reopens Google's consent form **in-app**. `GONE` unless `AdHost.isPrivacyOptionsRequired()`, which is always false until B3 |
| `btnBackToMenuFromPosition` | Return to main menu |

**App-list preloading.** `preloadApps()` runs `getInstalledApps()` on a plain `Thread` during
`onCreate`, storing the result in `cachedApps` under a lock. `initShortcutsScreen()` takes the fast
path when the cache is warm; otherwise it starts its own load and populates via `runOnUiThread`. The
list is sorted by lowercased app name.

**Slab Button Behavior (`applyStoneButtonBehavior`):**
- `ACTION_DOWN`: animate `translationY` down by `stone_button_pressed_translation` (4dp) over 80ms;
  haptic (30ms, amplitude 255); burst 6 dust particles; `crackView.crackAt(...)` at the touch point,
  converted from raw screen coordinates into the crack view's own coordinate space
- `ACTION_UP` / `ACTION_CANCEL`: animate `translationY` back to 0 over 120ms
- The touch listener returns `false`, so the button's normal click handling still fires

**Customize hook (`openCustomizeSliverDialog()`):** loads the current `SliverConfig`, shows
`SliverCustomizeDialog`; on Apply it applies the returned config to `positioningView` and sends
`ACTION_UPDATE_STYLE` to the service, then toasts "THE FANG IS FORGED".

**Discard dialog (`showDiscardDialog()`):** an `android.app.AlertDialog` titled
"ABANDON THE UNCARVED?" with "ABANDON" / "KEEP CARVING" buttons, whose window background is forced to
`bg_temple_panel` so no rounded system dialog frame shows.

**Service-state sync.** `onResume()` sets every eye to `SidebarService.isRunning`, so the indicator is
correct after the service dies, is killed by the system, or is stopped from elsewhere.

**Overlay suspension.** `onResume()` sets the static `isForeground` flag and calls
`setOverlaySuspended(true)`; `onPause()` clears the flag and calls `setOverlaySuspended(false)`.
`setOverlaySuspended` early-returns unless `SidebarService.isRunning`, so it only ever *signals* a
live service — it can never start one, and a service the user stopped stays stopped. The static flag
covers the remaining case: a service started while the settings screen is already open reads it in
`onCreate` and skips attaching, with no start-up race to lose.

**Ad wiring.** `AdHost` is constructed against `R.id.adFrame` immediately after `setContentView`
and started; `onDestroy()` releases it. Beyond that lifecycle pair the Activity touches it in only
three places — `setAdVisible` from each of the two dialogs, `showPrivacyOptionsForm` from
`btnAdConsent`, and `isPrivacyOptionsRequired` from `syncAdConsentButton`. Neither `showScreen()`
nor any screen initialiser knows the Plinth exists.

**Dialogs hide the banner.** Both of the app's dialogs call `adHost?.setAdVisible(false)` on open and
restore it from a dismiss listener, so the rule is complete: *any modal over the app hides the
banner.* The reasoning is in §5.19 — a dialog dims the ad and puts its action row beside it.

---

### 5.2 `SidebarService.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SidebarService.kt` · **490 lines**
**Extends:** `Service`

**Responsibilities:**
- Runs a persistent foreground service hosting the edge-overlay UI
- Instantiates two overlay windows: the Sliver (`ArcSliverView`) and the Tray
- Handles the swipe → tray-unfurl transition, and tray dismiss → sliver restore
- Loads shortcut icons from SharedPreferences and applies the 20% desaturation filter
- Hot-reloads shortcuts, position, and style via Intent actions
- Publishes its liveness through the static `isRunning` flag

**Companion object:**

| Member | Value |
|---|---|
| `isRunning` | `@Volatile var` — true between `onCreate` and `onDestroy`; read by `ServiceEyeView` |
| `ACTION_UPDATE_SHORTCUTS` | `"com.dicereligion.edgecase.UPDATE_SHORTCUTS"` |
| `ACTION_UPDATE_POSITION` | `"com.dicereligion.edgecase.UPDATE_POSITION"` |
| `ACTION_UPDATE_STYLE` | `"com.dicereligion.edgecase.UPDATE_STYLE"` |
| `ACTION_SUSPEND_OVERLAY` | `"com.dicereligion.edgecase.SUSPEND_OVERLAY"` — detach the windows, keep the service alive |
| `ACTION_RESUME_OVERLAY` | `"com.dicereligion.edgecase.RESUME_OVERLAY"` — re-attach the sliver |
| `CHANNEL_ID` | `"EdgeCaseEngineChannel"` (private) |
| `NOTIFICATION_ID` | `9182` (private) |

**Sliver config:** the service holds a `config: SliverConfig`, loaded from prefs in `onCreate` and
again in `applySliverUpdate()`. It drives both overlays' sizes, the fang geometry (via `ArcSliverView`
+ `SliverShape`), and the fill color/opacity.

**Overlay Window Parameters (Sliver):**
- Type: `TYPE_APPLICATION_OVERLAY`
- Flags: `FLAG_NOT_FOCUSABLE`, `FLAG_LAYOUT_IN_SCREEN`, `FLAG_LAYOUT_NO_LIMITS`,
  `FLAG_WATCH_OUTSIDE_TOUCH`
- Format: `TRANSLUCENT`
- Size: `config.widthDp × config.heightDp` dp (default 27 × 38)
- Gravity: `END|TOP` (right) or `START|TOP` (left)
- Y: mapped from `yBias` [0,1] → vertical range [10%, 90%] of screen height

**Overlay Window Parameters (Tray):**
- Same type/format; flags are the same **minus `FLAG_LAYOUT_NO_LIMITS`**
- Size: `config.trayWidthDp × config.trayHeightDp` dp (default 80 × 266)
- Same gravity as the sliver
- Y: bottom-anchored to the sliver's vertical center —
  `y = (sliverY + sliverHeight/2 − trayHeight).coerceAtLeast(0)`, so a tall drawer cannot float off
  the top of the screen

**Screen height** is read from `windowManager.currentWindowMetrics.bounds.height()` on API 30+
(the app's `minSdk`), with a deprecated `displayMetrics` fallback retained.

**Tray Layout Structure:**
```
LinearLayout (horizontal, root)
├── ImageView (meander border, 12dp wide, 0.7 alpha)  [placed on the INWARD side]
└── ScrollView (background @drawable/bg_serpent_scales, scrollbars off)
    └── LinearLayout (vertical, gravity BOTTOM|CENTER_HORIZONTAL, 8dp vertical padding)
        ├── ImageView (shortcut icon, 48dp, 8dp vertical margins)
        └── ...
```
For a right-side tray the meander is added first (so it sits on the left, facing inward); for a
left-side tray the order is reversed. Several methods rely on this ordering when reaching for the
`ScrollView` by index.

**Tray Icon Behavior:**
- Icons are added in **reverse** order, so shortcut #1 lands at the bottom nearest the thumb; the
  ScrollView is then scrolled to the bottom
- On press: color filter cleared (full saturation restored); haptic 20ms @ amplitude 150
- Launches via `getLaunchIntentForPackage()` with `FLAG_ACTIVITY_NEW_TASK`, then collapses the tray
- Packages that fail to resolve (uninstalled since selection) are silently skipped

**State Transitions:**
- `transitionToExpandedTray()`: remove the sliver window → set `scaleX = 0` with the pivot at the
  screen edge → animate `scaleX` to 1 over 250ms (DecelerateInterpolator) → add the tray window →
  haptic 40ms @ 200
- `transitionToSliverState()`: remove the tray window → re-add the sliver (`addSliverIfNeeded()`)

**In-place update (`applySliverUpdate()`):** handles both `ACTION_UPDATE_POSITION` and
`ACTION_UPDATE_STYLE`. It reloads position + `SliverConfig`, recomputes both windows' params, then
updates the **existing** sliver view via `ArcSliverView.applyConfig(config, side)` +
`windowManager.updateViewLayout(...)` — it does **not** destroy and recreate the overlay. This
replaced an older remove-then-add approach whose race could leave a stale sliver window on screen.
The tray *is* rebuilt from scratch (`assembleTrayView()`) so its size, side, and position match; it is
only attached on swipe, so rebuilding it is cheap and invisible.

**Overlay suspension (`detachOverlayWindows()`).** Removes the sliver and any open tray without
stopping the service, so the foreground notification, the shortcut state and the loaded config all
survive. `onCreate` additionally skips its initial `addSliverIfNeeded()` when
`MainActivity.isForeground` is true. Three independent justifications, per `Docs/Ads.md` §4.3:
compliance (an overlay above an ad is an obstruction, and the sliver can sit at 90% of screen height
— exactly where the Plinth lives), UX (an edge launcher over its own settings screen is noise), and
correctness (the fang otherwise overlaps the `PositioningView` drag canvas).

**Idempotence guards:** `sliverAdded` plus `isAttachedToWindow` checks wrap every add/remove;
`updateViewLayout` and the tray removal are wrapped in `try`/`catch` against window-manager races.

**Desaturation Filter:**
```kotlin
cm.setSaturation(0.8f) // 80% saturation = 20% desaturation
```

**Notification:** channel `EdgeCaseEngineChannel` ("EdgeCase Engine Active", `IMPORTANCE_LOW`);
notification titled "EdgeCase Active" / "Listening for gestures." with
`android.R.drawable.ic_dialog_info`, posted via `startForeground(9182, …)`.

**Lifecycle:** `onStartCommand` returns `START_STICKY`. `onCreate` calls `stopSelf()` if the overlay
permission is not granted. `onDestroy` clears `isRunning` and removes both windows if attached.

---

### 5.3 `ShortcutStateManager.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ShortcutStateManager.kt` · **158 lines**

**Responsibilities:**
- Single source of truth for the dual-list (Altar + Archives) state during shortcut editing
- Loads/persists shortcut order from SharedPreferences
- Provides dirty-checking for the discard-confirmation flow
- Supports atomic commit (Save) and discard operations

**Data Class:**
```kotlin
data class AltarItem(
    val packageName: String,
    var isSelected: Boolean = true
)
```

**Public API:**

| Method | Description |
|---|---|
| `allApps` | Immutable list of all installed launchable apps |
| `altarItems` | Mutable working list shown in the Altar RecyclerView |
| `isActiveShortcut(pkg)` | True if in the Altar AND selected (will survive Save) |
| `isDirty()` | True if the current selection ≠ the committed selection |
| `setFromArchives(pkg, add)` | Add/remove from the Altar via the Archives checkbox |
| `toggleAltarSelection(pos)` | Toggle selected state in the Altar, returns the new value |
| `moveAltarItem(from, to)` | Reorder an Altar item via drag-and-drop |
| `getAppInfo(pkg)` | Look up `AppInfoData` by package name |
| `commit()` | Persist selected items to prefs, evict unselected |
| `discard()` | Reset `altarItems` to the committed state |

**Load/Commit/Discard:** items are only added to the Altar if the package is still installed. Commit
filters to `isSelected`, writes both `saved_shortcuts_order` (ordered CSV) and `saved_shortcuts`
(legacy `Set<String>`), removes unselected items from the visible list, and updates the
`committedList` snapshot. Discard rebuilds `altarItems` from `committedList`.

> Note: `isInAltar(pkg)` was listed in a previous revision of this document; the class does not define
> it. Presence-in-Altar is tested inline where needed.

---

### 5.4 `AppInfoData.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/AppInfoData.kt` · **9 lines**

```kotlin
data class AppInfoData(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)
```

A launchable app with its display name, package identifier, and launcher icon drawable.

---

### 5.5 `ActiveShortcutsAdapter.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ActiveShortcutsAdapter.kt` · **60 lines**
**Extends:** `RecyclerView.Adapter<AltarViewHolder>`

**Purpose:** adapter for the **Altar** — the working set of shortcuts, with drag handles and gem
checkboxes.

**ViewHolder:** `AltarViewHolder`

| Field | Type | Layout ID |
|---|---|---|
| `ivIcon` | ImageView | `R.id.ivAltarIcon` |
| `tvName` | TextView | `R.id.tvAltarName` |
| `tvOrder` | TextView | `R.id.tvOrderNumber` |
| `cbSelect` | CheckBox | `R.id.cbAltarSelect` |
| `dragHandle` | ImageView | `R.id.ivDragHandle` |

**Binding:** order number shown as `position + 1`; name/icon from `stateManager.getAppInfo(...)`;
the checkbox listener is nulled before setting `isChecked` to avoid rebind loops, and reports through
`holder.bindingAdapterPosition`; unselected rows are dimmed to alpha 0.5.

**Drag Support:** `onItemMove(fromPos, toPos)` delegates to `stateManager.moveAltarItem()` then calls
`notifyItemMoved()`.

---

### 5.6 `AvailableAppsAdapter.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/AvailableAppsAdapter.kt` · **50 lines**
**Extends:** `RecyclerView.Adapter<ArchiveViewHolder>`

**Purpose:** adapter for the **Archives** — all installed launchable apps with gem checkboxes.

**ViewHolder:** `ArchiveViewHolder` — `ivArchiveIcon`, `tvArchiveName`, `cbArchiveSelect`.

**Binding:** renders from `stateManager.allApps`; checked state comes from
`stateManager.isActiveShortcut(packageName)`; the listener is nulled before setting checked; toggling
invokes `onToggle(packageName, checked)`.

---

### 5.7 `ShortcutDragCallback.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ShortcutDragCallback.kt` · **68 lines**
**Extends:** `ItemTouchHelper.Callback()`

**Purpose:** long-press drag-to-reorder in the Altar RecyclerView.

- Drag directions: UP and DOWN · Swipe: disabled (removal is via the checkbox)
- Long-press drag: enabled
- Visual feedback: scale to 1.05× over 150ms while dragging; back to 1.0× on idle and in `clearView`

---

### 5.8 `ArcSliverView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ArcSliverView.kt` · **125 lines**
**Extends:** `View`

**Purpose:** the live edge-sliver — one continuous shape with two sharp angular fang protrusions on the
inward-facing edge. It is config-driven: the path comes from `SliverShape.buildPath(...)` and the fill
from `config.fillColor()`. Straight `lineTo` geometry only — no curves, no glow, no border, no pulse.

**Constructor:** `ArcSliverView(context, side, config, onSwipeListener)` — `side` and `config` are
mutable `var`s so the service can retarget the view in place.

**Sizing:** `onMeasure` resolves `config.widthDp × config.heightDp` against the incoming specs;
`onSizeChanged` rebuilds the fang path at the measured size.

**Swipe Detection:**
- Tracks `rawX`/`rawY` (screen coordinates) from `ACTION_DOWN`
- `SWIPE_THRESHOLD_X = 30f` — horizontal delta required, measured **inward** from the edge
  (leftward for a right-side sliver, rightward for a left-side sliver)
- `MAX_SWIPE_DEVIATION_Y = 150f` — vertical tolerance; exceeding it releases the gesture back to the
  system rather than firing
- Fires `onSwipeListener` once per gesture (`trackingSwipe` is cleared on fire)

**`applyConfig(newConfig, newSide)`:** updates appearance/geometry in place — recolors the paint,
rebuilds the path at the current size, then `requestLayout()` + `invalidate()`. This is the hook the
service's in-place hot-reload uses.

**System gesture exclusion:** set by `SidebarService.assembleSliverView()` via
`systemGestureExclusionRects` on every layout change (API 29+), so the sliver's swipe is not stolen by
the system back gesture.

---

### 5.9 `PositioningView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/PositioningView.kt` · **505 lines**
**Extends:** `View`

**Purpose:** a scaled-down phone mockup ("the stele") with a draggable sliver preview, used on the
Positioning screen. The sliver snaps to the left or right edge on release.

**Visual Components:**
1. **Marble stele** — dark slab (`#1A2822`) with **square** corners (`mockupCornerRadius = 0`),
   bordered in Faded Olive Teal (`#3B5249`), 3px stroke
2. **Chiseled pediment** — a 2-step gable + cornice ledge across the top, drawn in the border paint
3. **Restricted zones** — the top and bottom 10% rendered as a **Greek-key (meander) hatch band**
   rather than plain crosshatch; the sliver cannot be placed there
4. **Sliver preview** — built by the shared `SliverShape.buildPath(...)` and filled with
   `sliverConfig.fillColor()`, so it mirrors the live sliver exactly
5. **Tracking arrow ("The Herald")** — an emerald arrow with a dark outline, pointing at the sliver
   from 20dp inward of its deepest fang tip. It remains visible when the sliver's opacity is 0, and
   doubles as a generous drag handle
6. **Particle trail** — Tarnished Silver particles (alpha 120) trailing the sliver while dragging or
   snapping
7. **Instruction text** — "Drag the sliver to reposition", drawn *inside* the bottom restricted band
   (the expanded canvas leaves no room below the stele), where it reads as carved signage

**Geometry (v1.4 expanded canvas):**
- Fit-inside sizing: `mockupW = min(viewW × 0.98, (viewH × 0.98) / 2.1)`, `mockupH = mockupW × 2.1`.
  On tall phones the height cap binds; on short or wide screens the width cap binds. The 2.1 aspect
  holds either way. Side gaps come from the layout's 46dp horizontal margin, not from this view.
- Valid Y zone: the middle 80% of the stele
- Preview size: `previewScale = (mockupW × 0.04) / 27dp`, then `width = previewScale × config.widthDp`
  and `height = previewScale × config.heightDp` — so editing the sliver's dp size changes the
  preview's aspect too (`recalcPreviewSize()`)

**Touch Handling:**
- `ACTION_DOWN`: cancels any running snap; begins a drag if the touch is on the sliver
  (`isTouchOnSliver`, 40px slop) **or** on the tracking arrow (`isTouchOnArrow`, 20dp slop)
- `ACTION_MOVE`: clamps Y to the valid zone; spawns a trail particle every 30ms
- `ACTION_UP`: picks the nearer edge by midpoint and animates the snap
- `ACTION_CANCEL`: ends the drag without snapping

**Snap Animation:** a `ValueAnimator` over 200ms with `DecelerateInterpolator`, interpolating X from
the current position to the target edge. On end it commits `sliverSide`, recomputes `sliverYBias` from
the pixel position, clears the trail, and invokes `onPositionChanged`.

**Particle System:** max 20 particles; random velocity (±2px/frame), alpha −0.03/frame, radius ×0.96
per frame; removed at alpha ≤ 0.

**Public API:** `setSliverPosition(side, yBias)`, `setSliverConfig(cfg)`, read-only `sliverSide` /
`sliverYBias` / `sliverConfig`, and the callback
`onPositionChanged: ((ArcSliverView.Side, Float) -> Unit)?`.

**Cleanup:** `onDetachedFromWindow` cancels the snap animator and clears the trail.

---

### 5.10 `DustParticleView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/DustParticleView.kt` · **104 lines**
**Extends:** `View`

**Purpose:** a dust-particle overlay that bursts when slab buttons are pressed.

- Particle color: Aged Marble (`#F5EFE6`) at alpha 200
- Spawn from the **center of the view** (±10px X, ±5px Y jitter) — not from the touch point
- Random radial velocity (1.5–4.5 px/frame) with a slight upward bias (`vy − 2`)
- Gravity: `vy += 0.5` per frame · Life: 0.4–0.7s · Radius: ×0.98 per frame
- Alpha fades proportionally to remaining life
- Driven by an infinite 600ms `ValueAnimator` assuming ~60fps (`dt = 0.016`), which **cancels itself**
  once the particle list empties; `onDetachedFromWindow` cancels and clears

**Public API:** `burst(count: Int = 6)`

---

### 5.11 `SliverConfig.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SliverConfig.kt` · **134 lines**
**Type:** `data class`

**Purpose:** the single model for the sliver's user-editable appearance and geometry — plus the app
drawer's dimensions — persisted in `EdgeCasePrefs`. Every default reproduces the original hardcoded
look, so an install that never opens the Customize dialog is visually unchanged.

**Fields & defaults:** `opacity` (0.5), `colorMode` (`DEFAULT`/`CUSTOM`), `customHue` (210),
`tooth1/2Thickness` (0.114 / 0.113), `tooth1/2Length` (0.60), `tooth1/2TipY` (0.20 / 0.80),
`gumsDepth` (0.07), `gap` (0.44), `widthDp`/`heightDp` (27 / 38), `trayWidthDp`/`trayHeightDp`
(80 / 266). Geometry values use the normalized fang model in `Docs/SliverAnatomy.md`.

**Helpers:**
- `baseColor()` → opaque grey `#808080` (DEFAULT) or `Color.HSVToColor(hue, 1, 1)` (CUSTOM)
- `fillColor()` → base color with alpha = `opacity·255` (opacity 0 ⇒ fully transparent)
- `save(context)` and companion `load(context)` — read/write the 15 style keys
- data-class `copy()` — used by the dialog for a discardable working copy

**Drawer-height seeding:** `load()` returns `tray_height_dp` when the key exists; otherwise it seeds
from `sliver_height_dp × 7`, reproducing the pre-v1.4 formula for existing installs exactly once.
`trayWidthDp` has no such migration — the old width was a hardcoded 80dp, which is already the default.

---

### 5.12 `SliverShape.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SliverShape.kt` · **50 lines**
**Type:** `object` (singleton)

**Purpose:** the single source of truth for the fang path. `buildPath(path, w, h, side, cfg)` computes
the 8 `(u,v)` vertices from the config knobs and writes them into `path`. All three renderers —
`ArcSliverView` (live), `PositioningView` and `SliverPreviewView` (previews) — call it, eliminating the
four hardcoded L/R copies this replaced.

**Model:** `u` = inward depth (0 = flat spine at the screen edge → 1 = deepest inward reach);
`v` = vertical (0 top → 1 bottom). Per side: `RIGHT → x = w·(1−u)`, `LEFT → x = w·u`; both `y = h·v`.

**Vertices:** `v3/v4 = 0.5 ∓ gap/2` (the gums span, with `gap/2` coerced into `[0.01, 0.48]`);
`v1 = v3 − tooth1Thickness` and `v6 = v4 + tooth2Thickness`, each coerced to stay inside `[0,1]` and
strictly ordered; tips at `(length, tipY)`; the gums wall at `u = gumsDepth`. Because every coordinate
is clamped with its ordering preserved, **no knob combination can invert or self-intersect the shape.**

---

### 5.13 `SliverCustomizeDialog.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SliverCustomizeDialog.kt` · **244 lines**

**Purpose:** controller for the **Customize Sliver** popup — an AppCompat `AlertDialog` hosting
`dialog_customize_sliver.xml`, sized to 92% of screen width, with its window background forced to
`bg_temple_panel` (square frame, no rounded system chrome). It edits a working copy of `SliverConfig`
against a live preview.

**Structure & controls:**
- Live `SliverPreviewView` at the top, mirroring the saved edge side (read from `sliver_side`)
- **APPEARANCE** — opacity slider (0–100%); a Default-grey / Custom `RadioGroup`; Custom reveals a
  rainbow hue `LabeledSeekBar` (its track painted with a 13-stop `GradientDrawable` spectrum, corner
  radius 0) plus a live swatch
- **FANG GEOMETRY** — eight `LabeledSeekBar`s: top/bottom thickness (0.02–0.35), top/bottom length
  (0.10–0.95), top/bottom angle a.k.a. tipY (0.02–0.48 / 0.52–0.98), gums depth (0–0.60),
  gap (0.05–0.90)
- **SIZE (DP)** — width (8–160) and height (12–240) `EditText`s
- **APP DRAWER (DP)** — width (64–200) and height (100–640) `EditText`s
- **Footer** — RESET / CANCEL / APPLY slab buttons

**Behavior.** Every slider and text field writes into the working config. Slider and sliver-size edits
call `preview.setConfig(...)`; the **drawer**-size watchers deliberately do *not* refresh the preview,
because the preview renders the sliver, not the drawer. A `binding` guard suppresses callbacks while
controls are being (re)populated. **Apply** re-parses and clamps all four dp fields, persists via
`working.save(context)`, then invokes `onApplied(working)`. **Reset** copies a fresh `SliverConfig()`
into the working copy (drawer dimensions included) and rebinds — it persists nothing until Apply.
**Cancel** discards.

Entry point: `SliverCustomizeDialog.show(context, initial, onApplied, onDismissed)`; the constructor
is private. `onDismissed` is optional and fires however the dialog closes — Apply, Cancel, back, or an
outside tap — which is what restores the ad banner hidden for the dialog's duration (§5.19).

---

### 5.14 `SliverPreviewView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SliverPreviewView.kt` · **53 lines**
**Extends:** `View`

**Purpose:** a static, scaled preview of the sliver fang for the Customize dialog. It draws
`SliverShape.buildPath` centered and scaled to fit within a 15% margin, preserving the config's
height/width aspect, filled with `config.fillColor()`, mirroring the saved edge `side`. `setConfig(cfg)`
invalidates for instant feedback while sliders move.

---

### 5.15 `LabeledSeekBar.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/LabeledSeekBar.kt` · **95 lines**
**Extends:** `LinearLayout` (compound view, declared directly in XML; children built in `init`)

**Purpose:** a reusable control row — `label | ——slider—— | value` — used for every slider in the
Customize dialog. The label is 104dp wide in Aged Marble at 13sp; the value is 56dp, end-aligned, in
Tarnished Silver at 12sp; the SeekBar takes the remaining width and uses `ic_gem_thumb` (an emerald-cut
octagon) as its thumb.

`configure(label, min, max, value, formatter, onChange)` maps the SeekBar's `0..1000` integer progress
onto a float `[min, max]` and reports only user-driven changes. `setValue()` updates the display
without firing the callback (used by Reset). `seek()` exposes the underlying `SeekBar`, which the
dialog uses to paint the hue-spectrum track.

---

### 5.16 `ObsidianCrackView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ObsidianCrackView.kt` · **277 lines**
**Extends:** `View` (XML-instantiable — takes `AttributeSet`)

**Purpose:** the living temple floor behind **every screen**: fractured obsidian with emerald gems
pulsing inside the cracks.

**Performance model.** Everything static — the obsidian body, four giant conchoidal facets, 90 mineral
speckles, the radial vignette, and the crack lines with their underglow — is rasterized **once** into a
`Bitmap` (`staticLayer`, ARGB_8888) on size change. Per frame, only the gem glow is redrawn: a handful
of hardware-accelerated `RadialGradient` circles plus small octagon paths. `BlurMaskFilter` is
deliberately never used.

**Tunables:** `crackCount = 7`, `gemCount = 11`, `seed = 20260711L` (fixed, so the temple is identical
on every launch), `maxGlowAlpha = 0.55f` (kept ambient so foreground text stays readable).

**Crack generation:** a jagged random walk seeded on a random screen edge, 9–16 steps, with a 25%
chance per step of a short branch splinter. All joins are `Paint.Join.MITER` — sharp kinks, never
smooth curves.

**Gem placement:** each gem is sampled onto a random point along a random crack via `PathMeasure`,
then clamped to the middle 88% of the view so none is cropped. Each carries its own size (4–9dp),
phase, period (2400–4800ms), and resting rotation.

**Per-frame drawing:** for each gem, `pulse = 0.5 + 0.5·sin(...)` drives a three-stop radial halo, an
emerald-cut octagon body whose color lerps between `emerald_deep` and `emerald_gem`, a thin facet
stroke, and — above `pulse > 0.75` — a hot `emerald_core` center pixel.

**Lifecycle:** the 10s infinite `ValueAnimator` starts on attach and on becoming `VISIBLE`, and stops
on detach or on becoming non-visible. Because the screens are toggled by visibility, only the
foreground screen's instance animates. Its clock (`nowMs`) is a frame counter (+16f per frame), not
wall time.

---

### 5.17 `CrackFlashView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/CrackFlashView.kt` · **109 lines**
**Extends:** `View`

**Purpose:** a one-shot fracture flash — when a slab is pressed, 2–3 jagged cracks spider outward from
the touch point and fade over ~300ms. Overlaid full-screen in the main menu's dust container, layered
above `DustParticleView`.

- `crackAt(x, y)` spawns 2–3 paths, each 3–4 straight segments of 12–30dp with ±0.55 rad kinks
- Two strokes per crack: a `#502E8B57` emerald fracture-light underglow at 5px, then a `#020403`
  crack-void line at 2.5px, both `Paint.Join.MITER`
- Alpha on both strokes decays linearly with age over a 0.30s life
- The infinite 300ms `ValueAnimator` self-cancels once no cracks remain; `onDetachedFromWindow`
  cancels and clears

---

### 5.18 `ServiceEyeView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ServiceEyeView.kt` · **155 lines**
**Extends:** `View` (XML-instantiable — takes `AttributeSet`)

**Purpose:** the **Serpent's Eye** — the overlay service's state indicator, one on each flank of the
main-menu lintel.

- **Stopped:** a closed, angular lid — a single dim horizontal slit line in Tarnished Silver
- **Running:** the lid opens into a blocky almond (left corner → top mid → right corner → bottom mid),
  filled with obsidian sclera, and an emerald-cut octagon iris breathes inside it, echoing the
  background gems — three-stop radial halo, body color lerped by the pulse, and a hot `emerald_core`
  center above `pulse > 0.7`

**State & animation:** `setRunning(r)` is a no-op if the state is unchanged; otherwise it flips
`running` and (re)starts the animator. `openFraction` eases toward its target by 18% per frame. The
iris and its halo are drawn only above `openFraction > 0.2`, the sclera and lid outline above 0.05.
When closing completes (`openFraction < 0.01` while stopped) the view snaps to 0, draws once, and
**stops its own animator** — so a closed eye costs nothing. The animator also starts/stops with
attach and visibility.

**Wiring:** `MainActivity` sets both eyes on Start/Stop and re-syncs them in `onResume()` from
`SidebarService.isRunning`.

---

### 5.19 `AdHost.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/AdHost.kt` · **324 lines**

**Purpose:** the single owner of every ad-related concern — the banner *and* the consent flow.
`MainActivity` gets a handful of call sites and no ad logic of its own.

**State: complete (B1–B4, 2026-08-30).** Nothing here is stubbed. A real GMA Next-Gen `AdView`
serves the Plinth, and UMP resolves consent before any ad is requested.

**Public API:**

| Method | Description |
|---|---|
| `start()` | Called once from `onCreate`. Runs the UMP flow, then loads the banner once `canRequestAds()` is true |
| `setAdVisible(visible)` | Flips the banner between `VISIBLE` and `INVISIBLE` while a modal dialog is on screen |
| `destroy()` | Detaches the `AdView` and calls its own `destroy()` |
| `isPrivacyOptionsRequired()` | `privacyOptionsRequirementStatus == REQUIRED`, guarded by `::consentInformation.isInitialized` |
| `showPrivacyOptionsForm()` | `UserMessagingPlatform.showPrivacyOptionsForm(...)`; re-fires `onConsentResolved` on dismissal |
| `onConsentResolved` | Callback property. UMP resolves *after* `onCreate`, so the AD CONSENT button's visibility cannot be decided once at start-up; `MainActivity` re-runs `syncAdConsentButton()` from here |

**The consent flow.** `requestConsentInfoUpdate` runs on **every** launch — consent, and the regime
that applies, can change between sessions. On success it calls `loadAndShowConsentFormIfRequired`;
on failure it logs and continues, because cached consent may still permit ads. Three paths can reach
`initializeAndLoad()` — form dismissed, update failed with usable cache, or cached consent allowing
an immediate request — and **all three check `canRequestAds()` first**. `initializeAndLoad()` is
idempotent (`adRequested`), so overlapping paths are harmless.

The entry point is **not a link** — UMP renders Google's own form in-process, and the choice it
records is what the ad request reads, so no URL can substitute for it. Where UMP reports `REQUIRED`,
offering it is a Google requirement (`Docs/Ads.md` §7.7), and the published privacy policy §6.2/§6.4
tells users the control exists.

**SDK initialisation** runs on a raw `Thread` — Next-Gen ANRs if initialised on the main thread —
matching the codebase's existing idiom (`MainActivity.preloadApps`) rather than adding
kotlinx-coroutines for one call. `sdkInitialized` is a static `AtomicBoolean` because
`MobileAds.initialize` is process-scoped while the Activity can be recreated.

**Sizing.** `AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, widthDp)` — the current,
non-deprecated call (`Docs/Ads.md` §3.8) — computed from `adFrame`'s **measured inner width**.
Measured result on a Pixel 9 Pro XL: **411×128dp**, well above the ~100dp the docs assumed. See §6.1.

**Space reservation happens twice**, and deliberately so. A nominal estimate
(`resolveNominalHeightDp()`, capped at `min(150dp, 20% of height)`) is applied *before* any layout,
so the well is correctly sized from the very first frame; the exact `AdSize` then replaces it inside
the layout callback. `Docs/Ads.md` §7.3 shows only the second, which would let the plinth pop on
launch — the layout shift §3.1 exists to forbid. `layout_ad_plinth.xml` also carries a static
`minHeight` floor, so an unfilled slot holds its space rather than collapsing.

**Why insertion waits for layout now.** The old placeholder was `MATCH_PARENT` and needed no
measurement, so it was attached synchronously. The adaptive size is a function of the frame's real
inner width in dp, so the `AdView` must wait for `doOnLayout`. Consequence worth knowing: **on a
locked device the window never lays out, so no ad loads** — that is expected, not a fault.

**No client-side retry** on a failed load. A failure leaves a correctly sized empty well and the
server-side 60s refresh takes the next attempt; retry loops are an invalid-traffic pattern.

**`logConsentState(phase)`** prints `canRequestAds` / `privacyOptionsRequirementStatus` /
`isConsentFormAvailable` under the `EdgeCaseAds` tag. It is the readout for the EEA acceptance test
(Appendix C, group C), where "did a button appear?" is otherwise hard to distinguish from a layout
bug.

**Why dialogs hide the banner.** Two reasons, both from `Docs/Ads.md`: a dialog dims everything behind
it including the ad, and a dimmed ad is an obstruction whose impressions are not viewable (§3.5); and
a dialog's action row lands close to the ad, bypassing the Plinth's entire buffer budget, which is the
adjacency Google enforces against (§3.2). `INVISIBLE` rather than `GONE` is deliberate — `INVISIBLE`
is one of the three states that explicitly clears the obstruction rule, and it preserves the slot's
height so nothing re-lays out behind the dialog.

**Hard rule:** nothing in this class may ever be constructed from `SidebarService` (`Docs/Ads.md` §4.2).

---

### 5.20 `DummyBannerView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/DummyBannerView.kt` · **106 lines**
**Extends:** `View`

**Dead code as of B2 — nothing references it.** It was the placeholder that let the Plinth's look,
footprint and inertness be judged before any AdMob account existed. A real `AdView` replaced it, so
its only remaining purpose is served. **Delete at Appendix C, B5.**

Retained here only so the deletion is a deliberate act rather than an unexplained absence: it was
drawn light (`#FAFAF7` face, `#C9C4BA` edge) rather than in the EdgeCase palette, because real
creatives are overwhelmingly bright and a temple-toned placeholder would have misrepresented how the
band actually reads against the obsidian frame. It printed its own measured dp size and carried a
stand-in AdChoices corner tag so that space was accounted for too.

---

## 6. Resources — Complete Reference

### 6.1 Layouts

#### `activity_main.xml`
Root: a **vertical `LinearLayout`** (id `rootColumn`) on `@color/obsidian_black` with
`fitsSystemWindows="true"`, holding two children:

1. `screenContainer` — a `FrameLayout` at `height=0dp, weight=1` containing the four screens:
   - `@+id/screenMainMenu` → `layout_screen_main_menu` (visible)
   - `@+id/screenShortcuts` → `layout_screen_shortcuts_container` (gone)
   - `@+id/screenPositioning` → `layout_screen_positioning_container` (gone)
   - `@+id/screenCredits` → `layout_screen_credits_container` (gone)
2. `adPlinth` — an include of `layout_ad_plinth` at `wrap_content`

`fitsSystemWindows` sits on `rootColumn` rather than `screenContainer`, so the system-navigation inset
lands **below** the plinth — the ad must never overlap or fight system navigation. The four screens
keep `match_parent` semantics inside a shorter box, so none of their internals needed changing, and
because the plinth is outside `screenContainer`, `showScreen()` never touches it.

#### `layout_temple_header.xml` — the shared lintel
A vertical `LinearLayout` included by **all four** screens:
- A `FrameLayout` of `header_height` (64dp) backed by `bg_temple_lintel`, containing
  `tvTempleTitle` (`TitleMonolith`, default text `ΞDGΞCΛSΞ` at `header_title_size`, with
  `contentDescription="EdgeCase"` since the stylised glyphs are not readable text)
- Two `ServiceEyeView`s (`serviceEyeLeft` / `serviceEyeRight`), 34×24dp, pinned to the flanks with a
  16dp margin, `visibility="gone"` by default
- A meander trim strip below the lintel: `ic_meander_horizontal`, `meander_trim_height` (10dp),
  `fitXY`, alpha 0.55

> Because this file is included four times in one activity, its IDs are **not unique** in the view
> tree. See the scoped-ID note in §5.1.

#### `layout_ad_plinth.xml` — the ad band
A vertical `LinearLayout` on `obsidian_black`, explicitly `clickable="false"`, `focusable="false"`,
`importantForAccessibility="no"`. Three children:
- `ic_meander_horizontal` at `meander_trim_height` (10dp), alpha 0.55 — the visible separator, the
  same trim that sits under every temple lintel
- a `Space` of `ad_buffer_gap` (10dp) — the dead zone
- `adFrame`, a `FrameLayout` with `ad_frame_margin_h` (8dp) sides, `ad_frame_margin_bottom` (6dp),
  `ad_frame_padding` (10dp), a `minHeight` of `ad_slot_min_height` (120dp), and `bg_ad_plinth` as its
  **background**

The banner is added programmatically by `AdHost`, never in XML. **Nothing in this file is clickable,
focusable, or animated** — that is the file's entire purpose. The frame padding is 10dp rather than
4dp so the drawable's 8dp corner notches stay visible around the banner.

**Vertical cost: 174dp measured**, not the 146dp previously assumed. B2 put a real banner on a
Pixel 9 Pro XL and the SDK returned **411×128dp**, not the ~100dp `Docs/Ads.md` §3.8 and §6.4
predicted ("roughly 90–110dp on a typical phone"). The arithmetic is 10 (trim) + 10 (gap) + 10 +
**128** + 10 (frame) + 6 (margin). On a large modern phone the format lands nearer its
min(150dp, 20% of height) ceiling than the middle of the documented range, so **budget 150dp, not
100dp**, and treat §6.4's "~145–165dp" as the floor. **Inert buffer above the first ad pixel: 30dp**
(unchanged — it sits above the banner), plus each screen's own bottom padding, so 38dp on Shortcuts,
the tightest screen. The small-screen pass in Appendix C must be re-run against 128dp+, since the
two defects the plinth originally exposed were found at the 100dp figure.

#### `layout_screen_main_menu.xml`
`FrameLayout` on `obsidian_black`, layered bottom-to-top:
1. `ObsidianCrackView` (match_parent, `importantForAccessibility="no"`)
2. Left pillar — `ic_pillar_serpent_left`, `pillar_width_new` (40dp), `fitXY`, alpha 0.85
3. Right pillar — the same drawable mirrored with `scaleX="-1"`
4. `dustContainer` `FrameLayout` (`clipChildren="false"`) — the host for `DustParticleView` and
   `CrackFlashView`
5. Center content `LinearLayout` (56dp side padding): the included temple header, then a weighted
   **`ScrollView`** (`fillViewport="true"`, scrollbars off) wrapping a `LinearLayout` that centres
   five slab buttons — `btnShortcuts`, `btnPosition`, `btnCredits`, a
   `ic_divider_fangs` divider, `btnStartService` ("Start", `selector_start_button`), and
   `btnStopService` ("Stop", `selector_stop_button`, Aged Marble text with an emerald glow shadow)
6. `tvVersion` at bottom-left (`CaptionChiseled`, alpha 0.6) — its **text is set in code** from
   `BuildConfig.VERSION_NAME`

The `ScrollView` exists because the plinth costs ~146dp: below roughly 800dp of screen height the
five-button stack no longer fits, and a plain centred block clips a button off **each** end. With
`fillViewport` the centred layout is byte-identical on tall screens and only scrolls when it must.

#### `layout_screen_shortcuts_container.xml`
Same backdrop + pillars, 52dp side padding. Content: the temple header (retitled "SHORTCUTS" in code);
a "CURRENT SHORTCUTS" caption; the **Altar** — a `bg_dark_seaweed_panel` `FrameLayout` at
`layout_weight="0.38"` holding `rvAltarShortcuts` and the centered `tvAltarEmpty`
("THE ALTAR LIES BARE — CHOOSE YOUR OFFERINGS BELOW", gone by default); a twin-fang divider; an
"AVAILABLE APPS" caption; `rvArchiveApps` at `layout_weight="0.42"`; and an action bar at
**`wrap_content`** with two equal-weight buttons, `btnBackToMenu` (BACK) and `btnSaveShortcuts`
(SAVE).

The action bar is fixed-height rather than weighted: at its former `weight="0.10"` the row shrank
below the 56dp its buttons need once the plinth took its 146dp, clipping BACK and SAVE. The two lists
keep the same 0.38 : 0.42 split of whatever remains.

#### `layout_screen_positioning_container.xml`
Same backdrop + pillars. **No shared side padding** — each child sets its own horizontal margin so the
header stays flush with the other screens while the canvas alone gets the extra room:
- Temple header (retitled "SLIVER POSITION" in code), `layout_marginHorizontal="52dp"`
- `PositioningView` (`positioningView`), `layout_weight="1"`, `layout_marginHorizontal="46dp"`
- A footer at `layout_marginHorizontal="52dp"`: `tvPositionInfo` (full-width readout) above an action
  row of two equal-weight buttons — `btnBackToMenuFromPosition` (BACK) on the left and
  `btnCustomizeSliver` (CUSTOMIZE) on the right

#### `layout_screen_credits_container.xml`
Same backdrop + pillars, 52dp side padding — structurally a sibling of the Shortcuts screen. Content:
the temple header (retitled "CREDITS" in code); a weighted **`ScrollView`**
(`fillViewport="true"`, `requiresFadingEdge="vertical"` at 20dp) wrapping a centred `LinearLayout`;
then a `wrap_content` action bar.

Inside the scroller, in order:
1. **DICE RELIGION** caption (`CaptionChiseled`) over a lead line at `text_body` and a supporting
   paragraph at `text_caption`, alpha 0.85 — the two-size split keeps the whole screen inside one
   viewport on a large phone
2. **The Seal** — `btnDeveloperSeal`, a `credits_seal_size` (108dp) square `FrameLayout` backed by
   `selector_dev_seal` with `credits_seal_padding` (11dp) and the slab elevation, holding a
   `fitCenter` `ic_dice_religion`. It is a `FrameLayout` rather than an `ImageButton` so the platform
   button background and its insets do not fight the frame drawable; `clickable`/`focusable` are set
   in XML and it carries the screen's only `contentDescription` that matters
   ("Dice Religion on Google Play"), with the `ImageView` marked
   `importantForAccessibility="no"`
3. A one-line hint caption, then a `ic_divider_fangs` twin-fang divider
4. **LETTERING** and **LIBRARIES** caption/body pairs
5. **ADVERTISING** caption/body (`tvCreditAdsHeading` / `tvCreditAdsBody`) — **visible as of B2**.
   A real ad network now serves the Plinth, so the attribution is both true and required

The action bar is a **vertical** `wrap_content` block, never weighted — the same rule the Shortcuts
screen needed. It holds a horizontal row of two equal-weight buttons, `btnBackToMenuFromCredits`
(BACK) and `btnPrivacyPolicy` (PRIVACY), and beneath it a full-width `btnAdConsent` (AD CONSENT) at
`visibility="gone"`.

The two privacy controls are deliberately distinct: **PRIVACY** opens the hosted policy in a
browser; **AD CONSENT** reopens Google's consent form in-process so the choice can be changed or
withdrawn. The second is `GONE` in XML and stays `GONE` at runtime unless
`AdHost.isPrivacyOptionsRequired()` is true — the EEA, UK, Switzerland and the applicable US states
only — so for most users the row costs no vertical space at all. Verified on device at both native
and 360×640dp with the flag forced true: all three slabs render and stay reachable.

These live in the fixed bar rather than the scroller so they are reachable without scrolling on any
screen size, and whichever is lowest remains the last interactive element above the Plinth's inert
buffer.

All prose and both URLs come from `strings.xml`; only the three button labels are inline.

#### `dialog_customize_sliver.xml`
A `bg_temple_panel` vertical panel with 16dp padding. Contains a "CUSTOMIZE SLIVER" title
(`TitleMonolith` at 16sp); a framed 110dp `SliverPreviewView`; a 320dp `ScrollView` holding the
**APPEARANCE** section (opacity `LabeledSeekBar`, the Default/Custom `RadioGroup`, and the hue
`LabeledSeekBar` + 32dp swatch in the initially-gone `hueContainer`), the **FANG GEOMETRY** section
(eight `LabeledSeekBar`s), the **SIZE (DP)** row, and the **APP DRAWER (DP)** row; then a footer of
RESET / CANCEL / APPLY. Section breaks use `ic_divider_fangs`.

#### `layout_item_shortcut_tile.xml` (Altar row)
`FrameLayout` over a translucent temple-sandstone tile (`#33D4C4A8`), containing a horizontal row:
- `ivDragHandle` — 24dp, `@android:drawable/ic_menu_sort_by_size`, alpha 0.5
- `tvOrderNumber` — 24dp, centered, 13sp serif bold in `serpent_emerald`
- App icon in a **square gem socket**: a 52dp `bg_icon_socket` `FrameLayout` with a 44dp `ivAltarIcon`
- `tvAltarName` — weight 1, `BodySerif`, single line, ellipsized
- `cbAltarSelect` — `android:button="@drawable/selector_gem_checkbox"`

#### `layout_item_available_app.xml` (Archives row)
Horizontal `LinearLayout` on `dark_seaweed`, 12dp padding: `ivArchiveIcon` (48dp), `tvArchiveName`
(weight 1, `BodySerif`, ellipsized), and `cbArchiveSelect` with the same gem checkbox.

---

### 6.2 Drawables

**Slabs, panels, and backgrounds**

| File | Type | Description |
|---|---|---|
| `bg_stone_button.xml` | layer-list | The limestone slab: `limestone_border` outer frame → 3dp inset `limestone_highlight` chisel line → 4dp inset `limestone_body` face → a `limestone_shadow` bottom bevel (48dp top inset) → `ic_texture_cracks` across the face. All square. |
| `bg_stone_button_pressed.xml` | layer-list | Same construction, but the inner chisel line goes **dark** (`#7A6C50` — the light source is blocked by your finger) and the body is `limestone_pressed` |
| `bg_start_button.xml` | layer-list | Emerald-tinged slab: `emerald_deep` border, `emerald_bright` chisel line, a sage-green body gradient (`#93A67F` → `#BED0A6`), `#7E9068` bevel, cracks |
| `bg_start_button_pressed.xml` | layer-list | Pressed variant of the above |
| `bg_stop_button.xml` | layer-list | Cold obsidian slab: `#20242A` border, a faded `#4D9AA0A6` silver glow line, a grey-black body gradient (`#22262B` → `#3B4147`), `#17191C` bevel |
| `bg_stop_button_pressed.xml` | layer-list | Pressed variant of the above |
| `bg_temple_lintel.xml` | layer-list | Header slab: `faded_olive_teal` border → `emerald_deep` hairline → a face gradient (`#C0122A23` → `#E607140F`) → four 8dp `obsidian_black` **ziggurat corner notches** so the rectangle reads as cut stone |
| `bg_temple_panel.xml` | layer-list | Dialog/window background: `limestone_border` → `emerald_deep` → `#F0081410` face. Square by construction |
| `bg_dark_seaweed_panel.xml` | layer-list | `faded_olive_teal` → `emerald_deep` → `panel_dark_seaweed_bg` face; square (the 4dp corners of the pre-v1.4 version are gone) |
| `bg_icon_socket.xml` | layer-list | Square gem socket: `tarnished_silver` rim with a 2dp `obsidian_black` recess |
| `bg_serpent_scales.xml` | vector | The tray backdrop — overlapping diamond snakeskin, 80×300dp: `#040807` base, shadowed lower facets `#07100C`, lit upper facets `#0E1F15`, `#020503` separation outlines |
| `bg_dev_seal.xml` | layer-list | The Seal's frame: `limestone_border` outer edge → 3dp `aged_bronze` hairline → 5dp `obsidian_black` niche → four 7dp `limestone_border` corner blocks. A deliberate **third** register: the slab reads raised, the plinth reads recessed, and this reads as a raised frame around a dark niche — press-able, but what sits inside is an artifact, not a control. Bronze rather than emerald because the mark is gold line-art on near-black, so the niche swallows the PNG's own ground |
| `bg_dev_seal_pressed.xml` | layer-list | Same, with the hairline lit to `emerald_gem` and the niche one step up to `obsidian_facet` — the "gems catch the light" language used by the Serpent's Eyes, rather than the limestone-darkening used by the slabs |
| `bg_ad_plinth.xml` | layer-list | The ad well: `limestone_border` frame → `emerald_deep` hairline → flat `obsidian_black` face (no gradient — the ad supplies the light) → four 8dp ziggurat corner notches. Mirrors the lintel's layer grammar but reads **recessed**, the deliberate inverse of the raised button language, so the band is never mistaken for a control |

**Icons, trim, and texture**

| File | Type | Description |
|---|---|---|
| `ic_texture_cracks.xml` | vector | Four crack strokes in `limestone_crack` tones at descending alpha, 360×56dp — the slab-face texture |
| `ic_pillar_serpent_left.xml` | vector | Doric serpent column, 40×640dp: capital, fluted shaft with straight-edged diagonal serpent bands (they shear cleanly under `fitXY` rather than distorting), scattered eyes and fangs, base. Mirrored via `scaleX="-1"` for the right pillar |
| `ic_meander_border.xml` | vector | Vertical Greek key, 32×1280 viewport, repeating every 64px, `tarnished_silver` — used on the tray's inward edge |
| `ic_meander_horizontal.xml` | vector | Horizontal Greek key, 640×16dp (1280×32 viewport) — the transpose of the vertical one; sits under every lintel |
| `ic_divider_fangs.xml` | vector | The twin-fang divider, 360×20dp: a silver lintel line with blocky bronze end studs, two emerald fangs 40 units apart, and an `emerald_core` venom glint at each tip |
| `ic_checkbox_gem.xml` | vector | Checked state: silver rim, obsidian recess, seated emerald-cut octagon with facet highlight and hot core |
| `ic_checkbox_socket.xml` | vector | Unchecked state: the empty socket — silver rim and obsidian recess only |
| `ic_gem_thumb.xml` | vector | The SeekBar thumb: a 20dp emerald-cut octagon with a dark rim, facet highlight, and hot core |
| `ic_launcher_background.xml` | vector | Transparent (`#00000000`) — the icon fills the launcher shape without a dark border |
| `ic_launcher_foreground.xml` | inset | 0dp inset wrapping `@drawable/icon_round` |
| `icon_round.png` | PNG | 512×512 RGBA, the app's circular icon |
| `drawable-xxhdpi/ic_dice_religion.png` | PNG | 512×512 RGB, the Dice Religion mark — gold line-art shrine on near-black. Placed at xxhdpi (≈170dp natural) and drawn `fitCenter` into a 108dp square, so it neither upscales nor holds an oversized bitmap |

**Selectors:** `selector_stone_button`, `selector_start_button`, `selector_stop_button`,
`selector_dev_seal` (each pressed → default), and `selector_gem_checkbox` (`state_checked` → gem,
else socket).

---

### 6.3 Values

#### Colors (`colors.xml`)

**Hellenic Serpent palette (original):**

| Name | Hex | Role |
|---|---|---|
| `abyssal_teal` | #071A15 | Engraved button text; formerly the root background |
| `dark_seaweed` | #122A23 | Archive item background |
| `faded_olive_teal` | #3B5249 | Borders, strokes, lintel frame |
| `temple_sandstone` | #D4C4A8 | Legacy button body (now referenced only as the literal `#33D4C4A8` altar tile) |
| `aged_marble` | #F5EFE6 | Primary text, dust particles |
| `serpent_emerald` | #2E8B57 | Accent, order numbers, radio/checkbox tint |
| `tarnished_silver` | #9AA0A6 | Secondary text, rims, meander |
| `ethereal_pink` | #4DFFC0CB | Unused (was the outer arc glow, pre-fang) |

**Obsidian Serpent additions (v1.4.0):**

| Group | Names |
|---|---|
| Obsidian | `obsidian_black` #07090B, `obsidian_sheen` #101816, `obsidian_facet` #0C1210, `crack_void` #020403 |
| Emerald ramp | `emerald_deep` #1D5C3F, `emerald_gem` #2E8B57, `emerald_bright` #50C878, `emerald_core` #A9F5C8, `emerald_glow_faint` #332E8B57 |
| Limestone | `limestone_body` #CEBFA3, `limestone_highlight` #EFE6D2, `limestone_border` #5E523C, `limestone_crack` #8A7A5E, `limestone_shadow` #9F8F72, `limestone_pressed` #B3A588 |
| Structure | `stele_marble` #1A2822, `pillar_stone_dark` #152521, `pillar_stone` #1E332C, `pillar_capital` #42594F, `serpent_scale_dark` #1B4D35, `serpent_scale_light` #2E7D53 |
| Accent | `aged_bronze` #8C7853 |

**Stone button shades (legacy):** `sandstone_bevel` #A39171, `sandstone_pressed` #BEB09A,
`text_engraved` #071A15. **Panel:** `panel_dark_seaweed_bg` #B3122A23.
**Legacy, unused:** `purple_200/500/700`, `teal_200/700`, `black`, `white`.

> **Palette duplication — largely resolved.** The custom views now resolve their colours through
> `ContextCompat.getColor(context, R.color.…)`; 18 `Color.parseColor` literals were replaced. What
> remains duplicated is (a) alpha-composited one-offs such as the crack underglow and the hatch
> stroke, which are a named colour at a given opacity rather than a palette entry of their own, and
> (b) the hex baked into the vector drawables' path data. The pillar and serpent-scale entries are
> still documentation-only for that second reason. See §10.

#### Dimensions (`dimens.xml`)

| Name | Value | Use |
|---|---|---|
| `margin_wide` | 24dp | Spacing between main-menu buttons |
| `stone_button_height` | 56dp | Slab button height |
| `stone_button_elevation` | 8dp | Slab button elevation |
| `stone_button_pressed_translation` | 4dp | Press-down translation |
| `app_icon_size` | 48dp | Archives icon size |
| `text_header` / `text_body` / `text_caption` | 18sp / 16sp / 14sp | Type scale (via styles + layouts) |
| `header_title_size` | 26sp | Main-menu lintel title |
| `header_title_size_sub` | 20sp | Sub-screen lintel titles |
| `header_height` | 64dp | Lintel height |
| `pillar_width_new` | 40dp | Serpent pillar width |
| `meander_trim_height` | 10dp | Meander strip under each lintel, and the plinth's separator |
| `ad_buffer_gap` | 10dp | The plinth's inert dead zone |
| `ad_frame_padding` | 10dp | The stone frame around the banner |
| `ad_frame_margin_h` | 8dp | Keeps the well off the screen edges |
| `ad_frame_margin_bottom` | 6dp | Below the well |
| `ad_slot_min_height` | 120dp | Floor so an unfilled slot holds its space; `AdHost` overrides with the exact size |
| `altar_min_height` | 148dp | Two rows, so drag-to-reorder stays usable on short screens |
| `credits_seal_size` | 108dp | The Seal — square by construction; the mark is a 1:1 asset |
| `credits_seal_padding` | 11dp | Inset between the Seal's stone frame and the mark |
| `credits_section_gap` | 16dp | Spacing between Credits sections |

The five `ad_*` values are **compliance-relevant, not cosmetic** — they form the buffer between the
lowest interactive element and the first ad pixel. If something does not fit, shrink app chrome, never
these.

Ten unreferenced entries were removed in the A-track cleanup, including `sliver_fang_width`,
`sliver_fang_height` and `tray_width` — stale duplicates of values that now live in `SliverConfig`
as `DEF_WIDTH_DP` / `DEF_HEIGHT_DP` / `DEF_TRAY_WIDTH_DP`. Every remaining entry is referenced.

#### Strings (`strings.xml`)

`app_name` = `"EdgeCase"`, plus the Credits screen's content:

| Name | Role |
|---|---|
| `credits_maker_heading` / `credits_maker_lead` / `credits_maker_body` | The Dice Religion caption, lead line and supporting paragraph |
| `credits_seal_hint` | One-line caption under the Seal |
| `credits_lettering_heading` / `credits_lettering_body` | Cinzel and GFS Neohellenic, each with its OFL 1.1 attribution |
| `credits_libraries_heading` / `credits_libraries_body` | AndroidX / AOSP, Apache 2.0 |
| `credits_ads_heading` / `credits_ads_body` | Written but **not shown** — the views are `gone` until the real SDK lands (B2) |
| `url_developer_page` | **Placeholder.** `…/store/apps/developer?id=Dice+Religion`. The canonical form is `…/store/apps/dev?id=<numeric publisher id>`, available from the Play Console once the account exists |
| `url_privacy_policy` | **Live** — `https://anumey.xyz/legal/edgecase/privacy`, deployed and verified **200 on 2026-08-30**, hosted on the Anumey's Lair site beside the Mach2 and BOTCH policies. **Must never move.** Not yet registered in Play Console, because no listing exists yet (Appendix C, B6) |

Both URLs are `translatable="false"`. The privacy policy and its companion
`https://anumey.xyz/legal/edgecase/delete-data` are **deployed and live** — both verified 200 on
2026-08-30, with §6.2/§6.3/§6.4 present as the code and this document cite them. `url_developer_page`
remains the one placeholder, pending the numeric `dev?id=` form. See Appendix C group B. Long-form prose lives here rather than in the layout so there
is exactly one place to edit the wording; every other UI label — screen titles, button text,
captions, the dialog's controls, toasts, and the discard dialog's copy — is still an inline literal
in the layouts or in code. Note the manifest sets `android:label="EdgeCase"` as a literal rather than
referencing `@string/app_name`.

#### Styles (`styles.xml`)

| Style | Font | Key properties |
|---|---|---|
| `TitleMonolith` | `@font/gfs_neohellenic` | All-caps, letterSpacing 0.18, `aged_marble`, emerald under-glow shadow (`#802E8B57`, dy 3, radius 6). `textStyle=normal` — the TTF is already Bold, so synthetic bold is suppressed |
| `SlabButtonText` | `@font/cinzel_black` | All-caps, letterSpacing 0.10, `text_engraved`, `text_header` size, a `#66FFFFFF` light-catch shadow below the incision. `textStyle=normal` — the TTF is already weight 900 |
| `CaptionChiseled` | `@font/cinzel_black` | All-caps, letterSpacing 0.14, `tarnished_silver`, `text_caption` size |
| `BodySerif` | serif | `aged_marble` — list rows, the empty-altar message, and the Credits prose |
| `CaptionSerif` | serif | `tarnished_silver`, `text_caption` — the two section captions and the Seal hint |

#### Themes (`themes.xml`)

```xml
<style name="Theme.EdgeCase" parent="Theme.AppCompat.DayNight.NoActionBar" />
```

Applied by the manifest as of the A-track cleanup (it previously named the AppCompat parent
directly, leaving this style unreferenced). There are still no custom theme attributes and no
`values-night/` resources, so the parent's behaviour is unchanged.

---

### 6.4 Fonts

Two OFL-licensed faces are bundled in `res/font/`, with their licenses kept in `Docs/fonts_licenses/`:

| File | Face | Used by |
|---|---|---|
| `gfs_neohellenic.ttf` | GFS Neohellenic (Bold) | `TitleMonolith` — lintel titles, dialog title |
| `cinzel_black.ttf` | Cinzel Black (weight 900) | `SlabButtonText`, `CaptionChiseled` — all button lettering and section captions |

Both faces are now **credited in-app** on the Credits screen (§6.1), not only in
`Docs/fonts_licenses/`.

Both styles set `textStyle="normal"` explicitly, because each TTF already carries its weight and
letting the system synthesize bold on top of it smears the letterforms.

---

### 6.5 Mipmaps & App Icon

**Source:** `icon_round.png` — a 512×512 RGBA PNG with alpha, in `res/drawable/` for adaptive-icon use.

**Adaptive icons (API 26+):** `mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml` are
identical — background `@drawable/ic_launcher_background` (transparent), foreground
`@drawable/ic_launcher_foreground` (a 0dp inset of `icon_round.png`, so the icon fills the full 108dp
viewport). Neither declares a `<monochrome>` layer.

**Raster fallbacks:** `ic_launcher.png` and `ic_launcher_round.png` at mdpi (48×48), hdpi (72×72),
xhdpi (96×96), xxhdpi (144×144), and xxxhdpi (192×192) — all the same circular icon, so it renders
correctly in both standard and circular launcher modes. The manifest sets
`android:roundIcon="@mipmap/ic_launcher_round"`.

---

### 6.6 XML Configuration

#### `AndroidManifest.xml`

```xml
<manifest xmlns:android="..." xmlns:tools="...">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="EdgeCase"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.EdgeCase"
        tools:targetApi="31">

        <!-- Added at B1. Resolved per build type; a missing or malformed value crashes
             the app at startup. -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="@string/admob_app_id" />

        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".SidebarService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse" />
    </application>
</manifest>
```

#### `backup_rules.xml` & `data_extraction_rules.xml`
Both now include `EdgeCasePrefs.xml` — `backup_rules.xml` for cloud backup below API 31, and
`data_extraction_rules.xml` for both cloud backup and device transfer on 31+. Restoring a shortcut
list onto a device missing some of those apps is safe: `ShortcutStateManager` filters to installed
packages on load, and `SidebarService.populateShortcuts` skips any package that fails to resolve.

#### `proguard-rules.pro`
Stock file with commented-out examples; no active rules. Minification is disabled.

---

## 7. Feature Inventory

### Implemented Features

| # | Feature | Status | Location |
|---|---|---|---|
| 1 | Four-screen navigation (Main Menu / Shortcuts / Positioning / Credits) | ✅ | `MainActivity.kt` |
| 2 | Slab button press animation (translationY) | ✅ | `MainActivity.applyStoneButtonBehavior()` |
| 3 | Haptic feedback on button press (30ms, amplitude 255) | ✅ | `MainActivity.triggerHaptic()` |
| 4 | Dust particle burst on button press | ✅ | `DustParticleView.kt` |
| 5 | App listing — all installed launchable apps | ✅ | `MainActivity.getInstalledApps()` |
| 6 | Bipartite shortcut editor (Altar + Archives) | ✅ | `MainActivity.initShortcutsScreen()` |
| 7 | Drag-to-reorder in the Altar via ItemTouchHelper | ✅ | `ShortcutDragCallback.kt` |
| 8 | Checkbox toggle to add/remove from the Altar | ✅ | Both adapters |
| 9 | Select/deselect within the Altar (dim, evict on Save) | ✅ | `ShortcutStateManager` |
| 10 | Discard confirmation on unsaved changes | ✅ | `MainActivity.showDiscardDialog()` |
| 11 | Save shortcuts to SharedPreferences (ordered CSV) | ✅ | `ShortcutStateManager.commit()` |
| 12 | Persistent foreground service | ✅ | `SidebarService.kt` |
| 13 | Foreground notification (priority LOW) | ✅ | `SidebarService.buildSystemNotification()` |
| 14 | Edge sliver overlay (`TYPE_APPLICATION_OVERLAY`) | ✅ | `SidebarService.assembleSliverView()` |
| 15 | Fang-path rendering (two protrusions, central recess) | ✅ | `ArcSliverView.kt` + `SliverShape` |
| 16 | Swipe gesture detection (inward from the edge) | ✅ | `ArcSliverView.onTouchEvent()` |
| 17 | Desaturated shortcut icons in the tray (20%) | ✅ | `SidebarService.desaturateIcon()` |
| 18 | Tray unfurl animation (scaleX 0→1 at the edge pivot) | ✅ | `SidebarService.transitionToExpandedTray()` |
| 19 | App launch from the tray with haptic feedback | ✅ | `SidebarService.populateShortcuts()` |
| 20 | Positioning screen with draggable sliver preview | ✅ | `PositioningView.kt` |
| 21 | Restricted zones (top/bottom 10%, Greek-key hatched) | ✅ | `PositioningView.drawGreekKeyZone()` |
| 22 | Snap-to-edge animation with particle trail | ✅ | `PositioningView.snapSliverTo()` |
| 23 | Live position persistence (saved on drag release) | ✅ | `onPositionChanged` → prefs |
| 24 | Left/right side support | ✅ | `ArcSliverView.Side.LEFT/RIGHT` |
| 25 | System gesture exclusion for the sliver (API 29+) | ✅ | `SidebarService.assembleSliverView()` |
| 26 | Hot-reload shortcuts in the running service | ✅ | `ACTION_UPDATE_SHORTCUTS` |
| 27 | Hot-reload position in the running service | ✅ | `ACTION_UPDATE_POSITION` |
| 28 | Overlay permission check + redirect to settings | ✅ | `MainActivity.checkAndRequestPermissions()` |
| 29 | Battery optimization exemption request | ✅ | `MainActivity.checkAndRequestPermissions()` |
| 30 | Custom adaptive app icon (transparent bg, 0dp inset) | ✅ | §6.5 |
| 31 | Tray dismiss on outside touch | ✅ | `trayView.setOnTouchListener()` |
| 32 | Idempotent sliver/tray add/remove guards | ✅ | `sliverAdded` + `isAttachedToWindow` |
| 33 | Dirty-state tracking for discard prompts | ✅ | `ShortcutStateManager.isDirty()` |
| 34 | Sticky service restart on kill | ✅ | `START_STICKY` |
| 35 | **Customize Sliver** dialog on the Position screen | ✅ v1.3.5 | `SliverCustomizeDialog.kt` |
| 36 | Configurable sliver **opacity** (0–100%) | ✅ v1.3.5 | `SliverConfig.opacity` |
| 37 | Configurable sliver **color** (grey or custom hue) | ✅ v1.3.5 | `SliverConfig.colorMode/customHue` |
| 38 | Configurable **fang geometry** (8 knobs) | ✅ v1.3.5 | `SliverConfig` + `SliverShape` |
| 39 | Configurable sliver **size** (width/height dp) | ✅ v1.3.5 | `SliverConfig.widthDp/heightDp` |
| 40 | **Live preview** in the dialog + on the Position screen | ✅ v1.3.5 | `SliverPreviewView`, `PositioningView` |
| 41 | Sliver style persistence | ✅ v1.3.5 | `SliverConfig.save/load` |
| 42 | Hot-reload sliver style in the running service | ✅ v1.3.5 | `ACTION_UPDATE_STYLE` |
| 43 | Single-source fang geometry builder | ✅ v1.3.5 | `SliverShape.buildPath()` |
| 44 | In-place overlay update (fixes the stale-sliver ghost) | ✅ v1.3.5 | `applySliverUpdate()` + `applyConfig()` |
| 45 | **Animated obsidian/emerald background** on all screens | ✅ v1.4.0 | `ObsidianCrackView.kt` |
| 46 | **Crack flash** at the touch point on button press | ✅ v1.4.0 | `CrackFlashView.kt` |
| 47 | **Serpent's Eyes** service-state indicator | ✅ v1.4.0 | `ServiceEyeView.kt` + `SidebarService.isRunning` |
| 48 | Unified **temple-lintel header** across all screens | ✅ v1.4.0 | `layout_temple_header.xml` |
| 49 | Bundled blocky-Greek **fonts** + three new text styles | ✅ v1.4.0 | `res/font/`, `styles.xml` |
| 50 | Cracked-**limestone slab buttons**, distinct Start/Stop | ✅ v1.4.0 | `bg_stone/start/stop_button.xml` |
| 51 | **Serpent pillars** (mirrored single vector) | ✅ v1.4.0 | `ic_pillar_serpent_left.xml` |
| 52 | **Gem-socket** checkboxes and SeekBar thumbs | ✅ v1.4.0 | `selector_gem_checkbox`, `ic_gem_thumb` |
| 53 | **Twin-fang divider** replacing the spear | ✅ v1.4.0 | `ic_divider_fangs.xml` |
| 54 | App-wide **de-rounding** (every corner square) | ✅ v1.4.0 | drawables + `PositioningView` |
| 55 | **Serpent-scale tray backdrop** | ✅ v1.4.0 | `bg_serpent_scales.xml` |
| 56 | **Expanded positioning canvas** (fit-inside, pediment, Greek-key zones) | ✅ v1.4.0 | `PositioningView.kt` |
| 57 | **Sliver-tracking arrow** (visible at 0% opacity, drag handle) | ✅ v1.4.0 | `PositioningView.drawTrackingArrow()` |
| 58 | **Predictive-back migration** (fixes the old limitation #11) | ✅ v1.4.0 | `MainActivity.backCallback` |
| 59 | Configurable **app-drawer size**, decoupled from the sliver | ✅ v1.4.x | `SliverConfig.trayWidthDp/trayHeightDp` |
| 60 | Background **app-list preloading** | ✅ v1.4.x | `MainActivity.preloadApps()` |
| 61 | Version label sourced from `BuildConfig` | ✅ v1.4.x | `MainActivity.wireMainMenuButtons()` |
| 62 | **The Plinth** — persistent, inert, bordered ad band below every screen | ✅ | `layout_ad_plinth.xml`, `bg_ad_plinth.xml` |
| 63 | ~~Placeholder banner~~ — replaced by a real `AdView` at B2 | ✅ | `DummyBannerView.kt` now dead (B5) |
| 64 | Slot space reserved before any creative arrives (no layout shift) | ✅ | `AdHost`, `ad_slot_min_height` — two-stage reservation |
| 65 | Banner hidden under modal dialogs (`INVISIBLE`, no re-layout) | ✅ | `AdHost.setAdVisible()` |
| 66 | Scrollable main-menu button stack (small-screen safety) | ✅ | `layout_screen_main_menu.xml` |
| 67 | Fixed-height Shortcuts action bar (small-screen safety) | ✅ | `layout_screen_shortcuts_container.xml` |
| 68 | **Overlay suspended while the Activity is foreground** | ✅ | `ACTION_SUSPEND_OVERLAY` / `ACTION_RESUME_OVERLAY`, `MainActivity.isForeground` |
| 69 | Altar keeps two draggable rows on short screens | ✅ | `altar_min_height` |
| 70 | Dust and crack effects draw above the slab buttons | ✅ | `dustContainer` elevation |
| 71 | Instrumented test suite (14 tests) | ✅ | `app/src/androidTest/` |
| 72 | Backup / device-transfer rules for `EdgeCasePrefs` | ✅ | `res/xml/` |
| 73 | **Credits screen** — maker, lettering and library attributions | ✅ | `layout_screen_credits_container.xml` |
| 74 | **The Seal** — framed developer mark linking to the Play Store page | ✅ | `bg_dev_seal.xml`, `MainActivity.openUrl()` |
| 75 | **Privacy** button linking to the policy (placeholder URL) | ✅ | `btnPrivacyPolicy` |
| 76 | Slab press behaviour generalised to any `View` | ✅ | `applyStoneButtonBehavior<T : View>` |
| 77 | **AD CONSENT** entry point, hidden unless a consent regime applies | ✅ | `btnAdConsent`, `AdHost.isPrivacyOptionsRequired()` — real UMP body as of B4 |
| 79 | **UMP consent resolved before any ad request** (`canRequestAds()` gate) | ✅ | `AdHost.start()` |
| 80 | Real GMA Next-Gen banner in the Plinth, adaptive-sized | ✅ | `AdHost.attachBanner()` |
| 78 | Published **privacy policy** and **data-deletion** pages | ✅ | `anumey.xyz/legal/edgecase/*` |

### Planned / Stub Features

| # | Feature | Status |
|---|---|---|
| 1 | Dummy button (third menu option) | ✅ **Gone** — became **CREDITS** (features 73–75) |
| 2 | Monochrome themed icon (Android 13+) | ❌ Removed — incompatible with a raster PNG foreground |
| 3 | AdMob monetization | 🔶 **Partial** — the Plinth and its placeholder are built (§5.19–5.20); no SDK, consent flow, or ad unit yet. See `Docs/Ads.md` and Appendix C |
| 4 | Overlay suspension while the Activity is foreground | ✅ **Done** — feature 68 |
| 5 | UMP **privacy options** entry point | ✅ **Done** (B3/B4) — real `UserMessagingPlatform` bodies; unproven under an actual consent regime, see §9 |
| 6 | In-app AdMob attribution | 🔶 Written but hidden — `tvCreditAdsHeading` / `tvCreditAdsBody` are `gone` until B2 |

---

## 8. Data Flow & State Management

### State Persistence Architecture

```
┌────────────────────────────────────────────┐
│            SharedPreferences               │
│              "EdgeCasePrefs"               │
│                                            │
│  saved_shortcuts_order: "pkg1,pkg2"        │
│  saved_shortcuts: Set<String>              │
│  sliver_side: "left" | "right"             │
│  sliver_y_bias: Float (0.0–1.0)            │
│  sliver_opacity / _color_mode / _color_hue │
│  sliver_t1|t2_thickness/_length/_tipy      │
│  sliver_gums_depth / _gap                  │
│  sliver_width_dp / _height_dp              │
│  tray_width_dp / tray_height_dp            │
└──────┬────────────────────────┬────────────┘
       │                        │
       ▼                        ▼
┌──────────────┐        ┌────────────────┐
│ MainActivity │        │ SidebarService │
│              │        │                │
│ Reads/writes │        │ Reads on start │
│ all keys     │        │ & on hot-reload│
└──────┬───────┘        └────────┬───────┘
       │                         │
       ▼                         ▼
┌────────────────────┐   ┌──────────────┐
│ ShortcutStateManager│   │ SliverConfig │
│                     │   │              │
│ • Working set       │   │ • 15 style   │
│ • Dirty tracking    │   │   keys       │
│ • Commit / Discard  │   │ • load/save  │
└─────────────────────┘   └──────────────┘
```

### Shortcut Editing Flow

1. User enters the Shortcuts screen → `initShortcutsScreen()` (once). If the background preload has
   finished, the lists build immediately; otherwise a load starts and populates via `runOnUiThread`.
2. `ShortcutStateManager` reads `saved_shortcuts_order` → populates `committedList` and `altarItems`,
   skipping packages that are no longer installed.
3. The Altar shows `altarItems` with gem checkboxes; the Archives shows `allApps`.
4. **Checkbox in Archives:** `setFromArchives(pkg, checked)` — immediately adds to or removes from
   `altarItems`; both adapters refresh.
5. **Checkbox in Altar:** `toggleAltarSelection(pos)` — flips `isSelected`; the row dims or un-dims;
   both adapters refresh.
6. **Drag in Altar:** `moveAltarItem(from, to)` → `notifyItemMoved`.
7. **SAVE:** `commit()` writes the selected packages in order, evicts unselected rows, and sends
   `ACTION_UPDATE_SHORTCUTS`; toast "CARVED IN STONE".
8. **BACK while dirty:** `showDiscardDialog()` — "ABANDON" or "KEEP CARVING".
9. **Discard:** `discard()` resets `altarItems` to `committedList`.
10. **Re-entry:** `refreshShortcutsState()` calls `discard()`, re-reading the committed state.

### Positioning Flow

1. User enters the Positioning screen → `initPositioningScreen()` (once).
2. The saved `SliverConfig` is applied to the preview, then `sliver_side` / `sliver_y_bias` are read
   and pushed into `PositioningView`; `tvPositionInfo` is updated.
3. User drags the sliver preview (or its tracking arrow) — a particle trail follows.
4. On release the sliver snaps to the nearer edge; `onPositionChanged` fires at the end of the
   animation.
5. The callback writes the new side and yBias to prefs immediately and refreshes the readout.
6. It then sends `ACTION_UPDATE_POSITION`; the service calls `applySliverUpdate()`, moving the existing
   overlay **in place**.

### Sliver Customization Flow

1. Position screen → **CUSTOMIZE** → `openCustomizeSliverDialog()` loads the current `SliverConfig`
   and shows `SliverCustomizeDialog`.
2. The dialog edits a **working copy**. Slider and sliver-size changes update the live
   `SliverPreviewView`; drawer-size changes update values only.
3. **Apply:** the four dp fields are re-parsed and clamped, `working.save(context)` writes the 15 style
   keys, and the callback applies the config to the Position screen's preview and sends
   `ACTION_UPDATE_STYLE`; toast "THE FANG IS FORGED".
4. The service's `applySliverUpdate()` reloads the config and updates the live overlay in place
   (recolor + `updateViewLayout`), then rebuilds the tray so its size and side match.
5. **Cancel** discards the working copy. **Reset** restores defaults in the dialog only — nothing is
   persisted unless Apply follows.

### Service Lifecycle

1. "START EDGE SERVICE" → `checkAndRequestPermissions()` → `startEdgeService()` →
   `startForegroundService(intent)`; the Serpent's Eyes open optimistically.
2. `SidebarService.onCreate()`:
   - Sets `isRunning = true`
   - Reads the real screen height from `currentWindowMetrics`
   - Loads position **and `SliverConfig`** from prefs
   - Builds the foreground notification
   - Creates both windows' parameters, assembles the sliver and tray views
   - Adds the sliver if the overlay permission is granted; otherwise `stopSelf()`
3. Swipe on the sliver → `transitionToExpandedTray()` (remove sliver, add tray with the unfurl
   animation).
4. Tap a tray icon → launch the app → `transitionToSliverState()`.
5. Outside touch on the tray → `transitionToSliverState()`.
6. "STOP SERVICE" → `stopService(...)` → `onDestroy()` clears `isRunning` and removes both windows;
   the eyes close.
7. `START_STICKY` — the system restarts the service if it is killed. `MainActivity.onResume()` re-reads
   `isRunning` so the eyes reflect reality after any such transition.

---

## 9. Permissions

| Permission | Purpose | Handling |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the overlay windows (Sliver & Tray) | `Settings.canDrawOverlays()`; redirect to settings if denied. The service also `stopSelf()`s if it starts without it |
| `FOREGROUND_SERVICE` | Run the persistent foreground service | Declared only |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Qualify the foreground service type | Declared only |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent Doze from killing the service | `PowerManager.isIgnoringBatteryOptimizations()`; redirect to settings (non-blocking — the flow continues either way) |
| `VIBRATE` | Haptics on button presses, swipes, and launches | Declared, no runtime check needed |

**Five more permissions arrive by manifest merge (B1).** None is declared in this app's manifest;
all are contributed by the ad libraries and appear only in the *merged* manifest. Verified against
`manifest-merger-release-report.txt`:

| Permission | Contributed by | Note |
|---|---|---|
| `INTERNET` | `ads-mobile-sdk:1.4.0` (also Cronet) | **Confirms policy claim P1** — the app declares no networking of its own; it arrives solely with the ad SDK |
| `ACCESS_NETWORK_STATE` | `ads-mobile-sdk:1.4.0` (also `androidx.work`) | Connectivity checks before an ad request |
| `AD_ID` | `ads-mobile-sdk:1.4.0` (also `play-services-ads-identifier`) | Required at `targetSdk` > 33. **Must match the Play Console Advertising ID declaration**, or the ad ID is zeroed and fill/CPM collapse |
| `READ_BASIC_PHONE_STATE` | `ads-mobile-sdk:1.4.0` | Normal-level, no runtime prompt — but it **is** listed publicly on the Play listing. Not mentioned anywhere in the published privacy policy; check before release |
| `WAKE_LOCK` | `androidx.work:2.7.0`, pulled in transitively by the ad SDK | Not requested by EdgeCase or by AdMob directly |

Do **not** hand-add any of these — they merge automatically, and a hand-added duplicate diverges
silently when the SDK version changes.

**Manifest `<queries>`:** declares the `MAIN`/`LAUNCHER` intent so `queryIntentActivities()` can
enumerate launchable apps under package-visibility restrictions.

**Notification Channel:** `EdgeCaseEngineChannel`, `IMPORTANCE_LOW`, created in
`buildSystemNotification()`. Content is `"EdgeCase Active"` / `"Listening for gestures."` — no user
data appears in it.

### Claims the privacy policy makes about this code

`https://anumey.xyz/legal/edgecase/privacy` is a **published legal document that asserts specific
properties of this source tree**. Breaking one does not fail a build or a test — it silently makes
that document false. Before changing any of the following, change the policy in the same pass.

| # | Asserted publicly | Where it lives now |
|---|---|---|
| P1 | The app makes **no network request of its own**. The only networking is the ads SDK fetching ads | No `File`, stream, socket or HTTP class appears anywhere in `app/src/main/java` — verified by grep. `INTERNET` is not in this manifest; it arrives only by merge from the GMA library |
| P2 | The installed-app list is **never persisted, logged or transmitted** — only the packages the user ticks are saved | `MainActivity.getInstalledApps()` builds an in-memory `List<AppInfoData>`; `ShortcutStateManager.commit()` writes only the selected packages |
| P3 | **No usage history** — no launch counts, no timestamps, no record of what was opened when | The launch path (`SidebarService.populateShortcuts()` → `getLaunchIntentForPackage`) writes nothing |
| P4 | The overlay **cannot read what is beneath it**, and the app is **not an accessibility service** | `TYPE_APPLICATION_OVERLAY` only; no `AccessibilityService`, no `BIND_ACCESSIBILITY_SERVICE`, nothing in `res/xml/` declaring one |
| P5 | The one exception is the bare **outside-touch notification**, used solely to dismiss the tray | Both windows set `FLAG_WATCH_OUTSIDE_TOUCH`; only `trayView`'s listener acts on `ACTION_OUTSIDE`, and only to call `transitionToSliverState()`. The first draft of the policy claimed the app is *never* told about outside touches — that was wrong, and this row is why the claim was rewritten |
| P6 | The ad appears **only in the Activity** — never in the overlay, the tray, or over another app | `AdHost` is constructed in `MainActivity.onCreate` and referenced nowhere else; `SidebarService` has no ad import |

**The narrow `<queries>` form is itself a promise.** The policy states the app does *not* hold
`QUERY_ALL_PACKAGES`. Widening package visibility would contradict it, and would also change the
Play Data safety answers.

**Android backup is ON, and the policy says so.** `backup_rules.xml` and `data_extraction_rules.xml`
both include `EdgeCasePrefs.xml`, so the shortcut list and every style key are copied to the user's
Google backup and carried in a device-to-device transfer. This is the single biggest divergence from
the sibling BOTCH policy, which switches backup off and says so — that wording could not be reused
here without becoming a false statement. Turning backup off later would make the policy *overstate*
collection, which is the safe direction, but it should still be updated.

**That commitment is now met (B3/B4, 2026-08-30).** §6.2 and §6.4 tell the user they can change or
withdraw advertising consent "from the Credits screen inside the app". `AdHost.showPrivacyOptionsForm()`
now calls `UserMessagingPlatform.showPrivacyOptionsForm(...)` for real, and `btnAdConsent` appears
whenever UMP reports `REQUIRED`. **Not yet verified end-to-end under a consent regime**: the device
available for testing reports `NOT_REQUIRED` (no EEA/UK/CH/US-state law applies to it), so the
button correctly never appears there. The EEA path must be exercised with AdMob → Privacy &
messaging → debug geography = EEA on a registered test device before release — see Appendix C group
C. Until that run happens, this row is *implemented but unproven*.

---

## 10. Known Limitations & Future Work

### Known Limitations

1. **The palette is still partly duplicated.** The custom views now read `@color` resources, but two
   sources of truth remain: alpha-composited one-offs written as literals (a named colour at a given
   opacity), and the hex baked into vector `pathData`. Editing `colors.xml` alone will restyle the
   views but not the vectors.

2. **Duplicate IDs across the included headers.** `layout_temple_header.xml` is included **four**
   times in one view tree — once per screen, the Credits screen included — so `tvTempleTitle`,
   `serviceEyeLeft`, `serviceEyeRight`, and `templeHeader` are each present four times. Every lookup
   must be scoped to its screen; an unscoped `findViewById` will silently return the wrong copy.
   This is currently handled correctly (§5.1) but is easy to regress, and each new screen adds
   another copy.

3. **Desaturation is in-memory and mutates the shared Drawable.** `desaturateIcon()` sets the color
   filter on the `Drawable` returned by `getApplicationIcon()` each time `populateShortcuts()` runs,
   and the press handler clears it with `colorFilter = null`. If the platform ever hands back a cached
   or shared `Drawable` instance, clearing the filter on one icon can desaturate-toggle another.
   `mutate()` on the drawable would remove the hazard.

4. **Extreme drawer heights can still overflow.** The tray's Y is clamped to `≥ 0`, so a drawer taller
   than the sliver's center-line is pinned to the top of the screen and its bottom can run past the
   screen edge. The dialog allows up to 640dp, which exceeds many portrait screens.

5. **No landscape support.** The Y-bias positioning and the tray layout assume portrait. There are no
   `layout-land/` resources and no orientation lock.

6. **No multi-window or foldable handling.** The sliver attaches to whatever edge the current window
   bounds provide, with no special handling for either case.

7. **No light theme.** `Theme.EdgeCase` inherits `Theme.AppCompat.DayNight.NoActionBar`, but there is no
    `values-night/` and the Obsidian Serpent palette is dark-only. The app renders identically in
    either system setting.

8. **Monochrome adaptive icon absent.** The `<monochrome>` layer was removed because it does not work
    with a raster PNG foreground; Android 13+ themed icons fall back to the standard adaptive icon.

9. **Icon PNGs are hand-scaled.** The density fallbacks were resized from the single 512×512 source
    rather than generated by the Image Asset tool.

10. **Each shown screen retains a full-screen bitmap.** `ObsidianCrackView` rasterizes its static layer
    into an ARGB_8888 bitmap sized to the view. A `GONE` screen is never laid out, so its bitmap is
    created lazily on first display — but once a screen has been visited, its bitmap is held for the
    life of the activity. Visiting all four now costs four full-screen bitmaps.

11. **`ObsidianCrackView` uses a frame counter, not wall time.** `nowMs` advances by a fixed 16f per
    animation callback, so the gem pulse rate tracks the achieved frame rate rather than real time.
    On a sustained frame drop the pulse slows; on a 120Hz panel it runs fast. Same for
    `ServiceEyeView`.

12. **No clipboard, deep-link, or app-shortcut support.** The tray launches apps only — no
    `ShortcutManager` entries, no custom actions, no widgets.


### Potential Future Enhancements

- **AdMob monetization** — the Plinth and its placeholder are built; the SDK, consent flow, ad unit
  and Play Console work remain. Fully specified in `Docs/Ads.md`; sequenced in Appendix C group B.
- Finish the palette reconciliation (#1) — the vector `pathData` hex and the alpha-composited one-offs
- Scope the included-header IDs safely, or give each screen its own header ID (#2)
- Call `mutate()` on tray icons so the desaturation filter cannot leak between ImageViews (#3)
- Clamp the drawer so an extreme height cannot run past the screen edge (#4)
- Add landscape support with adaptive tray sizing (#5)
- Create `values-night/` resources for a light theme variant (#7)
- Add a monochrome vector drawable for Android 13+ themed icons (#8)
- Regenerate the icon PNGs with the Image Asset tool (#9)
- Drive the gem and eye pulses from wall time rather than a frame counter (#11)
- Add custom action shortcuts and widget pinning in the tray (#12)
- ~~Configurable sliver size~~ — **done** (v1.3.5)
- ~~Configurable tray size~~ — **done** (v1.4.x)
- ~~Predictive-back navigation~~ — **done** (v1.4.0)
- ~~Overlay suspension while the app is foreground~~ — **done** (A track)
- ~~Prune dead resources~~, ~~unit tests~~, ~~backup rules~~ — **done** (A track)
- ~~Give the Dummy button a purpose~~ — **done**: it is now CREDITS. The UMP privacy-options entry
  point still needs a home on that screen once the consent SDK lands (Appendix C, B4)

---

## Appendix: Quick Reference

### Key Files at a Glance

| File | Lines | Purpose |
|---|---|---|
| `MainActivity.kt` | 664 | Main UI, navigation, back handling, permissions, service control, Customize hook, ad host, outbound links, consent entry point |
| `PositioningView.kt` | 505 | Marble stele, draggable fang, tracking arrow, snap animation, particles |
| `SidebarService.kt` | 490 | Foreground service, overlay windows, tray, in-place style/position update |
| `ObsidianCrackView.kt` | 278 | Animated obsidian background with pulsing emerald gems |
| `SliverCustomizeDialog.kt` | 244 | "Customize Sliver" popup controller |
| `ShortcutStateManager.kt` | 158 | Bipartite shortcut state, persistence, dirty tracking |
| `ServiceEyeView.kt` | 156 | Serpent's Eye service-state indicator |
| `AdHost.kt` | 324 | Owns the Plinth's banner, adaptive sizing, two-stage space reservation, dialog hiding, SDK init, and the full UMP consent flow |
| `SliverConfig.kt` | 134 | Sliver + drawer model, prefs I/O, drawer-height migration |
| `ArcSliverView.kt` | 125 | Config-driven fang rendering, swipe detection, `applyConfig` |
| `CrackFlashView.kt` | 110 | One-shot fracture flash on slab press |
| `DummyBannerView.kt` | 106 | Dead since B2; delete at B5 |
| `DustParticleView.kt` | 104 | Particle burst effect for button presses |
| `LabeledSeekBar.kt` | 95 | Reusable label + slider + value control row |
| `ShortcutDragCallback.kt` | 68 | Drag-to-reorder ItemTouchHelper |
| `ActiveShortcutsAdapter.kt` | 60 | Altar RecyclerView adapter |
| `SliverPreviewView.kt` | 53 | Live sliver preview for the Customize dialog |
| `AvailableAppsAdapter.kt` | 50 | Archives RecyclerView adapter |
| `SliverShape.kt` | 50 | Shared parametric fang-path builder |
| `AppInfoData.kt` | 9 | Data class for installed app info |
| | **3,783** | **total Kotlin (main)** |
| `androidTest/*.kt` | 246 | 14 instrumented tests |

### Color Hex Quick Reference

| Name | Hex | Preview |
|---|---|---|
| `obsidian_black` | #07090B | Near-black — the root background |
| `obsidian_facet` | #0C1210 | Mid-tone facet fill |
| `crack_void` | #020403 | The crack line itself |
| `emerald_deep` | #1D5C3F | Gem body, unlit |
| `emerald_gem` / `serpent_emerald` | #2E8B57 | Gem body, lit |
| `emerald_bright` | #50C878 | Glow mid, tracking arrow |
| `emerald_core` | #A9F5C8 | Hottest pixel at pulse peak |
| `limestone_body` | #CEBFA3 | Slab button face |
| `limestone_border` | #5E523C | Slab outer frame |
| `limestone_highlight` | #EFE6D2 | Slab chisel line |
| `aged_marble` | #F5EFE6 | Off-white — primary text, dust |
| `tarnished_silver` | #9AA0A6 | Cool grey — secondary text, rims |
| `faded_olive_teal` | #3B5249 | Muted olive-teal — borders |
| `abyssal_teal` | #071A15 | Very dark teal — engraved text |
| `aged_bronze` | #8C7853 | Divider studs |

---

*Document originally generated from a complete source-tree analysis on 2026-06-20. Updated 2026-07-06
(fang sliver redesign), 2026-07-10 (v1.3.5 Sliver Customize feature, `SliverShape`, `ACTION_UPDATE_STYLE`,
in-place overlay update), and 2026-07-11 (recorded the then-unimplemented `NewTheme.md` plan and the
back-gesture defect). **Fully re-audited against the source tree on 2026-08-29 for v1.4.1**: the
Obsidian Serpent overhaul is now implemented and documented as built — three new custom views, the
shared temple lintel, bundled fonts, slab buttons, serpent pillars, gem controls, the expanded
positioning canvas and tracking arrow, predictive-back navigation, the configurable and decoupled app
drawer, background app-list preloading, and the `BuildConfig`-sourced version label. Limitations were
re-derived from the code rather than carried forward; the old limitation #11 (back gesture) is fixed
and removed, and eight new ones were added. Updated again the same day for the **Ad Plinth preview
build**: `AdHost` + `DummyBannerView`, `layout_ad_plinth.xml` + `bg_ad_plinth.xml`, the restructured
`activity_main.xml` column, banner hiding under modal dialogs, and two small-screen layout fixes the
plinth's footprint exposed. No ad SDK or dependency is present. Updated again for the **A track**
(overlay suspension, Altar floor, overlay z-order, 14 instrumented tests, resource pruning and
palette reconciliation, backup rules) and then for the **Credits screen**: a fourth virtual screen
carrying the maker, lettering and library attributions, the framed Dice Religion Seal linking to the
Play Store, and a PRIVACY button — replacing the Dummy stub. Then for the **legal surface**:
`url_privacy_policy` now points at the written, hosted policy, leaving `url_developer_page` as the
only placeholder, and the AD CONSENT entry point was added to the Credits action bar as a hidden
seam. **Updated 2026-08-30 for the ad integration proper (B1–B4)**: the legal pages were deployed
and verified live; the GMA Next-Gen SDK and UMP were added with per-build-type ad IDs; the
placeholder banner was replaced by a real adaptive `AdView`; and the UMP consent flow was wired,
closing the last published-policy commitment the code had not met. Two items are implemented but
unproven — the EEA consent path and the small-screen pass at the measured 128dp banner.*

---

## Appendix B: Related Documents

| Document | Status | Purpose |
|---|---|---|
| `~/Work/Web/Anumey's Lair` | External repo | The website that **hosts EdgeCase's legal pages**: `/legal/edgecase/privacy` and `/legal/edgecase/delete-data`, beside the existing Mach2 and BOTCH policies. Next.js on Firebase App Hosting; **every push to `main` deploys**. Its `docs/stats.md` §4 → *Legal routes* is the authority on the URL constraints. The publisher-level `public/app-ads.txt` already carries the AdMob publisher ID EdgeCase will use — no change needed there |
| `Docs/SliverAnatomy.md` | Current | Deep-dive on the fang geometry: named parts, vertices, tuning knobs, and how they map to `SliverConfig`/`SliverShape` |
| `Docs/Dimensions.md` | Current | Stable ID/dimension addressing for every page and element; §6 covers the sliver anatomy and tuning knobs. Predates the v1.4 rehaul — verify IDs against the layouts before relying on it |
| `Docs/Publisher.md` | Partly superseded | Google Play publication roadmap / pre-launch checklist. Its §3 ("Ad Integration Strategy") is superseded by `Docs/Ads.md` §9 |
| `Docs/Ads.md` | **Implemented except §7.8** | AdMob integration plan (2026-08-28): the "Plinth" bordered-banner architecture, the overlay-service compliance hazard and its fix, a 7-phase implementation plan, and a compliance checklist. §5–§6 (architecture, visual spec), §7.1–7.5 (account, SDK, AdHost, layout, wiring), §7.6 (overlay suspension) and §7.7 (privacy entry point) are all built. Only **§7.8 (Play Console, versioning)** remains — Appendix C, B6. **Caveat: §7.3's code listing has drifted from the shipped SDK** and must not be pasted verbatim; verify symbols against the AAR |
| `Docs/Legacy/NewTheme.md` | **Implemented** | The v1.4.0 "Obsidian Serpent" blueprint. Its Phases 1–7 and §12 functional changes are all present in the code described above; retained for the design rationale behind the Design Laws referenced in source comments |
| `Docs/Legacy/EdgeCaseTD.md`, `EdgecaseTheme.md`, `EdgeNextPDP.md`, `IMPLEMENTATION_PLAN.md` | Historical | Superseded early design and planning documents |

---

## Appendix C: Next Steps

Ordered by dependency, not by size. Everything in group A is code and can be done now; group B needs
external setup first.

### A. Code work, no external dependencies — ✅ **COMPLETE**

| # | Task | Outcome |
|---|---|---|
| A1 ✅ | Suspend the overlay while the Activity is foreground | `ACTION_SUSPEND_OVERLAY` / `ACTION_RESUME_OVERLAY` + `detachOverlayWindows()`, driven from `onResume`/`onPause` and guarded on `isRunning`; `MainActivity.isForeground` closes the start-up race. Verified on device: fang absent in-app, present on Home |
| A2 ✅ | Altar / Archives split on short screens | Added `altar_min_height` (148dp ≈ two rows) rather than shifting the weights, so drag-to-reorder stays usable at 360×640dp and tall screens are untouched |
| A3 ✅ | Dust / crack overlay z-order | `dustContainer` moved after the content and given `elevation="16dp"` — draw order inside a FrameLayout is elevation-first, so 0dp lost to the buttons' 8dp. Verified: fractures now render across the button faces |
| A4 ✅ | Tests for `SliverShape` and `SliverConfig` | 14 **instrumented** tests (not JVM: both depend on `Path`, `Color` and `Context`, and Robolectric would have meant a new dependency). Covers the coercion invariant, L/R mirroring, prefs round-trip, the drawer-height migration, and colour/alpha edges |
| A5 ✅ | Prune dead resources; reconcile the palette | Removed 4 drawables, 3 styles and 10 dimens; pointed the manifest at `Theme.EdgeCase`; replaced 18 colour literals in the custom views with `@color` refs and named `stele_marble` |
| A6 ✅ | Backup rules for `EdgeCasePrefs` | Cloud backup and device transfer both include the prefs file, so a reinstall keeps shortcuts, position and every style key |

### B. Ad integration proper — needs an AdMob account first

| # | Task | Blocking? | Notes |
|---|---|---|---|
| B0 ◐ | **AdMob account, app registration, one banner unit** | mostly done 2026-08-30 | `Docs/Ads.md` §7.1. Publisher **pub-4587702028307036** (shared with the sibling apps, so any enforcement here hits them too). App registered as "Edgecase"/Android, *not listed on a store yet* — **relink to the Play listing after publication**, the unlinked path costs fill rate. One unit, `EdgeCase — Plinth Banner`, `…/8470994251`, format Banner, **auto-refresh 60s** ✅. Test device (Pixel 9 Pro XL) registered account-wide at Settings → Test devices, keyed to its advertising ID — resetting that ID silently de-registers it. **Outstanding: EU + US-states consent messages** must exist in Privacy & messaging or UMP has nothing to show, and app-ads.txt verification is blocked until a Play listing exists (the file itself is correct and serving 200) |
| B1 ✅ | Add GMA Next-Gen + UMP dependencies, `APPLICATION_ID` meta-data, per-build-type ad IDs | done 2026-08-30 | §7.2. `ads-mobile-sdk:1.4.0` + `user-messaging-platform:4.0.0`; meta-data reads `@string/admob_app_id`; debug→test IDs, release→live IDs. Needed `resValues = true` (AGP 9 gates it, undocumented in §7.2). Both variants build; `APPLICATION_ID` and `AD_ID` confirmed in both merged manifests. Four other permissions merged in too — see §9 |
| B2 ✅ | Replace the marked block in `AdHost.attachBanner()` with the real `AdView` | done 2026-08-30 | §7.3. Real `AdView` + `MobileAds.initialize` off the main thread; `doOnLayout` for the measured width, with a **two-stage** reservation (nominal before layout so the well never pops, exact `AdSize` after). ADVERTISING credit un-hidden. Verified on device: `Plinth banner loaded (411×128dp)`, no crash, overlay grep clean. Two API corrections vs §7.3 — see below |
| B3 ✅ | UMP consent flow in `AdHost.start()`, gated on `canRequestAds()` | done 2026-08-30 | §7.3. `requestConsentInfoUpdate` on every launch → `loadAndShowConsentFormIfRequired` → `canRequestAds()` gate on all three paths into `initializeAndLoad()`. Verified on device: `canRequestAds=true required=NOT_REQUIRED formAvailable=false`, both callbacks firing. UMP 4.0.0's API matched the doc exactly, unlike the ads SDK. **The §7.3 listing has drifted from the shipped SDK** — verify every symbol against the AAR (`javap` on the artifact in `~/.gradle/caches`) before trusting it. Two errors found at B2: `InitializationConfig` is in `…sdk.initialization`, **not** `…sdk.common`; and `LoadAdError.code`/`.message` are Kotlin properties, not `getCode()`/`getMessage()`. `MobileAds.initialize` also needs an explicit `object : OnAdapterInitializationCompleteListener` rather than the trailing lambda §7.3 shows |
| B4 ✅ | Fill in the two UMP consent stubs in `AdHost` | done 2026-08-30 | §7.7. Done with B3 — inseparable: B3 makes `consentInformation` live and B4 is the code that reads it, so B3 alone would have left the entry point permanently hidden even where Google requires it. `showPrivacyOptionsForm` also re-fires `onConsentResolved` on dismissal, since withdrawing consent can change the requirement status. ADVERTISING credit un-hidden with B2. **Still needs the EEA debug-geography run** (group C): the published policy §6.2/§6.4 promises this control, so it must be *seen* working, not assumed |
| B5 | Delete `DummyBannerView.kt` | **unblocked** | Dead code since B2 — nothing references it (§5.20). A one-line deletion |
| B6 | Play Console declarations, bump to v1.5.0 | **needs a Play Console account** | §7.8. The legal pages are **deployed and verified 200** (2026-08-30), and `url_privacy_policy` points at the real one. Remaining: create the developer account and listing; declare *Contains ads*; the **Advertising ID** declaration, which must match the merged `AD_ID` (§9) or the ad ID is zeroed; Data safety with Advertising ID collected+shared; re-run the content rating questionnaire (it now asks about ads); leave `TagForChildDirectedTreatment` unset; bump to `versionCode = 4` / `versionName = "1.5.0"`. Then relink AdMob to the listing (B0) and fix `url_developer_page` with the numeric `dev?id=` form. **Two open questions first** — the `READ_BASIC_PHONE_STATE` disclosure gap (§9) and whether Data safety should declare installed apps as collected, which `Publisher.md` §5.4 says yes and policy claim P2 says no |

### C. Verification before any release with ads

> **Two items here are now the critical path**, because the code they cover is written but has
> never been exercised:
>
> 1. **The EEA consent path has never rendered.** The test device reports `NOT_REQUIRED`, so the
>    consent form and the AD CONSENT slab have not been seen once. Set AdMob → Privacy & messaging
>    → **debug geography = EEA** for the registered test device, relaunch, and confirm: the form
>    appears on first launch, `EdgeCaseAds` logs `required=REQUIRED`, the slab is visible on
>    Credits, and tapping it reopens the form. The published policy §6.2/§6.4 promises this control
>    exists, so it must be seen working before release, not assumed.
> 2. **The small-screen pass must be re-run at 128dp**, not the 100dp it was originally done at.
>    The Plinth's real cost is 174dp (§6.1). The main-menu `ScrollView` and the fixed-height
>    Shortcuts action bar were both sized against the smaller figure.

- Run the full compliance checklist in `Docs/Ads.md` §8.
- Confirm the overlay grep is clean: no ad imports in `SidebarService.kt`, `ArcSliverView.kt`,
  `SliverPreviewView.kt`, or `PositioningView.kt`.
- Re-run the small-screen pass at 360×640dp and 360×720dp on all four screens **and** the Customize
  dialog, at both the 100dp nominal banner and the 150dp worst case.
- Watch CTR from day one. On a utility app, anything above ~2–3% means accidental clicks — widen the
  buffer before Google acts.
- **Re-verify the six claims in §9** against the shipping build. The privacy policy asserts them
  publicly, and the ad SDK is the one change most likely to disturb P1 and P6.
- Confirm `https://anumey.xyz/legal/edgecase/privacy` and `/delete-data` return **200** *before*
  either URL goes into Play Console. The in-app PRIVACY button points at the first one, so a 404
  breaks the app as well as the listing.
