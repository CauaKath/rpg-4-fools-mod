package net.abakath.rpg4fools.server;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Which chunks were composted during the season now running.
 *
 * <p>Compost lasts a growing season, and a season ends for ground nobody is standing on too. The
 * sweep clears what is loaded when the season turns; this is what settles the rest, and it answers
 * the one question the block cannot: composted soil in a chunk that is not in this set was composted
 * in an earlier season, so it has expired.
 *
 * <p>A set of chunks rather than a date per block. The block already says what compost it carries -
 * that is what makes it drawable - and the only thing missing is when. Asking it per chunk costs one
 * long per chunk a player has actually treated, instead of two more blockstate properties on every
 * piece of farmland in the world.
 *
 * <p>The whole set is dropped the moment the stored season stops matching the one running, so a
 * server that was shut down over a season change comes back with everything correctly expired
 * rather than needing the change to have been witnessed.
 */
public class CompostedChunks extends SavedData {
  private static final String SEASON = "season";
  private static final String CHUNKS = "chunks";

  private final LongOpenHashSet chunks = new LongOpenHashSet();
  private Season season = null;

  private static final Factory<CompostedChunks> type = new Factory<>(
          CompostedChunks::new,
          CompostedChunks::createFromNbt,
          null
  );

  public static CompostedChunks get(ServerLevel world) {
    CompostedChunks state = world.getDataStorage()
            .computeIfAbsent(type, "rpg4fools_composted_chunks");

    state.rollOver();

    return state;
  }

  public static CompostedChunks createFromNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
    CompostedChunks state = new CompostedChunks();

    if (nbt.contains(SEASON)) {
      state.season = Season.valueOf(nbt.getString(SEASON));
    }

    for (long chunk : nbt.getLongArray(CHUNKS)) {
      state.chunks.add(chunk);
    }

    return state;
  }

  @Override
  public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registryLookup) {
    if (season != null) {
      nbt.putString(SEASON, season.name());
    }

    nbt.putLongArray(CHUNKS, chunks.toLongArray());

    return nbt;
  }

  public void remember(ChunkPos chunk) {
    if (chunks.add(chunk.toLong())) {
      setDirty();
    }
  }

  /** Whether the compost standing in this chunk was applied during the season now running. */
  public boolean current(ChunkPos chunk) {
    return chunks.contains(chunk.toLong());
  }

  /**
   * Drops the whole set when the season has moved on.
   *
   * <p>Called on every read rather than only when a change is witnessed. A season that turned while
   * the server was down was witnessed by nobody, and compost that survived a shutdown would be the
   * one way to keep it forever.
   */
  private void rollOver() {
    Season running = CurrentSeason.season();

    if (season == running) {
      return;
    }

    season = running;
    chunks.clear();
    setDirty();
  }
}
