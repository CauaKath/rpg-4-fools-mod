package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModCrops;
import net.abakath.rpg4fools.init.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves an item back to the crop it belongs to.
 *
 * <p>Seasons live on blocks, but the player holds items. Both halves of a crop should answer the
 * same question, so a seed and the thing it grows into both have to find their way back to one
 * block state.
 *
 * <p>Most of that is free: a seed is a BlockItem pointing at the crop it plants, so deriving the
 * block covers every vanilla seed and any modded seed this mod has never heard of. The map below
 * only exists for produce that is not its crop's BlockItem - wheat and beetroot are plain items,
 * and melon and pumpkin are BlockItems of the fruit rather than of the stem that grew it.
 */
public class CropItems {
  private static final Map<Item, Block> PRODUCE = createProduce();

  /**
   * The crop this item would plant, or empty if placing it does not plant a crop.
   *
   * <p>Narrower than {@link #cropFor} on purpose. That one maps produce as well, and produce is not
   * a seed: Items.PUMPKIN maps to the stem that grew it, so gating placement on it would refuse a
   * pumpkin block as though it were a crop being sown.
   */
  public static Optional<BlockState> plantedBy(ItemStack stack) {
    if (!(stack.getItem() instanceof BlockItem blockItem)) {
      return Optional.empty();
    }

    BlockState planted = blockItem.getBlock().defaultBlockState();

    return CropSeasons.isCrop(planted) ? Optional.of(planted) : Optional.empty();
  }

  /** The crop this item belongs to, or empty if the item has nothing to do with farming. */
  public static Optional<BlockState> cropFor(ItemStack stack) {
    Item item = stack.getItem();

    // Checked before the map so a modded seed resolves without needing an entry. The crops tag is
    // what decides; a BlockItem for some unrelated block falls through.
    if (item instanceof BlockItem blockItem) {
      BlockState planted = blockItem.getBlock().defaultBlockState();

      if (CropSeasons.isCrop(planted)) {
        return Optional.of(planted);
      }
    }

    Block grownBy = PRODUCE.get(item);
    if (grownBy == null) {
      return Optional.empty();
    }

    BlockState crop = grownBy.defaultBlockState();

    // The map is written against vanilla, but a datapack can drop a crop out of the tag. Honour
    // that instead of reporting seasons for something no longer treated as a crop.
    return CropSeasons.isCrop(crop) ? Optional.of(crop) : Optional.empty();
  }

  private static Map<Item, Block> createProduce() {
    Map<Item, Block> produce = new LinkedHashMap<>();

    produce.put(Items.WHEAT, Blocks.WHEAT);
    produce.put(Items.BEETROOT, Blocks.BEETROOTS);

    // Stems, not the fruit. Blocks.MELON and Blocks.PUMPKIN are the harvest, and neither one grows
    // on its own, so the seasons that matter are the stem's.
    produce.put(Items.MELON, Blocks.MELON_STEM);
    produce.put(Items.MELON_SLICE, Blocks.MELON_STEM);
    produce.put(Items.PUMPKIN, Blocks.PUMPKIN_STEM);

    // Grown flowers, whose block items place the finished plant rather than the crop stage.
    produce.put(Items.TORCHFLOWER, Blocks.TORCHFLOWER_CROP);
    produce.put(Items.PITCHER_PLANT, Blocks.PITCHER_CROP);

    // The mod's own produce. A tomato is not its crop's BlockItem, so it needs the same treatment
    // wheat and beetroot get. Bush berries are left out on purpose: they are BlockItems already.
    for (CropDefinition definition : ModCrops.ALL) {
      if (definition.kind() == CropDefinition.Kind.FARMLAND) {
        produce.put(ModItems.produceItem(definition), ModBlocks.blockFor(definition));
      }
    }

    return produce;
  }
}
