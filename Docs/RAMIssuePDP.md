# RAM Issue — Phased Development Plan

> **Target release:** EdgeCase **1.6.1** (versionCode **7**)
> **Written:** 2026-09-25 · **Status:** ⏸ **PAUSED after Phase 3 (maintainer's request, 2026-09-25 ~19:45 IST).** Phases 0–3 done and
> approved; Phase 4 is next. **Start with "▶ Resume here" below.**
> **Device of record:** Pixel 9 Pro XL (`komodo_beta`), Android 17, serial `46281FDAS008EG`
> **Rules for this plan:** no `git commit` / `git push` at any point (the maintainer commits). Work
> stops at the end of every phase for review, and the next phase starts only when the maintainer
> says so.

---

## Progress tracker

| Phase | Title | Status | Review gate |
|---|---|---|---|
| 0 | Pre-flight: device state, signing, baselines | ✅ Done | ⏸ Stop for review |
| 1 | Spike: prove the cross-process mechanics on the device | ✅ Done: D5 change approved | ⏸ Stop for review — **decides Phase 2's design** |
| 2 | Process split, state sync, restart bug | ✅ Done | ⏸ Stop for review |
| 3 | Animations: tap cracks removed, everything else as light as possible | ✅ Done: visual sign-off given | ⏸ Stop for review — **visual sign-off** |
| 4 | Icon loading: labels up front, icons on demand | ⬜ Not started | ⏸ Stop for review |
| 5 | Notification decision and privacy-policy correction | ⬜ Not started | ⏸ Stop for review — **you deploy the policy** |
| 6 | 1.6.1 release candidate: build, full regression, docs | ⬜ Not started | ⏸ Stop for review — **you commit and upload** |

Legend: ⬜ not started · 🟨 in progress · ✅ done · ❌ failed / blocked · ⏸ review gate

---

## ▶ Resume here (session handoff, 2026-09-25 ~19:45 IST)

Work paused at the maintainer's request after Phase 3's sign-off. Everything needed to continue is
in this section; the detailed evidence sits under each phase below.

### Where things stand

| | State |
|---|---|
| **Phases 0–2** | ✅ Done, reviewed, and **committed by the maintainer** as `1b01f6a` *"Memory Bug Fixes Phases 0 to 2"* (on top of `4206412`) |
| **Phase 3** | ✅ Done and **approved**, **not committed**. Working tree: `D CrackFlashView.kt` (staged by `git rm`), `M DustParticleView.kt`, `M MainActivity.kt`, `M ObsidianCrackView.kt`, `M ServiceEyeView.kt`, `M layout_screen_main_menu.xml`, `?? TempleClock.kt`, `M Docs/RAMIssuePDP.md` |
| **Phase 4** | ⬜ **Next.** Icon loading (labels up front, icons on demand, bounded cache). Design and tasks are in the Phase 4 section; the cache size is already computed from the measured density (0.4: 108 px icons, 64-icon cache ≈ 2.99 MB) |
| **Phases 5–6** | ⬜ Not started. Phase 5's device fact is already known (see "Facts gathered early" below) |
| **Version** | Still `versionCode 6 / 1.5.2` in `app/build.gradle.kts`. The bump to **7 / 1.6.1** is Phase 6's first task, deliberately not done yet |
| **Phone** | Running the **Phase 3 build** (release, upload-key signed, installed 19:15 with `adb install -r`; still reports versionName 1.5.2). Sliver running, app closed, all settings as recorded in Phase 0.6 |
| **Emulator** | Stopped. Start it headless (`emulator -avd Pixel_9_Pro_XL -no-window -no-audio -no-boot-anim -no-snapshot-save`) only for instrumented tests |
| **Play** | 1.5.2 live. Nothing uploaded from this plan yet |
| **Anumey's Lair** | Untouched so far (Phase 5 edits it) |

### What 1.6.1 already contains (Phases 2 + 3), in one paragraph

`SidebarService` runs in its own `:overlay` process (~26 MB, always resident); the main process
(settings UI, ads SDK, Chromium) becomes a cached, freezable, reclaimable process once the user
leaves. Settings reach the overlay as an `OverlaySnapshot` inside every Intent. The Activity binds
without `BIND_AUTO_CREATE` while resumed, which keeps the sliver hidden over the settings screens
(and through restarts), and a guarded `syncOverlay()` means saving settings never restarts a
stopped sliver (the 1.5.2 bug). All overlay windows are tracked by explicit flags (the fix for the
leaked-window bug found in V4). The tap crack flash is gone; the gem background and the eyes'
breathing share a 12 fps `TempleClock` that stops when the app is hidden; dust is time-based.
Measured: always-resident memory **182 MB → 25.9 MB**; idle settings-screen CPU **28–35 % → 10–12 %**
of one core with the ad quiet; **23/23** instrumented tests pass.

### Facts gathered early for later phases

- **Phase 5 (5.3):** on Android 17 the running service appears in Quick Settings → **"N apps are
  active"** → a dialog titled **"Active apps"** (*"These apps are active and running, even when
  you're not using them…"*) listing **EdgeCase** with a **Stop** button. Pressing Stop ends every
  EdgeCase process (verified in V8). Use this wording for the policy.
- **Possible follow-up (not in scope, not verified):** the banner was serving a **video** creative
  during Phase 3, which alone drives the window at 22–56 fps and up to ~72 % of a core. Check
  whether the AdMob banner unit can exclude video ads.
- **Not yet covered by any phase:** the app-open memory is a little higher than 1.5.2 (the extra
  `:overlay` process, 17.5–25.9 MB), and the cached main process freezes before its GC runs, so it
  holds ~219 MB (reclaimable) until Android needs the memory. Phase 4 removes the ~79 MB of icons
  that make up much of that.

### Device-testing lessons (read before touching the phone)

1. **Always set `ANDROID_SERIAL=46281FDAS008EG`** once the emulator is running, or adb/gradle may
   hit the wrong device. **Never install a debug build or run `connected*` tests on the phone:** the
   signature differs from the upload-key build, and the only way round it (uninstall) wipes the
   maintainer's settings.
2. **Install with `./gradlew assembleRelease` + `adb install -r`**, then launch and check logcat
   (`AndroidRuntime|FATAL`). A green build proves nothing about launching.
3. **"Is the service running?"** Use `state.sh` (Appendix D). The record has
   `app=ProcessRecord`. **Don't** grep-count `/.SidebarService` in `dumpsys activity services`: the
   binding creates a record even for a stopped service, so that count is meaningless.
4. **Sliver window check:** `dumpsys window windows | grep -cE 'Window\{[0-9a-f]+ u0 com.dicereligion.edgecase\}'`
   counts only the overlay windows (the Activity window has `/…MainActivity` in its name).
5. **Crash tests:** space `am crash` calls **≥ 3 minutes apart** and leave the app in the background
   when possible. Repeated crashes trigger Android's crash-loop protection (the service isn't
   restarted), and a crash while the app is in front shows a dialog that a Home press dismisses as
   "close app". To kill the main process, use `am kill com.dicereligion.edgecase` with the app
   closed (a background kill), not `am crash`.
6. **Navigation traps:** `am start` of an existing task returns to the *last screen shown*, not the
   main menu; check `topResumedActivity` and the visible ids (`uiautomator dump`) before tapping. A
   BACK press on the main menu leaves the app.
7. **CPU samples must be classified by the ad:** a video creative redraws the whole window. Use
   `quiet.sh` (Appendix D); only `QUIET` samples (`Chrome_InProcGp` < 20 ticks) show the app's own
   cost.
8. **Cached processes freeze:** `meminfo` then shows `Act=- WV=-`, and PSS stays constant. That's
   expected, not a hang.
9. **Restoring the maintainer's settings:** style → Customize → RESET → APPLY (they're on defaults);
   shortcuts → only add/remove the *last* entry so the order is preserved; position → drag the green
   arrow horizontally at the same height (restored to the identical pixel frame
   `[948,1649][1008,1734]` in Phase 2).
10. The session scratchpad (raw dumps, screenshots, recordings) is **not permanent**; the scripts
    that matter are reproduced in Appendices B–D.

### To resume

1. Connect the phone; `adb devices -l` shows `46281FDAS008EG`; check that the installed build is
   still the Phase 3 build (`dumpsys package com.dicereligion.edgecase | grep lastUpdateTime` →
   2026-09-25 19:15:15), or rebuild the working tree and `install -r`.
2. Recreate the helper scripts from Appendices B–D in a scratch folder; `chmod +x`.
3. Ask the maintainer whether to commit Phase 3 first (their call; Claude never commits).
4. Start **Phase 4** at task 4.1.

---

## 1. Goals

The maintainer's goal: **find the real causes of EdgeCase's memory and performance cost and fix
them properly, without creating new problems.** In concrete terms:

| # | Goal | Measured today (§2) | Done when |
|---|---|---|---|
| G1 | The part of EdgeCase that must run all the time (the sliver) holds only what the sliver needs | **188.6 MB PSS** stays permanently after the app has been opened once | Once you leave the app, the always-running process holds roughly what the sliver-only process measured (**25.9 MB**), and the rest becomes normal, reclaimable background memory. Numbers are measured, not assumed |
| G2 | Android can reclaim the settings screen and ads memory like any other app | Refused: *"Unable to set a background trim level on a foreground process"* | After leaving the app, the main process goes to cached priority and accepts `send-trim-memory` |
| G3 | The settings screens don't burn CPU when nothing is happening, and every animation is as light as it can be without losing the look | **29–35 % of one core** idle on the main menu, spiking to **71 %** | Tap crack flash removed; idle cost clearly below baseline, with frame rate and CPU measured, and the look and frame rate signed off by the maintainer |
| G4 | Opening the app doesn't load every installed app's icon | **278 icons, ~79 MB** of bitmaps on every launch | Main menu loads no app icons; the Shortcuts screen holds a bounded icon cache |
| G5 | Saving settings never restarts a sliver the user stopped | **Confirmed bug**: SAVE with the sliver stopped restarted it | Stopped means stopped, whatever the user saves |
| G6 | The running-service disclosure is truthful | The published policy says *"which is why you see one"*, but on Android 13+ users don't see one | Policy wording matches what the app actually does |

### Non-goals (explicitly out of scope)

- Anything that changes how the sliver, tray, positioning or customisation look or behave (apart
  from G3's agreed trades: the lower frame rate of the two never-ending animations and the
  removal of the tap crack flash).
- Dropping the foreground service (see §4, rejected alternative R2).
- The ad SDK's own memory while the app is open. Chromium is the ad's cost; G1/G2 make it
  reclaimable instead of trying to shrink it.
- Why one `WebView` survives `AdHost.destroy()` (§2.3). Once the split lands it lives in a
  reclaimable process, so it stops mattering. Recorded, not chased.
- Known Limitations #1–#9, #12, #13 in `stats.md` §10. The tray-icon `mutate()` hazard (#3) is
  noted in §9 because Phase 2 touches that code, but it is not fixed here.

---

## 2. Verified findings (the evidence this plan is built on)

All measured on 2026-09-25 against the installed **1.5.2 / versionCode 6** (sideloaded, upload-key
signed). PSS (proportional set size) is the fair measure: shared pages are split between the
processes that share them. RSS is shown because external tools report it. Raw `dumpsys meminfo`
output for every stage was saved during the session; the measurement script is Appendix B.

### 2.1 Memory by stage

