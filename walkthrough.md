# Security Summary

**Reviewed Files**: 23 (Android manifest, backup rules, core security/storage/parsing helpers, Firebase AI & Firestore repositories, Compose dialogs, Firestore security rules, and localization resources)
**Issues Resolved**: 11

### Issues Resolved
| Area | File | Line | What Was Happening | Impact | Status | How It Was Fixed |
|---|---|---|---|---|---|---|
| Screen Capture & Spying Protection | `app/src/main/java/com/example/MainActivity.kt` & `app/src/main/java/com/example/ui/components/SecurityAndAvatarComponents.kt` | 106, 84 | Sensitive screens, student credentials, private messages, and dialogs had no screen-capture flag, allowing screenshots, screen recorders, and Recents thumbnails to capture private data. | High | Fixed | Enabled `WindowManager.LayoutParams.FLAG_SECURE` on `MainActivity` and added `SecureDialogWindowEffect()` via `DialogWindowProvider` on sensitive Compose dialogs (PIN setup, QCM players/stats, student answer details, AI class analysis, password reset/change, roster import/QR dialogs). |
| Device Backup & Transfer Exposure | `app/src/main/AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml` | 35 | Cloud backup (`allowBackup`) was not disabled and sensitive preference files / offline exports lacked explicit exclusion rules. | High | Fixed | Set `android:allowBackup="false"` and explicitly excluded PIN lock, biometric credential, auth draft, device lock preferences, `offline_svt`, and `exports` from both `<full-backup-content>` and `<data-extraction-rules>`. |
| PIN Storage & Lockout Durability | `app/src/main/java/com/example/core/SecurityLockManager.kt` | 39–145 | Secondary PIN verification used a single-pass SHA-256 hash and asynchronous `.apply()` writes that could lose failed-attempt lockout state on rapid restarts. | High | Fixed | Upgraded PIN hashing to salted `PBKDF2WithHmacSHA256` (`100,000` iterations, 256-bit key, `v2$` prefix) with transparent legacy upgrade on verify and synchronous `.commit()` persistence across `setPin`, `verifyPin`, and `disablePin`. |
| Unsafe Download & Redirect URLs | `app/src/main/java/com/example/data/offline/OfflineStorageManager.kt` & `app/src/main/java/com/example/core/AppUpdateUrlHelper.kt` | 231, 454, 595; 56, 78 | Resource downloads and update links did not block unencrypted `http://`, embedded URL credentials, redirect-based protocol downgrades, or private/loopback network hosts, and HTTP downloads lacked a hard size cap. | High | Fixed | Enforced `https://`-only remote URLs with `instanceFollowRedirects = false`, hop-by-hop `isSafeRemoteHttpsUrl` / `isSafeHttpsUpdateUrl` host validation (blocking `localhost`, `.local`, `.internal`, loopback, link-local, and private IP ranges), a 5-redirect cap, and a 100 MB download ceiling. |
| Local File URI & Path Confinement | `app/src/main/java/com/example/data/offline/OfflineStorageManager.kt` | 195, 241, 587, 692, 773 | Local `file://` URIs and persisted index paths were not strictly verified against an expected app-owned directory boundary before opening or indexing. | High | Fixed | Added canonical `isFileInsideDir(...)` boundary verification confining `file://` downloads strictly to `appContext.cacheDir` and cached resources strictly to `resourcesFilesDir`, plus strict regex validation (`^[a-zA-Z0-9_-]{1,128}$`) in `parseChunkResourceId`. |
| Archive Decompression Limits | `app/src/main/java/com/example/core/DocxTextExtractor.kt` & `app/src/main/java/com/example/core/ExcelRosterParser.kt` | 19, 33, 45; 33, 49, 104 | Word (`.docx`) and Excel (`.xlsx`) parsers read ZIP entries without strict byte/entry caps, exposing the app to out-of-memory crashes from crafted compressed files. | Medium | Fixed | Added `readBoundedBytes` and `MAX_ZIP_ENTRIES = 250` caps limiting raw archive uploads to 15 MB and extracted XML entries to 8 MB before parsing. |
| Firestore Validation Bypass & URL Schemes | `firestore.rules` | 167, 183, 212 | Quiz attempts did not enforce `score <= maxScore`, and course resource/announcement URL rules did not restrict remote URLs to `https://` or validated chunk URIs. | High | Fixed | Updated `isValidQuizAttempt` to enforce `data.score <= data.maxScore`, `isValidResource` to require `https://` or valid `firestore://resource_chunks/<id>`, and `isValidAnnouncement` to require `https://` for non-empty attachments; deployed rules to Firebase. |
| Sensitive Clipboard Previews | `app/src/main/java/com/example/core/QrCodeUtils.kt` | 91 | Copied passwords, class codes, or Massar identifiers appeared in plaintext Android clipboard popups and keyboard history. | Medium | Fixed | Marked clipboard entries copied via `copyTextToClipboard` with `ClipDescription.EXTRA_IS_SENSITIVE = true`. |
| Biometric Keystore Sync & Invalidation | `app/src/main/java/com/example/core/BiometricAuthHelper.kt` & `app/src/main/java/com/example/MainActivity.kt` | 156, 297; 356 | Initial student password change left biometric credentials out of sync, and Android Keystore invalidation after biometric re-enrollment could leave stale ciphertext trapped. | Medium | Fixed | Wired `BiometricAuthHelper.updatePasswordIfSaved` into onboarding password setup and added automatic cleanup of stale credentials/keystore alias on `KeyPermanentlyInvalidatedException`. |
| Stale Exported Credential PDFs & Sign-Out Residue | `app/src/main/java/com/example/core/StudentCredentialsPdfExporter.kt` & `app/src/main/java/com/example/ui/screens/AuthAndOnboardingScreens.kt` | 159, 174 | Generated credential PDFs accumulated across exports, and temporary picked upload caches/exports persisted on disk after user sign-out. | Medium | Fixed | Purged prior PDFs in `files/exports` on each new export (`try/finally { document.close() }`) and wiped `files/exports` and `cache/picked_uploads` upon sign-out. |
| Log Redaction & AI App Check Recovery | `app/src/main/java/com/example/core/FirestoreErrorHandler.kt` & `app/src/main/java/com/example/data/ai/SvtAiRepository.kt` | 20, 65; 488, 529 | Error diagnostics could emit user emails/UIDs in release logs, and Firebase AI Logic calls on physical devices failed when App Check was unprovisioned or returned a cold/invalid token. | Medium | Fixed | Masked emails and truncated UIDs (suppressing `authInfo` in release logs), provisioned Firebase `ai_logic` + App Check for `com.aistudio.svtclasse.kmpvzx`, and added token readiness preflight + automatic force-refresh retry in `SvtAiRepository`. |

