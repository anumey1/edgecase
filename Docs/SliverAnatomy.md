# EdgeCase — Sliver Anatomy & Tuning Guide

> **Extends:** `SLIVER (SL)` in `Docs/Dimensions.md`.
> **Purpose:** Break the fang-shaped sliver into named, individually addressable parts so that *any*
> instruction — "fatten the gums 2dp", "make the bottom fang shorter", "widen the gap between the fangs" —
> maps to an exact, repeatable geometry edit.
>
> **Rendered in code by:** one shared builder, `SliverShape.buildPath()`, which every renderer calls
> (`ArcSliverView` live overlay + `PositioningView`/`SliverPreviewView` previews). See §7.
>
> **Now runtime-editable:** all knobs below (plus color/opacity/size) are live-editable by the user via
> **Position → Customize**, persisted in `SliverConfig` (SharedPreferences). Editing the code values changes
> the *defaults*; the dialog changes the *saved* values on top of them.
>
> **Last generated:** 2026-07-10

---

## 1. The coordinate model (read first)

The sliver is drawn inside a box **`W` wide × `H` tall**:
- **`W` = 27dp** (`sliver_fang_width`) — the horizontal size.
- **`H` = 38dp** (`sliver_fang_height`) — the vertical size.

Every corner of the shape is a point `(u, v)` in a **normalized, side-independent** system:

| Axis | Symbol | Range | Meaning |
|------|--------|-------|---------|
| Depth (inward) | **`u`** | `0.0 → 1.0` | `0` = the flat **spine** (the screen-edge side). `1` = maximum reach **inward** (toward screen center). Larger `u` = sticks out further into the screen. |
| Vertical | **`v`** | `0.0 → 1.0` | `0` = top of the sliver, `1` = bottom. |

This `(u, v)` model is **the same for the left- and right-edge slivers** — "inward" always means `u→1`,
regardless of which edge the sliver sits on. The code converts per side automatically (§7).

### 1.1 Converting between units
Because `u`/`v` are fractions, you can instruct me in **dp**, **%**, or raw **fraction** — I convert:

| To move by… | Horizontal (depth `u`) | Vertical (`v`) |
|-------------|------------------------|----------------|
| **1 dp** | `Δu = 1 / 27 ≈ 0.037` | `Δv = 1 / 38 ≈ 0.026` |
| **X dp** | `Δu = X / 27` | `Δv = X / 38` |
| **X %** (of the box) | `Δu = X / 100` | `Δv = X / 100` |

So "push the top fang 3dp further inward" = `TOOTH1.length += 3dp` = `Δu = 3/27 ≈ +0.111`.

---

## 2. Anatomy diagram (RIGHT-edge sliver, current shape)

Spine is the flat edge flush with the screen (right side); the two fangs point **inward (left)**.

```
     inward  u=1  ← ────────────────────── →  u=0  spine (screen edge)
                                                    │
 v=0.000  ┌─────────────────────────────────────── C0     ┐
          │                                          │     │ top flat
 v=0.166  │                                   V1 ────┤     ┘  (SPINE.shoulderTop)
           \                                        /│
            \            TOOTH1 (F1)               / │
 v=0.200    V2 ◄──── tip ──────────────────────   /  │
            /                                     /   │
 v=0.280   V3 ───────────────────────────────────    │   ┐ gums top
           │  (u=0.07)                               │   │
 v=0.500   │            GUMS  (GM)                    │   │  bridge / joiner
           │                                         │   │
 v=0.720   V4 ───────────────────────────────────    │   ┘ gums bottom
            \                                     \   │
 v=0.800    V5 ◄──── tip ──────────────────────   \  │
            /            TOOTH2 (F2)               \ │
 v=0.833  │                                   V6 ───┤ │   ┐ (SPINE.shoulderBot)
          │                                          │  │ bottom flat
 v=1.000  └─────────────────────────────────────── C7  ┘
```

