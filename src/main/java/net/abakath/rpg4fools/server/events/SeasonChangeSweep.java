package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.server.LoadedChunks;
import net.abakath.rpg4fools.world.CropTransition;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Settles every loaded crop the moment a season turns.
 *
 * <p>A player standing in a wheat field when winter arrives should watch it die, not find it dead
 * next time it happens to tick. The random tick hook remains for the rest: a farm nobody had loaded
 * at the turn of the season converts when it comes back, which is the only thing that can be done
 * about ground the server is not holding.
 *
 * <p>Cost is kept off the scan by asking each chunk section whether its palette contains a crop at
 * all before looking at any position in it. A section is 4096 blocks and almost none of them hold
 * crops, so nearly every section is dismissed on a palette check.
 */
public final class SeasonChangeSweep {
  /** Blocks along one edge of a chunk section. */
  private static final int SECTION_SIZE = 16;

  private SeasonChangeSweep() {
  }

  public static void run(MinecraftServer server, Season season) {
    for (ServerWorld world : server.getWorlds()) {
      sweep(world, season);
    }
  }

  private static void sweep(ServerWorld world, Season season) {
    List<BlockPos> crops = new ArrayList<>();

    LoadedChunks.forEachChunk(world, chunk -> collectCrops(chunk, crops));

    int changed = 0;
    for (BlockPos pos : crops) {
      // Read the state again rather than trusting the one collected. Setting a block can break a
      // neighbour, and a stem taken out with its farmland should not then be replaced by a dead
      // crop standing on nothing.
      if (CropTransition.apply(world, pos, world.getBlockState(pos), season)) {
        changed++;
      }
    }

    if (changed > 0) {
      RPG4Fools.LOGGER.info("{} turned {} crop(s) with the change to {}", world.getRegistryKey().getValue(), changed, season.getName());
    }
  }

  private static void collectCrops(WorldChunk chunk, List<BlockPos> crops) {
    ChunkSection[] sections = chunk.getSectionArray();

    for (int index = 0; index < sections.length; index++) {
      ChunkSection section = sections[index];

      if (section.isEmpty() || !section.hasAny(CropTransition::settles)) {
        continue;
      }

      int startX = chunk.getPos().getStartX();
      int startY = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(index));
      int startZ = chunk.getPos().getStartZ();

      for (int y = 0; y < SECTION_SIZE; y++) {
        for (int z = 0; z < SECTION_SIZE; z++) {
          for (int x = 0; x < SECTION_SIZE; x++) {
            BlockState state = section.getBlockState(x, y, z);

            if (CropTransition.settles(state)) {
              crops.add(new BlockPos(startX + x, startY + y, startZ + z));
            }
          }
        }
      }
    }
  }
}
