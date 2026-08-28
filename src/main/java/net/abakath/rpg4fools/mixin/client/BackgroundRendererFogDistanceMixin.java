package net.abakath.rpg4fools.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.abakath.rpg4fools.client.ResolvedAtmosphere;
import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Closes the fog in around the player according to the biome they are standing in.
 *
 * <p>This is the same fog vanilla draws on the horizon, only brought near. Because vanilla computes
 * it from the camera position, the effect is felt only from inside a biome: standing outside a
 * swamp looking in, its fog is not yours.
 *
 * <p>Distances are set in blocks rather than scaled from the view distance. A scale factor means a
 * swamp feels completely different at render distance 8 and 32, and it cannot express "you can see
 * about 24 blocks in here" at all.
 *
 * <p>Runs at TAIL so vanilla has already set its own values, and only for FOG_TERRAIN. FOG_SKY uses
 * the same method, and overriding that too would wash the horizon out.
 */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererFogDistanceMixin {
  @Inject(method = "applyFog", at = @At("TAIL"))
  private static void rpg4fools$applyBiomeFog(Camera camera,
                                              BackgroundRenderer.FogType fogType,
                                              float viewDistance,
                                              boolean thickFog,
                                              float tickDelta,
                                              CallbackInfo ci) {
    if (fogType != BackgroundRenderer.FogType.FOG_TERRAIN) {
      return;
    }

    if (camera == null || camera.getSubmersionType() != CameraSubmersionType.NONE) {
      return;
    }

    MinecraftClient client = MinecraftClient.getInstance();
    ClientWorld world = client.world;

    if (world == null || !world.getRegistryKey().equals(World.OVERWORLD)) {
      return;
    }

    ResolvedAtmosphere atmosphere = SeasonAtmosphere.resolve(world, BlockPos.ofFloored(camera.getPos()));

    if (atmosphere.fogPresence() <= 0.0f) {
      return;
    }

    RenderSystem.setShaderFogStart(atmosphere.fogStart(viewDistance));
    RenderSystem.setShaderFogEnd(atmosphere.fogEnd(viewDistance));
  }
}
