# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static final ** *;
}
-keepnames enum * { *; }

# KotlinX Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class * {
    static ** Companion;
    *** serializer(...);
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keep class * implements kotlinx.serialization.internal.GeneratedSerializer { *; }

# Domain models, sync, and backup
-keep class com.denser.june.core.domain.model.** { *; }
-keep class com.denser.june.core.domain.sync.** { *; }
-keep class com.denser.june.core.domain.backup.** { *; }

# Room
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
    @androidx.room.TypeConverter *;
}
-keep class com.denser.june.core.data.database.converters.** { *; }

# Third-party libraries
-keep class com.jayway.jsonpath.** { *; }
-dontwarn com.jayway.jsonpath.**
-keep class com.charleskorn.kaml.** { *; }
-dontwarn com.charleskorn.kaml.**
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
