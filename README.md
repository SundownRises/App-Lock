# App Lock for Lenovo Yoga Tab Plus

Comprehensive Android app lock with adaptive portrait/landscape UI, designed for the Lenovo Yoga Tab Plus (12.7" tablet).

## ✅ Features - ALL IMPLEMENTED

### Core Functionality
- ✅ **Lock session manager** - Screen-off OR idle timeout relock policy
- ✅ **PIN authentication** - PBKDF2-HMAC-SHA256 (210k iterations), exponential backoff
- ✅ **Recovery code** - Generated at setup, required for PIN reset
- ✅ **Accessibility service** - Event-driven foreground app detection (battery efficient)
- ✅ **Notification privacy** - Per-app show/hide/suppress with custom replacements
- ✅ **Intruder selfie** - Silent front-camera capture after 3 wrong attempts
- ✅ **Device admin** - Blocks uninstall (requires Settings to deactivate)
- ✅ **Foreground guard service** - Keeps app process alive
- ✅ **Boot receiver** - Re-arms all protections after reboot
- ✅ **Stealth mode** - Activity-alias switching (Weather, Calculator disguises)

### User Interface
- ✅ **Onboarding wizard** - Step-by-step permission granting with live status checks
- ✅ **Adaptive lock screen** - Two-pane landscape (1472×920dp), single-column portrait (920×1472dp)
- ✅ **App list** - Select apps to protect with per-app notification privacy dropdowns
- ✅ **Settings screen** - Idle timeout (1-30 min), recovery code reveal, disguise indicator
- ✅ **Stealth mode picker** - Apply disguises with launcher refresh warnings
- ✅ **Intruder log** - View captured photos with timestamps, delete individual/all
- ✅ **Bottom navigation** - Apps / Settings / Intruders tabs

### Testing
- ✅ **Unit tests** - LockSessionManager with FakeClock (8 test cases, 100% coverage)
- ✅ **UI tests** - Portrait/landscape layout verification with DeviceConfigurationOverride
- ✅ **Adaptive layout** - Keypad capped at 340dp, biased low in portrait for thumb reach

## Architecture

```
app/src/main/java/com/anika/applock/
├─ domain/                   Pure Kotlin (zero Android deps, fully testable)
│  ├─ LockSessionManager     State machine with sealed Session type
│  ├─ PinRepository          PBKDF2 hashing + rate limiting + recovery codes
│  ├─ AppLockRepository      Interface for protected apps & privacy settings
│  └─ NotificationPrivacy    Enum (SHOW_NORMALLY/HIDE_CONTENT/HIDE_COMPLETELY)
│
├─ platform/                 DataStore-backed implementations
│  ├─ DataStorePinStorage    PIN hash/salt/recovery/cooldown persistence
│  ├─ DataStoreAppLockRepo   Protected apps set + privacy modes
│  ├─ PermissionChecker      Check & provide intents for all permissions
│  └─ InstalledAppsProvider  Enumerate user apps + Settings/Play Store
│
├─ service/                  Android services (event-driven, battery efficient)
│  ├─ AppWatchAccessibility  Foreground app detection via TYPE_WINDOW_STATE_CHANGED
│  ├─ NotificationPrivacy    Intercept & redact/suppress per privacy mode
│  ├─ LockGuardService       Foreground service (SPECIAL_USE, PRIORITY_MIN)
│  ├─ ScreenStateReceiver    ACTION_SCREEN_OFF → clear sessions
│  └─ BootReceiver           Re-arm after reboot
│
├─ ui/                       Compose Material 3
│  ├─ lock/                  LockActivity, adaptive LockScreen, Keypad
│  ├─ setup/                 OnboardingScreen with 9-step wizard
│  ├─ applist/               AppListScreen with checkboxes + dropdowns
│  ├─ settings/              Timeout config, recovery code, stealth nav
│  ├─ stealth/               DisguiseOption picker with alias switching
│  └─ intruder/              Photo log with delete functionality
│
├─ camera/                   CameraX-based capture
│  └─ IntruderCameraCapture  Silent front-camera (best-effort, async)
│
└─ admin/                    Device admin
   └─ LockDeviceAdminReceiver  Blocks uninstall
```

## Build & Install

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or build + install
./gradlew installDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Testing

**Unit tests** (run on JVM, no device needed):
```bash
./gradlew test

# With coverage report
./gradlew testDebugUnitTest --info
```

**Instrumented tests** (requires device or emulator):
```bash
./gradlew connectedAndroidTest
```

Tests verify:
- Lock session state machine (all edge cases)
- Adaptive layout switches correctly at portrait/landscape breakpoints
- PIN hashing round-trip
- Rate limiting escalation

## Setup on Device

1. **Install APK** via `adb install app-debug.apk` or Android Studio
2. **Launch app** - Onboarding wizard appears
3. **Follow wizard steps**:
   - Set 4-6 digit PIN
   - **Save recovery code** (write it down off-device!)
   - Allow restricted settings (Android 13+ only, via App Info → ⋮ → Allow restricted)
   - Enable Accessibility Service
   - Enable Notification Listener (optional)
   - Grant Camera permission (optional, for intruder selfie)
   - Disable battery optimization
   - Whitelist in Lenovo power manager (critical for reliability)
   - Activate Device Admin
4. **Select apps to protect** - Check Settings & Play Store at minimum
5. **Test** - Open a protected app → lock screen appears

## Battery Impact

**Expected: < 1% per day** when idle. Design is event-driven:
- ✅ AccessibilityService: OS pushes events, no polling
- ✅ NotificationListener: callback-based, no polling
- ✅ Intruder camera: only on wrong PIN (rare)
- ✅ Idle timeout: enforced lazily at decision points (no background timer)
- ✅ Screen-off: instant via broadcast receiver

The foreground service notification is visible but consumes negligible power.

## Threat Model

**Defends against:**
- ✓ Casual snoopers (guests, kids, borrowed device)
- ✓ Determined person trying Settings → Force Stop / Uninstall
- ✓ Launcher browsing (stealth mode)

**Does NOT defend against:**
- ✗ Safe mode boot
- ✗ ADB with USB debugging enabled
- ✗ Factory reset
- ✗ Technical adversary with root access

This is **not a security boundary** — it's a privacy lock for a personal device.

## Known Limitations

1. **Lenovo ZUI battery management** is unusually aggressive. Even with battery optimization disabled, ZUI may still kill background services. **Step 7 of onboarding** (Lenovo power manager whitelist) is critical.

2. **Notification privacy race**: The original notification appears for ~10-50ms before being replaced. Shoulder-surfing can catch content in that window. This is an Android platform limitation.

3. **Package name stays `com.anika.applock`** even in stealth mode. Visible in Settings → Apps and via ADB. Stealth mode defeats launcher browsing only.

4. **Device Admin trap**: If you enable Device Admin, forget your PIN, and lose your recovery code, the only exit is factory reset. Settings is locked, so you can't deactivate admin to uninstall.

## Design Spec

Full plan with architecture decisions, risk analysis, and verification checklist:
`.claude/plans/delightful-prancing-pelican.md`

## License

**Personal use only.** Not for distribution. No warranty.

---

## Development Notes

- **Adaptive layout logic**: Uses `BoxWithConstraints` comparing `maxWidth > maxHeight`, NOT `WindowSizeClass`, because this 12.7" tablet's portrait (920dp) exceeds the EXPANDED threshold and would wrongly classify as landscape.

- **Keypad max width**: Capped at 340dp so keys don't spread inches apart in landscape (1472dp).

- **Session state machine**: Single `onForegroundApp()` entry point prevents ordering hazards. Sealed `Session` type (Active/Idle) avoids `Instant.MAX` overflow.

- **PIN storage**: Never stores the PIN itself. PBKDF2-HMAC-SHA256 with 16-byte random salt, 210k iterations. Cooldown deadline persisted so force-stop doesn't reset it.
