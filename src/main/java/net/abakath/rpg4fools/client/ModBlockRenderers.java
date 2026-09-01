package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.init.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

/**
 * Render layers for the mod's blocks.
 *
 * <p>A cross model needs the cutout layer. Without this the block renders on the solid layer and
 * the transparent part of the texture comes out black, which looks like a broken texture rather
 * than a missing line of registration.
 */
@Environment(EnvType.CLIENT)
public final class ModBlockRenderers {
  private ModBlockRenderers() {
  }

  public static void register() {
    BlockRenderLayerMap.INSTANCE.putBlocks(
            RenderType.cutout(),
            ModBlocks.DEAD_CROP,
            ModBlocks.DORMANT_SWEET_BERRY_BUSH,
            ModBlocks.CROP_STICK,
            ModBlocks.CROP_WALL
    );

    // Everything the roster registered, so a crop added later is not left rendering black.
    BlockRenderLayerMap.INSTANCE.putBlocks(
            RenderType.cutout(),
            ModBlocks.cropBlocks().toArray(new Block[0])
    );
  }
}
