package net.abakath.rpg4fools.mixin.client;

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
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Pulls the fog in during the colder months and pushes it out slightly in summer.
 *
 * <p>Rather than setting the fog distance directly, this scales the view distance vanilla is handed
 * before it does its own maths. Everything downstream, including the thick fog case and the fog
 * shape, keeps working off a value it understands.
 *
 * <p>Only open air in the Overworld is affected. Water, lava and powder snow compute their fog from
 * their own constants, and scaling those would look wrong.
 *
 * <p>The handler takes no extra parameters on purpose. A ModifyVariable handler must append either
 * all of the target method's parameters or none of them, so reading the camera back from the client
 * is less brittle than trying to mirror the full applyFog signature.
 */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererFogDensityMixin {
  @ModifyVariable(method = "applyFog", at = @At("HEAD"), ordinal = 0, argsOnly = true)
  private static float rpg4fools$scaleFogDistance(float viewDistance) {
    MinecraftClient client = MinecraftClient.getInstance();
    Camera camera = client.gameRenderer.getCamera();

    if (camera == null || camera.getSubmersionType() != CameraSubmersionType.NONE) {
      return viewDistance;
    }

    ClientWorld world = client.world;

    if (world == null || !world.getRegistryKey().equals(World.OVERWORLD)) {
      return viewDistance;
    }

    ResolvedAtmosphere atmosphere = SeasonAtmosphere.resolve(world, BlockPos.ofFloored(camera.getPos()));

    return viewDistance * atmosphere.fogDistanceFactor();
  }
}
