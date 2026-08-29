package net.abakath.rpg4fools.world;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Where a right clicked harvest goes.
 *
 * <p>Straight into the inventory, which is the whole point of harvesting by hand: a player who
 * wanted items on the ground would have broken the plant. Two callers need the same answer - the
 * replanting hook for crops that are torn up, and the regrowing crop that is only picked - so it
 * lives here rather than in whichever one was written first.
 */
public class CropHarvest {
  public static void give(PlayerEntity player, List<ItemStack> stacks) {
    for (ItemStack stack : stacks) {
      give(player, stack);
    }
  }

  /** At the player's feet only when there is no room left. */
  public static void give(PlayerEntity player, ItemStack stack) {
    player.getInventory().insertStack(stack);

    if (!stack.isEmpty()) {
      player.dropItem(stack, false);
    }
  }
}
