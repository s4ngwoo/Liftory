# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Korean translation: [ko/CHANGELOG.md](ko/CHANGELOG.md)

---

## [Unreleased]

### Added
- Standardized documentation system (`docs/` and `notes/`).
- Android Clean Architecture scaffold with Jetpack Compose.
- Workout session edit (name/notes) and deletion with confirmation dialog and CASCADE set cleanup.
- Bottom-to-top layout for `ExerciseSetEditorSheet` with automatic autofocus and previous set reference.

### Fixed
- Fixed software keyboard Enter key inserting newlines instead of jumping to the next input field via `singleLine = true`, `ImeAction.Next`/`Done`, and `FocusRequester`.
