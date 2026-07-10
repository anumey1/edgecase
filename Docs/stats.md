# EdgeCase — Project Status & Blueprint

> **Last Updated:** 2026-07-10
> **Project Root:** `/Users/anumey/Work/Android/EdgeCase`
> **Package:** `com.dicereligion.edgecase`
> **App Name:** EdgeCase
> **Version:** 1.3.5 (versionCode 2, UI label displays "v1.3.5")
>
> **Recent change (2026-07-10):** Added the **Sliver Customize** feature — a popup on the Position screen
> that lets the user edit the sliver's opacity, color (default grey or a custom hue), the eight fang-geometry
> knobs, and its width/height, with a live preview and persistence. The fang geometry was refactored into a
> single shared builder (`SliverShape`) + config object (`SliverConfig`), and the live-overlay hot-reload was
> made in-place to fix a stale-overlay bug. See §5.11–5.15, §7 (items 35–43), and §8.

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
6. [Resources — Complete Reference](#6-resources--complete-reference)
   - [6.1 Layouts](#61-layouts)
   - [6.2 Drawables](#62-drawables)
   - [6.3 Values (Colors, Dimensions, Strings, Styles, Themes)](#63-values)
   - [6.4 Mipmaps & App Icon](#64-mipmaps--app-icon)
   - [6.5 XML Configuration](#65-xml-configuration)
7. [Feature Inventory](#7-feature-inventory)
8. [Data Flow & State Management](#8-data-flow--state-management)
9. [Permissions](#9-permissions)
10. [Known Limitations & Future Work](#10-known-limitations--future-work)

---

## 1. Project Overview

EdgeCase is an Android edge-launcher application themed with a **Hellenic Serpent** aesthetic. It provides a persistent, floating sidebar overlay (called the "Sliver") that lives on the left or right screen edge, which the user can swipe to reveal a tray of shortcut icons for launching apps. The main configuration activity allows users to select which apps appear, reorder them, and reposition the sliver on screen.

### Core Concept

- **Sliver (Fangs)**: A single continuous shape rendered at the screen edge. The spine (screen-edge side) is a flat vertical line; the inward-facing edge has two sharp fang protrusions with a central recess/gums between them. **As of v1.3.5 the sliver is fully user-customizable** — color (default grey `#808080` or a custom hue), opacity, the eight fang-geometry knobs, and its size (default 27dp × 38dp) — all persisted and applied to the live overlay. Defaults reproduce the original 50%-grey angular shape. See the `SliverConfig`/`SliverShape` model (§5.11–5.12) and `Docs/SliverAnatomy.md` for the knob spec.
- **Tray**: An 80dp-wide scrollable panel that unfurls (scales in from the edge) when the user swipes the Sliver. Contains desaturated app icons (20% desaturation for ancient-theme look). Tapping an icon launches the app.
- **Configuration**: A three-screen activity (Main Menu → Shortcuts → Positioning) for managing the shortcut list and sliver placement. The Positioning screen also hosts a **Customize** popup for the sliver's appearance/geometry.

---

## 2. Architecture & Blueprint

```
┌─────────────────────────────────────────────────────────┐
│                    MainActivity                         │
│  (AppCompatActivity, 3 virtual screens via visibility)  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │Screen 1  │  │Screen 2      │  │Screen 3           │  │
│  │MAIN MENU │  │SHORTCUTS     │  │POSITIONING        │  │
│  │          │  │              │  │                   │  │
│  │• Start   │  │┌────────────┐│  │┌─────────────────┐│  │
│  │  Service │  ││ Altar      ││  ││ PositioningView ││  │
│  │• Stop    │  ││ (top 30%)  ││  ││ (phone mockup   ││  │
│  │  Service │  ││ Recycler   ││  ││  w/ draggable   ││  │
│  │• Short-  │  ││ drag-to-   ││  ││  sliver preview)││  │
│  │  cuts    │  ││ reorder    ││  │└─────────────────┘│  │
│  │• Position│  │└────────────┘│  │                   │  │
│  │• Dummy   │  │┌────────────┐│  │                   │  │
│  │          │  ││ Archives   ││  │                   │  │
│  │          │  ││ (bot 60%)  ││  │                   │  │
│  │          │  ││ Recycler   ││  │                   │  │
│  │          │  │└────────────┘│  │                   │  │
│  │          │  │[BACK] [SAVE] │  │                   │  │
│  └──────────┘  └──────────────┘  └──────────────────┘  │
│                                                         │
│  DustParticleView overlay (bursts on button press)      │
└─────────────────────────────────────────────────────────┘

                    ║ (SharedPreferences: EdgeCasePrefs)
                    ║

┌─────────────────────────────────────────────────────────┐
│                  SidebarService                         │
│  (Foreground Service, TYPE_APPLICATION_OVERLAY)         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────┐       ┌──────────────────────────┐ │
│  │  ArcSliverView  │ ────→ │  TrayView                │ │
│  │  (27×38dp)      │ swipe │  (80dp wide, scrollable) │ │
│  │                 │       │                          │ │
│  │  • Sharp fangs  │       │  • Meander border        │ │
│  │  • Grey fill    │       │  • Desaturated icons     │ │
│  │  • Swipe detect │       │  • Unfurl animation      │ │
│  │                 │       │  • Launch intent         │ │
│  └─────────────────┘       └──────────────────────────┘ │
│                                                         │
│  Hot-reload via Intents:                                │
│  • ACTION_UPDATE_SHORTCUTS → refreshTrayUiElements()    │
│  • ACTION_UPDATE_POSITION  → applySliverUpdate()        │
│  • ACTION_UPDATE_STYLE     → applySliverUpdate()        │
│    (in-place: recolor + updateViewLayout, no recreate)  │
└─────────────────────────────────────────────────────────┘
```

The Sliver's appearance/geometry comes from a `SliverConfig` (loaded from prefs). Both `ArcSliverView` (live)
and the two previews (`PositioningView`, `SliverPreviewView`) render via the single builder `SliverShape.buildPath`.

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

| Key | Type | Description |
|-----|------|-------------|
| `saved_shortcuts_order` | String (CSV) | Ordered list of selected package names |
| `saved_shortcuts` | Set<String> | Legacy set of selected package names |
| `sliver_side` | String | `"left"` or `"right"` |
| `sliver_y_bias` | Float | 0.0–1.0, vertical position within valid zone |
| `sliver_opacity` | Float | 0.0–1.0 overall sliver transparency |
| `sliver_color_mode` | String | `"DEFAULT"` (grey) or `"CUSTOM"` (hue) |
| `sliver_color_hue` | Float | 0–360 hue (used when mode = CUSTOM) |
| `sliver_t1_thickness` / `sliver_t2_thickness` | Float | top/bottom fang thickness (fraction) |
| `sliver_t1_length` / `sliver_t2_length` | Float | top/bottom fang inward length (fraction) |
| `sliver_t1_tipy` / `sliver_t2_tipy` | Float | top/bottom fang tip vertical position (angle) |
| `sliver_gums_depth` | Float | gums/bridge depth (fraction) |
| `sliver_gap` | Float | gap between the fangs (fraction) |
| `sliver_width_dp` / `sliver_height_dp` | Float | sliver size in dp (defaults 27 / 38) |

The 13 `sliver_*` appearance/geometry keys are read/written through the `SliverConfig` model (§5.11).

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
└── widthDp / heightDp                 (default 27 / 38)
    • baseColor()  → grey or HSV(hue,1,1)
    • fillColor()  → baseColor with alpha = opacity·255
```

### Inter-Component Communication

```
MainActivity ──startService(intent)──→ SidebarService
                  ↑                    ↑
                  │ ACTION_UPDATE_SHORTCUTS / ACTION_UPDATE_POSITION / ACTION_UPDATE_STYLE
                  └────────────────────┘
```

All use `startService()` with action-bearing Intents. The service processes them in `onStartCommand()`.
`ACTION_UPDATE_STYLE` (sent when the Customize dialog is applied) and `ACTION_UPDATE_POSITION` both route to
`applySliverUpdate()`, which updates the existing overlay view in place.

---

## 3. Complete Directory Tree

```
EdgeCase/
├── build.gradle.kts                          # Root build script (AGP + Kotlin plugins)
├── settings.gradle.kts                       # Project settings, repositories, module include
├── gradle.properties                         # JVM args, Kotlin code style
├── local.properties                          # Local SDK path (machine-specific)
│
├── gradle/
│   ├── gradle-daemon-jvm.properties          # JDK path for Gradle daemon
│   └── libs.versions.toml                    # Version catalog (AGP 9.2.1, Kotlin 2.2.10)
│
└── app/
    ├── build.gradle.kts                      # Module build config
    ├── proguard-rules.pro                    # ProGuard rules (stock, unused)
    ├── .gitignore                            # Ignores /build
    │
    └── src/
        └── main/
            ├── AndroidManifest.xml
            │
            ├── java/com/dicereligion/edgecase/
            │   ├── ActiveShortcutsAdapter.kt
            │   ├── AppInfoData.kt
            │   ├── ArcSliverView.kt
            │   ├── AvailableAppsAdapter.kt
            │   ├── DustParticleView.kt
            │   ├── MainActivity.kt
            │   ├── LabeledSeekBar.kt              # Reusable label+slider+value row (Customize dialog)
            │   ├── PositioningView.kt
            │   ├── ShortcutDragCallback.kt
            │   ├── ShortcutStateManager.kt
            │   ├── SidebarService.kt
            │   ├── SliverConfig.kt                # Sliver appearance/geometry model + prefs I/O
            │   ├── SliverCustomizeDialog.kt       # "Customize Sliver" popup controller
            │   ├── SliverPreviewView.kt           # Live sliver preview inside the dialog
            │   └── SliverShape.kt                 # Shared parametric fang-path builder
            │
            └── res/
                ├── drawable/
                │   ├── bg_dark_seaweed_panel.xml
                │   ├── bg_stone_button.xml
                │   ├── bg_stone_button_pressed.xml
                │   ├── bg_stone_texture.xml
                │   ├── ic_check_rune.xml
                │   ├── ic_divider_spear.xml
                │   ├── ic_launcher_background.xml
                │   ├── ic_launcher_foreground.xml
                │   ├── ic_meander_border.xml
                │   ├── ic_pillar_left.xml
                │   ├── ic_pillar_right.xml
                │   ├── ic_silver_ring.xml
                │   ├── icon_round.png            (512×512 PNG, the app icon)
                │   └── selector_stone_button.xml
                │
                ├── layout/
                │   ├── activity_main.xml
                │   ├── dialog_customize_sliver.xml   # Customize Sliver popup content
                │   ├── layout_item_available_app.xml
                │   ├── layout_item_shortcut_tile.xml
                │   ├── layout_screen_main_menu.xml
                │   ├── layout_screen_positioning_container.xml
                │   └── layout_screen_shortcuts_container.xml
                │
                ├── mipmap-anydpi/
                │   ├── ic_launcher.xml           (adaptive icon)
                │   └── ic_launcher_round.xml     (adaptive round icon)
                │
                ├── mipmap-mdpi/
                │   ├── ic_launcher.png           (48×48)
                │   └── ic_launcher_round.png     (48×48)
                │
                ├── mipmap-hdpi/
                │   ├── ic_launcher.png           (72×72)
                │   └── ic_launcher_round.png     (72×72)
                │
                ├── mipmap-xhdpi/
                │   ├── ic_launcher.png           (96×96)
                │   └── ic_launcher_round.png     (96×96)
                │
                ├── mipmap-xxhdpi/
                │   ├── ic_launcher.png           (144×144)
                │   └── ic_launcher_round.png     (144×144)
                │
                ├── mipmap-xxxhdpi/
                │   ├── ic_launcher.png           (192×192)
                │   └── ic_launcher_round.png     (192×192)
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
- **Repositories:** Google (with group regex filters for `com.android.*`, `com.google.*`, `androidx.*`), Maven Central, Gradle Plugin Portal
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

- **namespace / applicationId:** `com.dicereligion.edgecase`
- **compileSdk:** 36 (with minorApiLevel 1)
- **minSdk:** 30
- **targetSdk:** 36
- **Java:** 11 (source & target)
- **Minification:** disabled (isMinifyEnabled = false)
- **Dependencies:**
  - `androidx.appcompat`
  - `androidx.recyclerview`
  - `androidx.core.ktx`
  - `junit` (test)
  - `androidx.espresso.core` (androidTest)
  - `androidx.junit` (androidTest)

---

## 5. Source Code — Full Reference

### 5.1 `MainActivity.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/MainActivity.kt`
**Extends:** `AppCompatActivity`

**Responsibilities:**
- Hosts the three-screen UI via visibility toggling (no fragments)
- Wires stone button press animations (translationY + haptics + dust particles)
- Manages permissions flow for overlay & battery optimization
- Controls foreground service start/stop
- Initializes bipartite Shortcuts screen (Altar + Archives)
- Initializes Positioning screen with live sliver preview (applies saved `SliverConfig` to the preview)
- Opens the **Customize Sliver** dialog and, on Apply, refreshes the preview + hot-reloads the overlay (`ACTION_UPDATE_STYLE`)
- Persists and reloads shortcut state to/from SharedPreferences

**Key State:**

| Field | Type | Purpose |
|---|---|---|
| `screenMainMenu` | View | Screen 1 container |
| `screenShortcuts` | View | Screen 2 container |
| `screenPositioning` | View | Screen 3 container |
| `stateManager` | ShortcutStateManager? | Manages Altar/Archives dual-list state |
| `altarAdapter` | ActiveShortcutsAdapter? | RecyclerView adapter for Altar |
| `archiveAdapter` | AvailableAppsAdapter? | RecyclerView adapter for Archives |
| `positioningView` | PositioningView? | Custom view for sliver placement |
| `dustView` | DustParticleView? | Particle burst overlay |
| `vibrator` | Vibrator? | Haptic engine |

**Screen Routing:**

```kotlin
private enum class Screen { MAIN_MENU, SHORTCUTS, POSITIONING }
```

- `showScreen(screen)` toggles visibility of all three screens
- Shortcuts screen: lazy-initialized via `initShortcutsScreen()`, state refreshed on re-entry
- Positioning screen: lazy-initialized via `initPositioningScreen()`
- Back press: shows discard dialog if shortcuts are dirty, else navigates to main menu

**Button Map:**

| Button ID | Action |
|---|---|
| `btnShortcuts` | Navigate to Shortcuts screen |
| `btnPosition` | Navigate to Positioning screen |
| `btnDummy` | Toast "Dummy — nothing here yet" |
| `btnStartService` | Check permissions → start SidebarService |
| `btnStopService` | Stop SidebarService |
| `btnBackToMenu` | Trigger onBackPressed (with dirty check) |
| `btnSaveShortcuts` | Commit shortcuts, notify service |
| `btnCustomizeSliver` | Open the Customize Sliver dialog (`openCustomizeSliverDialog()`) |
| `btnBackToMenuFromPosition` | Return to main menu |

**Customize hook (`openCustomizeSliverDialog()`):** loads the current `SliverConfig`, shows
`SliverCustomizeDialog`; on Apply it applies the returned config to `positioningView` and sends
`ACTION_UPDATE_STYLE` to the service, then toasts "Sliver updated".

**Stone Button Behavior (`applyStoneButtonBehavior`):**
- `ACTION_DOWN`: animate translationY down by `stone_button_pressed_translation` (4dp), trigger haptic (30ms, amplitude 255), burst 6 dust particles
- `ACTION_UP/CANCEL`: animate translationY back to 0

---

### 5.2 `SidebarService.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SidebarService.kt`
**Extends:** `Service`

**Responsibilities:**
- Manages a persistent foreground service hosting the edge-overlay UI
- Instantiates two overlay windows: the Sliver (ArcSliverView) and the Tray (LinearLayout with ScrollView)
- Handles swipe gesture → tray unfurl transition (and tray dismiss → sliver restore)
- Loads shortcut icons from SharedPreferences, applies 20% desaturation filter
- Hot-reloads via Intent actions: shortcuts update and position change
- System notification for foreground service compliance

**Constants:**

| Constant | Value |
|---|---|
| `CHANNEL_ID` | `"EdgeCaseEngineChannel"` |
| `NOTIFICATION_ID` | `9182` |
| `ACTION_UPDATE_SHORTCUTS` | `"com.dicereligion.edgecase.UPDATE_SHORTCUTS"` |
| `ACTION_UPDATE_POSITION` | `"com.dicereligion.edgecase.UPDATE_POSITION"` |
| `ACTION_UPDATE_STYLE` | `"com.dicereligion.edgecase.UPDATE_STYLE"` |

**Sliver config:** the service holds a `config: SliverConfig` (loaded from prefs in `onCreate` and again in
`applySliverUpdate()`). It drives the overlay size, the fang geometry (via `ArcSliverView` + `SliverShape`),
and the fill color/opacity.

**Overlay Window Parameters (Sliver):**
- Type: `TYPE_APPLICATION_OVERLAY`
- Flags: `FLAG_NOT_FOCUSABLE`, `FLAG_LAYOUT_IN_SCREEN`, `FLAG_LAYOUT_NO_LIMITS`, `FLAG_WATCH_OUTSIDE_TOUCH`
- Format: `TRANSLUCENT`
- Size: `config.widthDp × config.heightDp` dp (default 27 × 38)
- Gravity: `END|TOP` (right) or `START|TOP` (left)
- Y position: mapped from `yBias` [0,1] → vertical range [10%, 90%] of screen

**Overlay Window Parameters (Tray):**
- Same type/flags/format
- Size: 80dp wide, height = sliver height × 7 (38dp × 7 = 266dp)
- Same gravity and Y as sliver

**Tray Layout Structure:**
```
LinearLayout (horizontal, root)
├── ImageView (meander border, 12dp wide, 0.7 alpha)  [position depends on side]
└── ScrollView (background #E6121212)
    └── LinearLayout (vertical, bottom gravity)
        ├── ImageView (shortcut icon, 48dp)
        ├── ImageView (shortcut icon, 48dp)
        └── ...
```

**Tray Icon Behavior:**
- On press: color filter cleared (full saturation restored)
- Haptic: 20ms, amplitude 150
- Launches app via `getLaunchIntentForPackage()`
- Dismisses tray after launch

**State Transitions:**
- `transitionToExpandedTray()`: Remove sliver window → scale-in animation on tray (scaleX 0→1 over 250ms with DecelerateInterpolator, pivot at edge) → add tray window → haptic burst
- `transitionToSliverState()`: Remove tray window → add sliver window back

**In-place update (`applySliverUpdate()`):** Handles both `ACTION_UPDATE_POSITION` and `ACTION_UPDATE_STYLE`.
Reloads position + `SliverConfig`, recomputes window params, then updates the **existing** sliver view via
`ArcSliverView.applyConfig(config, side)` + `windowManager.updateViewLayout(...)` — it does **not** destroy and
recreate the overlay. This replaced the older `reinitializeSliverForPosition()` (remove-then-add), which had a
race that could leave a stale sliver window on screen (see §7 item 43 / §10). The tray is rebuilt so its
size/side match.

**Desaturation Filter:**
```kotlin
cm.setSaturation(0.8f) // 80% saturation = 20% desaturation
```

---

### 5.3 `ShortcutStateManager.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ShortcutStateManager.kt`

**Responsibilities:**
- Single source of truth for the dual-list (Altar + Archives) state during shortcut editing
- Loads/persists shortcut order from SharedPreferences
- Provides dirty-checking for discard-confirmation flow
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
| `isActiveShortcut(pkg)` | True if in Altar AND selected (will survive Save) |
| `isInAltar(pkg)` | True if exists in Altar at all (selected or not) |
| `isDirty()` | True if current selection ≠ committed selection |
| `setFromArchives(pkg, add)` | Add/remove from Altar via Archives checkbox |
| `toggleAltarSelection(pos)` | Toggle selected state in Altar, returns new value |
| `moveAltarItem(from, to)` | Reorder Altar item via drag-and-drop |
| `getAppInfo(pkg)` | Lookup AppInfoData by package name |
| `commit()` | Persist selected items to prefs, evict unselected |
| `discard()` | Reset altarItems to committed state |

**SharedPreferences Format:**
- `saved_shortcuts_order`: comma-separated ordered package names (e.g., `"com.app1,com.app2,com.app3"`)
- `saved_shortcuts`: legacy Set<String> (written for backward compatibility)

**Commit Logic:**
1. Filter `altarItems` to only those with `isSelected == true`
2. Extract ordered list of package names
3. Write `saved_shortcuts_order` and `saved_shortcuts` to SharedPreferences
4. Remove unselected items from `altarItems` (so the UI reflects committed state)
5. Update `committedList` snapshot

**Discard Logic:**
1. Clear `altarItems`
2. Rebuild from `committedList` (only apps still installed)

---

### 5.4 `AppInfoData.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/AppInfoData.kt`

```kotlin
data class AppInfoData(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)
```

Simple data class representing a launchable app with its display name, package identifier, and launcher icon drawable.

---

### 5.5 `ActiveShortcutsAdapter.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ActiveShortcutsAdapter.kt`
**Extends:** `RecyclerView.Adapter<AltarViewHolder>`

**Purpose:** RecyclerView adapter for the **Altar** (top 30% of Shortcuts screen). Displays the current working set of shortcuts with drag handles and selection checkboxes.

**ViewHolder:** `AltarViewHolder`
| Field | Type | Layout ID |
|---|---|---|
| `ivIcon` | ImageView | `R.id.ivAltarIcon` |
| `tvName` | TextView | `R.id.tvAltarName` |
| `tvOrder` | TextView | `R.id.tvOrderNumber` |
| `cbSelect` | CheckBox | `R.id.cbAltarSelect` |
| `dragHandle` | ImageView | `R.id.ivDragHandle` |

**Binding:**
- Order number shown as `position + 1`
- App name and icon from `stateManager.getAppInfo(packageName)`
- Checkbox reflects `isSelected`; toggling invokes `onToggleSelection(position)`
- Unselected items alpha = 0.5, selected items alpha = 1.0

**Drag Support:** `onItemMove(fromPos, toPos)` delegates to `stateManager.moveAltarItem()` and calls `notifyItemMoved()`.

---

### 5.6 `AvailableAppsAdapter.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/AvailableAppsAdapter.kt`
**Extends:** `RecyclerView.Adapter<ArchiveViewHolder>`

**Purpose:** RecyclerView adapter for the **Archives** (bottom 60% of Shortcuts screen). Shows all installed launchable apps with checkboxes.

**ViewHolder:** `ArchiveViewHolder`
| Field | Type | Layout ID |
|---|---|---|
| `ivIcon` | ImageView | `R.id.ivArchiveIcon` |
| `tvName` | TextView | `R.id.tvArchiveName` |
| `cbSelect` | CheckBox | `R.id.cbArchiveSelect` |

**Binding:**
- Renders from `stateManager.allApps` (immutable full list)
- Checked state determined by `stateManager.isActiveShortcut(packageName)`
- Listener nulled before setting checked to avoid rebind loops
- On toggle → callback `onToggle(packageName, checked)`

---

### 5.7 `ShortcutDragCallback.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ShortcutDragCallback.kt`
**Extends:** `ItemTouchHelper.Callback()`

**Purpose:** Enables long-press drag-to-reorder in the Altar RecyclerView.

**Configuration:**
- Drag directions: UP and DOWN
- Swipe: disabled (removal via checkbox)
- Long-press drag: enabled

**Visual Feedback:**
- During drag (`ACTION_STATE_DRAG`): scale to 1.05× over 150ms
- On idle/release: scale back to 1.0× over 150ms

---

### 5.8 `ArcSliverView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/ArcSliverView.kt`
**Extends:** `View`

**Purpose:** Custom View rendering the edge-sliver — a single continuous shape with two sharp angular fang protrusions on the inward-facing edge. **As of v1.3.5 it is config-driven:** it takes a `SliverConfig` (constructor param), builds its path via the shared `SliverShape.buildPath(...)` (no longer hardcoded here), and fills with `config.fillColor()` (color × opacity). Size comes from `config.widthDp/heightDp`. Still straight `lineTo` geometry — no curves, no glow.

**Visual Design (built by `SliverShape` from the config knobs — defaults reproduce the original shape):**
- **Spine:** flat line on the screen-edge side (`u = 0`), full height.
- **Top fang / gums / bottom fang:** two inward tips with a central gums bridge; the eight fang knobs
  (thickness ×2, length ×2, tipY ×2, gums depth, gap) place the 8 path vertices. See `Docs/SliverAnatomy.md`.
- **Fill:** `config.fillColor()` — default is 50% grey (`#808080` @ opacity 0.5 = `#80808080`); custom is a hue.
- **No glow, no border, no pulse animation.**
- **View dimensions:** `config.widthDp × config.heightDp` (default 27dp × 38dp).

**Swipe Detection:**
- Tracks `rawX`/`rawY` (screen coordinates)
- Swipe threshold: >30px horizontal delta from start
- Max vertical deviation: <150px
- Direction: swiping INWARD from the edge (leftward for right-side sliver, rightward for left-side sliver)

**Lifecycle:**
- `onSizeChanged()`: rebuilds the fang path (via `SliverShape`) whenever the view is measured
- `applyConfig(newConfig, newSide)`: updates appearance/geometry in place — recolors the paint, rebuilds the path at the current size, `requestLayout()` + `invalidate()`. Used by the service's in-place hot-reload.
- System gesture exclusion: set via `SidebarService.assembleSliverView()` using `systemGestureExclusionRects` (API 29+)

---

### 5.9 `PositioningView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/PositioningView.kt`
**Extends:** `View`

**Purpose:** Custom View that renders a scaled-down phone mockup with a draggable sliver preview. Used on the Positioning configuration screen.

**Visual Components:**
1. **Phone Mockup:** Dark marble slab (#1A2822) with rounded corners (4% of mockup width), bordered with Faded Olive Teal (#3B5249), 3px stroke
2. **Restricted Zones:** Top 10% and bottom 10% crosshatched (Faded Olive Teal at ~30% opacity, 1.5px lines, 12px spacing) — sliver cannot be placed here
3. **Sliver Preview:** Miniature fang path built by the shared `SliverShape.buildPath(...)` and filled with `sliverConfig.fillColor()`, so it mirrors the live sliver's color/opacity/geometry. `setSliverConfig(cfg)` updates it (called on screen init with the saved config, and after Customize → Apply); the preview's width/height reflect the config's aspect
4. **Particle Trail:** Tarnished Silver particles (semi-transparent, alpha 120) that trail behind the sliver while dragging or snapping
5. **Instruction Text:** "Drag the sliver to reposition" shown when idle and trail is empty

**Geometry:**
- Mockup width: 55% of view width
- Aspect ratio: 2.1:1 (height/width)
- Valid Y zone: middle 80% of mockup height
- Sliver preview: 4% of mockup width × 9% of mockup height

**Touch Handling:**
- `ACTION_DOWN`: Check if touch is within 40px of the sliver preview (generous touch target)
- `ACTION_MOVE`: Clamp Y to valid zone, spawn trail particles every 30ms
- `ACTION_UP`: Determine nearest edge (midpoint threshold), animate snap to that edge with DecelerateInterpolator over 200ms

**Snap Animation:**
- ValueAnimator from current X to target X
- On animation end: finalize side, recalculate `yBias`, clear trail, invoke `onPositionChanged` callback

**Particle System:**
- Max 20 particles in trail
- Each particle has random velocity (±2px/frame), fading alpha (0.03 per frame), shrinking radius (×0.96 per frame)
- Removed when alpha ≤ 0

**Callback:** `onPositionChanged: ((ArcSliverView.Side, Float) -> Unit)?`

---

### 5.10 `DustParticleView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/DustParticleView.kt`
**Extends:** `View`

**Purpose:** A subtle dust-particle overlay that bursts particles when stone buttons are pressed.

**Visual Design:**
- Particle color: Aged Marble (#F5EFE6) at alpha 200
- Particles spawn from center of view (±10px X, ±5px Y jitter)
- Random radial velocities (1.5–4.5 px/frame) with slight upward bias
- Gravity effect: vy increases by 0.5 px/frame each tick
- Life: 0.4–0.7 seconds
- Fade: alpha decreases proportionally to remaining life
- Radius: shrinks by ×0.98 per frame

**Animation:**
- 600ms ValueAnimator running at 60fps assumption (dt = 0.016s)
- Runs only while particles exist; auto-stops when all faded

**Public API:** `burst(count: Int = 6)` — spawns `count` particles at random angles

---

### 5.11 `SliverConfig.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SliverConfig.kt`
**Type:** `data class`

**Purpose:** The single model for the sliver's user-editable appearance/geometry, persisted in `EdgeCasePrefs`.
Every default reproduces the original hardcoded look, so nothing changes visually until the user edits something.

**Fields & defaults:** `opacity` (0.5), `colorMode` (`DEFAULT`/`CUSTOM`), `customHue` (210),
`tooth1/2Thickness` (0.114/0.113), `tooth1/2Length` (0.60), `tooth1/2TipY` (0.20/0.80), `gumsDepth` (0.07),
`gap` (0.44), `widthDp`/`heightDp` (27/38). Geometry values use the normalized fang model in `Docs/SliverAnatomy.md`.

**Helpers:**
- `baseColor()` → opaque grey `#808080` (DEFAULT) or `Color.HSVToColor(hue,1,1)` (CUSTOM)
- `fillColor()` → base color with alpha = `opacity·255` (so opacity 0 = fully transparent)
- `save(context)` and companion `load(context)` — read/write the 13 `sliver_*` prefs keys
- data-class `copy()` — used by the dialog for a discardable working copy

---

### 5.12 `SliverShape.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SliverShape.kt`
**Type:** `object` (singleton)

**Purpose:** The single source of truth for the fang path. `buildPath(path, w, h, side, cfg)` computes the 8
`(u,v)` vertices from the config knobs and writes them into `path`. All renderers (`ArcSliverView` live overlay,
`PositioningView` and `SliverPreviewView` previews) call this — eliminating the former four hardcoded L/R copies.

**Model:** `u` = inward depth (0 = flat spine at the screen edge → 1 = deepest inward reach), `v` = vertical
(0 top → 1 bottom). Per side: `RIGHT → x = w·(1−u)`, `LEFT → x = w·u`, both `y = h·v`. Vertices:
`v3/v4 = 0.5 ∓ gap/2` (gums span), `v1 = v3 − tooth1Thickness`, `v6 = v4 + tooth2Thickness`, tips at
`(length, tipY)`, gums wall at `u = gumsDepth`. All coordinates are coerced into `[0,1]` with preserved order,
so no knob combination can invert the shape.

---

### 5.13 `SliverCustomizeDialog.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SliverCustomizeDialog.kt`

**Purpose:** Controller for the **Customize Sliver** popup (an AppCompat `AlertDialog` hosting
`dialog_customize_sliver.xml`, sized to 92% of screen width). Edits a working copy of `SliverConfig` with a
live preview.

**Structure & controls:**
- Live `SliverPreviewView` at top (mirrors the saved edge side).
- **Opacity** slider (0–100%).
- **Color**: radio toggle *Default grey* / *Custom*; Custom reveals a **rainbow hue** `SeekBar`
  (track painted with a `GradientDrawable` hue spectrum) + a live swatch.
- **Fang geometry** sliders (`LabeledSeekBar`): top/bottom thickness, top/bottom length, top/bottom angle
  (tipY), gums depth, gap. Ranges are clamped (e.g. thickness 0.02–0.35, length 0.10–0.95, tipY1 0.02–0.48 /
  tipY2 0.52–0.98, gums 0–0.60, gap 0.05–0.90).
- **Size**: two numeric `EditText` for width (8–160dp) / height (12–240dp).
- **Footer**: Reset / Cancel / Apply.

Every control writes into the working config and calls `preview.setConfig(...)`. **Apply** persists via
`working.save(context)` and calls back `onApplied(working)` (MainActivity refreshes the preview + sends
`ACTION_UPDATE_STYLE`). **Reset** restores defaults (persist only on Apply). **Cancel** discards.

---

### 5.14 `SliverPreviewView.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/SliverPreviewView.kt`
**Extends:** `View`

**Purpose:** A static, scaled preview of the sliver fang for the Customize dialog. Draws `SliverShape.buildPath`
centered and scaled to fit (preserving the config's H/W aspect), filled with `config.fillColor()`, mirroring the
saved edge `side`. `setConfig(cfg)` updates it (invalidate) for instant feedback while sliders move.

---

### 5.15 `LabeledSeekBar.kt`

**Path:** `app/src/main/java/com/dicereligion/edgecase/LabeledSeekBar.kt`
**Extends:** `LinearLayout` (compound view, declared in XML)

**Purpose:** A reusable control row — `label | ——slider—— | value` — used for every slider in the Customize
dialog. `configure(label, min, max, value, formatter, onChange)` maps the SeekBar's `0..1000` progress onto a
float `[min,max]` and reports user-driven changes; `setValue()` updates without firing the callback (for Reset).
`seek()` exposes the underlying `SeekBar` (used to paint the hue-spectrum track).

---

## 6. Resources — Complete Reference

### 6.1 Layouts

#### `activity_main.xml`
Root layout: `FrameLayout` with `abyssal_teal` background, `fitsSystemWindows="true"`. Includes three child screens via `<include>`:
- `@+id/screenMainMenu` → `layout_screen_main_menu` (visible by default)
- `@+id/screenShortcuts` → `layout_screen_shortcuts_container` (gone)
- `@+id/screenPositioning` → `layout_screen_positioning_container` (gone)

#### `layout_screen_main_menu.xml`
Themed with `bg_stone_texture` background. Contains:
- Left and right decorative pillars (0.5 alpha) via `ic_pillar_left`/`ic_pillar_right`, `fitXY` scale
- `dustContainer` FrameLayout (clipChildren=false) for particle overlay
- Header bar: `bg_dark_seaweed_panel` background, "EDGECASE" engraved text (18sp, serif, all-caps)
- 5 stone buttons in a centered vertical LinearLayout:
  1. `btnShortcuts` — "SHORTCUTS"
  2. `btnPosition` — "POSITION"
  3. `btnDummy` — "DUMMY"
  4. Spear divider (`ic_divider_spear`) between dummy and service buttons
  5. `btnStartService` — "START EDGE SERVICE" (14sp, letterSpacing 0.04)
  6. `btnStopService` — "STOP SERVICE"
- Version label: `TextView` at bottom-left (layout_gravity `start|bottom`), "v1.3.5", CaptionSerif style, tarnished_silver color, 0.6 alpha

#### `layout_screen_shortcuts_container.xml`
Themed with `bg_stone_texture` background, left/right pillars at 0.5 alpha. Contains:
- Header: silver ring icon + "SHORTCUTS" engraved text
- "CURRENT SHORTCUTS" caption (CaptionSerif, all-caps, letterSpacing 0.1)
- Altar section (layout_weight=0.38): `bg_dark_seaweed_panel` FrameLayout containing:
  - `rvAltarShortcuts` RecyclerView
  - `tvAltarEmpty` centered TextView ("No shortcuts selected", gone by default)
- Spear divider
- "AVAILABLE APPS" caption
- `rvArchiveApps` RecyclerView (layout_weight=0.42)
- Action bar (layout_weight=0.10): `btnBackToMenu` (BACK) + `btnSaveShortcuts` (SAVE)

#### `layout_screen_positioning_container.xml`
Themed with `bg_stone_texture` background, left/right pillars. Contains:
- Header: "SLIVER POSITION" engraved text
- `PositioningView` (custom view, id=`positioningView`, layout_weight=1)
- Footer (vertical): `tvPositionInfo` (full-width readout) on top, then an action row with two equal-weight
  stone buttons — `btnCustomizeSliver` ("CUSTOMIZE") on the left and `btnBackToMenuFromPosition` ("BACK") on
  the right (restructured from the old single-row info + 120dp Back to fit the new button)

#### `dialog_customize_sliver.xml` (Customize Sliver popup)
Stone-textured vertical panel (used by `SliverCustomizeDialog`). Contains: an "CUSTOMIZE SLIVER" engraved
title; a framed `SliverPreviewView` (110dp) live preview; a 320dp `ScrollView` with an **APPEARANCE** section
(opacity `LabeledSeekBar`, a Default/Custom color `RadioGroup`, and a hue `LabeledSeekBar` + swatch shown when
Custom), a **FANG GEOMETRY** section (eight `LabeledSeekBar`s), and a **SIZE (DP)** row (width/height
`EditText`s); and a footer with RESET / CANCEL / APPLY stone buttons. Section dividers use `ic_divider_spear`.

#### `layout_item_shortcut_tile.xml` (Altar item row)
`FrameLayout` with translucent temple sandstone (#33D4C4A8) tile background. Contains horizontal LinearLayout:
- `ivDragHandle`: 24dp × 24dp, `@android:drawable/ic_menu_sort_by_size`, 0.5 alpha
- `tvOrderNumber`: 24dp × 24dp centered, 13sp, serpent_emerald, serif bold
- App icon in `ic_silver_ring` FrameLayout: 52dp × 52dp oval with tarnished_silver 2dp stroke, inner ImageView 44dp × 44dp
- `tvAltarName`: weight=1, BodySerif, single line ellipsized
- `cbAltarSelect`: serpent_emerald tint

#### `layout_item_available_app.xml` (Archive item row)
Horizontal LinearLayout, `dark_seaweed` background, 12dp padding:
- `ivArchiveIcon`: `app_icon_size` (48dp)
- `tvArchiveName`: weight=1, BodySerif, single line ellipsized, 16dp start margin
- `cbArchiveSelect`: serpent_emerald tint

---

### 6.2 Drawables

| File | Type | Description |
|---|---|---|
| `bg_stone_texture.xml` | layer-list | Abyssal Teal base + radial gradient vignette (#00000000 → #33000000) |
| `bg_dark_seaweed_panel.xml` | shape (rectangle) | Dark Seaweed at ~70% opacity (#B3122A23), 2dp Faded Olive Teal stroke, 4dp corners |
| `bg_stone_button.xml` | layer-list | Temple Sandstone body + bottom bevel (Sandstone Bevel #A39171, -4dp top inset) for 3D depth |
| `bg_stone_button_pressed.xml` | shape (rectangle) | Sandstone Pressed (#BEB09A) solid, no corners |
| `selector_stone_button.xml` | selector | Pressed → `bg_stone_button_pressed`, default → `bg_stone_button` |
| `ic_meander_border.xml` | vector | Greek meander (key) pattern, 32×1280 viewport, repeating every 64px, tarnished_silver |
| `ic_pillar_left.xml` | vector | Doric column: capital (#3B5249), shaft (#1A3328), left shadow (#122A23), 4 flute lines (#0F221B), base (#3B5249 + #2A3E34 shadow), 64×1280 viewport |
| `ic_pillar_right.xml` | vector | Mirror of left pillar — shadow on right side, fluting identical |
| `ic_divider_spear.xml` | vector | Tarnished Silver diamond-tapered spear (360×4 viewport) with Serpent Emerald central rivet |
| `ic_silver_ring.xml` | shape (oval) | 52dp × 52dp oval, abyssal_teal fill, 2dp tarnished_silver stroke |
| `ic_check_rune.xml` | vector | Serpent Emerald checkmark, 3dp strokes with round caps/joins, 24×24 viewport |
| `ic_launcher_background.xml` | vector | Transparent (#00000000) — icon fills the launcher shape without a dark border |
| `ic_launcher_foreground.xml` | inset | 0dp inset wrapping `@drawable/icon_round` — icon fills the full 108dp viewport |
| `icon_round.png` | PNG | 512×512 RGBA PNG, the app's circular icon, placed in drawable for adaptive icon foreground |

---

### 6.3 Values

#### Colors (`colors.xml`)

**Hellenic Serpent Palette:**

| Name | Hex | Role |
|---|---|---|
| `abyssal_teal` | #071A15 | Root background, darkest |
| `dark_seaweed` | #122A23 | Panel backgrounds, archive items |
| `faded_olive_teal` | #3B5249 | Borders, strokes, pillar capitals |
| `temple_sandstone` | #D4C4A8 | Stone button body |
| `aged_marble` | #F5EFE6 | Primary text, dust particles |
| `serpent_emerald` | #2E8B57 | Accent, checkmarks, active indicators |
| `tarnished_silver` | #9AA0A6 | Secondary text, borders, meander, icon rings |
| `ethereal_pink` | #4DFFC0CB | Unused (was outer arc glow) |

**Stone Button Shades:**

| Name | Hex | Role |
|---|---|---|
| `sandstone_bevel` | #A39171 | Button bottom bevel (3D depth) |
| `sandstone_pressed` | #BEB09A | Button pressed state |
| `text_engraved` | #071A15 | Button text (= abyssal_teal) |

**Panel Background:**
| `panel_dark_seaweed_bg` | #B3122A23 | Dark seaweed at ~70% opacity |

**Legacy Colors (unused, kept for compatibility):** `purple_200`, `purple_500`, `purple_700`, `teal_200`, `teal_700`, `black`, `white`

#### Dimensions (`dimens.xml`)

| Name | Value | Use |
|---|---|---|
| `margin_standard` | 16dp | General spacing |
| `margin_wide` | 24dp | Wide spacing, between buttons |
| `margin_narrow` | 8dp | Tight spacing |
| `padding_standard` | 16dp | Standard padding |
| `stone_button_height` | 56dp | Button height |
| `stone_button_elevation` | 8dp | Button elevation |
| `stone_button_pressed_translation` | 4dp | Press-down translation |
| `app_icon_size` | 48dp | App icon width/height |
| `pillar_width` | 32dp | Decorative pillar width |
| `sliver_fang_width` | 27dp | Fang sliver overlay width |
| `sliver_fang_height` | 38dp | Fang sliver overlay height |
| `tray_width` | 80dp | Tray overlay width |
| `text_header` | 18sp | Header text |
| `text_body` | 16sp | Body text |
| `text_caption` | 14sp | Caption text |

#### Strings (`strings.xml`)

Only one string: `app_name` = `"EdgeCase"`. All UI labels (including the Customize dialog's title, control
labels, and buttons) are inline literals in the layouts/code, consistent with the rest of the app.

#### Styles (`styles.xml`)

| Style | Parent | Key Properties |
|---|---|---|
| `EngravedHeader` | TextAppearance.AppCompat | serif, all-caps, letterSpacing 0.08, aged_marble color, black shadow (0,1,1) |
| `EngravedHeaderDark` | TextAppearance.AppCompat | serif, all-caps, letterSpacing 0.08, text_engraved color, white shadow (0,-1,1) |
| `BodySerif` | TextAppearance.AppCompat | serif, aged_marble color |
| `CaptionSerif` | TextAppearance.AppCompat | serif, tarnished_silver, text_caption size |
| `StoneButtonText` | TextAppearance.AppCompat | serif, all-caps, letterSpacing 0.08, text_engraved, text_header size |

#### Themes (`themes.xml`)

```xml
<style name="Theme.EdgeCase" parent="Theme.AppCompat.DayNight.NoActionBar" />
```

No custom theme attributes defined — relies entirely on AppCompat DayNight with NoActionBar.

---

### 6.4 Mipmaps & App Icon

**Source:** `icon_round.png` — 512×512 RGBA PNG with alpha transparency, sRGB color profile. Located in `res/drawable/` for adaptive icon use.

**Adaptive Icons (API 26+):**

| File | Content |
|---|---|
| `mipmap-anydpi/ic_launcher.xml` | `<adaptive-icon>` with background = `@drawable/ic_launcher_background` (transparent #00000000), foreground = `@drawable/ic_launcher_foreground` (0dp-inset icon_round.png) |
| `mipmap-anydpi/ic_launcher_round.xml` | Identical to `ic_launcher.xml` |

**Raster Fallbacks (pre-API 26):**

| Density | Size | File |
|---|---|---|
| mdpi | 48×48 | `mipmap-mdpi/ic_launcher.png`, `mipmap-mdpi/ic_launcher_round.png` |
| hdpi | 72×72 | `mipmap-hdpi/ic_launcher.png`, `mipmap-hdpi/ic_launcher_round.png` |
| xhdpi | 96×96 | `mipmap-xhdpi/ic_launcher.png`, `mipmap-xhdpi/ic_launcher_round.png` |
| xxhdpi | 144×144 | `mipmap-xxhdpi/ic_launcher.png`, `mipmap-xxhdpi/ic_launcher_round.png` |
| xxxhdpi | 192×192 | `mipmap-xxxhdpi/ic_launcher.png`, `mipmap-xxxhdpi/ic_launcher_round.png` |

All raster fallbacks are the same circular icon (resized from the 512×512 source), so the icon appears correctly in both standard and circular launcher icon modes (the `manifest` sets `android:roundIcon="@mipmap/ic_launcher_round"`).

---

### 6.5 XML Configuration

#### `AndroidManifest.xml`

```xml
<manifest xmlns:android="..."
          xmlns:tools="...">

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
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="EdgeCase"
        android:theme="@style/Theme.EdgeCase"
        ...>

        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".SidebarService"
            android:foregroundServiceType="specialUse"
            android:exported="false" />
    </application>
</manifest>
```

#### `backup_rules.xml` & `data_extraction_rules.xml`
Both are stock/default — no custom backup or data extraction rules have been configured.

#### `proguard-rules.pro`
Stock file with commented-out examples; no custom rules active. Minification is disabled in build.gradle.kts.

---

## 7. Feature Inventory

### Implemented Features

| # | Feature | Status | Location |
|---|---|---|---|
| 1 | Three-screen navigation (Main Menu / Shortcuts / Positioning) | ✅ Complete | `MainActivity.kt` |
| 2 | Stone button press animation (translationY down) | ✅ Complete | `MainActivity.applyStoneButtonBehavior()` |
| 3 | Haptic feedback on button press (30ms, amplitude 255) | ✅ Complete | `MainActivity.triggerHaptic()` |
| 4 | Dust particle burst on button press | ✅ Complete | `DustParticleView.kt` |
| 5 | App listing — all installed launchable apps | ✅ Complete | `MainActivity.getInstalledApps()` |
| 6 | Bipartite shortcut editor (Altar + Archives dual-list) | ✅ Complete | `MainActivity.initShortcutsScreen()` |
| 7 | Drag-to-reorder in Altar via ItemTouchHelper | ✅ Complete | `ShortcutDragCallback.kt` |
| 8 | Checkbox toggle to add/remove from Altar | ✅ Complete | `AvailableAppsAdapter`, `ActiveShortcutsAdapter` |
| 9 | Select/deselect within Altar (dim unselected, evict on Save) | ✅ Complete | `ShortcutStateManager` |
| 10 | Discard confirmation dialog on unsaved changes | ✅ Complete | `MainActivity.showDiscardDialog()` |
| 11 | Save shortcuts to SharedPreferences (ordered CSV) | ✅ Complete | `ShortcutStateManager.commit()` |
| 12 | Persistent foreground service (SidebarService) | ✅ Complete | `SidebarService.kt` |
| 13 | Foreground notification (EdgeCase Active, priority LOW) | ✅ Complete | `SidebarService.buildSystemNotification()` |
| 14 | Edge sliver overlay (floating window, TYPE_APPLICATION_OVERLAY) | ✅ Complete | `SidebarService.assembleSliverView()` |
| 15 | Fang-path rendering (two protrusions, central recess, #80808080 fill) | ✅ Complete | `ArcSliverView.kt` |
| 16 | Swipe gesture detection (inward from edge) | ✅ Complete | `ArcSliverView.onTouchEvent()` |
| 17 | Desaturated shortcut icons in tray (20% desaturation) | ✅ Complete | `SidebarService.desaturateIcon()` |
| 18 | Tray unfurl animation (scaleX 0→1 at edge pivot) | ✅ Complete | `SidebarService.transitionToExpandedTray()` |
| 19 | App launch from tray with haptic feedback | ✅ Complete | `SidebarService.populateShortcuts()` |
| 20 | Positioning screen with draggable sliver preview on phone mockup | ✅ Complete | `PositioningView.kt` |
| 21 | Restricted zones (top/bottom 10% crosshatched, cannot place sliver) | ✅ Complete | `PositioningView.drawCrosshatchZone()` |
| 22 | Snap-to-edge animation with particle trail | ✅ Complete | `PositioningView.snapSliverTo()` |
| 23 | Live position persistence (saved immediately on drag release) | ✅ Complete | `PositioningView.onPositionChanged` → prefs |
| 24 | Left/right side support for sliver | ✅ Complete | `ArcSliverView.Side.LEFT/RIGHT` |
| 25 | System gesture exclusion for sliver touch area (API 29+) | ✅ Complete | `SidebarService.assembleSliverView()` |
| 26 | Hot-reload shortcuts in running service | ✅ Complete | `ACTION_UPDATE_SHORTCUTS` intent |
| 27 | Hot-reload position in running service | ✅ Complete | `ACTION_UPDATE_POSITION` intent |
| 28 | Overlay permission check + redirect to settings | ✅ Complete | `MainActivity.checkAndRequestPermissions()` |
| 29 | Battery optimization exemption request | ✅ Complete | `MainActivity.checkAndRequestPermissions()` |
| 30 | Custom adaptive app icon (transparent background, 0dp inset) | ✅ Complete | See §6.4 |
| 31 | Tray dismiss on outside touch | ✅ Complete | `SidebarService.trayView.setOnTouchListener()` |
| 32 | Idempotent sliver/tray add/remove guards | ✅ Complete | `sliverAdded` flag + isAttachedToWindow checks |
| 33 | Dirty-state tracking for discard prompts | ✅ Complete | `ShortcutStateManager.isDirty()` |
| 34 | Sticky service restart on kill | ✅ Complete | `START_STICKY` return value |
| 35 | **Customize Sliver** dialog on the Position screen | ✅ Complete — 2026-07-10 | `SliverCustomizeDialog.kt`, `dialog_customize_sliver.xml` |
| 36 | Configurable sliver **opacity** (0–100%) | ✅ Complete — 2026-07-10 | `SliverConfig.opacity` / `fillColor()` |
| 37 | Configurable sliver **color** (default grey or custom hue via rainbow slider) | ✅ Complete — 2026-07-10 | `SliverConfig.colorMode/customHue`, `SliverCustomizeDialog` |
| 38 | Configurable **fang geometry** (per-fang thickness / length / angle, gums depth, gap) | ✅ Complete — 2026-07-10 | `SliverConfig` + `SliverShape` |
| 39 | Configurable sliver **size** (width/height dp) | ✅ Complete — 2026-07-10 | `SliverConfig.widthDp/heightDp` |
| 40 | **Live preview** in the dialog + on the Position screen | ✅ Complete — 2026-07-10 | `SliverPreviewView`, `PositioningView.setSliverConfig()` |
| 41 | Sliver style persistence (13 `sliver_*` prefs keys) | ✅ Complete — 2026-07-10 | `SliverConfig.save/load` |
| 42 | Hot-reload sliver style in running service | ✅ Complete — 2026-07-10 | `ACTION_UPDATE_STYLE` → `applySliverUpdate()` |
| 43 | Single-source fang geometry builder (removed 4× hardcoded path duplication) | ✅ Complete — 2026-07-10 | `SliverShape.buildPath()` |
| 44 | In-place overlay update (fixes stale-sliver / opacity-0 ghost bug) | ✅ Complete — 2026-07-10 | `SidebarService.applySliverUpdate()` + `ArcSliverView.applyConfig()` |

### Planned / Stub Features

| # | Feature | Status |
|---|---|---|
| 0 | Version display on home screen (bottom-left, now shows v1.3.5) | ✅ Complete — added 2026-06-20 |
| 1 | Dummy button (third menu option) | 🔶 Stub — shows Toast "Dummy — nothing here yet" |
| 2 | Monochrome themed icon support (Android 13+) | ❌ Removed — incompatible with raster PNG foreground |

---

## 8. Data Flow & State Management

### State Persistence Architecture

```
┌──────────────────────────────────────┐
│         SharedPreferences            │
│         "EdgeCasePrefs"              │
│                                      │
│  saved_shortcuts_order: "pkg1,pkg2"  │
│  saved_shortcuts: Set<String>        │
│  sliver_side: "left" | "right"       │
│  sliver_y_bias: Float (0.0–1.0)     │
│  sliver_opacity / _color_mode / _hue │
│  sliver_t1/t2_thickness/length/tipy  │
│  sliver_gums_depth / _gap            │
│  sliver_width_dp / _height_dp        │
└──────┬───────────────┬───────────────┘
       │               │
       ▼               ▼
┌─────────────┐  ┌─────────────┐
│ MainActivity│  │SidebarService│
│             │  │              │
│ Reads/writes│  │ Reads on     │
│ all keys    │  │ startup & hot│
│             │  │ -reload      │
└──────┬──────┘  └──────────────┘
       │
       ▼
┌──────────────────┐
│ShortcutStateManager│
│                    │
│ • Working set     │
│ • Dirty tracking  │
│ • Commit/Discard  │
└───────────────────┘
```

### Shortcut Editing Flow

1. User enters Shortcuts screen → `initShortcutsScreen()` called (once)
2. `ShortcutStateManager` loads `saved_shortcuts_order` from prefs → populates `committedList` and `altarItems`
3. Altar shows `altarItems` with checkboxes; Archives shows `allApps` with checkboxes
4. **Checkbox in Archives:** `setFromArchives(pkg, checked)` — immediately adds/removes from `altarItems`, both adapters refreshed
5. **Checkbox in Altar:** `toggleAltarSelection(pos)` — flips `isSelected`, item dims/un-dims, both adapters refreshed
6. **Drag in Altar:** `moveAltarItem(from, to)` — reorders `altarItems`, `notifyItemMoved`
7. **SAVE pressed:** `commit()` — writes selected packages (in order) to prefs, evicts unselected, sends `ACTION_UPDATE_SHORTCUTS` to service
8. **BACK pressed with dirty state:** `showDiscardDialog()` offers Discard or Keep Editing
9. **Discard:** `discard()` — resets `altarItems` to `committedList`
10. **Re-entry to Shortcuts screen:** `refreshShortcutsState()` — re-reads from prefs

### Positioning Flow

1. User enters Positioning screen → `initPositioningScreen()` called (once)
2. Reads `sliver_side` and `sliver_y_bias` from prefs → sets initial `PositioningView` state
3. User drags sliver preview on mockup → particle trail follows
4. On release → snap animation to nearest edge → `onPositionChanged` callback fires
5. Callback writes new side and yBias to prefs immediately
6. Sends `ACTION_UPDATE_POSITION` intent to service → service calls `applySliverUpdate()`, which updates the existing sliver overlay **in place** (no destroy/recreate)

### Sliver Customization Flow

1. Position screen → **CUSTOMIZE** → `MainActivity.openCustomizeSliverDialog()` loads the current `SliverConfig` and shows `SliverCustomizeDialog`
2. The dialog edits a **working copy**; every slider/field/color change updates the working config and the live `SliverPreviewView`
3. **Apply:** `working.save(context)` writes the 13 `sliver_*` keys; the callback applies the config to the Position screen's `PositioningView` preview and sends `ACTION_UPDATE_STYLE` to the service
4. Service `applySliverUpdate()` reloads the config and updates the live overlay in place (recolor + `updateViewLayout`)
5. **Cancel** discards the working copy; **Reset** restores defaults in the dialog (persisted only if Apply is then pressed)

### Service Lifecycle

1. User taps "START EDGE SERVICE" → `MainActivity.checkAndRequestPermissions()` → `startEdgeService()` → `startForegroundService(intent)`
2. `SidebarService.onCreate()`:
   - Loads position **and `SliverConfig`** from prefs
   - Builds foreground notification
   - Creates window parameters (TYPE_APPLICATION_OVERLAY; size from `config.widthDp/heightDp`)
   - Assembles sliver view (passing `config`) and tray view
   - Adds sliver to window if overlay permission granted
3. Swipe on sliver → `transitionToExpandedTray()` (remove sliver, add tray with animation)
4. Tap tray icon → launch app → `transitionToSliverState()` (remove tray, restore sliver)
5. Outside touch on tray → `transitionToSliverState()`
6. User taps "STOP SERVICE" → `stopService(intent)` → `onDestroy()` removes both windows
7. Service returns `START_STICKY` — restarts if killed

---

## 9. Permissions

| Permission | Purpose | Handling |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw overlay windows (Sliver & Tray) | Checked via `Settings.canDrawOverlays()`, redirect to settings if denied |
| `FOREGROUND_SERVICE` | Run persistent foreground service | Declared only |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Qualify foreground service type | Declared only |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent Doze from killing service | Checked via `PowerManager.isIgnoringBatteryOptimizations()`, redirect to settings |
| `VIBRATE` | Haptic feedback on button presses and swipes | Declared, no runtime check needed |

**Manifest `<queries>`:** Declares query for all apps with `MAIN`/`LAUNCHER` intent filter → enables `queryIntentActivities()` for app listing.

**Notification Channel:** `EdgeCaseEngineChannel` with `IMPORTANCE_LOW` — creates on API 26+ in `buildSystemNotification()`.

---

## 10. Known Limitations & Future Work

### Known Limitations

1. **No multi-density icon_round.png:** The source PNG is 512×512 and placed only in `res/drawable/` (not density-specific drawable folders). This is fine for the adaptive icon foreground (which scales within the 108dp viewport), but pre-API 26 fallbacks are hand-scaled PNGs — ideally these should be generated by Android Studio's Image Asset tool for optimal quality.

2. **Desaturation is in-memory only:** The 20% desaturation is applied to the Drawable each time `populateShortcuts()` is called. The color filter is set on the original Drawable object; subsequent calls will re-apply the filter on potentially already-filtered drawables. When the user presses an icon, `colorFilter = null` clears it, but this could cause side effects if the same Drawable instance is reused across multiple ImageViews.

3. **No clipboard/notification shortcuts:** The tray only shows app launch icons. There is no support for deep links, shortcuts (App Shortcuts / ShortcutManager), or custom actions.

4. **Dummy button:** The third main menu button ("DUMMY") does nothing useful — it's a stub for future functionality.

5. **Tray height = sliver height × 7 (266dp):** This is a fixed ratio. On very short screens (or in landscape), the tray may be taller than the screen → clipped. No dynamic height calculation based on screen size.

6. **No landscape support:** The sliver overlay is designed for portrait mode. The Y-bias positioning and tray layout assume portrait orientation.

7. **No multi-window / foldable handling:** No special handling for multi-window mode or foldable devices. The sliver will appear on whatever "edge" is available based on the current window bounds.

8. **No dark/light theme adapter:** The theme is `Theme.AppCompat.DayNight.NoActionBar` but no `values-night/` resources are provided. The Hellenic Serpent palette is dark-only — no light theme variant exists.

9. **Monochrome adaptive icon removed:** The `<monochrome>` tag was removed from the adaptive icon XMLs because it doesn't work well with raster PNG foregrounds. Android 13+ themed icons will fall back to the standard adaptive icon instead of displaying a monochrome variant.

10. **No backup configuration:** `backup_rules.xml` and `data_extraction_rules.xml` are stock templates — no specific backup rules are defined for the app's SharedPreferences.

### Potential Future Enhancements

- Implement the Dummy button functionality
- Add app-specific deep links or dynamic shortcuts in the tray
- Add landscape orientation support with adaptive tray sizing
- Create `values-night/` resources for a light theme variant
- Add monochrome vector drawable for Android 13+ themed icons
- Implement backup rules for `EdgeCasePrefs` (now includes the 13 `sliver_*` style keys)
- Add onboarding flow for first-time users (explain swipe gesture)
- ~~Add configurable sliver size~~ — **done** (v1.3.5 Customize dialog: size, color, opacity, fang geometry); tray width still fixed
- Add custom action shortcuts (not just app launches)
- Support for widget pinning in the tray

---

## Appendix: Quick Reference

### Key Files at a Glance

| File | Lines | Purpose |
|---|---|---|
| `SidebarService.kt` | 448 | Foreground service, overlay windows, tray, in-place style update |
| `MainActivity.kt` | 419 | Main UI, navigation, permissions, service control, Customize hook |
| `PositioningView.kt` | 411 | Phone mockup, draggable fang sliver, snap animation, particles, config preview |
| `SliverCustomizeDialog.kt` | 206 | "Customize Sliver" popup controller |
| `ShortcutStateManager.kt` | 163 | Bipartite shortcut state, persistence, dirty tracking |
| `ArcSliverView.kt` | 124 | Config-driven fang rendering (via SliverShape), swipe detection, `applyConfig` |
| `SliverConfig.kt` | 119 | Sliver appearance/geometry model + prefs I/O |
| `DustParticleView.kt` | 104 | Particle burst effect for button presses |
| `LabeledSeekBar.kt` | 94 | Reusable label+slider+value control row |
| `ShortcutDragCallback.kt` | 68 | Drag-to-reorder ItemTouchHelper |
| `ActiveShortcutsAdapter.kt` | 60 | Altar RecyclerView adapter |
| `SliverPreviewView.kt` | 53 | Live sliver preview for the Customize dialog |
| `AvailableAppsAdapter.kt` | 50 | Archives RecyclerView adapter |
| `SliverShape.kt` | 50 | Shared parametric fang-path builder |
| `AppInfoData.kt` | 9 | Data class for installed app info |

### Color Hex Quick Reference

| Name | Hex | Preview |
|---|---|---|
| `abyssal_teal` | #071A15 | Very dark teal |
| `dark_seaweed` | #122A23 | Dark green-black |
| `faded_olive_teal` | #3B5249 | Muted olive-teal |
| `temple_sandstone` | #D4C4A8 | Warm sand |
| `aged_marble` | #F5EFE6 | Off-white |
| `serpent_emerald` | #2E8B57 | Emerald green |
| `tarnished_silver` | #9AA0A6 | Cool gray |
| `ethereal_pink` | #4DFFC0CB | Pale pink (with alpha) |
| `sandstone_bevel` | #A39171 | Muted brown-gray |
| `sandstone_pressed` | #BEB09A | Warm gray-tan |

---

*Document generated from complete source tree analysis on 2026-06-20. Updated 2026-07-06 to reflect: fang sliver redesign (angular straight-line path, no glow/border, #80808080 fill), transparent adaptive icon background, and updated line counts. Updated 2026-07-10 (v1.3.5) to reflect: the **Sliver Customize** feature (opacity, color/hue, per-fang geometry, size — via `SliverConfig` + `SliverCustomizeDialog`), the shared `SliverShape` path builder replacing four hardcoded copies, `ACTION_UPDATE_STYLE` hot-reload, the in-place overlay update (`applySliverUpdate` / `ArcSliverView.applyConfig`) that fixed the stale-sliver / 0%-opacity ghost bug, five new source files, `dialog_customize_sliver.xml`, the 13 new `sliver_*` prefs keys, and updated line counts.*

---

## Appendix B: Related Documents

| Document | Purpose |
|---|---|
| `Docs/Dimensions.md` | Stable ID/dimension addressing for every page/element (incl. the `POSITION.btnCustomize` button and `DIALOG.customizeSliver`); §6 covers the sliver anatomy + tuning knobs |
| `Docs/SliverAnatomy.md` | Deep-dive on the fang geometry: named parts, vertices, tuning knobs, and how they map to `SliverConfig`/`SliverShape` |
| `Docs/Publisher.md` | Google Play publication roadmap / pre-launch checklist |
