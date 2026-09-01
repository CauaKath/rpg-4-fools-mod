package net.abakath.rpg4fools.server;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.CurrentSeason;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.abakath.rpg4fools.RPG4Fools;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.Optional;
import java.util.stream.LongStream;

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

  /**
   * The season is absent rather than empty on a state that has never rolled over, which is what
   * rollOver reads to tell "no season recorded yet" from "recorded, and it has moved on".
   */
  private static final Codec<CompostedChunks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.STRING.optionalFieldOf(SEASON).forGetter(state -> Optional.ofNullable(state.season).map(Season::name)),
          Codec.LONG_STREAM.optionalFieldOf(CHUNKS, LongStream.empty()).forGetter(state -> LongStream.of(state.chunks.toLongArray()))
  ).apply(instance, CompostedChunks::fromStored));

  private static final SavedDataType<CompostedChunks> TYPE = new SavedDataType<>(
          Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, "composted_chunks"),
          CompostedChunks::new,
          CODEC,
          null
  );

  public static CompostedChunks get(ServerLevel world) {
    CompostedChunks state = world.getDataStorage().computeIfAbsent(TYPE);

    state.rollOver();

    return state;
  }

  private static CompostedChunks fromStored(Optional<String> season, LongStream stored) {
    CompostedChunks state = new CompostedChunks();

    season.ifPresent(name -> state.season = Season.valueOf(name));
    stored.forEach(state.chunks::add);

    return state;
  }

  public void remember(ChunkPos chunk) {
    if (chunks.add(chunk.pack())) {
      setDirty();
    }
  }

  /** Whether the compost standing in this chunk was applied during the season now running. */
  public boolean current(ChunkPos chunk) {
    return chunks.contains(chunk.pack());
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
