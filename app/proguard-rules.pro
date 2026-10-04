# MoneyManager LK - Production ProGuard Rules

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }

# SQLCipher
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# PDFBox Android
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.bouncycastle.**

# OpenCSV & Commons
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**
-keep class org.apache.commons.** { *; }
-dontwarn org.apache.commons.**

# Apache POI / StAX / Aalto
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class com.fasterxml.aalto.** { *; }
-dontwarn com.fasterxml.aalto.**
-dontwarn javax.xml.stream.**

# Android Security & Biometrics
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
