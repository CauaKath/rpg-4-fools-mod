package net.abakath.rpg4fools.mixin.client;

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
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Pulls the near edge of the fog towards the player in the cold months.
 *
 * <p>Scaling the view distance alone, as the fog density hook does, moves the start and the end
 * together. Vanilla starts its fog far out, so a proportional scale still reads as a band on the
 * horizon rather than haze in the air nearby. This hook shortens the start on its own.
 *
 * <p>Targets the setShaderFogStart call by its full descriptor rather than by argument ordinal, so
 * it does not depend on the parameter order of applyFog.
 */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererFogStartMixin {
  @ModifyArg(
          method = "applyFog",
          at = @At(
                  value = "INVOKE",
                  target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V"
          ),
          index = 0
  )
  private static float rpg4fools$pullFogStartIn(float fogStart) {
    MinecraftClient client = MinecraftClient.getInstance();
    Camera camera = client.gameRenderer.getCamera();

    if (camera == null || camera.getSubmersionType() != CameraSubmersionType.NONE) {
      return fogStart;
    }

    ClientWorld world = client.world;

    if (world == null || !world.getRegistryKey().equals(World.OVERWORLD)) {
      return fogStart;
    }

    float strength = SeasonAtmosphere.getSeasonStrength(world, BlockPos.ofFloored(camera.getPos()));
    if (strength <= 0.0f) {
      return fogStart;
    }

    return fogStart * SeasonAtmosphere.getFogStartFactor(strength);
  }
}
