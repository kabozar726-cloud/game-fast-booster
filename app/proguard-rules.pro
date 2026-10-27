# Add project specific ProGuard rules here.
# Keep WireGuard classes (the tunnel backend uses reflection in places).
-keep class com.wireguard.** { *; }
