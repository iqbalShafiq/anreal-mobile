# Anreal

Kotlin Multiplatform mobile client for [DocChat](https://github.com/iqbalShafiq) (`chat-with-document`). Android ships first. iOS compiles; platform actuals may be TODOs.

- Architecture and coding rules: [AGENTS.md](./AGENTS.md)
- Visual, motion, UX-state, and accessibility contract: [DESIGN.md](./DESIGN.md)
- Implementation issues: [github.com/iqbalShafiq/anreal-mobile/issues](https://github.com/iqbalShafiq/anreal-mobile/issues)

The tree is still the Compose Multiplatform template (`:androidApp` + `:shared` + `:iosApp`). Milestone 0 splits it into `:app` / `:core:*` / `:feature:*`.

### Run

- Android: `./gradlew :androidApp:assembleDebug`
- iOS: open `iosApp` in Xcode

### Test

- `./gradlew :shared:testAndroidHostTest`
