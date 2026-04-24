# Changelog

## Unreleased

### Added
- Phase 15 player stats system with per-player stat tracking, chat stats command, GUI stats viewer, and PlaceholderAPI stat placeholders.
- Phase 14 debug tracing with `/bc debug on [player]`, `/bc debug off`, and `/bc debug status`.

### Changed
- Player open counts now live under the shared stats-backed player data section to keep limits, broadcasts, pity, and stats aligned.
- Server `server_stats.yml` writes are now coalesced with the leaderboard save cycle instead of being flushed on every open.

### Notes
- The build is passing with the same 2 existing deprecation warnings in `GUIManager` and `BroadcastManager`.
