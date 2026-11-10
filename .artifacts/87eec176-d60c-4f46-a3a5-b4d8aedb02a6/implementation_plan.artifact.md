# Profile & Account Management Flow Implementation

Implement the complete Profile interface, Edit Profile screen, and Change Password screen matching the user's detailed design mockups (7 screenshots).

## User Review Required

> [!IMPORTANT]
> - **Profile Screen**: Updates the Profile screen to match the mockup: green header "My Profile", avatar, user details card (Mobile number, Email), "Edit my details" button, and "Sign out" button with custom confirmation dialog.
> - **Edit My Details Screen**: Adds a dedicated edit screen with Personal Information inputs (Full Name, Phone Number), Save/Cancel buttons, and a "Change Password" section.
> - **Change Password Screen**: Adds a password update screen with Current Password, New Password, and Confirm Password fields, real-time validation checks, and success state banner.

## Proposed Changes

### [Navigation & Routes]
#### [MODIFY] [Routes.kt](file:///C:/Users/amber/OneDrive/Desktop/MediQApp/app/src/main/java/com/example/mediq/ui/navigation/Routes.kt)
- Add `EditProfile` and `ChangePassword` routes.

#### [MODIFY] [MediQNavHost.kt](file:///C:/Users/amber/OneDrive/Desktop/MediQApp/app/src/main/java/com/example/mediq/ui/navigation/MediQNavHost.kt)
- Register `EditProfileScreen` and `ChangePasswordScreen`.

### [Profile Screens]
#### [MODIFY] [ProfileScreen.kt](file:///C:/Users/amber/OneDrive/Desktop/MediQApp/app/src/main/java/com/example/mediq/ui/screens/ProfileScreen.kt)
- Redesign `ProfileScreen` to match screenshots 1 & 2 (green header "My Profile", white card with Mobile number & Email, Edit my details button, Sign out button, and styled confirmation dialog).

#### [NEW] [EditProfileScreen.kt](file:///C:/Users/amber/OneDrive/Desktop/MediQApp/app/src/main/java/com/example/mediq/ui/screens/EditProfileScreen.kt)
- Create `EditProfileScreen` (Personal Information fields, Save Changes & Cancel buttons, Change Password card with lock icon, and success banner).

#### [NEW] [ChangePasswordScreen.kt](file:///C:/Users/amber/OneDrive/Desktop/MediQApp/app/src/main/java/com/example/mediq/ui/screens/ChangePasswordScreen.kt)
- Create `ChangePasswordScreen` (Current Password, New Password, Confirm Password with eye visibility toggles, real-time validation checks, Save Changes & Cancel buttons).

## Verification Plan

### Automated Tests
- Run Gradle build: `app:assembleDebug`

### Manual Verification
- Deploy and navigate through Profile -> Edit My Details -> Change Password, testing validation errors and success states.
