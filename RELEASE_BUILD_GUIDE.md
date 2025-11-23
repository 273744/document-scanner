# How to Build for Google Play (16KB Compliant)

## Quick Commands

### 1. Build Release AAB (Recommended for Play Store)
```bash
cd C:\Users\273744\AndroidStudioProjects\MyApplication
.\gradlew.bat bundleRelease
```

**Output Location:**
`app\build\outputs\bundle\release\app-release.aab`

### 2. Build Release APK (For testing only)
```bash
.\gradlew.bat assembleRelease
```

**Output Location:**
`app\build\outputs\apk\release\app-release-unsigned.apk`

---

## Sign the Release Build

### Option 1: Using Android Studio
1. Build → Generate Signed Bundle/APK
2. Choose "Android App Bundle"
3. Select or create keystore
4. Build variant: "release"
5. Destination folder
6. Finish

### Option 2: Command Line (if you have keystore)
```bash
.\gradlew.bat bundleRelease

# Then sign using jarsigner or apksigner
```

---

## Verify 16KB Compliance

### Check Native Libraries
```powershell
# Extract AAB to check contents
Expand-Archive -Path "app\build\outputs\bundle\release\app-release.aab" -DestinationPath "temp_aab"

# List native libraries
Get-ChildItem -Path "temp_aab\lib" -Recurse

# Should ONLY see: lib\arm64-v8a\
# Should NOT see: lib\x86_64\
```

### Expected Output
```
lib\
  arm64-v8a\
    libarcore_sdk_c.so
    libarcore_sdk_jni.so  
    libc++_shared.so
    libopencv_java4.so
    [other .so files]
```

✅ **Only arm64-v8a = 16KB Compliant!**

---

## Upload to Google Play Console

1. Go to: https://play.google.com/console

2. Select your app (or create new)

3. Navigate to: **Production** → **Releases**

4. Click: **Create new release**

5. Upload: `app-release.aab`

6. Fill in release notes

7. Review and rollout

### Google Play Will Verify
- ✅ 16KB page size support
- ✅ Target SDK (35)
- ✅ Native library alignment
- ✅ Security scanning

---

## Testing Before Upload

### Internal Testing Track (Recommended)
1. Upload to **Internal Testing** first
2. Add test users
3. Test on real devices
4. Then promote to Production

### Pre-Launch Report
- Google Play runs automated tests
- Checks 16KB compatibility
- Tests on various devices
- Reviews crash reports

---

## Troubleshooting

### "APK contains x86_64 libraries"
**Cause:** Built debug instead of release
**Fix:** Use `bundleRelease` not `bundleDebug`

### "Native libraries not 16KB aligned"
**Cause:** Old build or incorrect configuration
**Fix:** 
1. Clean: `.\gradlew.bat clean`
2. Rebuild: `.\gradlew.bat bundleRelease`
3. Verify only arm64-v8a in output

### "Cannot find signing config"
**Fix:** Create keystore or use Android Studio signing wizard

---

## Build Variants Comparison

| Variant | Command | ABI | 16KB | Use |
|---------|---------|-----|------|-----|
| Debug | `assembleDebug` | arm64-v8a + x86_64 | ⚠️ Partial | Emulator testing |
| Release | `assembleRelease` | arm64-v8a only | ✅ Full | APK for testing |
| Release AAB | `bundleRelease` | arm64-v8a only | ✅ Full | **Google Play upload** |

---

## Checklist Before Upload

- [ ] Built release variant: `bundleRelease`
- [ ] Verified only arm64-v8a in AAB
- [ ] Signed with production keystore
- [ ] Tested on physical device (optional)
- [ ] Updated version code/name
- [ ] Prepared release notes
- [ ] Screenshots ready (if new app)

---

## Version Management

### Current Version
Check `app/build.gradle.kts`:
```kotlin
versionCode = 4
versionName = "1.3"
```

### For Next Release
Increment version code:
```kotlin
versionCode = 5  // Must be greater than previous
versionName = "1.4"
```

---

## Complete Release Workflow

```bash
# 1. Update version
# Edit app/build.gradle.kts → increment versionCode

# 2. Clean old builds
.\gradlew.bat clean

# 3. Build release AAB
.\gradlew.bat bundleRelease

# 4. Verify output
ls app\build\outputs\bundle\release\

# 5. Sign (if not auto-signed)
# Use Android Studio or jarsigner

# 6. Upload to Play Console
# Drag and drop app-release.aab

# 7. Submit for review
# Add release notes and submit
```

---

## Expected Build Times

- Clean build: ~2-3 minutes
- Incremental build: ~30-60 seconds
- AAB generation: ~10-20 seconds

---

## File Sizes (Approximate)

- Debug APK: ~75 MB (includes x86_64)
- Release APK: ~68 MB (arm64-v8a only)
- Release AAB: ~68 MB (optimized by Play Store)

**Google Play will generate optimized APKs:**
- Devices download only needed resources
- Typical download: 40-50 MB
- Depends on device screen density, language, etc.

---

## Important Notes

1. **Always use AAB for Play Store** (not APK)
2. **Keep your keystore safe** (cannot recover if lost)
3. **Test internal track first** before production
4. **Monitor pre-launch reports** for issues
5. **Check 16KB badge** in Play Console after upload

---

## Success Indicators

After upload to Play Console, check for:
- ✅ "16KB device support" badge
- ✅ No warnings about native libraries
- ✅ "Supported devices" shows modern Android phones
- ✅ Pre-launch report shows no crashes

---

**Ready to Ship! 🚀**

Your app is configured for 16KB compliance. Just build the release AAB and upload!

