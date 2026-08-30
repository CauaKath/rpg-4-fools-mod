# New crops: farmland vegetables and berry bushes

Status: design, awaiting approval. Not yet implemented.

## Goal

Add six growable plants of the mod's own: tomato, cucumber and lettuce on
farmland, and strawberry, blackberry and blueberry as bushes. They behave the way
their vanilla counterparts do, and they take part in the season system the mod
already applies to vanilla crops.

Today the mod ships no items and no crops of its own. It only tags vanilla blocks
and swaps them for `dead_crop` or `dormant_sweet_berry_bush` when a season turns.
This is the first content the mod registers, so it also establishes where items,
food and creative-tab entries live.

## Scope

In scope:

- Nine blocks: three farmland crops, three live bushes, three dormant bushes.
- Nine items: three seed items, three produce items, three berry items.
- Season tags for every new plant, driven from the same table that registers them.
- A creative tab holding the new items. No other way to obtain them yet.
- Generalising the bush/dormant pairing that `CropTransition` currently hardcodes.

Out of scope, deliberately:

- Seed acquisition in survival (grass drops, villager trades, chest loot).
- Crafting or cooking recipes.
- Custom growth rates, regrowing harvests, per-crop yields.
- Final art. Placeholder sprites ship first; real art replaces them in place.

## Roster

| Plant      | Kind     | Seasons          | Produce eats as | Thorns |
|------------|----------|------------------|-----------------|--------|
| Tomato     | farmland | Summer           | 3 / 0.3         | -      |
| Cucumber   | farmland | Summer           | 2 / 0.2         | -      |
| Lettuce    | farmland | Spring, Autumn   | 2 / 0.3         | -      |
| Strawberry | bush     | Spring, Summer   | 2 / 0.2         | no     |
| Blackberry | bush     | Summer, Autumn   | 2 / 0.1         | yes    |
| Blueberry  | bush     | Summer           | 2 / 0.2         | no     |

Nutrition and saturation sit at carrot and sweet-berry level; nothing here is a
meal. Winter grows nothing, matching the assignments already in the tag provider.

Thorns are inherited from `SweetBerryBushBlock`, which damages and slows anything
walking through it. That reads right for a blackberry thicket and wrong for a
strawberry patch, so it is a per-plant flag rather than a blanket clone.

## Architecture

### One table, four consumers

`ModCrops` holds the roster as data: name, kind, seasons, food values, thorn flag.
Four things read it, and none of them keeps its own list:

- `ModBlocks` registers the blocks it describes.
- `ModItems` registers the seeds, produce and berries.
- `SeasonCropTagProvider` emits the crop and `grows_in_*` tags.
- `CropItems` maps produce items back to the block that grew them.

Adding a seventh crop is one table line plus its textures. The existing provider
already works this way for vanilla crops, so this extends a pattern rather than
introducing one.

### Blocks

`ModCropBlock extends CropBlock` and `ModBerryBushBlock extends SweetBerryBushBlock`
carry no per-crop fields. Both need a `MapCodec` built by `createCodec(Ctor::new)`,
which admits only a `Settings` argument, so a field holding the seed or berry item
would have nowhere to come from on deserialisation. Instead each block asks
`ModCrops` for its own entry by block identity, which keeps one source of truth and
one constructor shape.

`ModCropBlock` overrides `getSeedsItem` so pick-block hands back the right seed.
`ModBerryBushBlock` overrides `onUse` and `getPickStack` for its own berry, and
overrides `onEntityCollision` to a no-op when the entry is not thorny.

`DormantSweetBerryBushBlock` becomes `DormantBerryBushBlock`. Its behaviour is
already generic - every growth path disabled, ticking left on so the season hook
can revive it - and only its name and doc tie it to sweet berries. The registered
block id `rpg4fools:dormant_sweet_berry_bush` does not change, so no world breaks.

### Dormant pairing

`CropTransition` currently names `Blocks.SWEET_BERRY_BUSH` and its dormant form in
two `if` blocks. With four pairs that becomes `DormantBushes`, a bidirectional map
registering the vanilla pair alongside the three new ones. `CropTransition` asks it
"is this a live bush" and "is this a dormant bush" and swaps accordingly; the
revive-at-age-1 rule and the dead-crop fallback are unchanged.

This is the one existing file the feature has to reshape. Everything else in the
season path - `SeasonChangeSweep`, `BlockStateRandomTickMixin`, `SeasonPlantingGate`,
`CropSeasonTooltip` - already works off tags and needs no edit.

### Items

Seeds and berries are `AliasedBlockItem`, so they plant on right-click while
reading as an item rather than a block. Produce items are plain `Item` with a
`FoodComponent`. Berries are both: the thing you eat and the thing you plant, as
sweet berries are.

`CropItems.PRODUCE` gains the three produce items, since a tomato is not its crop's
`BlockItem`. Berries need no entry - being `AliasedBlockItem` they already resolve
through the `BlockItem` branch.

### Creative tab

One tab, `rpg4fools:crops`, icon a tomato, holding all nine items. It is the only
way to obtain them until survival acquisition lands.

### Assets

Hand-written JSON under `src/main/resources`, matching how the two existing blocks
are done:

- 3 crop blockstates with eight age variants, 24 stage models.
- 3 bush blockstates with four age variants, 12 stage models.
- 3 dormant blockstates and models, pointing at the existing dormant bush texture.
- 9 item models.
- 9 block loot tables. Mature crops drop produce and seeds; immature drop the seed
  back. Bushes drop berries scaled by age. Dormant bushes drop nothing, matching
  the dormant sweet berry bush already shipped.
- Lang entries for every block, item and the tab.

