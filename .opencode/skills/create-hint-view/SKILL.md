---
name: CreateHintView
description: Add a new dismissible and persistent HintView hint to a WordTeacher screen, or replace an existing static instruction text with HintView. Registers a HintType, wires onHidden persistence via SettingStore, and inserts HintView into a composable. Use when asked to "add a hint with HintView", "replace <text> with HintView", or register a new HintType.
---

# CreateHintView

Adds a dismissible hint to a screen using the shared `HintView`, following the
existing hint pattern (`CardSetsUI`, `DefinitionsUI`, `LearningUI`).

## Arguments

Extract these from the user's request; ask for missing required ones:

| Argument | Required | Description | Example |
| --- | --- | --- | --- |
| `hintTypeName` | yes | New `HintType` enum value to register. | `LearningTestHint` |
| `target` | yes | UI file/composable that shows the hint, plus its ViewModel. | `testChallengeUI` in `android_app/.../learning/views/LearningUI.kt` + `LearningVM` |
| `stringResource` | one of `stringResource`/`hintText` | Name of an existing (or new) string resource constant with the hint text. Preferred over hardcoding. | `learning_instruction_challenge_test` |
| `hintText` | one of `stringResource`/`hintText` | Literal English text when no resource is used. | `Tap any word to get its definition` |
| `replaces` | no | Existing UI (e.g. an instruction `Text`) that `HintView` replaces; its now-dead fields, imports and string resources must be cleaned up (but never delete a string resource that is still referenced). | instruction `Text` + `Challenge.Test.instruction` |

Derived values:
- Flow name in the VM: `needShow<ShortName>Hint`, e.g. `LearningTestHint` -> `needShowTestHint`.
- The VM must already have `SettingStore` injected; if not, add it to the constructor (and its DI module/component).

## Workflow

1. **Register the hint type** in
   `shared/src/commonMain/kotlin/com/aglushkov/wordteacher/shared/general/settings/SettingStore+Hints.kt`.
   Append the value **at the end** of the `HintType` enum. Never insert in the
   middle: hints are persisted by ordinal (`toString() = this.ordinal.toString()`),
   so reordering breaks stored settings.

2. **Add the hint text** to `toAttributedString()` in
   `shared/src/composeSharedMain/kotlin/com/aglushkov/wordteacher/shared/general/settings/HintType+toAttributedString.kt`
   (the function is `@Composable`):

   With a string resource (preferred; add the constant to
   `shared/src/commonMain/composeResources/values/strings.xml` if it doesn't exist):

   ```kotlin
   HintType.<hintTypeName> -> {
       val instruction = stringResource(Res.string.<stringResource>)
       buildAnnotatedString {
           append(instruction)
       }
   }
   ```

   Without a resource (see existing cases for style):

   ```kotlin
   HintType.<hintTypeName> -> buildAnnotatedString {
       append("<hintText>")
   }
   ```

   Note: `stringResource` must be called outside `buildAnnotatedString { }` -
   its lambda is not a composable context.

3. **Extend the ViewModel** (interface + impl, e.g. `LearningVM` / `LearningVMImpl`).
   Interface:

   ```kotlin
   val needShow<ShortName>Hint: StateFlow<Boolean>
   fun onHintHidden(hintType: HintType)
   ```

   Implementation (needs `settings: SettingStore`, `analytics: Analytics`):

   ```kotlin
   override val needShow<ShortName>Hint = MutableStateFlow(!settings.isHintClosed(HintType.<hintTypeName>))

   override fun onHintHidden(hintType: HintType) {
       analytics.send(AnalyticEvent.createActionEvent("Hint_" + hintType.name))
       settings.setHintClosed(hintType)
       needShow<ShortName>Hint.update { false }
   }
   ```

   Imports:
   `...general.settings.HintType`, `...general.settings.isHintClosed`,
   `...general.settings.setHintClosed`.
   If other classes implement the same VM interface, update them too.

4. **Insert HintView** into the target composable (example reference:
   `CardSetsUI.kt`, `DefinitionsUI.kt`, `testChallengeUI` in `LearningUI.kt`):

   ```kotlin
   val needShowHint by vm.needShow<ShortName>Hint.collectAsState()

   if (needShowHint) {
       HintView(
           hintType = HintType.<hintTypeName>,
           contentPadding = PaddingValues(
               start = LocalDimens.current.contentPadding,
               top = LocalDimens.current.contentPadding,
               end = LocalDimens.current.contentPadding,
           ),
           onHidden = { vm.onHintHidden(HintType.<hintTypeName>) }
       )
   }
   ```

   The `if (needShowHint)` guard + persisted `isHintClosed` state guarantee the
   hint never reappears after the user hides it (also across app restarts).

5. **Clean up replaced UI** (only when `replaces` is given): remove the old
   composable and any fields/imports/string resources that became unused
   because of the replacement. Keep them if they are still referenced elsewhere.

6. **Verify the build**:

   ```sh
   ./gradlew :shared:compileComposeSharedMainKotlinMetadata :android_app:compileFullDebugKotlin
   ```

   If a string resource was added/removed and compilation fails with
   `Unresolved reference '_collect...'` in generated `ActualResourceCollectors.kt`,
   the generated compose resources are stale. Fix with:

   ```sh
   rm -rf shared/build/generated/compose
   ```

   then rebuild.
