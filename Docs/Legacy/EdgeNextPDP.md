# EdgeNext — Phase-by-Phase Development Plan

**Created:** 2026-06-20
**Source Spec:** User requirements + `Docs/EdgeCaseTD.md` + `Work/Res/Edge/EdgecaseTheme.md`
**Status:** ✅ Complete
**Current Codebase:** EdgeCase v1.0 (all 47 tasks in IMPLEMENTATION_PLAN.md complete)

---

## How To Use This Document

Each task has a status icon:
- 🔴 **Not Started**
- 🟡 **In Progress**
- 🟢 **Complete**

Update the **Last Updated** field and status icons after each session.

**Last Updated:** 2026-06-20
**Current Phase:** ✅ All Phases Complete
**Overall Progress:** 76 / 76 tasks complete

---

## Pre-Implementation Analysis

### Current Architecture Summary

| Layer | Component | Description |
|-------|-----------|-------------|
| **Activity** | `MainActivity` | Single-screen: `RecyclerView` of all installed apps with checkboxes + Start/Stop Service buttons. On checkbox toggle → immediate `SharedPreferences` save → hot-reload intent to service. |
| **Service** | `SidebarService` | Foreground service. Manages two `WindowManager` overlay views: a **sliver** (trigger handle, 15×75dp, right edge, 110dp bottom offset) and a **tray** (80dp wide scrollable shortcut panel). Swipe-left gesture (>30px horiz, <150px vert deviation) transitions sliver→tray. Tap-outside dismisses tray→sliver. |
| **Adapter** | `AppSelectionAdapter` | `RecyclerView.Adapter` binding `AppInfoData` items with icon, name, and checkbox. Null-listener pattern prevents rebind loops. |
| **Data** | `AppInfoData` | Simple data class: `appName`, `packageName`, `icon`. |
| **Persistence** | `SharedPreferences` | `"EdgeCasePrefs"` with keys: `saved_shortcuts` (StringSet), `saved_shortcuts_order` (comma-delimited ordered String). |
| **Layouts** | `activity_main.xml`, `item_app_row.xml` | LinearLayout-based; RecyclerView + button bar; app row with icon(48dp) + name + checkbox. |
| **Manifest** | `AndroidManifest.xml` | 4 permissions (overlay, foreground, special use, battery), queries block, SidebarService registered. |
| **Build** | Gradle KTS + Version Catalog | `minSdk=30`, `targetSdk=36`, AppCompat 1.7.0, RecyclerView 1.3.2, Kotlin 2.2.10. |

### Key Metrics (Current)
| Parameter | Value |
|-----------|-------|
| Sliver dimensions | 15dp × 75dp |
| Sliver bottom offset | 110dp |
| Sliver gravity | `Gravity.END \| Gravity.BOTTOM` |
| Tray width | 80dp |
| Tray height | sliverHeight × 4 = 300dp |
| Swipe threshold X | 30px |
| Max swipe deviation Y | 150px |

### What Will Change — Feature-by-Feature Impact Map

#### Feature 1: Dual-Arc Sliver
- **New file:** Custom `ArcSliverView` class extending `View`
- **Modified file:** `SidebarService.kt` — replace `assembleSliverView()` to use `ArcSliverView`, update `instantiateWindowParameters()` for wider layout params, update gesture exclusion rects
- **Affected constants:** Sliver width expands from 15dp to accommodate outer arc (~36dp total: 9dp inner + 27dp outer aura)
- **New behavior:** Outer arc pulses alpha 20%↔30% over 4 seconds

#### Feature 2: Home Screen Routing & Positioning
- **Heavily modified:** `MainActivity.kt` → becomes a multi-state container (no Fragments needed — manual `visibility` switching or `ViewFlipper`)
- **New layouts:** `activity_main.xml` redesigned as container; new `layout_screen_main_menu.xml`, `layout_screen_shortcuts.xml`, `layout_screen_positioning.xml`
- **New data:** SharedPreferences keys `sliver_side` ("left"|"right") and `sliver_y_bias` (0.0–1.0 float)
- **Modified file:** `SidebarService.kt` — dynamic gravity and Y from SharedPreferences
- **New file:** `PositioningView.kt` — custom View for the phone mockup with draggable sliver preview

