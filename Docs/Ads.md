# EdgeCase — AdMob Integration Plan

> **Document Type:** Monetization Architecture & Compliance Plan
> **App:** EdgeCase · `com.dicereligion.edgecase`
> **Current Version:** versionCode 4 / versionName 1.5.0
> **Author:** Engineering
> **Last Updated:** 2026-08-28 (status annotations 2026-09-04)
> **Supersedes:** `Docs/Publisher.md` §3 ("Ad Integration Strategy") — see [§9](#9-corrections-to-publishermd-3) for the list of corrections.

---

## Table of Contents

1. [Verdict on the three proposals](#1-verdict-on-the-three-proposals)
2. [Codebase audit — what we are actually integrating into](#2-codebase-audit--what-we-are-actually-integrating-into)
3. [Policy findings (researched 2026-08-28)](#3-policy-findings-researched-2026-08-28)
4. [The one genuine hazard: the overlay service](#4-the-one-genuine-hazard-the-overlay-service)
5. [Recommended architecture — "The Plinth"](#5-recommended-architecture--the-plinth)
6. [Visual design spec](#6-visual-design-spec)
7. [Implementation plan](#7-implementation-plan)
8. [Compliance checklist](#8-compliance-checklist)
9. [Corrections to Publisher.md §3](#9-corrections-to-publishermd-3)
10. [Testing & rollout](#10-testing--rollout)
11. [Risks and things that will get us limited](#11-risks-and-things-that-will-get-us-limited)
12. [Sources](#12-sources)

---

## 1. Verdict on the three proposals

**Short answer: all three of your ideas are acceptable, and idea #1 is not merely allowed — it is one of the implementations Google explicitly documents as *recommended*.** There is one hazard in this app that has nothing to do with the three ideas, and it is the overlay service. See [§4](#4-the-one-genuine-hazard-the-overlay-service).

### 1.1 — "Stone border around the banner"

**Verdict: ✅ Recommended by Google, not merely tolerated.**

Google's *Recommended banner implementations* page lists **"Ad separated by border"** as a first-class recommended pattern: a non-clickable border between the ad and app content, specifically for banners anchored at the top or bottom of the screen. The stated purpose — "reduce user confusion and accidental clicks" — is exactly what your stone frame does.

Four constraints make this compliant rather than risky:

| # | Constraint | Why |
|---|---|---|
| C1 | The frame must be a **`android:background` drawable on the container**, never a `foreground` drawable or a sibling `View` drawn on top of the `AdView`. | A view with `alpha > 0` and `visibility == VISIBLE` sitting over the ad counts as an **obstruction**, which degrades viewability measurement and can produce invalid impressions. A `background` is painted *behind* the child, so it is obstruction-free by construction. |
| C2 | The frame and everything inside it must be **non-clickable** — no `OnClickListener`, no `clickable="true"`, no ripple, no haptics, no dust/crack effects. | Anything tappable adjacent to (or worse, inside) the ad slot is the primary accidental-click vector Google enforces against. |
| C3 | Do **not** put app text, labels, icons, or a decorative "Ad"/themed caption inside the frame. | Google-served banners carry their own AdChoices attribution. A custom themed label risks reading as "ad is app content", which touches the deceptive-ads / "ads confused with app content" rules. Style the *frame*, never the *ad*. |
| C4 | The theme must not make the ad look like a button or a piece of EdgeCase UI. | The plinth reads as a **recessed well / foundation stone** — a hole in the temple floor that content sits in — not as a raised limestone slab (which is our button language). Deliberately different visual grammar from `selector_stone_button`. |

Within those four rules, the stone border is a straight win: it satisfies the AdMob "separated by border" recommendation *and* the app's aesthetic at the same time.

### 1.2 — "At minimum, a line above the ad"

**Verdict: ✅ Fine — and it is a strict subset of #1, so do both.**

A horizontal separator between app content and the ad slot is the same mechanism as the border, just weaker. The app already owns the perfect asset: **`@drawable/ic_meander_horizontal`**, the Greek meander trim already used under every temple lintel (`layout_temple_header.xml`). Reusing it as the ad-zone's upper rule makes the ad band read as native architecture rather than a bolted-on ad shelf.

There is no scenario in which we "cannot" put a border around the ad — the border is our own view's background, entirely under our control. So #2 is not a fallback; it becomes a *component* of #1.

### 1.3 — "Permanent bottom band, visible on every screen"

**Verdict: ✅ This is the canonical anchored-banner pattern, and it is the correct architecture for this app.**

An anchored adaptive banner pinned to the bottom of the window, persisting while the user moves between screens, is exactly what the format is designed for: "fixed placement at screen edges… stay on screen while users are interacting with the app." Because EdgeCase is a **single Activity** whose three screens are sibling `View`s toggled by visibility, a persistent bottom band is architecturally trivial — and it is *better* for monetization than per-screen banners:

- **One `AdView` instance, one ad request stream.** No duplicate requests, no double-billing of impressions, no risk of two banners on one screen.
- **Uninterrupted refresh cycle.** Screen switches don't destroy/recreate the ad, so the AdMob-side refresh timer (60 s minimum) runs cleanly. Recreating an `AdView` on every navigation is the classic way to look like refresh abuse.
- **No layout shift.** One reserved slot, sized once.

The caveats are about **buffer**, not about the concept:

- On the **Shortcuts** screen the `BACK` / `SAVE` buttons currently sit at the very bottom of the content area. On the **Positioning** screen `BACK` / `CUSTOMIZE` do the same. Dropping an ad immediately beneath either row reproduces Google's documented anti-pattern *"ad sandwiched between app items"* / *"ads adjacent to interactive elements"*. The fix is a mandated non-interactive buffer band — specified in [§6.3](#63-the-buffer-budget-compliance-critical).
- The **Shortcuts** screen has **drag-to-reorder** (`ItemTouchHelper` on `rvAltarShortcuts`) and the **Positioning** screen is a **drag canvas** (`PositioningView`). A user dragging toward the bottom of the screen with an ad just below is a high-risk accidental-click geometry. The buffer band plus keeping both drag surfaces inside the weighted content area (never extending to the window bottom) resolves this.

---

## 2. Codebase audit — what we are actually integrating into

Read of the tree at `c4ff912` (branch `development`).

### 2.1 Shape of the app

| Property | Value | Relevance to ads |
|---|---|---|
| UI toolkit | **Android Views + XML** (no Compose, no Fragments) | XML `AdView` works; no Compose interop needed |
| Activities | **One** — `MainActivity` | A single persistent ad host is natural |
| Screens | 3 sibling `View`s inside a root `FrameLayout` (`activity_main.xml`), switched by `visibility` in `showScreen()` | Restructuring the root to add a bottom band touches **one file** |
| `minSdk` / `targetSdk` / `compileSdk` | 30 / 36 / 36 (`release(36) { minorApiLevel = 1 }`) | Clears both SDK options (legacy needs 23+/35+, Next-Gen needs 24+/35+) |
| AGP / Kotlin | AGP 9.2.1, Kotlin 2.2.10 (AGP-9 built-in Kotlin; no explicit Kotlin plugin in `app/build.gradle.kts`) | Next-Gen requires Kotlin ≥ 1.9 ✅ |
| Dependencies | `appcompat`, `recyclerview`, `core-ktx` only | **No coroutines on the classpath** — see [§7.3](#73-phase-2--the-adhost) |
| `buildFeatures.buildConfig` | already `true` | We can use `resValue` / `buildConfigField` for debug-vs-release ad unit IDs |
| R8 | `isMinifyEnabled = true` **as of 2026-09-04** | GMA ships consumer ProGuard rules; this held — **no ad-specific keep rules were needed**. Verified by unzipping both AARs. The release APK has not yet been *run*, though |
| Background threading idiom | raw `Thread { … }` (`preloadApps()`, `initShortcutsScreen()`) | Match it — use `Thread` for SDK init, not coroutines |

### 2.2 The screens, in ad terms

| Screen | Content | Bottom-most interactive element | Dwell time |
|---|---|---|---|
| **Main Menu** | 5 stone buttons, centered in a weighted block; `tvVersion` bottom-left | `btnStopService` (centered stack — natural gap below it) | Short but every session passes through |
| **Shortcuts** | Altar `RecyclerView` (drag-to-reorder) + Archives `RecyclerView` + `BACK`/`SAVE` row | `BACK` / `SAVE`, flush to `paddingBottom="8dp"` | **Longest** — real configuration work |
| **Positioning** | `PositioningView` drag canvas + info text + `BACK`/`CUSTOMIZE` row | `BACK` / `CUSTOMIZE`, flush to `paddingBottom="16dp"` | Medium |

`SliverCustomizeDialog` is an `AlertDialog` launched from Positioning. **No ads in dialogs.**

### 2.3 Non-Activity surfaces (ad-free by mandate)

- `SidebarService` — a foreground service that adds **`TYPE_APPLICATION_OVERLAY`** windows to the `WindowManager`: the `ArcSliverView` fang and the shortcut `trayView`.
- The foreground-service notification.

These must never contain, host, or trigger an ad. See [§4](#4-the-one-genuine-hazard-the-overlay-service).

---

## 3. Policy findings (researched 2026-08-28)

### 3.1 What Google explicitly *recommends*

From **AdMob → Recommended banner implementations**:

1. **Ad separated from interactive elements** — buffer space between the ad and clickable app content; top or bottom placement.
2. **Ad separated by border** — a **non-clickable border** between ad and app content, called out specifically for sticky/anchored banners.
3. **Custom navigation button spacing** — keep distinct separation between banners and custom nav elements. *(EdgeCase's `BACK`/`SAVE`/`CUSTOMIZE` rows are precisely "custom navigational elements".)*
4. **Fixed space allocation** — reserve the ad's screen space **before** the ad loads, so content never shifts when it arrives.
5. **Adaptive banners** — use fixed *aspect ratios*, not fixed heights; renders correctly across devices. Smart banners are deprecated.

Items 1–4 are, together, an almost line-by-line description of what you proposed.

### 3.2 What Google explicitly *discourages*

From **AdMob → Discouraged banner implementations**:

1. **Ads adjacent to interactive elements** — "Close proximity of banner ads to other elements within an app is one of the biggest causes of accidental clicks." Named offenders: navigational buttons, custom app menu bars, chat boxes, image galleries, active gameplay screens.
2. **Ad sandwiched between app items** — ad placed between interactive content and a navigation/menu row, so users repeatedly cross the banner.
3. **Ad overlapping app content** — banners floating over scrolling menus or moving with scroll. *"This violates policy and risks ad serving suspension."*

Consequence, stated on the banner-guidance page: *"Not following these guidelines may lead to invalid activity and/or may result in Google disabling ad serving to your app."*

### 3.3 Google Play Ads policy (the harder line)

Google Play's **Ads** policy prohibits ads *"displayed to users in unexpected ways, that may result in inadvertent clicks, or impairing or interfering with the usability of device functions."* The operative clauses for EdgeCase:

> **"Ads may only be displayed inside of the app serving them and must not interfere with other apps, ads, or the operation of the device, including system or device buttons and ports."**

> Ads must not *"simulate or impersonate the user interface of any app feature, such as notifications or warning elements of an operating system."*

> Full-screen ads triggered by exit buttons or home-button presses are forbidden. Non-closeable full-screen ads beyond 15 s are disallowed.

> Ads that appear *"when the user is not in the app that contains the ad"* — out-of-context ads — are the specific enforcement target that removed hundreds of apps from Play.

This clause set is what makes [§4](#4-the-one-genuine-hazard-the-overlay-service) mandatory rather than optional.

### 3.4 Google Publisher Policies (ad placement)

- No ads that **"overlay or are adjacent to navigational or other action items"** causing unintended clicks.
- No ads on **"dead end" screens** where the user cannot exit without clicking the ad.
- No ads in **apps or pages that run in the background**, or where **"the user's attention is expected to be elsewhere."** *(Direct hit on the overlay/tray surface.)*
- Screens must contain **more publisher content than ads**.

### 3.5 Obstruction / viewability

For an ad **not** to count as blocked, any view over it must have `alpha == 0`, or `visibility == GONE`, or `visibility == INVISIBLE` — a transparent *background* is not enough; the **view's own alpha and visibility** decide. An obstruction over the ad can cause the impression to fail viewability measurement.

This is why constraint **C1** in §1.1 exists, and it is also the second reason the overlay sliver must not sit over the banner.

### 3.6 Implementation guidance / invalid traffic

- **60-second minimum refresh interval** for banners (configure it in the AdMob console — do not hand-roll faster reloads).
- Never encourage clicks; "click the ads" style copy is prohibited.
- Place ads at natural transition points, where users are *less* engaged.
- Invalid traffic = any click/impression not from genuine user interest. Developer self-clicks during testing are the #1 way small publishers get flagged — **always use test ad units or a registered test device**.

### 3.7 SDK landscape as of 2026-08-28

| | **GMA Next-Gen SDK** *(recommended)* | **Google Mobile Ads SDK (Legacy)** |
|---|---|---|
| Artifact | `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` | `com.google.android.gms:play-services-ads` |
| Latest | **1.4.0** (2026-08-20) | **25.4.0** (2026-06-17) |
| Status | **Stable / GA since 1.0.0 (Apr 2026)**, monthly releases | Official docs: *"in maintenance mode. For the latest updates and features, migrate and set up GMA Next-Gen SDK."* |
| Requirements | minSdk 24+, compileSdk 35+, Kotlin 1.9+ | minSdk 23+, compileSdk 35+ |
| Init | **Must** be off the main thread (ANR risk otherwise) | Recommended off the main thread |
| Mediation | AdMob mediation only (no ironSource / AppLovin as the mediator) | Full third-party mediation |
| Deprecated-API removal | — | Deprecated APIs start being **removed from May 2026**; calls fail as early as **July 2028** |

**Recommendation: build on GMA Next-Gen 1.4.0.** EdgeCase is a Kotlin codebase, is a brand-new integration (no migration cost), and has no third-party mediation requirement — the only real reason to stay on legacy. Building a fresh integration on an SDK Google itself labels "maintenance mode" would be starting in technical debt. Legacy 25.4.0 is documented in [§7.9](#79-fallback-legacy-sdk-variant) as a drop-in fallback if Next-Gen misbehaves.

### 3.8 Banner size API — an important detail

In the Next-Gen `AdSize` class:

- `getLargeAnchoredAdaptiveBannerAdSize(Context, widthDp)` — **current, non-deprecated.**
- `getLargePortraitAnchoredAdaptiveBannerAdSize` / `getLargeLandscapeAnchoredAdaptiveBannerAdSize` — orientation-locked variants, current.
- `getCurrentOrientationAnchoredAdaptiveBannerAdSize`, `getPortraitAnchoredAdaptiveBannerAdSize`, `getLandscapeAnchoredAdaptiveBannerAdSize` — **deprecated.**

So the standard-height anchored adaptive banner is on its way out; **large anchored adaptive is the supported path** (introduced in legacy 25.0.0, "designed to boost engagement and revenue"). Budget accordingly: a large anchored adaptive banner is meaningfully taller than the old 320×50 — expect roughly **90–110 dp** on a typical phone, hard-capped by the format at **min(150 dp, 20 % of device height)**.

**Never hardcode the height.** Compute it at runtime and reserve exactly that much space (§3.1 item 4). The layout budget in [§6.3](#63-the-buffer-budget-compliance-critical) is built around this.

---

## 4. The one genuine hazard: the overlay service

**This is the finding that matters most, and it is not something your three proposals introduced — it is pre-existing in EdgeCase's architecture.**

### 4.1 The problem

`SidebarService` adds `TYPE_APPLICATION_OVERLAY` windows via `WindowManager`. From `instantiateWindowParameters()`:

```kotlin
val restrictedTop = (screenHeight * 0.10f).toInt()
val validRange   = (screenHeight * 0.80f).toInt()
val sliverYPx    = restrictedTop + (validRange * currentYBias).toInt()
```

The sliver's Y is mapped into **10 %–90 % of screen height**. At `sliver_y_bias` near `1.0`, the fang sits at **90 % of screen height** — which is exactly where a bottom-anchored banner lives. The tray (`trayWidthDp` wide, bottom-aligned to the sliver's centre) can cover even more.

That produces two distinct violations:

1. **Obstruction over an ad** (§3.5). The sliver is a `VISIBLE` view with `alpha > 0` in a window above our Activity. It sits on top of the banner. Impressions under it are, at best, unviewable; at worst this reads as ad-stacking.
2. **Ads displayed outside the app / interference** (§3.3). Independently of us: EdgeCase draws over *other* apps by design. Any ad-bearing surface reachable from that overlay context would be an out-of-context ad — the exact behaviour Play enforces against.

### 4.2 The mandate

> **No ad object — banner, interstitial, native, app-open, rewarded — may ever be created, loaded, or shown by `SidebarService`, by `ArcSliverView`, by `trayView`, or by the foreground-service notification. Ads live only inside `MainActivity`'s window.**

This is non-negotiable and should be a code-review rule, ideally a lint/CI grep: no `com.google.android.libraries.ads` import may appear in `SidebarService.kt`, `ArcSliverView.kt`, or `SliverPreviewView.kt`.

### 4.3 The fix for the obstruction

**Suspend the overlay while `MainActivity` is in the foreground.** This is correct on three independent grounds:

- **Compliance:** the banner is never obscured — clean, viewable impressions.
- **UX:** an edge launcher floating over its own settings screen is noise. Nobody needs a shortcut-to-other-apps fang while configuring which shortcuts go in it.
- **Correctness:** the sliver currently overlaps our own UI — including the `PositioningView` drag canvas, where a stray fang next to the mock phone is actively confusing.

Implementation in [§7.6](#76-phase-5--overlay-suspension-compliance-critical).

### 4.4 Prominent disclosure still applies

Unrelated to ads but adjacent: `SYSTEM_ALERT_WINDOW` + `FOREGROUND_SERVICE_SPECIAL_USE` need the prominent-disclosure and special-use declarations tracked in `Publisher.md` §5.2–5.3. Ads do not change that; they raise the review scrutiny under which it is assessed.

---

## 5. Recommended architecture — "The Plinth"

**One persistent, bordered, non-interactive ad band at the bottom of the single Activity window, hosting one large anchored adaptive banner, visible on all three screens, gated on UMP consent, and never obscured by the overlay.**

That is idea #1 + idea #2 + idea #3 combined, which is also the configuration Google's own recommendation list describes.

### 5.1 View hierarchy after the change

```
MainActivity  (window)
└─ LinearLayout  @id/rootColumn  (vertical, fitsSystemWindows=true, obsidian_black)
   ├─ FrameLayout  @id/screenContainer   height=0dp, weight=1     ← ALL existing app UI, untouched
   │    ├─ include @id/screenMainMenu
   │    ├─ include @id/screenShortcuts
   │    └─ include @id/screenPositioning
   │
   └─ include  @id/adPlinth   height=wrap_content                 ← NEW, never scrolls, never moves
        └─ LinearLayout (vertical, non-clickable)
           ├─ ImageView  @drawable/ic_meander_horizontal   ← the separator line (idea #2)
           ├─ Space      (buffer)                          ← the anti-accidental-click gap
           └─ FrameLayout @id/adFrame                      ← background = @drawable/bg_ad_plinth (idea #1)
                └─ AdView  (added programmatically)
```

**Why this shape:**

- `fitsSystemWindows="true"` moves from the old root `FrameLayout` to `rootColumn`, so the system-navigation inset lands **below** the plinth. The ad never sits under the gesture bar and never fights the system nav — which matters, because Play's Ads policy explicitly forbids interfering with "system or device buttons."
- `weight=1` on `screenContainer` means every existing screen layout keeps `match_parent` semantics inside a shorter box. **No changes to the three screen layouts' internals are required** — they simply get a smaller canvas.
- The plinth is outside `screenContainer`, so `showScreen()` never touches it. Zero changes to the routing logic.
- One `AdView` for the app's whole lifetime.

### 5.2 Formats: what to use, what to skip

| Format | Decision | Reasoning |
|---|---|---|
| **Large anchored adaptive banner** | ✅ **Ship it.** The whole plan. | Highest UX-safety-per-revenue; the only format that fits a 3-screen utility; supported, non-deprecated API. |
| **Interstitial** | ❌ **Do not ship at launch.** | EdgeCase has no natural transition points. Every candidate trigger — `BACK` from a sub-screen, save-and-return — is a *navigation/exit* action, which is the highest-risk interstitial pattern under Play's Ads policy ("ads triggered by exit buttons"). Sessions are short and infrequent; the incremental revenue does not justify the suspension risk on a first-time publisher account. |
| **App-open ad** | ❌ **Do not ship.** | The app is a brief settings shell. Worse: with a live overlay service, the Activity can resume in contexts where a full-screen ad reads as out-of-context. |
| **Native advanced** | 🕐 **v2 candidate.** | Could genuinely be beautiful as a "carved tablet" on the main menu. But native styling has stricter deceptive-ads exposure (the ad must remain unmistakably an ad), and it is a much larger build. Revisit after the banner has a revenue baseline. |
| **Rewarded** | 🕐 **v2 candidate — the best of the optional ones.** | A clean, opt-in value exchange: watch an ad → unlock a premium sliver skin / colour in `SliverCustomizeDialog`. Fully policy-clean because it is user-initiated. Requires building a premium-skin tier first. |
| **Collapsible banner** | 🕐 **Post-launch experiment.** | Anchored-only, higher CPM. Only after the standard plinth is stable and the account is in good standing — the expand-over-content behaviour needs its own UX review against the "ad overlapping app content" rule. |

### 5.3 Ship exactly one banner

Do **not** add a second banner on the Shortcuts screen (as `Publisher.md` §3.4 #2 suggests). Two ad units means two requests, more accidental-click surface, and — per Publisher Policies — pushes the ad-to-content ratio the wrong way on the app's densest screen. One persistent plinth is both more compliant and better monetization.

---

## 6. Visual design spec

### 6.1 Concept

The plinth is the **foundation stone the temple stands on** — a recessed well cut into the floor beneath the pillars. Deliberately the *inverse* of the button language:

| | Buttons (`selector_stone_button`) | The Plinth (`bg_ad_plinth`) |
|---|---|---|
| Reads as | **raised** limestone slab | **recessed** well |
| Elevation | `8dp`, animates on press | `0dp`, never animates |
| Face | light limestone `#CEBFA3` | obsidian `#07090B` |
| Interaction | pressable, haptic, dust burst, crack flash | **none, ever** |

This separation is doing compliance work, not just design work: a user must never mistake the ad band for a tappable EdgeCase control (§1.1 C4).

### 6.2 `res/drawable/bg_ad_plinth.xml`

Mirrors the layer grammar of `bg_temple_lintel.xml` so it is visually native:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Outer blocky border — same dark limestone edge as the lintel -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/limestone_border" />
        </shape>
    </item>
    <!-- Inner emerald hairline -->
    <item android:left="3dp" android:top="3dp" android:right="3dp" android:bottom="3dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/emerald_deep" />
        </shape>
    </item>
    <!-- The well: flat obsidian, no gradient — the ad supplies the light -->
    <item android:left="4dp" android:top="4dp" android:right="4dp" android:bottom="4dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/obsidian_black" />
        </shape>
    </item>
    <!-- Ziggurat corner notches: reads as cut stone, not a CSS box -->
    <item android:gravity="top|start"    android:width="8dp" android:height="8dp">
        <shape android:shape="rectangle"><solid android:color="@color/obsidian_black" /></shape>
    </item>
    <item android:gravity="top|end"      android:width="8dp" android:height="8dp">
        <shape android:shape="rectangle"><solid android:color="@color/obsidian_black" /></shape>
    </item>
    <item android:gravity="bottom|start" android:width="8dp" android:height="8dp">
        <shape android:shape="rectangle"><solid android:color="@color/obsidian_black" /></shape>
    </item>
    <item android:gravity="bottom|end"   android:width="8dp" android:height="8dp">
        <shape android:shape="rectangle"><solid android:color="@color/obsidian_black" /></shape>
    </item>
</layer-list>
```

> **Detail:** the corner notches are 8 dp squares drawn at the drawable's corners. Since this is a `background`, they render *behind* the `AdView` — never over it, so there is **no obstruction risk** (§3.5). But they'd also be *invisible* if the ad covered them. Hence `adFrame` uses **`android:padding="10dp"`** (> 8 dp), so the notches stay visible and the ad never touches the frame. This is why the frame is 10 dp, not 4 dp.

### 6.3 The buffer budget (compliance-critical)

Measured from the bottom edge of the lowest interactive app element to the ad's first pixel:

| Layer | Size | Purpose |
|---|---|---|
| Screen layout `paddingBottom` | 8 dp (Shortcuts) / 16 dp (Positioning) / 32 dp (Main Menu) | existing, unchanged |
| `ic_meander_horizontal` trim | **10 dp** (`@dimen/meander_trim_height`) | the visible separator line — idea #2 |
| `Space` buffer | **10 dp** | dead zone |
| `adFrame` `layout_marginHorizontal` | 8 dp | ad never touches the screen edge |
| `adFrame` `android:padding` | **10 dp** | the stone frame itself — idea #1 |
| **Total vertical buffer** | **≥ 28 dp** (Shortcuts, the tightest) → **≥ 36 dp** (Positioning) → **≥ 52 dp** (Main Menu) | |

Google publishes no numeric minimum — the requirement is qualitative ("some buffer", "clear visual separation"). **28 dp of non-interactive, visually-distinct stone is a defensible, generous interpretation**, and every pixel of it is inert.

**Acceptance test:** on every screen, drag a finger from the lowest button downward. It must cross ≥ 28 dp of stone before reaching any ad pixel, and nothing in that band may respond to touch.

### 6.4 Vertical cost

Large anchored adaptive banner ≈ 90–110 dp on a phone, plus 10 dp trim + 10 dp space + 20 dp frame padding + 14 dp margins ≈ **~145–165 dp** of window height consumed.

Consequences to verify during implementation:

- **Main Menu** — five 56 dp buttons + margins + divider inside a weighted centred block. It will get tight on short devices. **Mitigation:** the buttons are already centred in a `weight=1` block, so they compress gracefully. If a device clips, reduce `@dimen/margin_wide` (24 dp) between buttons to 16 dp *for this screen only* rather than shrinking the ad or the buffer.
- **Shortcuts** — the Altar (`weight=0.38`) / Archives (`weight=0.42`) / actions (`weight=0.10`) split is proportional, so it rescales automatically. Verify the Altar still shows ≥ 2 rows on a small device; if not, rebalance to 0.34 / 0.46 / 0.10.
- **Positioning** — `PositioningView` is `weight=1`; the phone mock will shrink. Verify the drag interaction is still comfortable, and that the mock's aspect ratio handling degrades cleanly.

**Never** solve a space problem by shrinking the buffer or overlapping the ad. Shrink app chrome instead.

### 6.5 `res/layout/layout_ad_plinth.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- The Plinth: the foundation stone the temple stands on. Hosts the anchored
     adaptive banner. NOTHING in this file is clickable, focusable, or animated.
     See Docs/Ads.md §6 before changing any dimension here. -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/adPlinth"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/obsidian_black"
    android:clickable="false"
    android:focusable="false"
    android:importantForAccessibility="no">

    <!-- Separator: the same meander trim that sits under every temple lintel -->
    <ImageView
        android:layout_width="match_parent"
        android:layout_height="@dimen/meander_trim_height"
        android:src="@drawable/ic_meander_horizontal"
        android:scaleType="fitXY"
        android:alpha="0.55"
        android:importantForAccessibility="no" />

    <!-- Dead-zone buffer. Do not remove. -->
    <Space
        android:layout_width="match_parent"
        android:layout_height="@dimen/ad_buffer_gap" />

    <!-- The well. Background only — never a foreground/overlay view (Docs/Ads.md §1.1 C1). -->
    <FrameLayout
        android:id="@+id/adFrame"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginStart="@dimen/ad_frame_margin_h"
        android:layout_marginEnd="@dimen/ad_frame_margin_h"
        android:layout_marginBottom="@dimen/ad_frame_margin_bottom"
        android:padding="@dimen/ad_frame_padding"
        android:background="@drawable/bg_ad_plinth"
        android:clickable="false"
        android:focusable="false" />
    <!-- AdView is added programmatically in AdHost so the adaptive width can be
         measured from adFrame's real width. -->

</LinearLayout>
```

New entries for `res/values/dimens.xml`:

```xml
<!-- ══ Ad plinth (Docs/Ads.md §6.3) — buffer values are compliance-relevant ══ -->
<dimen name="ad_buffer_gap">10dp</dimen>
<dimen name="ad_frame_padding">10dp</dimen>
<dimen name="ad_frame_margin_h">8dp</dimen>
<dimen name="ad_frame_margin_bottom">6dp</dimen>
```

---

## 7. Implementation plan

Ten phases. Phases 0–7 ship v1.5.0; phase 8 is fallback documentation; phase 9 is post-launch.

### 7.1 Phase 0 — AdMob account & ad units

*(Blocking prerequisite; nothing else can be finished without the real IDs.)*

1. Create / sign in at [admob.google.com](https://admob.google.com).
2. **Add app** → Android → link to the Play Console listing if it exists, otherwise "not listed yet" (relink after publishing — the unlinked path costs fill rate).
3. Record the **App ID**: `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY` (tilde).
4. Create **one** ad unit: type **Banner**, name `EdgeCase — Plinth Banner`. Record `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ` (slash).
5. In that ad unit's settings, set **automatic refresh to 60 seconds** (§3.6). Do not disable it and do not implement manual refresh.
6. **Privacy & messaging** → create an **EU consent message** (GDPR/TCF) and a **US states** message. UMP only shows what exists here.
7. Add payment + tax details.
8. **Register your development devices** as test devices (AdMob → Settings → Test devices), using the device hash from logcat on first run. *This is the single most important step for not getting flagged.*

**Deliverable:** App ID + banner unit ID, recorded in a password manager, not in the repo history until phase 1.

### 7.2 Phase 1 — Dependencies & manifest

**`gradle/libs.versions.toml`**

```toml
[versions]
# … existing …
adsMobileSdk = "1.4.0"
userMessagingPlatform = "4.0.0"

[libraries]
# … existing …
ads-mobile-sdk = { group = "com.google.android.libraries.ads.mobile.sdk", name = "ads-mobile-sdk", version.ref = "adsMobileSdk" }
user-messaging-platform = { group = "com.google.android.ump", name = "user-messaging-platform", version.ref = "userMessagingPlatform" }
```

**`app/build.gradle.kts`**

```kotlin
dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.ads.mobile.sdk)              // NEW
    implementation(libs.user.messaging.platform)     // NEW
    // … test deps unchanged …
}
```

Ad IDs per build type — test IDs in debug, production IDs in release, so a debug build can **never** hit a live unit:

```kotlin
buildTypes {
    debug {
        resValue("string", "admob_app_id",     "ca-app-pub-3940256099942544~3347511713")
        resValue("string", "admob_banner_unit", "ca-app-pub-3940256099942544/9214589741")
    }
    release {
        isMinifyEnabled = true    // flipped 2026-09-04; see Publisher.md §2.1
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        resValue("string", "admob_app_id",     "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY")
        resValue("string", "admob_banner_unit", "ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ")
    }
}
```

> `ca-app-pub-3940256099942544/9214589741` is Google's official **anchored adaptive banner** test unit; `~3347511713` is the test App ID. Neither is tied to your account.
> AdMob App IDs and ad unit IDs are **not secrets** — they ship inside every APK. Committing the real ones is fine.

**`app/src/main/AndroidManifest.xml`** — inside `<application>`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="@string/admob_app_id" />
```

> A missing or malformed `APPLICATION_ID` meta-data **crashes the app at startup**. Verify it merged: `./gradlew :app:processDebugMainManifest` then inspect `app/build/intermediates/merged_manifest/debug/…/AndroidManifest.xml`.

**Advertising ID permission.** `targetSdk = 36` (> 33), so `com.google.android.gms.permission.AD_ID` is required to read the ad ID. The SDK's library manifest declares it and it merges automatically — **do not** add it by hand, but **do** confirm it appears in the merged manifest, because the Play Console **Advertising ID declaration** must match. If it is absent, the ad ID is zeroed out and fill/CPM collapse.

**R8:** GMA ships consumer ProGuard rules. When `isMinifyEnabled` is turned on (a separate `Publisher.md` §2.1 task) **no additional AdMob keep rules are needed** — the commented block in `Publisher.md` §2.2 can stay commented out.

**Acceptance:** app builds and launches; merged manifest contains `APPLICATION_ID` and `AD_ID`.

### 7.3 Phase 2 — The `AdHost`

New file: `app/src/main/java/com/dicereligion/edgecase/AdHost.kt`

One class owns consent, SDK init, banner lifecycle, and the space reservation. `MainActivity` gains four call sites and no ad logic.

```kotlin
package com.dicereligion.edgecase

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns every ad-related concern in EdgeCase.
 *
 * Invariants — see Docs/Ads.md before changing any of these:
 *  • Exactly one AdView exists for the Activity's lifetime.
 *  • The AdView is the ONLY child of [adFrame]; the stone frame is adFrame's
 *    *background*, never a view over the ad (Docs/Ads.md §1.1 C1, §3.5).
 *  • Nothing here is ever constructed from SidebarService (Docs/Ads.md §4.2).
 *  • Ads are requested only after UMP reports canRequestAds() == true.
 */
class AdHost(private val activity: Activity, private val adFrame: FrameLayout) {

    companion object {
        private const val TAG = "EdgeCaseAds"
        /** SDK init is process-wide; guard it against Activity recreation. */
        private val sdkInitialized = AtomicBoolean(false)
    }

    private var adView: AdView? = null
    private var adRequested = false
    private lateinit var consentInformation: ConsentInformation

    // ──────────────────────────────────────────────
    // 1. Consent (UMP) — must resolve before any ad request
    // ──────────────────────────────────────────────

    fun start() {
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        // Consent info must be refreshed on every app launch.
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode} ${formError.message}")
                    }
                    if (consentInformation.canRequestAds()) initializeAndLoad()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent update failed: ${requestError.errorCode} ${requestError.message}")
                // Non-fatal: cached consent may still permit (limited) ads.
                if (consentInformation.canRequestAds()) initializeAndLoad()
            }
        )

        // Cached consent from a previous session can allow an immediate request.
        if (consentInformation.canRequestAds()) initializeAndLoad()
    }

    /** True when the "Privacy options" entry point must be surfaced in the UI. */
    fun isPrivacyOptionsRequired(): Boolean =
        ::consentInformation.isInitialized &&
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Re-opens the consent form on user request (Docs/Ads.md §7.7). */
    fun showPrivacyOptionsForm() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy form error: ${formError.errorCode} ${formError.message}")
            }
        }
    }

    // ──────────────────────────────────────────────
    // 2. SDK initialisation — MUST be off the main thread (ANR risk)
    // ──────────────────────────────────────────────

    private fun initializeAndLoad() {
        if (adRequested) return
        adRequested = true

        if (sdkInitialized.compareAndSet(false, true)) {
            // Matches the codebase's existing raw-Thread idiom (see MainActivity.preloadApps);
            // avoids pulling kotlinx-coroutines in for one call.
            Thread {
                MobileAds.initialize(
                    activity.applicationContext,
                    InitializationConfig.Builder(
                        activity.getString(R.string.admob_app_id)
                    ).build()
                ) {
                    activity.runOnUiThread { attachBanner() }
                }
            }.start()
        } else {
            attachBanner()
        }
    }

    // ──────────────────────────────────────────────
    // 3. Banner — reserve space first, then load
    // ──────────────────────────────────────────────

    private fun attachBanner() {
        if (activity.isFinishing || activity.isDestroyed) return
        if (adView != null) return

        // Width must come from adFrame's real inner width, so wait for layout.
        adFrame.doOnLayout {
            val density = activity.resources.displayMetrics.density
            val innerPx = (adFrame.width - adFrame.paddingLeft - adFrame.paddingRight)
                .coerceAtLeast(1)
            val widthDp = (innerPx / density).toInt()
            if (widthDp <= 0) return@doOnLayout

            // Current, non-deprecated anchored adaptive API (Docs/Ads.md §3.8).
            val adSize = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, widthDp)

            // "Fixed space allocation": reserve the slot BEFORE the ad arrives so the
            // app UI never shifts under the user's finger (Docs/Ads.md §3.1 item 4).
            val reservedPx = (adSize.height * density).toInt() +
                adFrame.paddingTop + adFrame.paddingBottom
            adFrame.minimumHeight = reservedPx

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
                    Log.d(TAG, "Plinth banner loaded.")
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // No retry storm: a failed banner leaves an empty, correctly sized well.
                    Log.w(TAG, "Plinth banner failed: $adError")
                }
            })
        }
    }

    // ──────────────────────────────────────────────
    // 4. Teardown
    // ──────────────────────────────────────────────

    fun destroy() {
        adView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        adView = null
    }
}
```

**Notes on decisions in this file**

- **No coroutines.** The project has no `kotlinx-coroutines` dependency and already uses raw `Thread { }` in `MainActivity`. Adding a coroutines dependency for one background call is unwarranted; the Next-Gen requirement is only "off the main thread." *(If coroutines are added later for other reasons, switch to `CoroutineScope(Dispatchers.IO).launch { … }` as the docs show.)*
- **No retry loop on failure.** A failed fill leaves a correctly sized empty well; automatic refresh (configured server-side) will fill it on the next cycle. Client-side retry loops are an invalid-traffic pattern.
- **`sdkInitialized` is static** because `MobileAds.initialize` is process-scoped and `MainActivity` can be recreated on rotation/config change.
- **`doOnLayout`** comes from `androidx.core.view` — already on the classpath via `core-ktx`.

**Acceptance:** with debug (test) IDs, a "Test Ad" labelled banner appears inside the stone well on all three screens.

### 7.4 Phase 3 — Restructure `activity_main.xml`

The **only** existing layout file that changes.

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Root is a vertical column: app content on top (weighted), the ad plinth pinned
     below it. fitsSystemWindows lives here so the nav-bar inset lands BELOW the
     plinth — the ad must never overlap system navigation (Docs/Ads.md §5.1). -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/rootColumn"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/obsidian_black"
    android:fitsSystemWindows="true">

    <FrameLayout
        android:id="@+id/screenContainer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <!-- Screen 1: Main Menu (visible by default) -->
        <include
            android:id="@+id/screenMainMenu"
            layout="@layout/layout_screen_main_menu"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="visible" />

        <!-- Screen 2: Shortcuts Configuration -->
        <include
            android:id="@+id/screenShortcuts"
            layout="@layout/layout_screen_shortcuts_container"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone" />

        <!-- Screen 3: Positioning -->
        <include
            android:id="@+id/screenPositioning"
            layout="@layout/layout_screen_positioning_container"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone" />

    </FrameLayout>

    <!-- The Plinth — persistent ad band, all screens. See Docs/Ads.md §5, §6. -->
    <include
        android:id="@+id/adPlinth"
        layout="@layout/layout_ad_plinth"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

</LinearLayout>
```

**No other layout file needs editing for the ad to work.** `R.id.screenContainer` is not referenced anywhere in Kotlin (verified), so moving it is safe. The three screens keep `match_parent` inside a shorter box.

Then apply the §6.4 verification pass on small screens and adjust *app chrome only* if anything clips.

### 7.5 Phase 4 — Wire `MainActivity`

Four additions. No changes to `showScreen()`, the back-stack logic, or any screen initialisation.

```kotlin
// ── field, near the other screen-state fields ──
private var adHost: AdHost? = null
```

```kotlin
// ── in onCreate(), AFTER setContentView(R.layout.activity_main) ──
adHost = AdHost(this, findViewById(R.id.adFrame)).also { it.start() }
```

```kotlin
// ── new override ──
override fun onDestroy() {
    adHost?.destroy()
    adHost = null
    super.onDestroy()
}
```

> The Next-Gen `AdView` does not require the legacy `pause()`/`resume()` calls in `onPause`/`onResume`; `destroy()` in `onDestroy` is the required lifecycle hook.

**Acceptance:** navigate Main → Shortcuts → Positioning → back. The banner stays put, does not flicker, does not reload on navigation, and nothing shifts.

### 7.6 Phase 5 — Overlay suspension (compliance-critical)

Implements [§4.3](#43-the-fix-for-the-obstruction). Prevents the sliver overlay from covering the banner.

**`SidebarService.kt`** — add to `companion object`:

```kotlin
const val ACTION_SUSPEND_OVERLAY = "com.dicereligion.edgecase.SUSPEND_OVERLAY"
const val ACTION_RESUME_OVERLAY  = "com.dicereligion.edgecase.RESUME_OVERLAY"
```

Extend `onStartCommand`:

```kotlin
ACTION_SUSPEND_OVERLAY -> detachOverlayWindows()
ACTION_RESUME_OVERLAY  -> addSliverIfNeeded()
```

Add alongside the existing `addSliverIfNeeded()`:

```kotlin
/**
 * Detaches the sliver (and any open tray) without stopping the service.
 *
 * Called while MainActivity is in the foreground so the overlay can never sit on
 * top of the banner ad — an overlay over an ad is an obstruction that breaks
 * viewability and risks invalid impressions (Docs/Ads.md §3.5, §4).
 * It is also correct UX: the edge launcher has no business floating over its own
 * settings screen. Idempotent.
 */
private fun detachOverlayWindows() {
    if (sliverAdded && ::sliverView.isInitialized && sliverView.isAttachedToWindow) {
        try { windowManager.removeView(sliverView) } catch (_: Exception) {}
    }
    sliverAdded = false
    if (::trayView.isInitialized && trayView.isAttachedToWindow) {
        try { windowManager.removeView(trayView) } catch (_: Exception) {}
    }
}
```

**`MainActivity.kt`** — the service must not be *started* by these intents, only signalled, so both calls are guarded by `SidebarService.isRunning`:

```kotlin
override fun onResume() {
    super.onResume()
    serviceEyes.forEach { it.setRunning(SidebarService.isRunning) }
    setOverlaySuspended(true)    // NEW — hide the fang while our own UI (and the ad) is visible
}

override fun onPause() {
    super.onPause()
    setOverlaySuspended(false)   // NEW — hand the edge back to the user
}

/** Signals the running service; never starts it. */
private fun setOverlaySuspended(suspended: Boolean) {
    if (!SidebarService.isRunning) return
    startService(Intent(this, SidebarService::class.java).apply {
        action = if (suspended) SidebarService.ACTION_SUSPEND_OVERLAY
                 else SidebarService.ACTION_RESUME_OVERLAY
    })
}
```

**Interaction with the existing Start button.** `btnStartService` currently calls `startEdgeService()` while the Activity is foreground, so `onCreate`/`onResume` ordering matters: the service starts, `onCreate` calls `addSliverIfNeeded()`, and the sliver appears over our UI. Fix by suspending immediately after start:

```kotlin
applyStoneButtonBehavior(findViewById<Button>(R.id.btnStartService)).setOnClickListener {
    if (checkAndRequestPermissions()) {
        startEdgeService()
        serviceEyes.forEach { it.setRunning(true) }
        setOverlaySuspended(true)   // NEW — it will reappear when the user leaves the app
    }
}
```

Note `setOverlaySuspended` early-returns on `!SidebarService.isRunning`; `isRunning` is set in `SidebarService.onCreate`, which for a foreground service start is synchronous enough in practice, but post the suspend to the view's message queue (`btnStartService.post { setOverlaySuspended(true) }`) if a race shows up in testing.

**Acceptance:**
1. Start the service → the fang disappears while the app is foreground.
2. Press Home → the fang reappears over the launcher; swipe it → the tray still works.
3. Return to EdgeCase → the fang disappears again; the banner is never covered.
4. Set the sliver position to the very bottom (`yBias = 1.0`), re-enter the app: **no overlay anywhere near the plinth.**
5. Stop the service → no crashes, no orphaned windows.

### 7.7 Phase 6 — The privacy options entry point (and a use for `btnDummy`)

When UMP reports `PrivacyOptionsRequirementStatus.REQUIRED`, the app **must** offer a way to reopen the consent form. `Publisher.md` §2.7 already flags the placeholder `btnDummy` ("Dummy — nothing here yet") as a pre-launch blocker. One change solves both.

Repurpose `btnDummy` → **`btnPrivacy`**, labelled in the app's voice (e.g. **`THE COVENANT`**, or plainly `PRIVACY`):

```kotlin
val btnPrivacy = findViewById<Button>(R.id.btnPrivacy)
applyStoneButtonBehavior(btnPrivacy).setOnClickListener {
    adHost?.showPrivacyOptionsForm()
}
// Only shown where consent management is actually required (EEA/UK/CH, US states).
btnPrivacy.visibility = if (adHost?.isPrivacyOptionsRequired() == true) View.VISIBLE else View.GONE
```

`isPrivacyOptionsRequired()` is only meaningful after `requestConsentInfoUpdate` resolves, so re-evaluate the visibility from the UMP callback rather than only in `onCreate` — add a small `onConsentResolved: () -> Unit` callback to `AdHost` and toggle visibility there.

Where the button is hidden, the main menu keeps a clean four-button stack, which also buys back the vertical space the plinth consumes (§6.4).

**Acceptance:** on an EEA-simulated device (AdMob → Privacy & messaging → debug geography = EEA), the consent form appears on first launch, the button is visible, and tapping it reopens the form.

### 7.8 Phase 7 — Privacy policy, Play Console, versioning

1. **Privacy policy** (hard requirement — `Publisher.md` §4.2.2). Must now disclose: advertising ID collection, AdMob as a third-party ad partner, personalised vs non-personalised ads, and the installed-app-list access the launcher already requires. Host it (GitHub Pages / Firebase Hosting) and put the URL in the Play listing.
2. **Play Console → App content → Ads:** declare **"Yes, my app contains ads."** The store listing gets a *Contains ads* badge.
3. **Play Console → App content → Advertising ID:** declare use of the ad ID, purpose = **Advertising or marketing**. Must match the merged `AD_ID` permission (§7.2).
4. **Data safety:** add *Device or other IDs → Advertising ID*, collected & shared, for Advertising. Keep the existing installed-apps disclosure.
5. **Content rating:** re-run the questionnaire — it now asks about ads.
6. **Not child-directed.** EdgeCase is a system utility. Do **not** set `TagForChildDirectedTreatment`; leave the default. If the content rating ever lands in a family bracket, the Families ads policy (certified SDKs, no interest-based ads) applies and this plan must be revisited.
7. **Version bump:** `versionCode = 4`, `versionName = "1.5.0"`. The main-menu `tvVersion` picks it up automatically from `BuildConfig.VERSION_NAME`. **✅ Done 2026-09-04.**

> **Phase 7 status (2026-09-04).** Item 1 (privacy policy) and item 7 (version bump) are done. Items
> 2–6 are all Play Console work and remain blocked on the developer account. See `Docs/stats.md`
> Appendix C, B6 — including the two open questions that must be settled before the Data safety form
> is filled: the `READ_BASIC_PHONE_STATE` disclosure gap, and whether installed apps count as
> "collected" (`Publisher.md` §5.4 says yes; policy claim P2 says no).

### 7.9 Fallback: legacy SDK variant

If Next-Gen causes trouble (a mediation need appears, or a bug blocks the release), the plan is unchanged except inside `AdHost`. Legacy equivalents:

| Next-Gen | Legacy 25.4.0 |
|---|---|
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0` | `com.google.android.gms:play-services-ads:25.4.0` |
| `MobileAds.initialize(ctx, InitializationConfig.Builder(appId).build()) { }` | `MobileAds.initialize(ctx) { }` |
| `AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, widthDp)` | same name, same signature |
| `AdView` + `BannerAdRequest` + `AdLoadCallback<BannerAd>` | `AdView.setAdUnitId()` / `setAdSize()` / `loadAd(AdRequest.Builder().build())` |
| `adView.destroy()` in `onDestroy` | `pause()`/`resume()`/`destroy()` in the matching lifecycle callbacks |

The layout, the plinth drawable, the buffer budget, the overlay suspension, and every compliance decision carry over unchanged.

### 7.10 Post-launch (v2 candidates, in priority order)

1. **Rewarded ad → premium sliver skins** in `SliverCustomizeDialog`. Opt-in, policy-clean, thematically perfect.
2. **Collapsible banner** experiment on the plinth (anchored-only; needs its own UX review).
3. **Native advanced "carved tablet"** on the main menu — highest CPM, highest design and compliance cost.
4. **Remove-ads IAP.** Meaningfully raises goodwill and is often required for a good store rating on utility apps.

Interstitials remain off the table unless a genuine, non-exit transition point appears in the app.

---

## 8. Compliance checklist

Run this before every release that touches ads.

### Placement
- [ ] Exactly **one** banner ad unit in the entire app.
- [ ] The banner is anchored at the bottom of the Activity window, never inside a scrolling container.
- [ ] ≥ 28 dp of non-interactive space between the lowest interactive element and the first ad pixel, on **all three** screens (§6.3).
- [ ] Nothing inside `layout_ad_plinth.xml` is `clickable`, `focusable`, or animated.
- [ ] No app text, icon, label, or caption inside `adFrame`.
- [ ] The frame is `android:background` on `adFrame`, **not** a `foreground` and **not** a sibling view over the `AdView`.
- [ ] `adFrame.minimumHeight` is set from the computed `AdSize` **before** `loadAd`, so no layout shift occurs.
- [ ] The ad sits above the system navigation inset and never overlaps system UI.

### The overlay
- [ ] No `com.google.android.libraries.ads` (or `com.google.android.gms.ads`) import exists in `SidebarService.kt`, `ArcSliverView.kt`, `SliverPreviewView.kt`, or `PositioningView.kt`.
  - Verify: `grep -rn "ads.mobile.sdk\|gms.ads" app/src/main/java/ | grep -v "MainActivity\|AdHost"` returns nothing.
- [ ] The overlay is suspended whenever `MainActivity` is in the foreground.
- [ ] With `sliver_y_bias = 1.0` and the service running, re-entering the app leaves the plinth completely unobstructed.

### Consent & privacy
- [ ] UMP 4.0.0 integrated; `requestConsentInfoUpdate` runs on **every** launch.
- [ ] No ad is requested until `canRequestAds()` returns `true`.
- [ ] A privacy-options entry point is visible whenever `PrivacyOptionsRequirementStatus == REQUIRED`.
- [ ] EEA and US-states messages exist in the AdMob console. ⚠️ **Confirmed ABSENT 2026-09-04** —
  UMP returns *"Publisher misconfiguration … no form(s) configured for the input app ID"*. Note what
  this costs: the consent-failure path falls through to cached `canRequestAds()`, which is permissive
  outside a consent regime, so a non-EEA test device still shows a banner and the fault is invisible.
  **In the EEA it serves nothing.** This is the §11 "UMP not implemented → EEA ad serving restricted"
  risk materialising through the console rather than the code.
- [ ] Privacy policy is live, discloses AdMob + advertising ID + installed-app access, and its URL is in the Play listing.

### Play Console
- [ ] "Contains ads" declared.
- [ ] Advertising ID declaration completed and consistent with the merged `AD_ID` permission.
- [ ] Data safety lists Advertising ID as collected and shared.
- [ ] Content rating questionnaire re-submitted.

### Build & test hygiene
- [ ] Debug builds resolve **test** ad unit IDs; release builds resolve production IDs (`resValue` per build type).
- [ ] Every development device is registered as an AdMob test device.
- [ ] Nobody has clicked a live ad in a release build. Not once.
- [ ] Banner auto-refresh is set to 60 s in the AdMob console; there is no client-side refresh or retry loop.
- [ ] `APPLICATION_ID` meta-data present in the merged manifest (a missing one crashes at launch).
- [x] **R8 has not broken the SDK.** Verified on device 2026-09-04: `Plinth banner loaded
  (411×128dp)` from a signed release build. The ad AARs' own consumer rules were sufficient **for
  the ad SDKs** — but not for what they drag in: R8 stripped a constructor from Room 2.2.5 (via
  `androidx.work`) and the app crashed on launch until a keep rule was added
  (`Docs/stats.md` §4). **Re-run this check by launching, not compiling, after any dependency bump.**

---

## 9. Corrections to `Publisher.md` §3

`Publisher.md` §3 was written against an older SDK and contains guidance that would now cause problems. Fix or annotate it to point here.

| `Publisher.md` §3 says | Status | Correction |
|---|---|---|
| `play-services-ads = "23.6.0"` | **Outdated** | Legacy is at 25.4.0 and in maintenance mode. Use GMA Next-Gen `ads-mobile-sdk:1.4.0` (§3.7). |
| `MobileAds.initialize(this) { }` in `onCreate` on the main thread | **Wrong for Next-Gen** | Initialisation must run off the main thread or it risks an ANR (§7.3). |
| `ads:adSize="BANNER"` (fixed 320×50) in XML | **Deprecated approach** | Smart/fixed banners are superseded by adaptive. Use `getLargeAnchoredAdaptiveBannerAdSize` with a runtime-measured width (§3.8). |
| §3.4 #1: banner between the spear divider and the Start/Stop buttons | **Policy hazard** | That is "ad sandwiched between app items" — the ad sits between content and the primary action buttons (§3.2 #2). Move it out of the content column entirely, into the plinth. |
| §3.4 #2: second banner "between Back and Save buttons" on Shortcuts | **Policy hazard — drop it** | Directly adjacent to custom navigation buttons (§3.2 #1) and duplicates the ad unit. One plinth banner only (§5.3). |
| §3.4 #3: interstitial on Shortcuts/Positioning → Main Menu back-navigation | **High risk — drop it** | Back-triggered full-screen ads are the classic disruptive-ads pattern; Play forbids ads triggered by exit actions and treats back-navigation interstitials as a red flag on new accounts (§5.2). |
| §3.4 "Where NOT to put ads": *"ads in the overlay tray… **may** be considered disruptive"* | **Understated** | It is a definite violation, not a maybe: ads may only be displayed inside the app serving them, and never in a surface running over other apps (§3.3, §4). Strengthen the wording to an absolute prohibition. |
| §4.2.1: `user-messaging-platform:3.1.0` | **Outdated** | 4.0.0 (a dependency of legacy 25.0.0+). |
| Test IDs listed as banner `…/6300978111` | **Wrong unit for this design** | That is the *fixed-size* banner test unit. The anchored **adaptive** test unit is `ca-app-pub-3940256099942544/9214589741`. |
| §2.2: commented AdMob ProGuard keep rules | **Unnecessary** | GMA ships consumer ProGuard rules; no extra keep rules needed when R8 is enabled. Leave commented. |
| §2.7: "Handle the Dummy Button" | **Now resolved by this plan** | `btnDummy` becomes the privacy-options entry point (§7.7). |

---

## 10. Testing & rollout

### 10.1 Local

| Test | Expected |
|---|---|
| Cold launch, debug build | Consent flow (if applicable) → "Test Ad" banner inside the stone well |
| Navigate all three screens repeatedly | Banner never reloads, never flickers, never moves |
| Rotate the device (portrait ⇄ landscape) | Banner re-measures to the new width; no crash; no double `AdView` |
| Small-screen device (e.g. 5.0", 320 dp wide) | No clipping on any screen; Altar still shows ≥ 2 rows |
| Large / tablet-width device | Banner fills the frame; frame does not look stretched |
| Airplane mode | Empty well of the correct reserved height; no crash; no retry storm |
| Service running + sliver at `yBias = 1.0` | Fang absent while app is foreground; present after Home |
| Drag-to-reorder in the Altar, dragging downward | Finger never reaches an ad pixel; the drag terminates in stone |
| `PositioningView` drag to the bottom | Same |
| Rapid Home ⇄ app switching, 10× | No orphaned overlay windows, no leaked `AdView`, no ANR |
| `SliverCustomizeDialog` open | No ad in the dialog; plinth still visible behind it, unobscured |
| Kill and relaunch after granting consent | No consent form re-shown; ad loads on cached consent |

### 10.2 Pre-release

- Build a **release** AAB with production IDs; install via internal test track. **Do not click the ad.**
- Confirm impressions register in the AdMob dashboard within a few hours.
- Run the Play **pre-launch report** and check for ANRs around SDK init.
- Verify the *Contains ads* badge appears on the internal-track listing.
- Screenshot all three screens with the plinth for the store listing — Play requires screenshots to reflect the actual app, ads included.

### 10.3 Staged rollout

Ship v1.5.0 to internal testing first (which `Publisher.md` §7.1 requires anyway: 20 testers for a new developer account), then 20 % → 50 % → 100 %, watching:

- **AdMob:** match rate, fill rate, and — most importantly — **CTR**. A banner CTR above ~2–3 % on a utility app is a strong signal of accidental clicks and an invalid-activity flag waiting to happen. If it appears, **increase the buffer**; do not wait for Google to act.
- **Play Console:** ANR/crash rate, especially around `MobileAds.initialize`.
- **Reviews:** any mention of ads being in the way is a placement bug, not a user complaint to dismiss.

---

## 11. Risks and things that will get us limited

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| **Overlay covers the banner → invalid/unviewable impressions** | High **if unmitigated** | Severe (ad serving disabled) | Phase 5 overlay suspension. This is the top risk in this app. |
| **Accidental clicks from the drag surfaces + adjacent nav buttons** | Medium | Severe | 28 dp inert buffer (§6.3); watch CTR post-launch. |
| **Self-clicking during development** | Medium | Severe (account-level) | Test IDs in debug via `resValue`; register test devices; a standing "never tap the ad" rule. |
| **Missing/incorrect `APPLICATION_ID` meta-data** | Medium | App crashes at launch | Verify in the merged manifest during phase 1. |
| **UMP not implemented → EEA ad serving restricted** | **Materialised** (console side) | Revenue collapse in EU | The code is correct — UMP 4.0.0 gated on `canRequestAds()` — but **no consent messages exist in the AdMob console** (confirmed on device 2026-09-04), so UMP has nothing to serve. Mitigation is now a console task, not a code one. |
| **Ad ID declaration mismatch in Play Console** | Medium | Ad ID zeroed → fill/CPM collapse | Phase 7 items 3–4; check the merged manifest. |
| **Plinth squeezes the UI on small screens** | Medium | Poor reviews | §6.4 verification; shrink app chrome, never the buffer. |
| **`SYSTEM_ALERT_WINDOW` + ads draws extra review scrutiny** | Medium | Review delay / rejection | Prominent disclosure (`Publisher.md` §5.3); the overlay is demonstrably ad-free (§4.2 grep check). |
| **Someone later "just adds a small ad to the tray"** | Low but catastrophic | App removal | §4.2 written as a hard rule; the grep in §8 belongs in CI. |
| **Next-Gen SDK immaturity (GA since Apr 2026)** | Low | Schedule slip | §7.9 legacy fallback keeps everything but `AdHost` unchanged. |
| **CTR anomaly triggers automated review** | Low | Ad serving limited | Monitor from day one; widen the buffer at the first sign. |

---

## 12. Sources

Policy and SDK documentation consulted on **2026-08-28**. Google revises these pages without notice — re-verify version numbers and API names before implementation begins.

**AdMob placement policy**
- [Banner ad guidance](https://support.google.com/admob/answer/6128877?hl=en)
- [Recommended banner implementations](https://support.google.com/admob/answer/6275335?hl=en) — the "Ad separated by border" recommendation
- [Discouraged banner implementations](https://support.google.com/admob/answer/6275345?hl=en)
- [Implementation guidance](https://support.google.com/admob/answer/2936217?hl=en) — 60 s refresh, incentivisation
- [Overview of banner ads](https://support.google.com/admob/answer/9993556?hl=en) — anchored adaptive height caps
- [About collapsible banner ads](https://support.google.com/admob/answer/14160679?hl=en)
- [Invalid traffic](https://support.google.com/admob/answer/3342054?hl=en)
- [Behavioral policies](https://support.google.com/admob/answer/2753860?hl=en)

**Google Play & Publisher policy**
- [Google Play Ads policy](https://support.google.com/googleplay/android-developer/answer/9857753?hl=en)
- [Google Publisher Policies — ad placement](https://support.google.com/publisherpolicies/answer/10502938?hl=en)
- [Advertising ID — Play Console Help](https://support.google.com/googleplay/android-developer/answer/6048248?hl=en)
- [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/16559646?hl=en)

**SDK documentation**
- [Set up GMA Next-Gen SDK (Android)](https://developers.google.com/admob/android/next-gen/quick-start)
- [GMA Next-Gen banner ads](https://developers.google.com/admob/android/next-gen/banner)
- [GMA Next-Gen release notes](https://developers.google.com/admob/android/next-gen/rel-notes)
- [GMA Next-Gen `AdSize` reference](https://developers.google.com/admob/android/next-gen/reference/com/google/android/libraries/ads/mobile/sdk/banner/AdSize)
- [Set up Google Mobile Ads SDK (Legacy)](https://developers.google.com/admob/android/quick-start)
- [Legacy SDK release notes](https://developers.google.com/admob/android/rel-notes)
- [Test ads](https://developers.google.com/admob/android/test-ads)
- [Privacy & UMP (Android)](https://developers.google.com/admob/android/privacy)
- [Privacy strategies for Android](https://support.google.com/admob/answer/11402075?hl=en)
- [Set up Open Measurement (obstruction rules)](https://developers.google.com/admob/android/open-measurement)
- [Announcing the Google Mobile Ads Next-Gen SDK for Android](https://ads-developers.googleblog.com/2026/01/announcing-google-mobile-ads-next-gen.html)

---

*End of document.*
