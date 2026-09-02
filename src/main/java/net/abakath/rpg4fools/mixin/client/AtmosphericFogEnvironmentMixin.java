package net.abakath.rpg4fools.mixin.client;

import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grades the fog colour by the current season and the biome underfoot.
 *
 * <p>Graded here rather than on the FOG_COLOR attribute the environment reads. That attribute is
 * the raw biome colour, and vanilla still has its sunrise and sunset work to do afterwards, which
 * washed most of a swamp's green back out. This is the finished colour, which is what the old hook
 * on the fog renderer graded.
 *
 * <p>Only the atmospheric environment is touched, so a camera in water, lava or powder snow keeps
 * its own fog: those are separate environments that never run this.
 */
@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {
  @Inject(method = "getBaseColor", at = @At("RETURN"), cancellable = true)
  private void rpg4fools$gradeFogColor(ClientLevel world,
                                       Camera camera,
                                       int viewDistance,
                                       float partialTick,
                                       CallbackInfoReturnable<Integer> cir) {
    if (world == null || camera == null || !world.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    cir.setReturnValue(
            SeasonAtmosphere.resolve(world, camera.blockPosition()).applyToFogColor(cir.getReturnValue()));
  }
}
