# ══════════════════════════════════════════════════════════════════════════════
# EdgeCase — R8 keep rules
#
# Deliberately small. AAPT2 generates keep rules automatically for everything
# named in AndroidManifest.xml (MainActivity, SidebarService) and for every
# custom View referenced by a layout, including its (Context, AttributeSet)
# constructor. Restating those by hand adds nothing and hides real intent.
#
# This file therefore covers only the four things R8 cannot see for itself.
#
# Deviations from Docs/Publisher.md §2.2, which was written before the ad SDK
# landed and is over-broad:
#   · No blanket -keep on androidx.appcompat / androidx.recyclerview. Those
#     ship consumer rules and a wildcard keep defeats most of the shrink.
#   · No -keep public class * extends android.view.View, which would retain
#     every view in every dependency.
#   · No android.support.v7 rules — that namespace does not exist in this app.
#   · No AdMob rules. ads-mobile-sdk 1.4.0 and user-messaging-platform 4.0.0
#     both bundle a consumer proguard.txt (verified inside the AARs), and the
#     package in §2.2 (com.google.android.gms.ads) is the LEGACY SDK anyway.
# ══════════════════════════════════════════════════════════════════════════════

# ── 1. Enum constants read back out of SharedPreferences ──────────────────────
# SliverConfig persists ColorMode by name and restores it with
# ColorMode.valueOf(), so the constant names are load-bearing across an upgrade:
# an obfuscated name would throw on prefs written by an earlier build.
# ArcSliverView.Side is kept for the same reason should it ever be persisted.
-keepclassmembers enum com.dicereligion.edgecase.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── 2. Room databases instantiated by reflection ──────────────────────────────
# The ads SDK pulls in androidx.work 2.7.0, which pulls in Room 2.2.5, whose own
# consumer rule is:
#
#     -keep class * extends androidx.room.RoomDatabase
#
# That keeps the class but NOT its members, and Room creates the generated
# WorkDatabase_Impl by reflection. R8 duly removed the no-arg constructor, and the
# release build died on launch:
#
#     Unable to get provider androidx.startup.InitializationProvider
#     Caused by: Failed to create an instance of androidx.work.impl.WorkDatabase
#
# Room fixed its own rule in later versions by adding `{ <init>(); }`; 2.2.5 predates
# that and arrives transitively, so the version cannot simply be raised here. This
# rule is the fix. It costs one constructor.
#
# NOTE: this is the exact failure mode that a successful `assembleRelease` does NOT
# catch. Any dependency bump touching work/room means launching a release build again,
# not just compiling one.
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}

# ── 3. Readable crash reports ─────────────────────────────────────────────────
# Keep line numbers so Play Console stack traces are usable, but rename the
# source file so the class names stay obfuscated. Retrace with the mapping.txt
# that Play App Signing stores per release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── 4. Silence warnings from optional SDK back-references ─────────────────────
# The ads SDK compiles against classes it does not require at runtime.
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
