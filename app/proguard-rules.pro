# ===== 基础：保留注解、签名、反射元数据 =====
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions
-keepattributes SourceFile,LineNumberTable

# ===== Apache POI — 完整保留 XSSF 链路（反射密集，绝对不能混淆） =====
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# XmlBeans — POI XSSF 的核心 XML 绑定框架
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**

# OOXML Schema 类型（XSSF 运行时通过反射按需加载）
-keep class org.openxmlformats.schemas.** { *; }
-dontwarn org.openxmlformats.schemas.**

# XmlBeans 内置 schema 类型系统
-keep class schemaorg_apache_xmlbeans.** { *; }
-dontwarn schemaorg_apache_xmlbeans.**

# Apache Commons（POI 依赖）
-keep class org.apache.commons.compress.** { *; }
-keep class org.apache.commons.collections4.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.collections4.**

# POI / Commons 无关依赖（Android 上不可用）
-dontwarn aQute.bnd.**
-dontwarn edu.umd.cs.findbugs.**
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.osgi.framework.**
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
-dontwarn org.apache.commons.codec.**
-dontwarn org.apache.commons.math3.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coil
-keep class coil.** { *; }
