Android Security Samples Repository
===================================

This repository contains a set of individual Android Studio projects to help you get
started writing/understanding Android security features.

## [Android BiometricLogin Sample](https://github.com/android/security-samples/tree/master/BiometricLoginKotlin)

This is the canonical sample that demonstrates how to integrate Android Biometrics into your apps.
Whether your project is for government, healthcare, financial, enterprise, or general usage, this
sample should guide your implementation.

## [Android DirectBoot Sample](https://github.com/android/security-samples/tree/master/DirectBoot)

This sample demonstrates how to store and access data in a device protected
storage that is always available while the device is booted.

## ProGurad

```pro
# Protobuf
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
  <fields>;
}
```
