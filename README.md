# Public Library Visitor System

A Java Android application with a JavaScript-based interface and Firebase Authentication + Cloud Firestore. Library staff can sign in, register visitor details, and view visitor records.

## Features

- Email/password sign-in and account creation with Firebase Authentication.
- Visitor registration with name, phone number, date of birth, email, and address.
- Cloud Firestore storage in the `visitors` collection and a live visitor list.
- Sample visitor records seeded automatically when the collection is empty, with a button to add them again.

## Run in Android Studio

1. Open this repository in Android Studio and allow Gradle to sync. The included Gradle wrapper uses Gradle 8.9; use JDK 17 for the Android Gradle Plugin.
2. In Firebase Console, create a project and register an Android app with package name `com.library.visitorsystem`.
3. Download that app's `google-services.json` and replace `app/google-services.json` (the checked-in file is a build-only placeholder, not usable Firebase credentials).
4. In Firebase Authentication, enable **Email/Password**.
5. Create a Cloud Firestore database. For development, rules can allow access to signed-in users; tighten the rules before production. For example:

   ```text
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /visitors/{visitorId} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

6. Run the `app` configuration on an emulator or Android device (Android 7.0 / API 24 or newer). From a terminal, build with `./gradlew :app:assembleDebug`.

Sample visitors contain fictional contact details. Registration records and sample records are visible to signed-in app users under the example Firestore rules above.