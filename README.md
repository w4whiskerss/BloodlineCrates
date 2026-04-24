# BloodlineCrates

BloodlineCrates is a Paper crate plugin with:
- random and selectable crates
- physical and virtual keys
- in-game editing
- pity, cooldowns, limits, broadcasts, stats, and debug tools

This README is focused on one thing: how to actually create and manage your crate content in-game.

## Build

```powershell
./gradlew.bat build
```

Built jar:

```text
build/libs/
```

## Install

1. Put the jar into your server `plugins` folder.
2. Start the server once.
3. Stop the server after the plugin generates its files.
4. Check these files:

```text
plugins/BloodlineCrates/config.yml
plugins/BloodlineCrates/crates/example.yml
```

## Main Idea

You work with 3 things:
- crate configs: what the crate is, what rewards it has, how it opens
- keys: what players need to open the crate
- placements: where the crate exists in the world

## Quick Start

### 1. Reload the plugin

```text
/bc reload
```

### 2. Open the editor

```text
/bc editor
```

From there you can:
- browse crates
- edit rewards
- edit key display items
- change key mode
- change crate layout
- adjust cooldowns and other settings

### 3. Use the included example crate

The plugin ships with:
- crate id: `example`
- key id: `example_key`

Give yourself the example key:

```text
/bc givekey example <yourname> 5
```

Preview the crate:

```text
/bc preview example
```

Open it directly:

```text
/bc open example
```

## Creating Your Own Crates

### Option A: Use the editor

This is the easiest path.

1. Run:

```text
/bc editor
```

2. Open the crate manager.
3. Select a crate to edit.
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

### Option B: Copy the example config

Create a new `.yml` file inside:

```text
plugins/BloodlineCrates/crates/
```

The easiest method is:
1. copy `example.yml`
2. change the `id`
3. change the display name
4. replace the rewards
5. reload with:

```text
/bc reloadcrates
```

## Crate Types

### Random crate

Players open the crate and the plugin rolls one reward by chance.

Use when you want:
- classic RNG crates
- jackpot style crates
- weighted prize pools

### Selectable crate

Players open the crate and choose from the configured rewards.

Use when you want:
- battle pass style reward claiming
- rankup reward selectors
- guaranteed choice crates

## Key Modes

Each crate can only use one key mode:
- `PHYSICAL`
- `VIRTUAL`

You can change this in the crate editor.

### Physical keys

These are actual items in the player inventory.

Give them with:

```text
/bc givekey <crate_id> <player> [amount]
```

### Virtual keys

These are stored in player data instead of inventory.

Give them with:

```text
/bc givevirtualkey <crate_id> <player> [amount]
```

## Creating Rewards

Rewards are configured inside each crate.

Current supported reward flows include:
- item rewards
- command rewards
- economy rewards
- crate rewards
- multi rewards
- broadcast-aware rewards

### Good starter reward setup

For a basic item reward:
- put the item in the reward
- set a chance
- optionally set a display name
- optionally enable broadcast

### Good selectable crate setup

For selectable crates:
- keep reward list small and clear
- make every reward visually distinct
- use display names and descriptions

## Placing Crates In The World

### Place by command

Stand where you want the crate and use your placement flow:

```text
/bc setcrate <crate_id>
```

If your current placement workflow is item-based, use:

```text
/bc givecrate <crate_id> <player>
```

Then place the crate item in the world.

### Remove a placed crate

```text
/bc removecrate
```

### List placed crates

```text
/bc listcrates
```

### Teleport to a placed crate

```text
/bc teleportcrate <placement_id>
```

## Editing Existing Crates

Use the editor for most changes:

```text
/bc editor
```

Common edits:
- change physical/virtual key mode
- update reward chances
- change reward icons
- change pity threshold
- change cooldown
- change hologram text
- change layout rows and reward slots

After config-side edits from disk:

```text
/bc reloadcrates
```

That also resets crate cooldown state to the latest editor/config values.

## Testing Your Setup

Recommended test flow:

1. Give yourself keys

```text
/bc givekey <crate_id> <yourname> 10
```

2. Preview the crate

```text
/bc preview <crate_id>
```

3. Open the crate several times
4. Check:
- rewards are correct
- key consumption is correct
- cooldown works
- pity works
- broadcasts work
- stats update

## Stats

View your own stats:

```text
/bc stats
```

Open GUI stats:

```text
/bc stats gui
```

View another player's stats:

```text
/bc stats <player>
/bc stats gui <player>
```

Requires admin permission when targeting another player.

## Debugging

Trace all players:

```text
/bc debug on
```

Trace one player only:

```text
/bc debug on <player>
```

Disable debug:

```text
/bc debug off
```

Check debug status:

```text
/bc debug status
```

## Useful Admin Commands

```text
/bc reload
/bc reloadcrates
/bc editor
/bc preview <crate_id>
/bc open <crate_id>
/bc givekey <crate_id> <player> [amount]
/bc givevirtualkey <crate_id> <player> [amount]
/bc givecrate <crate_id> <player>
/bc setcrate <crate_id>
/bc removecrate
/bc listcrates
/bc teleportcrate <placement_id>
/bc limits ...
/bc stats [player]
/bc stats gui [player]
/bc debug on [player]
```

## Example Config

The included example crate is here:

[example.yml](D:/W4Whiskers/Development/Projects/Minecraft%20Mods/BloodlineCrates/src/main/resources/crates/example.yml)

Use it as your template if you want the fastest starting point.

## Suggested Workflow

If you’re building a real server setup, this is the smoothest order:

1. Create the crate config.
2. Decide whether it is `RANDOM` or `SELECTABLE`.
3. Decide whether it uses `PHYSICAL` or `VIRTUAL` keys.
4. Build rewards.
5. Test with `/bc preview`.
6. Give yourself keys.
7. Place the crate.
8. Test cooldown, pity, and broadcasts.
9. Check `/bc stats` after a few opens.
10. Use `/bc debug on <player>` if something feels off.

## Notes

- `reload` refreshes the plugin config and managers.
- `reloadcrates` refreshes crate files and applied crate cooldown definitions.
- Virtual keys are supported.
- Virtual crates were removed from the design; the crate itself is physical/placed, while the key mode can be virtual.
