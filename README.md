# BloodlineCrates

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&height=220&color=8B0000&text=BloodlineCrates&fontColor=ffffff&fontAlignY=38&desc=Premium%20Paper%201.21.11%20Crate%20Plugin&descAlignY=58&animation=fadeIn" alt="BloodlineCrates banner" />
</p>

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=18&pause=1200&color=E53935&center=true&vCenter=true&width=900&lines=RNG+Crates;Selectable+Non-RNG+Crates;Virtual+%2B+Physical+Keys;Timed+Rewards%2C+Analytics%2C+Anti-Exploit;Built+for+high-energy+Minecraft+servers" alt="Animated feature banner" />
</p>

<p align="center">
  <strong>A premium-style Paper crate plugin for modern Minecraft networks.</strong>
</p>

<p align="center">
  Built for <strong>Paper 1.21.11</strong> with <strong>RNG crates</strong>, <strong>selectable crates</strong>, <strong>virtual + physical keys</strong>, <strong>timed rewards</strong>, and a modular architecture designed to scale.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Paper-1.21.11-red?style=for-the-badge&logo=github" alt="Paper 1.21.11" />
  <img src="https://img.shields.io/badge/Status-Active%20Build-darkred?style=for-the-badge" alt="Active Build" />
  <img src="https://img.shields.io/badge/Storage-YAML%20%2B%20MySQL-black?style=for-the-badge" alt="Storage" />
  <img src="https://img.shields.io/badge/Mode-RNG%20%2B%20Selectable-red?style=for-the-badge" alt="Modes" />
</p>

<p align="center">
  <a href="https://github.com/w4whiskerss/BloodlineCrates">Repository</a>
  •
  <a href="#features">Features</a>
  •
  <a href="#quick-start">Quick Start</a>
  •
  <a href="#commands">Commands</a>
  •
  <a href="#architecture">Architecture</a>
</p>

---

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=rect&height=3&color=E53935" alt="divider" />
</p>

## Overview

BloodlineCrates is designed to feel closer to a polished premium crate system than a basic reward menu. It supports both classic weighted reward crates and a standout non-RNG selectable mode where players choose the reward they want instead of hoping for a lucky roll.

The plugin is built around clean managers, reloadable YAML configs, optional MySQL-backed player data, PlaceholderAPI support, in-game admin tools, analytics, and anti-exploit protections.

<p align="center">
  <img src="https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExcjZ1MHd4MHVyODl2d2l6b2t0Y2VmbmN2NGxwOGc1cjQwaGs4bGc2ZSZlcD12MV9naWZzX3NlYXJjaCZjdD1n/13HgwGsXF0aiGY/giphy.gif" width="720" alt="Minecraft style energy gif" />
</p>

## Features

### Crates

- RNG crates with weighted rewards
- Selectable crates with click-to-claim reward flow
- Multi-location crate support
- Preview menus for players
- Configurable per-crate cooldowns
- Global cooldown support
- Pity protection for guaranteed higher-tier rewards
- Bundle rewards after repeated opens

### Keys

- Physical keys with stackable item support
- Virtual keys stored in player data
- Timed key rewards with permission multipliers
- Key-restricted crate openings at actual crate locations

### Admin Experience

- In-game editor shell via `/bloodcrates editor`
- GUI-based crate and reward management
- Quick reward creation from item in hand
- Cached editing workflow with save support
- Audit log tracking for admin actions

### Polish

- Internal Chest GUI system
- Selectable reward menus
- Crate previews with optional chance visibility
- Animation modes and opening effects
- Analytics and open/reward logs
- PlaceholderAPI expansion
- Public Bukkit events for developer hooks

## Highlights

| System | Included |
| --- | --- |
| RNG crates | Yes |
| Selectable non-RNG crates | Yes |
| Physical keys | Yes |
| Virtual keys | Yes |
| Timed keys | Yes |
| YAML storage | Yes |
| Optional MySQL player storage | Yes |
| PlaceholderAPI | Yes |
| Analytics logging | Yes |
| Audit logs | Yes |
| Public events | Yes |

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=rect&height=3&color=8B0000" alt="divider" />
</p>

