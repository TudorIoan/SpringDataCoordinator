# Mastery Criteria Refactor Plan

Date: 2026-05-14

## Session Context

This note captures the useful context from the codebase familiarization pass and the proposed refactor plan for moving mastery criteria from objective item templates to kid-specific assigned objectives.

The project is an Android/Kotlin XML app for tracking ABA patient progress. It uses:

- Firebase Auth and Firestore for authentication and backend data.
- Hilt for dependency injection.
- Fragment + ViewModel + sealed Action/State classes for UI features.
- XML layouts with view binding and data binding.
- Room for cached Unsplash photos.
- Retrofit for Unsplash API access.
- MPAndroidChart for charting.
- iText for PDF report generation.

The build was verified with:

```sh
./gradlew testDebugUnitTest
```

Result: build successful. The task reported `NO-SOURCE` for unit tests, so this only confirms compile/build health for that task.

Current branch state at the time of analysis:

- Branch: `main`
- Status: clean
- Remote status: ahead of `origin/main` by 1 commit

## Current Architecture Notes

Primary app wiring:

- `app/src/main/java/net/abaresults/progresspath/BaseApp.kt`
- `app/src/main/java/net/abaresults/progresspath/view/main/MainActivity.kt`
- `app/src/main/res/navigation/main_nav_graph.xml`

Dependency injection:

- `app/src/main/java/net/abaresults/progresspath/module/FirebaseModule.kt`
- `app/src/main/java/net/abaresults/progresspath/module/NetworkModule.kt`
- `app/src/main/java/net/abaresults/progresspath/module/DbModule.kt`

Important repositories:

- `UserRepository`: Firebase auth, user profile loading, registration, therapist invites.
- `ClinicRepository`: clinic CRUD and clinic lists by coordinator or therapist.
- `KidRepository`: kid CRUD, therapist assignment/invites, kid removal cascade.
- `ObjectiveRepository`: objective library CRUD and propagation to assigned kid objectives.
- `KidObjectiveRepository`: assigns objectives to kids, fetches assigned objectives, updates progress.
- `OrgRepository`: in-memory session/selection holder for selected clinic, kid, objective, kid objective, report bytes, and level.

Important model concepts:

- `Clinic`: top-level organization unit.
- `Kid`: belongs to a clinic and has assigned therapists.
- `Objective`: reusable library/template objective.
- `ObjItem`: reusable objective item template.
- `KidObjective`: an objective assigned to a specific kid.
- `KidObjectiveItem`: kid-specific progress state for a copied objective item.

Current progress behavior:

- Progress tracking lives mainly in `ItemsViewModel`.
- `KidObjectiveItem` stores response history:
  - `yesNoList`
  - `frequencyList`
  - `checkmarkList`
  - `percentageList`
- Yes/no mastery currently reads `item.objItem.consecutiveYesses`.
- Therapist item visibility is filtered by `isItemAvailableForTherapist` in `Util.kt`.

## Refactor Goal

Move mastery criteria from `ObjItem.consecutiveYesses` to `KidObjective.consecutiveYesses`.

Current model:

```kotlin
data class ObjItem(
    val name: String = "",
    val normalizedName: String = "",
    val type: ObjItemType = ObjItemType.YES_NO,
    val consecutiveYesses: Int? = null
)
```

Target model:

```kotlin
data class ObjItem(
    val name: String = "",
    val normalizedName: String = "",
    val type: ObjItemType = ObjItemType.YES_NO
)

data class KidObjective(
    val id: String = "",
    val kidId: String = "",
    val objectiveId: String = "",
    val active: Boolean = false,
    val consecutiveYesses: Int? = null,
    ...
)
```

Semantics:

- `KidObjective.consecutiveYesses == null`: coordinator decides manually.
- `KidObjective.consecutiveYesses == 1..20`: auto-master after that many consecutive yes responses.
- Objective library items become reusable templates without kid-specific mastery rules.

## Implementation Plan

### 1. Add the new field first

Add `consecutiveYesses: Int? = null` to `KidObjective`.

Keep `ObjItem.consecutiveYesses` temporarily during the migration period so old Firestore data still deserializes and the app can stay backward compatible while behavior moves to the new location.

### 2. Change the objective assignment flow

In the "assign objective to kid" path, likely around:

- `app/src/main/java/net/abaresults/progresspath/view/objectives/add_objective/AddObjectiveViewModel.kt`
- `app/src/main/java/net/abaresults/progresspath/view/objectives/add_objective/AddObjectiveFragment.kt`
- `app/src/main/java/net/abaresults/progresspath/repo/KidObjectiveRepository.kt`

Add a modal before creating the `KidObjective`.

The modal should allow the coordinator to choose:

- Coordinator decides
- Consecutive yeses, with a numeric selector/input

Then update `KidObjectiveRepository.addKidObjective(...)` so it accepts `consecutiveYesses: Int?` and stores it on the newly created `KidObjective`.

### 3. Update mastery logic

In `ItemsViewModel`, replace usage of item-level mastery criteria:

```kotlin
val consecutiveYesses = item.objItem.consecutiveYesses
```

with kid-objective-level criteria:

```kotlin
val consecutiveYesses = orgRepo.requireSelectedKidObjective().consecutiveYesses
```

During migration, this can use an effective fallback:

```kotlin
val kidObjective = orgRepo.requireSelectedKidObjective()
val consecutiveYesses = kidObjective.consecutiveYesses
    ?: kidObjective.itemsList.mapNotNull { it.objItem.consecutiveYesses }.distinct().singleOrNull()
```

