# CareConnect

CareConnect is a mobile app: Native Android (Kotlin + Jetpack Compose).

## Screens

- **Welcome**: Entry point; update check and session check
- **Phone entry**: Register or sign in with mobile number
- **OTP entry**: Verify code; also used for fresh-code re-authentication
- **Patient Home**: Patient hub with shortcuts and bottom navigation
- **Doctor schedule**: Today's appointments and booking requests
- **Doctor search**: Find active doctors by specialization
- **Doctor profile and booking**: Review doctor, pick free slot and confirm (also used to reschedule)
- **Appointments**: List upcoming and past appointments with actions
- **Chat**: Conversations and messages with appointment doctors
- **Records**: Prescriptions list and history timeline
- **Prescription detail**: View and download prescription PDF
- **Consultation**: Add note and issue prescription for own patient
- **Availability**: Set weekly availability
- **Settings**: Alert preferences and help
- **Security and account**: Device safety, number change, account deletion

## Technology

- Kotlin, Jetpack Compose with Material 3, MVVM with ViewModel + StateFlow, Navigation Compose, Room for local storage, Hilt or manual DI, Gradle Kotlin DSL, JUnit + Compose UI tests
- Package / bundle: `com.mobile360.healthcare`

## Build and run

Requirements: JDK 17 and Android Studio (or the Android SDK command-line tools).

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew installDebug         # install it on a running emulator or a connected phone
```

Or open the folder in Android Studio and run the `app` configuration.

## Tests

```bash
./gradlew testDebugUnitTest
```

Device tests (Maestro) are in `.maestro/`: start an emulator, install the app, then run `maestro test -e APP_ID=<application id> .maestro`.

## Sample data

The app runs without a backend: its repositories serve seeded sample records, so every screen works out of the box. To connect the real service, implement the repository interfaces against the API and change the bindings - the screens do not change.

Sample images and people are placeholders: replace them with your own before a release.

## Status

Built; the tests that need a device or a build tool this machine lacks did not run here - the CI pipeline runs them.

## Continuous integration

`.github/workflows/android-ci.yml` builds and tests every push and pull request.
