# EdgeCase — Overlay / Recents / Ad Inspector Investigation & Fix Brief

**Target:** Claude Code session running inside the EdgeCase Android project.
**Author intent:** Investigate three reported symptoms, *verify root cause before changing anything*, then apply only the fixes that the evidence supports. Additionally, fully remove the shake-to-open ad debug feature.

---

## 0. Ground rules for this session

1. **Investigate before editing.** Two of the three symptoms have a plausible non-EdgeCase cause. Do not apply a fix for a cause you have not confirmed.
2. **No blanket manifest changes.** Specifically, do not add `android:noHistory` or `android:taskAffinity=""` to every activity (reasons in Task B).
3. **Report findings in a `FINDINGS.md`** at repo root as you go: hypothesis, evidence, verdict. Use the report template in §5.
4. **One commit per task**, with the evidence summarised in the commit body.
5. If a step needs a physical device and you cannot run `adb`, **stop and emit the exact commands for the human to run**, then wait for output. Do not guess the result.

---

## 1. App context

- **EdgeCase** is an Android app that draws a thin, fang-like sliver at the **bottom-right** of the screen. The sliver is an overlay window (`TYPE_APPLICATION_OVERLAY`) drawn over all other apps, kept alive by a long-running background/foreground service.
- The **Google Mobile Ads (AdMob) SDK** is integrated.
- Kotlin / Jetpack Compose codebase.

### Reported symptoms

| # | Symptom |
|---|---------|
| 1 | In WhatsApp, long-pressing a message to react causes the chat behind the reaction tray to render as solid dark grey/black instead of a blurred/dimmed chat. Only the emoji tray is visible. |
| 2 | An EdgeCase task appears in the Recents/Overview carousel unprompted, with a **black (empty) thumbnail**. |
| 3 | Shaking the phone opens the AdMob **ad inspector** UI, over whatever app is in the foreground, even when EdgeCase is not in use. |

### Critical piece of evidence already gathered

> Symptom 1 **still reproduces with the sliver stopped.** This strongly suggests EdgeCase is not the cause.

---

## 2. Prior AI analysis — what to ignore

A previous AI produced an explanation for symptom 1 that is **factually wrong**. It is recorded here so this session does not rediscover and act on it:

- It claimed WhatsApp uses `WindowManager.LayoutParams.FLAG_BLUR_BEHIND`. That flag has been **deprecated and a no-op since API 15**. Modern cross-window blur is `Window.setBackgroundBlurRadius()` / `setBlurBehindRadius()` (API 31+), gated on `WindowManager.isCrossWindowBlurEnabled()`.
- It claimed a full-screen `TYPE_APPLICATION_OVERLAY` layer makes SurfaceFlinger "fail the blur composition pass and default to an opaque black fallback". **No such fallback path exists.**
- Logical flaw: a cross-window blur samples the layers **below** the blurring window. The EdgeCase overlay sits **above** WhatsApp, so it is not in the sample set and its size cannot affect what WhatsApp blurs beneath itself.

It also invented an API for symptom 3: `RequestConfiguration.AdInspectorGesture` / `setAdInspectorGesture()` **does not exist**. Do not attempt to call it. See Task C for the real mechanism.

Its symptom-2 direction (a task left in Recents) was reasonable; its prescribed fixes were over-broad.

---

## 3. Task A — WhatsApp blur/black background

**Working hypothesis (most likely):** cross-window blur is disabled device-wide, and the dark fill is WhatsApp's own fallback scrim. Candidate triggers: battery saver, an accessibility setting ("Reduce transparency and blur" on One UI, "Remove animations"), a developer-options toggle ("Disable HW overlays"), or a low-end/blur-unsupported ROM path.

**Secondary hypothesis:** nothing is wrong on screen at all, and the black region is a **screenshot capture artifact** — blurred surfaces frequently fail to capture in screenshots and task snapshots on some ROMs. Note that symptom 2's thumbnail is *also* black; one capture-layer bug could explain both appearances.

### A1. Cheap human checks (ask the human to run these first)

