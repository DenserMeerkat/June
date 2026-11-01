# Preserve line numbers for Google Play stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose runtime flags optimization
-assumevalues public class androidx.compose.runtime.ComposeRuntimeFlags {
    static boolean isLinkBufferComposerEnabled return true;
}

# Navigation 3
-keepnames interface com.denser.june.presentation.navigation.Route
-keepnames class * implements com.denser.june.presentation.navigation.Route { *; }
-keepnames interface androidx.navigation3.runtime.NavKey
-keepnames class * implements androidx.navigation3.runtime.NavKey { *; }

# Google API Client, Google Drive & Play Services Auth
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.internal.auth.** { *; }
-dontwarn org.apache.http.**

# RevenueCat
-keep class com.revenuecat.purchases.** { *; }
-dontwarn com.revenuecat.purchases.**

# AboutLibraries
-keep class com.mikepenz.aboutlibraries.** { *; }
-keepclassmembers class **.R$string {
    public static final int define_int_*;
    public static final int define_license_*;
    public static final int library_*;
}
-keepclassmembers class **.R$raw {
    public static final int aboutlibraries;
}

