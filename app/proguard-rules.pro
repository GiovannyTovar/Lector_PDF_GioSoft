# Reglas de R8 para el build de release (minifyEnabled true).

# --- Gson ---
# Los modelos se serializan por reflexión: sus nombres de campo SON el formato
# de datos guardado en SharedPreferences. Si R8 los renombra, se pierde el
# historial de los usuarios que actualicen.
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class com.giosoft.lectorpdf.model.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# --- Código nativo (JNI) ---
# MuPDF y PDFium resuelven clases y métodos por nombre desde C.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.artifex.mupdf.** { *; }
-keep class com.shockwave.** { *; }

# --- Silenciar avisos de dependencias opcionales no usadas ---
-dontwarn com.artifex.mupdf.**
-dontwarn com.shockwave.**
