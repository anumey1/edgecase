# EdgeCase — Dimensions & Layout Reference

> **Purpose:** A single, stable addressing system for **every UI element on every page** of EdgeCase.
> Each element has a permanent **ID** (`PAGE.element`) and a short **code** (`M1`, `S4`, …), plus its
> size, on-screen anchor, offsets, and spacing to neighbours. Use this so that instructions like
> *"move `POSITION.btnBack` (P7) up by 12dp"* or *"widen `MENU.btnShortcuts` (M5) to 90%"* are
> unambiguous and repeatable.
>
> **Source of truth:** the layouts in `app/src/main/res/layout/` and the programmatic overlay in
> `SidebarService.kt` / `ArcSliverView.kt` / `PositioningView.kt`. Values below are the *design* values
> (dp/sp) as written in code — they are device-independent unless a percentage is given.
> The sliver's internal fang geometry has its own deep-dive companion: **`Docs/SliverAnatomy.md`**
> (§6 here is the summary; that file has the diagrams, per-value examples, and tuning grammar).
>
> **Last generated:** 2026-07-10 · **App version:** v1.3.5

---

## 0. Conventions (read first)

### 0.1 Coordinate system
- **Origin `(0,0)` = top-left** of the drawable screen area (below the status bar, since every screen sets
  `fitsSystemWindows="true"`). **X →** increases rightward, **Y ↓** increases downward.
