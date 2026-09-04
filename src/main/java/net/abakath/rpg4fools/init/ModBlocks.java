package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.CropDefinition;
import net.abakath.rpg4fools.world.CropStickBlock;
import net.abakath.rpg4fools.world.CropWallBlock;
import net.abakath.rpg4fools.world.DeadCropBlock;
import net.abakath.rpg4fools.world.DormantBerryBushBlock;
import net.abakath.rpg4fools.world.ModBerryBushBlock;
import net.abakath.rpg4fools.world.ModCropBlock;
import net.abakath.rpg4fools.world.RegrowingCropBlock;
import net.abakath.rpg4fools.world.StickedCropBlock;
import net.abakath.rpg4fools.world.WalledCropBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Blocks the mod adds.
 *
 * <p>Two kinds live here. The season blocks below get no BlockItem: they are not things a player
 * collects or places, and the season hook is the only thing that puts them in the world. The blocks
 * built from {@link ModCrops} are the opposite, and their items are registered in {@link ModItems}.
 */
public class ModBlocks {
  public static final Block DEAD_CROP = register("dead_crop", DeadCropBlock::new,
          BlockBehaviour.Properties.of()
                  // Ticks so a village farm nobody had loaded when spring came can still be sown
                  // again on its first tick, the way an out of season crop is settled late.
                  .randomTicks()
                  .instabreak()
                  .noCollision()
                  // No loot table of its own. A dead crop is a loss, not a harvest, and saying so
                  // here avoids shipping an empty loot table file.
                  .noLootTable()
                  .sound(SoundType.CROP)
                  .pushReaction(PushReaction.DESTROY)
  );

  /**
   * The off season form of a sweet berry bush. Sits in the crops tag, unlike DEAD_CROP, because the
   * season hook has to keep looking at it: that hook is the only thing that can revive it.
   */
  public static final Block DORMANT_SWEET_BERRY_BUSH = register("dormant_sweet_berry_bush", DormantBerryBushBlock::new,
          BlockBehaviour.Properties.of()
                  // Still ticks. Revival is decided at the head of the random tick, so a block that
                  // stopped ticking would never come back.
                  .randomTicks()
                  .noCollision()
                  .noLootTable()
                  .sound(SoundType.SWEET_BERRY_BUSH)
                  .pushReaction(PushReaction.DESTROY)
  );

  /**
   * A trellis with nothing on it. Gets a BlockItem, unlike the two blocks above: this one is a thing
   * the player crafts, carries and places, which is the whole point of it.
   *
   * <p>Deliberately outside the crops tag. It is not a crop, nothing about it grows, and the season
   * hook has no business looking at it - an empty stick is what a sticked crop becomes when its
   * season ends, not something a season can do anything to.
   */
  public static final Block CROP_STICK = register("crop_stick", CropStickBlock::new,
          BlockBehaviour.Properties.of()
                  .noCollision()
                  .instabreak()
                  .noOcclusion()
                  .sound(SoundType.WOOD)
                  .pushReaction(PushReaction.DESTROY)
  );

  /**
   * A trellis panel. Gets a BlockItem for the same reason the stick does, and like the stick it is
   * kept out of the crops tag.
   *
   * <p>Asks nothing of the world it is placed in - no support, no farmland, no size limit - because
   * half of what it is for is being scenery. See {@link CropWallBlock}.
   */
  public static final Block CROP_WALL = register("crop_wall", CropWallBlock::new,
          BlockBehaviour.Properties.of()
                  .noCollision()
                  .instabreak()
                  .noOcclusion()
                  .sound(SoundType.WOOD)
                  .pushReaction(PushReaction.DESTROY)
  );

  private static final Map<CropDefinition, Block> LIVE = new LinkedHashMap<>();
  private static final Map<CropDefinition, Block> DORMANT = new LinkedHashMap<>();
  private static final Map<CropDefinition, Block> STICKED = new LinkedHashMap<>();
  private static final Map<CropDefinition, Block> WALLED = new LinkedHashMap<>();
  private static final Map<Block, CropDefinition> BY_BLOCK = new HashMap<>();

