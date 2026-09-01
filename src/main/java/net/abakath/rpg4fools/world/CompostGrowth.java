package net.abakath.rpg4fools.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

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

  public static void boost(ServerWorld world, BlockPos pos, BlockState state, Random random) {
    if (BOOSTING.get()) {
      return;
    }

    if (!(state.getBlock() instanceof CropBlock)) {
      return;
    }

    if (Compost.at(world, pos.down()) != Compost.WARM) {
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
