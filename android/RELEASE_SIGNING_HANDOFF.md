# AlumniConnect — Release Signing Handoff Guide

This document contains instructions for manually performing the final production release signing for AlumniConnect.

---

## Pre-Signing Summary

| Parameter | Configuration Value |
| :--- | :--- |
| **Unsigned Production APK** | `android/app/build/outputs/apk/release/app-release-unsigned.apk` |
| **Production API Base URL** | `https://alumniconnect-bwoi.onrender.com/` |
| **Application ID** | `com.alumniconnect.app` |
| **Version Code** | `1` |
| **Version Name** | `1.0` |
| **Target SDK / Compile SDK** | `34` (Android 14) |
| **Min SDK** | `24` (Android 7.0) |

---

## Remaining Release Packaging Workflow

All compilation, configuration verification, dependency assembly, and production URL integration have been completed automatically. The ONLY remaining task is release signing with your private production keystore.

### Option A: Using Android Studio GUI

1. Open the `android/` directory in **Android Studio**.
2. Navigate to menu: **Build** → **Generate Signed Bundle / APK...**
3. Select **APK** and click **Next**.
4. In the Keystore dialog:
   - Choose your existing private release keystore, or click **Create new...** to create one.
   - Enter your keystore password, key alias, and key password.
5. Select build variant: **release**.
6. Check signature schemes: **V1 (JAR Signature)** and **V2 (Full APK Signature)** if prompted.
7. Click **Finish**.
8. Collect the generated signed APK from:
   `android/app/release/app-release.apk`
9. Rename to the recommended final production filename:
   ```text
   AlumniConnect-v1.0.apk
   ```

---

### Option B: Using Android SDK CLI (`apksigner`)

If signing via command-line, run `zipalign` followed by `apksigner`:

```bash
# 1. Align the unsigned release APK (if not already 4-byte aligned)
zipalign -v -p 4 app-release-unsigned.apk AlumniConnect-v1.0-unaligned.apk

# 2. Sign with your release keystore
apksigner sign --ks /path/to/your/release.keystore \
  --ks-key-alias your-key-alias \
  --out AlumniConnect-v1.0.apk \
  AlumniConnect-v1.0-unaligned.apk

# 3. Verify signature
apksigner verify --verbose AlumniConnect-v1.0.apk
```

---

## Critical Security Guidelines

> [!CAUTION]
> **DO NOT COMMIT PRIVATE CREDENTIALS OR KEYS TO GIT!**
>
> Ensure the following are **NEVER** committed, staged, or pushed to any repository:
> - `*.jks`
> - `*.keystore`
> - `keystore.properties`
> - Signing passwords or key alias passphrases
> - Binary release APKs
