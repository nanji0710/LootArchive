# Apache POI — 仅保留 XSSF（Excel .xlsx），剥离 Word/PPT/Visio/HSSF
-keep class org.apache.poi.ss.usermodel.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.openxml4j.** { *; }
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.poi.util.** { *; }
-keep class org.apache.poi.common.** { *; }
-dontwarn org.apache.poi.**

# XmlBeans — POI XSSF 的核心 XML 绑定框架，大量反射调用必须保留
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**

# OOXML Schema 类型（XSSF 运行时动态加载）
-keep class org.openxmlformats.schemas.spreadsheetml.** { *; }
-keep class org.openxmlformats.schemas.officeDocument.** { *; }
-keep class org.openxmlformats.schemas.drawingml.** { *; }
-dontwarn org.openxmlformats.**
-keep class org.openxmlformats.schemas.** { *; }

# XmlBeans 内置 schema 类型系统
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.apache.xmlbeans.metadata.** { *; }
-dontwarn schemaorg_apache_xmlbeans.**

# POI 用到的 Apache Commons 子集
-keep class org.apache.commons.compress.** { *; }
-keep class org.apache.commons.collections4.** { *; }
-keep class org.apache.commons.math3.util.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.collections4.**

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
