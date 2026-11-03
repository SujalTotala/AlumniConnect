# ProGuard rules for AlumniConnect

# Preserve generic signatures and annotations required for Gson and Retrofit reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Gson rules
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit & OkHttp rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Application models, network interfaces, and repositories
-keep class com.alumniconnect.app.models.** { *; }
-keep interface com.alumniconnect.app.network.** { *; }
-keep class com.alumniconnect.app.network.** { *; }
