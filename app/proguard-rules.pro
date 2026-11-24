# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Reglas críticas para Moshi y Metadatos de Nulabilidad en Kotlin ---
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# Preservar anotaciones esenciales de Moshi
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
}

# Evitar la ofuscación de los adaptadores generados por Moshi
-keep class *JsonAdapter { public <init>(...); }
-keep class *JsonAdapter { *; }

# Mantener intactos nuestros modelos de datos reales de Supabase
-keep class com.example.data.network.SupabasePost { *; }
-keep class com.example.data.network.SupabaseComment { *; }
-keep class com.example.data.network.SupabaseUser { *; }

