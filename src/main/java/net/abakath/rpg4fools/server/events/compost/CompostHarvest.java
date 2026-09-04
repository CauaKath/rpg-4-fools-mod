package net.abakath.rpg4fools.server.events.compost;

import net.abakath.rpg4fools.world.crop.CropHarvest;
import net.abakath.rpg4fools.world.compost.Compost;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/**
 * What rich soil adds to a crop that is torn up.
 *
 * <p>One more roll of the crop's own loot table. Using the table rather than handing out a fixed
 * item is what lets this work for wheat, carrots, potatoes and beetroot as well as the mod's own
 * crops - compost feeds the soil, not a species - and it keeps tools and Fortune in charge of the
 * numbers, which a flat bonus would quietly override.
 *
 * <p>The extra roll is not filtered. Filtering seeds out of it was the first plan and it cannot
 * work: a carrot is its own seed, so the filter would have removed the entire bonus for carrots and
 * potatoes while leaving wheat untouched. Doubling everything the plant gives is both simpler and
 * the same rule for every crop.
 *
 * <p>Only the break is handled here. A crop that is picked rather than torn up never reaches this
 * event, and {@link net.abakath.rpg4fools.world.crop.CropHarvest} pays those out instead - which is also
 * why the two cannot double up on one harvest.
 */
public final class CompostHarvest {
  private CompostHarvest() {
  }

  public static void register() {
    PlayerBlockBreakEvents.AFTER.register(CompostHarvest::afterBreak);
  }

  private static void afterBreak(Level world, Player player, BlockPos pos, BlockState state,
                                 BlockEntity blockEntity) {
    if (!(world instanceof ServerLevel serverWorld) || player.isCreative()) {
      return;
    }

    // Only a ripe crop pays. An unripe one drops seeds and nothing else, and doubling that would
    // make breaking a young field a way to farm seeds off compost.
    if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
      return;
    }

    if (Compost.at(serverWorld, pos.below()) != Compost.RICH) {
      return;
    }

    ItemStack tool = player.getMainHandItem();
    List<ItemStack> extra = Block.getDrops(state, serverWorld, pos, blockEntity, player, tool);

    for (ItemStack stack : extra) {
      Block.popResource(serverWorld, pos, stack);
    }
  }
}