  static {
    for (CropDefinition definition : ModCrops.ALL) {
      if (definition.kind() == CropDefinition.Kind.FARMLAND) {
        BlockBehaviour.Properties settings = BlockBehaviour.Properties.of()
                .randomTicks()
                .noCollision()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);

        // Same crop in every way a farmland crop is asked about; the roster only decides whether
        // picking it leaves the plant standing.
        register(definition, LIVE, definition.blockName(),
                definition.regrows() ? RegrowingCropBlock::new : ModCropBlock::new, settings);

        // The same crop again, as it grows on a trellis. A second block rather than a property on
        // the first: which one the player is looking at decides the models, the loot and whether a
        // season leaves sticks behind, and none of that is a state the plain crop should carry.
        if (definition.sticked()) {
          register(definition, STICKED, definition.stickedBlockName(), StickedCropBlock::new,
                  BlockBehaviour.Properties.of()
                          .randomTicks()
                          .noCollision()
                          .instabreak()
                          .sound(SoundType.CROP)
                          .pushReaction(PushReaction.DESTROY)
          );
        }

        // And again, as it spreads over a wall. A separate block for the same reasons the sticked
        // form is one, plus a rule the sticked form has no need of: a cell of this block turns back
        // into the panel it grew on, rather than dropping, when it loses its root.
        if (definition.walled()) {
          register(definition, WALLED, definition.walledBlockName(), WalledCropBlock::new,
                  BlockBehaviour.Properties.of()
                          .randomTicks()
                          .noCollision()
                          .instabreak()
                          .sound(SoundType.CROP)
                          .pushReaction(PushReaction.DESTROY)
          );
        }

        continue;
      }

      register(definition, LIVE, definition.blockName(), ModBerryBushBlock::new,
              BlockBehaviour.Properties.of()
                      .randomTicks()
                      .noCollision()
                      .sound(SoundType.SWEET_BERRY_BUSH)
                      .pushReaction(PushReaction.DESTROY)
      );

      // Dormant bushes drop nothing, matching the dormant sweet berry bush this mod already ships.
      register(definition, DORMANT, definition.dormantBlockName(), DormantBerryBushBlock::new,
              BlockBehaviour.Properties.of()
                      .randomTicks()
                      .noCollision()
                      .noLootTable()
                      .sound(SoundType.SWEET_BERRY_BUSH)
                      .pushReaction(PushReaction.DESTROY)
      );
    }
  }

  public static Block blockFor(CropDefinition definition) {
    return LIVE.get(definition);
  }

  public static Block dormantFor(CropDefinition definition) {
    return DORMANT.get(definition);
  }

  /** This crop as it grows on a trellis, or null for a crop no sticks were made for. */
  public static Block stickedFor(CropDefinition definition) {
    return STICKED.get(definition);
  }

  /** This crop as it grows on a wall, or null for a crop no wall art was made for. */
  public static Block walledFor(CropDefinition definition) {
    return WALLED.get(definition);
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

  /**
   * Every block the roster registered: each crop, followed by its dormant, sticked or walled form
   * where it has one.
   */
  public static List<Block> cropBlocks() {
    List<Block> blocks = new ArrayList<>();

    for (CropDefinition definition : ModCrops.ALL) {
      blocks.add(LIVE.get(definition));

      if (DORMANT.containsKey(definition)) {
        blocks.add(DORMANT.get(definition));
      }

      if (STICKED.containsKey(definition)) {
        blocks.add(STICKED.get(definition));
      }

      if (WALLED.containsKey(definition)) {
        blocks.add(WALLED.get(definition));
      }
    }

    return blocks;
  }

  private static void register(CropDefinition definition, Map<CropDefinition, Block> into, String name,
                              Function<BlockBehaviour.Properties, Block> factory,
                              BlockBehaviour.Properties settings) {
    Block registered = register(name, factory, settings);

    into.put(definition, registered);
    BY_BLOCK.put(registered, definition);
  }

  /**
   * A block has to be told its own registry key before it is constructed, so the key is built here
   * and the block is made from a factory rather than handed over already built.
   */
  private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
                                BlockBehaviour.Properties settings) {
    ResourceKey<Block> key =
            ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, name));

    return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(settings.setId(key)));
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
