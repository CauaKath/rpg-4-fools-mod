package net.abakath.rpg4fools.mixin.client;

import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grades the sky colour by the current season.
 *
 * <p>The sky is an environment attribute now, and this probe is where one is answered, so grading
 * it here reaches whatever draws the sky rather than chasing that renderer.
 *
 * <p>Vanilla still computes the colour, including its own time of day and weather handling. Only
 * the result is adjusted, so nothing about the existing sky behaviour is replaced.
 *
 * <p>The fog is not graded here. Its colour and distances are done in
 * {@link AtmosphericFogEnvironmentMixin}, which sees them once a frame and in one place.
 */
@Mixin(EnvironmentAttributeProbe.class)
public abstract class EnvironmentAttributeProbeMixin {
  @Shadow
  private Level level;

  @Shadow
  private Vec3 position;

  @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
  private void rpg4fools$gradeSkyColor(EnvironmentAttribute<?> attribute,
                                       float partialTick,
                                       CallbackInfoReturnable<Object> cir) {
    if (attribute != EnvironmentAttributes.SKY_COLOR || level == null || position == null) {
      return;
    }

    if (!level.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    Object vanilla = cir.getReturnValue();
    if (vanilla == null) {
      return;
    }

    cir.setReturnValue(
            SeasonAtmosphere.resolve(level, BlockPos.containing(position)).applyToSkyColor((Integer) vanilla));
  }
}
