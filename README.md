# Wants Tracker

Personal, offline Android wishlist tracker (Kotlin + Jetpack Compose + Room). No account, no ads, no network.

## Build
1. Open this folder in **Android Studio** (Ladybug 2024.2 or newer).
2. If asked for a Gradle version, use **8.9** or newer (Android Studio can download it). A `gradle-wrapper.properties` is not included; Studio will offer to create the wrapper.
3. Let Gradle sync, then press Run. minSdk 26 (Android 8.0), targetSdk 35.

## What's inside
- **Wants** with name, price (INR), link, photo, notes, priority, category, planned quarter (or Unplanned).
- **Status**: Not started / In progress / Bought / Failed (failed needs a reason). Every change is logged in a timeline.
- **Categories** (these double as goals): your own name, emoji and colour.
- **Home**: list grouped by quarter, or a 4-column board; filter by status / year / category / priority; search; sort.
- **Past years**: set any past quarter when adding; bulk import from pasted text or CSV (Settings > Import).
- **Overdue** wants (quarter has passed) are flagged, with one-tap "move to this quarter".
- **Cooling-off timer** (7/14/30 days) with a reminder when it ends.
- **Insights**: bought vs failed, success rate, per-quarter bars, spend by category, "let go" reasons, year in review.
- **Reminders**: new-quarter summary, cooling-off end, nudge for wants stuck in progress 30+ days.
- **Widget**, **app lock** (biometric / screen lock), **dark mode**, **backup & restore** (one JSON file, photos included).

## Import format (one want per line)
```
name, price, quarter, status, category, note
iPhone 15, 79900, Q2 2025, bought, Tech
Goa trip, 25000, Q4 2025, failed, Travel, Too expensive
Standing desk, 18000, , not started, Home
```

## Notes
- Quarters are calendar quarters (Q1 = Jan-Mar).
- A want's "year" for filters and insights is its planned year, or the year it was added if unplanned.
- This code was written without an Android SDK to compile against, so expect to fix a few small compile errors on first build.
