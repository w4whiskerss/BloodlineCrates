# BloodlineCrates

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&height=220&color=8B0000&text=BloodlineCrates&fontColor=ffffff&fontAlignY=38&desc=Paper%20crate%20plugin%20with%20editor%2C%20keys%2C%20stats%2C%20and%20debugging&descAlignY=58&animation=fadeIn" alt="BloodlineCrates banner" />
</p>

<p align="center">
  <strong>A feature-rich Paper crate plugin with a real admin workflow, not just a reward menu.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Paper-1.21-red?style=for-the-badge" alt="Paper" />
  <img src="https://img.shields.io/badge/Build-Gradle-darkred?style=for-the-badge" alt="Gradle" />
  <img src="https://img.shields.io/badge/Keys-Physical%20%2B%20Virtual-black?style=for-the-badge" alt="Keys" />
  <img src="https://img.shields.io/badge/Includes-Stats%20%2B%20Debug-red?style=for-the-badge" alt="Stats and Debug" />
</p>

<p align="center">
  <a href="#quick-start">Quick Start</a>
  •
  <a href="#how-to-create-your-stuff">Create Your Stuff</a>
  •
  <a href="#admin-flow">Admin Flow</a>
  •
  <a href="#commands">Commands</a>
  •
  <a href="#debug-and-stats">Debug and Stats</a>
</p>

---

## Overview

BloodlineCrates is built around the real workflow server owners actually need:
- create crate configs
- give or test keys
- place crates in the world
- edit rewards in-game
- tune cooldowns, pity, limits, and broadcasts
- inspect player stats
- trace problems with debug output

It supports:
- random crates
- selectable crates
- physical keys
- virtual keys
- in-game editor menus
- cooldowns
- pity
- limits
- timed rewards
- broadcast system
- migration tools
- player stats
- debug tracing

## Build

```powershell
./gradlew.bat build
```

Built jar:

```text
build/libs/
```

## Quick Start

1. Put the jar into your server `plugins/` folder.
2. Start the server once so files generate.
3. Stop the server and check:

```text
plugins/BloodlineCrates/config.yml
plugins/BloodlineCrates/crates/example.yml
```

4. Start the server again.
5. Reload the plugin:

```text
/bc reload
```

6. Open the editor:

```text
/bc editor
```

7. Use the included example crate:

```text
/bc givekey example <yourname> 5
/bc preview example
/bc open example
```

---

## How To Create Your Stuff

This plugin really revolves around 3 things:
- crates
- keys
- placements

### Crates

A crate defines:
- its id
- its display name
- whether it is `RANDOM` or `SELECTABLE`
- its rewards
- its key mode
- optional pity, cooldown, and limits

### Keys

A crate can use exactly one key mode:
- `PHYSICAL`
- `VIRTUAL`

You can switch that inside the editor.

### Placements

Placements are the actual in-world crate locations players click.

---

## Create A Crate

### Fastest method

1. Open the editor:

```text
/bc editor
```

2. Go into the crate manager.
3. Pick a crate to edit, or duplicate the example config from disk.
4. Configure:
- display name
- description
- crate type
- rewards
- key display item
- key mode
- layout
- cooldown
- pity
- limits

### Config-first method

Create a new `.yml` file inside:

```text
plugins/BloodlineCrates/crates/
```

The easiest route is:
1. copy `example.yml`
2. change the `id`
3. change the crate display name
4. replace the rewards
5. reload crate files:

```text
/bc reloadcrates
```

The included template is here:

[example.yml](D:/W4Whiskers/Development/Projects/Minecraft%20Mods/BloodlineCrates/src/main/resources/crates/example.yml)

---

## Choose The Crate Type

### Random

Use a random crate when you want:
- classic RNG openings
- rare jackpot hits
- weighted prize pools

### Selectable

Use a selectable crate when you want:
- players to choose their reward
- battle pass style claim menus
- premium reward selectors

---

## Choose The Key Type

### Physical keys

These are real inventory items.

Give them with:

```text
/bc givekey <crate_id> <player> [amount]
```

### Virtual keys

These are stored in player data.

Give them with:

```text
/bc givevirtualkey <crate_id> <player> [amount]
```

### Important

Each crate can only use one key mode at a time.

---

## Create Rewards

Rewards are configured per crate.

Current reward flows include:
- item rewards
- command rewards
- economy rewards
- crate rewards
- multi rewards
- broadcast-aware rewards

### Good basic reward setup

For a simple item reward:
- set the item
- set the chance
- give it a clean display name
- optionally add a description
- optionally enable broadcast

### Good selectable setup

For selectable crates:
- keep the reward pool readable
- make icons visually distinct
- use clear names so the menu feels premium

---

## Place Crates In The World

### Place by command

Stand where you want the crate and use:

```text
/bc setcrate <crate_id>
```

### Item-based flow

If you want the crate item first:

```text
/bc givecrate <crate_id> <player>
```

Then place the crate item in the world.

### Useful placement commands

```text
/bc removecrate
/bc listcrates
/bc teleportcrate <placement_id>
```

---

## Admin Flow

This is the cleanest workflow if you are building out a real server setup:

1. Create or copy a crate config.
2. Decide whether it is `RANDOM` or `SELECTABLE`.
3. Decide whether it uses `PHYSICAL` or `VIRTUAL` keys.
4. Build the rewards.
5. Preview it:

```text
/bc preview <crate_id>
```

6. Give yourself test keys:

```text
/bc givekey <crate_id> <yourname> 10
```

7. Place the crate in the world.
8. Open it repeatedly and test:
- key consumption
- reward selection
- cooldown
- pity
- broadcasts
- stats

9. If something feels wrong, trace it:

```text
/bc debug on <player>
```

---

## Commands

### Core

```text
/bc editor
/bc reload
/bc reloadcrates
/bc preview <crate_id>
/bc open <crate_id>
```

### Keys and crates

```text
/bc givekey <crate_id> <player> [amount]
/bc givevirtualkey <crate_id> <player> [amount]
/bc givecrate <crate_id> <player>
/bc setcrate <crate_id>
/bc removecrate
/bc listcrates
/bc teleportcrate <placement_id>
```

### Stats and debug

```text
/bc stats [player]
/bc stats gui [player]
/bc debug on [player]
/bc debug off
/bc debug status
```

### Other admin systems

```text
/bc limits ...
/bc migrate ...
/bc claims
```

---

## Debug And Stats

### Stats

View your own stats:

```text
/bc stats
/bc stats gui
```

View another player's stats:

```text
/bc stats <player>
/bc stats gui <player>
```

Viewing another player requires admin access.

### Debug

Trace all players:

```text
/bc debug on
```

Trace one player:

```text
/bc debug on <player>
```

Turn it off:

```text
/bc debug off
```

Check state:

```text
/bc debug status
```

---

## PlaceholderAPI

Stats placeholders now include:

```text
%bloodcrates_stats_total%
%bloodcrates_stats_opens_<crateId>%
%bloodcrates_stats_rarest_<crateId>%
%bloodcrates_stats_rarest_chance_<crateId>%
%bloodcrates_stats_last_<crateId>%
```

Existing placeholders for keys, cooldowns, timed keys, and leaderboards are still available too.

---

## Notes

- `/bc reload` refreshes managers and plugin config.
- `/bc reloadcrates` refreshes crate files and reapplies crate cooldown definitions.
- Virtual keys are supported.
- Virtual crates were removed from the design. The crate itself is still a real placed crate, while the key mode can be virtual.
- Current build status is clean aside from 2 existing deprecation warnings in older code paths.
