package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.server.SeasonData;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.awt.Color;
import java.util.*;

public class SeasonColorsHandler implements ServerTickEvents.StartTick {
  private final List<Block> foliageBlocks = Arrays.asList(
          Blocks.OAK_LEAVES,
          Blocks.SPRUCE_LEAVES,
          Blocks.BIRCH_LEAVES,
          Blocks.JUNGLE_LEAVES,
          Blocks.ACACIA_LEAVES,
          Blocks.DARK_OAK_LEAVES,
          Blocks.MANGROVE_LEAVES
  );

  private MinecraftServer serverInstance = null;
  private Season season = null;

  @Override
  public void onStartTick(MinecraftServer server) {
    if (server.getWorld(World.OVERWORLD) == null) {
      return;
    }

    this.serverInstance = server;

    Season oldSeason = this.season;
    SeasonData seasonData = SeasonData.getServerState(server);
    Season newSeason = seasonData.season;

    World overworld = server.getWorld(World.OVERWORLD);
    assert overworld != null;

    if (oldSeason == null || !oldSeason.equals(newSeason)) {
      reloadChunksForAllPlayers(overworld);
    }

    this.season = newSeason;

    ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
      if (world != null && pos != null) {
        return this.adjustSaturation(BiomeColors.getGrassColor(world, pos), this.getSeasonSaturation(seasonData));
      }
      return -1;
    }, Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN);

    // TODO: Add coloring to flowers, vines and more

    ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
      if (world != null && pos != null) {
        return this.adjustSaturation(BiomeColors.getFoliageColor(world, pos), this.getSeasonSaturation(seasonData));
      }
      return -1;
    }, this.foliageBlocksToArray());

    ColorProviderRegistry.ITEM.register((stack, tintIndex) -> 0x91BD59, Items.GRASS_BLOCK, Items.SHORT_GRASS, Items.TALL_GRASS, Items.FERN, Items.LARGE_FERN);
    ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
      Block block = Block.getBlockFromItem(stack.getItem());
      if (block != null) {
        return this.getDefaultFoliageColor(block);
      }
      return -1;
    }, this.foliageBlocksToArray());
  }

  private int adjustSaturation(int rgb, float saturationFactor) {
    Color color = new Color(rgb);
    float[] hsbVals = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

    hsbVals[1] = Math.min(1.0f, hsbVals[1] * saturationFactor);

    return Color.HSBtoRGB(hsbVals[0], hsbVals[1], hsbVals[2]);
  }

  private int getDefaultFoliageColor(Block block) {
    if (block == Blocks.OAK_LEAVES) {
      return 0x48b518;
    } else if (block == Blocks.BIRCH_LEAVES) {
      return 0x80a755;
    } else if (block == Blocks.SPRUCE_LEAVES) {
      return 0x619961;
    } else if (block == Blocks.JUNGLE_LEAVES) {
      return 0x48b518;
    } else if (block == Blocks.ACACIA_LEAVES) {
      return 0x48b518;
    } else if (block == Blocks.DARK_OAK_LEAVES) {
      return 0x48b518;
    } else if (block == Blocks.MANGROVE_LEAVES) {
      return 0x92c648;
    } else {
      return 0x48B518;
    }
  }

  private float getSeasonSaturation(SeasonData seasonData) {
    return switch (seasonData.season) {
      case WINTER -> 0.5f;
      case AUTUMN -> 0.8f;
      case SPRING -> 1.0f;
      case SUMMER -> 1.5f;
    };
  }

  private Block[] foliageBlocksToArray() {
    return this.foliageBlocks.toArray(new Block[0]);
  }

  private void reloadChunksForAllPlayers(World world) {
    for (ServerPlayerEntity player : this.serverInstance.getPlayerManager().getPlayerList()) {
      int viewDistance = player.getClientOptions().viewDistance();

      ChunkPos playerChunkPos = new ChunkPos(player.getBlockPos());
      for (int x = playerChunkPos.x - viewDistance; x <= playerChunkPos.x + viewDistance; x++) {
        for (int z = playerChunkPos.z - viewDistance; z <= playerChunkPos.z + viewDistance; z++) {
          ChunkPos chunkPos = new ChunkPos(x, z);
          if (world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
            BitSet skyBits = new BitSet(viewDistance * viewDistance * 384);
            BitSet blockBits = new BitSet(viewDistance * viewDistance * 384);

            player.networkHandler.sendPacket(new ChunkDataS2CPacket(
                    world.getChunk(chunkPos.x, chunkPos.z),
                    world.getLightingProvider(),
                    skyBits,
                    blockBits
            ));
          }
        }
      }
    }
  }
}
