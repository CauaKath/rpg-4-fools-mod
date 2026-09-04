package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModCrops;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.HashMap;
import java.util.Map;

/**
 * Which bush becomes which when the season turns.
 *
 * <p>Bushes are the one crop that comes back, so they swap between two blocks instead of dying. That
 * used to be two branches naming the sweet berry bush directly; with four pairs it is a map, and the
 * vanilla pair is registered here alongside the mod's own.
 */
public final class DormantBushes {
  private static final Map<Block, Block> TO_DORMANT = new HashMap<>();
  private static final Map<Block, Block> TO_LIVE = new HashMap<>();

  static {
    pair(Blocks.SWEET_BERRY_BUSH, ModBlocks.DORMANT_SWEET_BERRY_BUSH);

    for (CropDefinition definition : ModCrops.ALL) {
      if (definition.kind() == CropDefinition.Kind.BUSH) {
        pair(ModBlocks.blockFor(definition), ModBlocks.dormantFor(definition));
      }
    }
  }

  private DormantBushes() {
  }

  /** The dormant form of this bush, or null if this block is not a live bush. */
  public static Block dormantOf(Block bush) {
    return TO_DORMANT.get(bush);
  }

  /** The live form of this dormant bush, or null if this block is not a dormant bush. */
  public static Block liveOf(Block dormant) {
    return TO_LIVE.get(dormant);
  }

  private static void pair(Block live, Block dormant) {
    TO_DORMANT.put(live, dormant);
    TO_LIVE.put(dormant, live);
  }
}
