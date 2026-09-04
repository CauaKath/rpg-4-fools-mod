package net.abakath.rpg4fools.world.crop;

import net.abakath.rpg4fools.world.compost.Compost;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Where a right clicked harvest goes, and what rich soil adds to it.
 *
 * <p>On the ground at the plant's feet, exactly as breaking it would leave things. Harvesting by
 * hand is meant to save the replanting, not to change what a harvest is, and a full inventory
 * quietly swallowing the difference is the kind of thing that only shows up as missing crops.
 *
 * <p>Every caller passes the soil the plant is rooted in as well as the spot the drops land on,
 * because for a plant on a trellis or a wall those are not the same block: the drops belong at the
 * plant's feet, and the compost that paid for them is under the root. A caller that could work the
 * soil out for itself would be a caller that can get it wrong.
 *
 * <p>Three callers need the same answer - the replanting hook for crops that are torn up, the
 * regrowing crop that is only picked, and the two supported forms of it - so it lives here rather
 * than in whichever one was written first.
 */
public class CropHarvest {
  public static void drop(Level world, BlockPos pos, BlockPos soil, List<ItemStack> stacks) {
    for (ItemStack stack : stacks) {
      drop(world, pos, soil, stack);
    }
  }

  public static void drop(Level world, BlockPos pos, BlockPos soil, ItemStack stack) {
    Block.popResource(world, pos, enrich(world, soil, stack));
  }

  /**
   * Doubles a stack picked off rich soil.
   *
   * <p>The same size of bonus a crop that is torn up gets, which is one more roll of its loot table.
   * Matching them is what stops the two ways of harvesting from paying differently and turning one
   * of them into the wrong answer.
   */
  private static ItemStack enrich(Level world, BlockPos soil, ItemStack stack) {
    if (Compost.at(world, soil) != Compost.RICH) {
      return stack;
    }

    stack.setCount(stack.getCount() * 2);

    return stack;
  }
}
