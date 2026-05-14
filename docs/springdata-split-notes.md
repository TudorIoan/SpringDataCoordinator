# SpringData Coordinator Split Notes

This project was copied from `/Users/tudor/Desktop/Work/ProgressPath` on 2026-05-14 as a role-specific SpringData app.

## Current split state

- App label is `SpringData Coordinator`.
- `BuildConfig.SPRINGDATA_APP_ROLE` is `coordinator`.
- Registration is pinned to the `coordinator` role instead of showing the role switch.
- Bottom navigation uses the app role, so Objective Library is visible only in the coordinator app.
- The codebase intentionally remains structurally close to ProgressPath for the first extraction pass.

## Firebase note

`app/google-services.json` still contains only the original Firebase Android package `net.abaresults.progresspath`. The Gradle `applicationId` has intentionally been left unchanged for now so the copied projects can still build with the existing Firebase config.

Before installing both SpringData apps side by side or publishing them separately, create two Android app entries in Firebase and add matching `google-services.json` files, then change `applicationId` values to distinct IDs such as:

- `net.abaresults.springdata.coordinator`
- `net.abaresults.springdata.therapist`

## Next hardening pass

- Add login guards that reject accounts with the wrong `userType` for this app.
- Remove unreachable coordinator-only screens from the therapist app after Firebase/package IDs are finalized.
- Remove unreachable therapist-only management code from the coordinator app only if it is not needed for coordinator therapist management.
- Consider extracting shared repositories/models into a shared Android library module if the apps will evolve together.
