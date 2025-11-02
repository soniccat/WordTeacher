# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.osgi.framework.BundleActivator
-dontwarn org.slf4j.impl.StaticLoggerBinder

-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.Filter
-dontwarn org.osgi.framework.FrameworkUtil
-dontwarn org.osgi.framework.InvalidSyntaxException
-dontwarn org.osgi.util.tracker.ServiceTracker
-dontwarn org.osgi.util.tracker.ServiceTrackerCustomizer

-keep class opennlp.** { *; }

-keep class com.yandex.authsdk.** { *; }