> The shape is currently **perfectly vertically symmetric** about `v = 0.5`
> (mirror pairs: `V1↔V6`, `V2↔V5`, `V3↔V4`). Keep this in mind when you want a symmetric change.

---

## 3. Components (IDs)

| Code | ID | Part | Made of vertices | Plain description |
|------|----|------|------------------|-------------------|
| **SP** | `SLIVER.spine` | Spine | C0, V1, V6, C7 (edge `u=0`) | The flat outer edge + the two flat "shoulders" above/below the fangs. |
| **F1** | `SLIVER.tooth1` | Top fang | V1, V2, V6… (V1→V2→V3) | The upper tooth. Tip = **V2**. |
| **GM** | `SLIVER.gums` | Gums / bridge | V3, V4 | The joiner between the two fangs (the wall at `u=0.07`). |
| **F2** | `SLIVER.tooth2` | Bottom fang | V4, V5, V6 | The lower tooth. Tip = **V5**. |

### 3.1 Vertex table (current values)
| Vertex | ID | `(u, v)` | In dp (from spine / from top) | Role |
|--------|----|----------|-------------------------------|------|
| C0 | `SLIVER.c0` | (0.00, 0.000) | 0, 0 | top spine corner |
| V1 | `SLIVER.v1` | (0.00, 0.166) | 0, 6.3 | top fang root (on spine) |
| V2 | `SLIVER.v2` | (0.60, 0.200) | 16.2 inward, 7.6 | **top fang tip** |
| V3 | `SLIVER.v3` | (0.07, 0.280) | 1.9 inward, 10.6 | top fang inner root = gums top |
| V4 | `SLIVER.v4` | (0.07, 0.720) | 1.9 inward, 27.4 | bottom fang inner root = gums bottom |
| V5 | `SLIVER.v5` | (0.60, 0.800) | 16.2 inward, 30.4 | **bottom fang tip** |
| V6 | `SLIVER.v6` | (0.00, 0.833) | 0, 31.7 | bottom fang root (on spine) |
| C7 | `SLIVER.c7` | (0.00, 1.000) | 0, 38 | bottom spine corner |