### Safeguards Verified
| Area | File | How It's Protected |
|---|---|---|
| Firebase Authentication & Device Binding | `firestore.rules` & `app/src/main/java/com/example/core/DeviceSessionManager.kt` | All Firestore operations require verified Firebase Authentication (`request.auth != null`); student accounts bind to one device identifier, and cross-user profile reads are blocked by strict owner/admin rules. |
| Official Student Roster Integrity | `firestore.rules` & `app/src/main/java/com/example/core/TeacherAnalyticsHelper.kt` | Only the owning teacher can manage `class_roster`, and teacher analytics resolve display names exclusively from the official roster entry even if a student edits their self-declared profile name. |
| Biometric Hardware Protection | `app/src/main/java/com/example/core/BiometricAuthHelper.kt` | Credentials saved for fingerprint sign-in are encrypted with a hardware-backed 256-bit AES-GCM key in `AndroidKeyStore` and decrypted only after system `BiometricPrompt` confirmation. |
| Legal Transparency & User Consent | `app/src/main/res/values/strings.xml`, `values-en/strings.xml`, `values-ar/strings.xml` | Legal Notice, Privacy Policy, and Terms of Use (`2026 (v3)`) are localized across FR/EN/AR, document AI analysis, biometric login, official roster display, prioritized notifications, and anti-spying protections, and require acceptance before dashboard entry. |

---

## PoC Verification

### 1. Screen Capture & Spying Prevention (`FLAG_SECURE`)
#### Summary
| Field | Value |
|---|---|
| Issue Type | Screen Capture & Recents Preview Exposure |
| Impact | High |
| Affected File | `app/src/main/java/com/example/MainActivity.kt:106` & `app/src/main/java/com/example/ui/components/SecurityAndAvatarComponents.kt:84` |
| Test Scenario | Triggering a screenshot, background screen recorder, or switching to Android Recents while viewing `MainActivity` or opening `ClassAiAnalysisDialog` / `StudentQuizAnswersDialog` / `ClassQrDialog`. |

#### How It Was Fixed
`MainActivity` sets `WindowManager.LayoutParams.FLAG_SECURE` before composing UI, and sensitive Compose dialogs attach `SecureDialogWindowEffect()` to apply `FLAG_SECURE` to each dialog's own `DialogWindowProvider` window.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | `MainActivity.onCreate()` executes `window.setFlags(FLAG_SECURE, FLAG_SECURE)` at line 106. | Main activity surface marked secure |
| 2 | A modal dialog (e.g., `ClassAiAnalysisDialog`) opens in its own Android `Window` and composes `SecureDialogWindowEffect()` (`SecurityAndAvatarComponents.kt:84`). | Walks `view.parent` to `DialogWindowProvider` |
| 3 | `parent.window.setFlags(FLAG_SECURE, FLAG_SECURE)` runs in `SideEffect` at line 90. | **Handled Safely** — Android blocks screenshots, screen-recording apps, and Recents thumbnails across both activity and dialogs |

---

