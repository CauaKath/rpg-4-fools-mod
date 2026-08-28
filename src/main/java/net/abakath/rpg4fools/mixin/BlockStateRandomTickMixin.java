package net.abakath.rpg4fools.mixin;

import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.world.CropSeasons;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Kills crops whose season has passed.
 *
 * <p>Injected here rather than into each crop class because every growing block passes through this
 * one method, so modded crops die on the same rule as vanilla ones and there is a single target
 * string to get right instead of five.
 *
 * <p>This is the hottest block path in the game. The first line is a tag lookup, which is a set
 * membership test against the block's registry entry, and every block that is not a crop pays only
 * that.
 *
 * <p>Death is deliberately lazy: a crop dies on the next random tick it would have grown on. Chunks
 * only random tick near players, so a distant farm converts when someone comes back to it. The
 * season change itself costs nothing and nothing walks the world looking for work.
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public class BlockStateRandomTickMixin {
  @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
  private void rpg4fools$killOutOfSeasonCrops(ServerWorld world, BlockPos pos, Random random, CallbackInfo info) {
    BlockState state = (BlockState) (Object) this;

    if (!CropSeasons.isCrop(state)) {
      return;
    }

    boolean inSeason = CropSeasons.isInSeason(state, CurrentSeason.season());

    // Bushes are the one crop that comes back, so they swap between two blocks instead of dying.
    if (state.isOf(Blocks.SWEET_BERRY_BUSH)) {
      if (!inSeason) {
        world.setBlockState(pos, ModBlocks.DORMANT_SWEET_BERRY_BUSH.getDefaultState());
        info.cancel();
      }

      return;
    }

    if (state.isOf(ModBlocks.DORMANT_SWEET_BERRY_BUSH)) {
      if (inSeason) {
        // Age 1 is leafy with no fruit, so the bush picks up where a fresh one would rather than
        // handing back the berries it lost going dormant.
        world.setBlockState(pos, Blocks.SWEET_BERRY_BUSH.getDefaultState().with(SweetBerryBushBlock.AGE, 1));
      }

      info.cancel();
      return;
    }

    if (inSeason) {
      return;
    }

    world.setBlockState(pos, ModBlocks.DEAD_CROP.getDefaultState());
    info.cancel();
  }
}
