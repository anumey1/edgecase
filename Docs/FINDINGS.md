# EdgeCase — Findings

> Companion to `Docs/EdgeCase_Investigation_Brief.md`, using its §6 template.
> **Started:** 2026-09-15 · **Device:** Pixel 9 Pro XL (`komodo_beta`), Android 17 beta,
> build `CP41.260814.003.B1` · **Builds tested:** 1.5.0 (versionCode 4) and 1.5.1 (versionCode 5),
> both release builds signed with the upload key and sideloaded over adb.
> **1.5.1 was a test build and was never uploaded; the fix ships to Play as 1.5.2 (versionCode 6)**,
> which is identical apart from the version bump.
>
> | # | Symptom | Verdict |
> |---|---|---|
> | 3 | Shake opens ad inspector over other apps | **CONFIRMED and FIXED** in 1.5.1 |
> | 2 | EdgeCase task appears in Recents with a black thumbnail | **CONFIRMED mechanism** (same as #3), fix verified; black thumbnail not directly observed |
> | 1 | WhatsApp reaction tray shows a black background | **RULED OUT as an EdgeCase bug** — closed as a WhatsApp/platform issue |

---

## Corrections to the brief

The brief was written without the codebase in hand. These premises were checked and are wrong for
EdgeCase; later sections rely on the corrected versions.

| Brief | Reality | Evidence |
|---|---|---|
| Jetpack Compose codebase | XML layouts + custom `View`s; no Compose | No `compose` in `app/build.gradle.kts` or `app/src/main` |
| Sliver sits bottom-right; set `gravity = BOTTOM or END` | Side (left/right) and height are user-chosen; gravity is `TOP` + a y offset | `SidebarService.kt:177–179`, `:199–201`. **Applying A4's gravity item would break positioning** |
| `MobileAds.initialize` may be in `Application.onCreate` or the service (C4) | No `Application` subclass; init runs only inside `AdHost`, constructed only by `MainActivity` | `AdHost.kt:210`; no `: Application` in `app/src/main/java` |
| A hard-coded `setTestDeviceIds` to remove (C1) | None. The phone is registered as a test device in the AdMob console only | No `setTestDeviceIds` / `RequestConfiguration` in `app/src/main/java` |
| Ad inspector is "always disabled in production builds" | Gated on test-device registration, not build type — it opened from a **release** build | Symptom 3 baseline below |
| The disable key may not exist for the SDK in use (C3) | It exists in `ads-mobile-sdk:1.4.0` | Runtime jar strings: `com.google.android.libraries.ads.mobile.sdk.flag.DISABLE_AD_INSPECTOR`, *"Ad inspector is disabled in the AndroidManifest.xml."* |
| One commit per task | Not followed — the maintainer's standing rule is that Claude never commits | — |

---

### Symptom 3: shaking the phone opens the AdMob ad inspector over whatever app is in the foreground

- **Hypothesis tested:** the phone is an AdMob test device with a shake gesture configured; the SDK's
  gesture listener is registered on the first ad request from `MainActivity` and survives because
  `SidebarService` runs in the same process and keeps it alive.
- **Evidence gathered:**
  - *Code.* `SidebarService` has no `android:process` attribute, so it shares the app process with
    `AdHost`. No ad import in the overlay path
    (`grep -rn "ads.mobile.sdk\|gms.ads" app/src/main/java/ | grep -v "MainActivity\|AdHost"` → empty).
  - *Baseline, 1.5.0.* Service running (`isForeground=true foregroundId=9182`), banner loaded, app sent
    Home, YouTube opened, phone shaken → **inspector opened**:
    ```
    I ActivityTaskManager: START u0 {xflg=0x4 cmp=com.dicereligion.edgecase/com.google.android.libraries.ads.mobile.sdk.common.AdActivity (has extras)}
      with LAUNCH_MULTIPLE from uid 10520 (com.dicereligion.edgecase) (sr=262250927) (BAL_ALLOW_NON_APP_VISIBLE_WINDOW) result code=0
    I wm_create_activity: [0,164599716,2026,com.dicereligion.edgecase/…sdk.common.AdActivity,…]
    ```
    The WebView loaded `admob-gmats.uc.r.appspot.com` with page title *Ad inspector*.
  - **Why it appeared over other apps:** Android blocks background activity launches unless an
    exemption applies. The one granted was `BAL_ALLOW_NON_APP_VISIBLE_WINDOW` — the app owns a visible
    non-activity window, i.e. **the sliver overlay**. Without the overlay the launch would have been
    refused.
  - *After, 1.5.1* (manifest flag added). Service restarted, shaken with EdgeCase in the foreground and
    again over YouTube → **nothing opened**. No `AdActivity` in `ActivityTaskManager: START` lines, no
    inspector log line, no new EdgeCase task. Banner still `Plinth banner loaded (411×128dp)`.
- **Verdict:** CONFIRMED, and FIXED in 1.5.1.
- **Action taken:** `app/src/main/AndroidManifest.xml` —
  ```xml
  <meta-data
      android:name="com.google.android.libraries.ads.mobile.sdk.flag.DISABLE_AD_INSPECTOR"
      android:value="true" />
  ```
  Present in both merged manifests (debug and release). Applies to all build types.
- **Action deliberately NOT taken, and why:**
  - *Deleting the test-device entry (brief C2's preferred option).* That registration is what keeps
    the maintainer's own ad views and taps out of invalid-traffic accounting, on a publisher account
    shared with Mach2. Removing it would expose the account to exactly that risk.
  - *Moving `MobileAds.initialize` (C4).* Already Activity-only; the problem is process lifetime, which
    moving the call cannot change.
  - *Running `SidebarService` in a separate process.* Would isolate the SDK, but breaks the two
    `@Volatile` static flags (`SidebarService.isRunning`, `MainActivity.isForeground`) that the
    service and Activity share. Not justified now that the flag works.
  - *Using a manifest placeholder as the brief sketches.* Unnecessary — a literal meta-data value
    does the same thing with less indirection.
- **Open questions for the human:**
  - Optionally set the test device's ad-inspector gesture to **None** (AdMob → Settings → Test
    devices) as a second layer. **Keep the device registered.**
  - The fix ships only with 1.5.1; the Play build (1.5.0) still has the behaviour for any registered
    test device — which is only this phone.

---

### Symptom 2: an EdgeCase task appears in Recents unprompted, with a black thumbnail

- **Hypothesis tested:** brief B's cause #3 — a third-party SDK activity launched into EdgeCase's task
  — with the ad inspector as the SDK activity, making this the same root cause as symptom 3.
- **Evidence gathered:**
  - *Baseline, 1.5.0.* Before the shake, Recents #0 was the normal EdgeCase task
    (`#2026, realActivity=…MainActivity`), then Home. After the shake-over-YouTube, `AdActivity` was
    created **in task 2026** (`wm_create_activity: […,2026,…AdActivity]`) and that task sat at
    **Recents #0, above YouTube** (#2027) — EdgeCase's task brought forward unprompted, topped by an SDK
    WebView activity.
  - *Merged manifest.* `…sdk.common.AdActivity` declares no `excludeFromRecents` and no
    `taskAffinity`, so it inherits EdgeCase's affinity and joins its task.
  - *Code audit (B2).* `grep -rn "FLAG_SECURE\|excludeFromRecents\|noHistory\|taskAffinity\|launchMode"
    app/src/main` → empty. No `PendingIntent` or notification content intent
    (`grep "PendingIntent\|setContentIntent"` → empty). `startActivity` call sites:
    `MainActivity.kt:375` (`openUrl`, user tap), `:634` and `:656` (overlay / battery-optimisation
    settings, from the START flow), and `SidebarService.kt:395` (tray icon tap →
    `getLaunchIntentForPackage` of the chosen app). None launches without a user action.
  - *After, 1.5.1.* Recents after the shakes: Home, YouTube, `com.painless.pc` — no EdgeCase task
    created by the shake.
- **Verdict:** CONFIRMED mechanism — the inspector launch places an SDK activity in EdgeCase's task and
  surfaces it in Recents. **The black thumbnail itself was not captured** on this run; it is
  attributed, not observed. A WebView-backed activity whose snapshot is taken mid-load or after the
  WebView surface is released is a plausible source, but unproven.
- **Action taken:** none beyond symptom 3's flag, which removes the launch.
- **Action deliberately NOT taken, and why:** `excludeFromRecents`, `noHistory` or `taskAffinity` on
  `MainActivity` (it is the launcher activity and must stay in Recents), or a `tools:node="merge"`
  override on the SDK's `AdActivity` (it also hosts real full-screen ad clicks; changing its task
  behaviour to fix a debug tool is the wrong trade).
