# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.knowme.app.** {
    *** Companion;
}

# Tink（security-crypto 依赖）引用的 errorprone 注解，运行期用不到
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# slf4j（Ktor/OkHttp 传递依赖）可选绑定
-dontwarn org.slf4j.**

# Ktor 客户端
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
