package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.init.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

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
    BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DEAD_CROP, RenderLayer.getCutout());
  }
}
