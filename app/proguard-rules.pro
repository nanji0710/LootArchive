# Apache POI — 仅保留 XSSF（Excel .xlsx），剥离 Word/PPT/Visio/HSSF
-keep class org.apache.poi.ss.usermodel.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.openxml4j.** { *; }
-dontwarn org.apache.poi.**
-keep class org.openxmlformats.schemas.spreadsheetml.** { *; }
-keep class org.openxmlformats.schemas.officeDocument.** { *; }
-keep class org.openxmlformats.schemas.drawingml.** { *; }
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
