package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.DeadCropBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Blocks the mod adds.
 *
 * <p>None of them get a BlockItem. They are not things a player collects or places; the season hook
 * is the only thing that puts them in the world.
 */
public class ModBlocks {
  public static final Block DEAD_CROP = register("dead_crop", new DeadCropBlock(
          AbstractBlock.Settings.create()
                  .breakInstantly()
                  .noCollision()
                  // No loot table of its own. A dead crop is a loss, not a harvest, and saying so
                  // here avoids shipping an empty loot table file.
                  .dropsNothing()
                  .sounds(BlockSoundGroup.CROP)
                  .pistonBehavior(PistonBehavior.DESTROY)
  ));

  private static Block register(String name, Block block) {
    return Registry.register(Registries.BLOCK, new Identifier(RPG4Fools.MOD_ID, name), block);
  }

  /**
   * Forces this class to load, which is what actually performs the registrations above.
   *
   * <p>Called from the mod initialiser. Without it the static fields would be initialised at some
   * arbitrary first use, which for a block means after the registries have already frozen.
   */
  public static void registerBlocks() {
  }
}
