package net.abakath.rpg4fools.server.events.compost;

import net.abakath.rpg4fools.init.ModCompostItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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

  private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    if (!(world instanceof ServerLevel serverWorld)) {
      return InteractionResult.PASS;
    }

    ItemStack stack = player.getItemInHand(hand);

    if (!(stack.getItem() instanceof AxeItem)) {
      return InteractionResult.PASS;
    }

    BlockPos pos = hit.getBlockPos();
    BlockState state = world.getBlockState(pos);

    Item bark = ModCompostItems.barkFor(state.getBlock());

    if (bark == null) {
      return InteractionResult.PASS;
    }

    // The strip that is about to happen is what pays for the bark, and a player who cannot change
    // the block is not about to strip anything.
    if (!player.mayBuild()) {
      return InteractionResult.PASS;
    }

    Block.popResource(serverWorld, pos, new ItemStack(bark));

    return InteractionResult.PASS;
  }
}
