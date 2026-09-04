package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.server.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.List;

/**
 * Queues every loaded crop the moment a season turns.
 *
 * <p>A player standing in a wheat field when winter arrives should watch it die, not find it dead
 * next time it happens to tick. Ground nobody had loaded at the turn is settled by
 * {@link CropSettling} as it comes back, which is the only thing that can be done about chunks the
 * server is not holding - loading a world's worth of them to be thorough would cost minutes and
 * change nothing anybody could see.
 *
 * <p>Collects, and leaves the changing to the queue. Finding the crops is a palette check per chunk
 * section and costs almost nothing; changing them is neighbour updates and packets, and a season
 * turning over several large farms is thousands of those in one tick.
 */
public final class SeasonChangeSweep {
  private SeasonChangeSweep() {
  }

  public static void run(MinecraftServer server, Season season) {
    for (ServerLevel world : server.getAllLevels()) {
      sweep(world, season);
    }

    // Compost lasts a growing season and this is the moment one ended, so everything in the ground
    // has just expired. Queued rather than cleared here for the same reason the crops are.
    CompostExpiry.sweep(server);
  }

  private static void sweep(ServerLevel world, Season season) {
    long started = System.nanoTime();
    List<BlockPos> crops = new ArrayList<>();

    LoadedChunks.forEachChunk(world, chunk -> crops.addAll(CropSettling.collect(chunk)));
    CropSettling.enqueue(world, crops);

    if (crops.isEmpty()) {
      return;
    }

    RPG4Fools.LOGGER.info("{} queued {} crop(s) for the change to {}, found in {} ms, {} waiting",
            world.dimension().location(), crops.size(), season.getName(),
            (System.nanoTime() - started) / 1_000_000, CropSettling.waitingIn(world));
  }
}
