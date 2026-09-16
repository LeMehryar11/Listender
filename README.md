# Listender

> A lightweight, privacy-first to-do and habit tracker engineered for Android. It is completely offline, requiring zero unnecessary permissions, and is designed around a clear star-achievement mechanism across distinct temporal scopes: Daily, Weekly, Monthly, and Yearly.

[![Licence: GPL v3](https://img.shields.io/badge/Licence-GPL_v3-blue.svg)](LICENSE)

**Author:** Mehryar Lessani
**Coding agents used:** Google Gemini

---

## Key Features

- **Consolidated Temporal Scopes:** Organise tasks into **Daily**, **Weekly**, **Monthly**, and **Yearly** scopes with seamless past and future navigation.
- **Silver & Gold Star Progression:** Tasks specify an achievement target. Earned stars appear in silver until the minimum target is achieved, transitioning immediately into gold.
- **One-Touch Period Import:** Rapidly copy non-duplicate tasks from yesterday, last week, last month, or last year into the current interval without altering existing tasks.
- **Uncluttered Tasks Interface:** Minimalist layout prioritising tasks, immediate temporal switching, notes scratchpad, and a dedicated Floating Action Button (FAB) for adding tasks.
- **Performance & Reports Tab:** Interactive Bézier trend line graphs detailing completion percentages and star metrics across Days, Weeks, Months, and Years alongside current scope breakdown cards.
- **Backup & Restore:** Full offline backup import and export via structured JSON with selectable collision strategies (Merge progress, Skip existing, or Overwrite).
- **Dedicated Settings Tab:** Quick triplets for Appearance (Light / Dark / System), First day of the week preferences, Backup & Restore, and Starter Defaults reset.
- **Period Performance Banners:** Real-time completion percentages and star metrics calculated for every day, week, month, and year.
- **Scoped Period Notes:** An integrated scratchpad for every individual day, week, month, and year with contextual hints.
- **Zero Permissions & Full Privacy:** 100% on-device local persistence with Room Database. Zero network calls and zero telemetry.

---

## Technical Stack

- **UI Framework:** Jetpack Compose with Material 3
- **Language:** Kotlin 2.0 (Coroutines & Flow)
- **Local Persistence:** Room Database with KSP
- **Architecture:** Clean Architecture & MVVM
- **Target SDK:** Android 16 (API 36)
- **Minimum SDK:** Android 7.0 (API 24)

---

## Licence

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for the full licence text.

---

## Installation & Download

Ready-to-install Android APK packages (`Listender.apk`) are available on the [GitHub Releases](../../releases) page. Download the latest `.apk` directly to your Android device to install.

---

## Building and Verification

Run the standard compilation toolchain:

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```
