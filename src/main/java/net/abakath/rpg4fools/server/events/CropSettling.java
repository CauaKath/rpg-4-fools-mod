package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.CropSeasons;
import net.abakath.rpg4fools.world.CropTransition;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The queue of crops waiting to be brought into line with the season.
 *
 * <p>Two things fill it. The sweep empties every loaded chunk into it the moment a season turns,
 * and a chunk arriving later is scanned as it loads - which is what settles the ground nobody was
 * standing on when the season changed. A chunk that is already correct costs a palette check and
 * nothing else, since bringing a crop into line twice does nothing the second time.
 *
 * <p>Emptied on a budget rather than all at once. Finding the crops is nearly free, but changing
 * one means a neighbour update, a lighting check and a packet, and a season turning over several
 * large farms is thousands of those. Spread across ticks they are invisible; in a single tick they
 * are a stutter.
 *
 * <p>Positions rather than states, and the state is read again on the way out. Anything can have
 * happened to a block between being queued and being reached, up to and including the chunk going
 * away.
 */
public final class CropSettling {
  /** Blocks along one edge of a chunk section. */
  private static final int SECTION_SIZE = 16;

  /** How many crops are settled per tick. Enough to finish a season turn in well under a second. */
  private static final int PER_TICK = 400;

  private static final Map<ResourceKey<Level>, Deque<BlockPos>> WAITING = new ConcurrentHashMap<>();

  private CropSettling() {
  }

  public static void register() {
    ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> enqueue(world, collect(chunk)));
    ServerTickEvents.END_SERVER_TICK.register(CropSettling::settle);
  }

  public static void enqueue(ServerLevel world, List<BlockPos> positions) {
    if (positions.isEmpty()) {
      return;
    }

    waiting(world).addAll(positions);
  }

  /**
   * Every crop and dead crop in a chunk.
   *
   * <p>Cost is kept off the scan by asking each chunk section whether its palette holds anything
   * worth settling before looking at a single position in it. A section is 4096 blocks and almost
   * none of them hold crops, so nearly every section is dismissed on a palette check - which is
   * what makes this cheap enough to run on every chunk that loads.
   */
  public static List<BlockPos> collect(ChunkAccess chunk) {
    List<BlockPos> found = new ArrayList<>();
    LevelChunkSection[] sections = chunk.getSections();

    for (int index = 0; index < sections.length; index++) {
      LevelChunkSection section = sections[index];

      if (section.hasOnlyAir() || !section.maybeHas(CropTransition::settles)) {
        continue;
      }

      int startX = chunk.getPos().getMinBlockX();
      int startY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
      int startZ = chunk.getPos().getMinBlockZ();

      for (int y = 0; y < SECTION_SIZE; y++) {
        for (int z = 0; z < SECTION_SIZE; z++) {
          for (int x = 0; x < SECTION_SIZE; x++) {
            if (CropTransition.settles(section.getBlockState(x, y, z))) {
              found.add(new BlockPos(startX + x, startY + y, startZ + z));
            }
          }
        }
      }
    }

    return found;
  }

  /** How many are still waiting, which is what the sweep reports once it has finished. */
  public static int waitingIn(ServerLevel world) {
    return waiting(world).size();
  }

  private static void settle(MinecraftServer server) {
    for (ServerLevel world : server.getAllLevels()) {
      Deque<BlockPos> queue = waiting(world);

      if (queue.isEmpty()) {
        continue;
      }

      Season season = CurrentSeason.season();

      for (int settled = 0; settled < PER_TICK; settled++) {
        BlockPos pos = queue.poll();

        if (pos == null) {
          break;
        }

        // Skipped rather than forced. A chunk that has gone away since being queued will be scanned
        // again when it comes back, and loading it here to save that would be the whole point of
        // the budget thrown away.
        if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
          continue;
        }

        BlockState state = world.getBlockState(pos);

        if (CropTransition.settles(state)) {
          CropTransition.apply(world, pos, state, season);
        }
      }
    }
  }

  private static Deque<BlockPos> waiting(ServerLevel world) {
    return WAITING.computeIfAbsent(world.dimension(), key -> new ArrayDeque<>());
  }
}
