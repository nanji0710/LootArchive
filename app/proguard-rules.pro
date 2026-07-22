# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.openxmlformats.** { *; }
-dontwarn org.openxmlformats.**

# POI transitive dependencies - not available on Android
-dontwarn aQute.bnd.**
-dontwarn edu.umd.cs.findbugs.**
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.osgi.framework.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn com.graphbuilder.**
-dontwarn com.microsoft.schemas.**
-dontwarn org.etsi.**
-dontwarn org.w3.**
-dontwarn schemasMicrosoftCom.**
-dontwarn org.apache.xml.security.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.jcp.**
-dontwarn org.apache.xmlgraphics.**
-dontwarn org.apache.avalon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.commons.logging.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coil
-keep class coil.** { *; }