1. Point another camera at the phone while the reaction tray is open. **Is the screen itself dark, or only the screenshot?**
2. Pull down the notification shade over a colourful app; open Recents; open the volume panel. Are *those* backgrounds blurred? If not, blur is off system-wide.
3. Check: Battery saver on? Settings → Accessibility → "Reduce transparency and blur" / "Remove animations"? Developer options → "Disable HW overlays" or "Force GPU rendering"?
4. Reproduce after **fully uninstalling EdgeCase and rebooting** (stopping the sliver is not the same as having no overlay window and no live process).
5. List other apps holding `SYSTEM_ALERT_WINDOW` (Messenger chat heads, screen dimmers, blue-light filters, gesture-nav apps, Truecaller). Any of them could hold an overlay.

### A2. Definitive programmatic check

Write a throwaway debug entry point (a `@Preview`-independent debug activity, an instrumented test, or a one-shot log in the existing debug build) that reports:

```kotlin
val wm = context.getSystemService(WindowManager::class.java)
Log.d("BlurProbe", "crossWindowBlurEnabled=${wm.isCrossWindowBlurEnabled}")
wm.addCrossWindowBlurEnabledListener { enabled ->
    Log.d("BlurProbe", "blur toggled -> $enabled")
}
```

Run it (a) with the sliver service running and (b) with it stopped/uninstalled.

- `false` in both cases → **blur is disabled by the device/system, not by EdgeCase. Close symptom 1 as not-our-bug.**
- `true` while the symptom reproduces → blur is available and WhatsApp is choosing or failing to use it; still not something EdgeCase can cause, but note it in `FINDINGS.md`.
- Differs between (a) and (b) → genuinely interesting; escalate and report before fixing.

### A3. Window enumeration while reproducing

With the reaction tray open, from a second shell:

```bash
adb shell dumpsys window visible-apps | grep -E "Window\{|mOwner|package="
adb shell dumpsys window windows | grep -E "Window #|ty=|fl=|fmt=" 
adb shell dumpsys SurfaceFlinger --list
```

Record whether **any** EdgeCase window is present when the sliver is supposedly stopped. If one is, that is a separate real bug (service/window not being torn down) and should be fixed regardless of symptom 1.

### A4. Codebase audit (do this regardless of the verdict)

Locate the overlay window setup:

```bash
rg -n "TYPE_APPLICATION_OVERLAY|WindowManager.LayoutParams|addView\(|SYSTEM_ALERT_WINDOW" --type kotlin --type xml
```

Then confirm and, where wrong, correct the following **window hygiene** items. These are correct practice for a sliver overlay independent of symptom 1:

- Width/height are **explicit dp sizes for the sliver**, not `MATCH_PARENT`. A full-screen transparent host layer is wasteful and invites unrelated input/compositing oddities.
- `gravity = Gravity.BOTTOM or Gravity.END`.
- Flags include `FLAG_NOT_FOCUSABLE` and `FLAG_NOT_TOUCH_MODAL`. Add `FLAG_LAYOUT_NO_LIMITS` only if the sliver must sit in the gesture/inset area, and document why.
- **`FLAG_DIM_BEHIND` must not be set.**
- **`FLAG_SECURE` must not be set** on the overlay or on any activity (see Task B — it blanks task snapshots).
- `format = PixelFormat.TRANSLUCENT`, and the root view's background is genuinely transparent (check for a stray `windowBackground` or `Surface`/`Box` with an opaque colour in the Compose tree).
- Service teardown removes the view: `windowManager.removeViewImmediate(view)` on `onDestroy()` / on the stop path, and the view reference is nulled. Verify with A3 that no window survives a stop.

### A5. Decision gate

If A2 returns `false` with EdgeCase uninstalled, **write the conclusion in `FINDINGS.md` and do not chase symptom 1 further.** Apply only A4.

---

## 4. Task B — EdgeCase task appearing in Recents with a black thumbnail

**What a black/empty thumbnail usually means:** the task snapshot could not be taken. Common causes, in order of likelihood:

