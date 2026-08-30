package net.abakath.rpg4fools.mixin;

import net.abakath.rpg4fools.world.CropTransition;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Catches crops the season change could not reach.
 *
 * <p>SeasonChangeSweep settles everything loaded the moment a season turns. This is for the rest: a
 * farm nobody had loaded at the time converts on its first random tick instead, and out of season
 * growth is cancelled in the same breath.
 *
 * <p>Injected here rather than into each crop class because every growing block passes through this
 * one method, so modded crops obey the same rule as vanilla ones and there is a single target string
 * to get right instead of five.
 *
 * <p>This is the hottest block path in the game. CropTransition opens with a crops tag lookup, which
 * is a set membership test against the block's registry entry, and every block that is not a crop
 * pays only that.
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public class BlockStateRandomTickMixin {
  @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
  private void rpg4fools$settleOutOfSeasonCrops(ServerWorld world, BlockPos pos, Random random, CallbackInfo info) {
    BlockState state = (BlockState) (Object) this;

    // Cancelled only when the block was replaced. Whatever is standing there now is not the block
    // vanilla was about to grow.
    if (CropTransition.apply(world, pos, state, CurrentSeason.season())) {
      info.cancel();
    }
  }
}
