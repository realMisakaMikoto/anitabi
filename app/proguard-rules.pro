# Navigation SDK 7.8.0 requires these optimizations to remain disabled.
-optimizations !class/merging/horizontal
-optimizations !class/merging/vertical

# The SDK registry reflectively creates package-private implementations. Keep
# the reflector in its original package so R8 cannot break package access.
-keep class com.google.android.libraries.navigation.internal.als.ax { *; }