Datagen is not used for any of this. `runDatagen` needs a JDK that is not available
here and CI runs only `./gradlew build`, so a generated asset could never be
regenerated or verified. The season tags are the exception, because they are already
generated and committed; their provider gets its new entries and the five files
under `src/main/generated` are hand-synced to match, as this repo already requires.

### Placeholder art

About 45 sprites: eight stages per farmland crop, four per bush, one per dormant
bush, one per item. Generated by a throwaway script writing PNGs with `zlib` and
`struct` - no imaging library is installed here.

Bush blocks are the vanilla sweet berry bush, pixel for pixel, across all four
stages. Nothing about the skeleton changes; only the palette does. The texture uses
six greens and four reds, so the greens shift per plant - strawberry a little more
vibrant, blackberry darker and muted, blueberry cooler - and the reds become that
plant's berry ramp. A row of the three then reads as one plant grown in three
colours, which is what a bush patch should look like.

The three new dormant blocks reuse the existing texture as it stands. Their models
point at `rpg4fools:block/dormant_sweet_berry_bush`; no new dormant sprite is drawn
and no copy is committed. A dormant blackberry and a dormant sweet berry are the
same bare thicket, and four near-identical files would only be four things to keep
in sync.

The three farmland crops are drawn in the idiom Farmer's Delight uses for its own
crops: foliage in clumps rather than neat stalks, a five-tone green ramp, and fruit
carrying a dark rim with one bright highlight. The art is ours - Farmer's Delight
assets are not open - but the reading is the same, so the crops sit beside that mod
without looking foreign.

Each plant still keeps its own habit. Tomato threads clumps up a stem and hangs
fruit off it, cucumber creeps along the ground with a tendril and fruit lying flat,
and lettuce has no stem at all: a rounded head that packs tighter and wider each
stage, with outer leaves flaring at the base.

Their block models use a cross rather than `minecraft:block/crop`, which is the shape
this art is drawn for. Not vanilla's `block/cross` though: that spans y 0 to 16, and
farmland is only 15/16 tall, so a crop built on it hovers a pixel above the soil.
`rpg4fools:block/crop_cross` is the same cross dropped to y -1 to 15, which is
exactly why vanilla's own crop model uses that range.

Every stage is rooted: a few brown threads at the soil line, spreading as the plant
grows, drawn only into pixels the plant did not claim. Without them a crop reads as
resting on the farmland rather than growing out of it. The lettuce head sits two
pixels higher than it used to so there is somewhere for its roots to show.

Item sprites follow the same split. The three berry items are the vanilla sweet
berry item pixel for pixel: the same three-berry cluster, the same leaves, the same
shading, with only the six-tone red ramp swapped for the plant's own colour. Dark
fruit shades less and highlights more, or a blackberry ramp would collapse to black.

Strawberry is the exception. A strawberry is one conical seeded fruit under a
calyx, not a bunch, so its item is drawn rather than recoloured, and on the bush its
fruit hangs low and tapered instead of sitting in the canopy as clustered berries do.

That makes the twelve bush stages and the blackberry and blueberry berry items
recolours of Mojang textures. Fine while the art is scaffolding, not fine to publish:
those fourteen files have to be redrawn before any release. The dormant bushes are
not affected - they point at this repo's own texture.

Seeds get one shape
each - tomato pips, large upright cucumber seeds, thin lettuce chaff - because three
identical seed sprites in a row are unreadable in an inventory. Produce follows its
plant: a round tomato with a calyx, a long ridged cucumber, and lettuce as a single
ruffled leaf on a pale midrib rather than a head, since a leaf is what a player
pictures when they read the word.

Blackberry and blueberry take the sweet berry sprite's stems to brown. Those two
fruit on woody canes, and leaving the vanilla green there made them read as one more
sweet berry.

The script is a tool, not a deliverable; only the PNGs are committed, and real art
overwrites them without touching any model.

## Behaviour through the season system

A new crop is a crop like any other. It carries `#rpg4fools:crops`, so:

- Planting out of season is refused by `SeasonPlantingGate` with the existing
  message.
- Its seasons show in the tooltip on both the seed and the produce.
- When its season ends, a farmland crop becomes `dead_crop` and a bush becomes its
  dormant form, both through `CropTransition`, whether the chunk is loaded at the
  turn or reached later by random tick.
- When a bush's season returns, it comes back at age 1, fruitless, like the sweet
  berry bush does.

`CropTagValidator` stays quiet, because every new plant is tagged with at least one
season.

## Edge cases

- A dormant bush must keep its live form's seasons, or it reads as growing in all
  four and revives in winter. The table drives both, so they cannot drift.
- Age 7 loot conditions must name the mod's own crop block, not `minecraft:wheat`;
  a copied loot table with the wrong block silently drops nothing.
- Berry items plant bushes, so `CropItems.plantedBy` gates them. That is correct:
  planting a blackberry out of season should be refused like any other seed.
- `dead_crop` is deliberately outside the crops tag; nothing about it changes here.

## Testing

No JDK is available locally, so compilation is verified by pushing and reading the
`build (21)` CI job. Mixin targets and asset paths slip past that job, so they are
checked in game.

The repo has no test source set and no game-test harness. Adding one for six data
rows would cost more than it catches, so verification is CI plus a manual pass:

1. Every item appears in the tab with a sprite and a name, not a purple square or a
   translation key.
2. Each seed plants on farmland, grows through visible stages, and drops produce and
   seeds when mature.
3. Each bush plants, fruits, right-click harvests, and blackberry alone hurts to
   walk through.
4. Advancing to a season a plant does not grow in kills the crops and makes the
   bushes dormant; returning to its season revives the bushes fruitless.
5. Planting out of season is refused with the existing message.
6. Server log carries no `CropTagValidator` warning for the new plants.
