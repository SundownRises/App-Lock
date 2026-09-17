# App Lock for Lenovo Yoga Tab Plus

Android app lock with adaptive portrait/landscape UI, designed specifically for the Lenovo Yoga Tab Plus tablet.

## Features

✅ **Implemented:**
- Lock session manager with screen-off + idle timeout relock policy
- PIN authentication with PBKDF2 hashing and exponential backoff rate limiting
- Accessibility service for event-driven foreground app detection (battery efficient)
- Notification privacy service (per-app show/hide/suppress)
- Adaptive lock screen UI (two-pane landscape, single-column portrait)
- Intruder selfie capture (silent, best-effort)
- Device admin receiver (blocks uninstall)
- Foreground guard service (keeps app alive)
- Boot receiver (re-arms after reboot)
- Stealth mode via activity-alias (Weather, Calculator disguises)

🚧 **TODO:**
- MainActivity full UI (onboarding wizard, app list, settings)
- Intruder log screen
- Icon assets for stealth mode disguises
- Biometric prompt integration in LockActivity
- Compose UI tests for adaptive layout
- On-device testing and refinement

## Architecture

```
domain/          Pure Kotlin logic (zero Android deps, fully testable)
├─ LockSessionManager    State machine for lock/unlock sessions
├─ PinRepository         PIN hashing, verification, rate limiting
├─ AppLockRepository     Protected apps + notification privacy
└─ NotificationPrivacy   Enum (show/hide/suppress)

platform/        DataStore-backed storage implementations
service/         Android services (Accessibility, Notification, Guard)
ui/              Compose UI (lock screen, main, onboarding)
camera/          Intruder selfie capture (CameraX)
admin/           Device admin receiver
```

## Build

```bash
./gradlew assembleDebug
```

## Testing

Unit tests (no device needed):
```bash
./gradlew test
```

Compose UI tests (requires device/emulator):
```bash
./gradlew connectedAndroidTest
```

## Plan

See `.claude/plans/delightful-prancing-pelican.md` for the full design spec.

## License

Personal use only. Not for distribution.
