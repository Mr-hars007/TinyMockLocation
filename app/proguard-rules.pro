# Keep entry points
-keep public class com.mrhars007.mocklocation.MainActivity
-keep public class com.mrhars007.mocklocation.MockLocationService

# Aggressive size optimization
-repackageclasses ''
-allowaccessmodification
-dontusemixedcaseclassnames
-renamesourcefileattribute ""
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable,!MethodParameters

# Strip debug logging
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
