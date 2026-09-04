package net.abakath.rpg4fools.world.compost;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The extra growth roll warm soil buys.
 *
 * <p>A second random tick rather than a changed growth formula. Vanilla's roll already accounts for
 * light, moisture and what is planted next door, and every crop in the game - vanilla, this mod's,
 * and any other mod's - reaches it the same way. Running it twice doubles the rate without having
 * to know anything about the plant.
 *
 * <p>The guard is what makes that safe. The second tick goes back through the same block state
 * method the hook is injected into, so without it the hook would call itself until the stack ran
 * out. A thread local rather than a field because random ticks run on the server thread per world
 * and a shared boolean would have two worlds writing it.
 *
 * <p>Gated on CropBlock before anything is read out of the world. This is called from the hottest
 * path in the game, and an instanceof is cheap in a way that fetching the block below is not.
 */
public final class CompostGrowth {
  private static final ThreadLocal<Boolean> BOOSTING = ThreadLocal.withInitial(() -> false);

  private CompostGrowth() {
  }

  public static void boost(ServerLevel world, BlockPos pos, BlockState state, RandomSource random) {
    if (BOOSTING.get()) {
      return;
    }

    if (!(state.getBlock() instanceof CropBlock)) {
      return;
    }

    if (Compost.at(world, pos.below()) != Compost.WARM) {
      return;
    }

    BOOSTING.set(true);

    try {
      state.randomTick(world, pos, random);
    } finally {
      BOOSTING.set(false);
    }
  }
}
