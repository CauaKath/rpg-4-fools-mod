package net.abakath.rpg4fools.server;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.abakath.rpg4fools.world.Compost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.function.BiConsumer;

/**
 * Which composters have been labelled, and with what.
 *
 * <p>A composter has no block entity and its only state is how full it is, so a batch that is going
 * to come out as something other than bone meal has nowhere on the block to say so. This is that
 * somewhere.
 *
 * <p>Positions rather than a property added to the composter. Farmland earned a property because it
 * has to be drawn differently; a labelled composter looks exactly like an unlabelled one, and paying
 * for four times the blockstates to store something invisible would be paying for nothing.
 *
 * <p>Entries are dropped when the batch is collected, and again on the way out if the position is no
 * longer a composter. A composter that was broken while full would otherwise leave a label behind
 * for whatever gets built there next.
 */
public class PendingCompost extends SavedData {
  private static final String POSITIONS = "positions";
  private static final String KINDS = "kinds";

  private final Long2ByteMap marks = new Long2ByteOpenHashMap();

  private static final Factory<PendingCompost> type = new Factory<>(
          PendingCompost::new,
          PendingCompost::createFromNbt,
          null
  );

  public static PendingCompost get(ServerLevel world) {
    return world.getDataStorage().computeIfAbsent(type, "rpg4fools_pending_compost");
  }

  public static PendingCompost createFromNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
    PendingCompost pending = new PendingCompost();

    long[] positions = nbt.getLongArray(POSITIONS);
    byte[] kinds = nbt.getByteArray(KINDS);

    // Two arrays that have to line up. A save truncated between them is not worth crashing over -
    // the worst a short read costs is a label, and the player still has the composter.
    for (int i = 0; i < Math.min(positions.length, kinds.length); i++) {
      pending.marks.put(positions[i], kinds[i]);
    }

    return pending;
  }

  @Override
  public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registryLookup) {
    long[] positions = new long[marks.size()];
    byte[] kinds = new byte[marks.size()];
    int index = 0;

    for (Long2ByteMap.Entry entry : marks.long2ByteEntrySet()) {
      positions[index] = entry.getLongKey();
      kinds[index] = entry.getByteValue();
      index++;
    }

    nbt.putLongArray(POSITIONS, positions);
    nbt.putByteArray(KINDS, kinds);

    return nbt;
  }

  public void mark(BlockPos pos, Compost kind) {
    marks.put(pos.asLong(), (byte) kind.ordinal());
    setDirty();
  }

  public void clear(BlockPos pos) {
    if (!marks.containsKey(pos.asLong())) {
      return;
    }

    marks.remove(pos.asLong());
    setDirty();
  }

  /**
   * Visits every labelled composter this world is currently holding.
   *
   * <p>Walks a copy of the positions, because reading one can drop it: a label whose block has gone
   * is forgotten on the way out, and that would be a modification during iteration.
   *
   * <p>Positions in chunks that are not loaded are skipped rather than read. Asking for a block state
   * there answers air, which would forget a perfectly good label the moment a player walked away
   * from their composter.
   */
  public void forEach(ServerLevel world, BiConsumer<BlockPos, Compost> action) {
    for (long packed : marks.keySet().toLongArray()) {
      BlockPos pos = BlockPos.of(packed);

      if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
        continue;
      }

      Compost kind = marked(world, pos);

      if (kind != Compost.NONE) {
        action.accept(pos, kind);
      }
    }
  }

  /**
   * What this composter is labelled with, or {@link Compost#NONE} for one that is not.
   *
   * <p>Forgets the label when the block is no longer a composter, which is the only cleanup these
   * entries get.
   */
  public Compost marked(ServerLevel world, BlockPos pos) {
    if (!marks.containsKey(pos.asLong())) {
      return Compost.NONE;
    }

    if (!world.getBlockState(pos).is(Blocks.COMPOSTER)) {
      clear(pos);
      return Compost.NONE;
    }

    return Compost.values()[marks.get(pos.asLong())];
  }
}