## Quick Start

### 1. Build

```bash
mvn package
```

Compiled output:

```text
target/BloodlineCrates-1.0.0-SNAPSHOT.jar
```

### 2. Install

Drop the jar into your server's `plugins/` folder and start Paper `1.21.11`.

### 3. Configure

Main config files:

- `src/main/resources/config.yml`
- `src/main/resources/crates.yml`
- `src/main/resources/keys.yml`
- `src/main/resources/messages.yml`

### 4. Create Content

Use the editor or commands to create crates and keys:

```text
/bloodcrates editor
/bloodcrates create crate <name>
/bloodcrates create key <name>
```

## Commands

### Admin

```text
/bloodcrates editor
/bloodcrates givecrate <crate> <player>
/bloodcrates givekey <crate> <player> [amount]
/bloodcrates reload
/bloodcrates create crate <name>
/bloodcrates create key <name>
/bloodcrates test <crate>
```

### Player

```text
/bloodcrates open <crate>
/bloodcrates keys
/bloodcrates preview <crate>
```

## Permissions

```text
bloodlinecrates.admin
bloodlinecrates.editor
bloodlinecrates.editor.crates
bloodlinecrates.editor.keys
bloodlinecrates.create.crate
bloodlinecrates.create.key
bloodlinecrates.open.<crate>
bloodlinecrates.preview.<crate>
bloodlinecrates.reward.<reward>
bloodlinecrates.skipanimation
```

## PlaceholderAPI

Available placeholders include:

```text
%bloodcrates_keys_<crate>%
%bloodcrates_total_keys%
%bloodcrates_crates_opened%
%bloodcrates_last_reward%
%bloodcrates_next_key_<crate>%
%bloodcrates_time_until_next_key%
%bloodcrates_lb_daily_1%
%bloodcrates_lb_weekly_1%
%bloodcrates_lb_monthly_1%
%bloodcrates_lb_yearly_1%
```

## Example Config Shape

### `crates.yml`

```yaml
crates:
  example:
    display_name: "&cExample Blood Crate"
    type: SELECTABLE
    key: example_key
    itemsadder_model: "namespace:crate_model"
    rewards:
      reward1:
        tier: COMMON
        chance: 70.0
        permission: ""
```

### `keys.yml`

```yaml
keys:
  example_key:
    display_name: "&bExample Key"
    itemsadder_model: "namespace:key_model"
    physical_enabled: true
    virtual_enabled: true
```

## Architecture

The plugin is intentionally split into focused systems so it stays maintainable as features grow.

- `CrateManager`: crate definitions, opening flow, reward logic, pity handling
- `KeyManager`: physical keys, virtual keys, timed key grants
- `GuiManager`: previews, selection menus, editor menus
- `EffectManager`: opening effects and animation timing
- `PlayerDataManager`: cached player profiles and async persistence
- `AnalyticsManager`: crate open stats, reward logs, leaderboard data
- `AntiExploitManager`: spam protection and suspicious activity checks
- `AuditLogManager`: admin action tracking

## Integrations

Soft-depend support is prepared for:

- PlaceholderAPI
- DeluxeMenus
- ItemsAdder
- DecentHolograms
- Vault

## Current Status

This repository currently provides a strong compileable core and project foundation. Some enterprise-grade integrations from the original design brief are scaffolded or partially represented at the architecture level and can be expanded further in later passes.

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=16&pause=1000&color=8B0000&center=true&vCenter=true&width=800&lines=Built+to+feel+fast.;Built+to+feel+premium.;Built+to+keep+growing." alt="Animated footer banner" />
</p>

## Project Vision

BloodlineCrates is aiming for a premium network-ready feel:

- fast enough for high player counts
- modular enough for long-term maintenance
- polished enough to feel commercial-grade
- flexible enough for future sync, economy, and hologram expansion

## License

This project currently has no explicit license file in the repository. Add one before distributing publicly if needed.
