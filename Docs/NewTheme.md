# EdgeCase — "OBSIDIAN SERPENT" Theme & Style Guide

> **Document:** `Docs/NewTheme.md`
> **Created:** 2026-07-11
> **Target version:** v1.4.0 (theme overhaul release)
> **Scope:** Complete visual overhaul of every screen, drawable, style, and decorative element —
> **plus four functional UI changes (§12):** expanded positioning canvas + footer button swap,
> a draggable sliver-tracking arrow, the twin-fang divider, and back-gesture navigation.
> **Non-scope:** No other behavioral change — every button ID, callback, prefs key, service
> action, and screen flow stays exactly as documented in `Docs/stats.md`.
>
> **Decisions locked (2026-07-12):** the open points from the first review are now resolved and
> folded in below — Greek-glyph title (§4.3), **bundled fonts sourced** into `res/font/` with OFL
> licenses in `Docs/fonts_licenses/` (§4.2), fixed background seed & `maxGlowAlpha 0.55` (§6.5),
> recommended limestone bevel (§7.3) and `fitXY` pillars (§8.4), `fitXY` twin-fang divider (§10.4),
> version label in `CaptionChiseled` (§10.6), tracking-arrow flip at snap-end (§12.2), the dialog
> `ic_silver_ring` removed (§9), the `fontFamily="serif"` sweep scoped to **buttons only** (§7.5),
> and the former "phase 2" items (gem checkboxes, square dialog background, legacy-style migration)
> pulled into scope (§10.5, §10.2, §14). Search "**Decision (2026-07-12)**" for each inline note.
> The title's Greek Ξ/Λ coverage is resolved with **GFS Neohellenic Bold** (§4.3).

---

## Table of Contents

