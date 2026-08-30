package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.CropDefinition;
import net.abakath.rpg4fools.world.DeadCropBlock;
import net.abakath.rpg4fools.world.DormantBerryBushBlock;
import net.abakath.rpg4fools.world.ModBerryBushBlock;
import net.abakath.rpg4fools.world.ModCropBlock;
import net.abakath.rpg4fools.world.RegrowingCropBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blocks the mod adds.
 *
 * <p>Two kinds live here. The season blocks below get no BlockItem: they are not things a player
 * collects or places, and the season hook is the only thing that puts them in the world. The blocks
 * built from {@link ModCrops} are the opposite, and their items are registered in {@link ModItems}.
 */
public class ModBlocks {
  public static final Block DEAD_CROP = register("dead_crop", new DeadCropBlock(
          AbstractBlock.Settings.create()
                  // Ticks so a village farm nobody had loaded when spring came can still be sown
                  // again on its first tick, the way an out of season crop is settled late.
                  .ticksRandomly()
                  .breakInstantly()
                  .noCollision()
                  // No loot table of its own. A dead crop is a loss, not a harvest, and saying so
                  // here avoids shipping an empty loot table file.
                  .dropsNothing()
                  .sounds(BlockSoundGroup.CROP)
                  .pistonBehavior(PistonBehavior.DESTROY)
  ));

  /**
   * The off season form of a sweet berry bush. Sits in the crops tag, unlike DEAD_CROP, because the
   * season hook has to keep looking at it: that hook is the only thing that can revive it.
   */
  public static final Block DORMANT_SWEET_BERRY_BUSH = register("dormant_sweet_berry_bush", new DormantBerryBushBlock(
          AbstractBlock.Settings.create()
                  // Still ticks. Revival is decided at the head of the random tick, so a block that
                  // stopped ticking would never come back.
                  .ticksRandomly()
                  .noCollision()
                  .dropsNothing()
                  .sounds(BlockSoundGroup.SWEET_BERRY_BUSH)
                  .pistonBehavior(PistonBehavior.DESTROY)
  ));

  private static final Map<CropDefinition, Block> LIVE = new LinkedHashMap<>();
  private static final Map<CropDefinition, Block> DORMANT = new LinkedHashMap<>();
  private static final Map<Block, CropDefinition> BY_BLOCK = new HashMap<>();

  static {
    for (CropDefinition definition : ModCrops.ALL) {
      if (definition.kind() == CropDefinition.Kind.FARMLAND) {
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                .ticksRandomly()
                .noCollision()
                .breakInstantly()
                .sounds(BlockSoundGroup.CROP)
                .pistonBehavior(PistonBehavior.DESTROY);

        // Same crop in every way a farmland crop is asked about; the roster only decides whether
        // picking it leaves the plant standing.
        register(definition, LIVE, definition.blockName(),
                definition.regrows() ? new RegrowingCropBlock(settings) : new ModCropBlock(settings));
        continue;
      }

      register(definition, LIVE, definition.blockName(), new ModBerryBushBlock(
              AbstractBlock.Settings.create()
                      .ticksRandomly()
                      .noCollision()
                      .sounds(BlockSoundGroup.SWEET_BERRY_BUSH)
                      .pistonBehavior(PistonBehavior.DESTROY)
      ));

      // Dormant bushes drop nothing, matching the dormant sweet berry bush this mod already ships.
      register(definition, DORMANT, definition.dormantBlockName(), new DormantBerryBushBlock(
              AbstractBlock.Settings.create()
                      .ticksRandomly()
                      .noCollision()
                      .dropsNothing()
                      .sounds(BlockSoundGroup.SWEET_BERRY_BUSH)
                      .pistonBehavior(PistonBehavior.DESTROY)
      ));
    }
  }

  public static Block blockFor(CropDefinition definition) {
    return LIVE.get(definition);
  }

  public static Block dormantFor(CropDefinition definition) {
    return DORMANT.get(definition);
  }

  public static CropDefinition definitionFor(Block block) {
    return BY_BLOCK.get(block);
  }

  /**
   * Whether walking through this bush hurts.
   *
   * <p>Defaults to true for a block with no roster entry, which means the dormant sweet berry bush:
   * it stands in for a vanilla bush, and vanilla brambles have thorns.
   */
  public static boolean isThorny(Block block) {
    CropDefinition definition = BY_BLOCK.get(block);
    return definition == null || definition.thorny();
  }

  /** Every block the roster registered, each live bush followed by its dormant form. */
  public static List<Block> cropBlocks() {
    List<Block> blocks = new ArrayList<>();

    for (CropDefinition definition : ModCrops.ALL) {
      blocks.add(LIVE.get(definition));

      if (DORMANT.containsKey(definition)) {
        blocks.add(DORMANT.get(definition));
      }
    }

    return blocks;
  }

  private static void register(CropDefinition definition, Map<CropDefinition, Block> into, String name, Block block) {
    Block registered = register(name, block);

    into.put(definition, registered);
    BY_BLOCK.put(registered, definition);
  }

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
