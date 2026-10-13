# Difficulty+

A [Dungeon Difficulty](https://www.curseforge.com/minecraft/mc-mods/dungeon-difficulty) addon for
1.21.1 (Fabric + NeoForge) that adds per-structure level randomization, damage-type resistances,
zone effects, and an in-game editor for the whole config.

Everything is configured **inside Dungeon Difficulty's own `difficulty_v2.json`**, as a
`difficultyplus` block on the object it applies to. There is no second config file.

You do not need to add these blocks by hand. Difficulty+ extends DD's config schema, so every
`difficultyplus` block appears in the file automatically the first time the game loads with the mod
installed — empty, ready to edit.

## In-game editor

The whole config — Dungeon Difficulty's fields as well as ours — is editable in game, with no file
editing and no restart:

- **Fabric** — Mod Menu → Difficulty+ → config button
- **NeoForge** — Mods list → Difficulty+ → Config

Requires [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config). Without it the
mod loads normally and the button is simply absent.

Lists can be added to, removed from and reordered, so new zones, difficulty types, entity entries
and attribute modifiers can all be created in game. Saving writes `difficulty_v2.json` and reloads
Dungeon Difficulty, exactly as `/dungeon_difficulty_config_reload` does.

> Levels already applied to spawned mobs do not change retroactively — Dungeon Difficulty marks a
> scaled entity and skips it thereafter. Edits affect mobs that spawn afterwards.

## Config reference

### Global — on the root config object

```json
"difficultyplus": {
  "zone_poll_interval": 20,
  "resistance_reduction_cap": 0.8
}
```

| Field | Meaning |
|---|---|
| `zone_poll_interval` | Ticks between zone re-evaluations for a given entity. Checks are staggered across entities, so this is a per-entity period, not a global sweep. Status effects refresh at twice this value so they do not flicker between polls. |
| `resistance_reduction_cap` | Ceiling on stacked percentage resistance, so config cannot produce full immunity. Does **not** bound vulnerabilities. |
| `biome_fill_budget` | Biome patch flood fills allowed per tick, per dimension. A dimension can override it (below). |
| `reward_curve.scale` | Multiplies the level used for loot attribute values (`loot_scaling`). `0.5` makes a level 60 chest roll like level 30. Default `1.0`. |
| `reward_curve.ceiling` | Optional cap on that level, so loot stops growing past it. The item still records its real level. |
| `level_gap.mode` | How the listed Puffish categories combine into the player's level: `SUM` (default), `HIGHEST` or `AVERAGE`. |
| `level_gap.weights` | Optional `[{ "category": "ns:id", "weight": 0.5 }]`. A category's level is multiplied by its weight; `0` ignores it. Unlisted categories weigh `1.0`. |

On a dimension, `"difficultyplus": { "biome_fill_budget": 4 }` gives that dimension its own budget.

### Level ranges — on a zone, or on a per-entity entry

Randomizes a level per structure instance (on a zone) or per mob (on an entity entry).

```json
"zone_matches": { "structure": "#dungeon_difficulty:level_1" },
"difficulty": { "name": "dungeon", "level": 1 },
"difficultyplus": {
  "level_range": {
    "mode": "RELATIVE",
    "min": -1, "max": 1,
    "floor": 1, "ceiling": 6,
    "lock_on_generation": false
  }
}
```

| Field | Meaning |
|---|---|
| `mode` | `RELATIVE` (default) reads `min`/`max` as offsets from this object's own `difficulty.level`. `ABSOLUTE` reads them as literal levels, ignoring it. |
| `min` / `max` | The window. Equal values mean no randomization. |
| `floor` / `ceiling` | Optional absolute bounds applied after the offset. Omit for unbounded. |
| `lock_on_generation` | `true` freezes the rolled level into the structure's chunk NBT at generation; `false` (default) recomputes it on every read, so range edits re-level existing structures. |
| `bias_scale` | Optional. Socketed bias points become a share of the window instead of flat levels: each point shifts the window by `bias_scale × (max − min)`, at least 1. Omit to keep flat biases. |

`RELATIVE` is the default because one range block can then be shared across every tier of a pack:
`-1…+1` with `floor: 1, ceiling: 6` gives `1–2` on a level 1 zone and `5–6` on a level 6 zone,
without rewriting it per tier.

Rolls are deterministic — derived from the world seed plus the structure's position and id (or the
mob's UUID) — so they survive restarts without being stored.

**On a zone**, every mob in that structure shares the rolled level. **On an entity entry**, each mob
rolls individually, which also works outside any structure.

The rolled level replaces `difficulty.level`, and `entity_level`/`reward_level` follow it unless
explicitly pinned — the same rule Dungeon Difficulty uses. A zone written as
`"level": 1, "entity_level": 2` keeps its +1 offset as the roll moves the base.

#### Commands

```
/difficultyplus level query      # level, source (hash|locked|set|reroll), structure id
/difficultyplus level set <n>    # pin the structure you are standing in
/difficultyplus level reroll     # new roll for that structure
/difficultyplus level clear      # drop the override, back to the base roll
```

### Zone label — on a difficulty type

```json
"difficultyplus": { "label": "difficultyplus.label.safe_zone" }
```

A label (a translation key, or plain text) shown before the type name in the zone announcement
subtitle and above the zone name in map tooltips. The zone keeps its real level.
Difficulty+ ships `difficultyplus.label.safe_zone` ("Safe Zone") in its own lang files.

### Resistances — on a difficulty type

Keyed on damage types, so spell schools and vanilla damage use one mechanism. No dependency on
Spell Power: `spell_power:fire` is a datapack damage type like any other.

```json
"difficultyplus": {
  "mob_resistances": [
    { "damage_type": "spell_power:fire", "percent": 0.4 },
    { "damage_tag": "#minecraft:is_explosion", "percent": 0.35,
      "targets": { "type": "#minecraft:undead" } },
    { "damage_type": "spell_power:frost", "percent": -0.5,
      "targets": { "type": "~minecraft:(blaze|magma_cube)" } }
  ],
  "player_resistances": [
    { "damage_type": "spell_power:fire", "percent": -0.25 }
  ]
}
```

| Field | Meaning |
|---|---|
| `damage_type` | Exact damage type id. |
| `damage_tag` | Damage type tag, with or without a leading `#`. |
| `percent` | Fractional reduction. **Negative means vulnerability** — `-0.5` is 50% more damage taken. |
| `flat` | Flat reduction, subtracted before `percent`. |
| `targets` | Optional narrowing filter, same vocabulary as `zone_effects` below. Omitted means every entity the list covers. |

Mobs and players have separate lists because they are usually tuned in opposite directions — a zone
that armours its mobs against fire often wants players to take more of it.

Percentages combine multiplicatively (two 50% sources leave 25% through, never 0%).
`resistance_reduction_cap` bounds reduction; vulnerabilities are deliberately uncapped.

### Zone effects — on a difficulty type

Applied to entities inside the zone, and extensible by other mods.

```json
"difficultyplus": {
  "zone_effects": [
    {
      "type": "difficultyplus:status_effect",
      "targets": { "attitude": "!HOSTILE" },
      "effect": "minecraft:regeneration",
      "duration": 100,
      "linger": false
    }
  ]
}
```

| Field | Meaning |
|---|---|
| `type` | Registered effect id. Built-ins: `difficultyplus:attribute`, `difficultyplus:status_effect`, `difficultyplus:damage_over_time`. |
| `targets` | Which entities receive it. **Omitted means players only.** |
| `duration` | Ticks. For status effects this is the lingering tail. |
| `interval` | Ticks between periodic work; `0` disables ticking. |
| `linger` | Whether the effect outlives the zone. `false` ends it promptly on exit; `true` lets it drain afterwards. |

Remaining keys are read by whichever effect `type` names, so addon effects define their own shape.

#### Target filters

`targets` reuses Dungeon Difficulty's own matcher vocabulary, so the syntax is the same as
`entity_matches` elsewhere in the file:

```json
"targets": { "attitude": "HOSTILE", "type": "~minecraft:(zombie|skeleton)" }
"targets": { "type": "#minecraft:undead" }
"targets": { "type": "!minecraft:player" }
```

| Prefix | Meaning |
|---|---|
| *(none)* | Exact id |
| `#` | Tag |
| `~` | Regex |
| `!` | Negation |
| `*` | Any |

`attitude` is `HOSTILE`, `FRIENDLY` or `ANY`.

## Addon API

Difficulty+ exposes its own extension points, under
`net.pixeldreamstudios.difficultyplus.api`:

```java
// Register a zone effect type; your factory owns its config shape.
DifficultyPlusAPI.zoneEffects().register(id, MyEffect::fromJson);

// Decide levels yourself; the first resolver to answer wins.
DifficultyPlusAPI.registerLevelResolver(resolver);

// Grant resistance from equipment, progression, anywhere.
DifficultyPlusAPI.registerResistanceProvider(provider);

// Queries
DifficultyPlusAPI.levelAt(level, pos);
DifficultyPlusAPI.zoneAt(level, pos);
DifficultyPlusAPI.resistancesOf(entity);
```

Events are available for `LEVEL_RESOLVED` and `DAMAGE_RESISTED`.

## Compatibility

| Mod | Role |
|---|---|
| Dungeon Difficulty 3.6+ | **Required** |
| Cloth Config | Optional — the in-game editor |
| Mod Menu | Optional — Fabric entry point for the editor |
| Jade + [DDxJade](https://github.com/) | Optional — shows zone, level and resistances on the tooltip |

Difficulty+ has **no dependency on Spell Power**. Its schools are ordinary datapack damage types, so
they are configurable here whether or not it is installed, as is any school added by another mod.
