package net.abakath.rpg4fools.mixin.client;

import net.abakath.rpg4fools.client.ResolvedAtmosphere;
import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grades the sky colour by the current season.
 *
 * <p>Vanilla still computes the colour, including its own time of day and weather handling. Only
 * the result is adjusted, so nothing about the existing sky behaviour is replaced.
 */
@Mixin(ClientWorld.class)
public abstract class ClientWorldSkyColorMixin {
  @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
  private void rpg4fools$gradeSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
    ClientWorld world = (ClientWorld) (Object) this;

    if (!world.getRegistryKey().equals(World.OVERWORLD)) {
      return;
    }

    Vec3d original = cir.getReturnValue();
    if (original == null) {
      return;
    }

    ResolvedAtmosphere atmosphere = SeasonAtmosphere.resolve(world, BlockPos.ofFloored(cameraPos));

    cir.setReturnValue(atmosphere.applyToSkyColor(original));
  }
}
