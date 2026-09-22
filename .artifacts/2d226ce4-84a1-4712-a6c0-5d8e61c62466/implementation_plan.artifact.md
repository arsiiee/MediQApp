# Implementation Plan - MediQ Frontend

Implement the frontend for the MediQ medical appointment booking application based on the provided design specifications. The app will be built using modern Android development practices, primarily Jetpack Compose.

## User Review Required

> [!IMPORTANT]
> The design uses a specific shade of green and custom typography. I will approximate these using standard Material 3 tokens unless specific brand assets are provided.

## Proposed Changes

### 🎨 Theme & Foundation
- Update `ui/theme/Color.kt` with the MediQ green palette.
- Update `ui/theme/Theme.kt` to reflect the medical branding.
- Define custom typography in `ui/theme/Type.kt`.

### 🏗️ Navigation & Shell
- Implement a `MainScreen` that hosts the `Scaffold` and `BottomNavigationBar`.
- Set up `NavHost` for all major screens:
    - Auth Flow (Splash -> Sign In -> Register)
    - Main Tabs (Home, Doctors, Appointments, Messages, Profile)
    - Detail Screens (Doctor Details, Booking Flow, Appointment Details)

### 🔐 Authentication Screens
- **Splash Screen**: Logo and welcome message.
- **Sign In Screen**: Username/Password fields, Login button, Register link.
- **Registration Flow**:
    - Patient Registration (Name, Phone)
    - OTP Verification
    - Account Credentials (Username, Password)
    - Success Screen

### 🏠 Main Navigation Tabs
- **Home Screen**:
    - Dashboard showing next consultation.
    - "Book a consultation" primary action.
    - "Browse by specialty" grid.
    - "Most open slots this week" list.
- **Doctors Screen**: Searchable list of doctors with filters.
- **Appointments Screen**: Tabbed view for Upcoming and History.
- **Messages Screen**: List of conversations (placeholder for now).
- **Profile Screen**: User details, settings, and Sign Out.

### 📅 Booking & Appointment Flow
- **Doctor Details Screen**: Experience, rating, clinic hours, and slot selection.
- **Booking Flow**: Date selection, Time slot selection, Reason for visit, Confirmation.
- **Booking Successful**: Success state with "View my appointments" action.
- **Appointment Details**: Detailed view with options to Reschedule or Cancel.

### 🔔 Notifications & Support
- **Notifications Screen**: List of appointment reminders and status updates.

## Verification Plan

### Automated Tests
- I will not be writing tests in this phase unless requested, focusing on UI implementation.

### Manual Verification
- Render Compose Previews for each screen.
- Verify navigation flow between Auth and Main App.
- Check layout responsiveness on different screen sizes (though design is mobile-first).
