package net.abakath.rpg4fools.server;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.world.CropSeasons;
import net.abakath.rpg4fools.world.CropSticks;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

/**
 * Which crops a player put in the ground.
 *
 * <p>The one thing the world cannot work out for itself. A wheat plant in a village farm and a
 * wheat plant in somebody's back garden are the same block, and only one of them should sow itself
 * again when the season that killed it passes: a village is tended, a player's field is the
 * player's business.
 *
 * <p>Positions rather than a block property. Most of what grows in a village is vanilla wheat, and
 * giving CropBlock a property of this mod's own would mean rewriting a vanilla block's states -
 * expensive, and the kind of change that breaks every other mod that touches crops.
 *
 * <p>Entries are dropped when the crop is broken, and again on the way out if the position no
 * longer holds a crop at all. Explosions and pistons do not announce themselves, and a set that
 * only ever grew would be a leak with a saved copy.
 */
public class PlantedCrops extends PersistentState {
  private static final String KEY = "planted";

  private final LongOpenHashSet positions = new LongOpenHashSet();

  private static final Type<PlantedCrops> type = new Type<>(
          PlantedCrops::new,
          PlantedCrops::createFromNbt,
          null
  );

  public static PlantedCrops get(ServerWorld world) {
    return world.getPersistentStateManager().getOrCreate(type, "rpg4fools_planted_crops");
  }

  public static PlantedCrops createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    PlantedCrops planted = new PlantedCrops();

    for (long position : nbt.getLongArray(KEY)) {
      planted.positions.add(position);
    }

    return planted;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    nbt.putLongArray(KEY, positions.toLongArray());
    return nbt;
  }

  public void remember(BlockPos pos) {
    if (positions.add(pos.asLong())) {
      markDirty();
    }
  }

  public void forget(BlockPos pos) {
    if (positions.remove(pos.asLong())) {
      markDirty();
    }
  }

  /**
   * Whether a player planted what is standing here.
   *
   * <p>Forgets the position when the answer is stale, which is the only cleanup a set of positions
   * gets. A crop that was blown up leaves an entry behind; the first time anything asks about that
   * spot, it goes.
   */
  public boolean planted(ServerWorld world, BlockPos pos) {
    if (!positions.contains(pos.asLong())) {
      return false;
    }

    BlockState state = world.getBlockState(pos);

    // A trellis counts, empty or not. It outlives the plant it was built for, and forgetting the
    // spot while it stood bare would hand a player's trellis to the rule that resows village fields
    // the next time a season turned.
    if (CropSeasons.isCrop(state) || state.isOf(ModBlocks.DEAD_CROP) || CropSticks.isColumn(state)) {
      return true;
    }

    forget(pos);
    return false;
  }
}
