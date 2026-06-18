# kotlinx.serialization 用の保持ルール
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.example.frenchquiz.data.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