### 2. Arbitrary Local File & Unsafe Network URL Blocking
#### Summary
| Field | Value |
|---|---|
| Issue Type | Unrestricted Local File Read & Unsafe Redirect / Private Host Access |
| Impact | High |
| Affected File | `app/src/main/java/com/example/data/offline/OfflineStorageManager.kt:241-250, 454-485, 587-612` |
| Test Scenario | Calling `downloadResource` with `file:///data/data/com.aistudio.svtclasse.kmpvzx/shared_prefs/bioterre_secondary_security.xml`, `http://example.com/file.pdf`, `https://169.254.169.254/latest/meta-data/`, or `firestore://resource_chunks/../../escape`. |

#### How It Was Fixed
`OfflineStorageManager` restricts `file://` URIs via canonical directory confinement (`isFileInsideDir` against `appContext.cacheDir`), rejects non-HTTPS network URLs, validates every HTTP redirect hop manually against private/loopback/link-local hosts (`isSafeRemoteHttpsUrl`), and checks chunk IDs against `^[a-zA-Z0-9_-]{1,128}$`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Malicious `file:///data/.../shared_prefs/...` reaches `downloadResource()` (`OfflineStorageManager.kt:241`). | `uri.scheme == "file"` branch entered |
| 2 | `isFileInsideDir(candidateFile, appContext.cacheDir)` resolves canonical paths (`lines 587–593`) and finds it outside `cacheDir + '/'`. | Throws `SecurityException` at line 245 |
| 3 | Malicious `https://169.254.169.254/...` or `http://...` reaches `downloadHttpFile()` (`line 460`) and `isSafeRemoteHttpsUrl()` (`lines 595–612`). | Blocks non-HTTPS & metadata/private IP, throws `SecurityException` |
| 4 | Verified by automated Robolectric & JVM tests in `OfflineStorageManagerTest` and `OfflineStorageManagerHelperTest`. | **Handled Safely** |

---

### 3. Strong PIN Hashing & Synchronous Lockout Persistence
#### Summary
| Field | Value |
|---|---|
| Issue Type | Weak PIN Key Derivation & Asynchronous Lockout Persistence |
| Impact | High |
| Affected File | `app/src/main/java/com/example/core/SecurityLockManager.kt:39-145` |
| Test Scenario | Setting a 4–6 digit PIN (`1234`), verifying a legacy SHA-256 PIN, and submitting 5 consecutive invalid attempts followed by an immediate valid PIN attempt. |

#### How It Was Fixed
PINs are derived with `PBKDF2WithHmacSHA256` (`100_000` iterations, 256-bit output, 16-byte random salt, `v2$` prefix), legacy hashes automatically upgrade on first successful login, and failed-attempt counters/lockout timestamps commit synchronously via `.commit()`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | `setPin()` (`SecurityLockManager.kt:39`) generates 16 random bytes and calls `hashPinV2(pin, saltB64)` (`lines 129–139`). | Stores `v2$...` PBKDF2 hash via `.commit()` |
| 2 | Five invalid attempts reach `verifyPin()` (`line 89`), incrementing failures to `MAX_FAILED_ATTEMPTS` (5). | Commits `pin_lockout_until_<uid>` via `.commit()` (`line 95`) |
| 3 | Immediate valid attempt at `line 65` detects `lockoutUntil > nowMillis`. | Returns `PinVerifyResult.LockedOut` — **Handled Safely** |
| 4 | Verified by `SecurityLockManagerTest` (`setPin_storesPbkdf2V2Hash_andLegacySha256UpgradesOnVerify`, `fiveWrongAttempts_triggers30SecondLockoutAndBlocksValidPinUntilExpiry`). | **Handled Safely** |

---

### 4. Automated Build & Test Verification
- `compile_applet`: **Succeeded** cleanly across the full Android project (`app` module, updated resources in FR/EN/AR, and `google-services.json` / `firebase_applet_config.xml`).
- `gradle :app:testDebugUnitTest --tests "com.example.core.*" --tests "com.example.data.offline.*" --tests "com.example.ExampleUnitTest" --tests "com.example.ExampleRobolectricTest" --tests "com.example.MainActivityLauncherCompositionTest"`: **BUILD SUCCESSFUL** — all 17 unit & Robolectric test suites (60+ individual test cases including `SecurityLockManagerTest`, `AppUpdateUrlHelperTest`, `OfflineStorageManagerHelperTest`, `OfflineStorageManagerTest`, `AppCheckInitTest`, `BiometricAuthHelperTest`, `DocxTextExtractorTest`, `ExcelRosterParserTest`, `NotificationDrawerLogicTest`, `QrCodeUtilsTest`, and `TeacherAnalyticsAndAuthTest`) **PASSED**.
- `firestore.rules`: Deployed to Firebase project `gen-lang-client-0300039938` (`ai-studio-android-bioterre-eb3decdd-52a7-4d8e-8dc6-bad24a12b2c5`). *(Note: Live Firestore Emulator integration tests in `SvtRepositoryRuleTest` / `firestore.test.js` were not run in this container because `@firebase/rules-unit-testing` / local emulator binaries were not installed; rule syntax and deployment succeeded on Firebase.)*
