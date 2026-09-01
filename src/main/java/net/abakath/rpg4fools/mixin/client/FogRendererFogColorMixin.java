package net.abakath.rpg4fools.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.abakath.rpg4fools.client.ResolvedAtmosphere;
import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grades the fog colour by the current season, so fog and sky agree.
 *
 * <p>Vanilla derives the Overworld fog colour from the biome fog colour rather than from
 * ClientLevel.getSkyColor, so the sky hook does not cover this. Grading both keeps the horizon from
 * splitting into two different colours.
 *
 * <p>The hook runs at RETURN, after vanilla has finished its own water, lava and powder snow
 * handling, and it bails out unless the camera is in open air in the Overworld. That leaves every
 * submersion case untouched.
 */
@Mixin(FogRenderer.class)
public class FogRendererFogColorMixin {
  @Shadow
  private static float fogRed;

  @Shadow
  private static float fogGreen;

  @Shadow
  private static float fogBlue;

  @Inject(method = "setupColor", at = @At("RETURN"))
  private static void rpg4fools$gradeFogColor(Camera camera,
                                              float tickDelta,
                                              ClientLevel world,
                                              int viewDistance,
                                              float skyDarkness,
                                              CallbackInfo ci) {
    if (world == null || !world.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    if (camera.getFluidInCamera() != FogType.NONE) {
      return;
    }

    ResolvedAtmosphere atmosphere = SeasonAtmosphere.resolve(world, BlockPos.containing(camera.position()));

    int graded = atmosphere.applyToFogColor(packRgb(fogRed, fogGreen, fogBlue));

    fogRed = ((graded >> 16) & 0xFF) / 255.0f;
    fogGreen = ((graded >> 8) & 0xFF) / 255.0f;
    fogBlue = (graded & 0xFF) / 255.0f;

    RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0f);
  }

  private static int packRgb(float r, float g, float b) {
    int red = Math.round(Math.min(1.0f, Math.max(0.0f, r)) * 255.0f);
    int green = Math.round(Math.min(1.0f, Math.max(0.0f, g)) * 255.0f);
    int blue = Math.round(Math.min(1.0f, Math.max(0.0f, b)) * 255.0f);

    return (red << 16) | (green << 8) | blue;
  }
}
