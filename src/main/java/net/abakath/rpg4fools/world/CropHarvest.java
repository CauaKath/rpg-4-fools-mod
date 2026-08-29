package net.abakath.rpg4fools.world;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Where a right clicked harvest goes.
 *
 * <p>On the ground at the plant's feet, exactly as breaking it would leave things. Harvesting by
 * hand is meant to save the replanting, not to change what a harvest is, and a full inventory
 * quietly swallowing the difference is the kind of thing that only shows up as missing crops.
 *
 * <p>Two callers need the same answer - the replanting hook for crops that are torn up, and the
 * regrowing crop that is only picked - so it lives here rather than in whichever one was written
 * first.
 */
public class CropHarvest {
  public static void drop(World world, BlockPos pos, List<ItemStack> stacks) {
    for (ItemStack stack : stacks) {
      drop(world, pos, stack);
    }
  }

  public static void drop(World world, BlockPos pos, ItemStack stack) {
    Block.dropStack(world, pos, stack);
  }
}
