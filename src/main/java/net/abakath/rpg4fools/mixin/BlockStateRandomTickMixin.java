package net.abakath.rpg4fools.mixin;

import net.abakath.rpg4fools.server.events.season.SeasonChangeSweep;
import net.abakath.rpg4fools.world.compost.CompostGrowth;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.season.CropTransition;
import net.abakath.rpg4fools.world.season.CurrentSeason;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
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
 * <p>Crops that survive a winter are cancelled here before anything else runs. Not dying is only
 * half of standing through a winter; the other half is not growing, and this is the method that
 * would otherwise have grown them.
 *
 * <p>Warm compost rides here too, for the same reason: one method every growing block passes
 * through means vanilla wheat and this mod's crops are hurried by the same rule, and there is one
 * target string to get right rather than five.
 *
 * <p>This is the hottest block path in the game. CropTransition opens with a crops tag lookup, which
 * is a set membership test against the block's registry entry, and every block that is not a crop
 * pays only that.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateRandomTickMixin {
  @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
  private void rpg4fools$settleOutOfSeasonCrops(ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo info) {
    BlockState state = (BlockState) (Object) this;
    Season season = CurrentSeason.season();

    // A crop standing through winter neither dies nor grows, and this is the half that stops it
    // growing. Cancelled rather than left to fall through, because what follows is the compost
    // boost: warm compost would otherwise ripen a frozen crop in the middle of January.
    if (CropTransition.frozen(state, season)) {
      info.cancel();
      return;
    }

    // Cancelled only when the block was replaced. Whatever is standing there now is not the block
    // vanilla was about to grow.
    if (CropTransition.apply(world, pos, state, season)) {
      info.cancel();
      return;
    }

    CompostGrowth.boost(world, pos, state, random);
  }
}
