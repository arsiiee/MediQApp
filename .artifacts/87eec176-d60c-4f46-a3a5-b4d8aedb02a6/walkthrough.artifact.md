# Walkthrough - Display Address on Profile Under Date of Birth

Successfully added the **Address** row to the user profile details card on the `ProfileScreen`, positioned right under the Date of Birth field.

## Changes

### 1. Profile Screen Address Row
- **`ProfileScreen.kt`**: Added the Address field (`UserProfile.address`) below Date of Birth with a horizontal divider inside the details card.

## Verification Results

### Automated Tests
- Ran Gradle build `app:assembleDebug` successfully:
  ```
  BUILD SUCCESSFUL
  ```
