# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.wardrobescan.app.data.model.** { *; }

# MediaPipe
-keep class com.google.mediapipe.** { *; }

# Retrofit
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
