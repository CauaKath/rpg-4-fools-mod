package net.abakath.rpg4fools.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Which chunks are loaded, per world.
 *
 * <p>Needed because nothing public hands out that list: ServerChunkManager will look a chunk up by
 * position but will not enumerate what it holds. Keeping the positions as they load is cheaper than
 * reaching into its internals through a mixin, and it is the only way to act on every loaded chunk
 * at a specific moment.
 *
 * <p>Positions rather than chunk objects, so a missed unload cannot pin a chunk in memory. The
 * chunk is resolved on use and skipped if it has gone.
 */
public final class LoadedChunks {
  private static final Map<RegistryKey<World>, Set<Long>> BY_WORLD = new ConcurrentHashMap<>();

  private LoadedChunks() {
  }

  public static void register() {
    ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> positions(world).add(chunk.getPos().toLong()));
    ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> positions(world).remove(chunk.getPos().toLong()));

    // A world going away takes its whole set with it, so an unloaded dimension does not leak.
    ServerWorldEvents.UNLOAD.register((server, world) -> BY_WORLD.remove(world.getRegistryKey()));
  }

  /**
   * Visits every chunk of this world that is still loaded.
   *
   * <p>Iterates a copy of the position set. The action is free to change blocks, which can load or
   * unload chunks and would otherwise be modifying the set being walked.
   */
  public static void forEachChunk(ServerWorld world, Consumer<WorldChunk> action) {
    for (long position : Set.copyOf(positions(world))) {
      Chunk chunk = world.getChunk(ChunkPos.getPackedX(position), ChunkPos.getPackedZ(position), ChunkStatus.FULL, false);

      if (chunk instanceof WorldChunk loaded) {
        action.accept(loaded);
      }
    }
  }

  private static Set<Long> positions(ServerWorld world) {
    return BY_WORLD.computeIfAbsent(world.getRegistryKey(), key -> ConcurrentHashMap.newKeySet());
  }
}
