package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.server.CompostedChunks;
import net.abakath.rpg4fools.server.LoadedChunks;
import net.abakath.rpg4fools.world.Compost;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Takes compost back out of the ground when its season ends.
 *
 * <p>Two things fill the queue, the same two that settle crops. The sweep empties every loaded chunk
 * into it the moment a season turns, and a chunk arriving later is scanned as it loads. The second
 * is what expires ground nobody was standing on, and it is why {@link CompostedChunks} exists: soil
 * still carrying compost in a chunk that was not composted during the season now running was
 * composted in an earlier one.
 *
 * <p>Emptied on a budget, for the reason {@link CropSettling} is. Finding composted farmland is a
 * palette check per section and costs nothing; clearing it is a neighbour update and a packet each,
 * and a season turning over a treated farm is hundreds of those.
 *
 * <p>A chunk that was composted this season is skipped before it is scanned at all, so the common
 * case - a player's own farm, loading and unloading all season - costs one set lookup.
 */
public final class CompostExpiry {
  private static final int SECTION_SIZE = 16;

  /** How many blocks are cleared per tick. Matched to the crop queue next door. */
  private static final int PER_TICK = 400;

  private static final Map<RegistryKey<World>, Deque<BlockPos>> WAITING = new ConcurrentHashMap<>();

  /**
   * Whether the first sweep has run.
   *
   * <p>Chunks load before the server has ticked, and the season is not known until it has: the day
   * handler works it out on the first tick and everything before that is holding a default. Reading
   * the season during start up would compare a summer save against a spring guess and expire a
   * player's whole farm on the way in. So chunks that arrive before the first tick are not scanned
   * at all, and the first tick sweeps everything loaded instead - which is exactly the set of chunks
   * that was skipped.
   */
  private static boolean swept = false;

  private CompostExpiry() {
  }

  public static void register() {
    // Reset per server, not per JVM. In single player the client keeps this class loaded between
    // worlds, and a flag left set would let the next save be scanned against the season the last
    // one ended in.
    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      swept = false;
      WAITING.clear();
    });

    ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
      if (!swept || CompostedChunks.get(world).current(chunk.getPos())) {
        return;
      }

      enqueue(world, collect(chunk));
    });

    ServerTickEvents.END_SERVER_TICK.register(CompostExpiry::clear);
  }

  /**
   * Queues every composted block loaded right now.
   *
   * <p>Called the moment a season turns, when everything in the ground has just expired by
   * definition, so the chunk set is not consulted: it is about to be emptied anyway.
   */
  public static void sweep(MinecraftServer server) {
    sweep(server, true);
  }

  /**
   * Queues composted blocks across every loaded chunk.
   *
   * <p>Called two ways. A season turn takes everything, because everything in the ground has just
   * expired by definition. The first tick after start up takes only the chunks that were not
   * composted during the season now running, which is the same question the chunk load path asks -
   * it is standing in for the scans that were skipped while the season was still unknown.
   */
  private static void sweep(MinecraftServer server, boolean expired) {
    for (ServerWorld world : server.getWorlds()) {
      CompostedChunks composted = CompostedChunks.get(world);
      List<BlockPos> found = new ArrayList<>();

      LoadedChunks.forEachChunk(world, chunk -> {
        if (!expired && composted.current(chunk.getPos())) {
          return;
        }

        found.addAll(collect(chunk));
      });

      enqueue(world, found);
    }
  }

  /**
   * Every composted block of farmland in a chunk.
   *
   * <p>Cost is kept off the scan by asking each section whether its palette holds any composted
   * farmland before looking at a single position in it, exactly as the crop scan does. Almost every
   * section in the world is dismissed on that check.
   */
  private static List<BlockPos> collect(Chunk chunk) {
    List<BlockPos> found = new ArrayList<>();
    ChunkSection[] sections = chunk.getSectionArray();

    for (int index = 0; index < sections.length; index++) {
      ChunkSection section = sections[index];

      if (section.isEmpty() || !section.hasAny(Compost::composted)) {
        continue;
      }

      int startX = chunk.getPos().getStartX();
      int startY = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(index));
      int startZ = chunk.getPos().getStartZ();

      for (int y = 0; y < SECTION_SIZE; y++) {
        for (int z = 0; z < SECTION_SIZE; z++) {
          for (int x = 0; x < SECTION_SIZE; x++) {
            if (Compost.composted(section.getBlockState(x, y, z))) {
              found.add(new BlockPos(startX + x, startY + y, startZ + z));
            }
          }
        }
      }
    }

    return found;
  }

  private static void enqueue(ServerWorld world, List<BlockPos> positions) {
    if (positions.isEmpty()) {
      return;
    }

    waiting(world).addAll(positions);
  }

  private static void clear(MinecraftServer server) {
    if (!swept) {
      swept = true;
      sweep(server, false);
    }

    for (ServerWorld world : server.getWorlds()) {
      Deque<BlockPos> queue = waiting(world);

      if (queue.isEmpty()) {
        continue;
      }

      for (int cleared = 0; cleared < PER_TICK; cleared++) {
        BlockPos pos = queue.poll();

        if (pos == null) {
          break;
        }

        // Skipped rather than forced, like the crop queue: a chunk that has gone away since being
        // queued is scanned again when it comes back.
        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
          continue;
        }

        BlockState state = world.getBlockState(pos);

        // Read again on the way out. Anything can have happened to the block since it was queued,
        // including the player hoeing the compost off it or the farmland being trampled to dirt.
        if (!Compost.composted(state)) {
          continue;
        }

        world.setBlockState(pos, state.with(Compost.PROPERTY, Compost.NONE), Block.NOTIFY_LISTENERS);
      }
    }
  }

  private static Deque<BlockPos> waiting(ServerWorld world) {
    return WAITING.computeIfAbsent(world.getRegistryKey(), key -> new ArrayDeque<>());
  }
}