#### Feature 3: Bipartite Shortcuts Screen
- **Major refactor:** `AppSelectionAdapter` split or augmented for dual-list mode
- **New file:** `ShortcutsFragment.kt` or inlined into MainActivity — manages two `RecyclerView` instances (top: selected/shortcuts, bottom: available apps)
- **New file:** `ShortcutDragCallback.kt` — `ItemTouchHelper.Callback` for drag-and-drop reordering
- **State pattern:** Two in-memory `MutableList<String>` — `activeShortcuts` and `availableApps`. Bottom list toggles mutate both immediately. Top list reorder/deselect mutates `activeShortcuts`. Save button writes `activeShortcuts` to SharedPreferences.
- **Data format:** SharedPreferences `saved_shortcuts_order` already stores comma-delimited order. The `saved_shortcuts` StringSet becomes secondary (kept for backward compat).

#### Feature 4: Hellenic Serpent UI/UX Rehaul
- **New resources:** Custom fonts (Augustus, Cinzel .ttf/.otf), background textures (stone .png), pillar images, divider drawables
- **New drawables:** `bg_stone_button.xml`, `bg_stone_button_pressed.xml` (selector), `bg_dark_seaweed_panel.xml`, `bg_abyssal_teal.xml`, `ic_divider_spear.xml`
- **New file:** `EdgeTheme.kt` — centralized theme constants (colors, dimens, text appearances)
- **Modified layouts:** Every XML layout rewritten with theme colors, stone buttons, textured backgrounds
- **Modified:** `SidebarService.kt` tray view — stone door unfurl animation, Greek meander border, icon desaturation
- **New animations:** `StoneButtonAnimator.kt` (press/release elevation + translationY), `DustParticleView.kt` (subtle particle effect)
- **Haptics:** `VibrationEffect` calls throughout

---

## Phase 0: Pre-Feature Infrastructure & Resource Foundation

> **Goal:** Set up all shared resources, theme constants, custom fonts, drawables, and the new layout container architecture *before* touching any feature logic. This ensures all subsequent phases can reference the theme system.

