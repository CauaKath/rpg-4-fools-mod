package net.abakath.rpg4fools.server.events.season;

import net.abakath.rpg4fools.world.season.SeasonSnow;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Removes snow and ice once the season stops putting it there.
 *
 * <p>Vanilla never melts surface snow from daylight. SnowBlock only melts beside a block light
 * source above level 11, which is right for a biome meant to stay white but would leave a plains
 * permanently snowed over after a single winter.
 *
 * <p>Samples positions around each player rather than walking loaded chunks. That is where vanilla
 * laid the snow down in the first place, since chunk ticks only run near players, so it is the same
 * ground either way and it keeps the work proportional to how many people are online.
 *
 * <p>Deliberately not a mixin. Melting needs no vanilla internals, and ServerLevel.tickChunk is a
 * private method whose signature can only be checked by launching the game.
 */
public class SnowMeltHandler implements ServerTickEvents.StartTick {
  /** Ticks between passes. Thawing is meant to take days, not to clear a region at once. */
  private static final int TICK_INTERVAL = 20;

  /** Positions examined per player per pass. */
  private static final int SAMPLES_PER_PLAYER = 8;

  /** Horizontal reach of the sampling, in blocks. */
  private static final int SAMPLE_RADIUS = 96;

  private int tickCounter = 0;

  @Override
  public void onStartTick(MinecraftServer server) {
    if (++tickCounter < TICK_INTERVAL) {
      return;
    }

    tickCounter = 0;

    ServerLevel overworld = server.getLevel(Level.OVERWORLD);

    if (overworld == null) {
      return;
    }

    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      if (player.level() != overworld) {
        continue;
      }

      for (int i = 0; i < SAMPLES_PER_PLAYER; i++) {
        meltOneSample(overworld, player.blockPosition());
      }
    }
  }

  private void meltOneSample(ServerLevel world, BlockPos around) {
    RandomSource random = world.getRandom();

    int x = around.getX() + random.nextInt(SAMPLE_RADIUS * 2) - SAMPLE_RADIUS;
    int z = around.getZ() + random.nextInt(SAMPLE_RADIUS * 2) - SAMPLE_RADIUS;

    if (!world.hasChunk(x >> 4, z >> 4)) {
      return;
    }

    BlockPos top = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
    Biome biome = world.getBiome(top).value();

    if (!SeasonSnow.shouldThaw(biome.getBaseTemperature())) {
      return;
    }

    // Only snow layers, never SNOW_BLOCK, so anything a player built out of snow survives.
    if (world.getBlockState(top).is(Blocks.SNOW)) {
      world.removeBlock(top, false);
      return;
    }

    // Ice survives the midsummer thaw. Clearing a snowy plains of its cover for a month is the
    // intent; draining a frozen ocean and stranding whatever was standing on it is not.
    if (SeasonSnow.isMidsummerThaw()) {
      return;
    }

    BlockPos below = top.below();

    if (world.getBlockState(below).is(Blocks.ICE)) {
      world.setBlockAndUpdate(below, Blocks.WATER.defaultBlockState());
    }
  }
}