- **Units:**
  - **`dp`** for all sizes, margins, and paddings — these are the exact code values and are the primary,
    consistent measurement. Use dp whenever telling me to move/resize something.
  - **`%`** is used only for elements whose position/size is inherently proportional (layout `weight`s,
    and the overlay sliver's `yBias`). Percentages are of the **parent region**, stated per case.
  - **`sp`** for text sizes only.
- **Reference viewport** (only for resolving `%`→concrete): **411dp × 891dp** content area
  (a Pixel-class phone, after system insets). Sizes in dp do **not** depend on this; only the `%` resolutions do.

### 0.2 Anchor codes
Every element states an **anchor** = where in its parent it is pinned, then **offsets** (margins) from that anchor.

| Code | Meaning | Code | Meaning | Code | Meaning |
|------|---------|------|---------|------|---------|
| `TL` | top-left | `TC` | top-center | `TR` | top-right |
| `CL` | center-left | `C` | center | `CR` | center-right |
| `BL` | bottom-left | `BC` | bottom-center | `BR` | bottom-right |
| `FLOW↓` | stacked vertically; Y determined by the sibling above it + its top margin (LinearLayout child) | | | | |
| `FLOW→` | stacked horizontally; X determined by the sibling to its left + its start margin | | | | |

For `FLOW` elements the important number is the **gap** to the previous sibling (its top/start margin),
given in each table.

### 0.3 Size notation
`W × H` in dp. Special sizes:
- **`MP`** = `match_parent` (fills the parent dimension).
- **`WC`** = `wrap_content` (hugs its content).
- **`w=<n>`** = `layout_weight = n` → the element shares leftover space in its axis proportionally to `n`.
  Resolved `%` of the flexible region is given where useful.

### 0.4 ID & code scheme
- **ID = `PAGE.element`**. `element` matches the real Android view id where one exists (e.g. `btnShortcuts`),
  so it maps straight to code; coined names are used for programmatic/unnamed views (e.g. `TRAY.meander`).
- **Code = page letter + number** for terse reference. Page letters:

| Prefix | Page / surface |
|--------|----------------|
| `MENU` / **M** | Main Menu screen |
| `SHORTCUTS` / **S** | Shortcuts configuration screen |
| `TILE` / **TL** | One row in the Altar (current-shortcuts) list *(template)* |
| `APPROW` / **AR** | One row in the Archives (available-apps) list *(template)* |
| `POSITION` / **P** | Positioning screen |
| `SLIVER` / **SL** | Edge handle overlay (service) |
| `TRAY` / **T** | Expanded shortcut tray overlay (service) |
| `DIALOG` / **D** | Modal dialogs |

### 0.5 How to instruct a change
Any of these forms is understood:
- **Move:** `MOVE M5 down 16dp` · `MOVE P7 left 24dp` · `SET S9 gap 12dp` (change gap to previous sibling).
- **Resize:** `SIZE M5 → MP × 64dp` · `WIDTH P7 → 140dp` · `WEIGHT S6 → 0.5`.
- **Reposition anchor:** `ANCHOR MENU.version → BR` .
- **Spacing:** `PAD MENU.content L/R → 40dp` · `MARGIN S5.bottom → 6dp`.

A single element is always identified by **either** its ID **or** its code — both are listed in every table.

### 0.6 Shared / global elements
These recur on the three full-screen config pages. They are addressed **per page** (e.g. `MENU.pillarL`)
but share identical specs:

| Element (per page) | Size | Anchor | Notes |
|--------------------|------|--------|-------|
| `*.pillarL` | `32 × MP` | `CL` (`start\|center_vertical`) | `ic_pillar_left`, alpha 0.5, `fitXY` |
| `*.pillarR` | `32 × MP` | `CR` (`end\|center_vertical`) | `ic_pillar_right`, alpha 0.5, `fitXY` |
| screen background | `MP × MP` | fill | `bg_stone_texture`; root FrameLayout of every config screen |

Root host: `activity_main.xml` = FrameLayout (`R.id.screenContainer`, `MP×MP`, bg `abyssal_teal`) holding
the three screens as `<include>`s toggled by visibility (`MainActivity.showScreen`).

### 0.7 Shared button spec (stone buttons)
Unless a table overrides it, every `Button` uses: height **56dp** (`stone_button_height`), elevation **8dp**,
background `selector_stone_button`, text style `StoneButtonText` (serif, all-caps, 18sp, letterSpacing 0.08).
Press behaviour (from `MainActivity.applyStoneButtonBehavior`): on `ACTION_DOWN` it animates
`translationY = +4dp` (`stone_button_pressed_translation`, 80ms), fires a 30ms haptic, and bursts 6 dust particles.

---

## 1. Page `MENU` — Main Menu
`layout_screen_main_menu.xml` · shown by default on launch.

### 1.1 Hierarchy
```
MENU.root (FrameLayout, MP×MP, bg_stone_texture)
├─ MENU.pillarL         (M1)  32×MP        CL
├─ MENU.pillarR         (M2)  32×MP        CR
├─ MENU.dustContainer   (M3)  MP×MP        fill   ← DustParticleView added here at runtime
├─ MENU.content         (LinearLayout V, MP×MP, pad L/R 56, top 12, bottom 32)
│  ├─ MENU.headerBar    (M4)  MP×WC        FLOW↓  bg_dark_seaweed_panel, pad 10, marginBottom 20
│  │   └─ MENU.title    (M4a) w=1×WC              "EDGECASE" 18sp EngravedHeader
│  └─ MENU.buttonRegion (LinearLayout V, MP× w=1, gravity center)  ← vertically centered block
│     ├─ MENU.btnShortcuts     (M5)  MP×56   FLOW↓  "SHORTCUTS",  marginBottom 24
│     ├─ MENU.btnPosition      (M6)  MP×56   FLOW↓  "POSITION",   marginBottom 24
│     ├─ MENU.btnDummy         (M7)  MP×56   FLOW↓  "DUMMY",      marginBottom 24
│     ├─ MENU.divider          (M8)  MP×8    FLOW↓  ic_divider_spear, marginTop 8, marginBottom 24
│     ├─ MENU.btnStartService  (M9)  MP×56   FLOW↓  "START EDGE SERVICE" 14sp, marginBottom 24
│     └─ MENU.btnStopService   (M10) MP×56   FLOW↓  "STOP SERVICE"
└─ MENU.version         (M11) WC×WC        BL     "v1.3.5", padStart 16, padBottom 12, alpha 0.6
```

### 1.2 Element table
| Code | ID | Element | Size (dp) | Anchor | Offsets / gap | Notes |
|------|----|---------|-----------|--------|---------------|-------|
| M1 | `MENU.pillarL` | Left pillar | 32 × MP | CL | 0 | decorative, alpha 0.5 |
| M2 | `MENU.pillarR` | Right pillar | 32 × MP | CR | 0 | decorative, alpha 0.5 |
| M3 | `MENU.dustContainer` | Dust overlay | MP × MP | fill | 0 | hosts `DustParticleView` |
| M4 | `MENU.headerBar` | Header panel | MP × WC | TC (in content) | top 12 (content pad); marginBottom 20 | inner pad 10 |
| M4a | `MENU.title` | "EDGECASE" | w=1 × WC | CL (in header) | — | 18sp |
| M5 | `MENU.btnShortcuts` | Shortcuts btn | MP × 56 | FLOW↓ | gap 0; marginBottom 24 | region top |
| M6 | `MENU.btnPosition` | Position btn | MP × 56 | FLOW↓ | gap 24; marginBottom 24 | |
| M7 | `MENU.btnDummy` | Dummy btn | MP × 56 | FLOW↓ | gap 24; marginBottom 24 | stub (Toast) |
| M8 | `MENU.divider` | Spear divider | MP × 8 | FLOW↓ | gap 8 (marginTop); marginBottom 24 | `ic_divider_spear` |
| M9 | `MENU.btnStartService` | Start service | MP × 56 | FLOW↓ | gap 24; marginBottom 24 | 14sp, letterSpacing 0.04 |
| M10 | `MENU.btnStopService` | Stop service | MP × 56 | FLOW↓ | gap 24 | region bottom |
| M11 | `MENU.version` | Version label | WC × WC | BL | padStart 16, padBottom 12 | alpha 0.6 |

**Horizontal:** content is inset **56dp** on both sides → button width = screen − 112dp
(≈ **299dp / 72.7%** of the 411dp reference).
**Vertical:** `MENU.buttonRegion` fills all space between header and bottom padding and **centers the button
block** (5 buttons + divider) within it. Intrinsic block height ≈ `5×56 + 8 + 5×24 = 408dp`.

---

## 2. Page `SHORTCUTS` — Shortcuts configuration
`layout_screen_shortcuts_container.xml` · reached from `MENU.btnShortcuts`.

### 2.1 Hierarchy
```
SHORTCUTS.root (FrameLayout, MP×MP, bg_stone_texture)
├─ SHORTCUTS.pillarL   (S1) 32×MP  CL
├─ SHORTCUTS.pillarR   (S2) 32×MP  CR
└─ SHORTCUTS.content   (LinearLayout V, pad L/R 48, top 12, bottom 8)
   ├─ SHORTCUTS.headerBar   (S3)  MP×WC   FLOW↓  bg panel, pad 10, marginBottom 8
   │   ├─ SHORTCUTS.ringIcon (S3a) 20×20         ic_silver_ring, marginEnd 8
   │   └─ SHORTCUTS.title    (S3b) w=1×WC        "SHORTCUTS" 16sp
   ├─ SHORTCUTS.lblAltar    (S4)  WC×WC   FLOW↓  "CURRENT SHORTCUTS" caption, marginStart 4, marginBottom 4
   ├─ SHORTCUTS.altarPanel  (S5)  MP× w=0.38  FLOW↓  bg panel  ← the "Altar"
   │   ├─ SHORTCUTS.rvAltar   (S5a) MP×MP        RecyclerView, pad 4  → rows = TILE template
   │   └─ SHORTCUTS.tvEmpty   (S5b) WC×WC  C      "No shortcuts selected" (GONE unless empty)
   ├─ SHORTCUTS.divider     (S6)  MP×6    FLOW↓  ic_divider_spear, marginTop 4, marginBottom 4
   ├─ SHORTCUTS.lblArchive  (S7)  WC×WC   FLOW↓  "AVAILABLE APPS" caption, marginStart 4, marginBottom 4
   ├─ SHORTCUTS.rvArchive   (S8)  MP× w=0.42  FLOW↓  RecyclerView  → rows = APPROW template  ← the "Archives"
   └─ SHORTCUTS.actionBar   (S9)  MP× w=0.10  FLOW↓  horizontal, gravity end, marginTop 8
       ├─ SHORTCUTS.btnBack   (S9a) w=1×56  FLOW→  "BACK", marginEnd 12
       └─ SHORTCUTS.btnSave   (S9b) w=1×56  FLOW→  "SAVE"
```

### 2.2 Element table
| Code | ID | Element | Size (dp) | Anchor | Offsets / gap | Notes |
|------|----|---------|-----------|--------|---------------|-------|
| S1 | `SHORTCUTS.pillarL` | Left pillar | 32 × MP | CL | 0 | |
| S2 | `SHORTCUTS.pillarR` | Right pillar | 32 × MP | CR | 0 | |
| S3 | `SHORTCUTS.headerBar` | Header panel | MP × WC | TC | top 12; marginBottom 8 | pad 10 |
| S3a | `SHORTCUTS.ringIcon` | Silver ring | 20 × 20 | CL (header) | marginEnd 8 | `ic_silver_ring` |
| S3b | `SHORTCUTS.title` | "SHORTCUTS" | w=1 × WC | FLOW→ | — | 16sp |
| S4 | `SHORTCUTS.lblAltar` | "CURRENT SHORTCUTS" | WC × WC | FLOW↓ | marginStart 4, marginBottom 4 | caption |
| S5 | `SHORTCUTS.altarPanel` | Altar container | MP × w=0.38 | FLOW↓ | 0 | flexible height |
| S5a | `SHORTCUTS.rvAltar` | Altar list | MP × MP | fill (S5) | pad 4 | rows = **TILE** |
| S5b | `SHORTCUTS.tvEmpty` | Empty text | WC × WC | C (S5) | — | GONE unless list empty |
| S6 | `SHORTCUTS.divider` | Spear divider | MP × 6 | FLOW↓ | gap 4; marginBottom 4 | |
| S7 | `SHORTCUTS.lblArchive` | "AVAILABLE APPS" | WC × WC | FLOW↓ | marginStart 4, marginBottom 4 | caption |
| S8 | `SHORTCUTS.rvArchive` | Archives list | MP × w=0.42 | FLOW↓ | 0 | rows = **APPROW** |
| S9 | `SHORTCUTS.actionBar` | Action bar | MP × w=0.10 | FLOW↓ | marginTop 8 | right-aligned |
| S9a | `SHORTCUTS.btnBack` | BACK button | w=1 × 56 | FLOW→ | marginEnd 12 | 16sp |
| S9b | `SHORTCUTS.btnSave` | SAVE button | w=1 × 56 | FLOW→ | — | 16sp |

**Horizontal:** content inset **48dp** L/R → inner width = screen − 96dp (≈ **315dp / 76.6%**).
`btnBack`/`btnSave` split that width 50/50 minus the 12dp gap.
**Vertical (flexible zone):** the three weighted rows split the leftover height (after header, captions,
divider, margins) in ratio **38 : 42 : 10** →
Altar ≈ **42.2%**, Archives ≈ **46.7%**, Action bar ≈ **11.1%** of the flexible zone.

---

## 3. Component `TILE` — Altar row *(template)*
`layout_item_shortcut_tile.xml` · one instance per shortcut in `SHORTCUTS.rvAltar` (S5a).
Row height = WC (≈ 52dp icon + 12dp vertical padding ≈ **64dp**).

### 3.1 Hierarchy & table
```
TILE.root (FrameLayout, MP×WC, pad L/R 12, top/bottom 6)
├─ TILE.bg        (TL0) MP×MP  fill   translucent sandstone #33D4C4A8
└─ TILE.row       (LinearLayout H, pad 8, gravity center_vertical)
   ├─ TILE.dragHandle (TL1) 24×24  FLOW→  ic_menu_sort_by_size, alpha 0.5
   ├─ TILE.orderNum   (TL2) 24×24  FLOW→  order "1..n", 13sp emerald, marginStart 2 marginEnd 2
   ├─ TILE.iconRing   (TL3) 52×52  FLOW→  silver-ring frame, marginStart 4 marginEnd 12
   │   └─ TILE.icon   (TL3a) 44×44  C     app icon
   ├─ TILE.name       (TL4) w=1×WC FLOW→  app name, BodySerif, 1 line ellipsized
   └─ TILE.checkbox   (TL5) WC×WC  FLOW→  cbAltarSelect, emerald tint, marginStart 8
```
| Code | ID | Element | Size (dp) | Gap to prev | Notes |
|------|----|---------|-----------|-------------|-------|
| TL0 | `TILE.bg` | Row background | MP × MP | — | #33D4C4A8 |
| TL1 | `TILE.dragHandle` | Drag grip | 24 × 24 | 0 | long-press to reorder |
| TL2 | `TILE.orderNum` | Order number | 24 × 24 | 2 | serpent emerald, bold |
| TL3 | `TILE.iconRing` | Icon frame | 52 × 52 | 4 | `ic_silver_ring` bg |
| TL3a | `TILE.icon` | App icon | 44 × 44 | centered | inside ring |
| TL4 | `TILE.name` | App name | w=1 × WC | 12 (ring marginEnd) | fills middle |
| TL5 | `TILE.checkbox` | Select toggle | WC × WC | 8 | dims row to 0.5α when off |

---

## 4. Component `APPROW` — Archives row *(template)*
`layout_item_available_app.xml` · one instance per installed app in `SHORTCUTS.rvArchive` (S8).
Row height = WC (≈ 48dp icon + 24dp padding ≈ **72dp**).

```
APPROW.root (LinearLayout H, MP×WC, pad 12, gravity center_vertical, bg dark_seaweed)
├─ APPROW.icon     (AR1) 48×48   FLOW→  ivArchiveIcon
├─ APPROW.name     (AR2) w=1×WC  FLOW→  tvArchiveName, marginStart 16, marginEnd 8, BodySerif
└─ APPROW.checkbox (AR3) WC×WC   FLOW→  cbArchiveSelect, emerald tint
```
| Code | ID | Element | Size (dp) | Gap to prev | Notes |
|------|----|---------|-----------|-------------|-------|
| AR1 | `APPROW.icon` | App icon | 48 × 48 | 0 | `app_icon_size` |
| AR2 | `APPROW.name` | App name | w=1 × WC | 16 | fills middle, 1 line |
| AR3 | `APPROW.checkbox` | Select | WC × WC | 8 | checked = active shortcut |

---

## 5. Page `POSITION` — Positioning
`layout_screen_positioning_container.xml` · reached from `MENU.btnPosition`.

### 5.1 Hierarchy
```
POSITION.root (FrameLayout, MP×MP, bg_stone_texture)
├─ POSITION.pillarL   (P1) 32×MP  CL
├─ POSITION.pillarR   (P2) 32×MP  CR
└─ POSITION.content   (LinearLayout V, pad L/R 48, top 12, bottom 80)
   ├─ POSITION.headerBar (P3)  MP×WC   FLOW↓  bg panel, pad 10, marginBottom 8
   │   └─ POSITION.title (P3a) w=1×WC        "SLIVER POSITION" 16sp
   ├─ POSITION.view    (P4)  MP× w=1   FLOW↓  ← PositioningView (the "Astrolabe" mockup)
   └─ POSITION.footer  (LinearLayout V, MP×WC, marginTop 12)
      ├─ POSITION.info      (P5) MP×WC   FLOW↓  tvPositionInfo, 14sp tarnished silver
      └─ POSITION.actionRow (LinearLayout H, marginTop 8)
         ├─ POSITION.btnCustomize (P6) w=1×56 FLOW→ "CUSTOMIZE", marginEnd 12 → opens DIALOG.customizeSliver
         └─ POSITION.btnBack      (P7) w=1×56 FLOW→ "BACK"
```
| Code | ID | Element | Size (dp) | Anchor | Offsets / gap | Notes |
|------|----|---------|-----------|--------|---------------|-------|
| P1 | `POSITION.pillarL` | Left pillar | 32 × MP | CL | 0 | |
| P2 | `POSITION.pillarR` | Right pillar | 32 × MP | CR | 0 | |
| P3 | `POSITION.headerBar` | Header panel | MP × WC | TC | top 12; marginBottom 8 | pad 10 |
| P3a | `POSITION.title` | "SLIVER POSITION" | w=1 × WC | CL | — | 16sp |
| P4 | `POSITION.view` | Positioning canvas | MP × w=1 | FLOW↓ | 0 | custom-drawn (see §5.2) |
| P5 | `POSITION.info` | Position readout | MP × WC | FLOW↓ | — | full-width line, e.g. "Side: Right • 50% from top" |
| P6 | `POSITION.btnCustomize` | CUSTOMIZE button | w=1 × 56 | FLOW→ | marginEnd 12 | opens the Customize dialog (§8, `D2`) |
| P7 | `POSITION.btnBack` | BACK button | w=1 × 56 | FLOW→ | — | shares the action row with P6 |

**Horizontal:** content inset **48dp** L/R. **Vertical:** `POSITION.view` (P4) fills everything between the
header and the footer; content bottom padding is a large **80dp**. Footer sits `marginTop 12` above padding.

### 5.2 Sub-elements drawn *inside* `POSITION.view` (P4)
These are **canvas drawings**, not Android views, but are addressable for tuning (`PositioningView.kt`):

| Code | ID | Drawn element | Geometry (relative to P4) | Notes |
|------|----|---------------|---------------------------|-------|
| P4a | `POSITION.mockup` | Phone slab | width = **55%** of P4 width, aspect H/W = **2.1**, centered; corner radius = 4% of mockup W | dark marble fill + border |
| P4b | `POSITION.zoneTop` | Restricted top | top **10%** of mockup height | crosshatched |
| P4c | `POSITION.zoneBottom` | Restricted bottom | bottom **10%** of mockup height | crosshatched |
| P4d | `POSITION.sliverPreview` | Draggable sliver | W = 4% of mockup W, H = W×1.4; snaps to mockup left/right edge; Y clamped to the middle **80%** (valid zone) | fang shape, 50% grey |
| P4e | `POSITION.trail` | Drag particle trail | spawns at sliver, ≤20 particles | silver, fades |
| P4f | `POSITION.instruction` | Hint text | centered, 60px below mockup bottom | "Drag the sliver to reposition" (hidden while dragging) |

Dragging P4d updates `sliver_y_bias` (0=top of valid zone, 1=bottom); release snaps side and persists
`sliver_side` + `sliver_y_bias`, then hot-reloads the live overlay.

---

## 6. Overlay `SLIVER` — edge handle
Built in code (`SidebarService.instantiateWindowParameters` + `ArcSliverView.kt`; preview mirror in
`PositioningView.drawSliverPreview`). This is a `TYPE_APPLICATION_OVERLAY` window floating over all apps —
**not** part of any config screen.

> **Full detail:** `Docs/SliverAnatomy.md` — diagrams, what each value looks like, and the tuning grammar.

### 6.1 The window (`SL1`)
| Code | ID | Property | Value | Notes |
|------|----|----------|-------|-------|
| SL1 | `SLIVER.window` | Size | **27 × 38 dp** (`W × H`) | `sliver_fang_width`/`_height` |
| — | `SLIVER.window` | Anchor | `TR` if side=RIGHT, else `TL` | gravity TOP + END/START |
| — | `SLIVER.window` | **Y position** | `Ytop = 10%·Hscreen + 80%·Hscreen·yBias` | at `yBias 0.5` → top edge at **50%** of screen |
| — | `SLIVER.window` | X position | flush to screen edge (0 inset) | left or right per `sliver_side` |
| — | `SLIVER.window` | Swipe trigger | inward drag **>30px** X, **<150px** Y deviation | expands to TRAY |
| — | `SLIVER.window` | Gesture exclusion | full bounds registered | blocks system edge-swipe steal |

### 6.2 The fang shape (`SL2 = SLIVER.fang`)
The fill (50% grey `#80808080`) is an 8-point path inside the `W × H` box. Points use a **side-independent
`(u, v)`** model: **`u`** = inward depth (`0` = flat spine at the screen edge → `1` = deepest inward reach);
**`v`** = vertical (`0` = top → `1` = bottom). Code converts per side: RIGHT `X = W·(1−u)`, LEFT `X = W·u`,
both `Y = H·v`. Convert units with **`Δu = dp/27`**, **`Δv = dp/38`**. Shape is currently symmetric about `v=0.5`.

**Components:**
| Code | ID | Part | Vertices | Description |
|------|----|------|----------|-------------|
| SP | `SLIVER.spine` | Spine | C0,V1,V6,C7 (edge `u=0`) | Flat outer edge + the flat shoulders above/below the fangs |
| F1 | `SLIVER.tooth1` | Top fang | V1→V2→V3 | Upper tooth; **tip = V2** |
| GM | `SLIVER.gums` | Gums / bridge | V3→V4 | The joiner between the fangs (wall at `u=0.07`) |
| F2 | `SLIVER.tooth2` | Bottom fang | V4→V5→V6 | Lower tooth; **tip = V5** |

**Vertices** (V3 & V4 are shared between a fang and the gums):
| Vertex | `(u, v)` | dp (inward, from top) | Role |
|--------|----------|------------------------|------|
| C0 | (0.00, 0.000) | 0, 0 | top spine corner |
| V1 | (0.00, 0.166) | 0, 6.3 | top fang root (on spine) |
| V2 | (0.60, 0.200) | 16.2, 7.6 | **top fang tip** |
| V3 | (0.07, 0.280) | 1.9, 10.6 | top fang inner root = gums top |
| V4 | (0.07, 0.720) | 1.9, 27.4 | bottom fang inner root = gums bottom |
| V5 | (0.60, 0.800) | 16.2, 30.4 | **bottom fang tip** |
| V6 | (0.00, 0.833) | 0, 31.7 | bottom fang root (on spine) |
| C7 | (0.00, 1.000) | 0, 38 | bottom spine corner |

### 6.3 Tuning knobs (instruct as `<KNOB> <+=|-=|=> <value>[dp|%|u/v]`)
| Knob | Moves | Current | ↑ effect |
|------|-------|---------|----------|
| `SLIVER.width` / `SLIVER.height` | box `W` / `H` | 27dp / 38dp | whole shape wider / taller |
| `SPINE.shoulderTop` / `SPINE.shoulderBot` | V1.v / V6.v | 0.166 / 0.833 | more flat edge above / below the fangs |
| `TOOTH1.length` / `TOOTH2.length` | V2.u / V5.u | 0.60 / 0.60 | fang pokes further inward (its tip's horizontal reach) |
| `TOOTH1.tipY` / `TOOTH2.tipY` | V2.v / V5.v | 0.20 / 0.80 | slides the pointed tip **down** (↓ = up) |
| `TOOTH1.thickness` / `TOOTH2.thickness` | V1/V3 spread · V4/V6 spread | 0.114 / 0.113 | fatter/chunkier fang (vertical base span) |
| `GUMS.depth` | V3.u & V4.u | 0.07 | **fatter gums** (bridge bulges inward) |
| `GAP` | V3.v & V4.v spread | 0.44 | **fangs pushed apart** (gap between them) |

> Terminology: a fang's **LENGTH** = how far it pokes in (its tip's horizontal position — there is no separate
> "tipX"); **THICKNESS** = how fat it is; **`tipY`** = the tip's vertical position.
>
> **Runtime:** these knobs (+ color/opacity/size) are user-editable via **Position → Customize** (dialog `D2`)
> and persisted in `SliverConfig` (SharedPreferences). All renderers build from the one shared
> `SliverShape.buildPath`, so the live overlay and both previews always match — there are no longer separate
> hardcoded L/R path copies to keep in sync.

---

## 7. Overlay `TRAY` — expanded shortcut panel
Built in code (`SidebarService.assembleTrayView` / `populateShortcuts`). Appears when `SLIVER` is swiped.

| Code | ID | Property | Value | Notes |
|------|----|----------|-------|-------|
| T0 | `TRAY.window` | Size | **80 × 266 dp** | width `tray_width`; height = sliver H × 7 = 38×7 |
| — | `TRAY.window` | Anchor | `TR`/`TL` (same side as sliver) | TOP + END/START |
| — | `TRAY.window` | **Y position** | `Ytop = sliverY + 19 − 266` | tray **bottom** aligns to sliver vertical center; grows upward |
| — | `TRAY.window` | Enter anim | scaleX 0→1 from edge pivot, 250ms decelerate | "stone door unfurl" + haptic |
| T1 | `TRAY.meander` | Border strip | **12 × MP dp** | `ic_meander_border`, alpha 0.7, on the **inward** edge |
| T2 | `TRAY.scroll` | Scroll area | w=1 × MP | bg `#E6121212`; meander + scroll ordered by side |
| T3 | `TRAY.list` | Icon column | MP × WC | vertical, gravity bottom+center, pad top/bottom 8 |
| T4 | `TRAY.icon[n]` | Shortcut icon | **48 × 48 dp** | margins top/bottom 8; desaturated 80%; reverse order (#1 at bottom) |

Dismiss: `ACTION_OUTSIDE` touch → collapse back to `SLIVER`. Tapping `TRAY.icon[n]` launches the app.

---

## 8. `DIALOG` — modal dialogs

| Code | ID | Dialog | Trigger | Buttons |
|------|----|--------|---------|---------|
| D1 | `DIALOG.discard` | "Discard Changes?" AlertDialog | Back on SHORTCUTS while state is dirty | **Discard** (positive) / **Keep Editing** (negative) |
| D2 | `DIALOG.customizeSliver` | "Customize Sliver" popup (custom view) | `POSITION.btnCustomize` (P6) | **Reset** / **Cancel** / **Apply** |

`D1` is a system-styled `AlertDialog`. `D2` (`dialog_customize_sliver.xml`, driven by `SliverCustomizeDialog.kt`)
is an `AlertDialog` with a custom scrollable stone-panel view: a live `SliverPreviewView`, an **Opacity**
slider, a **Color** toggle (Default grey / Custom → rainbow **Hue** slider + swatch), the eight **fang
geometry** sliders (`LabeledSeekBar`), and two **Size** (width/height dp) fields. Every control edits a working
`SliverConfig`; **Apply** persists it and hot-reloads the overlay via `ACTION_UPDATE_STYLE`. See §6.3 for the
knob↔control mapping.

---

## 9. Master index (all codes)

| Code | ID | Code | ID |
|------|----|----|----|
| M1 | MENU.pillarL | S9b | SHORTCUTS.btnSave |
| M2 | MENU.pillarR | TL0 | TILE.bg |
| M3 | MENU.dustContainer | TL1 | TILE.dragHandle |
| M4 | MENU.headerBar | TL2 | TILE.orderNum |
| M4a | MENU.title | TL3 | TILE.iconRing |
| M5 | MENU.btnShortcuts | TL3a | TILE.icon |
| M6 | MENU.btnPosition | TL4 | TILE.name |
| M7 | MENU.btnDummy | TL5 | TILE.checkbox |
| M8 | MENU.divider | AR1 | APPROW.icon |
| M9 | MENU.btnStartService | AR2 | APPROW.name |
| M10 | MENU.btnStopService | AR3 | APPROW.checkbox |
| M11 | MENU.version | P1 | POSITION.pillarL |
| S1 | SHORTCUTS.pillarL | P2 | POSITION.pillarR |
| S2 | SHORTCUTS.pillarR | P3 | POSITION.headerBar |
| S3 | SHORTCUTS.headerBar | P3a | POSITION.title |
| S3a | SHORTCUTS.ringIcon | P4 | POSITION.view |
| S3b | SHORTCUTS.title | P4a–f | POSITION.(mockup/zones/sliver/trail/instruction) |
| S4 | SHORTCUTS.lblAltar | P5 | POSITION.info |
| S5 | SHORTCUTS.altarPanel | P6 | POSITION.btnCustomize |
| S5a | SHORTCUTS.rvAltar | P7 | POSITION.btnBack |
| | | SL1 | SLIVER.window |
| S5b | SHORTCUTS.tvEmpty | SL2 | SLIVER.fang |
| S6 | SHORTCUTS.divider | T0 | TRAY.window |
| S7 | SHORTCUTS.lblArchive | T1 | TRAY.meander |
| S8 | SHORTCUTS.rvArchive | T2 | TRAY.scroll |
| S9 | SHORTCUTS.actionBar | T3 | TRAY.list |
| S9a | SHORTCUTS.btnBack | T4 | TRAY.icon[n] |
| | | D1 | DIALOG.discard |
| | | D2 | DIALOG.customizeSliver |

**Sliver anatomy sub-codes** (under `SL2 = SLIVER.fang`, see §6.2): components `SP` spine · `F1` top fang ·
`GM` gums · `F2` bottom fang; vertices `C0,V1–V6,C7`; knobs `SLIVER.width/height`, `SPINE.shoulderTop/Bot`,
`TOOTHn.length/tipY/thickness`, `GUMS.depth`, `GAP`.

---

## 10. Shared dimension tokens (`dimens.xml`)
For reference when instructing edits by token:

| Token | Value | Used by |
|-------|-------|---------|
| `stone_button_height` | 56dp | all stone buttons |
| `stone_button_elevation` | 8dp | all stone buttons |
| `stone_button_pressed_translation` | 4dp | press animation |
| `pillar_width` | 32dp | all pillars |
| `sliver_fang_width` / `_height` | 27dp / 38dp | SLIVER |
| `tray_width` | 80dp | TRAY |
| `app_icon_size` | 48dp | APPROW icon, TRAY icons |
| `margin_wide` / `_standard` / `_narrow` | 24 / 16 / 8 dp | button gaps, paddings |
| `text_header` / `_body` / `_caption` | 18 / 16 / 14 sp | type scale |
```
