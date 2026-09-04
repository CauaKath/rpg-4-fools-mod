package net.abakath.rpg4fools.mixin.client;

import net.abakath.rpg4fools.client.ResolvedAtmosphere;
import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
@Mixin(ClientLevel.class)
public abstract class ClientLevelSkyColorMixin {
  @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
  private void rpg4fools$gradeSkyColor(Vec3 cameraPos, float tickDelta, CallbackInfoReturnable<Vec3> cir) {
    ClientLevel world = (ClientLevel) (Object) this;

    if (!world.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    Vec3 original = cir.getReturnValue();
    if (original == null) {
      return;
    }

    ResolvedAtmosphere atmosphere = SeasonAtmosphere.resolve(world, BlockPos.containing(cameraPos));

    cir.setReturnValue(atmosphere.applyToSkyColor(original));
  }
}