> **Shared vertices matter:** `V3` belongs to both the top fang *and* the gums; `V4` belongs to both the
> bottom fang *and* the gums. Moving them affects both parts (which is geometrically correct — e.g. widening
> the gap naturally moves the fangs' inner roots). I always keep the shape closed and consistent.

---

## 4. The tuning parameters (the "knobs")

Each knob has an **ID**, the **vertices it moves**, its **current value**, and what **increasing** it does.
Instruct me by knob ID (see §6 grammar). Values shown in fraction (dp).

### 4.1 Whole-sliver size
| Knob | Controls | Current | ↑ increases → |
|------|----------|---------|---------------|
| `SLIVER.width` | box width `W` (all inward depths scale) | 27dp | fangs & whole shape reach further out; sliver gets fatter horizontally |
| `SLIVER.height` | box height `H` (all `v` scale) | 38dp | taller sliver overall |

*(These live in `dimens.xml` + `ArcSliverView.onMeasure` + the `SidebarService` window size + the preview scale — I update all; see §7.)*

### 4.2 Spine / shoulders
| Knob | Vertex(es) | Current | Meaning · ↑ effect |
|------|-----------|---------|--------------------|
| `SPINE.shoulderTop` | V1.v | 0.166 (6.3dp) | Length of flat edge above the top fang. ↑ = more flat above, fang starts lower. |
| `SPINE.shoulderBot` | V6.v | 0.833 (6.3dp from bottom) | Flat edge below the bottom fang. ↓ (toward 1.0) = more flat below. |

### 4.3 TOOTH1 — top fang
| Knob | Vertex(es) | Current | Meaning · ↑ effect |
|------|-----------|---------|--------------------|
| `TOOTH1.length` | V2.u | 0.60 (16.2dp) | How far the fang pokes **inward** (its reach/protrusion). ↑ = longer, pointier fang. |
| `TOOTH1.tipY` | V2.v | 0.20 (7.6dp) | Vertical position of the tip. ↑ = tip slides **down**. |
| `TOOTH1.thickness` | V1.v & V3.v spread | 0.114 (4.3dp) | Vertical "fatness" at the base (V3.v − V1.v). ↑ = fatter/chunkier fang (moves V1 up and V3 down symmetrically by default). |
| `TOOTH1.rootOuterY` | V1.v | 0.166 | Fine control: upper (on-spine) root only. |
| `TOOTH1.rootInnerY` | V3.v | 0.280 | Fine control: inner (by-gums) root only. |

### 4.4 GUMS — the bridge between the fangs
| Knob | Vertex(es) | Current | Meaning · ↑ effect |
|------|-----------|---------|--------------------|
| `GUMS.depth` | V3.u & V4.u | 0.07 (1.9dp) | How far the gum wall bulges **inward** from the spine. ↑ = **fatter gums** (more material between the fangs, shallower recess). ↓ toward 0 = fangs separated by a deep, near-empty notch. |
| `GAP` | V3.v & V4.v spread | 0.44 (16.7dp) | **Gap between the fangs** = vertical distance between the fangs' inner roots (V4.v − V3.v). ↑ = fangs pushed apart (V3 up, V4 down, symmetric by default). |
| `GUMS.top` | V3.v | 0.280 | Fine control: top of the bridge only. |
| `GUMS.bottom` | V4.v | 0.720 | Fine control: bottom of the bridge only. |

### 4.5 TOOTH2 — bottom fang
| Knob | Vertex(es) | Current | Meaning · ↑ effect |
|------|-----------|---------|--------------------|
| `TOOTH2.length` | V5.u | 0.60 (16.2dp) | Inward reach of the bottom fang. ↑ = longer. |
| `TOOTH2.tipY` | V5.v | 0.80 (30.4dp) | Vertical position of the bottom tip. ↑ = tip slides **down**. |
| `TOOTH2.thickness` | V4.v & V6.v spread | 0.113 (4.3dp) | Vertical fatness at the base (V6.v − V4.v). ↑ = fatter fang. |
| `TOOTH2.rootInnerY` | V4.v | 0.720 | Fine control: inner (by-gums) root only. |
| `TOOTH2.rootOuterY` | V6.v | 0.833 | Fine control: lower (on-spine) root only. |

---

## 5. Disambiguating "width" and "height" of a fang

A fang points sideways, so plain "width"/"height" are ambiguous. **Use these exact terms** and there's no doubt:

| You want to… | Say | Knob |
|--------------|-----|------|
| make a fang poke out **further / less** (its length) | **LENGTH** | `TOOTHn.length` |
| make a fang **fatter / thinner** (chunkier body) | **THICKNESS** | `TOOTHn.thickness` |
| move a fang's **pointed tip up/down** | **TIP** | `TOOTHn.tipY` |
| make the fang **sharper / blunter** | sharpen/blunt (I adjust length vs thickness ratio) | `length` ↑ + `thickness` ↓ = sharper |

> When you said *"increase the width of a fang"* → that's almost certainly **THICKNESS** (fatter). If you
> instead mean "make it stick out more," say **LENGTH**. When you said *"decrease the height of the second
> fang"* → that maps to **`TOOTH2.thickness`** (its vertical span). If you meant "move its tip up," say
> **`TOOTH2.tipY`**.

---

## 6. How to instruct me (grammar)

**Form:** `<KNOB> <op> <value><unit> [modifier]`

- **`<op>`**: `+=` (increase) · `-=` (decrease) · `=` (set absolute).
- **`<unit>`**: `dp` (default, exact), `%` (of the box), or `u`/`v` (raw fraction).
- **`[modifier]`** (optional, for spread knobs like thickness/GAP): `symmetric` (default), `from-top`,
  `from-bottom`, `from-tip`. Controls which side absorbs the change.
- **Target** (optional): geometry now flows through one builder + `SliverConfig`, so a change lands everywhere
  at once. Say `default` to change the baked-in default (`SliverConfig.kt`, affects everyone) or `saved` to
  change the persisted value (as the Customize dialog does). Default assumption: `saved`.

### 6.1 Worked examples (covering your scenarios)
| Your intent | Instruction |
|-------------|-------------|
| Increase width (fatness) of the top fang by 2dp | `TOOTH1.thickness += 2dp` |
| Make the top fang poke out 4dp more | `TOOTH1.length += 4dp` |
| Decrease the height (span) of the second fang by 3dp | `TOOTH2.thickness -= 3dp` |
| Move the bottom fang's tip up by 2dp | `TOOTH2.tipY -= 2dp` |
| Fatten the gums by 1.5dp | `GUMS.depth += 1.5dp` |
| Widen the gap between the fangs by 5dp | `GAP += 5dp` |
| Narrow the gap, but only from the top fang side | `GAP -= 3dp from-top` |
| Make the whole sliver 4dp wider | `SLIVER.width += 4dp` |
| Make the top fang sharper | `TOOTH1.length += 3dp` and `TOOTH1.thickness -= 1dp` |
| Set the bottom tip exactly at 75% height | `TOOTH2.tipY = 75%` |
| Give the top a longer flat shoulder | `SPINE.shoulderTop += 2dp` |

You can also just talk plainly ("make the fangs longer and further apart") — I'll translate to these knobs
and, if it's ambiguous, name the exact knobs I'm about to change before doing it.

---

## 7. Where the geometry lives (so changes stay consistent)

The `(u, v)` point set now lives in **one place** — `SliverShape.buildPath(path, w, h, side, cfg)` — which
every renderer calls, so the live overlay and both previews are always identical by construction (no more
hardcoded L/R copies to keep in sync):

| # | Renderer | Uses |
|---|----------|------|
| 1 | `ArcSliverView` (live overlay window) | `SliverShape.buildPath(...)` + `cfg.fillColor()` |
| 2 | `PositioningView` (Position-screen preview) | same builder + `cfg.fillColor()` |
| 3 | `SliverPreviewView` (Customize-dialog preview) | same builder + `cfg.fillColor()` |

Per-side conversion is inside the builder: `RIGHT → x = w·(1−u)`, `LEFT → x = w·u`, both `y = h·v`.

- The knob values come from a **`SliverConfig`** (defaults in `SliverConfig.kt`, overridable + persisted via
  the Customize dialog). `SliverShape` coerces the vertices into `[0,1]` and preserves order, so no combination
  can invert the shape.
- **Color/opacity:** `cfg.fillColor()` = base color (grey default, or `HSV(hue,1,1)` custom) with `opacity`
  as its alpha.
- **`SLIVER.width`/`SLIVER.height`** feed `SliverConfig.widthDp/heightDp`, which drive
  `ArcSliverView.onMeasure`, the overlay window size in `SidebarService.instantiateWindowParameters`
  (`cfg.widthDp/heightDp × densityDpi`, plus the tray height derived from it), and the preview aspect.
  `dimens.xml` `sliver_fang_width`/`_height` (27/38) remain only as the documented defaults.

---

## 8. Quick reference — knob cheat sheet

```
WHOLE:   SLIVER.width (27dp)   SLIVER.height (38dp)
SPINE:   SPINE.shoulderTop (0.166)   SPINE.shoulderBot (0.833)
TOOTH1:  .length (0.60)  .tipY (0.20)  .thickness (0.114)  .rootOuterY (0.166)  .rootInnerY (0.28)
GUMS:    .depth (0.07)   GAP (0.44)   .top (0.28)   .bottom (0.72)
TOOTH2:  .length (0.60)  .tipY (0.80)  .thickness (0.113)  .rootInnerY (0.72)  .rootOuterY (0.833)
```
Every value is a fraction of the box; multiply by 27 (horizontal) or 38 (vertical) for dp.
```
