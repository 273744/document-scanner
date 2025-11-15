# Document Scanner App - Permissions Setup

## Overview
This document explains the permissions configuration for the Document Scanner Android app, including runtime permission handling for Android 6.0+ (API 23+).

## Permissions Added to AndroidManifest.xml

### 1. Camera Permission
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```
- **Purpose**: Allows the app to capture documents using the device camera
- **Required for**: "Capture Document" feature
- **Runtime permission**: Yes (Android 6.0+)

### 2. Storage Permissions

#### For Android 10-12 (API 29-32)
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
```
- **Purpose**: Read and write files to external storage
- **Runtime permission**: Yes

#### For Android 13+ (API 33+)
```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```
- **Purpose**: Granular media access for images
- **Runtime permission**: Yes
- **Note**: Android 13+ uses more specific permissions for better privacy

### 3. Internet Permission
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
- **Purpose**: Future cloud features (backup, sync, sharing)
- **Runtime permission**: No (granted at install time)

### 4. Network State Permission
```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
- **Purpose**: Check network connectivity before cloud operations
- **Runtime permission**: No (granted at install time)

## Runtime Permission Handling

### Implementation Details

The app uses **Accompanist Permissions** library for Jetpack Compose to handle runtime permissions:

```kotlin
implementation("com.google.accompanist:accompanist-permissions:0.37.0")
```

### How It Works

1. **Permission State Management**
   - The app dynamically requests the appropriate permissions based on Android version
   - Android 13+ requests `READ_MEDIA_IMAGES`
   - Android 10-12 requests `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`
   - All versions request `CAMERA` permission

2. **User Flow**
   - When user taps "Capture Document" or "View Gallery":
     - If permissions are granted → Proceed with action
     - If permissions are denied but rationale should be shown → Show explanation dialog
     - Otherwise → Request permissions

3. **Permission Rationale Dialog**
   - Explains why permissions are needed
   - Provides "Grant Permissions" and "Cancel" options
   - Follows Material Design guidelines

### Code Structure

#### MainActivity.kt
- Contains Compose UI with integrated permission handling
- Uses `rememberMultiplePermissionsState()` for permission state
- Shows rationale dialog when appropriate
- Handles button clicks with permission checks

#### PermissionHelper.kt
- Utility class for traditional Activity-based permission handling
- Provides methods for checking and requesting permissions
- Version-aware permission handling
- Can be used for non-Compose scenarios

## Testing Permissions

### Test Scenarios

1. **First Launch**
   - Tap "Capture Document" → Permission dialog appears
   - Grant permissions → Feature works
   
2. **Permission Denied Once**
   - Tap button → Rationale dialog appears
   - Grant from rationale → Permission dialog appears again
   
3. **Permission Permanently Denied**
   - If user denies twice with "Don't ask again"
   - App should guide user to app settings (future enhancement)

### Testing on Different Android Versions

- **Android 6-9 (API 23-28)**: Tests standard runtime permissions
- **Android 10-12 (API 29-32)**: Tests scoped storage with legacy permissions
- **Android 13+ (API 33+)**: Tests granular media permissions

## Best Practices Implemented

1. ✅ **Minimal Permissions**: Only request permissions when needed
2. ✅ **Version-Aware**: Different permissions for different Android versions
3. ✅ **User Education**: Show rationale before requesting permissions
4. ✅ **Graceful Handling**: App doesn't crash if permissions denied
5. ✅ **maxSdkVersion**: Old storage permissions don't apply to newer Android versions
6. ✅ **Optional Hardware**: Camera feature marked as not required (works on devices without camera)

## Future Enhancements

- [ ] Add "Open Settings" option when permissions are permanently denied
- [ ] Implement video recording permission for document scanning
- [ ] Add location permission for geotagging documents (optional feature)
- [ ] Implement permission status persistence

## References

- [Android Permissions Documentation](https://developer.android.com/guide/topics/permissions/overview)
- [Accompanist Permissions](https://google.github.io/accompanist/permissions/)
- [Request App Permissions](https://developer.android.com/training/permissions/requesting)

