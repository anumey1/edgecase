# EdgeCase — Google Play Store Publication Roadmap

> **Document Type:** Take-to-Market / Pre-Publication Checklist
> **App:** EdgeCase
> **Package:** `com.dicereligion.edgecase`
> **Current Version (Code):** 1 (1.0)
> **Current UI Version Label:** v1.2.1
> **Status:** Code-complete, pre-publication engineering required
> **Last Updated:** 2026-07-06

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Pre-Launch Code Changes Required](#2-pre-launch-code-changes-required)
3. [Ad Integration Strategy (AdMob)](#3-ad-integration-strategy-admob)
4. [Security & Release Signing](#4-security--release-signing)
5. [Permissions & Policy Compliance](#5-permissions--policy-compliance)
6. [Store Listing Requirements](#6-store-listing-requirements)
7. [Testing & Pre-Launch Quality](#7-testing--pre-launch-quality)
8. [Monetization Strategy](#8-monetization-strategy)
9. [Post-Launch Maintenance](#9-post-launch-maintenance)
10. [Complete Actionable Checklist](#10-complete-actionable-checklist)

---

## 1. Executive Summary

EdgeCase is a feature-complete edge-launcher app themed with a "Hellenic Serpent" aesthetic. The codebase is finished and compiles cleanly. However, **the following areas need work before any Play Store submission is possible**:

| Area | Current State | Action Required |
|------|--------------|-----------------|
| **R8 / ProGuard** | Minification disabled, stock ProGuard file empty | Enable R8, write keep rules |
| **App Signing** | No release keystore configured | Generate release keystore, configure signing |
| **Backup Rules** | Stock templates, completely empty | Configure proper backup rules for SharedPreferences |
| **Ads** | None | Integrate AdMob SDK, place ad units |
| **Privacy Policy** | None | Write and host privacy policy URL |
| **Foreground Service Declaration** | No prominent disclosure | Add justification in Play Console |
| **Dummy Button** | Shows a Toast stub | Remove or implement real feature |
| **Store Assets** | None (except app icon) | Create screenshots, feature graphic, description |
| **Testing** | No formal testing | Set up internal test track, 20 testers minimum (new accounts) |
| **Version Code** | 1 | Bump to appropriate release version |

---

## 2. Pre-Launch Code Changes Required

### 2.1 Enable R8 Minification & Obfuscation

**File:** `app/build.gradle.kts`  
**Change:**

```kotlin
// Current (line 24):
release {
    isMinifyEnabled = false
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}

// Required:
release {
    isMinifyEnabled = true
    shrinkResources = true    // NEW: remove unused resources
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

**Impact:** This enables R8 shrinking, obfuscation, and optimization on release builds. Reduces APK/AAB size and makes reverse engineering harder. `shrinkResources` removes unused drawables, layouts, etc.

### 2.2 Write ProGuard/R8 Keep Rules

**File:** `app/proguard-rules.pro`  
**Current State:** Completely empty (only comments). **This is critical.** Without proper keep rules, R8 will strip or obfuscate classes referenced by reflection, XML, or the manifest.

**Required Keep Rules:**

```proguard
# ── AppCompat & RecyclerView ──────────────────────────
-keep class androidx.appcompat.** { *; }
-keep class androidx.recyclerview.** { *; }
-dontwarn androidx.appcompat.**
-dontwarn androidx.recyclerview.**

# ── View constructors used by XML inflation ───────────
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ── Custom Views (ArcSliverView, PositioningView, DustParticleView) ──
-keep class com.dicereligion.edgecase.ArcSliverView { *; }
-keep class com.dicereligion.edgecase.PositioningView { *; }
-keep class com.dicereligion.edgecase.DustParticleView { *; }

# ── Service declared in manifest ──────────────────────
-keep class com.dicereligion.edgecase.SidebarService { *; }

# ── Activity declared in manifest ─────────────────────
-keep class com.dicereligion.edgecase.MainActivity { *; }

# ── Data classes (may be serialized/deserialized) ─────
-keep class com.dicereligion.edgecase.AppInfoData { *; }
-keep class com.dicereligion.edgecase.ShortcutStateManager$AltarItem { *; }

# ── Adapters (RecyclerView binding may use reflection) ─
-keep class com.dicereligion.edgecase.ActiveShortcutsAdapter { *; }
-keep class com.dicereligion.edgecase.AvailableAppsAdapter { *; }
-keep class com.dicereligion.edgecase.ShortcutDragCallback { *; }
-keep class com.dicereligion.edgecase.ShortcutStateManager { *; }

# ── Event listeners / callbacks ───────────────────────
-keepclassmembers class * {
    void onClick(android.view.View);
    boolean onTouch(android.view.View, android.view.MotionEvent);
    void onSwipeListener();
}

# ── Animator listeners used via object expressions ────
-keepclassmembers class * {
    void onAnimationStart(android.animation.Animator);
    void onAnimationEnd(android.animation.Animator);
    void onAnimationCancel(android.animation.Animator);
    void onAnimationRepeat(android.animation.Animator);
}

# ── Retain SourceFile and LineNumberTable for crash reports ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── AdMob (when integrated) ───────────────────────────
# -keep class com.google.android.gms.ads.** { *; }
# -keep class com.google.android.gms.ads.identifier.** { *; }
# -dontwarn com.google.android.gms.ads.**

# ── General Android ───────────────────────────────────
-keep class android.support.v7.widget.** { *; }
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.view.View
```

### 2.3 Configure Backup Rules

**File:** `app/src/main/res/xml/backup_rules.xml`  
**Current State:** Empty template — no rules defined.

**Required:**
EdgeCase uses `SharedPreferences` with the file `"EdgeCasePrefs"` containing four keys: `saved_shortcuts_order`, `saved_shortcuts`, `sliver_side`, `sliver_y_bias`. This data should be backed up for user convenience.

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="sharedpref" path="EdgeCasePrefs.xml" />
</full-backup-content>
```

### 2.4 Configure Data Extraction Rules (Android 12+)

**File:** `app/src/main/res/xml/data_extraction_rules.xml`  
**Current State:** Empty template.

**Required:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="sharedpref" path="EdgeCasePrefs.xml" />
    </cloud-backup>
    <device-transfer>
        <include domain="sharedpref" path="EdgeCasePrefs.xml" />
    </device-transfer>
</data-extraction-rules>
```

### 2.5 Release Signing Configuration

**File:** `app/build.gradle.kts`  
Add a `signingConfigs` block inside the `android { }` block:

```kotlin
android {
    // ... existing config ...

    signingConfigs {
        create("release") {
            storeFile = file("../edgecase-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: "edgecase"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... existing ...
        }
    }
}
```

> **⚠️ IMPORTANT:** Never commit passwords or keystore files to Git. Use environment variables or a secure CI/CD secret manager. Add `*.keystore` and `*.jks` to `.gitignore` (already present in this project).

**To generate the keystore:**
```bash
keytool -genkey -v -keystore edgecase-release.keystore \
    -alias edgecase -keyalg RSA -keysize 2048 -validity 10000 \
    -storetype JKS
```

> **Recommendation:** Use **Play App Signing** (Google manages the signing key). You upload an "upload key" and Google re-signs with a private key they hold. This is free, more secure, and allows key upgrades.

### 2.6 Bump Version Code & Version Name

**File:** `app/build.gradle.kts`

```kotlin
defaultConfig {
    // ... existing ...
    versionCode = 2          // Required: must be higher than last published APK
    versionName = "1.0.0"    // First production release
}
```

> The current code has `versionCode = 1` and `versionName = "1.0"` but the main menu UI displays `v1.2.1` — this inconsistency should be resolved. Update the label in `layout_screen_main_menu.xml` to match `versionName` or read it programmatically:

```kotlin
// In MainActivity onCreate — set version dynamically
val versionInfo = packageManager.getPackageInfo(packageName, 0)
findViewById<TextView>(R.id.tvVersion).text = "v${versionInfo.versionName}"
```

Assign `android:id="@+id/tvVersion"` to the version TextView in the layout.

### 2.7 Handle the Dummy Button

**File:** `app/src/main/res/layout/layout_screen_main_menu.xml`  
**File:** `app/src/main/java/com/dicereligion/edgecase/MainActivity.kt`

**Three options:**

| Option | Effort | Recommendation |
|--------|--------|----------------|
| **A. Remove it** | Low | Easiest. Remove `btnDummy` from both layout and code. |
| **B. Repurpose as "About / Settings"** | Medium | Create a simple About screen with app info, privacy policy link, version. |
| **C. Repurpose as "Upgrade / Remove Ads"** | Medium | Link to an in-app purchase or premium ad-free version. |

**Recommended: Option B** — Create a simple About dialog or screen that shows:
- App name and version
- Link to Privacy Policy
- Link to Terms of Service (if applicable)
- Credits

### 2.8 Add `usesCleartextTraffic` Consideration

If any ad networks or content needs to load over HTTP (not HTTPS), add to manifest:
```xml
<application
    android:usesCleartextTraffic="false"  <!-- Secure default -->
    ...>
```

AdMob runs over HTTPS, so `false` is fine. If using test ad units during development, keep this `false` — test ads work over HTTPS.

---

## 3. Ad Integration Strategy (AdMob)

### 3.1 Why AdMob?

Google AdMob is the standard ad network for Android apps. It provides:
- Banner, interstitial, native, and rewarded ad formats
- Mediation support for other networks
- Integrated with Google Play Console
- Easy SDK integration via Maven/Google repository

### 3.2 Dependency Setup

**File:** `gradle/libs.versions.toml`

Add to `[versions]`:
```toml
playServicesAds = "23.6.0"
```

Add to `[libraries]`:
```toml
play-services-ads = { group = "com.google.android.gms", name = "play-services-ads", version.ref = "playServicesAds" }
```

**File:** `app/build.gradle.kts`

In the `dependencies` block, add:
```kotlin
implementation(libs.play.services.ads)
```

### 3.3 Initialize AdMob

**File:** `app/src/main/java/com/dicereligion/edgecase/MainActivity.kt`

Add in `onCreate()`, after `super.onCreate()`:

```kotlin
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener

// In onCreate():
MobileAds.initialize(this) { status: InitializationStatus ->
    // Optional: log initialization status for debugging
}
```

### 3.4 Ad Placement Strategy — Where to Put Ads

EdgeCase has 3 screens. Here is the recommended placement:

#### 📍 Ad Placement #1: Main Menu — Banner Ad (Highest Value)

**Where:** Between the spear divider and the Start/Stop service buttons, or below all buttons.  
**Why:** This is the screen users see most. A banner here has the highest impression potential.  
**Format:** Adaptive Banner (anchored at bottom) — adapts to screen width, better CPM.

**Layout Change:** `layout_screen_main_menu.xml`

Add after the spear divider (or at the very bottom of the center LinearLayout):

```xml
<!-- Ad banner container -->
<com.google.android.gms.ads.AdView
    xmlns:ads="http://schemas.android.com/apk/res-auto"
    android:id="@+id/adBannerMainMenu"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="12dp"
    android:layout_marginBottom="8dp"
    ads:adSize="BANNER"
    ads:adUnitId="@string/ad_banner_main_menu_unit_id" />
```

**Code in MainActivity:**
```kotlin
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdSize

// In onCreate(), after setting content view:
val adView = findViewById<AdView>(R.id.adBannerMainMenu)
val adRequest = AdRequest.Builder().build()
adView.loadAd(adRequest)
```

#### 📍 Ad Placement #2: Shortcuts Screen — Banner Ad (Optional)

**Where:** In the bottom action bar area, between Back and Save buttons.  
**Why:** Users spend time here configuring shortcuts. Secondary placement.  
**Format:** Small inline banner or 320×50.

**Layout Change:** `layout_screen_shortcuts_container.xml`

Add between the Archives RecyclerView and the action bar:

```xml
<com.google.android.gms.ads.AdView
    xmlns:ads="http://schemas.android.com/apk/res-auto"
    android:id="@+id/adBannerShortcuts"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    ads:adSize="BANNER"
    ads:adUnitId="@string/ad_banner_shortcuts_unit_id" />
```

#### 📍 Ad Placement #3: Interstitial Ad on Screen Transitions

**Where:** When navigating from Shortcuts back to Main Menu, or from Positioning back to Main Menu.  
**Why:** Interstitials have much higher CPM than banners. Show sparingly.  
**Rule:** Show at most once every 3-5 minutes (use a cooldown timer).

**Code Pattern in MainActivity:**
```kotlin
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : AppCompatActivity() {
    private var interstitialAd: InterstitialAd? = null
    private var lastInterstitialTime = 0L
    private val interstitialCooldownMs = 300_000L // 5 minutes

    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            getString(R.string.ad_interstitial_unit_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialIfReady() {
        val now = System.currentTimeMillis()
        if (interstitialAd != null && (now - lastInterstitialTime) > interstitialCooldownMs) {
            interstitialAd?.show(this)
            lastInterstitialTime = now
            interstitialAd = null
            loadInterstitial() // Preload next one
        } else {
            // No interstitial available or cooldown active — just navigate
        }
    }

    // Call showInterstitialIfReady() in showScreen() when leaving Shortcuts/Positioning
}
```

#### 📍 Ad Placement #4: Native Ad on Main Menu (Advanced, Optional)

A native ad could blend into the stone theme — styled as a "Carved Tablet" with a small "Ad" label. This is more complex but yields higher CPM and fits the aesthetic. Consider for a v2 release.

#### 📍 Where NOT to Put Ads

- **Do not** put ads in the overlay tray (SidebarService) — this violates Google Play policy (ads in overlay windows may be considered disruptive).
- **Do not** put ads on the Positioning screen alone — not enough dwell time.
- **Do not** show interstitials during the swipe-to-launch flow — disruptive and policy-violating.

### 3.5 Ad Unit ID String Resources

**File:** `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">EdgeCase</string>

    <!-- AdMob Ad Unit IDs — replace with production IDs before release -->
    <string name="ad_banner_main_menu_unit_id">ca-app-pub-3940256099942544/6300978111</string>
    <string name="ad_banner_shortcuts_unit_id">ca-app-pub-3940256099942544/6300978111</string>
    <string name="ad_interstitial_unit_id">ca-app-pub-3940256099942544/1033173712</string>
</resources>
```

> **Above IDs are test IDs** — they show test ads. Replace with your own production IDs from the AdMob dashboard before release.

### 3.6 Manifest Entry for AdMob

**File:** `app/src/main/AndroidManifest.xml`

Add inside `<application>`:

```xml
<!-- AdMob App ID -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713" />
```

> Replace `ca-app-pub-3940256099942544~3347511713` with your actual AdMob App ID.

### 3.7 AdMob Account Setup

1. Go to [admob.google.com](https://admob.google.com)
2. Create an account (needs a Google account)
3. Add EdgeCase as an app
4. Create ad units:
   - Banner (for main menu + shortcuts)
   - Interstitial (for screen transitions)
5. Link AdMob to your Play Console developer account
6. Add payment details (bank account, tax info)
7. Configure GDPR consent if targeting EU users (see Section 4.2.1)

---

## 4. Security & Release Signing

### 4.1 Keystore Management

**Critical path — do not skip or shortcut this.**

| Step | Description |
|------|-------------|
| 1 | Generate a release keystore (see 2.5) |
| 2 | **Back up the keystore file and all passwords** to a secure location (password manager, encrypted USB, HSM) |
| 3 | If you lose the keystore, you can **never update** the app on Play Store. That package name dies forever. |
| 4 | Use Play App Signing for the actual production key — Google holds the key. |
| 5 | The upload key is what you keep — if you lose it, Google can reset it (unlike the production key). |

### 4.2 GDPR & Privacy Compliance

#### 4.2.1 GDPR Consent for Ads (Required for EU Users)

If you serve personalized ads, you need GDPR consent from EU/UK users. Use Google's **User Messaging Platform (UMP) SDK**:

```kotlin
// Add dependency:
implementation("com.google.android.ump:user-messaging-platform:3.1.0")
```

The UMP SDK shows a consent dialog automatically before any ads load. If you skip this and target EU users, Google can suspend ad serving.

#### 4.2.2 Privacy Policy (Required)

The app must have a publicly accessible privacy policy URL. This is required because:
- The app accesses the list of installed apps (`QUERY_ALL_PACKAGES` via queries block)
- The app uses AdMob (collects advertising ID)
- The app runs a persistent foreground service

**Required content:**
- What data the app collects (installed app list, advertising ID for ads)
- How data is used (shortcut configuration, ad targeting)
- Third-party data sharing (AdMob, Google analytics if added)
- Data retention policy
- User rights (GDPR, CCPA)
- Contact information

**Hosting options:**
- Free: GitHub Pages, Firebase Hosting, Google Sites
- Include as a local asset fallback in the app's About section

---

## 5. Permissions & Policy Compliance

### 5.1 Current Permission Review

| Permission | Purpose | Play Policy Impact |
|------------|---------|--------------------|
| `SYSTEM_ALERT_WINDOW` | Draw overlay sliver and tray | Requires **Prominent Disclosure** in the app before requesting. Already handled in `checkAndRequestPermissions()`. |
| `FOREGROUND_SERVICE` | Run SidebarService persistently | Must declare foreground service type. Already declared: `specialUse`. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Qualify the service | Must provide justification in Play Console. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent Doze from killing service | Must justify in app and Play Console. |
| `VIBRATE` | Haptic feedback | No special policy requirements. |

### 5.2 Foreground Service Justification

**Play Console Declaration:** When you upload the app, you'll be asked: "Why does this app need a foreground service?"

**Write this justification:**
> "EdgeCase uses a foreground service to maintain a persistent edge-sliver overlay that provides users with quick, one-swipe access to their favorite apps. The overlay must be continuously available — it cannot function as a background task because it responds to real-time touch gestures on the screen edge. The service runs with `foregroundServiceType="specialUse"` and displays a low-priority notification to keep the user informed. The user starts and stops the service explicitly from the app's main menu."

### 5.3 SYSTEM_ALERT_WINDOW Prominent Disclosure

Your `checkAndRequestPermissions()` method already redirects the user to system Settings. However, Play Store policy requires a **prominent disclosure** *before* the system dialog appears — a custom dialog explaining why the permission is needed.

**Add before the Settings redirect in `checkAndRequestPermissions()`:**

```kotlin
private fun showOverlayPermissionDialog() {
    AlertDialog.Builder(this)
        .setTitle("Overlay Permission Required")
        .setMessage(
            "EdgeCase needs permission to display the edge-sliver overlay " +
            "on top of other apps. This is required for the quick-launch sidebar " +
            "to function. No data is collected through the overlay."
        )
        .setPositiveButton("Grant Permission") { _, _ ->
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        .setNegativeButton("Cancel", null)
        .show()
}

private fun checkAndRequestPermissions(): Boolean {
    if (!Settings.canDrawOverlays(this)) {
        showOverlayPermissionDialog()  // Show custom disclosure first
        return false
    }
    // ... rest unchanged
}
```

### 5.4 Data Safety Section (Play Console)

When filling the Data Safety form in Play Console, declare:

| Data Type | Collected? | Shared? | Purpose |
|-----------|-----------|---------|---------|
| **Installed Apps** | Yes | No | App functionality — to populate the shortcuts configuration list |
| **Advertising ID** | Yes (via AdMob) | Yes (AdMob) | Advertising or marketing |
| **App Interactions** | No | No | — |
| **Crash Logs** | No (unless Crashlytics added) | — | — |
| **Device or Other IDs** | No | No | — |

> Data is stored **only locally** via SharedPreferences. No data is transmitted off-device by EdgeCase itself.

### 5.5 Content Rating Questionnaire

Complete the content rating questionnaire in Play Console. EdgeCase should receive a **"Everyone"** or **"Teen"** rating:
- No violence
- No sexual content
- No profanity
- No user-generated content
- No location data
- Minimal data collection

---

## 6. Store Listing Requirements

### 6.1 Required Graphic Assets

| Asset | Dimensions | Format | Notes |
|-------|-----------|--------|-------|
| **App Icon** | 512×512 px | PNG (32-bit) | Already exists: `icon_round.png`. May want to refine. |
| **Feature Graphic** | 1024×500 px | PNG/JPG | Shown at top of store listing. **Must be created.** |
| **Phone Screenshots** | Min 320px, max 3840px | PNG/JPG | Minimum 2, recommend 4-8. **Must be created.** |
| **Short Description** | ≤ 80 characters | Text | Shown in search results. **Must be written.** |
| **Full Description** | ≤ 4000 characters | Text | Detailed feature list. **Must be written.** |

### 6.2 Screenshot Requirements (4-8 screenshots minimum)

Each screenshot should showcase one screen or feature:

| # | Screen | What to Show |
|---|--------|-------------|
| 1 | Main Menu | The themed Atrium with 5 stone buttons, pillars, and spear divider |
| 2 | Shortcuts — Altar | The drag-to-reorder shortcut list with silver-ring-framed icons |
| 3 | Shortcuts — Archives | The scrollable app list with serpent emerald checkbox toggles |
| 4 | Positioning | The phone mockup (Astrolabe) with draggable sliver and crosshatched zones |
| 5 | Sliver Overlay (Right) | Fang-shaped edge overlay on the right side of the screen |
| 6 | Tray Panel Expanded | The meander-bordered tray with desaturated shortcut icons |
| 7 | App Launch from Tray | Tapping a shortcut icon launching an app |
| 8 | Sliver Overlay (Left) | Optional: show left-side positioning support |

**Capture from an emulator or device running the app.** Use:
- Pixel 6/7 resolution (1080×2400) for a modern standard
- Clean wallpaper background when showing the overlay

### 6.3 Short Description (80 characters max)

> "Swipe-from-edge app launcher with a themed sidebar. Quick shortcuts, no clutter."

### 6.4 Full Description (suggested)

```
EdgeCase — Swipe-From-Edge App Launcher

EdgeCase puts your favorite apps one swipe away. A subtle, themed edge-sliver lives on the side of your screen. Swipe inward to reveal a beautiful tray of shortcuts. Launch any app instantly, then the tray collapses back — invisible until you need it again.

=== FEATURES ===

⚔️ EDGE SLIVER — A subtle fang-shaped handle on the left or right screen edge. Sits quietly, always ready. No on-screen button, no gestures to memorize.

📜 SHORTCUT TRAY — Swipe inward to reveal a scrollable panel of your chosen apps. Launch with a single tap. The tray unfurls like an ancient stone door and vanishes when you're done.

🎨 HELLENIC SERPENT THEME — A fully themed UI inspired by Greek temple architecture. Stone-textured backgrounds, Doric pillars, chiseled typography, serpent emerald accents, and desaturated iconography. Every screen is a visual experience.

📋 BIPARTITE SHORTCUT EDITOR — Two-pane configuration screen. Drag to reorder your shortcuts in "The Altar" (top section). Toggle apps on and off in "The Archives" (bottom section). All changes save instantly.

📍 FULLY POSITIONABLE — Drag the sliver up or down the screen edge, then snap to left or right side. The Astrolabe (phone mockup) previews your layout. Changes apply immediately to the overlay.

🔊 HAPTIC FEEDBACK — Stone button presses trigger a satisfying thud. Swiping the sliver produces an escalating vibration that snaps when the tray locks open. Every interaction feels physical.

💨 DUST PARTICLE EFFECTS — Press any stone button and a subtle burst of aged marble particles erupts from the center. Atmospheric without being distracting.

🛡️ PRIVACY-FIRST — All configuration data stays on your device in local storage. No accounts, no cloud sync, no analytics, no tracking (by EdgeCase itself). The app listing uses your device's installed apps, but this data never leaves your phone.

=== REQUIREMENTS ===

• Android 11 (API 30) or higher
• Overlay permission (prompted on first launch)
• Battery optimization exemption (recommended for service persistence)

=== PERMISSIONS EXPLAINED ===

• SYSTEM_ALERT_WINDOW — Required to display the edge-sliver overlay on top of other apps
• FOREGROUND_SERVICE — Required to keep the sliver running continuously in the background
• REQUEST_IGNORE_BATTERY_OPTIMIZATIONS — Prevents Android from killing the edge service during deep sleep
• VIBRATE — Provides tactile haptic feedback for button presses, swipes, and icon taps

EdgeCase is an open-source edge-launcher designed for enthusiasts who value aesthetics, minimalism, and instant access to their most-used apps.
```

### 6.5 Promo Video (Optional, Recommended)

A 30-second screen recording showing:
1. Open EdgeCase → Main Menu (3s)
2. Navigate to Shortcuts → select apps → Save (8s)
3. Navigate to Positioning → drag sliver (5s)
4. Start Service → sliver appears (3s)
5. Swipe sliver → tray unfurls → launch app → tray dismisses (8s)
6. Logo/name card (3s)

---

## 7. Testing & Pre-Launch Quality

### 7.1 Closed Testing Requirement (New Developer Accounts)

As of November 2023, Google requires **new Play Console developer accounts** to run a **closed test with 20 testers for 14 continuous days** before production access is granted.

**Action plan:**
1. Upload the app to the **Internal Testing** or **Closed Testing** track
2. Recruit 20 testers via Google Groups, Reddit, Discord, or friends/family
3. Testers must opt-in to the test track
4. Testers must use the app at least briefly over the 14-day period
5. After 14 days + 20 testers, the "Apply for production" button becomes available

### 7.2 Pre-Launch Report

Play Console runs automated tests on your app. It crawls your app on real devices and reports:
- Stability (crashes, ANRs)
- Performance (startup time, memory usage)
- Accessibility issues
- Security vulnerabilities

**Review the report and fix all critical issues before production launch.**

### 7.3 Manual Test Checklist

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| 1 | Fresh install → launch app | Main Menu displays with 5 buttons, pillars, stone theme |
| 2 | Tap SHORTCUTS | Bipartite screen shows with empty Altar + populated Archives |
| 3 | Check several apps in Archives | Apps appear immediately in Altar with drag handles |
| 4 | Drag to reorder in Altar | Items swap positions, lift animation plays |
| 5 | Uncheck an Altar item | Item dims to 0.5 alpha |
| 6 | Tap SAVE | Toast "Shortcuts saved", items remain, unselected removed |
| 7 | Tap BACK with unsaved changes | Discard dialog appears, Discard works, Keep Editing works |
| 8 | Tap POSITION | Phone mockup with sliver preview appears |
| 9 | Drag sliver on mockup | Particle trail follows, sliver stays in valid zone |
| 10 | Release near left edge | Sliver snaps left, info text updates |
| 11 | Release near right edge | Sliver snaps right, info text updates |
| 12 | Tap START EDGE SERVICE | Foreground notification appears, sliver shows on screen edge |
| 13 | Swipe sliver inward (>30px horizontally) | Sliver disappears, tray unfurls from edge with animation |
| 14 | Tap a shortcut icon in tray | App launches, tray collapses back to sliver |
| 15 | Tap outside tray | Tray collapses back to sliver |
| 16 | Go back to Main Menu, change shortcuts, Save | Running tray updates with new shortcuts |
| 17 | Change sliver position in app | Running sliver moves to new position immediately |
| 18 | Tap STOP SERVICE | Notification disappears, sliver/tray removed from screen |
| 19 | Grant overlay permission, then revoke in Settings | App handles gracefully (service stops if permission missing) |
| 20 | Test on different Android versions (API 30, 33, 35, 36) | Works consistently |
| 21 | Test with system gesture navigation (no 3-button) | Sliver works alongside gesture nav, no conflicts |
| 22 | Test on different screen sizes (compact, regular, large) | Mockup scales, overlay positions correctly |

### 7.4 Emulator Testing Configuration

**Recommended devices to test on:**
| Device | API | Screen | Purpose |
|--------|-----|--------|---------|
| Pixel 6a | 30 | 1080×2400 | Minimum API test |
| Pixel 7 | 33 | 1080×2400 | Mid-range API |
| Pixel 8 Pro | 35 | 1344×2992 | Current API, large screen |
| Pixel Tablet | 35 | 1600×2560 | Large screen test |
| Pixel Fold | 35 | Foldable | Foldable test (if ambitious) |

---

## 8. Monetization Strategy

### 8.1 Ad-Based Model (Primary)

| Ad Format | eCPM (Est.) | Daily Impressions (Est.) | Est. Daily Revenue (1000 DAU) |
|-----------|-------------|--------------------------|-------------------------------|
| **Banner (Main Menu)** | $0.50 – $1.50 | 2-3 per user | $1.00 – $4.50 |
| **Banner (Shortcuts)** | $0.50 – $1.50 | 0.5-1 per user | $0.25 – $1.50 |
| **Interstitial** | $3.00 – $8.00 | 0.2-0.5 per user | $0.60 – $4.00 |

> **Conservative estimate:** ~$0.005 – $0.01 per daily active user per day. At 10,000 DAU, ~$50-$100/day.

### 8.2 Premium / Ad-Free Version (Optional, v2)

If the app gains traction, offer an in-app purchase to remove ads:
- **Price:** $2.99 – $4.99 one-time
- **Removes:** All banners and interstitials
- **Implementation:** SharedPreferences boolean `"ads_disabled"`, checked before loading ads

### 8.3 Donation / Tip Jar (Optional, v2)

Add a "Support Development" option via Google Play Billing (consumable in-app purchases at $1, $3, $5 tiers). This is separate from ad removal and doesn't gate features.

---

## 9. Post-Launch Maintenance

### 9.1 Version Management

| Release | Type | Content |
|---------|------|---------|
| **v1.0.0** | Production | Initial release with ads + all current features |
| **v1.0.1** | Bug fix | Ad loading fixes, crash fixes from Play Console reports |
| **v1.1.0** | Feature | Dummy button → About screen, privacy policy link |
| **v1.2.0** | Feature | Premium ad-free IAP, improved landscape support |
| **v2.0.0** | Major | Foldable support, dynamic tray sizing, native ads, light theme |

### 9.2 Crash Reporting (Recommended)

Add Firebase Crashlytics to track production crashes:

```kotlin
// app/build.gradle.kts
implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
implementation("com.google.firebase:firebase-crashlytics-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")
```

This requires a `google-services.json` from Firebase Console (not committed to Git — already in `.gitignore`).

### 9.3 API Target Updates

Google requires apps to target the latest Android API within one year. Check each year:
- 2026 target: API 36 ✅ (already set)
- 2027 target: API 37 (expected)
- 2028 target: API 38 (expected)

---

## 10. Complete Actionable Checklist

### 🔴 Critical — Must Complete Before Submission

- [ ] **Enable R8:** Set `isMinifyEnabled = true` and `shrinkResources = true` in `app/build.gradle.kts`
- [ ] **Write ProGuard rules:** Copy the rules from Section 2.2 into `app/proguard-rules.pro`
- [ ] **Configure backup rules:** Update `backup_rules.xml` and `data_extraction_rules.xml` (Sections 2.3-2.4)
- [ ] **Generate release keystore:** See Section 4.1, follow instructions thoroughly
- [ ] **Configure signing:** Add `signingConfigs` block to `app/build.gradle.kts` (Section 2.5)
- [ ] **Create AdMob account:** [admob.google.com](https://admob.google.com)
- [ ] **Add AdMob dependency** to `gradle/libs.versions.toml` and `app/build.gradle.kts` (Section 3.2)
- [ ] **Add AdMob App ID meta-data** to `AndroidManifest.xml` (Section 3.6)
- [ ] **Implement banner ads** on Main Menu + Shortcuts screens (Section 3.4)
- [ ] **Implement interstitial ads** on screen transitions (Section 3.4)
- [ ] **Add ad unit IDs** to `strings.xml` (Section 3.5)
- [ ] **Initialize AdMob** in `MainActivity.onCreate()` (Section 3.3)
- [ ] **Add prominent disclosure dialog** for SYSTEM_ALERT_WINDOW (Section 5.3)
- [ ] **Write and host a Privacy Policy** (Section 4.2.2)
- [ ] **Bump versionCode** to 2 and **versionName** to "1.0.0" (Section 2.6)
- [ ] **Fix version label** to read dynamically or match versionName (Section 2.6)
- [ ] **Handle the Dummy button** — remove or implement (Section 2.7)

### 🟡 Important — Complete Before Submission

- [ ] **Resolve version inconsistency:** Current code shows `v1.2.1` in the UI label, but `versionName = "1.0"` and `versionCode = 1`. Align these. (Section 2.6)
- [ ] **Create feature graphic** (1024×500 PNG) (Section 6.1)
- [ ] **Capture 4-8 store screenshots** (Section 6.2)
- [ ] **Write short description** (≤80 characters) (Section 6.3)
- [ ] **Write full description** (≤4000 characters) (Section 6.4)
- [ ] **Complete content rating questionnaire** (Section 5.5)
- [ ] **Fill Data Safety section** in Play Console (Section 5.4)
- [ ] **Write foreground service justification** for Play Console (Section 5.2)
- [ ] **Run through manual test checklist** (Section 7.3)
- [ ] **Test on at least 3 emulated devices** covering low, mid, and current API levels
- [ ] **Test on a physical device** if at all possible

### 🟢 Nice-to-Have — Complete When Ready

- [ ] **Add UMP SDK** for GDPR consent (Section 4.2.1) — required if targeting EU/UK
- [ ] **Add Firebase Crashlytics** (Section 9.2)
- [ ] **Create promo video** (Section 6.5)
- [ ] **Set up Play App Signing** on first upload
- [ ] **Create Google Group** for closed testing track
- [ ] **Recruit 20 testers** for 14-day closed test (Section 7.1)
- [ ] **Set up Play Console internal test track**
- [ ] **Run Pre-Launch Report** and fix issues
- [ ] **Consider monetization alternatives** (premium ad-free, tips) (Section 8)
- [ ] **Prepare a support email/SM account** for user inquiries
- [ ] **Monitor Play Console vitals** (crashes, ANR rate) for first 2 weeks after release
- [ ] **Plan v1.1.0 features** — repurpose Dummy button (Section 9.1)

---

## Appendix A: Files That Need Modification

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Enable R8, add signing config, add AdMob dep, bump versionCode |
| `app/proguard-rules.pro` | Full rewrite with keep rules |
| `app/src/main/AndroidManifest.xml` | Add AdMob App ID meta-data |
| `app/src/main/res/values/strings.xml` | Add ad unit ID strings |
| `app/src/main/res/xml/backup_rules.xml` | Add SharedPreferences backup rule |
| `app/src/main/res/xml/data_extraction_rules.xml` | Add cloud-backup and device-transfer rules |
| `app/src/main/res/layout/layout_screen_main_menu.xml` | Add AdView for banner, add id to version text |
| `app/src/main/res/layout/layout_screen_shortcuts_container.xml` | Add AdView for banner (optional) |
| `app/src/main/java/com/dicereligion/edgecase/MainActivity.kt` | AdMob init, banner loading, interstitial logic, prominent disclosure dialog, dynamic version, Dummy button handling |
| `gradle/libs.versions.toml` | Add play-services-ads version and library declaration |

## Appendix B: New Files That Need Creation

| File | Purpose |
|------|---------|
| `edgecase-release.keystore` | Release signing keystore (outside Git, kept secure) |
| Privacy policy HTML/MD page | Hosted publicly, URL referenced in app and Play Console |
| Feature graphic PNG | Store listing hero image |
| 4-8 Screenshot PNGs | Store listing screenshots |
| (Optional) Firebase `google-services.json` | Crashlytics integration |

## Appendix C: Estimated Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Code changes (R8, ProGuard, ads, permissions, version) | 2-3 days | — |
| Keystore generation + signing config | 1 day | — |
| AdMob account setup | 1-2 days | Google account verification |
| Privacy policy writing + hosting | 1 day | — |
| Asset creation (screenshots, feature graphic) | 1-2 days | App functional on emulator |
| Store listing writing | 1 day | — |
| Internal testing + bug fixes | 3-5 days | App signed, uploaded |
| Closed testing (20 testers, 14 days) | 14 days (mandatory minimum) | Testers recruited |
| Play Console review | 1-7 days | Everything above complete |
| **TOTAL (optimistic)** | **~4-5 weeks** | — |

---

> **Final Note:** This document covers everything needed to publish EdgeCase on Google Play Store as of July 2026. Google's policies change periodically. Before final submission, verify that Play Console policy requirements have not changed, especially around foreground service declarations, ad policies, and data safety requirements.
