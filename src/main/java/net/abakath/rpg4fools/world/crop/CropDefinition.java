package net.abakath.rpg4fools.world.crop;

import net.abakath.rpg4fools.enums.Season;

import java.util.Set;

/**
 * One plant the mod adds, as data.
 *
 * <p>Blocks, items, season tags and the item lookup are all derived from this, so a crop cannot be
 * registered without a season or given an item its block does not know about. Adding a plant is one
 * entry in {@link net.abakath.rpg4fools.init.ModCrops}.
 *
 * <p>Two shapes, and they are different plants rather than one plant with a flag. A farmland crop is
 * sown, grows through eight ages, can be picked back to a lower one and dies when its season ends; a
 * bush is planted, grows through four, gives a berry that replants it and sits a winter out as a
 * dormant block. What they share is what is here: a name, the seasons they grow in, and what eating
 * them is worth.
 *
 * <p>Everything else belongs to one shape or the other, which is why this is sealed rather than one
 * record carrying every field. A bush has no trellis to climb and no age to be picked back to; a
 * farmland crop has no thorns and no dormant block. Held as one type those absences were arguments
 * every crop had to pass and every reader had to know to ignore - and the one pairing that was
 * actually dangerous, a bush claiming to survive winter, could only be caught by a check at
 * construction. Split, the compiler catches all of it: there is no field to pass wrongly.
 *
 * <p>Names are derived rather than stored. A crop that spelled its block one way and its seed
 * another would still compile, and the mistake would only show up as a missing texture.
 */
public sealed interface CropDefinition permits FarmlandCrop, BushCrop {
  /** The plant's own name, and the stem every other name here grows from. */
  String id();

  /** The seasons this plant grows in. Nothing grows in winter. */
  Set<Season> seasons();

  int nutrition();

  float saturation();

  /** The block this plant is, standing in the ground. */
  String blockName();

  /** What harvesting this plant gives: the crop itself, or a bush's berry. */
  String produceName();
}