The fallback should be temporary and removed after Firestore data is migrated.

### 4. Update objective library and item UI

Remove mastery criteria UI from objective item creation/editing because objective items should no longer own kid-specific mastery rules.

Likely files:

- `app/src/main/java/net/abaresults/progresspath/view/obj_item_library/add_item/AddObjItemViewModel.kt`
- `app/src/main/java/net/abaresults/progresspath/view/obj_item_library/AddObjItemFragment.kt`
- `app/src/main/java/net/abaresults/progresspath/view/obj_item_library/ObjItemLibraryViewModel.kt`
- related XML layouts for add/edit item dialogs

Objective item UI should only define:

- item name
- item type

### 5. Update objective update/sync logic

In `KidObjectiveRepository.updateAllKidObjectives(...)`, item identity currently includes `consecutiveYesses`:

```kotlin
Triple(it.objItem.normalizedName, it.objItem.type, it.objItem.consecutiveYesses)
```

Change this identity to exclude mastery criteria:

```kotlin
Pair(it.objItem.normalizedName, it.objItem.type)
```

This matters because mastery criteria is no longer part of the item template and should not cause items to be treated as different.

### 6. Remove the old field after migration

After the app has shipped with backward-compatible reads and the Firestore migration is complete, remove `consecutiveYesses` from `ObjItem` and clean up any remaining UI/data references.

## Firestore Transition Plan

Use a safe two-phase migration.

### Phase 1: Backward-compatible app release

Ship an app version that can read both structures.

Behavior should prefer:

```kotlin
KidObjective.consecutiveYesses
```

but temporarily fall back to:

```kotlin
itemsList[].objItem.consecutiveYesses
```

Any time a `KidObjective` is saved, write the new `consecutiveYesses` field. This gives passive migration for records users touch.

Do not delete legacy nested fields in this phase.

### Phase 2: Admin/backend migration script

Run a one-time Firestore migration over `kid_objectives`.

For each `kid_objectives/{id}` document:

1. If `consecutiveYesses` already exists, skip it.
2. Inspect `itemsList[].objItem.consecutiveYesses`.
3. If all non-null values are the same, copy that value to `kid_objectives/{id}.consecutiveYesses`.
4. If there are no values, set `consecutiveYesses = null`.
5. If there are mixed values, set `consecutiveYesses = null` and log the document ID for manual review.
6. Add migration metadata such as `migrationVersion = 1` and `migratedAt = serverTimestamp()`.

Pseudo-logic:

```kotlin
for each kid_objective:
    if document has consecutiveYesses:
        continue

    values = itemsList
        .mapNotNull { item.objItem.consecutiveYesses }
        .distinct()

    migratedValue = when {
        values.size == 1 -> values.first()
        else -> null
    }

    update document:
        consecutiveYesses = migratedValue
        migrationVersion = 1
        migratedAt = serverTimestamp()
```

Recommended safety behavior:

- Run a dry-run first and log counts:
  - total documents scanned
  - already migrated
  - migrated with a value
  - migrated with null
  - mixed-value/manual-review cases
- Use batched writes with conservative batch sizes.
- Do not delete old nested fields in the first migration.
- After production confidence, run a cleanup migration or let future app writes omit the old field.

## Data Compatibility Notes

The new model cannot represent different `consecutiveYesses` values per item inside the same `KidObjective`. If old data contains mixed values inside one assigned objective, that case must be handled deliberately.

Recommended rule:

- If all existing non-null item values are identical, migrate that value.
- If values differ, migrate to `null` and manual review, because "coordinator decides" is safer than inventing one mastery threshold.

## Suggested Implementation Order

1. Add `KidObjective.consecutiveYesses`.
2. Add assignment modal and pass selected mastery criteria into `addKidObjective`.
3. Update `ItemsViewModel` mastery calculation to use `KidObjective`.
4. Update repository item identity logic to exclude `ObjItem.consecutiveYesses`.
5. Remove mastery UI from objective item add/edit screens.
6. Keep backward-compatible fallback reads.
7. Build and manually test:
   - coordinator assigns objective with "coordinator decides"
   - coordinator assigns objective with consecutive yeses
   - yes/no item auto-masters after threshold
   - old Firestore documents still behave correctly
   - objective library editing still propagates items correctly
8. Ship backward-compatible app.
9. Run dry-run Firestore migration.
10. Run Firestore migration.
11. After confidence, remove `ObjItem.consecutiveYesses` and any legacy fallback code.

## Known Build/Tooling Notes

The local shell environment had a sparse `PATH`, so Gradle needed explicit `JAVA_HOME` and `PATH` when run from this Codex session.

Working command shape:

```sh
JAVA_HOME=/Users/tudor/Library/Java/JavaVirtualMachines/ms-17.0.15/Contents/Home \
PATH=/usr/bin:/bin:/usr/sbin:/sbin:/Users/tudor/Library/Java/JavaVirtualMachines/ms-17.0.15/Contents/Home/bin \
/bin/sh ./gradlew testDebugUnitTest
```

Gradle warnings observed:

- Android Gradle Plugin `8.1.4` is only tested up to compile SDK 34, while the app uses compile SDK 35.
- Kapt support for Kotlin language version 2.0+ is alpha and falls back to 1.9.
- Some dependencies trigger Jetifier mixed AndroidX/support-library warnings.

