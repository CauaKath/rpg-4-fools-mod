package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.init.ModCompostItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

/**
 * Bark, off the log an axe was about to strip anyway.
 *
 * <p>Passes rather than handles. The strip is vanilla's and stays vanilla's - this only catches the
 * moment before it and keeps what the plane would have thrown away, so nothing about stripping
 * changes except that the player now has something to show for it.
 *
 * <p>Which bark comes off is looked up per log rather than being one brown item for every tree. The
 * roster in {@link ModCompostItems} is also what decides whether a block has bark at all, so a log
 * cannot be given a drop it has no item for - and the three strippable blocks that are not logs
 * (bamboo, and the two fungus stems) fall out of it by simply not being listed.
 */
public final class BarkStripping {
  private BarkStripping() {
  }

  public static void register() {
    UseBlockCallback.EVENT.register(BarkStripping::onUseBlock);
  }

  private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    if (!(world instanceof ServerWorld serverWorld)) {
      return ActionResult.PASS;
    }

    ItemStack stack = player.getStackInHand(hand);

    if (!(stack.getItem() instanceof AxeItem)) {
      return ActionResult.PASS;
    }

    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);

    Item bark = ModCompostItems.barkFor(state.getBlock());

    if (bark == null) {
      return ActionResult.PASS;
    }

    // The strip that is about to happen is what pays for the bark, and a player who cannot change
    // the block is not about to strip anything.
    if (!player.canModifyBlocks()) {
      return ActionResult.PASS;
    }

    Block.dropStack(serverWorld, pos, new ItemStack(bark));

    return ActionResult.PASS;
  }
}
