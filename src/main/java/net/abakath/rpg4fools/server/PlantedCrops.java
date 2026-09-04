package net.abakath.rpg4fools.server;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.world.CropSeasons;
import net.abakath.rpg4fools.world.CropSticks;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.stream.LongStream;

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
public class PlantedCrops extends SavedData {
  private static final String KEY = "planted";

  private final LongOpenHashSet positions = new LongOpenHashSet();

  private static final Codec<PlantedCrops> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.LONG_STREAM.optionalFieldOf(KEY, LongStream.empty()).forGetter(state -> LongStream.of(state.positions.toLongArray()))
  ).apply(instance, PlantedCrops::fromPositions));

  private static final SavedDataType<PlantedCrops> TYPE = new SavedDataType<>(
          Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, "planted_crops"),
          PlantedCrops::new,
          CODEC,
          // Not null. SavedDataStorage calls update on this without checking, so a null one throws
          // the moment a saved file actually exists - which is every load after the first save.
          // No vanilla type describes a mod's own data, so this is the one with the least to do:
          // its schema is a bare long array, and the fixers only run at all when the stored data
          // version is older than the current one.
          DataFixTypes.SAVED_DATA_FORCED_CHUNKS
  );

  public static PlantedCrops get(ServerLevel world) {
    return world.getDataStorage().computeIfAbsent(TYPE);
  }

  private static PlantedCrops fromPositions(LongStream stored) {
    PlantedCrops planted = new PlantedCrops();

    stored.forEach(planted.positions::add);

    return planted;
  }

  public void remember(BlockPos pos) {
    if (positions.add(pos.asLong())) {
      setDirty();
    }
  }

  public void forget(BlockPos pos) {
    if (positions.remove(pos.asLong())) {
      setDirty();
    }
  }

  /**
   * Whether a player planted what is standing here.
   *
   * <p>Forgets the position when the answer is stale, which is the only cleanup a set of positions
   * gets. A crop that was blown up leaves an entry behind; the first time anything asks about that
   * spot, it goes.
   */
  public boolean planted(ServerLevel world, BlockPos pos) {
    if (!positions.contains(pos.asLong())) {
      return false;
    }

    BlockState state = world.getBlockState(pos);

    // A trellis counts, empty or not. It outlives the plant it was built for, and forgetting the
    // spot while it stood bare would hand a player's trellis to the rule that resows village fields
    // the next time a season turned.
    if (CropSeasons.isCrop(state) || state.is(ModBlocks.DEAD_CROP) || CropSticks.isColumn(state)) {
      return true;
    }

    forget(pos);
    return false;
  }
}
