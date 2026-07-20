# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.openxmlformats.** { *; }
-dontwarn org.openxmlformats.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coil
-keep class coil.** { *; }