1. The activity (or its window) has **`FLAG_SECURE`** set.
2. The activity was created but **never drew a frame** (e.g. launched headlessly, or a transparent/`Theme.Translucent` activity with nothing in it).
3. A **third-party SDK activity** was launched into EdgeCase's task — the AdMob ad inspector is a prime suspect here, which would make symptoms 2 and 3 the same root cause.
4. Snapshot discarded by the system (low memory) — benign.

### B1. Find out what launched it

```bash
# Clear, then reproduce (shake the phone / wait for the task to appear)
adb logcat -c
adb logcat | grep -iE "ActivityTaskManager|ActivityManager" 
# Look for:  START u0 {cmp=com.yourpkg/.SomeActivity ...} from uid <N>
```

Then inspect the task:

```bash
adb shell dumpsys activity recents | grep -iA5 "<your.package.name>"
adb shell dumpsys activity activities | grep -iE "Hist|taskId|realActivity|<your.package.name>"
```

Record: **which component**, **which uid launched it**, and **whether it is your activity or an SDK activity.**

### B2. Codebase audit

```bash
rg -n "startActivity|PendingIntent|FLAG_ACTIVITY_NEW_TASK|FLAG_SECURE|excludeFromRecents|noHistory|taskAffinity|launchMode" --type kotlin --type xml
```

Check:

- Does the overlay service ever call `startActivity()`? If so, from where, and on what trigger?
- Is any `PendingIntent` (notification action, tile, widget) firing unintentionally?
- Does the foreground-service notification's content intent launch an activity that shouldn't be sticky?
- Is `FLAG_SECURE` set anywhere? If yes and it isn't deliberate, remove it — that alone may explain the black thumbnail.

### B3. Fix — targeted only

Once B1 names the component:

- **If it is an SDK/ad inspector activity:** completing Task C should remove the trigger. Re-test before adding any manifest attribute.
- **If it is an EdgeCase activity that genuinely should not persist in Recents:** add `android:excludeFromRecents="true"` **to that one activity**, and add `Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS` at the launch site. If the activity is a fire-and-forget trampoline, call `finishAndRemoveTask()` when it completes.
- **If the activity should not exist at all** (a trampoline that could be a service/broadcast call): delete it rather than hiding it.

**Do not:**
- Add `android:noHistory="true"` to all activities. It finishes an activity as soon as it stops being visible, so a settings screen dies behind a permission dialog, the photo picker, or an ads consent flow.
- Add `android:taskAffinity=""` broadly — it changes task grouping and interacts badly with launch modes and `FLAG_ACTIVITY_NEW_TASK`.
- Set `excludeFromRecents` on the main launcher activity; the user needs it in Recents.

---

## 5. Task C — Remove the shake-to-open ad debug feature entirely

### What this feature actually is

It is **ad inspector**, a debugging tool built into the Google Mobile Ads SDK (Android SDK 20.0.0+). It is **not** code in this repo, so there is no local function to delete. It is triggered by:

1. The device being registered as an **AdMob test device**, and
2. A **launch gesture** (shake / double-flick) selected for that device **in the AdMob web console**.

The SDK learns the gesture setting when the app makes an ad request. Because EdgeCase keeps its process alive with a persistent overlay service, the sensor listener stays registered after the user leaves the app — which is why the shake fires over other apps.

**Worth noting before any work:** ad inspector is always disabled in production builds and requires a registered test device, so end users cannot hit this. It is a debug-build annoyance, not a shipping bug. Remove it anyway, per the requirement.

`RequestConfiguration.setAdInspectorGesture(...)` does not exist. Do not write it.

### C1. Code: stop registering this device as a test device

```bash
rg -n "setTestDeviceIds|RequestConfiguration|MobileAds.initialize|MobileAds\.|AdRequest.Builder|ADMOB_APP_ID|com.google.android.gms.ads" --type kotlin --type xml --type gradle
```

- Remove any `RequestConfiguration.Builder().setTestDeviceIds(listOf("..."))` that hard-codes this phone's device hash. If test-device behaviour is still wanted for ad fill testing, gate it behind a `BuildConfig` flag that is **off by default**, or use Google's official test ad unit IDs instead (which do not require test-device registration).
- Note the exact GMA SDK version from the Gradle files and record it in `FINDINGS.md` — the disable option in C3 depends on it.