| Stage | Main process PSS | Chromium renderer PSS | Total PSS | Notes |
|---|---|---|---|---|
| As found (service up 6h38m, app closed) | 132.1 MB | 36.6 MB | **168.7 MB** | plus **95.6 MB** of these pages in compressed swap |
| App open, first seconds | 785.4 MB | 106.7 MB | ~892 MB | transient; `.apk mmap` 444 MB, mostly private clean (droppable) file pages |
| App open, settled (3 samples) | 304–321 MB | 94–107 MB | **~400–430 MB** | RSS ~517 + ~234 MB |
| Home pressed, Activity alive (30 s–2 min) | 224.3–225.2 MB | 67.6–73.2 MB | **~295 MB** | steady |
| Swiped from Recents (Activity destroyed), 2 and 5 min | 128.0 MB | 60.6 MB | **188.6 MB** | steady; 1 `WebView` object still alive; renderer still alive |
| **Sliver only** (fresh process, app never opened) | **25.9 MB** | none | **25.9 MB** | no WebView, no renderer, sliver attached and working |

The "sliver only" process came from deliberately crashing the main process (`am crash`), after
which Android restarted the `START_STICKY` service by itself in a fresh process (*"Start proc
24136 … for started-service"*). The renderer did **not** come back. This is the best available
stand-in for the post-split overlay process. The real split should do slightly better, because
the stand-in still ran `androidx.startup` (the WorkManager database was open in it) and a
`:overlay` process will not (§2.5).

### 2.2 Priority and reclaimability

- `dumpsys activity oom`: main process `prcp` (perceptible) **FGS**; the renderer `prcp + 1`
  because the main process is bound to it.
- `am send-trim-memory <pid> COMPLETE` → `IllegalArgumentException: Unable to set a background trim
  level on a foreground process`. While the sliver runs, Android treats the whole process as
  foreground and won't even ask it to shrink.

### 2.3 What is in the retained memory

- After the Activity is destroyed: `Activities: 0`, `WebViews: 1`, `ViewRootImpl: 1` (the sliver).
- `AdHost.destroy()` (`AdHost.kt:145`) removes and destroys the `AdView`, yet a WebView object and
  the renderer process both survive. The ads SDK initialises once per process
  (`sdkInitialized` is a static `AtomicBoolean`, `AdHost.kt:49`).
- The Chromium renderer is a child of the main process (`SandboxedProcessService0:0 <= Proc{main}`).

### 2.4 CPU

| Situation | CPU | Frames |
|---|---|---|
| App closed, sliver running (as found) | 7 ticks / 60 s = **0.12 %** of one core; renderer 0 | none |
| App closed after Home (fresh run) | 7 ticks / 30 s = **0.23 %** | none |
| Main menu, idle, eyes open | **26–29 %** of one core | 1851 frames / 30 s ≈ **62 fps**, 0 % janky |
| Main menu, idle, eyes closed (3 × 20 s) | **29 %, 71 %, 35 %** | continuous |

Per-thread, idle baseline sample: `RenderThread` 257, UI thread 147, `binder` 34, `mali-event-hand`
33 ticks per 20 s. The 71 % sample was dominated by `RenderThread` 511, UI thread 221,
`Chrome_InProcGp` 203 and `VizWebView` 116, which is the ad creative redrawing.

**Attribution (by elimination, not measured in isolation):**
- Closing the eyes did not lower the baseline, so the eyes are not the driver *today*.
- `DustParticleView` and `CrackFlashView` stop themselves when idle.
- The Chromium threads are small in the baseline samples.
- That leaves `ObsidianCrackView`, whose `ValueAnimator` is infinite and calls `invalidate()` on
  every frame (`ObsidianCrackView.kt:88–99`). Because it sits behind every view, each of those
  frames redraws the whole window.
- **But `ServiceEyeView` does the same thing** (`ServiceEyeView.kt` `startIfNeeded`, infinite
  animator, `invalidate()` every frame while running). Slowing only the background would leave the
  eyes driving 60 fps. This shapes Phase 3.
- Per-frame allocations in `ObsidianCrackView.onDraw`: 11 new `RadialGradient`s, an 11-path
  rebuild of static geometry, and 22 `ContextCompat.getColor` calls, every frame.

### 2.5 Other verified facts

| Claim | Verdict | Evidence |
|---|---|---|
| The sliver is drawn with Chromium | **False** | Sliver-only process: `WebViews: 0`, no renderer |
| The notification is hidden | **True** | `POST_NOTIFICATION: ignore` app-op; 0 `NotificationRecord`s while the FGS ran; `POST_NOTIFICATIONS` not declared |
| Something starts EdgeCase at boot | **False** | `query-receivers BOOT_COMPLETED` returns nothing for EdgeCase (WorkManager's `RescheduleReceiver` is `enabled="false"`); no `RECEIVE_BOOT_COMPLETED`; today's service record shows `createdFromFg=true`, created 1 h after boot |
| Saving settings restarts a stopped sliver | **True (confirmed on device)** | Stop → Shortcuts → SAVE with no changes → service record back, `createTime=-2s`. The cause: `MainActivity.kt:402`, `:584` and `:610` call `startService()` without a running check |
| Idle cost while the app is closed | **Negligible** | 0.12–0.23 % CPU; no EdgeCase wakelocks, alarms or jobs; Doze whitelist entry is `user` (the battery-optimisation grant) |
| The only content provider runs in the main process only | **True** | Merged release manifest: `androidx.startup.InitializationProvider` (emoji2, WorkManager, ProcessLifecycle, ProfileInstaller) has no `android:process`; no component anywhere sets one |
| Icons are loaded for every launchable app on every launch | **True** | `preloadApps()` in `onCreate` → `getInstalledApps()` calls `ri.loadIcon(pm)` for each of **278** entries; foreground `Bitmap (malloced)`: **299 / 79.2 MB**; after the Activity was destroyed and GC ran: **11 / 5.8 MB** |
| `AppInfoData.icon` is read outside the two adapters | **False** | Only `ActiveShortcutsAdapter.onBindViewHolder` and `AvailableAppsAdapter.onBindViewHolder` |
| `getRunningServices()` still works for the app's own services | **Documented true** | SDK source `android-36.1/android/app/ActivityManager.java`: *"no longer available to third party applications. For backwards compatibility, it will still return the caller's own services."* Cross-process timing still to be proven on the device in **Phase 1** |
| A foreground service needs a *visible* notification | **False** | Android: *"Apps don't need to request the POST_NOTIFICATIONS permission in order to launch a foreground service. However, apps must include a notification … if the user denies the notification permission, they still see notices related to foreground services in the Task Manager but don't see them in the notification drawer."* Play: users must be aware the FGS is running, *"through user initiation or notification"* ([Play Console Help 13392821](https://support.google.com/googleplay/android-developer/answer/13392821?hl=en)). EdgeCase's FGS is started by the user tapping START, and the sliver itself is always on screen |
| The published policy is accurate about the notification | **False on Android 13+** | Anumey's Lair `src/app/legal/edgecase/privacy/page.tsx:102`: *"Android requires a permanent notification for this, which is why you see one"* |

---

## 3. Decisions

| # | Decision | Chosen | Source |
|---|---|---|---|
| D1 | How to cut the animation cost | **Remove the tap crack flash.** The two never-ending animations (gems, eyes) share **one** clock at **12 fps** (decided by the maintainer), with nothing recomputed per frame that can be computed once. Dust becomes time-based. Interaction-only animations unchanged | Maintainer, 2026-09-25 (lower frame rate; then "even lighter… get rid of the tap animations"; then "keep it 12") |
| D2 | Notification visibility | **Don't request `POST_NOTIFICATIONS`.** The FGS must still *post* a notification (Android requires it), but without the permission Android 13+ keeps it out of the shade and lists EdgeCase only in the Task Manager (Quick Settings' active-apps list). Android 11–12 have no such permission and keep showing the existing low-priority notification. This is today's behaviour, now chosen deliberately | Maintainer asked *"Can we not show it?"*. §2.5 shows that's permitted, so it's recorded here. **Confirm or overturn at the Phase 5 gate** |
| D3 | START behaviour without notification permission | **Moot under D2**: nothing is requested | — |
| D4 | Privacy policy | **I edit the Anumey's Lair source; the maintainer commits, pushes and deploys** | Maintainer, 2026-09-25 |
| D5 | Cross-process "is the sliver running / is the app in front?" | **Changed after Phase 1 (approved by the maintainer, 2026-09-25):** the Activity **binds** to the service without `BIND_AUTO_CREATE` while resumed. That tells the service to stay hidden and guards updates. `getRunningServices()` is used **only** for the eyes' state on resume. See Phase 1 results | Phase 1 S1 failed, S2 and S7 passed |
| D6 | Cross-process settings delivery | **Every intent to the service carries a full settings snapshot**; the service reads SharedPreferences only on a sticky restart, which always happens in a fresh process | This plan (§5.3) |

---

## 4. Rejected alternatives (and why)

| # | Alternative | Why not |
|---|---|---|
| R1 | Keep one process; tear down the ads SDK / WebView when the Activity closes | There is no public API to unload Chromium from a process. `AdView.destroy()` is already called and a WebView and the renderer still survive (§2.3). The only reliable way to release a process's memory is to let the process die, which the FGS prevents |
| R2 | Drop the foreground service entirely so no notification exists | A process with a visible overlay window gets elevated priority in AOSP, but **that is not documented behaviour**. The documented foreground conditions are a visible activity, a foreground service, or a foreground client bound to it. Relying on internals for the app's core feature, on every OEM's Android, is exactly the "more issues later" this plan must avoid. It would also mean re-filing the Play FGS declaration |
| R3 | `MODE_MULTI_PROCESS` SharedPreferences | Deprecated since API 23 with an explicit *"does not work reliably"* warning |
| R4 | Share state through a `ContentProvider` | Correct, but a lot of new surface for 19 keys that already have a single writer. D6 gets the same correctness with far less code |
| R5 | Keep 60 fps and only remove allocations | Removes UI-thread work but not the full-window redraw that dominates `RenderThread`. The maintainer picked D1 |
| R6 | Stop preloading entirely (load the list only when Shortcuts opens) | Would bring back the slow open that v1.4.x's preload was added to fix. Loading labels up front and icons on demand keeps the fast open without the 79 MB |

---

## 5. Target design

### 5.1 Process layout after 1.6.1

```
┌───────────────────────────── com.dicereligion.edgecase (main) ─────────────────────────────┐
│ MainActivity · AdHost · GMA Next-Gen SDK · UMP · Chromium browser side · WorkManager/startup │
│ Lifetime: while the user is in the app; afterwards an ordinary CACHED process that Android   │
│ may trim, freeze or kill. Its Chromium renderer child dies with it.                          │
└───────────────────────────────┬─────────────────────────────────────────────────────────────┘
                                │ startForegroundService / startService (Intents + snapshot)
                                │ bindService(flags=0) while the settings screen is resumed
                                ▼
┌──────────────────────── com.dicereligion.edgecase:overlay ─────────────────────────────────┐
│ SidebarService (FGS, specialUse) · ArcSliverView · tray · SliverShape/SliverConfig          │
│ No ads, no WebView, no androidx.startup providers. Always running while the sliver is on.   │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 What currently assumes one process (the complete list)

Found with `grep -rn "isRunning\|isForeground\|getSharedPreferences" app/src/main/java`:

| Site | Assumption | Replacement |
|---|---|---|
| `SidebarService.companion.isRunning` (`@Volatile` static), written `SidebarService.kt:67/121` | Readers share the static | Binding state in `MainActivity` (connected ⇔ running), plus `SidebarService.isRunning(context)` → `getRunningServices` for the eyes on resume (D5) |
| `MainActivity.kt:161` eyes sync, `:180` suspend guard | Reads that static | Eyes: the helper plus binding callbacks. Suspend guard: **deleted**, since binding replaces SUSPEND/RESUME |
| `MainActivity.companion.isForeground`, read by `SidebarService.kt:102` | Service reads the Activity's static | The service's own `bound` flag, set in `onBind` / `onRebind` and cleared in `onUnbind` (arrives before `onStartCommand`, S7a) |
| `SidebarService.loadPositionFromPrefs()` (`:261`), `SliverConfig.load(this)` (`:81` in `onCreate`, and again in `applySliverUpdate`), `populateShortcuts()` (`:369`) | The service's prefs cache reflects the Activity's writes | Snapshot from the intent (D6) |
| `MainActivity.kt:402`, `:584`, `:610` (`startService` for STYLE / POSITION / SHORTCUTS) | Harmless if the service is stopped (**wrong today: this is the restart bug**) | Send only when running; carry the snapshot |

`SliverCustomizeDialog.kt:45`, `ShortcutStateManager` and `SliverConfig.save/load` run in the
Activity's process only, so they are unaffected.

### 5.3 Snapshot protocol (D6)

- A new `OverlaySnapshot` (side, yBias, `SliverConfig`, ordered shortcut list) with
  `fromPrefs(context)`, `writeTo(intent)` and `fromIntent(intent)`. `fromPrefs` absorbs the three
  prefs readers now in `SidebarService`, including the legacy `saved_shortcuts` set fallback and
  `SliverConfig.load`'s drawer-height seeding, so nothing is lost.
- `SliverConfig` gains `toBundle()` / `fromBundle()`, reusing its existing `K_*` keys.
- **Every** intent to the service carries a snapshot, built from the Activity's own prefs cache
  right after writing. `apply()` updates the in-memory map synchronously, so the snapshot is
  always current.
- Service actions after the change:
  - `ACTION_START`: snapshot
  - `ACTION_SYNC_STATE`: snapshot. Replaces UPDATE_SHORTCUTS / POSITION / STYLE; all three
    already end in the same rebuild
  - `ACTION_SUSPEND_OVERLAY`, `ACTION_RESUME_OVERLAY`: **deleted**, since the binding replaces them
    (Phase 1 D5 change)
- A `null` intent means a `START_STICKY` restart, which by definition happens in a new process, so
  that process's first `getSharedPreferences` reads the file fresh → `OverlaySnapshot.fromPrefs`.
- This also fixes the stale-cache case where the overlay process is still alive after STOP: the
  next START carries fresh values instead of reading that process's old cache.

---

## 6. Phases

Every phase ends at a **⏸ review gate**: I report what was done, what was measured and anything
unexpected, update the tracker, and stop.

### Standing rules for all phases

- **Never `git commit` / `git push`** (either repository).
- **Device builds are signed release builds** (`./gradlew assembleRelease`) installed with
  `adb install -r`. A debug build carries a different signature, would refuse to install over the
  phone's upload-key build, and the only way round that (uninstalling) wipes the maintainer's
  shortcuts and sliver settings.
- **Instrumented tests run on the emulator only** (`Pixel_9_Pro_XL`, API 37), with
  `ANDROID_SERIAL=<emulator serial>` set. Gradle's `connected*` tasks otherwise target every
  attached device, including the phone.
- **Protect the maintainer's settings.** Before any step that changes position, style or
  shortcuts on the phone, take screenshots of the Positioning screen, the Customize dialog and the
  Shortcuts list; restore afterwards and say so.
- **A green build proves nothing about launch** (the stats.md §4 R8 lesson). Every device build is
  launched and checked in logcat.
- **Standing checks** (stats.md Appendix C, group C) are re-run in Phase 6: the overlay grep, `AdHost`
  referenced only from `MainActivity`, no networking or file output in app code, merged-manifest
  permissions unchanged.

---

### Phase 0 — Pre-flight: device state, signing, baselines ✅

**Purpose:** confirm the ground this plan stands on is still true on the day work starts.

- [ ] **0.1** Connect the phone; `adb devices -l` shows `46281FDAS008EG`.
- [ ] **0.2** Record the installed build: `dumpsys package com.dicereligion.edgecase` → versionCode,
      versionName, installer. Expected: 6 / 1.5.2 / `null` (sideloaded).
- [ ] **0.3** **Signing check.** `adb pull` the installed `base.apk`; `apksigner verify --print-certs`.
      It must match the local upload key, SHA-256
      `22:09:30:95:AA:7A:3D:EC:FC:3C:EA:8A:16:78:0A:06:F2:DE:F1:59:59:9B:CE:16:70:9E:6D:41:43:C1:1D:DC`
      (what `app/build/outputs/apk/release/app-release.apk` carries today). **If it doesn't
      match, stop:** in-place updates would fail and the test plan needs rethinking.
- [ ] **0.4** Record `wm density` and `wm size`. Phase 4 sizes its icon cache from the density,
      so it's read here, not assumed.
- [ ] **0.5** Ask the maintainer about Play: is 1.5.2 live or still in review? This affects Phase 6
      only (whether 1.6.1 replaces a pending review).
- [ ] **0.6** Screenshot the maintainer's current settings (Positioning, Customize dialog values,
      Shortcuts list) and save them to the scratchpad.
- [ ] **0.7** Re-run the baseline protocol (Appendix A) on the unmodified 1.5.2 so before/after
      figures come from the same day and device. Stages: app open settled; Home + 2 min; Recents
      swipe + 5 min; main-menu idle CPU (3 × 20 s, per thread); app-closed CPU (60 s).
- [ ] **0.8** Record that `adb shell dumpsys window windows` shows exactly one EdgeCase window
      with the app closed.

#### Phase 0 results (2026-09-25)

| Step | Result |
|---|---|
| 0.1 | ✅ `46281FDAS008EG` connected (`komodo_beta`, Pixel 9 Pro XL) |
| 0.2 | ✅ versionCode 6 / 1.5.2, installer `null` (sideloaded), last updated 2026-09-15 21:02 |
| 0.3 | ✅ **Signer matches.** Installed `base.apk` and local `app-release.apk` both: `CN=Dice Religion, OU=EdgeCase`, SHA-256 `22093095aa7a3decfc3cea8a16780a06f2def159599bce16709e6d4143c11ddc` (the upload key). `adb install -r` updates in place and keeps data |
| 0.4 | ✅ `wm density` **360** (2.25×), `wm size` **1008×2244** (the phone's lower resolution setting; all measurements in this plan are at this setting), rendering at **120 Hz**. Phase 4 icon size: 48dp × 2.25 = **108 px**; 64-icon cache = 64 × 108² × 4 B = **2.99 MB** |
| 0.5 | ✅ **1.5.2 is live on Play** (maintainer, 2026-09-25). 1.6.1 will be a normal update over a live release, not a replacement of a pending review. The phone still runs the *sideloaded* upload-key 1.5.2, which is fine for this plan's in-place test installs |
| 0.6 | ✅ Settings captured (screenshots in the session scratchpad `settings/`). **Everything at factory defaults** except position and shortcuts: side **Right**, **79 %** from top; opacity 50 %, Default grey, thick 0.11/0.11, length 0.60/0.60, angle 0.20/0.80, gums 0.07, gap 0.44, size 27×38, drawer 80×266. Shortcuts (11, in order): TaskMaster, BomberWordy, DraStic, EdgeCase, BOTCH, Mach2, QRDD, Vagabond, DungeonSeeker, RatePrince, ShadeCase |
| 0.7 | ✅ Baseline re-run on unmodified 1.5.2, 17:49–18:00 IST (table below) |
| 0.8 | ✅ Exactly **1** EdgeCase window (the sliver) with the app closed |

**0.7 baseline: 1.5.2, same device and day, PSS in MB**

| Stage | Main | Renderer | Total | Earlier run (§2.1) |
|---|---|---|---|---|
| Cold start `TotalTime` | — | — | **144 ms** | 154 ms |
| App open, settled (samples 2–3) | 307.7 / 297.6 | 61.2 / 57.0 | **~355–369** | ~400–430 |
| App open, service started | 299.2 | 85.4 | **384.6** | 400.2 |
| Home, 30 s / 60 s / 120 s | 232.3 / 233.4 / 232.5 | 81.5 / 88.7 / 87.1 | **~314–322** | ~295 |
| Recents swipe, 30 s | 211.8 | 81.1 | 292.9 | 267.6 |
| Recents swipe, 2 min | 132.7 | 58.5 | 191.2 | 188.2 |
| Recents swipe, 5 min | **129.7** | **52.6** | **182.3** | 188.6 |

- After the Recents swipe: `Activities: 0`, `WebViews: 1`; both processes still `prcp` FGS (renderer
  `prcp + 1`, bound by the main process); `send-trim-memory COMPLETE` **refused** (*"Unable to set a
  background trim level on a foreground process"*). Every §2.2 finding reproduces.
- The renderer's figure moves between runs (52–107 MB). It depends on the ad creative being served, so
  comparisons in later phases use the same protocol and look at the retained total after 5 min,
  which is the stable number (182–189 MB across both runs).

**0.7 baseline CPU**

| Situation | Result |
|---|---|
| Main menu, service running (eyes open), 3 × 20 s | **69 %** (RenderThread 518, UI 251, Chrome_InProcGp 180, VizWebView 99: ad redrawing) · **39 %** (RenderThread 336, UI 184) · **28 %** (RenderThread 258, UI 157) of one core |
| Frames over that 60 s | **3690 ≈ 61.5 fps** |
| App in background, Activity alive, 60 s | **9 ticks = 0.15 %** of one core |
| App closed (Activity destroyed), 60 s | **6 ticks = 0.10 %** |

The phone was left as found: sliver running, app closed, settings untouched (a force-stop doesn't
touch saved settings).

**Exit criteria:** signing matches; baselines recorded in this document (§2 updated if anything
moved).
**⏸ STOP for review.**

---

### Phase 1 — Spike: prove the cross-process mechanics on the device ✅

**Purpose:** Phase 2 depends on platform behaviour the docs don't spell out for our exact
sequence. Prove it on the real device **before** writing the real code.
**Code:** temporary, on a local build only, clearly marked `// SPIKE`, and **fully removed at the
end of this phase** (`git diff` must be empty for app code).

Spike build contents: `android:process=":overlay"` on `SidebarService`; a `SPIKE` log helper that
prints every `getRunningServices` entry for our package (`service`, `pid`, `process`, `started`,
`foreground`); calls to it at each point below; `onCreate` / `onStartCommand(action, intent==null)` /
`onDestroy` logging in the service. **The service code is otherwise untouched.** Tag: `EdgeCaseSpike`.

| # | Question | Pass condition |
|---|---|---|
| S1 | Does `getRunningServices` list the `:overlay` service **immediately** after `startForegroundService()` returns, before its `onCreate` has run? | Entry present, `started=true`, on the very next line. This decides whether the Start → onPause → RESUME sequence can be lost |
| S2 | After `stopService()`, when does the entry disappear? | Gone right away, or a bounded delay is measured (sample at 0 / 100 / 500 ms) |
| S3 | Is the process really separate? | `ps -A` shows `com.dicereligion.edgecase:overlay` with its own PID; the sliver is drawn from it |
| S4 | Does the `:overlay` process stay free of startup providers and WebView? | `dumpsys meminfo <overlay pid>`: no `androidx.work.workdb` database, `WebViews: 0` |
| S5 | Does a `START_STICKY` restart after the `:overlay` process dies deliver `onStartCommand(null)`? | `am crash <overlay pid>` → service back, log shows `intent==null` |
| S6 | Does killing the **main** process leave the overlay untouched? | With the app closed, `am crash <main pid>` → sliver still attached, overlay PID unchanged |
| S7 | *Only if S1 or S2 fails:* does binding without `BIND_AUTO_CREATE` give reliable running/stopped signals (connect-later on start; `onServiceDisconnected` / `onBindingDied` on stop and on kill)? | All four events observed as documented in `ServiceConnection` |
| S8 | After leaving the app with the overlay running, what happens to the main process? | `dumpsys activity oom` shows it leave `prcp` for a cached state; `send-trim-memory <main pid> COMPLETE` is **accepted** (the G2 test) |
| S9 | Memory of the spike's `:overlay` process at 30 s, 2 min and 5 min after the app closes | Recorded. Compared against the 25.9 MB stand-in |

- [x] **1.1** Build the spike (`assembleRelease`), install with `-r`, launch; confirm no crash.
- [x] **1.2** Run S1–S6 and S8–S9; record every log excerpt and number in this document.
- [x] **1.3** If S1 or S2 fails: add S7 to the spike, run it, record it, and propose switching
      D5 to binding. **S1 failed → S7 run → switch proposed (below).**
- [x] **1.4** Remove every `SPIKE` change; `git diff -- app/` is empty; rebuild and reinstall a
      clean build of the current code so the phone runs the unmodified app again.

#### Phase 1 results (2026-09-25, 18:09–18:21 IST, spike build on the Pixel 9 Pro XL)

Tag `EdgeCaseSpike`; `t=` is `SystemClock.uptimeMillis()`. Main process PID 20720 → 22438 →
23904 across rebuilds; overlay PIDs as noted.

| # | Result | Evidence |
|---|---|---|
| S1 | ❌ **FAIL.** Immediately after `startForegroundService()` returns, `getRunningServices` reports **NONE**. The service is listed only once its process is up | `[start+0ms] running services: NONE`; polling every 10 ms: `listed=false` until **+99 ms** (run 1) and **+70 ms** (run 2), the moment `svc.onCreate` runs in the new process. On a loaded phone the blind window would be longer. **K1 is real** |
| S2 | ✅ Gone immediately after `stopService()` and stays gone | `[stop+0ms] … NONE` in the same millisecond as `svc.onDestroy`; NONE at +100, +500, +2000 ms |
| S3 | ✅ Separate process | `ps`: `com.dicereligion.edgecase:overlay` (PID 21762) beside main (20720); every service log line reports `proc=com.dicereligion.edgecase:overlay` |
| S4 | ✅ No startup providers, no WebView in `:overlay` | `dumpsys meminfo <overlay>`: `WebViews: 0`, `Activities: 0`, **no `androidx.work.workdb`** database; 10 bitmaps / 5.8 MB |
| S5 | ✅ Sticky restart delivers a `null` intent, in a new process | `am crash` (app closed) → *"Scheduling restart of crashed service … in 1167ms"* → new PID 23257: `svc.onStartCommand … intentNull=true startId=2`; sliver back (1 window) |
| S6 | ✅ Killing the main process leaves the overlay untouched | `am kill` with the app closed: *"Killing 23904 … (adj 905): kill background"* and the renderer *"isolated not needed"*; `:overlay` 24774 unchanged, still FGS, sliver attached |
| S7 | ✅ **Binding without `BIND_AUTO_CREATE` gives every signal needed, with no blind window** | (a) `bindService(flags=0)` returns `true` while the service is stopped, and the binding **connects by itself** when START creates the service: order observed 3 of 3 times is `svc.onCreate` → `svc.onBind` → `svc.onStartCommand` → `probe.onServiceConnected`. **`onBind` always arrives before `onStartCommand`.** (b) STOP → client gets `onServiceDisconnected` **and** `onBindingDied` (the binding must be unbound and rebound). (c) Overlay killed while the app is open → `onServiceDisconnected` only; the sticky restart (`intentNull=true`) re-binds by itself (`svc.onBind` → `probe.onServiceConnected`). (d) Leaving the app (`unbindService` in `onPause`) → `svc.onUnbind` |
| S8 | ✅ **G2 met in principle.** After the Recents swipe, main and renderer go **`cch-empty`** (cached, reclaimable); only `:overlay` stays `prcp` FGS; `send-trim-memory COMPLETE` on main is **ACCEPTED** (1.5.2 refuses it) | `dumpsys activity oom` excerpt recorded in the session; trim output |
| S9 | ✅ **G1 met in principle.** `:overlay` PSS **28.1 MB** (30 s) → **25.7 MB** (2 min) → **25.7 MB** (5 min); RSS ~158 MB; GL 5.9, Java 2.7, Native 12.2 MB. After the main process was reclaimed (S6), **EdgeCase's total resident footprint was 25.7 MB, against 182–189 MB for 1.5.2** | Appendix A `measure.sh` output |

**Other things the spike showed:**

- **K2 is real, and D6 is required.** After STOP, the old `:overlay` process (PID 22964) stayed
  alive and was **reused** by the next START (`svc.onCreate pid=22964`). Its SharedPreferences cache
  would have been stale.
- **The cached main process is frozen.** While cached, its PSS stays constant (204.2 MB main +
  56.7 MB renderer at 30 s, 2 min and 5 min), and `meminfo` can't read its object counts (`-`).
  That memory is now **Android's to reclaim** (S6 shows it being reclaimed), which is the point
  of G1/G2.
- **Testing artifact, not app behaviour:** three deliberate `am crash`es inside ~60 s triggered
  Android's crash-loop protection, which stopped the service and did not restart it. Real memory
  kills aren't crashes. Phase 2's crash tests must be **≥ 60 s apart**, and main-process kills
  use `am kill` (a background kill), not `am crash`.
- The spike had no fix for the per-process statics, so it drew the sliver over its own settings
  screen (2 EdgeCase windows with the app open). That was expected; Phase 2 handles it.
- A transient `NETWORK_ERROR … ERR_NAME_NOT_RESOLVED` banner failure at 18:09 was the phone's DNS;
  the banner loaded normally at 18:21 on the clean build.

**Clean-up (1.4):** the manifest line, the `SidebarService` / `MainActivity` hooks and `SpikeLog.kt`
were removed; `git diff -- app/` is empty and `grep -rn "SPIKE\|SpikeLog\|:overlay" app/src` finds
nothing. A clean release build (merged manifest: no `android:process`) was reinstalled with `-r`.
The phone is on the unmodified app: single process, sliver running, banner loaded. Settings were
never touched.

#### Change to D5 (✅ approved by the maintainer, 2026-09-25)

The evidence says **binding**, not `getRunningServices`, should tell the service when the
settings screen is in front. **Each mechanism is used only where Phase 1 proved it reliable:**

| Need | Mechanism | Why (evidence) |
|---|---|---|
| **Hide the sliver while the settings screen is in front** | The Activity **binds** (flags `0`, never `BIND_AUTO_CREATE`) in `onResume` and unbinds in `onPause`. The service hides its windows in `onBind` / `onRebind` and shows them again in `onUnbind` | S7a: `onBind` always arrives **before** `onStartCommand`, so a START, or a sticky restart, while the app is open already knows to stay hidden. No blind window (fixes **K1**), no `ACTION_SUSPEND/RESUME` intents, no `EXTRA_START_DETACHED`, and the restart-while-open case (**K3**) is handled too (S7c) |
| **Guard settings updates (the G5 fix)** | Send `ACTION_SYNC_STATE` only while the binding is connected | Updates are only ever made from the settings screen, which is resumed and therefore bound (S7a/c) |
| **Serpent's Eyes on resume** | `getRunningServices` in `onResume`, refined by the binding callbacks | S2: once the service is up, `getRunningServices` is exact and immediate. Its only blind spot (S1) is the ~100 ms right after START, when the eyes are already set open by the tap |
| **After STOP** | `onBindingDied` → `unbindService` + `bindService` again, if still resumed | S7b: the binding dies on STOP; without rebinding, a second START in the same visit would draw the sliver over the app |

**Exit criteria:** every question answered with evidence; D5 confirmed or changed.
**⏸ STOP for review.** The maintainer approves the Phase 2 design based on these results.

---

### Phase 2 — Process split, state sync, restart bug ✅

**Purpose:** goals G1, G2 and G5.

**Files:** `AndroidManifest.xml`, `SidebarService.kt`, `MainActivity.kt`, `SliverConfig.kt`, new
`OverlaySnapshot.kt`, new `androidTest/.../OverlaySnapshotTest.kt`, `SliverConfigTest.kt`.

#### 2A — Model and serialisation (no behaviour change yet)
- [x] **2A.1** `SliverConfig.toBundle()` / `SliverConfig.fromBundle(bundle)`, reusing the `K_*` keys.
      `fromBundle` falls back to the `DEF_*` defaults for missing keys, and restores `ColorMode`
      with the same `valueOf` + catch that `load()` uses. That's why the R8 enum keep rule stays.
- [x] **2A.2** `OverlaySnapshot` (§5.3): `fromPrefs`, `writeTo(intent)`, `fromIntent(intent)`
      (returns `null` if the snapshot extra is absent). `fromPrefs` moves the logic now in
      `SidebarService.loadPositionFromPrefs` and `populateShortcuts` (including the legacy
      `saved_shortcuts` set fallback and the `coerceIn(0f, 1f)` on yBias) verbatim.
- [x] **2A.3** Instrumented tests:
  - `SliverConfig` Bundle round-trip, for defaults and for all 15 fields changed
  - `fromBundle` on an empty Bundle equals `SliverConfig()`
  - An unknown ColorMode string falls back to DEFAULT
  - `OverlaySnapshot` intent round-trip
  - `fromPrefs` with only the legacy set present
  - `fromPrefs` with neither key present gives an empty list

#### 2B — The split
- [x] **2B.1** Manifest: `android:process=":overlay"` on `SidebarService`, plus a comment explaining
      why (memory; this plan). **No other component moves.**
- [x] **2B.2** `SidebarService`:
  - Remove the static `isRunning`; add `companion fun isRunning(context)` (`getRunningServices`,
    used only for the eyes on resume).
  - Binding (D5): `onBind` returns a plain `Binder()` (it is `null` today, which would give the
    client `onNullBinding`) and sets `bound = true` + `detachOverlayWindows()`; `onRebind` does the
    same; `onUnbind` sets `bound = false`, re-attaches if state exists, and returns `true` so later
    binds come through `onRebind`.
  - `onCreate` keeps: window manager, screen height, vibrator, `buildSystemNotification()`
    (`startForeground` must stay here, within the FGS start deadline), and the
    `canDrawOverlays` → `stopSelf()` guard. It no longer reads prefs, builds views or attaches.
  - `onStartCommand`:
    - `ACTION_START` / `ACTION_SYNC_STATE` → take the snapshot from the intent, or else from
      prefs; apply it. The first application builds params and views; later ones go through
      the existing in-place `applySliverUpdate` path.
    - Attach only when `!bound` (for START, SYNC and the `null` intent alike).
    - `null` intent → `fromPrefs`, then attach unless bound.
    - Delete `ACTION_SUSPEND_OVERLAY` / `ACTION_RESUME_OVERLAY` and their handling.
  - Delete the `MainActivity.isForeground` read. Keep `START_STICKY`.
- [x] **2B.3** `MainActivity`:
  - Delete the `isForeground` companion.
  - `onResume`: eyes from `SidebarService.isRunning(this)`; then `bindService(intent, overlayConnection, 0)`.
    `onPause`: `unbindService(overlayConnection)`. Delete `setOverlaySuspended`.
  - `overlayConnection`:
    - `onServiceConnected` → `overlayConnected = true`, eyes open
    - `onServiceDisconnected` → `overlayConnected = false`, eyes closed
    - `onBindingDied` → same as disconnected, then `unbindService` and, if still resumed, rebind
  - START sends `ACTION_START` + snapshot (no extra flag needed).
  - New private `syncOverlay()` sends `ACTION_SYNC_STATE` + snapshot **only if `overlayConnected`**.
    It replaces the three unguarded `startService` calls (`:402`, `:584`, `:610`); this fixes G5.
  - STOP unchanged (`stopService`); the binding then dies and is re-established by the callback.
- [x] **2B.4** Update the KDoc that describes the old statics (`SidebarService` companion,
      `MainActivity` companion, the "Mirrors … isRunning" comments) so no comment describes code
      that no longer exists.

#### 2C — Verification on the phone (release build, installed with `-r`)

| # | Scenario | Expected |
|---|---|---|
| V1 | Fresh launch → START | Sliver not drawn over the app; `ps` shows `:overlay`; Home → sliver appears |
| V2 | Swipe in from sliver → tray → launch an app | Works as in 1.5.2 |
| V3 | While running: change shortcuts / position / style, then Home | The overlay reflects every change immediately |
| V4 | **G5**: STOP → change and SAVE shortcuts, drag the position, APPLY a style → Home | Service stays stopped; no `SidebarService` record; no sliver |
| V5 | **Stale cache**: START → Home → back → STOP → change a style → START → Home | The sliver shows the **new** style (the `:overlay` process may have lived through the STOP) |
| V6 | `am crash <overlay pid>` with the app closed (≥ 60 s after any other crash) | Service restarts (`intentNull`); sliver back with the current settings |
| V7 | `am kill com.dicereligion.edgecase` with the app closed (a background kill, not a crash) | Main and renderer killed; overlay unaffected |
| V8 | Eyes: open the app with the service running / stopped; stop it via Quick Settings' active-apps **Stop** and reopen | Eyes match reality every time |
| V9 | Memory (Appendix A): Recents swipe + 5 min | `:overlay` PSS recorded and compared with 25.9 MB; main process cached (`dumpsys activity oom`); `send-trim-memory` on main accepted |
| V10 | Leave for 5 min, reopen | Plinth banner loads; `DISABLE_AD_INSPECTOR` still in the merged manifest; shake over another app does nothing |
| V11 | Overlay grep (stats.md group C) | Empty |
| V12 | START → STOP → START → STOP → START, all within one visit to the settings screen | Sliver **never** drawn over the app (1 EdgeCase window = the Activity's); eyes correct after each tap; Home → sliver appears |
| V13 | App open and bound: `am crash <overlay pid>` (≥ 60 s after any other crash) | Restarted service stays **hidden** while the app is open (K3); Home → sliver appears |
| V14 | Open the Customize dialog, then press Home with it open, then return | Sliver appears on Home, hides again on return; dialog intact |

- [x] **2C.1** Run V1–V11; record results and numbers here.
- [x] **2C.2** Instrumented tests (2A.3 and the existing 14) pass on the emulator.
- [x] **2C.3** Restore the maintainer's settings from the 0.6 screenshots if any scenario changed them.

#### Phase 2 results (2026-09-25, 18:36–19:07 IST, release build, Pixel 9 Pro XL)

**Files changed** (no commit): `AndroidManifest.xml` (+ `android:process=":overlay"`, comments),
`SidebarService.kt`, `MainActivity.kt`, `SliverConfig.kt` (+ `toBundle`/`fromBundle`), new
`OverlaySnapshot.kt`, `SliverConfigTest.kt` (+3 tests), new `OverlaySnapshotTest.kt` (6 tests).
Merged release manifest: `SidebarService` in `:overlay`; `DISABLE_AD_INSPECTOR` and
`PROPERTY_SPECIAL_USE_FGS_SUBTYPE` present; permission set identical to 1.5.2 (the eleven in stats.md §9).

**⚠️ A bug found and fixed during verification (V4).** The first build leaked a sliver window. On
STOP while the app is open, Android calls `onUnbind` and then `onDestroy` back to back. The new
`onUnbind` re-attached the sliver, and `onDestroy` only removed views that reported
`isAttachedToWindow`, which isn't true until the next frame. So the removal was skipped, and a
sliver stayed on screen owned by a process with no service (`app=null`, window session = the
`:overlay` PID). 1.5.2 doesn't have this bug; it was introduced by the split. The fix:
- Every add and remove of either window is now tracked by flags set at the moment of the call
  (`sliverAdded`, `trayAdded`), never inferred from `isAttachedToWindow`.
- A `destroyed` flag blocks any later add, and all attaches go through one guarded
  `addSliverIfNeeded()` (refuses when `destroyed`, `bound`, or already added).
- `onUnbind`'s re-attach is **posted**, so STOP's already-queued `onDestroy` runs first and nothing
  flashes on screen.

Re-verified below.

| # | Result | Evidence |
|---|---|---|
| V1 | ✅ | App open: overlay windows 0 before and after START, service running, `:overlay` PID separate; Home → 1 window (the sliver); banner loaded; no crash |
| V2 | ✅ | Real swipe on the sliver (`frame=[948,1649][1008,1734]`) → tray `[828,1093][1008,1691]` with the shortcuts in order (#1 TaskMaster at the bottom); tapping TaskMaster → `topResumedActivity=com.dicereligion.TaskMaster`; tray folded back into the sliver |
| V3 | ✅ | While running: **style** (opacity 50→99 %) → sliver fangs went solid (zoomed before/after crops); **shortcuts** (70mai added) → 70mai at the top of the tray; **position** (dragged to the left) → live window moved to `[0,1649][60,1734]`, then back to `[948,1649][1008,1734]`, pixel-identical to the start |
| V4 | ✅ (after the fix) | STOP → SAVE → position drag → APPLY → Home: `service_running=0`, `overlay_windows=0` at every step. **1.5.2's restart-on-SAVE bug (G5) is gone** |
| V5 | ✅ | START → Home → STOP → set 99 % while stopped → START: the `:overlay` process was **reused** (PID 5110 throughout), yet the sliver showed the new 99 % style. The snapshot defeats the stale cache (K2) |
| V6 | ✅ | `am crash` of `:overlay` with the app closed (a `CrashedByAdbException`, the induced one) → restart in 1.2 s, new PID 7572, sliver at the identical frame, default style read from prefs |
| V7 | ✅ | `am kill` with the app closed → main and renderer gone; overlay PID 7572 unchanged, still showing |
| V8 | ✅ | Eyes **open** with the service running (screenshot); stopped via Quick Settings → *Active apps* → EdgeCase **Stop** → every EdgeCase process ended → reopen → eyes are **closed slits** (screenshot) |
| V9 | ✅ | Table below |
| V10 | ✅ | **Shake over another app does not open the ad inspector: checked by the maintainer by hand, 2026-09-25.** Banner loads (`Plinth banner loaded (411×128dp)`, 18:55:05); `DISABLE_AD_INSPECTOR` in the merged manifest. With the split, the main process is also frozen once cached (V9), so its gesture listener can't run then anyway |
| V11 | ✅ | Overlay grep empty; `AdHost` referenced only from `MainActivity`; no `WebView` anywhere in app code; overlay-side classes mention `MainActivity` only in comments |
| V12 | ✅ | App open: STOP / START / STOP / START → `0/0, 1/0, 0/0, 1/0` (running / windows); Home → `1/1`. The rebind after each STOP works; the sliver was never drawn over the app; no exceptions |
| V13 | ✅ | App open and bound, `am crash` of `:overlay` (PID 10227 → 10313, restart in 1.1 s): restarted service stayed **hidden** (`1/0`); Home → `1/1`. **K3 resolved.** *(A first attempt 2 min after V6 triggered Android's repeated-crash dialog, which Home dismissed as "close app"; retried with a 3-minute gap.)* |
| V14 | ✅ | Customize dialog open → Home → sliver shows (`1/1`) → return → hidden (`1/0`), dialog still open |
| 2C.2 | ✅ | **23/23 instrumented tests pass** on the emulator (Pixel_9_Pro_XL AVD, API 37): `OverlaySnapshotTest` 6, `SliverConfigTest` 11, `SliverShapeTest` 6; 0 failures, 0 errors. The phone was untouched (its last update is the 18:45 release install) |
| 2C.3 | ✅ | Settings restored and verified: shortcuts back to the same 11 in the same order; style RESET to defaults (dialog reads 50 %, Default grey); position Right 79 % at the identical pixel frame. Phone left with the sliver running and the app closed |

**V9: memory, same protocol as Phase 0 (PSS, MB)**

| Stage | 1.5.2 (Phase 0) main / renderer | 1.6.1-wip main / renderer / **overlay** |
|---|---|---|
| App open, settled | 307.7 / 61.2 | 324.7–336.3 / 91.2–103.0 / — |
| App open, service started | 299.2 / 85.4 | 319.1 / 102.2 / **17.5** |
| Home, 30 s | 232.3 / 81.5 | 244.7 / 94.3 / **25.9** |
| Home, 120 s | 232.5 / 87.1 | 219.3 / 94.5 / **25.9**, main and renderer **already frozen** (`Act=-`) |
| Recents swipe, 5 min | **129.7 / 52.6 = 182.3, held at `prcp` forever** | 219.0 / 94.5 **cached (`cch-empty`), reclaimable** / **25.9 always resident** |
| Priority of main after leaving | `prcp` FGS | **`cch-empty`** |
| `send-trim-memory` on main | **REFUSED** | **accepted** |
| Main CPU, app in background | 9 ticks / 60 s | **0** (frozen) |
| Main CPU, app closed | 6 ticks / 60 s | **0** (frozen) |
| Overlay CPU, app closed | — | **0 ticks / 60 s** |

How to read it:
- **G1 met.** What must stay resident is now the 25.9 MB overlay (Phase 1's 25.7 MB and the 25.9 MB
  stand-in agree). Everything else becomes a cached process that Android freezes and kills as it
  needs the memory; Phase 1 S6 showed it being reclaimed, leaving 25.7 MB in total.
- **G2 met.** The trim is accepted, and the main process is `cch-empty`.
- **The cached main process is *bigger* at the moment it freezes** (219 MB against 1.5.2's 130 MB
  after GC). It's frozen within about 2 minutes of leaving, before the garbage collector frees the
  ~79 MB of app icons (native heap still 121.5 MB). That's reclaimable memory, not resident cost,
  and Phase 4 removes the icons at the source.
- **Cost of the split while the app is open:** the overlay adds 17.5 MB (app in front) to 25.9 MB
  (sliver drawn). The renderer's figure varies with the ad creative, as noted in Phase 0.
- *(Measurement note: the "activities: 5" line in the raw log was taken 3 s after the swipe, while
  the frozen process's Activity destruction was still deferred; it reads 0 afterwards, and `oom`
  shows `cch-empty`.)*

**Exit criteria:** V1–V11 pass; G1, G2 and G5 met with numbers.
**⏸ STOP for review.**

---

### Phase 3 — Animations: tap cracks removed, everything else as light as possible ✅

**Purpose:** goal G3, under decision D1 as amended on 2026-09-25: remove the tap crack effect, and
make every remaining animation as light as possible without visibly changing the look and feel.

**Files:** delete `CrackFlashView.kt`; new `TempleClock.kt`; `ObsidianCrackView.kt`,
`ServiceEyeView.kt`, `DustParticleView.kt`, `MainActivity.kt`, `layout_screen_main_menu.xml`
(comment only).

#### 3.0 Animation inventory (every animation in `app/src/main/java`, found by grep)

| Animation | When it runs | Cost today | Decision |
|---|---|---|---|
| **Crack flash** (`CrackFlashView`, `MainActivity.kt:142–148`, `:429–434`) | ~0.3 s on every slab press | Full-screen view redrawn every frame for the flash | **Removed** (maintainer, 2026-09-25) |
| **Gem background** (`ObsidianCrackView`, every screen) | **Never stops** while a screen is visible | Full-window redraw every frame (~62/s); per-frame allocations (§2.4) | **Shared clock at 12 fps** plus the allocation removal |
| **Serpent's Eyes breathing** (`ServiceEyeView`, main menu) | **Never stops** while the service runs | Full-window redraw every frame | **Same shared clock**; lid open/close at display rate (a few hundred ms) |
| **Dust burst** (`DustParticleView`, main menu) | 0.4–0.7 s per press | Short; physics advance per *frame*, so it plays about **2× faster on a 120 Hz display** | Kept; physics made time-based so it looks the same at any refresh rate; stops as soon as the particles are gone (as today) |
| Slab press dip (`MainActivity.applyStoneButtonBehavior`) | 80 / 120 ms per press | `ViewPropertyAnimator`, runs on the render thread | Unchanged |
| Drag-reorder lift (`ShortcutDragCallback`) | 150 ms while dragging | Same | Unchanged |
| Positioning snap and particle trail (`PositioningView`) | 200 ms snap, and only while dragging | Interaction-only | Unchanged |
| Tray unfurl (`SidebarService`) | 250 ms per swipe, overlay process | Interaction-only | Unchanged |
| Ad creative | Whenever the SDK redraws | Not our code (§2.4's 71 % sample) | Out of scope |

Only the two never-ending animations cost anything while you're idle, so that is where "lighter"
has to come from. The interaction-only animations are left alone: they cost nothing at rest, and
cutting them would change how the app feels to the touch. The static crack **texture** on the slab
faces (`ic_texture_cracks.xml`) is a drawable, not an animation, and **stays**.

#### Design points, each with its reason

1. **One shared clock, not one per view.** Two independent timers out of phase would double the
   distinct redraws, and each redraw re-renders the whole window. `TempleClock` is a main-thread
   singleton that ticks at one fixed interval while it has subscribers and calls `invalidate()` on
   each one in the same tick. No subscribers means no ticking.
2. **12 fps (maintainer's decision, 2026-09-25).** The gem pulse periods are 2.4–4.8 s and the
   eye's is 2.6 s. At 12 fps one tick advances the fastest gem's sine phase by `2π / (2.4 s × 12)`
   ≈ 0.22 rad, so glow steps stay small. The interval (≈ 83 ms) is one constant.
3. **Time-based, not frame-counted.** Views read `SystemClock.uptimeMillis()`, not a `+16f` counter,
   so every pulse keeps its period at any frame rate. This also fixes Known Limitation #11.
4. **Explicit lifecycle.** A view subscribes only when it's attached, `VISIBLE` **and** its window is
   visible (`onWindowVisibilityChanged`). A `Handler` loop, unlike today's `ValueAnimator`, would
   *not* stop by itself when the Activity is stopped, so this is required to keep the closed-app
   CPU at its current ~0.2 %.
5. **Nothing is computed per frame that can be computed once**, in `ObsidianCrackView` and `ServiceEyeView`:
   - Gem paths are static, so build them once in `placeGems`. The eye's lens and iris paths are
     rebuilt only when `openFraction` or the size changes.
   - One `RadialGradient` per gem (and one for the eye), created once at unit radius with colours
     at full strength (255 and 102 alpha, then transparent). Each frame sets
     `shader.setLocalMatrix` (scale about the centre) and `glowPaint.alpha = glowAlpha`. Paint alpha
     multiplies shader alpha, so the output is the same as today's per-frame gradient.
   - Resolve `emerald_deep` / `emerald_gem` once, not 22 (gems) + 2 (eye) times a frame.
6. **The eyes follow the same clock**, with the lid easing converted to time
   (`1 − 0.82^(dt/16.67)` per step keeps today's 0.18-per-60fps-frame curve). **Lid open and close
   transitions run at display rate** via `postInvalidateOnAnimation`, so that brief motion stays
   smooth; only the steady breathing uses the clock. The self-stop when fully closed stays.
7. **Removing the crack flash**:
   - Delete `CrackFlashView.kt`.
   - In `MainActivity`: delete the `crackView` field, its construction and `addView`, and the
     `crackAt` block in `applyStoneButtonBehavior`. The press dip, haptic and dust burst stay.
   - Reword the `dustContainer` comment in `layout_screen_main_menu.xml` ("Dust + crack overlay").
     The container keeps its 16dp elevation, because the dust still needs to draw above the slabs
     (stats.md A3).
   - Nothing else references the class (grep: only `MainActivity`). None of its colours are
     resources, so no resource becomes unused.
8. **Dust**: replace the fixed `dt = 0.016` and per-frame position steps with elapsed time since the
   previous frame, scaled so it looks exactly like today's 60 fps behaviour. The particle count (6),
   life (0.4–0.7 s) and look are unchanged.

#### Tasks
- [x] **3.1** Before changing anything, record 10 s screen captures (`adb shell screenrecord`) of
      the main menu with the eyes open and with them closed, and one slow-motion capture of a slab
      press (dust + cracks), for the before/after record.
- [x] **3.2** Remove the crack flash (point 7); build; launch; press every slab once. No crash,
      and the dust still shows above the button.
- [x] **3.3** Implement `TempleClock`; convert `ObsidianCrackView` and `ServiceEyeView` (points 1–6);
      convert `DustParticleView` (point 8).
- [ ] **3.4** *(Frame-rate comparison dropped: the maintainer chose 12 fps on 2026-09-25.)*
- [x] **3.5** At 12 fps, measure on the phone (Appendix A, CPU part):
  - Main menu idle, eyes open and eyes closed: 3 × 20 s per thread each
  - `gfxinfo` frames over 30 s (≈ 12/s expected when the ad isn't redrawing)
  - Each sub-screen for 20 s, since all four screens carry an `ObsidianCrackView`
  - Closed-app CPU after Home: 60 s, must stay ≤ baseline (0.12–0.23 %)
  - Cost of one slab press (dust only now): process CPU over the 1 s after a single tap, before
    vs after
- [x] **3.6** Record the same captures after the change; hand both sets to the maintainer for a
      side-by-side check.
- [x] **3.7** Check:
  - Gems still pulse at their own periods (2.4–4.8 s)
  - Eyes open and close smoothly, and still stop when closed
  - Dust looks the same as the 60 fps "before" capture
  - No animation runs after Home (CPU check)
  - Stats.md's 14 instrumented tests still pass (emulator)

#### Phase 3 results (2026-09-25, 19:11–19:40 IST, release build, Pixel 9 Pro XL)

*(Phases 0–2 were committed by the maintainer as `1b01f6a` before this phase started; the working tree
now holds only Phase 3.)*

**Code** (no commit):
- `CrackFlashView.kt` deleted (`git rm`); its field, construction and `crackAt` block removed from
  `MainActivity`; KDoc and the `dustContainer` layout comment reworded.
- New `TempleClock.kt`: a main-thread 12 fps ticker, running only while something is subscribed; a
  tick allocates nothing.
- `ObsidianCrackView`: subscribes only while attached, shown and window-visible; wall-time phases
  (Double); gem path and halo `RadialGradient` built once per layout, with per-frame local matrix and
  paint alpha; colours resolved once.
- `ServiceEyeView`: lid open/close eased by elapsed time at display rate; steady breathing on the
  clock; closed = no animation; halo shader and colours cached.
- `DustParticleView`: physics scaled by elapsed time (was per frame), same constants, count and life.
- Remaining per-frame `getColor` / `RadialGradient` calls: none (the ones left in `drawBase` run once
  per size).

**CPU and frames, main menu and sub-screens** (20 s samples, per thread, frames in the same window).
A **video** ad creative was being served during this phase, and while it plays it drives the window
at 22–56 fps whatever the app does. So samples are classified by the ad's GPU thread
(`Chrome_InProcGp` < 20 ticks = QUIET). The 1.5.2 comparison uses Phase 0's samples in which that
thread was also idle.

| Screen | 1.5.2, ad quiet | **1.6.1-wip, ad quiet** | 1.6.1-wip, ad playing video |
|---|---|---|---|
| Main menu, eyes open | 28 % (Render 258, UI 157); **61.5 fps** | **10 %, 10 %** (Render 81–100, UI 51–59); **12.2 fps** | 33–57 %, 27–46 fps |
| Main menu, eyes closed | 29–35 % (Phase 0 of the first session) | **12 %** (Render 93, UI 59) | 40–53 % |
| Shortcuts | not sampled | **12 %, 12 %**; 12.2 fps | 23–63 %, 22–51 fps |
| Credits | not sampled | **11 %**; 12.2 fps | 34–55 % |
| Position | not sampled | **8 %, 12 %**; 12.2 fps | 72 %, 56 fps |

- **The app's own idle cost fell from ~28–35 % to ~10–12 % of one core, and its redraw rate from
  61.5 to 12.2 fps**, identical on all four screens (they share the clock).
- **What remains is mostly the ad.** A playing video creative keeps the window redrawing at up to
  56 fps and costs up to ~72 %. That's outside this app's code. *Possible follow-up, not verified:*
  check whether the AdMob unit's settings can exclude video creatives for this banner.
- **Presses:** in the one quiet pair, 5 s idle = 13 %, 5 s with 3 slab presses (dust only; pressed
  then slid off so nothing activated) = 26 %, so about **0.2 CPU-seconds per press**. The other two
  pairs overlapped a video ad and are discarded. No "before" figure: the pre-Phase-3 build isn't
  installed, and the crack flash it had was an extra full-screen redraw on top of this.
- **The clock stops when the app is hidden (K6), verified per thread:** after Home the UI thread used
  2, 0, 1, 0 ticks in four 20 s windows; the closed-app total (21 ticks in the first minute) was
  Chromium `MemoryInfra` and the garbage collector winding down, then **0** once cached
  (`cch … previous-expired`).

**Look (3.6 / 3.7):**
- Recordings for the maintainer's side-by-side check are in the session scratchpad `p3/`:
  `before-menu-eyes-open.mp4`, `before-menu-eyes-closed.mp4`, `before-press.mp4`,
  `after-menu-eyes-open.mp4`, `after-menu-eyes-closed.mp4`, `after-press.mp4`,
  `after-eyes-opening-on-START.mp4`, `after-eyes-closing-on-STOP.mp4`.
- Checked by screenshots: the same gem 1.3 s apart goes from a full halo with hot core to a dim
  trough, so the pulse and the cached-shader halo render correctly; the eyes open with the iris and
  core; the closed eyes are a slit. Lid smoothness can only be judged from the videos (a screenshot is
  slower than the transition).
- **23/23 instrumented tests pass** on the emulator; the phone was untouched by the test run.
- Phone left with the sliver running, the app closed, settings untouched (only START/STOP were used).

**✅ Visual sign-off (maintainer, 2026-09-25):** *"Looks good, I literally can't even tell any
difference. Even side by side."*

**Exit criteria:** crack flash gone; CPU and frame numbers recorded and clearly below baseline;
closed-app CPU not worse; **maintainer approves the look**.
**⏸ STOP for review (visual sign-off).**

---

### Phase 4 — Icon loading: labels up front, icons on demand ⬜

**Purpose:** goal G4, keeping the instant Shortcuts open that preloading provides (R6).

**Files:** `AppInfoData.kt`, `MainActivity.kt` (`getInstalledApps`, `preloadApps`, lifecycle),
`ActiveShortcutsAdapter.kt`, `AvailableAppsAdapter.kt`, new `AppIconCache.kt`.

Design:
1. `AppInfoData` becomes `(appName, packageName, componentName)`; the `Drawable` field goes. It's
   read only in the two adapters (§2.5).
2. `getInstalledApps()` loads **labels only**. The preload thread stays, so the list is ready
   instantly as today.
3. `AppIconCache`, owned by `MainActivity`:
   - An `LruCache<ComponentName, Bitmap>` sized in bytes from the density measured in 0.4:
     `64 icons × (48dp × density)² × 4 bytes`. At least three screens of rows fit, and the
     maximum is fixed in advance, not proportional to installed apps.
   - Icons are loaded on one background thread with `pm.getActivityIcon(componentName)`. That is
     the same icon `ResolveInfo.loadIcon` returns today, so nothing changes visually.
   - Each icon is **rendered into a Bitmap at display size** (48dp), then the full-size source
     drawable is dropped. This is where the memory goes.
   - Loads already in flight are de-duplicated.
4. Adapters set `holder.ivIcon.tag = component`: use a cache hit straight away; otherwise clear the
   image and request a load, applying the result on the UI thread **only if the tag still matches**,
   since recycled rows must not show another app's icon.
5. The cache is cleared in `onDestroy` and on `onTrimMemory(TRIM_MEMORY_UI_HIDDEN)`, and the loader
   thread shuts down in `onDestroy`.

Tasks:
- [ ] **4.1** Before the change: add a temporary `Log.d` timing around `getInstalledApps()`
      (labels + icons) and around the Shortcuts screen's first `buildShortcutsLists`; record.
- [ ] **4.2** Implement 1–5.
- [ ] **4.3** Measure (Appendix A):
  - Main menu settled, Shortcuts never opened: `Bitmap (malloced)` count and size, compared with
    the baseline 299 / 79.2 MB
  - Shortcuts opened and the Archives list flung end to end: bitmap total stays within the
    cache budget
  - Timing from 4.1 repeated. Label-only preload must not be slower; the Shortcuts open must stay
    instant
- [ ] **4.4** Check: every row shows its correct icon after fast scrolling in both directions;
      Altar and Archives icons match the app; drag-reorder still works; an app uninstalled while
      the screen is open doesn't crash.
- [ ] **4.5** Remove the temporary timing logs.

**Exit criteria:** G4 met with numbers; no visible difference beyond icons appearing a moment
later on a very fast fling.
**⏸ STOP for review.**

---

### Phase 5 — Notification decision and privacy-policy correction ⬜

**Purpose:** goal G6 and decision D2. **App code change: a comment only.**

- [ ] **5.1** Confirm D2 with the maintainer (the first question of this gate). If overturned,
      replace this phase with: declare `POST_NOTIFICATIONS`; request it on START (API 33+) with
      the service starting whatever the answer; give the notification a content intent and a proper
      monochrome small icon; add a policy row for the new permission.
- [ ] **5.2** `SidebarService.buildSystemNotification()`: add a comment explaining why
      `POST_NOTIFICATIONS` is deliberately not requested (D2, with the Android and Play references
      from §2.5), so nobody "fixes" it later.
- [ ] **5.3** On the phone, confirm where Android 17 shows EdgeCase with the app closed: Quick
      Settings → active-apps list (screenshot). The policy wording in 5.4 must match what is
      actually on screen, not what the docs call it.
- [ ] **5.4** Anumey's Lair (`~/Work/Web/Anumey's Lair`, branch `development`, no commit):
  - `src/app/legal/edgecase/privacy/page.tsx:102`: rewrite the `FOREGROUND_SERVICE` row's `why`.
    Draft (final wording set after 5.3):
    > *Keeps the fang alive while you use other apps. Android requires every app that does this to
    > register a notice with the system. EdgeCase does not ask for permission to post notifications,
    > so on Android 13 and later that notice appears only in the list of active apps in Quick
    > Settings, where you can also stop EdgeCase; on Android 11 and 12 it appears as a low-priority
    > notification. Either way it is the system telling you the app is running, not the app telling
    > you anything.*
  - Bump `EFFECTIVE_DATE`. §12 of the policy requires an updated effective date. The date is set
    to the day the maintainer will deploy; ask.
  - Update the header comment (lines 21–26), which still describes 1.5.0.
  - Run `npm run build` (or the repo's build command) to confirm the page compiles. Do not deploy.
- [ ] **5.5** Record in Anumey's Lair `docs/stats.md` the same note the F2 fix got (what changed and
      why), for the maintainer to commit alongside.
- [ ] **5.6** Confirm nothing else needs changing: the merged-manifest permission list is unchanged
      (no new permission under D2), so the policy's permission table and Play's Data safety form
      stay as they are.

**Exit criteria:** policy edited and building; D2 confirmed.
**⏸ STOP for review. The maintainer commits, pushes and deploys the policy** (it can go live
before 1.6.1, since it describes behaviour that is already true today).

---

### Phase 6 — 1.6.1 release candidate: build, full regression, docs ⬜

- [ ] **6.1** `app/build.gradle.kts`: `versionCode = 7`, `versionName = "1.6.1"`.
- [ ] **6.2** Clean `assembleRelease` + `bundleRelease`; record APK/AAB sizes; confirm the merged
      release manifest: `SidebarService` has `android:process=":overlay"`, permissions are
      identical to 1.5.2 (the eleven in stats.md §9), `DISABLE_AD_INSPECTOR` and
      `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` are present, versionCode 7.
- [ ] **6.3** R8 check: `usage.txt` shows no removed member of `OverlaySnapshot` / `SliverConfig`
      that is used across the Bundle boundary; `mapping.txt` still maps `ColorMode` `DEFAULT` /
      `CUSTOM` to themselves.
- [ ] **6.4** Install the signed APK over 1.5.2 with `-r` on the phone, then **launch it**. Also record
      what happens to a running sliver across the in-place update (it may need START again, as with
      any app update; note the behaviour, don't change it).
- [ ] **6.5** Full regression on the phone:
  - V1–V11 from Phase 2
  - Phase 3's CPU checks and Phase 4's bitmap checks
  - Every screen at 360×640dp and 360×720dp (`wm size`/`wm density` override, then reset)
  - Customize dialog (Apply / Reset / Cancel)
  - Credits links
  - Overlay disclosure dialog (revoke overlay access with
    `appops set com.dicereligion.edgecase SYSTEM_ALERT_WINDOW default`, then restore)
  - Battery-optimisation prompt path
  - Reboot: nothing starts until the app is opened
- [ ] **6.6** Final memory table (Appendix A, all stages) next to Phase 0's baseline, in this
      document.
- [ ] **6.7** All instrumented tests pass on the emulator.
- [ ] **6.8** Standing checks (stats.md group C) re-run and recorded.
- [ ] **6.9** Docs:
  - `Docs/stats.md`: version banner, §2 architecture diagram (two processes), §5.1 / §5.2
    (statics removed, snapshot protocol, `isRunning(context)`), §5.10 / §5.16 / §5.18 (clock, dust),
    §5.17 `CrackFlashView` marked deleted (like §5.20), §3 directory tree, §6.6
    manifest, §7 feature rows (#46 crack flash → removed), §8 service lifecycle, §9 notification (D2), §10 (#11 fixed; new
    notes), Appendix C (new group for 1.6.1), and fix the stale "Working tree ≠ HEAD" banner
  - A new section in `Docs/FINDINGS.md` recording §2 of this plan
  - This document: tracker to ✅
- [ ] **6.10** Proposed Play release notes (maintainer's call): *"Uses much less memory in the
      background, and lighter animations save battery while the app is open."*
- [ ] **6.11** Restore the maintainer's settings (0.6) if anything changed them; leave the phone
      with the sliver running and the app closed.

**Exit criteria:** every check green and recorded; the signed AAB is ready.
**⏸ STOP. The maintainer commits and uploads 1.6.1.**

---

## 7. Risk register

| # | Risk | Where it's caught | Mitigation |
|---|---|---|---|
| K1 | Start → onPause race: RESUME skipped because the running check misses a just-started service → sliver never shows after leaving | **Confirmed in Phase 1 (S1: 70–99 ms blind)** | **Resolved by design:** binding replaces SUSPEND/RESUME (D5 change); verified by V1/V12 |
| K2 | Stale settings in a surviving `:overlay` process | **Confirmed in Phase 1** (PID 22964 reused after STOP) | Snapshot on every intent (D6); verified by V5 |
| K3 | Sticky restart happens while the Activity is open → sliver drawn over the app | Phase 1 S7c | **Resolved by design:** `onBind` precedes `onStartCommand`, so the restarted service stays hidden; verified by V13 |
| K4 | R8 strips something new | Phase 6 6.3–6.4 | Launch the signed build; read `usage.txt` |
| K5 | Throttled animation looks worse | Phase 3 3.6 | Before/after captures for the maintainer; the interval is one constant |
| K6 | Handler clock keeps ticking in the background | Phase 3 3.4 | Window-visibility unsubscription, verified by closed-app CPU |
| K7 | Recycled rows show the wrong icon | Phase 4 4.4 | Tag check before applying |
| K8 | Something in `:overlay` touches WebView (would need a data-dir suffix) | Phase 1 S4, Phase 6 overlay grep | Overlay code never imports ads/WebView (claim P6) |
| K9 | Installing a debug build or running tests against the phone wipes or blocks the maintainer's data | Standing rules | Release builds only on the phone; tests on the emulator with `ANDROID_SERIAL` |
| K10 | Policy wording drifts from what users see | Phase 5 5.3 | Wording written from a device screenshot |

---

## 8. Definition of done (1.6.1)

- [ ] G1: `:overlay` steady-state PSS measured and reported against 25.9 MB; the main process
      cached after leaving.
- [ ] G2: `send-trim-memory` accepted by the main process after leaving.
- [ ] G3: tap crack flash removed; main-menu idle CPU and frame rate measured below baseline; look approved.
- [ ] G4: no app icons loaded until Shortcuts is opened; bounded afterwards.
- [ ] G5: STOP means stopped, whatever is saved.
- [ ] G6: policy corrected (maintainer deploys).
- [ ] No regression in V1–V11, the standing checks, or the 14 existing tests plus the new ones.
- [ ] `stats.md` and `FINDINGS.md` updated. No commit made by Claude.

---

## 9. Noted, not in scope

- **Tray icons and `mutate()`** (stats.md limitation #3): `desaturateIcon` mutates a possibly
  shared drawable. Phase 2 moves this code into another process but doesn't change it.
- **The WebView that survives `AdHost.destroy()`**: harmless once the main process is
  reclaimable. Worth a look only if a future SDK update keeps the process from caching.
- **Android 11–12 notification**: still visible (no permission exists there). Lowering it to
  `IMPORTANCE_MIN` is possible, but how Android treats a MIN-importance FGS notification on those
  versions hasn't been verified, so it's not proposed.

---

## Appendix A — Measurement protocol

Use it the same way before (Phase 0) and after (Phases 2–4, 6). Always the release build, and the
same device.

**Memory stages** (run `measure.sh <label>` at each):
1. `am force-stop`, `logcat -c`, `am start -W -n com.dicereligion.edgecase/.MainActivity`; wait for
   `EdgeCaseAds: Plinth banner loaded`; take 3 samples 10 s apart → *fg-settled*.
2. Tap START (find it with `uiautomator dump`, id `btnStartService`); sample → *fg-svc*.
3. `input keyevent KEYCODE_HOME`; sample at 30 s, 60 s, 120 s → *home*.
4. Remove the task (`dumpsys activity recents` for the id, then `am stack remove <id>`); sample at
   30 s, 2 min, 5 min → *noact*. Also record `dumpsys activity oom | grep edgecase`.
5. Try `am send-trim-memory <main pid> COMPLETE`; record accepted or refused.

**CPU:**
- Per thread: `cat /proc/<pid>/task/*/stat` before and after a 20 s `adb shell sleep 20`; utime +
  stime (fields 14 and 15, counted after the `)`), 100 ticks = 1 core-second. The parser is
  Appendix C.
- Frames: `dumpsys gfxinfo com.dicereligion.edgecase reset`, wait 30 s, read *Total frames
  rendered*.
- Closed-app CPU: process total over 60 s after Home.

## Appendix B — `measure.sh`

```zsh
#!/bin/zsh
# Usage: measure.sh <label>   — full meminfo per EdgeCase process into raw/<label>.txt + one summary line each
label=$1; dir=${0:h}/raw; mkdir -p $dir; out=$dir/$label.txt
{
  echo "### $label  $(adb shell date)"
  adb shell ps -A -o PID,RSS,NAME | grep edgecase
  adb shell dumpsys activity oom | grep -i edgecase
  for pid in $(adb shell ps -A -o PID,NAME | grep edgecase | awk '{print $1}'); do
    echo "=== meminfo $pid"; adb shell dumpsys meminfo $pid
  done
} > $out 2>&1
for pid in $(adb shell ps -A -o PID,NAME | grep edgecase | awk '{print $1}'); do
  adb shell dumpsys meminfo $pid | awk -v L=$label -v P=$pid '
    /MEMINFO in pid/ { n=$0; sub(/.*\[com.dicereligion.edgecase/,"",n); sub(/\].*/,"",n); n=substr(n,1,30); if(n=="") n="(main)" }
    /^ *GL mtrack/ { gl=$3 }  /Java Heap:/ { jv=$3 }  /^ *Native Heap:/ { nh=$3 }
    /TOTAL PSS:/ { pss=$3; rss=$6; swp=$10 }  /WebViews:/ { wv=$NF }  /Activities:/ { act=$NF }
    END { printf "%-12s pid=%-6s PSS=%6.1fMB RSS=%6.1fMB SwapPSS=%6.1fMB GL=%5.1fMB Java=%5.1fMB Native=%5.1fMB Act=%s WV=%s %s\n",
          L,P,pss/1024,rss/1024,swp/1024,gl/1024,jv/1024,nh/1024,(act==""?"-":act),(wv==""?"-":wv),n }'
done
```

## Appendix C — per-thread CPU parser (`threads.py`)

```python
import sys, re
d, secs = sys.argv[1], float(sys.argv[2])   # dir holding t0.txt/t1.txt, sample length
def load(f):
    r = {}
    for l in open(f"{d}/{f}"):
        m = re.match(r'(\d+) \((.*)\) (.*)', l.strip())
        if m:
            rest = m.group(3).split(); r[m.group(1)] = (m.group(2), int(rest[11]) + int(rest[12]))
    return r
a, b = load('t0.txt'), load('t1.txt')
rows = sorted(((b[t][1] - a[t][1], b[t][0]) for t in b if t in a), reverse=True)
tot = sum(x for x, _ in rows)
print(f"total {tot} ticks in {secs:.0f}s = {tot/secs:.0f}% of one core |",
      ", ".join(f"{n}={x}" for x, n in rows[:6] if x > 0))
```

## Appendix D — Device helper scripts

All take no device argument; set `ANDROID_SERIAL` first. They sit together in one folder with
`measure.sh` (Appendix B) and `threads.py` (Appendix C), and `S=${0:h}` means "this folder".

### `tapid.sh` — tap a view by resource id

```zsh
#!/bin/zsh
# tapid.sh <resource-id-suffix> : taps the centre of the first view with that id
adb shell uiautomator dump /sdcard/ec.xml >/dev/null 2>&1
xy=$(adb shell cat /sdcard/ec.xml | tr '>' '\n' | grep "id/$1\"" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1 | tr -c '0-9\n' ' ' | awk '{print int(($1+$3)/2), int(($2+$4)/2)}')
[ -z "$xy" ] && { echo "NOT FOUND: $1"; exit 1; }
adb shell input tap ${=xy}; echo "tapped $1 at $xy"
```

### `state.sh` — is the service running, how many overlay windows

```zsh
#!/bin/zsh
# state.sh <label>: service running? (record has a live process) + EdgeCase overlay windows
run=$(adb shell dumpsys activity services com.dicereligion.edgecase | grep -A12 "ServiceRecord{.*/.SidebarService" | grep -c "app=ProcessRecord")
win=$(adb shell dumpsys window windows | grep -cE 'Window\{[0-9a-f]+ u0 com.dicereligion.edgecase\}')
echo "$1: service_running=$run overlay_windows=$win"
```

### `baseline.sh` — the full Appendix A protocol in one pass (~12 min)

```zsh
#!/bin/zsh
# Appendix A protocol, one pass. Usage: baseline.sh <prefix>
S=${0:h}; P=$1
pid_main(){ adb shell ps -A -o PID,NAME | awk '$2=="com.dicereligion.edgecase"{print $1}'; }
cpu_threads(){ # $1=label $2=secs
  local p=$(pid_main)
  adb shell "cat /proc/$p/task/*/stat" > $S/t0.txt; adb shell sleep $2; adb shell "cat /proc/$p/task/*/stat" > $S/t1.txt
  echo "$1: $(python3 $S/threads.py $S $2)"
}
echo "== $P  $(date)"
adb shell am force-stop com.dicereligion.edgecase; sleep 2; adb logcat -c
adb shell am start -W -n com.dicereligion.edgecase/.MainActivity | grep TotalTime
for i in $(seq 1 40); do adb logcat -d -s EdgeCaseAds | grep -q "banner loaded" && break; sleep 1; done
adb logcat -d -s EdgeCaseAds | grep "banner loaded"
sleep 10
for i in 1 2 3; do $S/measure.sh $P-1fg-s$i; sleep 10; done
$S/tapid.sh btnStartService; sleep 5
$S/state.sh "after START"
$S/measure.sh $P-2fg-svc
adb shell dumpsys gfxinfo com.dicereligion.edgecase reset >/dev/null
for i in 1 2 3; do cpu_threads "$P-cpu-menu-eyesopen-$i" 20; done
echo "gfx frames over the 60s above: $(adb shell dumpsys gfxinfo com.dicereligion.edgecase | grep 'Total frames rendered')"
adb shell input keyevent KEYCODE_HOME
sleep 30; $S/measure.sh $P-3home-30s
sleep 30; $S/measure.sh $P-3home-60s
sleep 60; $S/measure.sh $P-3home-120s
p=$(pid_main); a=$(adb shell "cat /proc/$p/stat" | awk '{print $14+$15}'); adb shell sleep 60; b=$(adb shell "cat /proc/$p/stat" | awk '{print $14+$15}')
echo "$P-cpu-closed-activity-alive: $((b-a)) ticks/60s"
T=$(adb shell dumpsys activity recents | grep -oE 'Recent #[0-9]+: Task\{[0-9a-f]+ #[0-9]+ type=standard A=10520:com.dicereligion.edgecase' | grep -oE '#[0-9]+ type' | grep -oE '[0-9]+')
echo "removing task $T"; adb shell am stack remove $T; sleep 3
echo "activities: $(adb shell dumpsys activity activities | grep -c 'com.dicereligion.edgecase/.MainActivity')"
sleep 27; $S/measure.sh $P-4noact-30s
sleep 90; $S/measure.sh $P-4noact-2m
sleep 180; $S/measure.sh $P-4noact-5m
adb shell dumpsys activity oom | grep -i edgecase
p=$(pid_main); out=$(adb shell am send-trim-memory $p COMPLETE 2>&1)
if echo "$out" | grep -q "Unable"; then echo "trim on main $p: REFUSED ($(echo "$out" | grep -o 'Unable[^.]*'))"; else echo "trim on main $p: accepted"; fi
a=$(adb shell "cat /proc/$p/stat" | awk '{print $14+$15}'); adb shell sleep 60; b=$(adb shell "cat /proc/$p/stat" | awk '{print $14+$15}')
echo "$P-cpu-closed-noact: $((b-a)) ticks/60s"
$S/state.sh "end"
echo "== done $(date)"
```

### `quiet.sh` — CPU + frames per 20 s, classified by whether the ad was redrawing

```zsh
#!/bin/zsh
# quiet.sh <label> <n>: n x 20 s samples on the current screen: CPU% of one core, our two threads,
# the ad's GPU thread, and frames rendered in the same 20 s. "ad=" > ~20 ticks means the ad redrew.
S=${0:h}; L=$1; N=$2
P=$(adb shell ps -A -o PID,NAME | awk '$2=="com.dicereligion.edgecase"{print $1}')
for i in $(seq 1 $N); do
  adb shell dumpsys gfxinfo com.dicereligion.edgecase reset >/dev/null
  adb shell "cat /proc/$P/task/*/stat" > $S/t0.txt; adb shell sleep 20; adb shell "cat /proc/$P/task/*/stat" > $S/t1.txt
  f=$(adb shell dumpsys gfxinfo com.dicereligion.edgecase | grep 'Total frames rendered' | grep -oE '[0-9]+$')
  python3 - $S $L $i $f <<'EOF'
import sys,re
d,L,i,f=sys.argv[1],sys.argv[2],sys.argv[3],int(sys.argv[4])
def load(fn):
    r={}
    for l in open(f"{d}/{fn}"):
        m=re.match(r'(\d+) \((.*)\) (.*)',l.strip())
        if m: rest=m.group(3).split(); r[m.group(1)]=(m.group(2),int(rest[11])+int(rest[12]))
    return r
a,b=load('t0.txt'),load('t1.txt')
by={}
for t in b:
    if t in a: by[b[t][0]]=by.get(b[t][0],0)+b[t][1]-a[t][1]
tot=sum(by.values())
ad=by.get('Chrome_InProcGp',0)
tag='QUIET' if ad<20 else 'AD-ACTIVE'
print(f"{L}-{i}: {tot/20:.0f}% core | Render={by.get('RenderThread',0)} UI={by.get('ligion.edgecase',0)} adGPU={ad} | frames={f} ({f/20:.1f}/s) {tag}")
EOF
done
```