1. [Design Vision — "The Serpent's Temple"](#1-design-vision)
2. [Design Laws (non-negotiable rules)](#2-design-laws)
3. [Color System — the Obsidian Serpent palette](#3-color-system)
4. [Typography — blocky Greek letterforms](#4-typography)
5. [The Temple Lintel — unified header component](#5-the-temple-lintel--unified-header)
6. [The Living Background — obsidian, cracks, pulsating emeralds](#6-the-living-background)
7. [Stone Slab Buttons — cracked limestone](#7-stone-slab-buttons)
8. [Serpentine Pillars — the new columns](#8-serpentine-pillars)
9. [De-rounding the rest of the app](#9-de-rounding-the-rest-of-the-app)
10. [Secondary elements — panels, dividers, checkboxes, tiles](#10-secondary-elements)
11. [Creative Suggestions — going further (§5 of the brief)](#11-creative-suggestions)
12. [Functional UI Changes — canvas, tracking arrow, divider, back gesture](#12-functional-ui-changes)
13. [Architecture & File Manifest](#13-architecture--file-manifest)
14. [Implementation Plan — phased, ordered, checklisted](#14-implementation-plan)
15. [Performance Budget & Android APIs used](#15-performance-budget--android-apis)
16. [QA / Verification Checklist](#16-qa--verification-checklist)

---

## 1. Design Vision

**Codename: OBSIDIAN SERPENT.**

The app is no longer merely "themed like ancient Greece" — it *is* a place: the inner sanctum of
a ruined Hellenic serpent-cult temple, carved from black volcanic glass, lit from below by veins
of raw emerald that pulse like a sleeping creature's heartbeat. Every interaction should feel
like operating ancient stone machinery:

- **Weight.** Buttons are limestone slabs. They sink when pressed (already implemented), they are
  square, thick-bordered, and cracked with age.
- **Darkness with life.** The background is not a flat color — it is fractured obsidian, and
  through the fractures, emerald gems *breathe* light.
- **Monumental lettering.** Titles look chiseled by a mason using blocky, Greek-inscription
  letterforms — never thin, never rounded, always centered like a temple pediment inscription.
- **The serpent is everywhere but never cartoonish.** It coils around the pillars, its scales
  pattern the tray, its eye is the service indicator. Suggestion, not mascot.

Everything below is specified to the pixel so it can be implemented file-by-file.

---

## 2. Design Laws

These are the invariants every new or modified asset MUST obey:

| # | Law | Enforcement |
|---|-----|-------------|
| L1 | **Zero rounded corners.** No `<corners>` radius > 0dp in any drawable. No `cornerRadius` in code. Dialogs get square window backgrounds. | Audit: `grep -rn "radius" app/src/main/res/drawable/` must show only `0dp` (or the tag removed). `PositioningView.mockupCornerRadius = 0f`. |
| L2 | **Every screen shares the identical header component** (same drawable, same text style, same centering, same height). | One include-able layout: `layout_temple_header.xml`. |
| L3 | **Borders are blocky and doubled**: a thick dark outer line + a thin light inner line (chiseled edge). Single hairline borders are forbidden on interactive elements. | All button/panel drawables use the double-stroke layer-list pattern in §7.3. |
| L4 | **Emerald is the only luminous color.** Nothing else glows, pulses, or saturates. Sandstone/limestone/marble/silver are all matte. | Palette discipline (§3). |
| L5 | **No functional regressions.** All view IDs referenced from Kotlin (`btnShortcuts`, `rvAltarShortcuts`, `positioningView`, `dustContainer`, `tvPositionInfo`, `tvAltarEmpty`, etc.) keep their IDs and types. | Diff review + run app. |
| L6 | **Animations are ambient, slow, and cheap.** Background pulses run at 2.4–4.8s periods, GPU-friendly (shaders, not blurs), and pause when the view is invisible. | §6.6, §15. |

---

## 3. Color System

### 3.1 Palette philosophy

Three material families, one gem:

1. **Obsidian** (backgrounds) — near-black with a cold green undertone, so the emeralds read as
   the same mineral world.
2. **Limestone** (interactive stone) — the existing `temple_sandstone` family, extended with
   crack/border shades.
3. **Verdigris / Seaweed** (panels, structure) — the existing dark-teal family, kept.
4. **Emerald** (the only light source) — a 4-step ramp from deep gem to hot core highlight.

### 3.2 Full color table — `res/values/colors.xml`

Keep every existing color (backwards compatibility with unchanged layouts), **add** the block
below. Names are final; use them verbatim.

```xml
<!-- ══════════ OBSIDIAN SERPENT — v1.4.0 additions ══════════ -->

<!-- Obsidian family (backgrounds) -->
<color name="obsidian_black">#07090B</color>        <!-- deepest base, replaces flat abyssal_teal as root bg -->
<color name="obsidian_sheen">#101816</color>        <!-- subtle facet highlight on the glass -->
<color name="obsidian_facet">#0C1210</color>        <!-- mid-tone facet fill -->
<color name="crack_void">#020403</color>            <!-- the crack line itself (darker than base) -->

<!-- Emerald glow ramp (dark → hot) -->
<color name="emerald_deep">#1D5C3F</color>          <!-- gem body, unlit -->
<color name="emerald_gem">#2E8B57</color>           <!-- = serpent_emerald; gem body, lit -->
<color name="emerald_bright">#50C878</color>        <!-- classic 'emerald' — glow mid -->
<color name="emerald_core">#A9F5C8</color>          <!-- hottest pixel at pulse peak -->
<color name="emerald_glow_faint">#332E8B57</color>  <!-- 20% serpent_emerald, ambient crack underglow -->

<!-- Limestone family (buttons) -->
<color name="limestone_body">#CEBFA3</color>        <!-- slightly dustier than temple_sandstone -->
<color name="limestone_highlight">#EFE6D2</color>   <!-- inner chisel line (light) -->
<color name="limestone_border">#5E523C</color>      <!-- outer blocky border (dark) -->
<color name="limestone_crack">#8A7A5E</color>       <!-- crack strokes on the slab face -->
<color name="limestone_shadow">#9F8F72</color>      <!-- bottom bevel (replaces sandstone_bevel use on new buttons) -->
<color name="limestone_pressed">#B3A588</color>     <!-- pressed body -->

<!-- Structure -->
<color name="pillar_stone_dark">#152521</color>     <!-- pillar shaft shadow side -->
<color name="pillar_stone">#1E332C</color>          <!-- pillar shaft lit side -->
<color name="pillar_capital">#42594F</color>        <!-- capital & base slabs -->
<color name="serpent_scale_dark">#1B4D35</color>    <!-- coiled serpent body -->
<color name="serpent_scale_light">#2E7D53</color>   <!-- serpent belly/highlight -->

<!-- Bronze accent (used sparingly: divider rivets, mason's marks) -->
<color name="aged_bronze">#8C7853</color>
```

### 3.3 Usage matrix

| Surface | Color(s) |
|---|---|
| Root background base | `obsidian_black` → vignette to `crack_void` |
| Crack lines | `crack_void` stroke + `emerald_glow_faint` under-stroke |
| Gems (idle → peak) | body `emerald_deep`→`emerald_gem`, glow `emerald_bright`, core `emerald_core` |
| Button body / pressed | `limestone_body` / `limestone_pressed` |
| Button border outer / inner | `limestone_border` (3dp) / `limestone_highlight` (1dp) |
| Button cracks | `limestone_crack` |
| Button text | `text_engraved` (existing `#071A15`) |
| Header lintel fill / border | `panel_dark_seaweed_bg` (existing) / double stroke `faded_olive_teal` + `emerald_deep` |
| Header title | `aged_marble` + emerald under-shadow (§4.4) |
| Pillars | `pillar_*` + `serpent_scale_*` + `emerald_gem` eyes |
| Captions / secondary | `tarnished_silver` (unchanged) |

---

## 4. Typography

### 4.1 The problem

`android:fontFamily="serif"` (Noto Serif) is thin and bookish — the opposite of "blocky, stocky,
impressive." We need a **display face that looks like Greek stone inscription**: heavy, square,
geometric, with the flavor of Λ, Σ, Ξ letterforms.

### 4.2 Font selection (bundled, offline — the primary approach)

Bundle TTFs under `app/src/main/res/font/`. All three below are **SIL Open Font License** (free
for commercial use, no attribution required in-app) and downloadable from Google Fonts
(fonts.google.com):

| Role | Font | File name (must be lowercase a–z, 0–9, `_`) | Why |
|---|---|---|---|
| **Display / Titles** (`ΞDGΞCΛSΞ`, screen titles) | **GFS Neohellenic Bold** | `gfs_neohellenic.ttf` | Greek Font Society humanist face — **covers Greek Ξ/Λ/Σ**, so the Greek-glyph wordmark (§4.3) renders in-face. Authentic Hellenic letterforms (chosen 2026-07-12 over blocky-but-Latin-only options like Caesar Dressing / Cinzel, which lack Greek). |
| **Buttons / sub-headers** | **Cinzel Black** (weight 900 of Cinzel) | `cinzel_black.ttf` | Roman-inscription capitals, monumental at 900 weight. Button/caption text is Latin-only, so Cinzel's lack of Greek is fine. |
| **Body / captions** | **Not adopted** — body text keeps `android:fontFamily="serif"` (A3, §7.5) | — | Body stays quiet/legible; only titles, buttons & captions get display faces. |

**Decision (2026-07-12, B2/B3): bundled fonts — sourced and committed.** Both TTFs are already in
`app/src/main/res/font/` and their OFL license texts are archived in `Docs/fonts_licenses/`:
- `gfs_neohellenic.ttf` — GFS Neohellenic **Bold** (Google Fonts / Greek Font Society, OFL). A
  static Bold that **covers Greek Ξ/Λ/Σ**, so it renders the `ΞDGΞCΛSΞ` title in-face (§4.3).
- `cinzel_black.ttf` — Cinzel **900 (Black)**, a static instance (Fontsource, OFL).

Both are already heavy static weights, so the styles reference `@font/…` directly — no
`fontVariationSettings` needed — and, being Bold already, they set `textStyle="normal"` to avoid
synthetic (faux) bolding (§4.4).

Referenced as `@font/gfs_neohellenic` / `@font/cinzel_black` (minSdk 30 → the `res/font` XML API is
fully supported, no support-lib fallback needed).

**Fallback option (not used — B2 chose bundling):** Downloadable Fonts via Google Play services
(`FontsContractCompat`, `res/font/*.xml` with `app:fontProviderAuthority="com.google.android.gms.fonts"`)
would avoid binary assets but adds a first-launch network fetch and a flash of fallback font —
rejected for an app whose whole identity is the title lettering.

### 4.3 The Greek-glyph title trick — **ADOPTED (B1)**

The main title ONLY uses genuine Greek codepoints that read as Latin:

```
EDGECASE  →  ΞDGΞCΛSΞ
```

- `E → Ξ` (Xi — three horizontal bars, extremely blocky)
- `A → Λ` (Lambda)

`android:contentDescription="EdgeCase"` is set on the title TextView (screen readers announce the
real name), and the launcher label stays plain "EdgeCase".

> **✅ Font-coverage — RESOLVED (2026-07-12).** The trick only looks right if the *display* face
> actually contains Ξ (U+039E) and Λ (U+039B); otherwise those glyphs fall back to a system font.
> Every blocky/inscriptional face surveyed (Caesar Dressing, Cinzel, Alfa Slab One, Zilla Slab, …)
> turned out **Latin-only**, so the title face is **GFS Neohellenic Bold** (`gfs_neohellenic.ttf`),
> verified via its cmap to cover Ξ/Λ/Σ. It is an authentic Greek humanist face rather than a blocky
> slab — the deliberate trade-off chosen for genuine Hellenic letterforms. The header default (§5.2)
> and main-menu row (§5.5) therefore ship the Greek-glyph string `ΞDGΞCΛSΞ` and render entirely
> in-face; the Latin `EDGECASE` fallback is no longer needed.

### 4.4 Text styles — replace/extend `res/values/styles.xml`

```xml
<!-- Monumental display: the temple-pediment inscription -->
<style name="TitleMonolith" parent="TextAppearance.AppCompat">
    <item name="android:fontFamily">@font/gfs_neohellenic</item>
    <item name="android:textAllCaps">true</item>
    <!-- gfs_neohellenic.ttf is already the Bold weight; avoid synthetic (faux) bold -->
    <item name="android:textStyle">normal</item>
    <item name="android:letterSpacing">0.18</item>
    <item name="android:textColor">@color/aged_marble</item>
    <!-- Emerald under-glow shadow: the inscription is lit by the gems below -->
    <item name="android:shadowColor">#802E8B57</item>
    <item name="android:shadowDx">0</item>
    <item name="android:shadowDy">3</item>
    <item name="android:shadowRadius">6</item>
</style>

<!-- Button lettering: Roman-inscription capitals at maximum weight -->
<style name="SlabButtonText" parent="TextAppearance.AppCompat">
    <item name="android:fontFamily">@font/cinzel_black</item>
    <item name="android:textAllCaps">true</item>
    <!-- cinzel_black.ttf is already weight 900; avoid synthetic (faux) bold -->
    <item name="android:textStyle">normal</item>
    <item name="android:letterSpacing">0.10</item>
    <item name="android:textColor">@color/text_engraved</item>
    <item name="android:textSize">@dimen/text_header</item>
    <!-- Engraved: light catch below the incision -->
    <item name="android:shadowColor">#66FFFFFF</item>
    <item name="android:shadowDx">0</item>
    <item name="android:shadowDy">1</item>
    <item name="android:shadowRadius">0.5</item>
</style>

<!-- Section captions ("CURRENT SHORTCUTS", "AVAILABLE APPS") -->
<style name="CaptionChiseled" parent="TextAppearance.AppCompat">
    <item name="android:fontFamily">@font/cinzel_black</item>
    <item name="android:textAllCaps">true</item>
    <item name="android:letterSpacing">0.14</item>
    <item name="android:textColor">@color/tarnished_silver</item>
    <item name="android:textSize">@dimen/text_caption</item>
</style>
```

Keep `EngravedHeader`, `BodySerif`, `CaptionSerif`, `StoneButtonText` in the file. **All three
screen layouts move to the new styles**, and — **Decision (2026-07-12, B11)** — the Customize
dialog's display/caption/button text migrates to the new styles too (formerly deferred to phase 2).
Only body text (`BodySerif`) stays serif (A3).

### 4.5 New dimensions — add to `res/values/dimens.xml`

```xml
<!-- ══ Obsidian Serpent v1.4.0 ══ -->
<dimen name="header_title_size">26sp</dimen>       <!-- main menu "EDGECASE" -->
<dimen name="header_title_size_sub">20sp</dimen>   <!-- sub-screen titles -->
<dimen name="header_height">64dp</dimen>
<dimen name="border_blocky_outer">3dp</dimen>
<dimen name="border_blocky_inner">1dp</dimen>
<dimen name="slab_crack_stroke">1.25dp</dimen>
<dimen name="pillar_width_new">40dp</dimen>        <!-- pillars get wider & prouder -->
<dimen name="meander_trim_height">10dp</dimen>
```

---

## 5. The Temple Lintel — unified header

### 5.1 Concept

The header is a **lintel**: the massive horizontal stone beam resting across two columns above a
temple doorway. Every screen has the *same* lintel; only the inscription changes. Centered text,
stepped (ziggurat-profile) border, thin meander trim underneath.

```
   ╔═══════════════════════════════════╗   ← 3dp dark outer stroke (stepped corners)
   ║ ┌───────────────────────────────┐ ║   ← 1dp emerald-deep inner line
   ║ │                               │ ║
   ║ │      E D G E C A S E         │ ║   ← TitleMonolith, centered, 26sp
   ║ │                               │ ║
   ║ └───────────────────────────────┘ ║
   ╚═══════════════════════════════════╝
     ▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄▀▄     ← 10dp meander trim strip (ic_meander_border, rotated 90°? No —
                                            use a horizontal meander, §5.4)
```

### 5.2 New reusable layout — `res/layout/layout_temple_header.xml`

One file, included by all three screens. The title is set per-screen (see §5.5).

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="@dimen/header_height"
        android:background="@drawable/bg_temple_lintel">

        <TextView
            android:id="@+id/tvTempleTitle"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="ΞDGΞCΛSΞ"
            android:contentDescription="EdgeCase"
            android:textAppearance="@style/TitleMonolith"
            android:textSize="@dimen/header_title_size" />
    </FrameLayout>

    <!-- Meander trim under the lintel -->
    <ImageView
        android:layout_width="match_parent"
        android:layout_height="@dimen/meander_trim_height"
        android:src="@drawable/ic_meander_horizontal"
        android:scaleType="fitXY"
        android:alpha="0.55"
        android:importantForAccessibility="no" />
</LinearLayout>
```

> **NOTE — include + findViewById collision:** the same `@+id/tvTempleTitle` will exist three
> times inside `activity_main.xml` (once per included screen). MainActivity must resolve it
> scoped: `screenShortcuts.findViewById<TextView>(R.id.tvTempleTitle).text = "SHORTCUTS"`,
> etc. — never `findViewById` on the Activity directly for this ID. Alternatively set
> per-screen titles right in each screen layout by replacing the include with a copy — but the
> include + scoped-lookup approach keeps L2 airtight and is the **recommended** one.

### 5.3 Lintel background — `res/drawable/bg_temple_lintel.xml`

Blocky double border, zero radius, with a faint vertical gradient so the slab reads as lit from
below (by the emeralds):

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Outer blocky border -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/faded_olive_teal" />
        </shape>
    </item>
    <!-- Inner emerald hairline (inset by the outer border width) -->
    <item android:left="3dp" android:top="3dp" android:right="3dp" android:bottom="3dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/emerald_deep" />
        </shape>
    </item>
    <!-- Slab face -->
    <item android:left="4dp" android:top="4dp" android:right="4dp" android:bottom="4dp">
        <shape android:shape="rectangle">
            <gradient
                android:angle="90"
                android:startColor="#C0122A23"
                android:endColor="#E607140F" />
        </shape>
    </item>
    <!-- Stepped 'ziggurat' corner notches: four small dark squares at the corners
         make the rectangle read as cut stone rather than a CSS box -->
    <item android:gravity="top|start" android:width="8dp" android:height="8dp">
        <shape android:shape="rectangle"><solid android:color="@color/obsidian_black" /></shape>
    </item>
    <item android:gravity="top|end" android:width="8dp" android:height="8dp">
        <shape android:shape="rectangle"><solid android:color="@color/obsidian_black" /></shape>
    </item>
    <item android:gravity="bottom|start" android:width="8dp" android:height="8dp">
        <shape android:shape="rectangle"><solid android:color="@color/obsidian_black" /></shape>
    </item>
    <item android:gravity="bottom|end" android:width="8dp" android:height="8dp">
        <shape android:shape="rectangle"><solid android:color="@color/obsidian_black" /></shape>
    </item>
</layer-list>
```

(The corner notches literally carve the corners *off* — the anti-rounded-corner statement.)

### 5.4 Horizontal meander — `res/drawable/ic_meander_horizontal.xml`

The existing `ic_meander_border.xml` is a 32×1280 **vertical** strip. Create the horizontal
twin (1280×32 viewport, same key pattern rotated), fill `tarnished_silver`. Simplest correct
implementation: copy the vertical file and swap every `x,y` coordinate pair in the pathData
(x′=y, y′=x). It repeats every 64px, so 20 repetitions fill the 1280 viewport.

### 5.5 Per-screen wiring

| Screen | Current header (to delete) | New title text |
|---|---|---|
| `layout_screen_main_menu.xml` | lines 44–63 (LinearLayout + left-aligned 18sp TextView) | `ΞDGΞCΛSΞ` (§4.3; Latin `EDGECASE` fallback if no Greek-capable font) @ 26sp |
| `layout_screen_shortcuts_container.xml` | lines 35–61 — **includes the 20dp `ic_silver_ring` ImageView → DELETE the ring** (brief item 1) | `SHORTCUTS` @ 20sp |
| `layout_screen_positioning_container.xml` | lines 38–57 | `SLIVER POSITION` @ 20sp |

Each screen replaces its header block with:

```xml
<include
    android:id="@+id/templeHeader"
    layout="@layout/layout_temple_header"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp" />
```

MainActivity `onCreate()` addition (after `setContentView`):

```kotlin
screenShortcuts.findViewById<TextView>(R.id.tvTempleTitle)?.apply {
    text = "SHORTCUTS"
    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.header_title_size_sub))
}
screenPositioning.findViewById<TextView>(R.id.tvTempleTitle)?.apply {
    text = "SLIVER POSITION"
    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.header_title_size_sub))
}
// Main-menu copy keeps the default "ΞDGΞCΛSΞ" @ header_title_size — no code needed.
```

---

## 6. The Living Background

### 6.1 Concept

A static drawable cannot pulse, so the background becomes a **custom View**:
`ObsidianCrackView` — layered *behind* each screen's content. It renders:

1. **Obsidian base** — near-black with 3–4 huge, faint polygonal "facets" (obsidian is
   conchoidal volcanic glass; giant low-contrast triangles sell that instantly).
2. **Cracks** — 6–8 procedurally generated jagged polylines crossing the screen, drawn as a
   dark `crack_void` stroke with a wider, faint `emerald_glow_faint` under-stroke (light leaking
   through the fracture).
3. **Emerald gems** — 8–14 small emerald-cut (elongated octagon) gems seated *on the crack
   lines*, each with a radial-gradient glow halo that **pulses on its own phase and period**
   (2.4–4.8s), like embers breathing.

Layers 1–2 are rendered **once** into an offscreen `Bitmap` (cheap). Only layer 3 animates.

### 6.2 Architecture

```
ObsidianCrackView : View
├── onSizeChanged(w,h)     → regenerate(seed)         [builds staticLayer Bitmap + gem list]
├── regenerate()
│     ├── drawBase(canvas)          facets + vignette + speckle noise
│     ├── generateCracks(random)    List<Path> via jagged random-walks from edge seeds
│     ├── drawCracks(canvas)        under-glow stroke, then void stroke
│     └── placeGems(random)         pick vertices on cracks → List<Gem>
├── Gem(x, y, sizePx, phase, periodMs, angleDeg)
├── pulseAnimator : ValueAnimator   INFINITE, updates `now`, invalidate()
├── onDraw(canvas)
│     ├── drawBitmap(staticLayer)
│     └── for gem in gems: drawGlow(RadialGradient) + drawGemPath(facets)
├── onAttachedToWindow / onDetachedFromWindow / onVisibilityChanged → start/stop animator
└── XML-safe: (context, attrs) constructor so it can be declared in layouts
```

### 6.3 Full source — `app/src/main/java/com/dicereligion/edgecase/ObsidianCrackView.kt`

```kotlin
package com.dicereligion.edgecase

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/**
 * The living temple floor: fractured obsidian with emerald gems pulsing inside the cracks.
 *
 * Static layers (obsidian facets, vignette, speckles, crack lines) are rasterised ONCE into
 * [staticLayer] on size change. Only the gem glow pulses per-frame — a handful of
 * hardware-accelerated radial-gradient circles + tiny paths. See Docs/NewTheme.md §6.
 */
class ObsidianCrackView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    // ── Tunables (Docs/NewTheme.md §6.5) ────────────────────────────
    var crackCount = 7
    var gemCount = 11
    var seed = 20260711L          // fixed seed → identical temple on every launch
    var maxGlowAlpha = 0.55f      // keep ambient so foreground text stays readable

    // ── Static layer ────────────────────────────────────────────────
    private var staticLayer: Bitmap? = null
    private val crackPaths = mutableListOf<Path>()

    // ── Gems ────────────────────────────────────────────────────────
    private class Gem(
        val x: Float, val y: Float, val size: Float,
        val phase: Float, val periodMs: Float, val angleDeg: Float
    )
    private val gems = mutableListOf<Gem>()
    private val gemPath = Path()
    private val gemMatrix = Matrix()

    // ── Paints ──────────────────────────────────────────────────────
    private val basePaint = Paint()
    private val facetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val crackGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#332E8B57")   // emerald_glow_faint
        strokeWidth = 7f
        strokeJoin = Paint.Join.MITER            // sharp — never round (Design Law L1)
    }
    private val crackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#020403")      // crack_void
        strokeWidth = 2.5f
        strokeJoin = Paint.Join.MITER
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gemBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gemFacetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.parseColor("#66A9F5C8")    // emerald_core @ 40%
    }

    // ── Animation clock ─────────────────────────────────────────────
    private var animator: ValueAnimator? = null
    private var nowMs = 0f

    // ── Lifecycle ───────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) regenerate(w, h)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); startPulse() }
    override fun onDetachedFromWindow() { stopPulse(); super.onDetachedFromWindow() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) startPulse() else stopPulse()
    }

    private fun startPulse() {
        if (animator != null || !isAttachedToWindow) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10_000L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                nowMs = (nowMs + 16f)   // monotonic-enough clock for sine phases
                invalidate()
            }
            start()
        }
    }

    private fun stopPulse() { animator?.cancel(); animator = null }

    // ── Generation (runs once per size) ─────────────────────────────

    private fun regenerate(w: Int, h: Int) {
        val rnd = Random(seed)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        drawBase(c, w, h, rnd)
        crackPaths.clear()
        repeat(crackCount) { crackPaths.add(generateCrack(w, h, rnd)) }
        for (p in crackPaths) { c.drawPath(p, crackGlowPaint); c.drawPath(p, crackPaint) }

        placeGems(w, h, rnd)
        staticLayer?.recycle()
        staticLayer = bmp
    }

    private fun drawBase(c: Canvas, w: Int, h: Int, rnd: Random) {
        // Obsidian body
        basePaint.color = Color.parseColor("#07090B")   // obsidian_black
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        // 4 giant conchoidal facets — huge dim triangles
        val facetColors = intArrayOf(
            Color.parseColor("#0C1210"),  // obsidian_facet
            Color.parseColor("#101816"),  // obsidian_sheen
            Color.parseColor("#0A0E0C"),
            Color.parseColor("#0D1412")
        )
        repeat(4) { i ->
            facetPaint.color = facetColors[i]
            val p = Path()
            p.moveTo(rnd.nextFloat() * w, rnd.nextFloat() * h)
            p.lineTo(rnd.nextFloat() * w, rnd.nextFloat() * h)
            p.lineTo(rnd.nextFloat() * w, rnd.nextFloat() * h)
            p.close()
            c.drawPath(p, facetPaint)
        }

        // Mineral speckle: 90 one-px flecks
        facetPaint.color = Color.parseColor("#14FFFFFF")
        repeat(90) {
            c.drawRect(
                rnd.nextFloat() * w, rnd.nextFloat() * h,
                rnd.nextFloat() * w % w + 1.5f + rnd.nextFloat() * w * 0f + 0f,
                0f, facetPaint
            ).let { }
        }
        // (simpler + correct speckle:)
        repeat(90) {
            val sx = rnd.nextFloat() * w; val sy = rnd.nextFloat() * h
            c.drawRect(sx, sy, sx + 1.5f, sy + 1.5f, facetPaint)
        }

        // Vignette — darkness pooling at the edges
        val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2f, h / 2f, (w.coerceAtLeast(h)) * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#66020403")),
                floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vignette)
    }

    /** Jagged random-walk from a random edge point across the screen, with one optional branch. */
    private fun generateCrack(w: Int, h: Int, rnd: Random): Path {
        val path = Path()
        // Seed on a random edge
        var x: Float; var y: Float; var dirX: Float; var dirY: Float
        when (rnd.nextInt(4)) {
            0 -> { x = rnd.nextFloat() * w; y = 0f;            dirX = rnd.nextFloat() - 0.5f; dirY = 1f }
            1 -> { x = rnd.nextFloat() * w; y = h.toFloat();   dirX = rnd.nextFloat() - 0.5f; dirY = -1f }
            2 -> { x = 0f; y = rnd.nextFloat() * h;            dirX = 1f; dirY = rnd.nextFloat() - 0.5f }
            else -> { x = w.toFloat(); y = rnd.nextFloat() * h; dirX = -1f; dirY = rnd.nextFloat() - 0.5f }
        }
        path.moveTo(x, y)
        val steps = 9 + rnd.nextInt(8)
        val stepLen = (w + h) / 2f / steps
        repeat(steps) {
            // advance with jitter — sharp kinks, never smooth curves
            x += dirX * stepLen * (0.6f + rnd.nextFloat() * 0.8f) + (rnd.nextFloat() - 0.5f) * stepLen * 0.9f
            y += dirY * stepLen * (0.6f + rnd.nextFloat() * 0.8f) + (rnd.nextFloat() - 0.5f) * stepLen * 0.9f
            path.lineTo(x, y)
            // 25% chance: a short branch splinter
            if (rnd.nextFloat() < 0.25f) {
                val bx = x + (rnd.nextFloat() - 0.5f) * stepLen * 1.6f
                val by = y + (rnd.nextFloat() - 0.5f) * stepLen * 1.6f
                path.moveTo(x, y); path.lineTo(bx, by); path.moveTo(x, y)
            }
        }
        return path
    }

    private fun placeGems(w: Int, h: Int, rnd: Random) {
        gems.clear()
        val density = resources.displayMetrics.density
        repeat(gemCount) {
            // Sample a point along a random crack via PathMeasure
            val path = crackPaths[rnd.nextInt(crackPaths.size)]
            val pm = android.graphics.PathMeasure(path, false)
            val pos = FloatArray(2)
            pm.getPosTan(pm.length * rnd.nextFloat(), pos, null)
            // Keep gems on-screen with margin
            val gx = pos[0].coerceIn(w * 0.06f, w * 0.94f)
            val gy = pos[1].coerceIn(h * 0.06f, h * 0.94f)
            gems.add(
                Gem(
                    x = gx, y = gy,
                    size = (4f + rnd.nextFloat() * 5f) * density,
                    phase = rnd.nextFloat(),
                    periodMs = 2400f + rnd.nextFloat() * 2400f,
                    angleDeg = rnd.nextFloat() * 180f
                )
            )
        }
    }

    // ── Per-frame drawing ───────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        staticLayer?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        for (g in gems) {
            // pulse ∈ [0,1], sinusoidal, per-gem phase & period
            val pulse = 0.5f + 0.5f * sin((nowMs / g.periodMs + g.phase) * 2f * Math.PI.toFloat())
            val glowAlpha = (pulse * maxGlowAlpha * 255).toInt()
            val glowRadius = g.size * (2.6f + 1.8f * pulse)

            // Halo (RadialGradient — fully hardware accelerated; never BlurMaskFilter)
            glowPaint.shader = RadialGradient(
                g.x, g.y, glowRadius,
                intArrayOf(
                    Color.argb(glowAlpha, 0x50, 0xC8, 0x78),          // emerald_bright core
                    Color.argb((glowAlpha * 0.4f).toInt(), 0x2E, 0x8B, 0x57),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
            )
            canvas.drawCircle(g.x, g.y, glowRadius, glowPaint)

            // Emerald-cut gem body: elongated octagon
            buildGemPath(g)
            gemBodyPaint.color = lerpColor(pulse,
                Color.parseColor("#1D5C3F"),   // emerald_deep (trough)
                Color.parseColor("#2E8B57"))   // emerald_gem (peak)
            canvas.drawPath(gemPath, gemBodyPaint)
            canvas.drawPath(gemPath, gemFacetPaint)

            // Hot core pixel at peak
            if (pulse > 0.75f) {
                gemBodyPaint.color = Color.argb(
                    ((pulse - 0.75f) / 0.25f * 255).toInt(), 0xA9, 0xF5, 0xC8)  // emerald_core
                canvas.drawCircle(g.x, g.y, g.size * 0.22f, gemBodyPaint)
            }
        }
    }

    /** Elongated octagon = classic emerald cut, rotated by the gem's resting angle. */
    private fun buildGemPath(g: Gem) {
        val w = g.size; val h = g.size * 1.5f; val c = 0.30f  // corner cut fraction
        gemPath.reset()
        gemPath.moveTo(-w / 2 + w * c, -h / 2)
        gemPath.lineTo(w / 2 - w * c, -h / 2)
        gemPath.lineTo(w / 2, -h / 2 + h * c)
        gemPath.lineTo(w / 2, h / 2 - h * c)
        gemPath.lineTo(w / 2 - w * c, h / 2)
        gemPath.lineTo(-w / 2 + w * c, h / 2)
        gemPath.lineTo(-w / 2, h / 2 - h * c)
        gemPath.lineTo(-w / 2, -h / 2 + h * c)
        gemPath.close()
        gemMatrix.reset()
        gemMatrix.postRotate(g.angleDeg)
        gemMatrix.postTranslate(g.x, g.y)
        gemPath.transform(gemMatrix)
    }

    private fun lerpColor(t: Float, from: Int, to: Int): Int = Color.rgb(
        (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt(),
        (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt(),
        (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
    )
}
```

> Implementation note: the first `repeat(90)` speckle block in `drawBase` contains a
> placeholder mis-draw kept from drafting — **delete it and keep only the second (correct)
> speckle loop.** Flagging it here so it cannot slip through review.

### 6.4 Wiring the background into the screens

The three screen layouts currently set `android:background="@drawable/bg_stone_texture"` on
their root `FrameLayout`. Change per screen:

1. Root `FrameLayout`: **remove** the `android:background` attribute (or set
   `@color/obsidian_black` as a safety base).
2. Insert as the **first child** (bottom of the z-order, *under* the pillars):

```xml
<com.dicereligion.edgecase.ObsidianCrackView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:importantForAccessibility="no" />
```

3. Also set `activity_main.xml`'s root background to `@color/obsidian_black` (replacing
   `abyssal_teal`) so screen transitions never flash teal.

After this change no screen references `bg_stone_texture.xml`; once the dialog background also moves
to `bg_temple_panel` in Phase 5 (B11, §10.2) it becomes fully unreferenced and can be deleted.

### 6.5 Tuning table

| Property | Default | Range | Effect |
|---|---|---|---|
| `crackCount` | 7 | 4–10 | fracture density |
| `gemCount` | 11 | 6–16 | number of pulsing emeralds |
| `seed` | **fixed (locked, B4)** | any Long | **Decision (2026-07-12): keep the fixed seed** — same temple every launch, the room feels *real*. (`System.nanoTime()` for a new cave each launch is not used.) |
| `maxGlowAlpha` | **0.55 (locked, B5)** | 0.3–0.7 | **Decision (2026-07-12): 0.55 ships.** Re-tune on-device only if a test device shows text-contrast problems over a peak gem. |
| gem `periodMs` | 2400–4800 | — | breathing tempo; never below 1500 (distracting) |

### 6.6 Battery discipline

- Animator runs **only** while attached + `VISIBLE` (three overlapping screens → the two `GONE`
  screens' background views cost nothing: `onVisibilityChanged` stops their animators).
- One `invalidate()` per frame; static bitmap blit + ≤16 gradient circles ≈ trivially cheap.
- No `BlurMaskFilter` (software-path trap), no per-frame allocation except the RadialGradient
  shader (acceptable at ≤16/frame; optimization option: pre-bake 32 glow bitmaps at load).

---

## 7. Stone Slab Buttons

### 7.1 Concept

Each button is a **quarried limestone block**: matte body, hairline cracks across the face,
blocky double border (dark outer + light inner chisel line), and the existing bottom bevel for
weight. The existing press behavior (`applyStoneButtonBehavior`: 4dp sink + haptic + dust) is
kept untouched — it already feels perfect for a stone slab.

```
  ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓ ← 3dp limestone_border (dark outer)
  ┃▛▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▜┃ ← 1dp limestone_highlight (light inner)
  ┃▌      ╱          ╲              ▐┃
  ┃▌     ╱  S H O R T C U T S       ▐┃ ← cracks (vector overlay) + Cinzel Black text
  ┃▌  ──╱               ╲__         ▐┃
  ┃▙▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▟┃
  ┃█████████ bottom bevel ██████████┃ ← 4dp limestone_shadow strip
  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

### 7.2 Crack texture — `res/drawable/ic_texture_cracks.xml`

A vector of thin jagged strokes, tileable-ish across a 360×56 viewport (matches the widest
button at 56dp height; `fitXY` scaling on other sizes is fine — cracks stretch imperceptibly).

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="360dp"
    android:height="56dp"
    android:viewportWidth="360"
    android:viewportHeight="56">

    <!-- Primary crack: enters top-left third, exits right edge -->
    <path
        android:strokeColor="#668A7A5E"
        android:strokeWidth="1.2"
        android:fillColor="#00000000"
        android:pathData="M74,0 L79,9 L71,15 L83,26 L78,34 L92,41 L88,50 L96,56" />
    <!-- Splinter off the primary -->
    <path
        android:strokeColor="#4D8A7A5E"
        android:strokeWidth="0.8"
        android:fillColor="#00000000"
        android:pathData="M83,26 L97,22 L108,25" />
    <!-- Secondary crack: right side, shallower -->
    <path
        android:strokeColor="#598A7A5E"
        android:strokeWidth="1"
        android:fillColor="#00000000"
        android:pathData="M266,56 L262,47 L271,40 L263,31 L272,20 L268,12 L277,4 L274,0" />
    <!-- Hairline crossing crack, low alpha -->
    <path
        android:strokeColor="#338A7A5E"
        android:strokeWidth="0.7"
        android:fillColor="#00000000"
        android:pathData="M0,38 L28,34 L44,39 L69,33 M170,8 L188,14 L204,10 L221,16" />
    <!-- Chips: tiny triangular losses at edges -->
    <path
        android:fillColor="#409F8F72"
        android:pathData="M0,0 L10,0 L0,7 Z M360,56 L348,56 L360,49 Z M198,56 L206,56 L202,50 Z" />
    <!-- Weathering pocks -->
    <path
        android:fillColor="#269F8F72"
        android:pathData="M140,20 l2.5,0 0,2.5 -2.5,0 Z M52,44 l2,0 0,2 -2,0 Z
                          M310,15 l2.5,0 0,2.5 -2.5,0 Z M240,38 l2,0 0,2 -2,0 Z" />
</vector>
```

### 7.3 Slab background — REWRITE `res/drawable/bg_stone_button.xml`

The double-border pattern (Design Law L3): outer solid → inset lighter solid → inset body.

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 1. Outer blocky border -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/limestone_border" />
        </shape>
    </item>
    <!-- 2. Inner light chisel line -->
    <item android:left="3dp" android:top="3dp" android:right="3dp" android:bottom="3dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/limestone_highlight" />
        </shape>
    </item>
    <!-- 3. Limestone body -->
    <item android:left="4dp" android:top="4dp" android:right="4dp" android:bottom="4dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/limestone_body" />
        </shape>
    </item>
    <!-- 4. Bottom bevel: darker strip, under-lit stone weight -->
    <item android:left="4dp" android:top="48dp" android:right="4dp" android:bottom="4dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/limestone_shadow" />
        </shape>
    </item>
    <!-- 5. Crack texture across the face -->
    <item android:left="4dp" android:top="4dp" android:right="4dp" android:bottom="4dp"
        android:drawable="@drawable/ic_texture_cracks" />
</layer-list>
```

> The `android:top="48dp"` bevel offset assumes the standard 56dp button. It degrades gracefully
> on taller buttons (bevel just sits 48dp down). **Decision (2026-07-12, B6): keep this 48dp-inset
> bevel (the recommended approach); the 90° `<gradient>` hard-stop alternative is not used.**

### 7.4 Pressed state — REWRITE `res/drawable/bg_stone_button_pressed.xml`

Pressed = the slab sinks (translation already animated in code) *and* the light leaves it:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/limestone_border" />
        </shape>
    </item>
    <!-- inner line goes DARK when pressed (light source blocked by your finger) -->
    <item android:left="3dp" android:top="3dp" android:right="3dp" android:bottom="3dp">
        <shape android:shape="rectangle">
            <solid android:color="#7A6C50" />
        </shape>
    </item>
    <item android:left="4dp" android:top="4dp" android:right="4dp" android:bottom="4dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/limestone_pressed" />
        </shape>
    </item>
    <item android:left="4dp" android:top="4dp" android:right="4dp" android:bottom="4dp"
        android:drawable="@drawable/ic_texture_cracks" />
</layer-list>
```

`selector_stone_button.xml` needs **no change** — it already routes pressed/default to these two
files.

### 7.5 Button XML deltas (all buttons, all screens + dialog)

Per `<Button>`: change `android:textAppearance="@style/StoneButtonText"` →
`"@style/SlabButtonText"` and **delete** the redundant `android:fontFamily="serif"` line
(it currently overrides the style's font — with the new bundled font that override would break
everything; this is the single most important layout edit not to miss).
Applies to: `btnShortcuts`, `btnPosition`, `btnDummy`, `btnStartService`, `btnStopService`,
`btnBackToMenu`, `btnSaveShortcuts`, `btnCustomizeSliver`, `btnBackToMenuFromPosition`, and the
dialog's `btnResetSliver`, `btnCancelSliver`, `btnApplySliver`.

> **Decision (2026-07-12, A2): the `fontFamily="serif"` deletion is scoped to `<Button>` elements
> ONLY.** Body `TextView`s carry the same attribute but must **keep** it — do **not** blanket
> grep-and-delete. Specifically leave `serif` in place on `tvAltarName`
> (`layout_item_shortcut_tile.xml`), `tvArchiveName` (`layout_item_available_app.xml`), and any
> other `BodySerif`/`CaptionSerif` body text. Those rows stay serif by design (§4.4, A3); only the
> title/button/caption *display* text migrates to the bundled faces.

---

## 8. Serpentine Pillars

### 8.1 Concept

The current pillars are flat rectangles with four scratch lines. The new ones are **monumental
serpent columns** (cf. the Serpent Column of Delphi): a proper Doric composition — stepped
crepidoma base, entasis-suggesting shaft with alternating-shade flutes, triple-slab capital —
with a **serpent coiled five times around the shaft**, passing in front of and behind it, its
head resting on the capital with a pulsing-look emerald eye.

Also: pillars get **wider** (32dp → 40dp, `pillar_width_new`) and **more present**
(alpha 0.5 → 0.85) — they are architecture now, not wallpaper.

Vector z-order trick for the coil: draw *behind-shaft* serpent arcs first, then the shaft +
flutes over them, then *front-of-shaft* arcs on top. The serpent appears to wrap.

### 8.2 Full vector — `res/drawable/ic_pillar_serpent_left.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="40dp"
    android:height="640dp"
    android:viewportWidth="80"
    android:viewportHeight="1280">

    <!-- ══ CAPITAL: abacus + echinus + necking (3 stepped slabs) ══ -->
    <path android:fillColor="#42594F"
        android:pathData="M4,28 L76,28 L76,44 L4,44 Z" />                 <!-- abacus (widest) -->
    <path android:fillColor="#3B5249"
        android:pathData="M10,44 L70,44 L66,62 L14,62 Z" />               <!-- echinus (tapered) -->
    <path android:fillColor="#2A3E34"
        android:pathData="M16,62 L64,62 L64,72 L16,72 Z" />               <!-- necking band -->
    <path android:fillColor="#1B4D35"
        android:pathData="M16,74 L64,74 L64,78 L16,78 Z" />               <!-- annulet ring, serpent-green -->

    <!-- ══ SERPENT — BEHIND-SHAFT ARCS (drawn first, shaft covers middles) ══ -->
    <path android:fillColor="#123524"
        android:pathData="M18,300 Q40,340 62,300 L62,330 Q40,372 18,330 Z" />
    <path android:fillColor="#123524"
        android:pathData="M18,620 Q40,660 62,620 L62,650 Q40,692 18,650 Z" />
    <path android:fillColor="#123524"
        android:pathData="M18,940 Q40,980 62,940 L62,970 Q40,1012 18,970 Z" />

    <!-- ══ SHAFT with entasis suggestion (slightly narrower at top) ══ -->
    <path android:fillColor="#1E332C"
        android:pathData="M20,78 L60,78 L62,1180 L18,1180 Z" />
    <!-- shadow side (left pillar: shadow on left half) -->
    <path android:fillColor="#152521"
        android:pathData="M20,78 L40,78 L40,1180 L18,1180 Z" />
    <!-- Flutes: 5 grooves, alternating dark groove + thin light arris -->
    <path android:fillColor="#0F221B"
        android:pathData="M26,80 L29,80 L28,1178 L25,1178 Z
                          M34,80 L37,80 L36,1178 L33,1178 Z
                          M42,80 L45,80 L44,1178 L41,1178 Z
                          M50,80 L53,80 L52,1178 L49,1178 Z" />
    <path android:fillColor="#2E4A3F"
        android:pathData="M30,80 L31.5,80 L30.5,1178 L29,1178 Z
                          M38,80 L39.5,80 L38.5,1178 L37,1178 Z
                          M46,80 L47.5,80 L46.5,1178 L45,1178 Z" />

    <!-- ══ SERPENT — FRONT-OF-SHAFT COILS (over the shaft) ══ -->
    <!-- Each coil: a thick S-band crossing the shaft diagonally downward -->
    <path android:fillColor="#1B4D35"
        android:pathData="M62,150 Q40,190 18,150 L18,182 Q40,226 62,182 Z" />
    <path android:fillColor="#1B4D35"
        android:pathData="M62,470 Q40,510 18,470 L18,502 Q40,546 62,502 Z" />
    <path android:fillColor="#1B4D35"
        android:pathData="M62,790 Q40,830 18,790 L18,822 Q40,866 62,822 Z" />
    <path android:fillColor="#1B4D35"
        android:pathData="M62,1090 Q40,1130 18,1090 L18,1122 Q40,1166 62,1122 Z" />
    <!-- Belly highlight stripe on every front coil -->
    <path android:fillColor="#2E7D53"
        android:pathData="M62,158 Q40,198 18,158 L18,166 Q40,208 62,166 Z
                          M62,478 Q40,518 18,478 L18,486 Q40,528 62,486 Z
                          M62,798 Q40,838 18,798 L18,806 Q40,848 62,806 Z
                          M62,1098 Q40,1138 18,1098 L18,1106 Q40,1148 62,1106 Z" />
    <!-- Scale ticks: short chevrons along the first coil (repeat pattern per coil if desired) -->
    <path android:strokeColor="#33071A15" android:strokeWidth="2" android:fillColor="#00000000"
        android:pathData="M26,168 l6,6 M36,174 l6,6 M46,172 l6,6
                          M26,488 l6,6 M36,494 l6,6 M46,492 l6,6
                          M26,808 l6,6 M36,814 l6,6 M46,812 l6,6
                          M26,1108 l6,6 M36,1114 l6,6 M46,1112 l6,6" />

    <!-- ══ SERPENT HEAD resting on the capital ══ -->
    <!-- Neck rises from first coil up the right side to the echinus -->
    <path android:fillColor="#1B4D35"
        android:pathData="M56,150 Q72,120 64,92 Q60,78 48,72 L42,60 Q52,52 64,58 Q76,66 74,84 Q78,116 62,152 Z" />
    <!-- Head: angular wedge (blocky, per Design Law) -->
    <path android:fillColor="#2E7D53"
        android:pathData="M42,60 L64,52 L70,40 L54,34 L38,42 L36,54 Z" />
    <!-- Brow ridge -->
    <path android:fillColor="#1B4D35"
        android:pathData="M42,44 L60,38 L62,42 L44,48 Z" />
    <!-- EMERALD EYE -->
    <path android:fillColor="#2E8B57"
        android:pathData="M50,44 l6,0 0,6 -6,0 Z" />
    <path android:fillColor="#A9F5C8"
        android:pathData="M52,46 l2,0 0,2 -2,0 Z" />                       <!-- hot core pixel -->
    <!-- Forked tongue flicking toward screen center -->
    <path android:strokeColor="#2E8B57" android:strokeWidth="2" android:fillColor="#00000000"
        android:pathData="M70,40 l8,-3 M78,37 l4,-3 M78,37 l5,1" />

    <!-- ══ CREPIDOMA (3-step base) ══ -->
    <path android:fillColor="#2A3E34"
        android:pathData="M14,1180 L66,1180 L66,1204 L14,1204 Z" />
    <path android:fillColor="#3B5249"
        android:pathData="M8,1204 L72,1204 L72,1230 L8,1230 Z" />
    <path android:fillColor="#42594F"
        android:pathData="M2,1230 L78,1230 L78,1258 L2,1258 Z" />
    <!-- step shadows -->
    <path android:fillColor="#152521"
        android:pathData="M14,1180 L40,1180 L40,1204 L14,1204 Z
                          M8,1204 L40,1204 L40,1230 L8,1230 Z
                          M2,1230 L40,1230 L40,1258 L2,1258 Z" />
    <!-- Serpent tail-tip curling out from behind the base -->
    <path android:strokeColor="#1B4D35" android:strokeWidth="6" android:fillColor="#00000000"
        android:pathData="M60,1180 Q74,1196 66,1214 Q60,1226 70,1236" />
</vector>
```

### 8.3 Right pillar — `ic_pillar_serpent_right.xml`

Do **not** hand-mirror 40+ paths. Mirror in the layout instead:

```xml
<!-- Right pillar = left pillar flipped horizontally -->
<ImageView
    android:layout_width="@dimen/pillar_width_new"
    android:layout_height="match_parent"
    android:layout_gravity="end|center_vertical"
    android:src="@drawable/ic_pillar_serpent_left"
    android:scaleX="-1"
    android:scaleType="fitXY"
    android:alpha="0.85"
    android:contentDescription="Right pillar" />
```

(`scaleX="-1"` gives a true mirror: serpent heads face inward from both sides, framing the
content — deliberate and symmetrical, like temple guardians. Delete `ic_pillar_right.xml`
usage; the old files may be removed once no layout references them.)

### 8.4 Layout deltas (all three screens)

- `@drawable/ic_pillar_left` → `@drawable/ic_pillar_serpent_left` (both sides, right one with
  `scaleX="-1"` as above).
- `@dimen/pillar_width` → `@dimen/pillar_width_new`.
- `android:alpha="0.5"` → `android:alpha="0.85"`.
- Content padding on each screen's main LinearLayout: main menu keeps 56dp; shortcuts/positioning
  raise `paddingLeft/Right` 48dp → 52dp so content clears the wider pillars.

---

## 9. De-rounding the rest of the app

The complete rounded-corner elimination audit (Design Law L1):

| Location | Current | Change |
|---|---|---|
| `bg_dark_seaweed_panel.xml` | `<corners android:radius="4dp"/>` | **Delete the corners tag**; upgrade the single 2dp stroke to the double-border pattern (§10.1) |
| `PositioningView.kt:158` | `mockupCornerRadius = mockupW * 0.04f` | `mockupCornerRadius = 0f` — and see §11.4: reframe the mockup as a **marble stele** (a stone tablet), which squareness actually improves |
| `PositioningView.kt` field default `mockupCornerRadius = 40f` | 40f | `0f` |
| AppCompat `AlertDialog` (Customize dialog + Discard dialog) | System rounded window bg | `SliverCustomizeDialog.build()`: after `dialog.show()`, add `dialog.window?.setBackgroundDrawableResource(R.drawable.bg_temple_panel)`. Discard dialog in `MainActivity.showDiscardDialog()`: same via `.create()` then `window.setBackgroundDrawableResource(...)` before `show()` |
| `bg_stone_button*.xml` | already `radius="0dp"` | rewritten anyway (§7) — keep zero |
| `ic_silver_ring.xml` (oval) | header, `layout_item_shortcut_tile.xml` icon frame, **and `dialog_customize_sliver.xml:107` `colorSwatch` background** | **Decision (A1): remove the ring in all three.** Header: removed (§5.5). Tile: → square `bg_icon_socket` (§10.3). Dialog `colorSwatch`: **delete `android:background="@drawable/ic_silver_ring"`** — its color is already set at runtime by `updateSwatch()`'s `setBackgroundColor()`, so no drawable is needed. With all three gone, `ic_silver_ring.xml` is unreferenced → **delete the file.** |
| SeekBar hue track `GradientDrawable` (`SliverCustomizeDialog.paintHueTrack`, `cornerRadius = 8f * density`) | rounded | `g.cornerRadius = 0f` |
| `LabeledSeekBar` system thumb | round thumb | **Decision (B11): in scope.** Provide a custom square-gem thumb drawable (reuse the emerald-cut octagon, §6.3/§10.5) via `seek().thumb` / `android:thumb`. |

---

## 10. Secondary elements

### 10.1 Panels — REWRITE `bg_dark_seaweed_panel.xml` (used by Altar frame; header no longer uses it)

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/faded_olive_teal" />
        </shape>
    </item>
    <item android:left="2dp" android:top="2dp" android:right="2dp" android:bottom="2dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/emerald_deep" />
        </shape>
    </item>
    <item android:left="3dp" android:top="3dp" android:right="3dp" android:bottom="3dp">
        <shape android:shape="rectangle">
            <solid android:color="@color/panel_dark_seaweed_bg" />
        </shape>
    </item>
</layer-list>
```

### 10.2 New dialog/panel background — `res/drawable/bg_temple_panel.xml`

Square, opaque (dialogs must not show the pulsing background through them — reads as z-fighting):

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item><shape android:shape="rectangle"><solid android:color="@color/limestone_border" /></shape></item>
    <item android:left="3dp" android:top="3dp" android:right="3dp" android:bottom="3dp">
        <shape android:shape="rectangle"><solid android:color="@color/emerald_deep" /></shape>
    </item>
    <item android:left="4dp" android:top="4dp" android:right="4dp" android:bottom="4dp">
        <shape android:shape="rectangle"><solid android:color="#F0081410" /></shape>
    </item>
</layer-list>
```

Used by: Customize dialog window, Discard dialog window, and (**now in scope, B11**)
the `dialog_customize_sliver.xml` root `background`.

### 10.3 Icon socket — `res/drawable/bg_icon_socket.xml` (replaces `ic_silver_ring` in the Altar tile)

A square recess an icon "sits" in, like a gem setting:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item><shape android:shape="rectangle"><solid android:color="@color/tarnished_silver" /></shape></item>
    <item android:left="2dp" android:top="2dp" android:right="2dp" android:bottom="2dp">
        <shape android:shape="rectangle"><solid android:color="@color/obsidian_black" /></shape>
    </item>
</layer-list>
```

In `layout_item_shortcut_tile.xml`: the icon-frame FrameLayout's
`android:background="@drawable/ic_silver_ring"` → `"@drawable/bg_icon_socket"`. Everything else
in the tile stays (IDs `ivDragHandle`, `tvOrderNumber`, `ivAltarIcon`, `tvAltarName`,
`cbAltarSelect` untouched).

### 10.4 Divider redesign — the Twin Fangs (`ic_divider_fangs.xml`, NEW — replaces `ic_divider_spear`)

Per the 2026-07-11 UX brief (see §12.3): the divider becomes a horizontal lintel line with
**two emerald fangs pointing straight down**, mirror-symmetric about the center, with a
**≈40dp gap between them**.

```
  ▪━━━━━━━━━━━━━━━━━━━▼      ▼━━━━━━━━━━━━━━━━━━━▪
                       ╲    ╱  ╲    ╱
                        ╲  ╱    ╲  ╱          ← two fangs, tips down,
                         ╲╱      ╲╱             inner gap 40dp, centered
```

`res/drawable/ic_divider_fangs.xml` (new file):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="360dp"
    android:height="20dp"
    android:viewportWidth="360"
    android:viewportHeight="20">

    <!-- Lintel line (full width, 3 units thick) -->
    <path android:fillColor="#9AA0A6"
        android:pathData="M0,2 L360,2 L360,5 L0,5 Z" />
    <!-- Blocky bronze end studs -->
    <path android:fillColor="#8C7853"
        android:pathData="M0,0 L6,0 L6,7 L0,7 Z M354,0 L360,0 L360,7 L354,7 Z" />

    <!-- LEFT FANG: 12 wide × 14 deep, inner edge at x=160 -->
    <path android:fillColor="#2E8B57"
        android:pathData="M148,5 L160,5 L154,19 Z" />
    <!-- RIGHT FANG: inner edge at x=200 → inner gap = 40 units ≈ 40dp -->
    <path android:fillColor="#2E8B57"
        android:pathData="M200,5 L212,5 L206,19 Z" />
    <!-- Venom glint at each tip -->
    <path android:fillColor="#A9F5C8"
        android:pathData="M153,15 L155,15 L154,17.5 Z M205,15 L207,15 L206,17.5 Z" />
</vector>
```

**Geometry notes.** Fangs span x 148–160 and 200–212 → the midpoint between them is exactly
x=180 (canvas center) and the inner-edge gap is 40 viewport units. Rendered `fitXY` at
`match_parent`, 1 unit ≈ 1dp on a ~360dp-wide content column — the gap lands at ≈40dp.
**Decision (2026-07-12, B8): accept the `fitXY` ≈40dp gap.** The exact-40dp fixed-width fang-pair
overlay is not built (a visually-identical, not-worth-it refinement).

**Usage swaps (all consumers of `ic_divider_spear`):**

| File | Old | New |
|---|---|---|
| `layout_screen_main_menu.xml` | `ic_divider_spear` @ height 8dp | `ic_divider_fangs` @ height **20dp** + `layout_marginBottom` ≥ 4dp (fangs hang down) |
| `layout_screen_shortcuts_container.xml` | @ 6dp | `ic_divider_fangs` @ **14dp** |
| `dialog_customize_sliver.xml` (section dividers) | spear | `ic_divider_fangs` @ **12dp** |
| `ic_divider_spear.xml` | — | delete once unreferenced |

### 10.5 Checkboxes — **in scope (B11)**

`cbAltarSelect` / `cbArchiveSelect` currently use emerald `buttonTint`. Thematic upgrade: a
custom `selector` — unchecked = empty square socket (§10.3 mini), checked = square socket with an
emerald-cut gem glyph (reuse the octagon geometry from §6.3 as a vector). Files:
`ic_checkbox_socket.xml`, `ic_checkbox_gem.xml`, `selector_gem_checkbox.xml`, then
`android:button="@drawable/selector_gem_checkbox"` on both checkboxes.

> **Decision (2026-07-12, B11): implement the gem checkboxes** (formerly phase-2/optional). Lands in
> Phase 5 (§14) with the rest of the de-rounding/secondary work. Both checkboxes live in the
> RecyclerView **item** layouts (`layout_item_shortcut_tile.xml`, `layout_item_available_app.xml`),
> not the screen layouts — both are in the §13.2 modified set.

### 10.6 Version label → mason's mark

`layout_screen_main_menu.xml` bottom-left version TextView: text `v1.3.5` → **`ΕΚΔ. 1.4.0`**
("ΕΚΔ." abbreviates ΕΚΔΟΣΙΣ, "edition"). **Decision (2026-07-12, B8): use `ΕΚΔ. 1.4.0` in the new
`CaptionChiseled` style.** Pure flavor, zero risk.

---

## 11. Creative Suggestions

The brief's item 5 — ranked by impact-per-effort. Each is scoped enough to become its own task.

### Tier 1 — high impact, low-medium effort

1. **The Serpent's Eye service indicator.** Replace nothing — *add* a small custom view in the
   main-menu header (right slot of the lintel): a closed angular eye when `SidebarService` is
   stopped; when running, the lid opens and the emerald iris pulses in sync with the background
   gems. Start/Stop buttons get instant, thematic feedback. (Custom View ~120 lines; service
   state via a static `isRunning` flag or `ActivityManager` check on resume.)
2. **Crack-from-touch on button press.** In `applyStoneButtonBehavior`'s ACTION_DOWN, overlay a
   one-shot vector crack that flashes from the touch point (a `DustParticleView`-style sibling
   view drawing 2–3 jagged lines outward, fading over 300ms). Pairs with the existing dust burst
   — the slab *fractures* under your thumb.
3. **Tray = the serpent's spine.** In `SidebarService.assembleTrayView()`: replace the flat
   `#E6121212` ScrollView background with a vertical scale-pattern drawable (chevron rows,
   `serpent_scale_dark` on `obsidian_black`), and make the meander border `aged_bronze`. The
   unfurl animation already scales from the edge — rename the feel: the serpent uncoils.
4. **Marble stele positioning mockup (§11.4).** In `PositioningView`, the phone mockup becomes a
   standing stone tablet: square corners (§9), a chiseled 2-step top edge (pediment hint), and
   crosshatch zones re-drawn as **Greek-key hatching**. The "phone" was always an abstraction —
   a stele is the same rectangle, fully in-world.
5. **Greek epigram empty states & microcopy.** `tvAltarEmpty`: "No shortcuts selected" → *"THE
   ALTAR LIES BARE — CHOOSE YOUR OFFERINGS BELOW"*. Toasts: "Shortcuts saved" → *"CARVED IN
   STONE"*; "Sliver updated" → *"THE FANG IS FORGED"*. Discard dialog: *"ABANDON THE
   UNCARVED?"* / buttons *"ABANDON" / "KEEP CARVING"*. Costs strings only; sells the theme
   harder than any drawable.

### Tier 2 — high impact, medium-high effort

6. **Torchlight vignette.** A second ambient layer in `ObsidianCrackView`: a very slow (7–9s)
   low-amplitude flicker of the vignette alpha, as if the room is torch-lit. Two sine waves
   summed (7s + 1.3s at 15% amplitude) reads uncannily like fire without any particle cost.
7. **Mosaic shatter screen transitions.** `showScreen()` currently flips visibility. Wrap it:
   capture the outgoing screen to a bitmap, split into a 6×10 grid of square tesserae, animate
   them falling/fading (staggered 0–150ms, translate+rotate ±4°) while the new screen fades in
   beneath. ~1 custom `TransitionView` + 60 lightweight animations, 400ms total. Square tiles =
   on-theme (mosaics!).
8. **The sliver "awakens".** When the service starts, the live sliver overlay plays a one-shot
   1.2s intro: fangs draw themselves as a stroke animation (PathMeasure segment sweep), then
   fill. `ArcSliverView` gains an optional `playAwakenAnimation()` called from
   `SidebarService.onCreate` after `addSliverIfNeeded()`.
9. **Serpent-scale desaturation → venom highlight in tray.** Pressed tray icons currently just
   clear the desaturation filter. Add a brief emerald ring-flash behind the pressed icon
   (drawable overlay, 250ms fade) — "the serpent strikes" as the app launches.
10. **Marching meander.** Animate the lintel's meander trim: a `ClipDrawable`/translate loop
    scrolling the pattern horizontally at ~24dp/s. Subtle, hypnotic, zero interaction cost.
    (Implement as a 2× wide ImageView with a repeating translate animation.)

### Tier 3 — signature moves, higher effort

11. **Medusa gaze onboarding.** First launch: the screen is raw obsidian; a pair of emerald eyes
    opens in the dark, cracks spider outward from them (animated version of
    `ObsidianCrackView.generateCrack`), light floods the cracks, and the lintel slams down with
    a heavy haptic. 3 seconds, skippable, unforgettable. Gate on a `first_run` prefs key.
12. **Live serpent in the pillars.** Promote the pillar coils from static vector to a slow
    ambient animation: the coil highlights shift down the shaft on a 20s loop (implemented by
    stacking two `AnimatedVectorDrawable` states or a custom View replacing the ImageView). The
    temple breathes.
13. **Oracle mode for the Dummy button.** The stub button becomes **"ORACLE"**: tapping it
    returns a random Delphic maxim ("ΓΝΩΘΙ ΣΑΥΤΟΝ — Know thyself", "Nothing in excess", …) as a
    stone-tablet toast/dialog. Zero-function whimsy that makes the stub *feel* intentional.
14. **Soundscape (optional, default-off).** 3 assets, `SoundPool`: stone-scrape (button press),
    deep stone-thud (screen change), dry serpent hiss (tray unfurl). A settings toggle in the
    Oracle/Dummy slot. Sound is the biggest atmosphere multiplier if the user opts in.
15. **App icon refresh.** A blocky emerald serpent-eye set in an obsidian square (adaptive icon
    foreground), replacing `icon_round.png`'s circular composition — matches the de-rounding law
    at the launcher level.
16. **Tray handedness ritual.** When the sliver snaps to a new side in `PositioningView`, the
    trail particles (already implemented) briefly form a serpent-spine chevron chain rather than
    random scatter — the snake slithers to its new post. (Particle emitter tweak: positions
    sampled along a sine of the drag path instead of pure random.)

---

## 12. Functional UI Changes

> Added 2026-07-11 (second brief, same day). Unlike §§3–10 these four items **change runtime
> behavior**, not just skin. Each is independent and lands in Phase 6 of the plan (§14).

### 12.1 Expanded Positioning canvas + footer button swap

**Goal.** The positioning mockup currently floats as a small island (55% of an already-padded
view). It must now span **pillar-to-pillar with a small gap** horizontally, and stretch
**from the header down to the footer** vertically.

**Layout changes — `layout_screen_positioning_container.xml`:**

1. Content `LinearLayout`: **delete** `android:paddingLeft="48dp"` / `android:paddingRight="48dp"`,
   change `android:paddingBottom="80dp"` → `"16dp"` (keep `paddingTop="12dp"`).
2. Per-child horizontal margins replace the shared padding — this keeps the header the same
   width as the other screens (Law L2) while the canvas alone gets the extra room:

```xml
<include android:id="@+id/templeHeader"
    layout="@layout/layout_temple_header"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:layout_marginHorizontal="52dp" android:layout_marginBottom="8dp" />

<com.dicereligion.edgecase.PositioningView
    android:id="@+id/positioningView"
    android:layout_width="match_parent" android:layout_height="0dp"
    android:layout_weight="1"
    android:layout_marginHorizontal="46dp" />   <!-- 40dp pillar + 6dp breathing gap -->

<!-- Footer (tvPositionInfo + button row) keeps 52dp margins -->
```

3. **Footer swap — BACK on the left, CUSTOMIZE on the right.** IDs, styles, weights unchanged;
   only the element order and the `layout_marginEnd` move:

```xml
<Button android:id="@+id/btnBackToMenuFromPosition" ... android:layout_marginEnd="12dp"
    android:text="BACK" ... />
<Button android:id="@+id/btnCustomizeSliver" ...
    android:text="CUSTOMIZE" ... />
```

**Code changes — `PositioningView.kt`:** a bigger view isn't enough — the mockup itself must
fill it. Replace the fixed width-fraction sizing with **fit-inside** sizing:

```kotlin
// Field changes
private var mockupWidthFraction = 0.98f    // was 0.55f — side gaps now come from layout margins
private var mockupHeightFraction = 0.98f   // NEW

// onSizeChanged(): replace the first two lines
val mockupW = minOf(w * mockupWidthFraction, (h * mockupHeightFraction) / mockupAspectRatio)
val mockupH = mockupW * mockupAspectRatio
```

On tall phones the height cap binds (mockup nearly touches header and footer); on short/wide
screens the width cap binds. The 2.1 aspect is preserved either way.

**Instruction text** ("Drag the sliver to reposition") is currently drawn at `mockupBottom + 60f`
— now off-view. Relocate it *inside* the bottom restricted zone (the crosshatch is dead space
anyway, and text-over-hatching reads as carved signage):

```kotlin
// onDraw(): replace the instruction draw's y-coordinate
val restrictedH = (mockupBottom - mockupTop) * RESTRICTED_FRACTION
canvas.drawText(
    "Drag the sliver to reposition",
    width / 2f,
    mockupBottom - restrictedH / 2f + instructionPaint.textSize / 3f,  // optically centered
    instructionPaint
)
```

### 12.2 The Herald — sliver tracking arrow

**Goal.** An arrow that (a) always points at the sliver — critically, still visible when sliver
opacity is 0%, (b) keeps a **constant 20dp gap** from the sliver's inward-most point no matter
how far the fangs poke inward, (c) flips to the opposite flank when the sliver changes sides,
and (d) is itself a **drag handle** — grabbing the arrow moves the sliver exactly like grabbing
the sliver.

**Visual spec.** Blocky arrow (Law L1: hard corners only) pointing *toward* the sliver:
triangular head 10dp long × 16dp tall + rectangular tail 12dp × 6dp; fill `emerald_bright`
(#50C878) with a 1.5px `#071A15` outline so it reads against both the marble slab and the
crosshatch. Drawn with its own opaque paints → unaffected by the sliver's opacity setting.

```
   sliver on RIGHT edge:               sliver on LEFT edge:

   ─────────┃◄ fang               fang ►┃─────────
     ▬▬▬►   ┃                           ┃   ◄▬▬▬
     ↑ 20dp gap ↑               ↑ 20dp gap ↑
   ─────────┃                           ┃─────────
```

**Code — `PositioningView.kt` additions:**

```kotlin
// ── Tracking arrow (The Herald) ─────────────────────────
private val density = resources.displayMetrics.density
private val arrowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.parseColor("#50C878")     // emerald_bright
    style = Paint.Style.FILL
}
private val arrowOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.parseColor("#071A15")
    style = Paint.Style.STROKE
    strokeWidth = 1.5f
    strokeJoin = Paint.Join.MITER
}
private val arrowPath = Path()

/** X of the sliver's inward-most point (deepest fang tip) at the current preview scale. */
private fun sliverInwardX(): Float {
    val reach = sliverPreviewWidth *
        maxOf(sliverConfig.tooth1Length, sliverConfig.tooth2Length).coerceIn(0f, 1f)
    return if (sliverSide == ArcSliverView.Side.RIGHT) sliverPixelX - reach else sliverPixelX + reach
}

private fun drawTrackingArrow(canvas: Canvas) {
    val gap = 20f * density
    val headLen = 10f * density; val headHalf = 8f * density
    val tailLen = 12f * density; val tailHalf = 3f * density
    val dir = if (sliverSide == ArcSliverView.Side.RIGHT) 1f else -1f   // +x = rightward
    val tipX = sliverInwardX() - dir * gap                              // 20dp inward of the fangs
    val y = sliverPixelY

    arrowPath.reset()
    arrowPath.moveTo(tipX, y)                                           // tip aims at the sliver
    arrowPath.lineTo(tipX - dir * headLen, y - headHalf)
    arrowPath.lineTo(tipX - dir * headLen, y - tailHalf)
    arrowPath.lineTo(tipX - dir * (headLen + tailLen), y - tailHalf)
    arrowPath.lineTo(tipX - dir * (headLen + tailLen), y + tailHalf)
    arrowPath.lineTo(tipX - dir * headLen, y + tailHalf)
    arrowPath.lineTo(tipX - dir * headLen, y + headHalf)
    arrowPath.close()
    canvas.drawPath(arrowPath, arrowFillPaint)
    canvas.drawPath(arrowPath, arrowOutlinePaint)
}

/** Generous hit-test around the arrow so it works as a drag handle. */
private fun isTouchOnArrow(tx: Float, ty: Float): Boolean {
    val gap = 20f * density
    val len = 22f * density          // head + tail
    val slop = 20f * density         // finger-friendly
    val dir = if (sliverSide == ArcSliverView.Side.RIGHT) 1f else -1f
    val tipX = sliverInwardX() - dir * gap
    val nearX = minOf(tipX, tipX - dir * len) - slop
    val farX = maxOf(tipX, tipX - dir * len) + slop
    val halfH = 8f * density + slop
    return tx in nearX..farX && ty >= sliverPixelY - halfH && ty <= sliverPixelY + halfH
}
```

**Wiring (3 one-line edits):**

1. `onDraw()` — after `drawSliverPreview(canvas)`: add `drawTrackingArrow(canvas)`.
2. `onTouchEvent()` ACTION_DOWN — `if (isTouchOnSliver(x, y))` →
   `if (isTouchOnSliver(x, y) || isTouchOnArrow(x, y))`. (The rest of the drag path is shared —
   the arrow needs no separate drag logic.)
3. Nothing else: `setSliverConfig()` already calls `invalidate()`, so the arrow re-gaps
   automatically when fang length / size knobs change in the Customize dialog.

**Behavior notes:**
- During drag and snap the arrow follows for free — it derives from `sliverPixelX/Y` every frame.
- The side flip happens the moment `sliverSide` is finalized at snap-animation end: the arrow
  hops flanks in the same frame the fang mirrors — reads as intentional. **Decision (2026-07-12,
  B10): keep the flip at snap-end** (derive `dir` from `sliverSide`); the mid-flight refinement
  (deriving `dir` from `sliverPixelX < (mockupLeft + mockupRight) / 2f`) is **not** adopted.
- The 20dp gap cannot collapse at extreme `widthDp`/`tooth*Length` configs: the reach is
  computed from the *preview-scaled* geometry, so correctness holds by construction.

### 12.3 Twin-fang divider

Specified with the element library — see **§10.4**: new `ic_divider_fangs.xml` (silver lintel
line, two 12×14 emerald fangs pointing down, inner edges 40 units apart, mirror-symmetric about
the canvas center) plus the swap table for all consuming layouts.

### 12.4 Back-gesture navigation fix

**Root cause.** `MainActivity` overrides the legacy `onBackPressed()` (MainActivity.kt:114).
The app targets SDK 36 — on Android 16, **predictive back is enabled by default for target-36
apps**, and the system back *gesture* is delivered through the `OnBackInvoked` /
`OnBackPressedDispatcher` path, which **never calls the legacy `onBackPressed()` override**.
Result: the swipe gesture bypasses the in-app screen stack entirely; only the on-screen BACK
buttons work (they call `onBackPressed()` directly).

**Fix — migrate to `OnBackPressedCallback`** (androidx.activity, already on the classpath via
appcompat 1.7):

```kotlin
// MainActivity.kt — new import
import androidx.activity.OnBackPressedCallback

// 1. DELETE the entire `override fun onBackPressed()` block (lines 114–126).

// 2. Add the callback as a field (starts disabled = main menu):
private val backCallback = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
        when (currentScreen) {
            Screen.SHORTCUTS ->
                if (stateManager?.isDirty() == true) showDiscardDialog()
                else showScreen(Screen.MAIN_MENU)
            Screen.POSITIONING -> showScreen(Screen.MAIN_MENU)
            Screen.MAIN_MENU -> Unit   // unreachable: callback is disabled on the menu
        }
    }
}

// 3. Register in onCreate() (any point after super.onCreate()):
onBackPressedDispatcher.addCallback(this, backCallback)

// 4. Arm/disarm per screen — ONE line at the end of showScreen(screen):
backCallback.isEnabled = (screen != Screen.MAIN_MENU)

// 5. btnBackToMenu's click listener: replace `onBackPressed()` with
//    `onBackPressedDispatcher.onBackPressed()`.
```

**Why the enable/disable dance:** with the callback *disabled* on the main menu, the system
handles back natively there — the user gets the OS predictive back-to-home animation when
leaving the app, exactly as platform guidelines intend. On sub-screens the *enabled* callback
intercepts both the gesture **and** 3-button back, routing them through the same dirty-check
logic as before. The BACK stone buttons converge on the dispatcher, so there is exactly one
back path in the app.

**Test matrix:** {gesture nav, 3-button nav} × {menu → exits app with predictive animation;
shortcuts clean → menu; shortcuts dirty → discard dialog ("Keep Editing" stays); positioning →
menu}.

---

## 13. Architecture & File Manifest

### 13.1 NEW files (13)

| # | Path | Kind | Purpose | Spec |
|---|---|---|---|---|
| 1 | `app/src/main/res/font/gfs_neohellenic.ttf` | binary | title/display font (Greek-capable) | §4.2 |
| 2 | `app/src/main/res/font/cinzel_black.ttf` | binary | button font | §4.2 |
| 3 | `app/src/main/java/com/dicereligion/edgecase/ObsidianCrackView.kt` | Kotlin | living background | §6.3 |
| 4 | `app/src/main/res/layout/layout_temple_header.xml` | layout | shared header | §5.2 |
| 5 | `app/src/main/res/drawable/bg_temple_lintel.xml` | drawable | header bg | §5.3 |
| 6 | `app/src/main/res/drawable/ic_meander_horizontal.xml` | vector | header trim | §5.4 |
| 7 | `app/src/main/res/drawable/ic_texture_cracks.xml` | vector | button crack overlay | §7.2 |
| 8 | `app/src/main/res/drawable/ic_pillar_serpent_left.xml` | vector | new pillar | §8.2 |
| 9 | `app/src/main/res/drawable/bg_temple_panel.xml` | drawable | dialog bg | §10.2 |
| 10 | `app/src/main/res/drawable/bg_icon_socket.xml` | drawable | tile icon frame | §10.3 |
| 11 | `selector_gem_checkbox.xml` + `ic_checkbox_socket.xml` + `ic_checkbox_gem.xml` | drawable | gem checkboxes (**in scope, B11**) | §10.5 |
| 12 | ~~`eb_garamond.ttf`~~ | — | **not adopted** — body text stays `serif` (A3) | §4.2 |
| 13 | `app/src/main/res/drawable/ic_divider_fangs.xml` | vector | twin-fang divider | §10.4 |

> **Fonts already sourced (B3, 2026-07-12):** `gfs_neohellenic.ttf` (title, Greek-capable) +
> `cinzel_black.ttf` (buttons/captions) are in `res/font/`, OFL licenses in `Docs/fonts_licenses/`.
> Row counts here are nominal — row 11 is
> multiple files and row 12 (`eb_garamond`) was dropped, so the concrete new-file total is **≈14**.
> **File to delete:** `ic_silver_ring.xml` once unreferenced (A1, §9).

### 13.2 MODIFIED files (14)

| Path | Changes |
|---|---|
| `res/values/colors.xml` | + §3.2 block (≈22 colors) |
| `res/values/dimens.xml` | + §4.5 block (8 dimens) |
| `res/values/styles.xml` | + `TitleMonolith`, `SlabButtonText`, `CaptionChiseled` (§4.4); legacy styles kept |
| `res/drawable/bg_stone_button.xml` | full rewrite §7.3 |
| `res/drawable/bg_stone_button_pressed.xml` | full rewrite §7.4 |
| `res/drawable/bg_dark_seaweed_panel.xml` | rewrite §10.1 (drop corners, double border) |
| `res/drawable/ic_divider_spear.xml` | superseded by `ic_divider_fangs` (§10.4) — delete once unreferenced |
| `res/layout/activity_main.xml` | root bg → `@color/obsidian_black` |
| `res/layout/layout_screen_main_menu.xml` | header→include; ObsidianCrackView first child; bg attr removed; pillar swap (§8.4); button style swaps (§7.5); version label (§10.6); divider → `ic_divider_fangs` @ 20dp (§10.4) |
| `res/layout/layout_screen_shortcuts_container.xml` | same set **+ delete the `ic_silver_ring` header ImageView**; captions → `CaptionChiseled`; divider → `ic_divider_fangs` @ 14dp (§10.4) |
| `res/layout/layout_screen_positioning_container.xml` | same set + expanded-canvas padding/margins & BACK/CUSTOMIZE swap (§12.1) |
| `res/layout/dialog_customize_sliver.xml` | section dividers → `ic_divider_fangs` @ 12dp (§10.4); **remove `ic_silver_ring` from `colorSwatch` (A1)**; root bg → `bg_temple_panel` (**now in scope, B11**); button text → `SlabButtonText` (§7.5) |
| `res/layout/layout_item_shortcut_tile.xml` | ring → socket (§10.3); `cbAltarSelect` → gem checkbox (§10.5, B11); **keep `serif` on `tvAltarName` (A2)** |
| `res/layout/layout_item_available_app.xml` | `cbArchiveSelect` → gem checkbox (§10.5, B11); **keep `serif` on `tvArchiveName` (A2)** — otherwise unchanged |
| `MainActivity.kt` | scoped header-title assignment (§5.5); discard-dialog square bg (§9); back-gesture migration to `OnBackPressedDispatcher` (§12.4); microcopy (#5) is a Phase-7 opt-in (not core) |
| `PositioningView.kt` | `mockupCornerRadius = 0f` (both assignments, §9); fit-inside mockup sizing + instruction-text relocation (§12.1); tracking arrow (§12.2) |
| `SliverCustomizeDialog.kt` | window bg → `bg_temple_panel`; hue-track `cornerRadius = 0f` (§9); `updateSwatch()` already sets the swatch color at runtime, so the removed `ic_silver_ring` needs no code change (A1) |

### 13.3 Untouched (guaranteed)

`SidebarService.kt` (phase 1), `ArcSliverView.kt`, `SliverShape.kt`, `SliverConfig.kt`,
`SliverPreviewView.kt`, `LabeledSeekBar.kt`, `ShortcutStateManager.kt`, all adapters, drag
callback, `DustParticleView.kt`, manifest, gradle files, prefs schema, all IDs and callbacks.

### 13.4 Framework/API inventory (everything this theme touches)

| API | Used for |
|---|---|
| `res/font` XML fonts (`android:fontFamily="@font/…"`) | §4 typography |
| `layer-list`, `shape`, `selector` drawables | lintel, slabs, panels, sockets |
| `VectorDrawable` (`pathData`, stroke+fill) | cracks texture, pillars, meander |
| Custom `View` + `Canvas` (`Path`, `Paint`, `RadialGradient`, `PathMeasure`, `Bitmap`, `Matrix`) | `ObsidianCrackView` |
| `ValueAnimator` (INFINITE) | gem pulse clock |
| `onVisibilityChanged` / attach-detach | animator lifecycle |
| `Window.setBackgroundDrawableResource` | square dialogs |
| `<include>` + scoped `findViewById` | shared header |
| `View.scaleX = -1` | mirrored right pillar |
| `OnBackPressedDispatcher` + `OnBackPressedCallback` (androidx.activity — transitive via appcompat 1.7.0; **recommend adding an explicit `androidx.activity` dependency for stability, C**) | §12.4 back-gesture navigation |
| Existing: `ViewPropertyAnimator`, `Vibrator`, `ItemTouchHelper` | unchanged behaviors |

---

## 14. Implementation Plan

Ordered so the app **builds and runs after every phase**.

### Phase 0 — Assets & tokens *(no visible change; ~30 min)*
- [x] **Fonts sourced (2026-07-12):** `gfs_neohellenic.ttf` (title, covers Greek Ξ/Λ) and
      `cinzel_black.ttf` (buttons/captions) in `res/font/`; OFL licenses in `Docs/fonts_licenses/`.
      Title Greek-glyph coverage is resolved (§4.3) — no Latin fallback needed.
- [ ] Add §3.2 colors, §4.5 dimens, §4.4 styles.
- [ ] Build. Nothing changed visually. Commit: `Theme tokens & fonts`.

### Phase 1 — Header unification *(brief item 1; ~1 h)*
- [ ] Create `bg_temple_lintel.xml`, `ic_meander_horizontal.xml`, `layout_temple_header.xml`.
- [ ] Replace headers in all three screens; **delete the ring** from shortcuts header.
- [ ] Wire per-screen titles in `MainActivity` (§5.5).
- [ ] Verify: identical lintel on all screens, centered blocky title. Commit: `Temple lintel headers`.

### Phase 2 — Slab buttons *(brief item 3; ~1 h)*
- [ ] `ic_texture_cracks.xml`; rewrite the two `bg_stone_button*` drawables.
- [ ] Sweep all 12 buttons: `SlabButtonText`, delete inline `fontFamily="serif"` (§7.5 — critical).
- [ ] Verify press animation/haptics/dust intact. Commit: `Cracked limestone slabs`.

### Phase 3 — Living background *(brief item 2; ~2–3 h)*
- [ ] `ObsidianCrackView.kt` (remember §6.3's flagged cleanup).
- [ ] Insert into 3 screens; root backgrounds → obsidian.
- [ ] Tune `maxGlowAlpha` on-device for text contrast (§6.5).
- [ ] Verify: animator stops on hidden screens (Layout Inspector / debug log). Commit: `Obsidian crack background`.

### Phase 4 — Serpent pillars *(brief item 4; ~1–2 h)*
- [ ] `ic_pillar_serpent_left.xml`; swap all six pillar ImageViews (mirror trick §8.3); widths/alphas/paddings (§8.4).
- [ ] Visual pass in Android Studio preview at multiple heights (`fitXY` stretch check — if coil
      ellipses distort badly on short screens, change `scaleType` to `fitStart` + `adjustViewBounds`).
- [ ] Commit: `Serpentine pillars`.

### Phase 5 — De-rounding & secondary *(~1.5–2 h — expanded by B11)*
- [ ] §9 audit table end-to-end (panels, PositioningView radii, both dialogs, hue track, socket, divider, version label, **dialog `colorSwatch` ring removal / A1**).
- [ ] **Gem checkboxes (§10.5, B11):** `ic_checkbox_socket.xml`, `ic_checkbox_gem.xml`, `selector_gem_checkbox.xml`; wire `android:button` on `cbAltarSelect` + `cbArchiveSelect`.
- [ ] **Square dialog background (B11):** `dialog_customize_sliver.xml` root → `bg_temple_panel`; migrate its title/caption/button text to the new styles.
- [ ] **Square-gem SeekBar thumb (§9, B11)** on `LabeledSeekBar`.
- [ ] **Legacy-style migration (B11):** move remaining dialog consumers off `EngravedHeader`/`StoneButtonText`/`CaptionSerif` (body `BodySerif` stays, A3).
- [ ] Version label → `ΕΚΔ. 1.4.0` in `CaptionChiseled` (§10.6, B8).
- [ ] Commit: `Square the temple`.

### Phase 6 — Functional UI changes *(§12; ~3–4 h)*
- [ ] Positioning canvas expansion + BACK/CUSTOMIZE swap (§12.1) — verify mockup spans
      pillar-to-pillar and header-to-footer; drag/snap/persistence unaffected.
- [ ] Tracking arrow (§12.2) — verify: drag from arrow works, 20dp gap holds at min/max fang
      length and size knobs, side flip on snap, visible at 0% sliver opacity.
- [ ] Twin-fang divider (§10.4) — swap in all consuming layouts; delete `ic_divider_spear.xml`
      when unreferenced.
- [ ] Back-gesture migration (§12.4) — run the full test matrix (gesture + 3-button nav).
- [ ] One commit per item. Commits: `Expanded positioning canvas`, `Sliver tracking arrow`,
      `Twin-fang divider`, `Predictive back migration`.

### Phase 7 — Tier-1 creative picks *(scope by appetite)*
- [ ] Recommended minimum: #5 microcopy (30 min) + #1 Serpent's Eye (2–3 h) + #3 tray scales (1–2 h).
- [ ] Each is its own commit.

### Rollback safety
Every phase is a standalone commit; `git revert` any phase independently. Phases 1–5 have zero
Kotlin behavioral changes beyond cosmetic assignments, so reverts cannot strand state.

---

## 15. Performance Budget & Android APIs

| Concern | Budget | Mitigation |
|---|---|---|
| Background per-frame cost | ≤ 1 bitmap blit + ≤ 16 gradient circles + ≤ 16 small paths | Static layer pre-render (§6.2); no blurs |
| Memory | 1 ARGB_8888 screen-size bitmap ≈ 1080×2400×4 ≈ 9.9 MB | Acceptable; single instance per *visible* screen; recycled on resize |
| Animator count | 1 per visible `ObsidianCrackView` (= 1 at any time) | visibility-gated start/stop |
| Overdraw | Obsidian view + content ≈ 2× worst case | Removed the old layer-list bg; pillars are narrow columns |
| Battery | ambient 60fps invalidate on config screens ONLY (never in the overlay service) | The sliver/tray overlay is untouched — the always-on surface stays static |
| Cold start | +2 font TTFs (~300 KB) | negligible |
| APK size | +fonts +vectors ≈ 400 KB | negligible |

Hard rules: **no `BlurMaskFilter`** (silently falls back to software layers), no per-frame `Path`
allocation (reuse `gemPath`), no animation in `SidebarService` surfaces in this overhaul.

---

## 16. QA / Verification Checklist

**Visual**
- [ ] All three screens show the identical lintel header; titles centered; blocky font renders (not fallback serif).
- [ ] Shortcuts header has **no ring**.
- [ ] Zero rounded corners anywhere: buttons, panels, dialogs (both), positioning mockup, hue track, icon sockets.
- [ ] Background: cracks visible, gems pulse asynchronously, text everywhere remains readable (contrast spot-check over the brightest gem at peak).
- [ ] Buttons: double border + cracks visible in default AND pressed states; press still sinks 4dp with haptic + dust.
- [ ] Pillars: serpent coils read clearly at phone size; heads face inward; emerald eyes visible; no ugly stretch on smallest supported height.

**Functional (identical to v1.3.5, except the §12 items below)**
- [ ] Back navigation works from BOTH the BACK buttons and the back swipe gesture (and 3-button
      back): shortcuts clean → menu; shortcuts dirty → discard dialog ("Keep Editing" stays);
      positioning → menu; main menu → app exits with predictive animation (§12.4).
- [ ] Positioning mockup spans pillar-to-pillar (small gap) and header-to-footer (§12.1);
      footer shows BACK left, CUSTOMIZE right.
- [ ] Tracking arrow: visible at 0% sliver opacity; maintains the 20dp gap at min/max fang
      length + size configs; flips flank on side change; dragging the arrow moves the sliver
      identically to dragging the sliver (§12.2).
- [ ] Twin-fang divider: fangs point down, symmetric about center, ≈40dp inner gap (§10.4).
- [ ] Instruction text renders inside the bottom crosshatch zone, not clipped (§12.1).
- [ ] Add/remove/reorder/save shortcuts; empty-state text shows.
- [ ] Positioning drag + snap + persistence; Customize dialog all controls; Apply hot-reloads overlay.
- [ ] Service start/stop; sliver swipe → tray; icon launch; outside-touch dismiss.
- [ ] Rotation / theme-change smoke test (activity recreation regenerates background cleanly).

**Performance**
- [ ] GPU profile bars stay green on the main menu with pulse running.
- [ ] Switch screens 20×: no bitmap leak (Memory Profiler steady).
- [ ] Leave app on menu 10 min: no thermal/battery anomaly; animator confirmed stopped when app backgrounded.

---

*End of NewTheme.md — written 2026-07-11 against the v1.3.5 codebase as documented in
`Docs/stats.md`; amended the same day with §12 (functional UI changes: expanded positioning
canvas + button swap, sliver tracking arrow, twin-fang divider, predictive-back migration).
On implementation, update `stats.md` §6 (Resources), §7 (Feature Inventory), §8 (back-navigation
flow), and bump the version label per §10.6.*

*Amended 2026-07-12 — open review points resolved (search "**Decision (2026-07-12)**"): Greek-glyph
title adopted with a flagged font-coverage dependency (§4.3); bundled fonts sourced + OFL licenses
archived (§4.2, Phase 0); fixed seed & `maxGlowAlpha` 0.55 (§6.5); recommended limestone bevel (§7.3);
`fitXY` pillars (§8.4) and `fitXY` twin-fang divider (§10.4); version label `ΕΚΔ. 1.4.0` in
`CaptionChiseled` (§10.6); tracking-arrow flip at snap-end (§12.2); dialog `ic_silver_ring` removed →
file deletable (§9, A1); the `fontFamily="serif"` sweep scoped to buttons only, body rows keep serif
(§7.5, A2); former phase-2 items (gem checkboxes, square dialog bg, square-gem thumb, legacy-style
migration) pulled into Phase 5 (§10.5, §10.2, §14). The title's Greek Ξ/Λ coverage is resolved with
**GFS Neohellenic Bold** (`gfs_neohellenic.ttf`, §4.3); Caesar Dressing was dropped as Latin-only.*