### C2. AdMob console (human action — emit as an instruction, you cannot do this)

Instruct the human to:

1. Open the AdMob console → **Settings → Test devices**.
2. Find the entry for this phone.
3. Either set its **ad inspector launch gesture to "None"/disabled**, or **delete the test-device entry entirely** (preferred, given the goal is full removal).
4. Wait ~15 minutes for propagation, then launch EdgeCase and let it make one ad request so the SDK picks up the new setting.
5. Force-stop and relaunch EdgeCase, then shake the phone to confirm nothing appears.

Without this step, the gesture can keep working even after code changes, because the setting lives server-side against the device.

### C3. Build-level disable (belt and braces)

Google documents a manifest-placeholder switch that prevents ad inspector from launching by gesture, debug menu, or API call. **Verify the exact key and minimum SDK version against the current docs before implementing** — it is documented for the GMA Next-Gen SDK (1.3.0+) under "Disable ad inspector", and availability on the legacy SDK line must be checked:

- <https://developers.google.com/admob/android/next-gen/ad-inspector/disable-ad-inspector>

Intended shape (confirm against docs, do not copy blindly):

```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        debug   { manifestPlaceholders["disableAdInspector"] = true }
        release { manifestPlaceholders["disableAdInspector"] = true }
    }
}
```

with the corresponding `<meta-data>` entry in `AndroidManifest.xml` as the docs specify. If the key does not exist for the SDK version in use, **say so in `FINDINGS.md` and skip this step** rather than inventing a placeholder name. Also consider whether upgrading the GMA SDK is in scope; if it is not, C1 + C2 are sufficient.

### C4. Architectural fix: don't initialise ads in the always-alive process path

```bash
rg -n "MobileAds.initialize" --type kotlin
```

If `MobileAds.initialize()` is called from `Application.onCreate()` or from the overlay service, move it to the first activity/screen that actually shows an ad. Rationale:

- The sensor listener and the SDK's memory footprint no longer persist for the entire lifetime of the overlay service.
- Fixes the class of bug where an ads-SDK side effect leaks into the always-on process.

Make sure ad loading still works on the screens that show ads, and that initialisation is idempotent / guarded so it isn't called repeatedly.

### C5. Verification

- [ ] Shake the phone with EdgeCase in the foreground → nothing appears.
- [ ] Shake with EdgeCase backgrounded but the sliver running → nothing appears.
- [ ] Shake with EdgeCase force-stopped → nothing appears.
- [ ] `adb logcat | grep -i "inspector"` during shakes shows no ad-inspector activity.
- [ ] `adb shell dumpsys activity recents` shows no new task after shaking.
- [ ] Ads still load correctly on the screens that are supposed to show them.

---

## 6. Findings report template

Write this to `FINDINGS.md`. One block per symptom.

```markdown
### Symptom N: <one line>
- Hypothesis tested:
- Evidence gathered (commands run / output / code read):
- Verdict: CONFIRMED / RULED OUT / INCONCLUSIVE
- Action taken:
- Action deliberately NOT taken, and why:
- Open questions for the human:
```

---

## 7. Acceptance criteria

1. `FINDINGS.md` exists with a verdict for all three symptoms, and each verdict cites concrete evidence (command output or a file/line reference), not reasoning alone.
2. Symptom 1: either proven not to be EdgeCase's fault and closed, or a confirmed mechanism is documented. Window hygiene items from §3 A4 applied either way.
3. Symptom 2: the launching component is named, and the fix is scoped to that component. No blanket `noHistory` / `taskAffinity` changes anywhere in the manifest.
4. Symptom 3: all of C5 passes. No invented APIs in the codebase.
5. App builds, installs, launches; sliver appears and disappears correctly; overlay window is confirmed gone via `dumpsys window` after stopping the service.
6. Any step that could not be completed without the human is listed explicitly with the exact commands or console actions needed.