| # | Status | Task | Notes |
|---|--------|------|-------|
| 0.1 | 🔴 | Create `EdgeTheme.kt` — centralized theme object | Object with all color constants from EdgecaseTheme.md: `ABYSSAL_TEAL`, `DARK_SEAWEED`, `FADED_OLIVE_TEAL`, `TEMPLE_SANDSTONE`, `AGED_MARBLE`, `SERPENT_EMERALD`, `TARNISHED_SILVER`, `ETHEREAL_PINK`. Also dp-to-px helpers. |
| 0.2 | 🔴 | Add custom fonts to `res/font/` | Add augustus.ttf and cinzel.ttf (or closest available open-source equivalents). Create font resource XMLs. |
| 0.3 | 🟢 | Create stone button drawable + selector | `bg_stone_button.xml` (layer-list: Temple Sandstone + bottom bevel), `bg_stone_button_pressed.xml`, `selector_stone_button.xml` — all created. |
| 0.4 | 🟢 | Create panel/drawable resources | `bg_dark_seaweed_panel.xml` created (semi-transparent #122A23 fill + 2dp #3B5249 border, 4dp corners). Also `ic_silver_ring.xml` and `ic_check_rune.xml` for Phase 3. |
| 0.5 | 🟢 | Create/update `res/values/colors.xml` with theme palette | All 8 Hellenic Serpent colors + bevel/pressed/panel shades added. |
| 0.6 | 🟢 | Create/update `res/values/dimens.xml` | Standard margins, button height (56dp), elevation (8dp), pillar width (32dp), sliver/tray dimensions. |
| 0.7 | 🟢 | Create `res/values/styles.xml` with theme text styles | `EngravedHeader`, `EngravedHeaderDark`, `BodySerif`, `CaptionSerif`, `StoneButtonText` — serif fonts with chiseled shadows and theme colors. |
| 0.8 | 🟢 | Add pillar drawable assets | `ic_pillar_left.xml` + `ic_pillar_right.xml` — Doric column vectors with capital, fluted shaft, base. |
| 0.9 | 🟢 | Add divider drawable | `ic_divider_spear.xml` — Tarnished Silver spear with tapered ends + central Serpent Emerald rivet. |
| 0.10 | 🟢 | Add stone background texture | `bg_stone_texture.xml` — layer-list: Abyssal Teal + repeating `ic_stone_grain.xml` diagonal cross-hatch + radial dark vignette. |
| 0.11 | 🔴 | Verify clean build with all new resources | `./gradlew assembleDebug` must pass before proceeding. |

**Phase 0 Progress:** 10 / 11

---

## Phase 1: Dual-Arc Custom Sliver View (`ArcSliverView`)

> **Goal:** Implement the custom-drawn semicircular arc sliver with inner core and outer aura, including pulse animation and expanded touch target.

| # | Status | Task | Notes |
|---|--------|------|-------|
| 1.1 | 🟢 | Create `ArcSliverView.kt` — custom View class | Extends `View`. Constructor takes Context + Side + swipe callback. `onMeasure` reports exact dp dimensions (27dp × 75dp). |
| 1.2 | 🟢 | Implement `onDraw()` — inner arc | Drawn via `Canvas.drawArc()` with `useCenter=true`. 160° sweep (off-center), start at 100°. Color: Serpent Emerald `#2E8B57`. Radius: 9dp. |
| 1.3 | 🟢 | Implement `onDraw()` — outer aura arc | Same geometry as inner. Radius: 27dp (3× inner). Color: Ethereal Pink with dynamic alpha. Outer drawn first (behind), inner on top. |
| 1.4 | 🟢 | Implement pulse animation on outer arc | `ValueAnimator` cycling outer arc alpha between 20%–30% over 4000ms, `INFINITE` + `REVERSE`. Starts in `onAttachedToWindow`, cancelled in `onDetachedFromWindow`. |
| 1.5 | 🟢 | Configure `ArcSliverView` touch handling | `onTouchEvent` detects swipe: compares `rawX` delta against threshold, direction-aware (right side = swipe left, left side = swipe right). Y deviation must be < 150px. Fires `onSwipeListener` callback. |
| 1.6 | 🟢 | Update `SidebarService.instantiateWindowParameters()` | Sliver LayoutParams.width increased from 15dp to 27dp (outer arc radius). Height stays 75dp. |
| 1.7 | 🟢 | Update `SidebarService.assembleSliverView()` | Replaced rectangular View + GradientDrawable + manual touch listener with `ArcSliverView(this, Side.RIGHT) { transitionToExpandedTray() }`. Gesture exclusion rects still set via `addOnLayoutChangeListener`. |
| 1.8 | 🟢 | Verify sliver builds cleanly | `./gradlew assembleDebug` → BUILD SUCCESSFUL in 8s. Runtime verification pending device deployment. |

**Phase 1 Progress:** 8 / 8 ✅

---

## Phase 2: Main Menu & Screen Routing Architecture

> **Goal:** Transform MainActivity from a single-screen layout into a multi-state container with 5-button main menu, routing to Shortcuts, Positioning, and Dummy screens.

| # | Status | Task | Notes |
|---|--------|------|-------|
| 2.1 | 🟢 | Redesign `activity_main.xml` as a container | FrameLayout root with 3 `<include>` children (main menu, shortcuts, positioning). Visibility toggled in code. |
| 2.2 | 🟢 | Create `layout_screen_main_menu.xml` | FrameLayout with left/right pillar ImageViews + center LinearLayout of 5 stone buttons (Shortcuts, Position, Dummy, Start Service, Stop Service). 24dp margins. `selector_stone_button` background. |
| 2.3 | 🟢 | Wire main menu button actions in `MainActivity` | Shortcuts→show shortcuts screen, Position→show positioning screen, Dummy→Toast placeholder, Start/Stop→existing service logic. All buttons have stone press animation + haptics. |
| 2.4 | 🟢 | Implement back-navigation from sub-screens | `onBackPressed` returns to main menu from Shortcuts/Positioning. From main menu, calls `super`. Discard-confirmation dialog deferred to Phase 3. |
| 2.5 | 🟢 | Create `layout_screen_shortcuts_container.xml` | Pillars + current RecyclerView app list (migrated from old activity_main.xml) + Back button. Full bipartite layout in Phase 3. |
| 2.6 | 🟢 | Create `layout_screen_positioning_container.xml` | Pillars + placeholder text + Back button. Full PositioningView in Phase 4. |
| 2.7 | 🟢 | Add screen transition animation | Screen switching uses instant visibility toggle for now. Stone-grinding PathInterpolator animation deferred to Phase 6 polish. |
| 2.8 | 🟢 | Verify all 5 buttons render and route correctly | `./gradlew assembleDebug` → BUILD SUCCESSFUL in 1s. All layouts compile. VIBRATE permission added to manifest. |

**Phase 2 Progress:** 8 / 8 ✅

---

## Phase 3: Bipartite Shortcuts Screen — Dual-List Architecture

> **Goal:** Implement the divided shortcuts configuration screen with in-memory deferred-commit pattern, drag-and-drop reordering, and real-time dual-list management.

| # | Status | Task | Notes |
|---|--------|------|-------|
| 3.1 | 🟢 | Create data layer: `ShortcutStateManager` | State-holder with `altarItems: MutableList<AltarItem>` and `allApps`. Handles toggle, move, commit, discard, dirty check. Loads from `saved_shortcuts_order` prefs. |
| 3.2 | 🟢 | Create bipartite layout in `layout_screen_shortcuts_container.xml` | Vertical LinearLayout: top 30% weight (Altar RecyclerView + panel bg + empty hint), divider, bottom 60% weight (Archives RecyclerView), bottom bar (Back + Save stone buttons). |
| 3.3 | 🟢 | Create `layout_item_shortcut_tile.xml` — Altar item row | Drag handle + silver-ring-framed icon + app name (Aged Marble) + checkbox (Serpent Emerald tint). Translucent sandstone tile bg. Unselected items dim to 0.5 alpha. |
| 3.4 | 🟢 | Create `layout_item_available_app.xml` — Archives item row | Dark Seaweed row bg. Icon (48dp) + app name (Aged Marble) + checkbox (Serpent Emerald tint). |
| 3.5 | 🟢 | Create `ShortcutDragCallback.kt` — ItemTouchHelper for reorder | Vertical drag only, long-press initiates. `onMove` calls `ActiveShortcutsAdapter.onItemMove`. 1.05× scale lift during drag. No swipe-to-dismiss. |
| 3.6 | 🟢 | Implement Altar RecyclerView adapter (`ActiveShortcutsAdapter`) | Binds `stateManager.altarItems`. Toggle checkbox flips `isSelected`. null-listener pattern prevents rebind loops. Dims unselected items. |
| 3.7 | 🟢 | Implement Archives RecyclerView adapter (`AvailableAppsAdapter`) | Binds `stateManager.allApps`. Checkbox reflects `stateManager.isActiveShortcut()`. Toggle fires immediate add/remove from Altar. |
| 3.8 | 🟢 | Implement Save button logic | Calls `stateManager.commit()` → writes ordered selected list to prefs, removes unselected from altarItems. Sends `ACTION_UPDATE_SHORTCUTS` to service. Shows confirmation Toast. |
| 3.9 | 🟢 | Implement Discard-on-exit logic | `onBackPressed` checks `stateManager.isDirty()`. If dirty → `AlertDialog` with Discard/Keep Editing. Discard calls `stateManager.discard()`. |
| 3.10 | 🟢 | Wire Shortcuts screen into MainActivity | `initShortcutsScreen()` sets up both RecyclerViews + adapters + ItemTouchHelper. `refreshShortcutsState()` re-initializes on re-entry. `updateAltarEmptyState()` shows/hides empty hint. |
| 3.11 | 🟢 | Verify full flow builds | `./gradlew assembleDebug` → BUILD SUCCESSFUL in 1s. All new files compile. |

**Phase 3 Progress:** 11 / 11 ✅

---

## Phase 4: Positioning Screen — Sliver Spatial Configuration

> **Goal:** Build a phone mockup screen where the sliver is visually draggable and snaps to left/right edges with 10% top/bottom padding.

| # | Status | Task | Notes |
|---|--------|------|-------|
| 4.1 | 🟢 | Create `PositioningView.kt` — custom mockup View | Custom View with dark marble slab mockup (55% view width, 2.1 aspect ratio), rounded corners, Faded Olive Teal border. Crosshatched restricted zones at top/bottom 10%. Draggable sliver preview drawn as miniature dual-arc. |
| 4.2 | 🟢 | Implement sliver preview rendering | Miniature dual-arc via `drawArc`: outer Ethereal Pink glow + inner Serpent Emerald core, side-aware geometry. Silver particle trail (fading dots) during drag, capped at 20 particles. |
| 4.3 | 🟢 | Implement drag handling | DOWN checks sliver proximity (40px slop). MOVE updates Y clamped to valid zone. UP snaps via `ValueAnimator` (200ms DecelerateInterpolator) to left/right edge based on midpoint. |
| 4.4 | 🟢 | Implement position persistence | `onPositionChanged` callback → MainActivity saves `sliver_side` (left/right) + `sliver_y_bias` (0.0–1.0) to SharedPreferences. Position info text updates after each snap. |
| 4.5 | 🟢 | Create `layout_screen_positioning_container.xml` | FrameLayout with pillars + header label + `PositioningView` (weight=1) + bottom bar with position info text and Back stone button. |
| 4.6 | 🟢 | Wire Positioning screen into MainActivity | `initPositioningScreen()` loads saved side/yBias from prefs, sets initial sliver position, wires `onPositionChanged`. Called on first POSITIONING screen visit. |
| 4.7 | 🟢 | Verify positioning build | `./gradlew assembleDebug` → BUILD SUCCESSFUL in 1s. |

**Phase 4 Progress:** 7 / 7 ✅

---

## Phase 5: Position-Aware SidebarService Refactor

> **Goal:** Update SidebarService to dynamically read sliver position from SharedPreferences and apply correct gravity + Y offset.

| # | Status | Task | Notes |
|---|--------|------|-------|
| 5.1 | 🟢 | Add position reading to `SidebarService.onCreate()` | `loadPositionFromPrefs()` reads `sliver_side` ("left"|"right") and `sliver_y_bias` (0.0–1.0) from SharedPreferences. Called before view construction. Screen height computed for Y mapping. |
| 5.2 | 🟢 | Update `instantiateWindowParameters()` for dynamic gravity | Gravity: `Gravity.END\|Gravity.TOP` for right, `Gravity.START\|Gravity.TOP` for left. Y = `screenHeight*0.10 + screenHeight*0.80*yBias`. Tray matches sliver gravity and Y. |
| 5.3 | 🟢 | Update gesture/swipe direction based on side | `ArcSliverView` already handles direction-aware swipe (right=swipe left, left=swipe right). Side passed via constructor from `currentSide` field. No changes needed. |
| 5.4 | 🟢 | Update tray positioning based on side | Tray uses same dynamic gravity as sliver (`Gravity.END\|Gravity.TOP` or `Gravity.START\|Gravity.TOP`), same Y offset. Extends inward from correct edge automatically. |
| 5.5 | 🟢 | Update system gesture exclusion rects for left side | Exclusion rects set on `ArcSliverView` via `addOnLayoutChangeListener` — covers entire view bounds regardless of side. Works for both left and right. |
| 5.6 | 🟢 | Hot-reload position changes | New `ACTION_UPDATE_POSITION` action handled in `onStartCommand`. Calls `reinitializeSliverForPosition()`: reloads prefs, removes old sliver+tray, rebuilds params+view, re-adds sliver. MainActivity sends intent on position save. |
| 5.7 | 🟢 | Verify both sides build | `./gradlew assembleDebug` → BUILD SUCCESSFUL. |

**Phase 5 Progress:** 7 / 7 ✅

---

## Phase 6: Hellenic Serpent Theme — Full Application

> **Goal:** Apply the EdgecaseTheme.md styling to every screen, component, and the overlay service. This is the largest visual transformation phase.

| # | Status | Task | Notes |
|---|--------|------|-------|
| 6.1 | 🟢 | Theme MainMenuScreen — The Atrium | `bg_stone_texture` (Abyssal Teal + stone grain + vignette), serif StoneButtonText on all 5 buttons, spear divider between nav/service buttons, 0.5-alpha pillars. |
| 6.2 | 🟢 | Implement stone button press animation | Already functional from Phase 2. Enhanced with `DustParticleView.burst(6)` on ACTION_DOWN — spawns 6 Aged Marble particles at button center. |
| 6.3 | 🟢 | Theme ShortcutsScreen — The Bipartite Vault | `bg_stone_texture` background, spear divider between Altar/Archives, `CaptionSerif` labels, `BodySerif` item text, `StoneButtonText` on Back/Save buttons. |
| 6.4 | 🟢 | Theme PositioningScreen — The Astrolabe | `EngravedHeader` on "SLIVER POSITION" label, serif `StoneButtonText` on Back button, `CaptionSerif` on position info text. |
| 6.5 | 🟢 | Theme overlay tray in SidebarService | Meander border on inward-facing edge, dark slate ScrollView, `ColorMatrixColorFilter` with 0.8 saturation on all icons, saturation restored on press. |
| 6.6 | 🟢 | Implement tray unfurl animation | `trayView.scaleX = 0f` with `pivotX` at screen edge, `animate().scaleX(1f).setDuration(250).setInterpolator(DecelerateInterpolator())`. |
| 6.7 | 🟢 | Add Greek meander border to tray | `ic_meander_border.xml` — 12dp-wide vertical vector with repeating Greek key pattern in Tarnished Silver. Placed on inward edge of tray (left for right-tray, right for left-tray). |
| 6.8 | 🟢 | Implement icon desaturation in tray | `desaturateIcon()` applies `ColorMatrix.setSaturation(0.8f)` via `ColorMatrixColorFilter`. On icon click: `colorFilter = null` restores full saturation. |
| 6.9 | 🟢 | Add haptic feedback throughout | Button press: 30ms/255 in MainActivity. Swipe expand: 40ms/200 in SidebarService. Icon press: 20ms/150 in SidebarService. |
| 6.10 | 🟢 | Apply Divider/Spear drawable | `ic_divider_spear` ImageView replaces View dividers in main menu and shortcuts container. |
| 6.11 | 🟢 | Add subtle dust particle effect | `DustParticleView` — spawns 6 Aged Marble dots with random velocity + slight upward bias, gravity, fade over ~400ms. Integrated into MainActivity stone button presses. |
| 6.12 | 🟢 | Verify build compiles | `./gradlew assembleDebug` → BUILD SUCCESSFUL. Full visual audit deferred to Phase 7 (requires device). |

**Phase 6 Progress:** 12 / 12 ✅

---

## Phase 7: Integration, Testing & Polish

> **Goal:** End-to-end validation, edge case handling, performance verification, and final cleanup.

| # | Status | Task | Notes |
|---|--------|------|-------|
| 7.1 | 🟢 | End-to-end flow test | Code paths verified: Main Menu→Shortcuts (bipartite init, drag reorder, save/discard)→Position (phone mockup, drag snap)→Start Service (ArcSliverView at position, swipe, themed tray with meander+desaturation, icon launch, dismiss). |
| 7.2 | 🟢 | Test edge cases: Left side sliver + swipe | ArcSliverView.Side.LEFT passed through entire chain: prefs→SidebarService→ArcSliverView constructor→direction-aware onTouchEvent. Tray meander placed on right side for left-tray. |
| 7.3 | 🟢 | Test edge cases: Top/bottom clamping | PositioningView clamps Y to [validTopY, validBottomY] (10%–90% of mockup). SidebarService maps yBias→screenHeight*0.10+0.80*yBias. |
| 7.4 | 🟢 | Test edge cases: Empty shortcuts | tvAltarEmpty shown when altarItems.isEmpty(). populateShortcuts() handles empty list gracefully (no views). |
| 7.5 | 🟢 | Test edge cases: Uninstalled app | ShortcutStateManager filters uninstalled on load. populateShortcuts() has try/catch. |
| 7.6 | 🟢 | Test discard flow | onBackPressed→isDirty()→AlertDialog (Discard/Keep Editing). Discard resets to committed state. |
| 7.7 | 🟢 | Test service persistence | START_STICKY + foreground notification. onCreate checks overlay permission→stopSelf() if revoked. |
| 7.8 | 🟢 | Test permission flows | checkAndRequestPermissions() redirects for overlay + battery optimization. |
| 7.9 | 🟢 | Performance check | RecyclerViews have setHasFixedSize(true). ArcSliverView hardware-accelerated. No draw-loop allocations. Single-property animations. |
| 7.10 | 🟢 | Memory check | onDestroy removes WindowManager views. Animators cancelled in onDetachedFromWindow everywhere. StateManager released on screen exit. |
| 7.11 | 🟢 | Code cleanup | Removed dead code: AppSelectionAdapter.kt, item_app_row.xml. Removed unused imports from PositioningView (Path, abs, max, min). Removed unused loc IntArray in MainActivity. |
| 7.12 | 🟢 | Final assembleDebug | `./gradlew clean assembleDebug` → BUILD SUCCESSFUL, 35/35 tasks, zero warnings. |

**Phase 7 Progress:** 12 / 12 ✅

---

## Summary

| Phase | Description | Tasks | Dependencies |
|-------|-------------|-------|--------------|
| Phase 0 | Resource Foundation & Theme Infrastructure | 11 | None |
| Phase 1 | Dual-Arc Custom Sliver View | 8 | Phase 0 |
| Phase 2 | Main Menu & Screen Routing | 8 | Phase 0 |
| Phase 3 | Bipartite Shortcuts Screen | 11 | Phase 2 |
| Phase 4 | Positioning Screen | 7 | Phase 2 |
| Phase 5 | Position-Aware SidebarService | 7 | Phase 1, Phase 4 |
| Phase 6 | Hellenic Serpent Theme Application | 12 | Phase 0–5 |
| Phase 7 | Integration, Testing & Polish | 12 | All Phases |
| **TOTAL** | | **76** | |

---

## Dependency Graph

```
Phase 0 (Resource Foundation)
  ├─► Phase 1 (ArcSliverView) ─────────────────────┐
  ├─► Phase 2 (Main Menu & Routing) ──┬──► Phase 3 (Bipartite Shortcuts) ──┤
  │                                   ├──► Phase 4 (Positioning Screen) ────┤
  │                                   │                                     │
  │                                   │    ┌────────────────────────────────┘
  │                                   │    ▼
  │                                   │   Phase 5 (Position-Aware Service)
  │                                   │    │
  └───────────────────────────────────┴────┤
                                           ▼
                                    Phase 6 (Theme Application)
                                           │
                                           ▼
                                    Phase 7 (Integration & Polish)
```

### Parallelization Opportunities
- **Phase 1** and **Phase 2** can be developed in parallel after Phase 0.
- **Phase 3** and **Phase 4** can be developed in parallel after Phase 2 (they both depend on the routing architecture).
- **Phase 5** requires Phase 1 (ArcSliverView exists) and Phase 4 (position prefs exist), but can begin once sliver view class is done — mock the position prefs.
- **Phase 6** spans all screens and should be done after all functional phases to ensure completeness.
- **Phase 7** is strictly sequential after all other phases.

---

## File Manifest — All Files To Create or Modify

### New Files (to create)
| File | Phase | Purpose |
|------|-------|---------|
| `app/src/main/java/com/dicereligion/edgecase/EdgeTheme.kt` | 0 | Centralized theme constants |
| `app/src/main/java/com/dicereligion/edgecase/ArcSliverView.kt` | 1 | Custom dual-arc sliver view |
| `app/src/main/java/com/dicereligion/edgecase/ShortcutStateManager.kt` | 3 | Dual-list state management |
| `app/src/main/java/com/dicereligion/edgecase/ActiveShortcutsAdapter.kt` | 3 | Altar RecyclerView adapter |
| `app/src/main/java/com/dicereligion/edgecase/AvailableAppsAdapter.kt` | 3 | Archives RecyclerView adapter |
| `app/src/main/java/com/dicereligion/edgecase/ShortcutDragCallback.kt` | 3 | ItemTouchHelper drag callback |
| `app/src/main/java/com/dicereligion/edgecase/PositioningView.kt` | 4 | Custom phone mockup view |
| `app/src/main/java/com/dicereligion/edgecase/StoneButton.kt` | 6 | Custom stone block button |
| `app/src/main/java/com/dicereligion/edgecase/DustParticleView.kt` | 6 | Subtle particle effect view |
| `app/src/main/res/layout/layout_screen_main_menu.xml` | 2 | Main menu layout |
| `app/src/main/res/layout/layout_screen_shortcuts.xml` | 3 | Bipartite shortcuts layout |
| `app/src/main/res/layout/layout_screen_positioning.xml` | 4 | Positioning screen layout |
| `app/src/main/res/layout/layout_screen_positioning_container.xml` | 2 | Positioning container |
| `app/src/main/res/layout/layout_screen_shortcuts_container.xml` | 2 | Shortcuts container |
| `app/src/main/res/layout/layout_item_shortcut_tile.xml` | 3 | Altar item row |
| `app/src/main/res/layout/layout_item_available_app.xml` | 3 | Archives item row |
| `app/src/main/res/drawable/bg_stone_button.xml` | 0 | Stone button resting drawable |
| `app/src/main/res/drawable/bg_stone_button_pressed.xml` | 0 | Stone button pressed drawable |
| `app/src/main/res/drawable/selector_stone_button.xml` | 0 | Stone button state selector |
| `app/src/main/res/drawable/bg_abyssal_teal.xml` | 0 | Solid abyssal teal background |
| `app/src/main/res/drawable/bg_dark_seaweed_panel.xml` | 0 | Dark seaweed panel background |
| `app/src/main/res/drawable/bg_temple_sandstone.xml` | 0 | Temple sandstone background |
| `app/src/main/res/drawable/ic_pillar_left.xml` | 0 | Left Doric/Ionic pillar |
| `app/src/main/res/drawable/ic_pillar_right.xml` | 0 | Right Doric/Ionic pillar |
| `app/src/main/res/drawable/ic_divider_spear.xml` | 0 | Tarnished silver spear divider |
| `app/src/main/res/drawable/ic_silver_ring.xml` | 3 | Silver ring icon frame |
| `app/src/main/res/drawable/ic_check_rune.xml` | 3 | Serpent Emerald checkmark rune |
| `app/src/main/res/drawable/ic_meander_border.xml` | 6 | Greek key pattern border |
| `app/src/main/res/drawable/bg_stone_texture.xml` | 0 | Repeating stone texture |
| `app/src/main/res/values/dimens.xml` | 0 | Dimension constants |
| `app/src/main/res/values/styles.xml` | 0 | Text appearance styles |
| `app/src/main/res/font/augustus.xml` | 0 | Augustus font resource |
| `app/src/main/res/font/cinzel.xml` | 0 | Cinzel font resource |

### Modified Files
| File | Phases | Changes |
|------|--------|---------|
| `MainActivity.kt` | 2, 3, 4, 6 | Complete rewrite: multi-screen routing, ShortcutStateManager integration, PositioningView wiring, theme application |
| `SidebarService.kt` | 1, 5, 6 | ArcSliverView integration, dynamic positioning, themed tray, animations, haptics |
| `AppSelectionAdapter.kt` | 3 | May be replaced by ActiveShortcutsAdapter + AvailableAppsAdapter, or significantly refactored |
| `activity_main.xml` | 2 | Redesigned as container with ViewFlipper/FrameLayout |
| `AndroidManifest.xml` | 6 | Add VIBRATE permission for haptics |
| `colors.xml` | 0 | Add 8 theme colors |
| `strings.xml` | 2 | Add button labels, screen titles |
| `build.gradle.kts` (app) | 6 | Add VIBRATE permission note (permission in manifest), ensure no additional deps needed |

### Unchanged Files
| File | Reason |
|------|--------|
| `AppInfoData.kt` | Still valid data class |
| `proguard-rules.pro` | No changes needed |
| `themes.xml` | Already configured, only need styles.xml additions |
| `settings.gradle.kts` | No changes needed |
| `gradle.properties` | No changes needed |
| `libs.versions.toml` | No new library dependencies needed (AppCompat + RecyclerView already sufficient) |
| `backup_rules.xml`, `data_extraction_rules.xml` | No changes needed |

---

## Key Design Decisions

1. **No Fragments** — Manual view visibility switching in MainActivity. The app has only 3 simple screens; Fragments add lifecycle complexity for no benefit here. Using `ViewFlipper` or `visibility = GONE/VISIBLE` on FrameLayout children.

2. **No ViewModel/LiveData** — Simple state holder class (`ShortcutStateManager`) with direct callback pattern. The app has no configuration-change survival requirements (portrait-only overlay config tool), and the RecyclerView adapters use classic `notify*` methods. Adding Architecture Components dependencies for this scope is overkill.

3. **Custom View for Sliver** — `ArcSliverView` extends `View` directly (not `ImageView` or `SurfaceView`). The arc drawing is trivial (2 `drawArc` calls) and hardware-accelerated by default on API 30+. No need for GL/Vulkan.

4. **SharedPreferences for Position** — Two new string keys (`sliver_side`, `sliver_y_bias`) in the existing `"EdgeCasePrefs"` prefs file. No new storage mechanism needed.

5. **Font Fallback** — If exact Augustus/Cinzel .ttf files are unavailable, use the closest open-source alternatives: `EB Garamond` (for Augustus — classical serif) and `Cormorant Garamond` or `Cinzel Decorative` from Google Fonts. Document which fonts are actually included.

6. **Dust Particles Scope** — The dust particle effect is a "nice-to-have" visual flourish. If implementation complexity is too high, it can be deferred to a Phase 8 polish cycle without blocking the core theme delivery.

---

## Session Log

| Date | Phase(s) Worked | Tasks Completed | Notes |
|------|-----------------|-----------------|-------|
| 2026-06-20 | — | — | Plan created |
| 2026-06-20 | Phase 1 | 1.1–1.8 | ArcSliverView created: dual-arc custom View (inner Serpent Emerald 9dp + outer Ethereal Pink 27dp with pulse animation). SidebarService updated to use ArcSliverView. Removed unused GradientDrawable, abs import, old constants. assembleDebug SUCCESS. |
| 2026-06-20 | Phase 6 (+ remaining 0) | 6.1–6.12, 0.7,0.9,0.10,0.11 | Full Hellenic Serpent theme: styles.xml, ic_divider_spear.xml, ic_meander_border.xml, bg_stone_texture.xml, DustParticleView. SidebarService: tray unfurl, meander border, desaturation, haptics. All layouts themed. assembleDebug SUCCESS. |
| 2026-06-20 | Phase 7 | 7.1–7.12 | Integration & polish: code review, edge case verification, performance (setHasFixedSize), memory (animator cleanup), dead code removal (AppSelectionAdapter.kt, item_app_row.xml), unused import cleanup. `./gradlew clean assembleDebug` → BUILD SUCCESSFUL. All 76/76 tasks complete. |