- **Open questions for the human:** does an unprompted black EdgeCase task **still** appear on 1.5.1
  over the next few days? If yes, capture it immediately:
  ```bash
  adb logcat -d | grep "ActivityTaskManager: START u0" | grep -i edgecase
  adb shell dumpsys activity recents | grep -iA6 edgecase
  ```

---

### Symptom 1: in WhatsApp, the reaction tray's background renders solid dark grey/black

- **Hypothesis tested:** (a) EdgeCase's overlay interferes with WhatsApp's blur; (b) the brief's
  primary hypothesis, cross-window blur disabled system-wide.
- **Evidence gathered (device, 2026-09-15, on 1.5.1 — the maintainer reports the symptom still
  reproduces):**
  - **Blur is enabled system-wide**, which rules out hypothesis (b):
    `settings get global disable_window_blurs` → `0` ("Allow window-level blurs" is on);
    `ro.surface_flinger.supports_background_blur` → `1`; `dumpsys window` → `mBlurEnabled=true`;
    `settings get global low_power` → `0` (battery saver off). `dumpsys SurfaceFlinger` shows live
    layers with `blurRegions.size()=2` and `disableBackgroundBlur=false` — SurfaceFlinger is actively
    compositing blurs for other windows.
  - **EdgeCase's only window is the sliver.** `dumpsys window windows` lists a single
    `com.dicereligion.edgecase` window (#9) — no stray or full-screen EdgeCase surface.
  - **Other overlay holders:** `appops … SYSTEM_ALERT_WINDOW: allow` for `com.truecaller` (last used
    ~49 min before the check), `com.dicereligion.edgecase`, and `naukriApp.appModules.login`.
    (`com.painless.pc` does not hold the permission.)
  - WhatsApp `2.26.35.75`, updated 2026-09-10. No blur- or scrim-related log lines from it.
  - Prior evidence: reproduces with the sliver stopped.
- **Code audit:** A4 window-hygiene audit of `SidebarService.kt`:

  | A4 item | Sliver window | Tray window | Result |
  |---|---|---|---|
  | Explicit size, not `MATCH_PARENT` | `config.widthDp × heightDp` (27×38dp default) | `trayWidthDp × trayHeightDp` (80×266dp) | ✅ |
  | `FLAG_NOT_FOCUSABLE` | set (`:170`) | set (`:193`) | ✅ — implies `FLAG_NOT_TOUCH_MODAL` |
  | `FLAG_LAYOUT_NO_LIMITS` only if needed | set — lets the fang sit flush at the screen edge | not set | ✅ |
  | No `FLAG_DIM_BEHIND` | absent | absent | ✅ |
  | No `FLAG_SECURE` | absent anywhere | absent anywhere | ✅ |
  | `PixelFormat.TRANSLUCENT`, transparent root | `TRANSLUCENT`; `ArcSliverView` draws only the fang path | `TRANSLUCENT`; opaque serpent-scale backdrop, but only while the tray is open | ✅ |
  | Teardown removes views | `onDestroy` removes both, guarded by `isAttachedToWindow` (`:123–127`); suspend path `:252–256` | same | ✅ |
  | Gravity `BOTTOM or END` | **not applicable** — user-positioned `TOP` + y offset | — | ⚠️ do not apply |

  Structurally, EdgeCase's overlay sits *above* WhatsApp, and a cross-window blur samples layers
  *below* the blurring window, so the sliver cannot be in WhatsApp's blur input.
- **Verdict:** RULED OUT as an EdgeCase bug; closed per brief §A5. Blur is available and working
  system-wide, the symptom persists with the sliver stopped, EdgeCase contributes only a small window
  that sits above WhatsApp and so cannot be in its blur input, and the window audit is clean. What
  remains is how WhatsApp renders its reaction tray (in-app blur or a dark scrim of its own) on this
  Android 17 beta build — outside EdgeCase's reach. The exact WhatsApp mechanism was **not** traced; the
  reaction tray was not open during the device checks.
- **Action taken:** none. The A4 audit found nothing to correct.
- **Action deliberately NOT taken, and why:** no window-flag or gravity change — the audit is clean,
  and the gravity item would regress a feature. No A2 blur-probe build — `mBlurEnabled=true` and
  `disable_window_blurs=0` already answer the question it asks.
- **Open questions for the human:** none required. If it ever needs reopening, the cheapest
  discriminators are: the camera-on-screen check (real rendering vs capture artifact), reproducing
  with Truecaller's overlay permission revoked, and reproducing after the next Android 17 beta or
  WhatsApp update.
